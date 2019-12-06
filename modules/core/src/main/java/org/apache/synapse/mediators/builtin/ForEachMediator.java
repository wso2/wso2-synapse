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

package org.apache.synapse.mediators.builtin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMContainer;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.commons.json.Constants;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.continuation.ContinuationStackManager;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.eip.EIPUtils;
import org.apache.synapse.util.MessageHelper;
import org.apache.synapse.util.xpath.SynapseJsonPath;
import org.apache.synapse.util.xpath.SynapseXPath;

import java.util.ArrayList;
import java.util.List;

public class ForEachMediator extends AbstractMediator implements ManagedLifecycle {

    /* The path that will list the elements to be split */
    private SynapsePath expression = null;

    /* Reference to the synapse environment */
    private SynapseEnvironment synapseEnv;

    private SequenceMediator sequence;

    private String sequenceRef;

    private String id;

    private static final String FOREACH_ORIGINAL_MESSAGE = "FOREACH_ORIGINAL_MESSAGE";

    private static final String FOREACH_COUNTER = "FOREACH_COUNTER";

    public boolean mediate(MessageContext synCtx) {

        if (synCtx.getEnvironment().isDebuggerEnabled()) {
            if (super.divertMediationRoute(synCtx)) {
                return true;
            }
        }

        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Foreach mediator");
        }

