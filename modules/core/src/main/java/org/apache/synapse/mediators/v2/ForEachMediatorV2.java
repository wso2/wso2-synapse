/*
 *  Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.mediators.v2;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMContainer;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.OperationContext;
import org.apache.synapse.ContinuationState;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.StatisticIdentityGenerator;
import org.apache.synapse.aspects.flow.statistics.collectors.CloseEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.OpenEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.aspects.flow.statistics.data.artifact.ArtifactHolder;
import org.apache.synapse.aspects.flow.statistics.util.StatisticDataCollectionHelper;
import org.apache.synapse.aspects.flow.statistics.util.StatisticsConstants;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.config.xml.SynapsePath;
import org.apache.synapse.continuation.ContinuationStackManager;
import org.apache.synapse.continuation.SeqContinuationState;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.FlowContinuableMediator;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.eip.EIPConstants;
import org.apache.synapse.mediators.eip.SharedDataHolder;
import org.apache.synapse.mediators.eip.Target;
import org.apache.synapse.mediators.eip.aggregator.ForEachAggregate;
import org.apache.synapse.transport.passthru.util.RelayUtils;
import org.apache.synapse.util.MessageHelper;
import org.apache.synapse.util.synapse.expression.constants.ExpressionConstants;
import org.apache.synapse.util.xpath.SynapseExpression;
import org.apache.synapse.util.xpath.SynapseExpressionUtils;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

public class ForEachMediatorV2 extends AbstractMediator implements ManagedLifecycle, FlowContinuableMediator {

    public static final String VARIABLE_DOT = ExpressionConstants.VARIABLES + ".";
    public static final String JSON_TYPE = "JSON";
    public static final String XML_TYPE = "XML";
    private final Object lock = new Object();
    private final Map<String, ForEachAggregate> activeAggregates = Collections.synchronizedMap(new HashMap<>());
    private final String id;
    private SynapsePath collectionExpression = null;
    private Target target;
    private boolean parallelExecution = true;
    private Integer statisticReportingIndex;
    private String contentType;
    private String resultTarget = null;
    private String counterVariableName = null;
    private boolean continueWithoutAggregation = false;
    private SynapseEnvironment synapseEnv;

    public ForEachMediatorV2() {

        id = String.valueOf(new Random().nextLong());
    }

    @Override
    public boolean mediate(MessageContext synCtx) {

        boolean aggregationResult = false;

        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Foreach mediator");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        try {
            if (!continueWithoutAggregation) {
                // Clone the original MessageContext and save it to continue the flow after aggregation
                MessageContext clonedMessageContext = MessageHelper.cloneMessageContext(synCtx);
                synCtx.setProperty(EIPConstants.EIP_SHARED_DATA_HOLDER + "." + id, new SharedDataHolder(clonedMessageContext));
            }

            Object collection = collectionExpression.objectValueOf(synCtx);

            if (collection instanceof JsonArray) {
                int msgNumber = 0;
                JsonArray list = (JsonArray) collection;
                if (list.isEmpty()) {
                    log.info("No elements found for the expression : " + collectionExpression);
                    return true;
                }
                int msgCount = list.size();
                for (Object item : list) {
                    MessageContext iteratedMsgCtx = getIteratedMessage(synCtx, msgNumber++, msgCount, item);
                    ContinuationStackManager.addReliantContinuationState(iteratedMsgCtx, 0, getMediatorPosition());
                    boolean result = target.mediate(iteratedMsgCtx);
                    if (!parallelExecution && result && !continueWithoutAggregation) {
                        aggregationResult = aggregateMessages(iteratedMsgCtx, synLog);
                    }
                }
            } else if (collection instanceof List) {
                int msgNumber = 0;
                List list = (List) collection;
                if (list.isEmpty()) {
                    log.info("No elements found for the expression : " + collectionExpression);
                    return true;
                }
                int msgCount = list.size();
                for (Object item : list) {
                    MessageContext iteratedMsgCtx = getIteratedMessage(synCtx, msgNumber++, msgCount, item);
                    ContinuationStackManager.addReliantContinuationState(iteratedMsgCtx, 0, getMediatorPosition());
                    boolean result = target.mediate(iteratedMsgCtx);
                    if (!parallelExecution && result && !continueWithoutAggregation) {
                        aggregationResult = aggregateMessages(iteratedMsgCtx, synLog);
                    }
                }
            } else {
                handleException("Expression " + collectionExpression + " did not resolve to a valid array", synCtx);
            }
        } catch (AxisFault e) {
            handleException("Error executing Foreach mediator", e, synCtx);
        }
        if (continueWithoutAggregation) {
            return true;
        } else {
            OperationContext opCtx
                    = ((Axis2MessageContext) synCtx).getAxis2MessageContext().getOperationContext();
            if (opCtx != null) {
                opCtx.setProperty(Constants.RESPONSE_WRITTEN, "SKIP");
            }
            synCtx.setProperty(StatisticsConstants.CONTINUE_STATISTICS_FLOW, true);
            return aggregationResult;
        }
    }

    private MessageContext getIteratedMessage(MessageContext synCtx, int msgNumber, int msgCount, Object node) throws AxisFault {

        MessageContext newCtx = MessageHelper.cloneMessageContext(synCtx, false, false);
        // Adding an empty envelope since JsonUtil.getNewJsonPayload requires an envelope
        SOAPEnvelope newEnvelope = createNewSoapEnvelope(synCtx.getEnvelope());
        newCtx.setEnvelope(newEnvelope);
        if (node instanceof OMNode) {
            if (newEnvelope.getBody() != null) {
                newEnvelope.getBody().addChild((OMNode) node);
            }
        } else {
            JsonUtil.getNewJsonPayload(((Axis2MessageContext) newCtx).getAxis2MessageContext(), node.toString(), true,
                    true);
        }
        newCtx.setProperty(EIPConstants.AGGREGATE_CORRELATION + "." + id, synCtx.getMessageID());
        newCtx.setProperty(EIPConstants.MESSAGE_SEQUENCE + "." + id, msgNumber + EIPConstants.MESSAGE_SEQUENCE_DELEMITER + msgCount);
        // Set the SCATTER_MESSAGES property to the cloned message context which will be used by the MediatorWorker
        // to continue the mediation from the continuation state
        newCtx.setProperty(SynapseConstants.SCATTER_MESSAGES, true);
        if (!parallelExecution && counterVariableName != null) {
            newCtx.setVariable(counterVariableName, msgNumber);
        }
        ((Axis2MessageContext) newCtx).getAxis2MessageContext().setServerSide(
                ((Axis2MessageContext) synCtx).getAxis2MessageContext().isServerSide());
        return newCtx;
    }

    private SOAPEnvelope createNewSoapEnvelope(SOAPEnvelope envelope) {

        SOAPFactory fac;
        if (SOAP11Constants.SOAP_ENVELOPE_NAMESPACE_URI.equals(envelope.getBody().getNamespace().getNamespaceURI())) {
            fac = OMAbstractFactory.getSOAP11Factory();
        } else {
            fac = OMAbstractFactory.getSOAP12Factory();
        }
        return fac.getDefaultEnvelope();
    }

    public void init(SynapseEnvironment synapseEnv) {

        this.synapseEnv = synapseEnv;
        ManagedLifecycle seq = target.getSequence();
        if (seq != null) {
            seq.init(synapseEnv);
        }
        // Registering the mediator for enabling continuation
        synapseEnv.updateCallMediatorCount(true);
    }

    public void destroy() {

        ManagedLifecycle seq = target.getSequence();
        if (seq != null) {
            seq.destroy();
        }
        // Unregistering the mediator for continuation
        synapseEnv.updateCallMediatorCount(false);
    }

    public Target getTarget() {

        return target;
    }

    public void setTarget(Target target) {

        this.target = target;
    }

    @Override
    public boolean mediate(MessageContext synCtx, ContinuationState continuationState) {

        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Foreach mediator : Mediating from ContinuationState");
        }

        boolean result;
        SequenceMediator branchSequence = target.getSequence();
        boolean isStatisticsEnabled = RuntimeStatisticCollector.isStatisticsEnabled();
        // If there are no children and the continuation was triggered from a mediator worker start aggregation
        // otherwise mediate through the sub branch sequence
        if (!continuationState.hasChild()) {
            if (Utils.isContinuationTriggeredFromMediatorWorker(synCtx)) {
                synLog.traceOrDebug("Continuation is triggered from a mediator worker");
                result = true;
            } else {
                synLog.traceOrDebug("Continuation is triggered from a callback, mediating through the sub branch sequence");
                result = branchSequence.mediate(synCtx, continuationState.getPosition() + 1);
            }
        } else {
            synLog.traceOrDebug("Continuation is triggered from a callback, mediating through the child continuation state");
            FlowContinuableMediator mediator =
                    (FlowContinuableMediator) branchSequence.getChild(continuationState.getPosition());

            result = mediator.mediate(synCtx, continuationState.getChildContState());
            if (isStatisticsEnabled) {
                ((Mediator) mediator).reportCloseStatistics(synCtx, null);
            }
        }
        // If this a continue without aggregation scenario, return false to end the mediation
        if (continueWithoutAggregation) {
            return false;
        }
        if (result) {
            return aggregateMessages(synCtx, synLog);
        }
        return false;
    }

    private boolean aggregateMessages(MessageContext synCtx, SynapseLog synLog) {

        // If the mediation is completed, remove the child continuation state from the stack, so the aggregation
        // will continue the mediation from the parent continuation state
        ContinuationStackManager.removeReliantContinuationState(synCtx);

        ForEachAggregate aggregate = null;
        String correlationIdName = EIPConstants.AGGREGATE_CORRELATION + "." + id;

        Object correlationID = synCtx.getProperty(correlationIdName);
        String correlation;

        // When the target sequences are not content aware, the message builder won't get triggered.
        // Therefore, we need to build the message to do the aggregation.
        try {
            RelayUtils.buildMessage(((Axis2MessageContext) synCtx).getAxis2MessageContext());
        } catch (IOException | XMLStreamException e) {
            handleException("Error building the message", e, synCtx);
        }
        if (correlationID instanceof String) {
            correlation = (String) correlationID;
            synLog.traceOrDebug("Aggregating messages started for correlation : " + correlation);
            while (aggregate == null) {
                synchronized (lock) {
                    if (activeAggregates.containsKey(correlation)) {
                        aggregate = activeAggregates.get(correlation);
                        if (aggregate != null) {
                            if (!aggregate.getLock()) {
                                aggregate = null;
                            }
                        } else {
                            break;
                        }
                    } else {
                        if (isAggregationCompleted(synCtx)) {
                            return false;
                        }
                        synLog.traceOrDebug("Creating new ForeachAggregator");
                        aggregate = new ForEachAggregate(correlation, id);
                        aggregate.getLock();
                        activeAggregates.put(correlation, aggregate);
                    }
                }
            }
        } else {
            synLog.traceOrDebug("Unable to find aggregation correlation property");
            return false;
        }
        // if there is an aggregate continue on aggregation
        if (aggregate != null) {
            boolean collected = aggregate.addMessage(synCtx);
            if (synLog.isTraceOrDebugEnabled()) {
                if (collected) {
                    synLog.traceOrDebug("Collected a message during aggregation");
                    if (synLog.isTraceTraceEnabled()) {
                        synLog.traceTrace("Collected message : " + synCtx);
                    }
                }
            }
            if (aggregate.isComplete(synLog)) {
                synLog.traceOrDebug("Aggregation completed");
                return completeAggregate(aggregate);
            } else {
                aggregate.releaseLock();
            }
        } else {
            synLog.traceOrDebug("Unable to find an aggregate for this message - skip");
        }
        return false;
    }

    private boolean isAggregationCompleted(MessageContext synCtx) {

        Object aggregateHolderObj = synCtx.getProperty(EIPConstants.EIP_SHARED_DATA_HOLDER + "." + id);

        if (aggregateHolderObj != null) {
            SharedDataHolder sharedDataHolder = (SharedDataHolder) aggregateHolderObj;
            if (sharedDataHolder.isAggregationCompleted()) {
                if (log.isDebugEnabled()) {
                    log.debug("Received a response for already completed Aggregate");
                }
                return true;
            }
        }
        return false;
    }

    private boolean completeAggregate(ForEachAggregate aggregate) {

        boolean markedCompletedNow = false;
        boolean wasComplete = aggregate.isCompleted();
        if (wasComplete) {
            return false;
        }
        if (log.isDebugEnabled()) {
            log.debug("Aggregation completed for the correlation : " + aggregate.getCorrelation() +
                    " in the ForEach mediator");
        }

        synchronized (this) {
            if (!aggregate.isCompleted()) {
                aggregate.setCompleted(true);
                MessageContext lastMessage = aggregate.getLastMessage();
                if (lastMessage != null) {
                    Object aggregateHolderObj = lastMessage.getProperty(EIPConstants.EIP_SHARED_DATA_HOLDER + "." + id);
                    if (aggregateHolderObj != null) {
                        SharedDataHolder sharedDataHolder = (SharedDataHolder) aggregateHolderObj;
                        sharedDataHolder.markAggregationCompletion();
                    }
                }
                markedCompletedNow = true;
            }
        }

        if (!markedCompletedNow) {
            return false;
        }

        MessageContext originalMessageContext = getOriginalMessageContext(aggregate);

        if (originalMessageContext != null) {
            if (updateOriginalContent()) {
                updateOriginalPayload(originalMessageContext, aggregate);
            } else {
                setAggregatedMessageAsVariable(originalMessageContext, aggregate);
            }
            StatisticDataCollectionHelper.collectAggregatedParents(aggregate.getMessages(), originalMessageContext);
            aggregate.clear();
            activeAggregates.remove(aggregate.getCorrelation());
            // Update the continuation state to current mediator position as we are using the original message context
            ContinuationStackManager.updateSeqContinuationState(originalMessageContext, getMediatorPosition());

            getLog(originalMessageContext).traceOrDebug("End : Foreach mediator");
            boolean result = false;

            // Set CONTINUE_STATISTICS_FLOW to avoid mark event collection as finished before the aggregation is completed
            originalMessageContext.setProperty(StatisticsConstants.CONTINUE_STATISTICS_FLOW, true);
            if (RuntimeStatisticCollector.isStatisticsEnabled()) {
                CloseEventCollector.closeEntryEvent(originalMessageContext, getMediatorName(), ComponentType.MEDIATOR,
                        statisticReportingIndex, isContentAltering());
            }
            do {
                SeqContinuationState seqContinuationState =
                        (SeqContinuationState) ContinuationStackManager.peakContinuationStateStack(originalMessageContext);
                if (seqContinuationState != null) {
                    SequenceMediator sequenceMediator = ContinuationStackManager.retrieveSequence(originalMessageContext, seqContinuationState);
                    result = sequenceMediator.mediate(originalMessageContext, seqContinuationState);
                    if (RuntimeStatisticCollector.isStatisticsEnabled()) {
                        sequenceMediator.reportCloseStatistics(originalMessageContext, null);
                    }
                } else {
                    break;
                }
            } while (result && !originalMessageContext.getContinuationStateStack().isEmpty());
            CloseEventCollector.closeEventsAfterScatterGather(originalMessageContext);
            return result;
        } else {
            handleException(aggregate, "Error retrieving the original message context", null, aggregate.getLastMessage());
            return false;
        }
    }

    private MessageContext getOriginalMessageContext(ForEachAggregate aggregate) {

        MessageContext lastMessage = aggregate.getLastMessage();
        if (lastMessage != null) {
            Object aggregateHolderObj = lastMessage.getProperty(EIPConstants.EIP_SHARED_DATA_HOLDER + "." + id);
            if (aggregateHolderObj != null) {
                SharedDataHolder sharedDataHolder = (SharedDataHolder) aggregateHolderObj;
                return sharedDataHolder.getSynCtx();
            }
        }
        return null;
    }

    private void setAggregatedMessageAsVariable(MessageContext originalMessageContext, ForEachAggregate aggregate) {

        Object variable = null;
        if (Objects.equals(contentType, JSON_TYPE)) {
            log.debug("Merging aggregated JSON responses to variable");
            // fill JSON array with null
            variable = new JsonArray();
            Collections.nCopies(aggregate.getMessages().size(), JsonNull.INSTANCE).forEach(((JsonArray) variable)::add);
            setJSONResultToVariable((JsonArray) variable, aggregate);
        } else if (Objects.equals(contentType, XML_TYPE)) {
            log.debug("Merging aggregated XML responses to variable");
            variable = OMAbstractFactory.getOMFactory().createOMElement(new QName(resultTarget));
            setXMLResultToVariable((OMElement) variable, aggregate);
        } else {
            handleException(aggregate, "Error merging aggregation results to variable : " + resultTarget +
                    " unknown content type : " + contentType, null, originalMessageContext);
        }
        originalMessageContext.setVariable(resultTarget, variable);
    }

    private void setJSONResultToVariable(JsonArray variable, ForEachAggregate aggregate) {

        for (MessageContext synCtx : aggregate.getMessages()) {
            Object prop = synCtx.getProperty(EIPConstants.MESSAGE_SEQUENCE + "." + id);
            String[] msgSequence = prop.toString().split(EIPConstants.MESSAGE_SEQUENCE_DELEMITER);
            JsonElement jsonElement = null;
            try {
                Object result = new SynapseExpression(ExpressionConstants.PAYLOAD).objectValueOf(synCtx);
                if (result instanceof JsonElement) {
                    jsonElement = (JsonElement) result;
                }
            } catch (JaxenException e) {
                log.warn("Error extracting the JSON payload for iteration : " + msgSequence[0]);
            }
            variable.set(Integer.parseInt(msgSequence[0]), jsonElement);
        }
    }

    private void setXMLResultToVariable(OMElement variable, ForEachAggregate aggregate) {

        List<OMNode> list = getXMLPayloadsAsList(aggregate);
        for (OMNode node : list) {
            variable.addChild(node);
        }
    }

    private void updateOriginalPayload(MessageContext originalMessageContext, ForEachAggregate aggregate) {

        Object collection = this.collectionExpression.objectValueOf(originalMessageContext);

        if (collection instanceof JsonArray) {
            try {
                log.debug("Updating original JSON array with iteration results");
                //Read the complete JSON payload from the synCtx
                String jsonPayload = JsonUtil.jsonPayloadToString(((Axis2MessageContext) originalMessageContext).getAxis2MessageContext());
                DocumentContext parsedJsonPayload = JsonPath.parse(jsonPayload);
                JsonArray jsonArray = (JsonArray) collection;
                for (MessageContext synCtx : aggregate.getMessages()) {
                    Object prop = synCtx.getProperty(EIPConstants.MESSAGE_SEQUENCE + "." + id);
                    String[] msgSequence = prop.toString().split(EIPConstants.MESSAGE_SEQUENCE_DELEMITER);
                    JsonElement jsonElement = null;
                    Object result = new SynapseExpression(ExpressionConstants.PAYLOAD).objectValueOf(synCtx);
                    if (result instanceof JsonElement) {
                        jsonElement = (JsonElement) result;
                    }
                    jsonArray.set(Integer.parseInt(msgSequence[0]), jsonElement);
                }
                JsonPath jsonPath = getJsonPathFromExpression(this.collectionExpression.getExpression());
                JsonElement jsonPayloadElement;
                if (isWholeContent(jsonPath)) {
                    jsonPayloadElement = jsonArray;
                } else {
                    jsonPayloadElement = parsedJsonPayload.set(jsonPath, jsonArray).json();
                }
                if (isCollectionReferencedByVariable(this.collectionExpression)) {
                    String variableName = getVariableName(this.collectionExpression);
                    originalMessageContext.setVariable(variableName, jsonPayloadElement);
                } else {
                    JsonUtil.getNewJsonPayload(((Axis2MessageContext) originalMessageContext).getAxis2MessageContext(),
                            jsonPayloadElement.toString(), true, true);
                }
            } catch (AxisFault axisFault) {
                handleException("Error updating the json stream after foreach transformation", axisFault, originalMessageContext);
            } catch (JaxenException e) {
                handleException("Error extracting the JSON payload after iteration", e, originalMessageContext);
            }
        } else if (collection instanceof List) {
            try {
                log.debug("Updating original XML array with iteration results");
                List<OMNode> results = getXMLPayloadsAsList(aggregate);
                if (isCollectionReferencedByVariable(this.collectionExpression)) {
                    String variableName = getVariableName(this.collectionExpression);
                    updateXMLCollection(originalMessageContext.getVariable(variableName), results);
                } else if (SynapseExpressionUtils.isVariableXPathExpression(this.collectionExpression.getExpression())) {
                    // Collection is referenced by a variable and xpath as "${xpath('someXPathExpression', 'someVariable')}"
                    String variableName = SynapseExpressionUtils.
                            getVariableFromVariableXPathExpression(this.collectionExpression.getExpression());
                    String xpath = SynapseExpressionUtils.
                            getXPathFromVariableXPathExpression(this.collectionExpression.getExpression());
                    SynapseXPath synapseXPath = new SynapseXPath(xpath);
                    Object oldCollectionNodes = synapseXPath.evaluate(originalMessageContext.getVariable(variableName));
                    updateXMLCollection(oldCollectionNodes, results);
                } else {
                    // Extract the xpath value inside xpath() function from the expression
                    String xpath = this.collectionExpression.getExpression().
                            substring(7, this.collectionExpression.getExpression().length() - 2);
                    SynapseXPath synapseXPath = new SynapseXPath(xpath);
                    Object oldCollectionNodes = synapseXPath.evaluate(originalMessageContext);
                    updateXMLCollection(oldCollectionNodes, results);
                }
            } catch (JaxenException e) {
                handleException(aggregate, "Error updating the original XML array", e, originalMessageContext);
            }
        }
    }

    private List<OMNode> getXMLPayloadsAsList(ForEachAggregate aggregate) {

        List<OMNode> results = new ArrayList<>(Collections.nCopies(aggregate.getMessages().size(), null));
        for (MessageContext synCtx : aggregate.getMessages()) {
            Object prop = synCtx.getProperty(EIPConstants.MESSAGE_SEQUENCE + "." + id);
            String[] msgSequence = prop.toString().split(EIPConstants.MESSAGE_SEQUENCE_DELEMITER);
            results.set(Integer.parseInt(msgSequence[0]), synCtx.getEnvelope().getBody().getFirstElement());
        }
        return results;
    }

    private void updateXMLCollection(Object oldCollectionNodes, List<OMNode> results) {

        OMContainer parent = null;
        // This is an XML collection. Hence detach the elements from the original collection and attach the new elements
        if (oldCollectionNodes instanceof OMNode) {
            parent = ((OMNode) oldCollectionNodes).getParent();
            ((OMNode) oldCollectionNodes).detach();
        } else if (oldCollectionNodes instanceof List) {
            List oList = (List) oldCollectionNodes;
            if (!oList.isEmpty()) {
                parent = (((OMNode) oList.get(0)).getParent());
            }
            for (Object elem : oList) {
                if (elem instanceof OMNode) {
                    ((OMNode) elem).detach();
                }
            }
        }
        if (parent != null) {
            for (OMNode result : results) {
                parent.addChild(result);
            }
        }
    }

    @Override
    public Integer reportOpenStatistics(MessageContext messageContext, boolean isContentAltering) {

        statisticReportingIndex = OpenEventCollector.reportFlowContinuableEvent(messageContext, getMediatorName(),
                ComponentType.MEDIATOR, getAspectConfiguration(), isContentAltering() || isContentAltering);
        return statisticReportingIndex;
    }

    @Override
    public void reportCloseStatistics(MessageContext messageContext, Integer currentIndex) {

        // Do nothing here as the close event is reported in the completeAggregate method
    }

    @Override
    public void setComponentStatisticsId(ArtifactHolder holder) {

        if (getAspectConfiguration() == null) {
            configure(new AspectConfiguration(getMediatorName()));
        }
        String sequenceId =
                StatisticIdentityGenerator.getIdForFlowContinuableMediator(getMediatorName(), ComponentType.MEDIATOR, holder);
        getAspectConfiguration().setUniqueId(sequenceId);
        target.setStatisticIdForMediators(holder);
        StatisticIdentityGenerator.reportingFlowContinuableEndEvent(sequenceId, ComponentType.MEDIATOR, holder);
    }

    @Override
    public boolean isContentAltering() {

        return true;
    }

    private void handleException(ForEachAggregate aggregate, String msg, Exception exception, MessageContext msgContext) {

        aggregate.clear();
        activeAggregates.remove(aggregate.getCorrelation());
        if (exception != null) {
            super.handleException(msg, exception, msgContext);
        } else {
            super.handleException(msg, msgContext);
        }
    }

    public String getContentType() {

        return contentType;
    }

    public void setContentType(String contentType) {

        this.contentType = contentType;
    }

    public String getResultTarget() {

        return resultTarget;
    }

    public void setResultTarget(String resultTarget) {

        this.resultTarget = resultTarget;
    }

    public SynapsePath getCollectionExpression() {

        return collectionExpression;
    }

    public void setCollectionExpression(SynapsePath collectionExpression) {

        this.collectionExpression = collectionExpression;
    }

    public boolean getParallelExecution() {

        return this.parallelExecution;
    }

    public void setParallelExecution(boolean parallelExecution) {

        this.parallelExecution = parallelExecution;
    }

    private boolean updateOriginalContent() {

        return resultTarget == null;
    }

    public String getId() {

        return id;
    }

    private String getVariableName(SynapsePath expression) {

        return expression.getExpression().split("\\.")[1];
    }

    private boolean isCollectionReferencedByVariable(SynapsePath expression) {

        return expression.getExpression().startsWith(VARIABLE_DOT);
    }

    private JsonPath getJsonPathFromExpression(String expression) {

        String jsonPath = expression;
        if (jsonPath.startsWith(ExpressionConstants.PAYLOAD)) {
            jsonPath = jsonPath.replace(ExpressionConstants.PAYLOAD, ExpressionConstants.PAYLOAD_$);
        } else if (jsonPath.startsWith(VARIABLE_DOT)) {
            // Remove the "vars." prefix and variable name and replace it with "$" for JSON path
            jsonPath = expression.replaceAll(ExpressionConstants.VARIABLES + "\\.\\w+\\.(\\w+)", "\\$.$1")
                    .replaceAll(ExpressionConstants.VARIABLES + "\\.\\w+", "\\$");
        }
        return JsonPath.compile(jsonPath);
    }

    private boolean isWholeContent(JsonPath jsonPath) {

        return ExpressionConstants.PAYLOAD_$.equals(jsonPath.getPath().trim())
                || ExpressionConstants.PAYLOAD_$.equals(jsonPath.getPath().trim());
    }

    public String getCounterVariable() {

        return counterVariableName;
    }

    public void setCounterVariable(String counterVariableName) {

        this.counterVariableName = counterVariableName;
    }

    public void setContinueWithoutAggregation(boolean continueWithoutAggregation) {

        this.continueWithoutAggregation = continueWithoutAggregation;
    }

    public boolean isContinueWithoutAggregation() {

        return continueWithoutAggregation;
    }
}
