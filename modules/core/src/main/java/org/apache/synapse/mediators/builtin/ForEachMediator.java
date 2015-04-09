/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.mediators.builtin;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMContainer;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.util.MessageHelper;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import java.util.ArrayList;
import java.util.List;

public class ForEachMediator extends AbstractMediator {

    private static OMContainer parent;
    /**
     * The xpath that will list the elements to be split
     */
    private SynapseXPath expression = null;

    private SequenceMediator sequence;

    private String sequenceRef;

    public boolean mediate(MessageContext synCtx) {
        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Foreach mediator");
        }

        if (expression == null) {
            synLog.error("ForEach: expression is null");
            return false;
        } else {
            synLog.traceOrDebug("ForEach: expression = " +
                    expression.toString());

            if (!validateSequenceRef(synCtx)) {
                synLog.error(
                        "ForEach: Referred sequence is invalid or null :: "
                                + "cannot contain Call, Send or CallOut mediators");
                return false;
            } else {

                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("Splitting with Xpath : " +
                            expression);

                }
                try {

                    SOAPEnvelope envelope = synCtx.getEnvelope();
                    // get the iteration elements and iterate through the list, this call
                    // will also detach all the iteration elements from the message and deduce the
                    // parent node to merge back the mediated content
                    List<?> splitElements =
                            getDetachedMatchingElements(envelope,
                                    synCtx,
                                    (SynapseXPath) expression);

                    if (synLog.isTraceOrDebugEnabled()) {
                        if (parent != null) {
                            synLog.traceOrDebug(
                                    "Parent node for merging is : " + parent.toString());
                        } else {
                            synLog.traceOrDebugWarn("Error detecting parent element to merge");
                        }
                    }

                    int msgCount = splitElements.size();
                    int msgNumber = 0;

                    if (synLog.isTraceOrDebugEnabled()) {
                        synLog.traceOrDebug("Splitting with XPath : " +
                                expression + " resulted in " +
                                msgCount + " elements");
                    }

                    // iterate through the list
                    for (Object element : splitElements) {
                        if (!(element instanceof OMNode)) {
                            handleException("Error splitting message with XPath : " +
                                            expression +
                                            " - result not an OMNode",
                                    synCtx);
                        }

                        if (synLog.isTraceOrDebugEnabled()) {
                            synLog.traceOrDebug("Submitting " + msgNumber +
                                    " of " + msgCount +
                                    " messages for processing in sequentially, in a general loop");
                        }

                        MessageContext iteratedMsgCtx =
                                getIteratedMessage(synCtx,
                                        envelope,
                                        (OMNode) element);

                        mediateSequence(iteratedMsgCtx);

                        //add the mediated element to the parent from original message context
                        ((OMElement) parent)
                                .addChild(iteratedMsgCtx.getEnvelope().getBody().getFirstElement());

                        synCtx.setEnvelope(envelope);
                    }

                } catch (JaxenException e) {
                    handleException("Error evaluating split XPath expression : " +
                            expression, e, synCtx);
                } catch (AxisFault af) {
                    handleException("Error creating an iterated copy of the message",
                            af, synCtx);
                }
                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("End : For Each mediator");
                }
                return true;

            }
        }
    }

    private void mediateSequence(MessageContext synCtx) {
        if (sequence != null) {
            if (log.isDebugEnabled()) {
                log.debug("Synchronously mediating using the in-line anonymous sequence");
            }
            sequence.mediate(synCtx);
        } else if (sequenceRef != null) {
            SequenceMediator refSequence = (SequenceMediator) synCtx.getSequence(sequenceRef);
            if (refSequence != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Synchronously mediating using the sequence " +
                            "named : " + sequenceRef);
                }
                refSequence.mediate(synCtx);
            }
        } else {
            handleException("Couldn't find the sequence named : " + sequenceRef, synCtx);
        }
    }

    /**
     * Validate at runtime the the Sequence of the ForEach mediator. The sequence cannot contain :
     * Call, CallOut and Send Mediators
     * <ul>
     * <li>A sequence with Call, CallOut or Send Mediators</li>
     * <li>A reference to a sequence that contains Call, CallOut or Send Mediators </li>
     * <li>A target endpoint</li>
     * <li>A reference to a target endpoint</li>
     * </ul>
     * <p/>
     * This method only validates a sequence reference since other cases are covered during mediator
     * creation.
     *
     * @param synCtx Message Context being mediated
     * @return validity of the sequence
     */
    private boolean validateSequenceRef(MessageContext synCtx) {
        //String sequenceRef = target.getSequenceRef();
        if (sequenceRef != null) {
            SequenceMediator refSequence =
                    (SequenceMediator) synCtx.getSequence(sequenceRef);
            if (refSequence != null) {
                return validateSequenceMediatorList(refSequence);
            } else {
                return false;
            }
        }
        return true;

    }

    /**
     * Validate a sequence mediator to not contain Call, CallOut or Send mediators in it.
     *
     * @param seqMediator Sequence Mediator to validate
     * @return validity of the sequence mediator
     */
    private boolean validateSequenceMediatorList(SequenceMediator seqMediator) {
        boolean valid = true;
        List<Mediator> mediators = seqMediator.getList();
        for (Mediator m : mediators) {
            if (m instanceof CallMediator) {
                valid = false;
                break;
            } else if (m instanceof CalloutMediator) {
                valid = false;
                break;
            } else if (m instanceof SendMediator) {
                valid = false;
                break;
            }
        }
        return valid;
    }

    /**
     * Create a new message context using the given original message context,
     * the envelope
     * and the split result element.
     *
     * @param synCtx   - original message context
     * @param envelope - envelope to be used in the iteration
     * @param omNode   - element which participates in the iteration replacement
     * @return new Message Context created by the iteration
     * @throws AxisFault      if there is a message creation failure
     * @throws JaxenException if the expression evaluation failure
     */
    private MessageContext getIteratedMessage(MessageContext synCtx,
                                              SOAPEnvelope envelope, OMNode omNode)
            throws AxisFault,
            JaxenException {
        MessageContext newCtx = MessageHelper.cloneMessageContext(synCtx);

        SOAPEnvelope newEnvelope = createNewSoapEnvelope(envelope);

        if (newEnvelope.getBody() != null) {

            if (newEnvelope.getBody().getFirstElement() != null) {
                newEnvelope.getBody().getFirstElement().detach();
            }
            newEnvelope.getBody().addChild(omNode);
        }

        newCtx.setEnvelope(newEnvelope);

        return newCtx;
    }

    private SOAPEnvelope createNewSoapEnvelope(SOAPEnvelope envelope) {
        SOAPFactory fac;
        if (SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI
                .equals(envelope.getBody().getNamespace().getNamespaceURI())) {
            fac = OMAbstractFactory.getSOAP11Factory();
        } else {
            fac = OMAbstractFactory.getSOAP12Factory();
        }
        return fac.getDefaultEnvelope();
    }

    /**
     * Return the set of detached elements specified by the XPath over the given envelope
     *
     * @param envelope   SOAPEnvelope from which the elements will be extracted
     * @param synCtx     Message context from which to extract the elements
     * @param expression SynapseXPath expression describing the elements to be extracted
     * @return List detached OMElements in the envelope matching the expression
     * @throws JaxenException if the XPath expression evaluation fails
     */
    public static List<OMNode> getDetachedMatchingElements(SOAPEnvelope envelope,
                                                           MessageContext synCtx,
                                                           SynapseXPath expression)
            throws JaxenException {

        List<OMNode> elementList = new ArrayList<OMNode>();
        Object o = expression.evaluate(envelope, synCtx);
        if (o instanceof OMNode) {
            parent = ((OMNode) o).getParent();
            elementList.add(((OMNode) o).detach());
        } else if (o instanceof List) {
            List oList = (List) o;
            if (oList.size() > 0) {
                parent = (((OMNode) oList.get(0)).getParent());
            } else {
                parent = null;
            }
            for (Object elem : oList) {
                if (elem instanceof OMNode) {
                    elementList.add(((OMNode) elem).detach());
                }
            }
        }
        return elementList;
    }

    public SynapseXPath getExpression() {
        return expression;
    }

    public void setExpression(SynapseXPath expression) {
        this.expression = expression;
    }

    public SequenceMediator getSequence() {
        return sequence;
    }

    public void setSequence(SequenceMediator sequence) {
        this.sequence = sequence;
    }

    public String getSequenceRef() {
        return sequenceRef;
    }

    public void setSequenceRef(String sequenceKey) {
        this.sequenceRef = sequenceKey;
    }

}
