/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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
import com.google.gson.JsonSyntaxException;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.OperationContext;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.ContinuationState;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
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
import org.apache.synapse.continuation.ReliantContinuationState;
import org.apache.synapse.continuation.SeqContinuationState;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.FlowContinuableMediator;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.eip.EIPConstants;
import org.apache.synapse.mediators.eip.SharedDataHolder;
import org.apache.synapse.mediators.eip.Target;
import org.apache.synapse.mediators.eip.aggregator.Aggregate;
import org.apache.synapse.transport.passthru.util.RelayUtils;
import org.apache.synapse.util.MessageHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Timer;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import static org.apache.synapse.SynapseConstants.XML_CONTENT_TYPE;
import static org.apache.synapse.transport.passthru.PassThroughConstants.JSON_CONTENT_TYPE;

public class ScatterGather extends AbstractMediator implements ManagedLifecycle, FlowContinuableMediator {

    public static final String JSON_TYPE = "JSON";
    public static final String XML_TYPE = "XML";
    private final Object lock = new Object();
    private final Map<String, Aggregate> activeAggregates = Collections.synchronizedMap(new HashMap<>());
    private String id;
    private List<Target> targets = new ArrayList<>();
    private long completionTimeoutMillis = 0;
    private Value maxMessagesToComplete;
    private Value minMessagesToComplete;
    private SynapsePath correlateExpression = null;
    private SynapsePath aggregationExpression = null;
    private boolean parallelExecution = true;
    private Integer statisticReportingIndex;
    private String contentType;
    private String rootElementName;
    private String resultTarget;
    private SynapseEnvironment synapseEnv;

    public ScatterGather() {

        id = String.valueOf(new Random().nextLong());
    }

    public void setParallelExecution(boolean parallelExecution) {

        this.parallelExecution = parallelExecution;
    }

    public boolean getParallelExecution() {

        return this.parallelExecution;
    }

    public String getId() {

        return id;
    }

