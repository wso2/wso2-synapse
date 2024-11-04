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
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.OperationContext;
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
import org.apache.synapse.mediators.eip.EIPUtils;
import org.apache.synapse.mediators.eip.SharedDataHolder;
import org.apache.synapse.mediators.eip.Target;
import org.apache.synapse.mediators.eip.aggregator.Aggregate;
import org.apache.synapse.transport.passthru.util.RelayUtils;
import org.apache.synapse.util.MessageHelper;
import org.apache.synapse.util.xpath.SynapseJsonPath;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import javax.xml.stream.XMLStreamException;

public class ScatterGather extends AbstractMediator implements ManagedLifecycle, FlowContinuableMediator {

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

        synCtx.setProperty(id != null ? EIPConstants.EIP_SHARED_DATA_HOLDER + "." + id :
                EIPConstants.EIP_SHARED_DATA_HOLDER, new SharedDataHolder());
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
        return aggregationResult;
    }

    public void init(SynapseEnvironment se) {

        for (Target target : targets) {
            ManagedLifecycle seq = target.getSequence();
            if (seq != null) {
                seq.init(se);
            }
        }
    }

    public void destroy() {

        for (Target target : targets) {
            ManagedLifecycle seq = target.getSequence();
            if (seq != null) {
                seq.destroy();
            }
        }
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
            if (id != null) {
                newCtx.setProperty(EIPConstants.AGGREGATE_CORRELATION + "." + id, synCtx.getMessageID());
                newCtx.setProperty(EIPConstants.MESSAGE_SEQUENCE + "." + id, messageSequence +
                        EIPConstants.MESSAGE_SEQUENCE_DELEMITER + messageCount);
            } else {
                newCtx.setProperty(EIPConstants.MESSAGE_SEQUENCE, messageSequence +
                        EIPConstants.MESSAGE_SEQUENCE_DELEMITER + messageCount);
            }
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
        // If the continuation is triggered from a mediator worker and has children, then mediate through the sub branch
        // otherwise start aggregation
        if (isContinuationTriggeredFromMediatorWorker(synCtx)) {
            if (continuationState.hasChild()) {
                int subBranch = ((ReliantContinuationState) continuationState.getChildContState()).getSubBranch();
                SequenceMediator branchSequence = targets.get(subBranch).getSequence();
                boolean isStatisticsEnabled = RuntimeStatisticCollector.isStatisticsEnabled();
                FlowContinuableMediator mediator =
                        (FlowContinuableMediator) branchSequence.getChild(continuationState.getChildContState().getPosition());

                result = mediator.mediate(synCtx, continuationState.getChildContState());
                if (isStatisticsEnabled) {
                    ((Mediator) mediator).reportCloseStatistics(synCtx, null);
                }
            } else {
                result = true;
            }
        } else {
            // If the continuation is triggered from a callback, continue the mediation from the continuation state
            int subBranch = ((ReliantContinuationState) continuationState).getSubBranch();

            SequenceMediator branchSequence = targets.get(subBranch).getSequence();
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
            // If the mediation is completed, remove the child continuation state from the stack, so the aggregation
            // will continue the mediation from the parent continuation state
            ContinuationStackManager.removeReliantContinuationState(synCtx);
        }
        if (result) {
            return aggregateMessages(synCtx, synLog);
        }
        return false;
    }

    private boolean aggregateMessages(MessageContext synCtx, SynapseLog synLog) {

        Aggregate aggregate = null;
        String correlationIdName = (id != null ? EIPConstants.AGGREGATE_CORRELATION + "." + id :
                EIPConstants.AGGREGATE_CORRELATION);

        Object correlationID = synCtx.getProperty(correlationIdName);
        String correlation;

        Object result = null;
        if (correlateExpression != null) {
            try {
                result = correlateExpression instanceof SynapseXPath ? correlateExpression.evaluate(synCtx) :
                        ((SynapseJsonPath) correlateExpression).evaluate(synCtx);
            } catch (JaxenException e) {
                handleException("Unable to execute the XPATH over the message", e, synCtx);
            }
            if (result instanceof List) {
                if (((List) result).isEmpty()) {
                    handleException("Failed to evaluate correlate expression: " + correlateExpression.toString(), synCtx);
                }
            }
            if (result instanceof Boolean) {
                if (!(Boolean) result) {
                    return true;
                }
            }
        }
        if (result != null) {
            while (aggregate == null) {
                synchronized (lock) {
                    if (activeAggregates.containsKey(correlateExpression.toString())) {
                        aggregate = activeAggregates.get(correlateExpression.toString());
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
                                correlateExpression.toString(),
                                completionTimeoutMillis,
                                minMsg.intValue(),
                                maxMsg.intValue(), this, synCtx.getFaultStack().peek());

                        if (completionTimeoutMillis > 0) {
                            synCtx.getConfiguration().getSynapseTimer().
                                    schedule(aggregate, completionTimeoutMillis);
                        }
                        aggregate.getLock();
                        activeAggregates.put(correlateExpression.toString(), aggregate);
                    }
                }
            }
        } else if (correlationID instanceof String) {
            correlation = (String) correlationID;
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
        } else {
            synLog.traceOrDebug("Unable to find aggregation correlation property");
            return true;
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
                boolean onCompleteSeqResult = completeAggregate(aggregate);
                synLog.traceOrDebug("End : Scatter Gather mediator");
                return onCompleteSeqResult;
            } else {
                aggregate.releaseLock();
            }
        } else {
            synLog.traceOrDebug("Unable to find an aggregate for this message - skip");
            return true;
        }
        return false;
    }

    private boolean isAggregationCompleted(MessageContext synCtx) {

        Object aggregateTimeoutHolderObj =
                synCtx.getProperty(id != null ? EIPConstants.EIP_SHARED_DATA_HOLDER + "." + id :
                        EIPConstants.EIP_SHARED_DATA_HOLDER);

        if (aggregateTimeoutHolderObj != null) {
            SharedDataHolder sharedDataHolder = (SharedDataHolder) aggregateTimeoutHolderObj;
            if (sharedDataHolder.isAggregationCompleted()) {
                if (log.isDebugEnabled()) {
                    log.debug("Received a response for already completed Aggregate");
                }
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

        if (log.isDebugEnabled()) {
            log.debug("Aggregation completed or timed out");
        }

        // cancel the timer
        synchronized (this) {
            if (!aggregate.isCompleted()) {
                aggregate.cancel();
                aggregate.setCompleted(true);

                MessageContext lastMessage = aggregate.getLastMessage();
                if (lastMessage != null) {
                    Object aggregateTimeoutHolderObj =
                            lastMessage.getProperty(id != null ? EIPConstants.EIP_SHARED_DATA_HOLDER + "." + id :
                                    EIPConstants.EIP_SHARED_DATA_HOLDER);

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

        MessageContext newSynCtx = getAggregatedMessage(aggregate);

        if (newSynCtx == null) {
            log.warn("An aggregation of messages timed out with no aggregated messages", null);
            return false;
        }
        aggregate.clear();
        activeAggregates.remove(aggregate.getCorrelation());
        newSynCtx.setProperty(SynapseConstants.CONTINUE_FLOW_TRIGGERED_FROM_MEDIATOR_WORKER, false);
        SeqContinuationState seqContinuationState = (SeqContinuationState) ContinuationStackManager.peakContinuationStateStack(newSynCtx);
        boolean result = false;

        if (RuntimeStatisticCollector.isStatisticsEnabled()) {
            CloseEventCollector.closeEntryEvent(newSynCtx, getMediatorName(), ComponentType.MEDIATOR,
                    statisticReportingIndex, isContentAltering());
        }

        if (seqContinuationState != null) {
            SequenceMediator sequenceMediator = ContinuationStackManager.retrieveSequence(newSynCtx, seqContinuationState);
            result = sequenceMediator.mediate(newSynCtx, seqContinuationState);
            if (RuntimeStatisticCollector.isStatisticsEnabled()) {
                sequenceMediator.reportCloseStatistics(newSynCtx, null);
            }
        }
        CloseEventCollector.closeEventsAfterScatterGather(newSynCtx);
        return result;
    }

    private MessageContext getAggregatedMessage(Aggregate aggregate) {

        MessageContext newCtx = null;
        JsonArray jsonArray = new JsonArray();
        JsonElement result;
        boolean isJSONAggregation = aggregationExpression instanceof SynapseJsonPath;

        for (MessageContext synCtx : aggregate.getMessages()) {
            if (newCtx == null) {
                try {
                    newCtx = MessageHelper.cloneMessageContext(synCtx, true, false, true);
                } catch (AxisFault axisFault) {
                    handleException(aggregate, "Error creating a copy of the message", axisFault, synCtx);
                }

                if (log.isDebugEnabled()) {
                    log.debug("Generating Aggregated message from : " + newCtx.getEnvelope());
                }
                if (isJSONAggregation) {
                    jsonArray.add(EIPUtils.getJSONElement(synCtx, (SynapseJsonPath) aggregationExpression));
                } else {
                    try {
                        EIPUtils.enrichEnvelope(newCtx.getEnvelope(), synCtx, (SynapseXPath) aggregationExpression);
                    } catch (JaxenException e) {
                        handleException(aggregate, "Error merging aggregation results using XPath : " +
                                aggregationExpression.toString(), e, synCtx);
                    }
                }
            } else {
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Merging message : " + synCtx.getEnvelope() + " using XPath : " +
                                aggregationExpression);
                    }
                    // When the target sequences are not content aware, the message builder wont get triggered.
                    // Therefore, we need to build the message to do the aggregation.
                    RelayUtils.buildMessage(((Axis2MessageContext) synCtx).getAxis2MessageContext());
                    if (isJSONAggregation) {
                        jsonArray.add(EIPUtils.getJSONElement(synCtx, (SynapseJsonPath) aggregationExpression));
                    } else {
                        EIPUtils.enrichEnvelope(newCtx.getEnvelope(), synCtx.getEnvelope(), synCtx, (SynapseXPath)
                                aggregationExpression);
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("Merged result : " + newCtx.getEnvelope());
                    }
                } catch (JaxenException e) {
                    handleException(aggregate, "Error merging aggregation results using XPath : " +
                            aggregationExpression.toString(), e, synCtx);
                } catch (SynapseException e) {
                    handleException(aggregate, "Error evaluating expression: " + aggregationExpression.toString(), e, synCtx);
                } catch (JsonSyntaxException e) {
                    handleException(aggregate, "Error reading JSON element: " + aggregationExpression.toString(), e, synCtx);
                } catch (IOException e) {
                    handleException(aggregate, "IO Error occurred while building the message", e, synCtx);
                } catch (XMLStreamException e) {
                    handleException(aggregate, "XML Error occurred while building the message", e, synCtx);
                }
            }
        }

        result = jsonArray;

        StatisticDataCollectionHelper.collectAggregatedParents(aggregate.getMessages(), newCtx);
        if (isJSONAggregation) {
            // setting the new JSON payload to the messageContext
            try {
                JsonUtil.getNewJsonPayload(((Axis2MessageContext) newCtx).getAxis2MessageContext(), new
                        ByteArrayInputStream(result.toString().getBytes()), true, true);
            } catch (AxisFault axisFault) {
                log.error("Error occurred while setting the new JSON payload to the msg context", axisFault);
            }
        } else {
            // Removing the JSON stream after aggregated using XML path.
            // This will fix inconsistent behaviour in logging the payload.
            ((Axis2MessageContext) newCtx).getAxis2MessageContext()
                    .removeProperty(org.apache.synapse.commons.json.Constants.ORG_APACHE_SYNAPSE_COMMONS_JSON_JSON_INPUT_STREAM);
        }
        return newCtx;
    }

    public SynapsePath getCorrelateExpression() {

        return correlateExpression;
    }

    public void setCorrelateExpression(SynapsePath correlateExpression) {

        this.correlateExpression = correlateExpression;
        this.id = null;
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

    /**
     * Check whether the message is a scatter message or not
     *
     * @param synCtx MessageContext
     * @return true if the message is a scatter message
     */
    private static boolean isContinuationTriggeredFromMediatorWorker(MessageContext synCtx) {

        Boolean isContinuationTriggeredMediatorWorker =
                (Boolean) synCtx.getProperty(SynapseConstants.CONTINUE_FLOW_TRIGGERED_FROM_MEDIATOR_WORKER);
        return isContinuationTriggeredMediatorWorker != null && isContinuationTriggeredMediatorWorker;
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
        activeAggregates.clear();
        if (exception != null) {
            super.handleException(msg, exception, msgContext);
        } else {
            super.handleException(msg, msgContext);
        }
    }
}