        if (expression == null) {
            handleException("ForEach: expression is null", synCtx);
        }

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("ForEach expression : " + expression.toString());
        }

        String idPrefix = (getId() == null) ? "" : getId() + "_";

        // Set original message property
        String originalMsgPropName = idPrefix + FOREACH_ORIGINAL_MESSAGE;
        SOAPEnvelope originalEnvelope = MessageHelper.cloneSOAPEnvelope(synCtx.getEnvelope());
        synCtx.setProperty(originalMsgPropName, originalEnvelope);

        // Set counter property
        String counterPropName = idPrefix + FOREACH_COUNTER;
        int msgCounter = 0;
        synCtx.setProperty(counterPropName, msgCounter);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Saved original message property : " + originalMsgPropName +
                    " = " + originalEnvelope +
                    "Initialized foreach counter property, " + counterPropName +
                    " = " + msgCounter);
        }

        if (expression != null && expression instanceof SynapseJsonPath) {
            //Gson parser to parse the string json objects
            JsonParser parser = new JsonParser();

            //Read the complete JSON payload from the synCtx
            String jsonPayload = JsonUtil.jsonPayloadToString(((Axis2MessageContext) synCtx).getAxis2MessageContext());
            DocumentContext parsedJsonPayload = JsonPath.parse(jsonPayload);

            // SynapseJSONPath implementation reads the JSON stream and execute the JSON path.
            JsonElement iterableChildElements = parsedJsonPayload.read(((SynapseJsonPath) expression).getJsonPath());

            //Check whether the JSON element expressed by the jsonpath is a valid JsonArray
            //else throw an exception
            if (!(iterableChildElements instanceof JsonArray)) {
                handleException("JSON element expressed by the path "
                        + ((SynapseJsonPath) expression).getJsonPathExpression()
                        + " is not a valid JSON array", synCtx);
            }
            JsonArray iterableJsonArray = iterableChildElements.getAsJsonArray();
            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("Splitting with JSONPath : " + expression + " resulted in " +
                        iterableJsonArray.size() + " elements.");
            }
            JsonArray modifiedPayloadArray = new JsonArray();
            for (JsonElement element : iterableJsonArray) {
                try {
                    updateIteratedMessage(synCtx, element);
                    boolean mediateResult = mediateSequence(synCtx);
                    modifiedPayloadArray.add(EIPUtils.tryParseJsonString(parser,
                            JsonUtil.jsonPayloadToString(((Axis2MessageContext) synCtx).getAxis2MessageContext())));
                    msgCounter++;
                    if (synLog.isTraceOrDebugEnabled()) {
                        synLog.traceOrDebug("Incrementing foreach counter , " + counterPropName + " = "
                                + msgCounter);
                    }
                    synCtx.setProperty(counterPropName, msgCounter);
                    if (!mediateResult) { // break the loop if mediate result is false
                        break;
                    }
                } catch (AxisFault axisFault) {
                    handleException("Error updating the stream with iterater element : " +
                            element.toString(), axisFault, synCtx);
                }
            }
            JsonElement jsonPayloadElement = parsedJsonPayload
                    .set(((SynapseJsonPath) expression).getJsonPath(), modifiedPayloadArray).json();
            try {
                JsonUtil.getNewJsonPayload(((Axis2MessageContext) synCtx).getAxis2MessageContext(),
                        jsonPayloadElement.toString(), true, true);
            } catch (AxisFault af) {
                handleException("Error updating the json stream after foreach transformation", af, synCtx);
            }
        } else {
            SOAPEnvelope processingEnvelope = synCtx.getEnvelope();

            // get the iteration elements and iterate through the list, this call
            // will also detach all the iteration elements from the message and deduce the
            // parent node to merge back the mediated content
            DetachedElementContainer detachedElementContainer =
                    getDetachedMatchingElements(processingEnvelope, synCtx, (SynapseXPath) expression);

            List<?> splitElements = detachedElementContainer.getDetachedElements();

            int splitElementCount = splitElements.size();
            if (splitElementCount == 0) {  // Continue the message flow if no matching elements found
                return true;
            }

            OMContainer parent = detachedElementContainer.getParent();
            if (parent == null) {
                handleException("Error detecting parent element to merge", synCtx);
            }
            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("Splitting with XPath : " + expression + " resulted in " + splitElementCount +
                        " elements. Parent node for merging is : " + parent.toString());
            }

            // iterate through the split elements
            for (Object element : splitElements) {

                if (!(element instanceof OMNode)) {
                    handleException("Error splitting message with XPath : " + expression +
                            " - result not an OMNode", synCtx);
                }
                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("Submitting " + msgCounter + " of " + splitElementCount +
                            " messages for processing in sequentially, in a general loop");
                }

                MessageContext iteratedMsgCtx = null;
                try {
                    iteratedMsgCtx = getIteratedMessage(synCtx, processingEnvelope,
                            (OMNode) element);

                    //Removes the json stream property from the iterated context.
                    ((Axis2MessageContext) iteratedMsgCtx).getAxis2MessageContext().
                            removeProperty(Constants.ORG_APACHE_SYNAPSE_COMMONS_JSON_JSON_INPUT_STREAM);

                } catch (AxisFault axisFault) {
                    handleException("Error creating an iterated copy of the message", axisFault, synCtx);
                }
                boolean mediateResult = mediateSequence(iteratedMsgCtx);
                //add the mediated element to the parent from original message context
                parent.addChild(iteratedMsgCtx.getEnvelope().getBody().getFirstElement());

                msgCounter++;
                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("Incrementing foreach counter , " + counterPropName + " = "
                            + msgCounter);
                }
                synCtx.setProperty(counterPropName, msgCounter);

                if (!mediateResult) { // break the loop if mediate result is false
                    break;
                }
            }

            //set the modified envelop to message context
            try {
                synCtx.setEnvelope(processingEnvelope);
            } catch (AxisFault axisFault) {
                handleException("Error while setting the envelope to the message context", axisFault, synCtx);
            }
        }

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("After mediation foreach counter, " + counterPropName + " = "
                    + msgCounter);
            synLog.traceOrDebug("End : For Each mediator");
        }

        return true;
    }

    private boolean mediateSequence(MessageContext synCtx) {
        if (sequence != null) {
            if (log.isDebugEnabled()) {
                log.debug("Synchronously mediating using the in-line anonymous sequence");
            }
            ContinuationStackManager.addReliantContinuationState(synCtx, 1, getMediatorPosition());
            return sequence.mediate(synCtx);

        } else if (sequenceRef != null) {
            SequenceMediator referredSequence = (SequenceMediator) synCtx.getSequence(sequenceRef);
            if (referredSequence != null) {
                if (!validateSequence(referredSequence)) {
                    handleException("ForEach: Referred sequence cannot contain Call," +
                            " Send or CallOut mediators", synCtx);
                }
                if (log.isDebugEnabled()) {
                    log.debug("Synchronously mediating using the sequence " + "named : " + sequenceRef);
                }
                return referredSequence.mediate(synCtx);
            } else {
                handleException("Couldn't find the sequence named : " + sequenceRef, synCtx);
            }
        } else {
            handleException("Couldn't find sequence information", synCtx);
        }
        return false;
    }

    /**
     * Validate a sequence mediator to not contain Call, CallOut or Send mediators in it.
     *
     * @param seqMediator Sequence Mediator to validate
     * @return validity of the sequence mediator
     */
    private boolean validateSequence(SequenceMediator seqMediator) {
        List<Mediator> mediators = seqMediator.getList();
        for (Mediator m : mediators) {
            if (m instanceof CallMediator || m instanceof CalloutMediator ||
                    m instanceof SendMediator) {
                return false;
            }
        }
        return true;
    }

    /**
     * Create a new message context using the given original message context,
     * the envelope
     * and the split result element.
     *
     * @param synCtx           original message context
     * @param omNode           element which participates in the iteration replacement
     * @param originalEnvelope original envelope when reaching foreach
     * @return modified message context with new envelope created with omNode
     * @throws AxisFault if there is a message creation failure
     */
    private MessageContext getIteratedMessage(MessageContext synCtx, SOAPEnvelope originalEnvelope,
                                              OMNode omNode) throws AxisFault {

        SOAPEnvelope newEnvelope = createNewSoapEnvelope(originalEnvelope);

        if (newEnvelope.getBody() != null) {
            newEnvelope.getBody().addChild(omNode);
        }

        //set the new envelop to original message context
        synCtx.setEnvelope(newEnvelope);

        return synCtx;
    }

    /**
     * Update the message context using the given original message context and
     * jsonElement
     *
     * @param synCtx       original message context
     * @param splitElement JsonElement which participates in the foreach operation
     * @return modified message context with new envelope created with new jsonstream
     * @throws AxisFault if there is a message creation failure
     */
    private void updateIteratedMessage(MessageContext synCtx, JsonElement splitElement)
            throws AxisFault {
        // write the new JSON message to the stream
        JsonUtil.getNewJsonPayload(((Axis2MessageContext) synCtx).getAxis2MessageContext(),
                splitElement.toString(), true, true);
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
     * Return the set of detached elements and parent specified by the XPath over the given envelope
     *
     * @param envelope   SOAPEnvelope from which the elements will be extracted
     * @param synCtx     Message context from which to extract the elements
     * @param expression SynapseXPath expression describing the elements to be extracted
     * @return data container which hold the detached OMElements in the envelope matching the expression
     * and the parent
     */
    private DetachedElementContainer getDetachedMatchingElements(SOAPEnvelope envelope,
                                                                 MessageContext synCtx,
                                                                 SynapseXPath expression) {

        DetachedElementContainer resultContainer = new DetachedElementContainer();
        List<OMNode> elementList = new ArrayList<>();
        Object o = expression.evaluate(envelope, synCtx);
        if (o instanceof OMNode) {
            resultContainer.setParent(((OMNode) o).getParent());
            elementList.add(((OMNode) o).detach());
        } else if (o instanceof List) {
            List oList = (List) o;
            if (oList.size() > 0) {
                resultContainer.setParent((((OMNode) oList.get(0)).getParent()));
            }
            for (Object elem : oList) {
                if (elem instanceof OMNode) {
                    elementList.add(((OMNode) elem).detach());
                }
            }
        }
        resultContainer.setDetachedElements(elementList);
        return resultContainer;
    }

    @Override
    public void init(SynapseEnvironment se) {
        synapseEnv = se;

        if (null != sequence) {
            sequence.init(se);
        } else if (null != sequenceRef) {
            SequenceMediator refferedSeq =
                    (SequenceMediator) se.getSynapseConfiguration().
                            getSequence(sequenceRef);

            if (refferedSeq == null || refferedSeq.isDynamic()) {
                se.addUnavailableArtifactRef(sequenceRef);
            }
        }
    }

    @Override
    public void destroy() {
        if (null != sequence) {
            sequence.destroy();
        } else if (null != sequenceRef) {
            SequenceMediator refferedSeq =
                    (SequenceMediator) synapseEnv.getSynapseConfiguration().
                            getSequence(sequenceRef);

            if (refferedSeq == null || refferedSeq.isDynamic()) {
                synapseEnv.removeUnavailableArtifactRef(sequenceRef);
            }
        }
    }

    /**
     * Result container for detached elements and parent
     */
    private class DetachedElementContainer {

        private OMContainer parent;
        private List<OMNode> detachedElements;

        public OMContainer getParent() {
            return parent;
        }

        public void setParent(OMContainer parent) {
            this.parent = parent;
        }

        public List<OMNode> getDetachedElements() {
            return detachedElements;
        }

        public void setDetachedElements(List<OMNode> detachedElements) {
            this.detachedElements = detachedElements;
        }
    }

    public SynapsePath getExpression() {
        return expression;
    }

    public void setExpression(SynapsePath expression) {
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
