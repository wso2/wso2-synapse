/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.mediators.eip.splitter;

import java.util.Iterator;
import java.util.List;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.OperationContext;
import org.apache.synapse.ContinuationState;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.commons.json.JSONProviderUtil;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.continuation.ContinuationStackManager;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.FlowContinuableMediator;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.eip.EIPConstants;
import org.apache.synapse.mediators.eip.EIPUtils;
import org.apache.synapse.mediators.eip.Target;
import org.apache.synapse.util.MessageHelper;
import org.apache.synapse.util.xpath.SynapseJsonPath;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

/**
 * Splits a message using an XPath expression and creates a new message to hold
 * each resulting element. This is very much similar to the clone mediator, and
 * hands over the newly created messages to a target for processing
 */
public class IterateMediator extends AbstractMediator implements ManagedLifecycle,
                                                                 FlowContinuableMediator {

    /** Continue mediation on the parent message or not? */
    private boolean continueParent = false;

    /**
     * Preserve the payload as a template to create new messages with the selected
     * elements with the rest of the parent, or create new message that contain only
     * the selected element as its payload?
     */
    private boolean preservePayload = false;

    /** The Path that will list the elements to be splitted */
    private SynapsePath expression = null;

    /**
     * An Path expression that specifies where the splitted elements should be attached when
     * the payload is being preserved
     */
    private SynapsePath attachPath = null;

    /** The target for the newly splitted messages */
    private Target target = null;

    private String id = null;

    private SynapseEnvironment synapseEnv;

    /**
     * Splits the message by iterating over the results of the given XPath expression
     *
     * @param synCtx - MessageContext to be mediated
     * @return boolean false if need to stop processing of the parent message
     */
    public boolean mediate(MessageContext synCtx) {

        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Iterate mediator");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        try {
        	// If the expression is an instance of SynapseXpath then XML version will be used. Otherwise JSON Stream will be used.
        	if(expression!=null && expression instanceof SynapseXPath){
                // get a copy of the message for the processing, if the continueParent is set to true
                // this original message can go in further mediations and hence we should not change
                // the original message context
                SOAPEnvelope envelope = MessageHelper.cloneSOAPEnvelope(synCtx.getEnvelope());
    
                // get the iteration elements and iterate through the list,
                // this call will also detach all the iteration elements 
                List splitElements = EIPUtils.getDetachedMatchingElements(envelope, synCtx, (SynapseXPath)expression); // TODO Check
    
                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("Splitting with XPath : " + expression + " resulted in " +
                        splitElements.size() + " elements");
                }
    
                // if not preservePayload remove all the child elements
                if (!preservePayload && envelope.getBody() != null) {
                    for (Iterator itr = envelope.getBody().getChildren(); itr.hasNext();) {
                        ((OMNode) itr.next()).detach();
                    }
                }
    
                int msgCount = splitElements.size();
                int msgNumber = 0;
                
                // iterate through the list
                for (Object o : splitElements) {
                    // for the moment iterator will look for an OMNode as the iteration element
                    if (!(o instanceof OMNode)) {
                        handleException("Error splitting message with XPath : "
                            + expression + " - result not an OMNode", synCtx);
                    }
    
                    if (synLog.isTraceOrDebugEnabled()) {
                        synLog.traceOrDebug(
                                "Submitting " + (msgNumber+1) + " of " + msgNumber +
                                (target.isAsynchronous() ? " messages for processing in parallel" :
                                 " messages for processing in sequentially"));
                    }
    
                    MessageContext itereatedMsgCtx =
                            getIteratedMessage(synCtx, msgNumber++, msgCount, envelope, (OMNode) o);
                    ContinuationStackManager.
                            addReliantContinuationState(itereatedMsgCtx, 0, getMediatorPosition());
                    target.mediate(itereatedMsgCtx);
                }
        	}else{
        		// SynapseJSONPath implementation read the JSON stream and execute the JSON path.
        		Object resultValue = null;
				if (expression != null)
					resultValue = expression.evaluate(synCtx);
				
				Object rootObject=EIPUtils.getRootJSONObject((Axis2MessageContext) synCtx);
				JsonUtil.removeJsonPayload(((Axis2MessageContext) synCtx).getAxis2MessageContext());
				
				int msgNumber = 0;
				int msgCount=0;
				if(resultValue!=null && resultValue instanceof List){
					List list=(List)resultValue;
					msgCount=list.size();
					for (int i = 0; i < list.size(); i++) {
						MessageContext itereatedMsgCtx = getIteratedMessage(synCtx, msgNumber++, msgCount, rootObject, list.get(i));
						ContinuationStackManager.addReliantContinuationState(itereatedMsgCtx, 0, getMediatorPosition());
						target.mediate(itereatedMsgCtx);
					}
				}
				JsonUtil.newJsonPayload(((Axis2MessageContext) synCtx).getAxis2MessageContext(), JSONProviderUtil.objectToString(rootObject), true, true);
        	}
        } catch (JaxenException e) {
            handleException("Error evaluating split Path expression : " + expression, e, synCtx);
        } catch (AxisFault af) {
            handleException("Error creating an iterated copy of the message", af, synCtx);
        }

        // if the continuation of the parent message is stopped from here set the RESPONSE_WRITTEN
        // property to SKIP to skip the blank http response 
        OperationContext opCtx
            = ((Axis2MessageContext) synCtx).getAxis2MessageContext().getOperationContext();
        if (!continueParent && opCtx != null) {
            opCtx.setProperty(Constants.RESPONSE_WRITTEN,"SKIP");
        }

        synLog.traceOrDebug("End : Iterate mediator");

        // whether to continue mediation on the original message
        return continueParent;
    }

    public boolean mediate(MessageContext synCtx,
                           ContinuationState continuationState) {
        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Iterate mediator : Mediating from ContinuationState");
        }

        boolean result;
        if (!continuationState.hasChild()) {
            result = target.getSequence().mediate(synCtx, continuationState.getPosition() + 1);
        } else {
            FlowContinuableMediator mediator =
                    (FlowContinuableMediator) target.getSequence().
                            getChild(continuationState.getPosition());
            result = mediator.mediate(synCtx, continuationState.getChildContState());
        }
        return result;
    }
    
    /**
     * This method is for JSON messages
     * @param synCtx
     * @param msgNumber
     * @param msgCount
     * @param node
     * @return
     * @throws AxisFault
     * @throws JaxenException
     */
    private MessageContext getIteratedMessage(MessageContext synCtx, int msgNumber, int msgCount, Object rootJsonObject, Object node) throws AxisFault, JaxenException {
    	// clone the message for the mediation in iteration
        MessageContext newCtx = MessageHelper.cloneMessageContext(synCtx);
        if (id != null) {
            // set the parent correlation details to the cloned MC -
            //                              for the use of aggregation like tasks
            newCtx.setProperty(EIPConstants.AGGREGATE_CORRELATION + "." + id,
                    synCtx.getMessageID());
            // set the messageSequence property for possibal aggreagtions
            newCtx.setProperty(
                    EIPConstants.MESSAGE_SEQUENCE + "." + id,
                    msgNumber + EIPConstants.MESSAGE_SEQUENCE_DELEMITER + msgCount);
        } else {
            newCtx.setProperty(
                    EIPConstants.MESSAGE_SEQUENCE,
                    msgNumber + EIPConstants.MESSAGE_SEQUENCE_DELEMITER + msgCount);
        }
        // Initially set the extracted object as root and send if payload is not preserved
        Object rootObject=node;

        // if payload should be preserved then attach the iteration element to the
        // node specified by the attachPath
        if (preservePayload) {
        	//rootObject=EIPUtils.getRootJSONObject((Axis2MessageContext) synCtx);
        	rootObject=EIPUtils.getRootJSONObject(JSONProviderUtil.objectToString(rootJsonObject));
        	if(rootObject!=null){
        		rootObject = ((SynapseJsonPath)attachPath).replace(rootObject, node);
        	}else{
        		handleException("Error in attaching the splitted elements :: " +
                        "Unable to get the attach path specified by the expression " +
                        attachPath, synCtx);
        	}
        }
        // write the new JSON message to the stream
        JsonUtil.newJsonPayload(((Axis2MessageContext) newCtx).getAxis2MessageContext(), JSONProviderUtil.objectToString(rootObject), true, true);
        return newCtx;
    }
    

    /**
     * Create a new message context using the given original message context, the envelope
     * and the split result element.
     *
     * @param synCtx    - original message context
     * @param msgNumber - message number in the iteration
     * @param msgCount  - total number of messages in the split
     * @param envelope  - envelope to be used in the iteration
     * @param o         - element which participates in the iteration replacement
     * @return newCtx created by the iteration
     * @throws AxisFault if there is a message creation failure
     * @throws JaxenException if the expression evauation failure
     */
    private MessageContext getIteratedMessage(MessageContext synCtx, int msgNumber, int msgCount,
        SOAPEnvelope envelope, OMNode o) throws AxisFault, JaxenException {
        
        // clone the message for the mediation in iteration
        MessageContext newCtx = MessageHelper.cloneMessageContext(synCtx);

        if (id != null) {
            // set the parent correlation details to the cloned MC -
            //                              for the use of aggregation like tasks
            newCtx.setProperty(EIPConstants.AGGREGATE_CORRELATION + "." + id,
                    synCtx.getMessageID());
            // set the messageSequence property for possibal aggreagtions
            newCtx.setProperty(
                    EIPConstants.MESSAGE_SEQUENCE + "." + id,
                    msgNumber + EIPConstants.MESSAGE_SEQUENCE_DELEMITER + msgCount);
        } else {
            newCtx.setProperty(
                    EIPConstants.MESSAGE_SEQUENCE,
                    msgNumber + EIPConstants.MESSAGE_SEQUENCE_DELEMITER + msgCount);
        }

        // get a clone of the envelope to be attached
        SOAPEnvelope newEnvelope = MessageHelper.cloneSOAPEnvelope(envelope);

        // if payload should be preserved then attach the iteration element to the
        // node specified by the attachPath
        if (preservePayload) {
            Object attachElem = attachPath.evaluate(newEnvelope);
            if (attachElem != null &&
                attachElem instanceof List && !((List) attachElem).isEmpty()) {
                attachElem = ((List) attachElem).get(0);
            }

            // for the moment attaching element should be an OMElement
            if (attachElem != null && attachElem instanceof OMElement) {
                ((OMElement) attachElem).addChild(o);
            } else {
                handleException("Error in attaching the splitted elements :: " +
                    "Unable to get the attach path specified by the expression " +
                    attachPath, synCtx);
            }
        } else if (newEnvelope.getBody() != null) {
            // if not preserve payload then attach the iteration element to the body
        	if(newEnvelope.getBody().getFirstElement() !=null){
        		newEnvelope.getBody().getFirstElement().detach();
        	}
            newEnvelope.getBody().addChild(o);
        }

        // set the envelope and mediate as specified in the target
        newCtx.setEnvelope(newEnvelope);

        return newCtx;
    }

    ///////////////////////////////////////////////////////////////////////////////////////
    //                        Getters and Setters                                        //
    ///////////////////////////////////////////////////////////////////////////////////////

    public boolean isContinueParent() {
        return continueParent;
    }

    public void setContinueParent(boolean continueParent) {
        this.continueParent = continueParent;
    }

    public boolean isPreservePayload() {
        return preservePayload;
    }

    public void setPreservePayload(boolean preservePayload) {
        this.preservePayload = preservePayload;
    }

    public SynapsePath getExpression() {
        return expression;
    }

    public void setExpression(SynapsePath expression) {
        this.expression = expression;
    }

    public SynapsePath getAttachPath() {
        return attachPath;
    }

    public void setAttachPath(SynapsePath attachPath) {
        this.attachPath = attachPath;
    }

    public Target getTarget() {
        return target;
    }

    public void setTarget(Target target) {
        this.target = target;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void init(SynapseEnvironment se) {

        synapseEnv = se;
        if (target != null) {
            Endpoint endpoint = target.getEndpoint();
            if (endpoint != null) {
                endpoint.init(se);
            }

            ManagedLifecycle seq = target.getSequence();
            if (seq != null) {
                seq.init(se);
            } else if (target.getSequenceRef() != null) {
                SequenceMediator targetSequence =
                        (SequenceMediator) se.getSynapseConfiguration().
                                getSequence(target.getSequenceRef());

                if (targetSequence == null || targetSequence.isDynamic()) {
                    se.addUnavailableArtifactRef(target.getSequenceRef());
                }
            }
        }
    }

    public void destroy() {
        if (target != null) {
            Endpoint endpoint = target.getEndpoint();
            if (endpoint != null && endpoint.isInitialized()) {
                endpoint.destroy();
            }

            ManagedLifecycle seq = target.getSequence();
            if (seq != null) {
                seq.destroy();
            } else if (target.getSequenceRef() != null) {
                SequenceMediator targetSequence =
                        (SequenceMediator) synapseEnv.getSynapseConfiguration().
                                getSequence(target.getSequenceRef());

                if (targetSequence == null || targetSequence.isDynamic()) {
                    synapseEnv.removeUnavailableArtifactRef(target.getSequenceRef());
                }
            }
        }
    }

}