    @Override
    public boolean mediate(MessageContext synCtx) {

        boolean aggregationResult = false;

        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Scatter Gather mediator");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        MessageContext orginalMessageContext = null;
        if (!isTargetBody()) {
            try {
                // Clone the original MessageContext and save it to continue the flow using it when the scatter gather
                // output is set to a variable
                orginalMessageContext = MessageHelper.cloneMessageContext(synCtx);
            } catch (AxisFault e) {
                handleException("Error cloning the message context", e, synCtx);
            }
        }

        synCtx.setProperty(EIPConstants.EIP_SHARED_DATA_HOLDER + "." + id, new SharedDataHolder(orginalMessageContext));
        Iterator<Target> iter = targets.iterator();
        int i = 0;
        while (iter.hasNext()) {
            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("Submitting " + (i + 1) + " of " + targets.size() +
                        " messages for " + (parallelExecution ? "parallel processing" : "sequential processing"));
            }

            MessageContext clonedMsgCtx = getClonedMessageContext(synCtx, i++, targets.size());
            ContinuationStackManager.addReliantContinuationState(clonedMsgCtx, i - 1, getMediatorPosition());
            boolean result = iter.next().mediate(clonedMsgCtx);
            if (!parallelExecution && result) {
                aggregationResult = aggregateMessages(clonedMsgCtx, synLog);
            }
        }
        OperationContext opCtx
                = ((Axis2MessageContext) synCtx).getAxis2MessageContext().getOperationContext();
        if (opCtx != null) {
            opCtx.setProperty(Constants.RESPONSE_WRITTEN, "SKIP");
        }
        synCtx.setProperty(StatisticsConstants.CONTINUE_STATISTICS_FLOW, true);
        return aggregationResult;
    }

    public void init(SynapseEnvironment synapseEnv) {

        this.synapseEnv = synapseEnv;
        for (Target target : targets) {
            ManagedLifecycle seq = target.getSequence();
            if (seq != null) {
                seq.init(synapseEnv);
            }
        }
        // Registering the mediator for enabling continuation
        synapseEnv.updateCallMediatorCount(true);
    }

    public void destroy() {

        for (Target target : targets) {
            ManagedLifecycle seq = target.getSequence();
            if (seq != null) {
                seq.destroy();
            }
        }
        // Unregistering the mediator for continuation
        synapseEnv.updateCallMediatorCount(false);
    }

    /**
     * Clone the provided message context as a new message, and set the aggregation ID and the message sequence count
     *
     * @param synCtx          - MessageContext which is subjected to the cloning
     * @param messageSequence - the position of this message of the cloned set
     * @param messageCount    - total of cloned copies
     * @return MessageContext the cloned message context
     */
    private MessageContext getClonedMessageContext(MessageContext synCtx, int messageSequence, int messageCount) {

        MessageContext newCtx = null;
        try {
            newCtx = MessageHelper.cloneMessageContext(synCtx);
            // Set isServerSide property in the cloned message context
            ((Axis2MessageContext) newCtx).getAxis2MessageContext().setServerSide(
                    ((Axis2MessageContext) synCtx).getAxis2MessageContext().isServerSide());
            // Set the SCATTER_MESSAGES property to the cloned message context which will be used by the MediatorWorker
            // to continue the mediation from the continuation state
            newCtx.setProperty(SynapseConstants.SCATTER_MESSAGES, true);
            newCtx.setProperty(EIPConstants.AGGREGATE_CORRELATION + "." + id, synCtx.getMessageID());
            newCtx.setProperty(EIPConstants.MESSAGE_SEQUENCE + "." + id, messageSequence +
                    EIPConstants.MESSAGE_SEQUENCE_DELEMITER + messageCount);
        } catch (AxisFault axisFault) {
            handleException("Error cloning the message context", axisFault, synCtx);
        }
        return newCtx;
    }

    public List<Target> getTargets() {

        return targets;
    }

    public void setTargets(List<Target> targets) {

        this.targets = targets;
    }

    public void addTarget(Target target) {

        this.targets.add(target);
    }

    public SynapsePath getAggregationExpression() {

        return aggregationExpression;
    }

    public void setAggregationExpression(SynapsePath aggregationExpression) {

        this.aggregationExpression = aggregationExpression;
    }

    @Override
    public boolean mediate(MessageContext synCtx, ContinuationState continuationState) {

        SynapseLog synLog = getLog(synCtx);

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Scatter Gather mediator : Mediating from ContinuationState");
        }

        boolean result;
        int subBranch = ((ReliantContinuationState) continuationState).getSubBranch();

        SequenceMediator branchSequence = targets.get(subBranch).getSequence();
        boolean isStatisticsEnabled = RuntimeStatisticCollector.isStatisticsEnabled();
        // If there are no children and the continuation was triggered from a mediator worker start aggregation
        // otherwise mediate through the sub branch sequence
        if (!continuationState.hasChild()) {
            if (ScatterGatherUtils.isContinuationTriggeredFromMediatorWorker(synCtx)) {
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
        if (result) {
            return aggregateMessages(synCtx, synLog);
        }
        return false;
    }

    private boolean aggregateMessages(MessageContext synCtx, SynapseLog synLog) {

        // If the mediation is completed, remove the child continuation state from the stack, so the aggregation
        // will continue the mediation from the parent continuation state
        ContinuationStackManager.removeReliantContinuationState(synCtx);

        Aggregate aggregate = null;
        String correlationIdName = EIPConstants.AGGREGATE_CORRELATION + "." + id;

        Object correlationID = synCtx.getProperty(correlationIdName);
        String correlation = (String) correlationID;
        synLog.traceOrDebug("Aggregating messages started for correlation : " + correlation);

        boolean isAggregationConditionMet = false;
        // When the target sequences are not content aware, the message builder wont get triggered.
        // Therefore, we need to build the message to do the aggregation.
        try {
            RelayUtils.buildMessage(((Axis2MessageContext) synCtx).getAxis2MessageContext());
        } catch (IOException | XMLStreamException e) {
            handleException("Error building the message", e, synCtx);
        }

        if (correlateExpression != null) {
            String expressionResult = correlateExpression.stringValueOf(synCtx);
            if ("true".equalsIgnoreCase(expressionResult)) {
                isAggregationConditionMet = true;
            }
        }
        if (correlateExpression == null || isAggregationConditionMet) {
            while (aggregate == null) {
                synchronized (lock) {
                    if (activeAggregates.containsKey(correlation)) {
                        aggregate = activeAggregates.get(correlation);
                        if (aggregate != null) {
                            if (!aggregate.getLock()) {
                                aggregate = null;
                            }
                        }
                    } else {
                        if (synLog.isTraceOrDebugEnabled()) {
                            synLog.traceOrDebug("Creating new Aggregator - " +
                                    (completionTimeoutMillis > 0 ? "expires in : "
                                            + (completionTimeoutMillis / 1000) + "secs" :
                                            "without expiry time"));
                        }
                        if (isAggregationCompleted(synCtx)) {
                            return false;
                        }

                        Double minMsg = -1.0;
                        if (minMessagesToComplete != null) {
                            minMsg = Double.parseDouble(minMessagesToComplete.evaluateValue(synCtx));
                        }
                        Double maxMsg = -1.0;
                        if (maxMessagesToComplete != null) {
                            maxMsg = Double.parseDouble(maxMessagesToComplete.evaluateValue(synCtx));
                        }
                        aggregate = new Aggregate(
                                synCtx.getEnvironment(),
                                correlation,
                                completionTimeoutMillis,
                                minMsg.intValue(),
                                maxMsg.intValue(), this, synCtx.getFaultStack().peek());

                        if (completionTimeoutMillis > 0) {
                            synchronized (aggregate) {
                                if (!aggregate.isCompleted()) {
                                    try {
                                        synCtx.getConfiguration().getSynapseTimer().
                                                schedule(aggregate, completionTimeoutMillis);
                                    } catch (IllegalStateException e) {
                                        log.warn("Synapse timer already cancelled. Resetting Synapse timer");
                                        synCtx.getConfiguration().setSynapseTimer(new Timer(true));
                                        synCtx.getConfiguration().getSynapseTimer().
                                                schedule(aggregate, completionTimeoutMillis);
                                    }
                                }
                            }
                        }
                        aggregate.getLock();
                        activeAggregates.put(correlation, aggregate);
                    }
                }
            }
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

        Object aggregateTimeoutHolderObj = synCtx.getProperty(EIPConstants.EIP_SHARED_DATA_HOLDER + "." + id);

        if (aggregateTimeoutHolderObj != null) {
            SharedDataHolder sharedDataHolder = (SharedDataHolder) aggregateTimeoutHolderObj;
            if (sharedDataHolder.isAggregationCompleted()) {
                log.debug("Received a response for already completed Aggregate");
                return true;
            }
        }
        return false;
    }

    public boolean completeAggregate(Aggregate aggregate) {

        boolean markedCompletedNow = false;
        boolean wasComplete = aggregate.isCompleted();
        if (wasComplete) {
            return false;
        }
        log.debug("Aggregation completed or timed out");

        // cancel the timer
        synchronized (this) {
            if (!aggregate.isCompleted()) {
                aggregate.cancel();
                aggregate.setCompleted(true);

                MessageContext lastMessage = aggregate.getLastMessage();
                if (lastMessage != null) {
                    Object aggregateTimeoutHolderObj =
                            lastMessage.getProperty(EIPConstants.EIP_SHARED_DATA_HOLDER + "." + id);

                    if (aggregateTimeoutHolderObj != null) {
                        SharedDataHolder sharedDataHolder = (SharedDataHolder) aggregateTimeoutHolderObj;
                        sharedDataHolder.markAggregationCompletion();
                    }
                }
                markedCompletedNow = true;
            }
        }

        if (!markedCompletedNow) {
            return false;
        }

        if (isTargetBody()) {
            MessageContext newSynCtx = getAggregatedMessage(aggregate);
            return processAggregation(newSynCtx, aggregate);
        } else {
            MessageContext originalMessageContext = getOriginalMessageContext(aggregate);
            if (originalMessageContext != null) {
                setAggregatedMessageAsVariable(originalMessageContext, aggregate);
                return processAggregation(originalMessageContext, aggregate);
            } else {
                handleException(aggregate, "Error retrieving the original message context", null, aggregate.getLastMessage());
                return false;
            }
        }
    }

    private boolean processAggregation(MessageContext messageContext, Aggregate aggregate) {

        if (messageContext == null) {
            log.warn("An aggregation of messages timed out with no aggregated messages", null);
            return false;
        }
        aggregate.clear();
        activeAggregates.remove(aggregate.getCorrelation());

        if (isTargetBody()) {
            // Set content type to the aggregated message
            setContentType(messageContext);
        } else {
            // Update the continuation state to current mediator position as we are using the original message context
            ContinuationStackManager.updateSeqContinuationState(messageContext, getMediatorPosition());
        }
        messageContext.setProperty(StatisticsConstants.CONTINUE_STATISTICS_FLOW, true);

        if (RuntimeStatisticCollector.isStatisticsEnabled()) {
            CloseEventCollector.closeEntryEvent(messageContext, getMediatorName(), ComponentType.MEDIATOR,
                    statisticReportingIndex, isContentAltering());
        }

        getLog(messageContext).traceOrDebug("End : Scatter Gather mediator");
        boolean result = false;
        do {
            SeqContinuationState seqContinuationState =
                    (SeqContinuationState) ContinuationStackManager.peakContinuationStateStack(messageContext);
            if (seqContinuationState != null) {
                SequenceMediator sequenceMediator = ContinuationStackManager.retrieveSequence(messageContext, seqContinuationState);
                result = sequenceMediator.mediate(messageContext, seqContinuationState);
                if (RuntimeStatisticCollector.isStatisticsEnabled()) {
                    sequenceMediator.reportCloseStatistics(messageContext, null);
                }
            } else {
                break;
            }
        } while (result && !messageContext.getContinuationStateStack().isEmpty());
        CloseEventCollector.closeEventsAfterScatterGather(messageContext);
        return result;
    }

    /**
     * Return the original message context using the SharedDataHolder.
     *
     * @param aggregate Aggregate object
     * @return original message context
     */
    private MessageContext getOriginalMessageContext(Aggregate aggregate) {

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

    private void setContentType(MessageContext synCtx) {

        org.apache.axis2.context.MessageContext a2mc = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        if (Objects.equals(contentType, JSON_TYPE)) {
            a2mc.setProperty(Constants.Configuration.MESSAGE_TYPE, JSON_CONTENT_TYPE);
            a2mc.setProperty(Constants.Configuration.CONTENT_TYPE, JSON_CONTENT_TYPE);
            setContentTypeHeader(JSON_CONTENT_TYPE, a2mc);
        } else {
            a2mc.setProperty(Constants.Configuration.MESSAGE_TYPE, XML_CONTENT_TYPE);
            a2mc.setProperty(Constants.Configuration.CONTENT_TYPE, XML_CONTENT_TYPE);
            setContentTypeHeader(XML_CONTENT_TYPE, a2mc);
        }
        a2mc.removeProperty("NO_ENTITY_BODY");
    }

    private void setContentTypeHeader(Object resultValue, org.apache.axis2.context.MessageContext axis2MessageCtx) {

        axis2MessageCtx.setProperty(org.apache.axis2.Constants.Configuration.CONTENT_TYPE, resultValue);
        Object o = axis2MessageCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        Map headers = (Map) o;
        if (headers != null) {
            headers.put(HTTP.CONTENT_TYPE, resultValue);
        }
    }

    private void setAggregatedMessageAsVariable(MessageContext originalMessageContext, Aggregate aggregate) {

        Object variable = null;
        if (Objects.equals(contentType, JSON_TYPE)) {
            log.debug("Merging aggregated JSON responses to variable");
            variable = new JsonArray();
            setJSONResultToVariable((JsonArray) variable, aggregate);
        } else if (Objects.equals(contentType, XML_TYPE)) {
            log.debug("Merging aggregated XML responses to variable");
            variable = OMAbstractFactory.getOMFactory().createOMElement(new QName(rootElementName));
            setXMLResultToRootOMElement((OMElement) variable, aggregate);
        } else {
            handleException(aggregate, "Error merging aggregation results to variable : " + resultTarget +
                    " unknown content type : " + contentType, null, originalMessageContext);
        }
        originalMessageContext.setVariable(resultTarget, variable);
        StatisticDataCollectionHelper.collectAggregatedParents(aggregate.getMessages(), originalMessageContext);
    }

    private void setJSONResultToVariable(JsonArray variable, Aggregate aggregate) {

        for (MessageContext synCtx : aggregate.getMessages()) {
            Object evaluatedResult = aggregationExpression.objectValueOf(synCtx);
            variable.add((JsonElement) evaluatedResult);
        }
    }

    private void setXMLResultToRootOMElement(OMElement element, Aggregate aggregate) {

        try {
            for (MessageContext synCtx : aggregate.getMessages()) {
                OMElement cloneResult = AXIOMUtil.stringToOM(aggregationExpression.stringValueOf(synCtx));
                cloneResult.buildWithAttachments();
                element.addChild(cloneResult);
            }
        } catch (XMLStreamException e) {
            handleException(aggregate, "Error reading XML element: " + aggregationExpression.toString(), e,
                    aggregate.getLastMessage());
        }
    }

    private MessageContext getAggregatedMessage(Aggregate aggregate) {

        MessageContext newCtx = null;
        if (Objects.equals(contentType, JSON_TYPE)) {
            JsonArray jsonArray = new JsonArray();
            log.debug("Merging aggregated JSON responses to body");
            for (MessageContext synCtx : aggregate.getMessages()) {
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Merging message : " + synCtx.getEnvelope() + " using expression : " +
                                aggregationExpression);
                    }
                    Object evaluatedResult = aggregationExpression.objectValueOf(synCtx);
                    if (evaluatedResult instanceof JsonElement) {
                        jsonArray.add((JsonElement) evaluatedResult);
                    } else {
                        jsonArray.add(evaluatedResult.toString());
                    }
                } catch (SynapseException e) {
                    handleException(aggregate, "Error evaluating expression: " + aggregationExpression.toString(), e, synCtx);
                } catch (JsonSyntaxException e) {
                    handleException(aggregate, "Error reading JSON element: " + aggregationExpression.toString(), e, synCtx);
                }
            }
            // setting the new JSON payload to the messageContext
            try {
                newCtx = MessageHelper.cloneMessageContext(aggregate.getLastMessage(), false, false, true);
                SOAPEnvelope newEnvelope = createNewSoapEnvelope(aggregate.getLastMessage().getEnvelope());
                newCtx.setEnvelope(newEnvelope);
                JsonUtil.getNewJsonPayload(((Axis2MessageContext) newCtx).getAxis2MessageContext(), new
                        ByteArrayInputStream(jsonArray.toString().getBytes()), true, true);
            } catch (AxisFault axisFault) {
                handleException(aggregate, "Error occurred while setting the new JSON payload to the message context",
                        axisFault, newCtx);
            }
        } else if (Objects.equals(contentType, XML_TYPE)) {
            log.debug("Merging aggregated XML responses to body");
            OMElement rootElement = OMAbstractFactory.getOMFactory().createOMElement(new QName(rootElementName));
            setXMLResultToRootOMElement(rootElement, aggregate);
            try {
                newCtx = MessageHelper.cloneMessageContext(aggregate.getLastMessage(), false, false, true);
                SOAPEnvelope newEnvelope = createNewSoapEnvelope(aggregate.getLastMessage().getEnvelope());
                newEnvelope.getBody().addChild(rootElement);
                newCtx.setEnvelope(newEnvelope);
            } catch (AxisFault axisFault) {
                handleException(aggregate, "Error creating a copy of the message", axisFault, aggregate.getLastMessage());
            }
            // Removing the JSON stream after aggregated using XML path.
            // This will fix inconsistent behaviour in logging the payload.
            ((Axis2MessageContext) newCtx).getAxis2MessageContext()
                    .removeProperty(org.apache.synapse.commons.json.Constants.ORG_APACHE_SYNAPSE_COMMONS_JSON_JSON_INPUT_STREAM);
        } else {
            handleException(aggregate, "Error aggregating results. Unknown content type : " + contentType, null,
                    aggregate.getLastMessage());
        }
        StatisticDataCollectionHelper.collectAggregatedParents(aggregate.getMessages(), newCtx);
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

    public SynapsePath getCorrelateExpression() {

        return correlateExpression;
    }

    public void setCorrelateExpression(SynapsePath correlateExpression) {

        this.correlateExpression = correlateExpression;
    }

    public long getCompletionTimeoutMillis() {

        return completionTimeoutMillis;
    }

    public void setCompletionTimeoutMillis(long completionTimeoutMillis) {

        this.completionTimeoutMillis = completionTimeoutMillis;
    }

    public Value getMinMessagesToComplete() {

        return minMessagesToComplete;
    }

    public void setMinMessagesToComplete(Value minMessagesToComplete) {

        this.minMessagesToComplete = minMessagesToComplete;
    }

    public Value getMaxMessagesToComplete() {

        return maxMessagesToComplete;
    }

    public void setMaxMessagesToComplete(Value maxMessagesToComplete) {

        this.maxMessagesToComplete = maxMessagesToComplete;
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
        for (Target target : targets) {
            target.setStatisticIdForMediators(holder);
        }

        StatisticIdentityGenerator.reportingFlowContinuableEndEvent(sequenceId, ComponentType.MEDIATOR, holder);
    }

    @Override
    public boolean isContentAltering() {

        return true;
    }

    private void handleException(Aggregate aggregate, String msg, Exception exception, MessageContext msgContext) {

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

    public String getRootElementName() {

        return rootElementName;
    }

    public void setRootElementName(String rootElementName) {

        this.rootElementName = rootElementName;
    }

    private boolean isTargetBody() {

        return "body".equalsIgnoreCase(resultTarget);
    }
}
