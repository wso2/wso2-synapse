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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.JsonPath;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.OperationContext;
import org.apache.synapse.ContinuationState;
import org.apache.synapse.FaultHandler;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.StatisticIdentityGenerator;
import org.apache.synapse.aspects.flow.statistics.collectors.OpenEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.aspects.flow.statistics.data.artifact.ArtifactHolder;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.continuation.ContinuationStackManager;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.FlowContinuableMediator;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.eip.SharedDataHolder;
import org.apache.synapse.mediators.eip.EIPConstants;
import org.apache.synapse.mediators.eip.EIPUtils;
import org.apache.synapse.mediators.eip.Target;
import org.apache.synapse.util.MessageHelper;
import org.apache.synapse.util.xpath.SynapseJsonPath;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

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
     * A Path expression that specifies where the splitted elements should be attached when
     * the payload is being preserved
     */
    private SynapsePath attachPath = null;

    /** The target for the newly splitted messages */
    private Target target = null;

    private String id = null;

    private SynapseEnvironment synapseEnv;

    /**
     * A flag used to check whether attachPath was present in the original synapse configuration
     */
    private boolean isAttachPathPresent;

    /**
     * Splits the message by iterating over the results of the given Path expression
     *
     * @param synCtx - MessageContext to be mediated
     * @return boolean false if need to stop processing of the parent message
     */
    public boolean mediate(MessageContext synCtx) {

        if (synCtx.getEnvironment().isDebuggerEnabled()) {
            if (super.divertMediationRoute(synCtx)) {
                return true;
            }
        }

        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Iterate mediator");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        synCtx.setProperty(id != null ? EIPConstants.EIP_SHARED_DATA_HOLDER + "." + id :
                EIPConstants.EIP_SHARED_DATA_HOLDER, new SharedDataHolder());

        try {

            // check whether expression contains jsonpath or xpath and process according to it
            if (expression != null && expression instanceof SynapseJsonPath) {

                // SynapseJSONPath implementation reads the JSON stream and execute the JSON path.
                Object resultValue = expression.evaluate(synCtx);

                //Gson parser to parse the string json objects
                JsonParser parser = new JsonParser();

                //Complete json payload read from the synCtx
                Object rootJSON = parser.parse(
                        JsonUtil.jsonPayloadToString(((Axis2MessageContext) synCtx).getAxis2MessageContext())).
                        toString();

                //delete the iterated json object if the attachPath is different from expression.
                // If both are same, the json object will be replaced eventually, so no need to do it here
                if (!((SynapseJsonPath) expression).getJsonPath().getPath()
                        .equals(((SynapseJsonPath) attachPath).getJsonPath().getPath())) {

                    //parse the json into gson to delete the iterated json array
                    JsonElement rootJsonElement = parser.parse(rootJSON.toString());

                    //Check whether the JSON element expressed by the jsonpath is a valid JsonArray
                    //else throw an exception
                    if (!(EIPUtils.formatJsonPathResponse(JsonPath.parse(rootJsonElement.toString())
                            .read(((SynapseJsonPath) expression).getJsonPath())) instanceof JsonArray)) {
                        handleException("JSON element expressed by the path "
                                + ((SynapseJsonPath) expression).getJsonPathExpression()
                                + " is not a valid JSON array that can be iterated", synCtx);
                    };

                    //replace the expressed jsonElement with an empty array
                    rootJSON = JsonPath.parse(rootJsonElement.toString())
                            .set(((SynapseJsonPath) expression).getJsonPath(), new JsonArray()).jsonString();

                }

                if (resultValue instanceof List) {
                    List list = (List) resultValue;

                    int msgNumber = 0;
                    int msgCount = list.size();

                    for (Object o : list) {
                        MessageContext iteratedMsgCtx
                                = getIteratedMessage(synCtx, msgNumber++, msgCount, rootJSON, o);
                        ContinuationStackManager.
                                addReliantContinuationState(iteratedMsgCtx, 0, getMediatorPosition());
                        if (target.isAsynchronous()) {
                            target.mediate(iteratedMsgCtx);
                        } else {
                            try {
                                /*
                                 * if Iteration is sequential we won't be able to execute correct fault
                                 * handler as data are lost with clone message ending execution. So here we
                                 * copy fault stack of clone message context to original message context
                                 */
                                target.mediate(iteratedMsgCtx);
                            } catch (SynapseException synEx) {
                                copyFaultyIteratedMessage(synCtx, iteratedMsgCtx);
                                throw synEx;
                            } catch (Exception e) {
                                copyFaultyIteratedMessage(synCtx, iteratedMsgCtx);
                                handleException("Exception occurred while executing sequential iteration " +
                                        "in the Iterator Mediator", e, synCtx);
                            }
                        }
                    }
                }

            } else {
                // get a copy of the message for the processing, if the continueParent is set to true
                // this original message can go in further mediations and hence we should not change
                // the original message context
                SOAPEnvelope envelope = MessageHelper.cloneSOAPEnvelope(synCtx.getEnvelope());

                // get the iteration elements and iterate through the list,
                // this call will also detach all the iteration elements
                List splitElements = EIPUtils.getDetachedMatchingElements(envelope, synCtx, (SynapseXPath) expression);

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
                                "Submitting " + (msgNumber+1) + " of " + msgCount +
                                        (target.isAsynchronous() ? " messages for processing in parallel" :
                                                " messages for processing in sequentially"));
                    }

                    MessageContext iteratedMsgCtx =
                            getIteratedMessage(synCtx, msgNumber++, msgCount, envelope, (OMNode) o);
                    ContinuationStackManager.
                            addReliantContinuationState(iteratedMsgCtx, 0, getMediatorPosition());
                    if (target.isAsynchronous()) {
                        target.mediate(iteratedMsgCtx);
                    } else {
                        try {
                            /*
                             * if Iteration is sequential we won't be able to execute correct fault
                             * handler as data are lost with clone message ending execution. So here we
                             * copy fault stack of clone message context to original message context
                             */
                            target.mediate(iteratedMsgCtx);
                        } catch (SynapseException synEx) {
                            copyFaultyIteratedMessage(synCtx, iteratedMsgCtx);
                            throw synEx;
                        } catch (Exception e) {
                            copyFaultyIteratedMessage(synCtx, iteratedMsgCtx);
                            handleException("Exception occurred while executing sequential iteration " +
                                    "in the Iterator Mediator", e, synCtx);
                        }
                    }
                }
            }

        } catch (JaxenException e) {
            handleException("Error evaluating split XPath expression : " + expression, e, synCtx);
        } catch (AxisFault af) {
            handleException("Error creating an iterated copy of the message", af, synCtx);
        } catch (SynapseException synEx) {
            throw synEx;
        } catch (Exception e) {
            handleException("Exception occurred while executing the Iterate Mediator", e, synCtx);
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

    /**
     * Copy fault stack and properties of the iteratedMsgCtx to synCtx
     *
     * @param synCtx         Original Synapse Message Context
     * @param iteratedMsgCtx cloned Message Context used for the iteration
     */
    private void copyFaultyIteratedMessage(MessageContext synCtx, MessageContext iteratedMsgCtx) {
        synCtx.getFaultStack().clear(); //remove original fault stack
        Stack<FaultHandler> faultStack = iteratedMsgCtx.getFaultStack();

        if (!faultStack.isEmpty()) {
            List<FaultHandler> newFaultStack = new ArrayList<FaultHandler>();
            newFaultStack.addAll(faultStack);
            for (FaultHandler faultHandler : newFaultStack) {
                if (faultHandler != null) {
                    synCtx.pushFaultHandler(faultHandler);
                }
            }
        }
        // copy all the String keyed synapse level properties to the Original synCtx
        for (Object keyObject : iteratedMsgCtx.getPropertyKeySet()) {
            /*
             * There can be properties added while executing the iterated sequential flow and
             * these may be accessed in the fault sequence, so updating string valued properties
             */
            if (keyObject instanceof String) {
                String stringKey = (String) keyObject;
                synCtx.setProperty(stringKey, iteratedMsgCtx.getProperty(stringKey));
            }
        }
    }

    public boolean mediate(MessageContext synCtx,
                           ContinuationState continuationState) {
        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Iterate mediator : Mediating from ContinuationState");
        }

        boolean result;
        SequenceMediator branchSequence = target.getSequence();
        boolean isStatisticsEnabled = RuntimeStatisticCollector.isStatisticsEnabled();
        if (!continuationState.hasChild()) {
            result = branchSequence.mediate(synCtx, continuationState.getPosition() + 1);
        } else {
            FlowContinuableMediator mediator =
                    (FlowContinuableMediator) branchSequence.getChild(continuationState.getPosition());

            result = mediator.mediate(synCtx, continuationState.getChildContState());

            if (isStatisticsEnabled) {
                ((Mediator) mediator).reportCloseStatistics(synCtx, null);
            }
        }
        if (isStatisticsEnabled) {
            branchSequence.reportCloseStatistics(synCtx, null);
        }
        return result;
    }

    /**
     * Creates a new message context using the given original message context, the envelope
     *      * and the split result element. This is method is specific for JSON payloads
     * @param synCtx original message context
     * @param msgNumber message number in the iteration
     * @param msgCount total number of messages in the split
     * @param rootJsonObject total number of messages in the split
     * @param node Json object to be attached
     * @return newCtx created by the iteration
     * @throws AxisFault if there is a message creation failure
     * @throws JaxenException if the expression evaluation failure
     */
    private MessageContext getIteratedMessage(MessageContext synCtx, int msgNumber,
                                              int msgCount, Object rootJsonObject, Object node)
            throws AxisFault, JaxenException {

        // clone the message for the mediation in iteration
        MessageContext newCtx = MessageHelper.cloneMessageContext(synCtx);

        //Remove the original jsonstream from the context
        JsonUtil.removeJsonPayload(((Axis2MessageContext) newCtx).getAxis2MessageContext());

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
        Object rootObject = node;

        // if payload should be preserved then attach the iteration element to the
        // node specified by the attachPath
        if (preservePayload) {
            rootObject = rootJsonObject;
            if (rootObject != null){
                rootObject = ((SynapseJsonPath) attachPath).replace(rootObject, node);
            } else {
                handleException("Error in attaching the splitted elements :: " +
                        "Unable to get the attach path specified by the expression " +
                        attachPath, synCtx);
            }
        }

        // write the new JSON message to the stream
        JsonUtil.getNewJsonPayload(((Axis2MessageContext) newCtx).getAxis2MessageContext(),
                rootObject.toString(), true, true);

        // Set isServerSide property in the cloned message context
        ((Axis2MessageContext) newCtx).getAxis2MessageContext().setServerSide(
                ((Axis2MessageContext) synCtx).getAxis2MessageContext().isServerSide());


        return newCtx;
    }

    /**
     * Creates a new message context using the given original message context, the envelope
     * and the split result element. This method is specific for xml payloads
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

        // clone the message context without cloning the SOAP envelope, for the mediation in iteration.
        MessageContext newCtx = MessageHelper.cloneMessageContext(synCtx, false, false);

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

            Object attachElem = ((SynapseXPath) attachPath).evaluate(newEnvelope, synCtx);

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

        // Set isServerSide property in the cloned message context
        ((Axis2MessageContext) newCtx).getAxis2MessageContext().setServerSide(
                ((Axis2MessageContext) synCtx).getAxis2MessageContext().isServerSide());

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

    @Override
    public boolean isContentAltering() {
        return true;
    }

    public boolean isAttachPathPresent() {
        return isAttachPathPresent;
    }

    public void setAttachPathPresent(boolean attachPathPresent) {
        isAttachPathPresent = attachPathPresent;
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

    @Override
    public Integer reportOpenStatistics(MessageContext messageContext, boolean isContentAltering) {
        return OpenEventCollector.reportFlowSplittingEvent(messageContext, getMediatorName(), ComponentType.MEDIATOR,
                                                           getAspectConfiguration(),
                                                           isContentAltering() || isContentAltering);
    }

    @Override
    public void setComponentStatisticsId(ArtifactHolder holder) {
        if (getAspectConfiguration() == null) {
            configure(new AspectConfiguration(getMediatorName()));
        }
        String mediatorId =
                StatisticIdentityGenerator.getIdForFlowContinuableMediator(getMediatorName(), ComponentType.MEDIATOR, holder);
        getAspectConfiguration().setUniqueId(mediatorId);
        if(target != null){
            target.setStatisticIdForMediators(holder);
        }
        StatisticIdentityGenerator.reportingFlowContinuableEndEvent(mediatorId, ComponentType.MEDIATOR, holder);
    }
}
