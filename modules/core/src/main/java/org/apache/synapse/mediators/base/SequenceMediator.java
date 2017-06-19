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

package org.apache.synapse.mediators.base;

import org.apache.synapse.ContinuationState;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.Nameable;
import org.apache.synapse.SequenceType;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.aspects.AspectConfiguration;
import org.apache.synapse.aspects.flow.statistics.StatisticIdentityGenerator;
import org.apache.synapse.aspects.flow.statistics.collectors.CloseEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.OpenEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.aspects.flow.statistics.data.artifact.ArtifactHolder;
import org.apache.synapse.transport.customlogsetter.CustomLogSetter;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.continuation.ContinuationStackManager;
import org.apache.synapse.continuation.SeqContinuationState;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.AbstractListMediator;
import org.apache.synapse.mediators.FlowContinuableMediator;
import org.apache.synapse.mediators.MediatorFaultHandler;
import org.apache.synapse.mediators.Value;

import java.util.Stack;

/**
 * The Sequence mediator either refers to a named Sequence mediator instance
 * or is a *Named* list/sequence of other (child) Mediators
 * <p/>ce
 * If this instance defines a sequence mediator, then the name is required, and
 * an errorHandler sequence name optional. If this instance refers to another (defined)
 * sequence mediator, the errorHandler will not have a meaning, and if an error in
 * encountered in the referred sequence, its errorHandler would execute.
 */
public class SequenceMediator extends AbstractListMediator implements Nameable,
                                                                      FlowContinuableMediator {

    /** The name of the this sequence */
    private String name = null;
    /**The version of this sequence*/
    private String version = null;
    /** The local registry key which is used to pick a sequence definition*/
    private Value key = null;
    /** The name of the error handler which is used to handle error during the mediation */
    private String errorHandler = null;
    /** is this definition dynamic */
    private boolean dynamic = false;
    /** flag to ensure that each and every sequence is initialized and destroyed atmost once */
    private boolean initialized = false;
    /** the registry key to load this definition if dynamic */
    private String registryKey = null;
    /** The name of the file where this sequence is defined */
    private String fileName;
    /** type of the sequence*/
    private SequenceType sequenceType = SequenceType.NAMED;
    /** Reference to the synapse environment */
    private SynapseEnvironment synapseEnv;
    /** Name of the car file which the sequence deployed from */
    private String artifactContainerName;
    /**
     * Whether the sequence is edited through the management console or not
     */
    private boolean isEdited;

    /**
     * If this mediator refers to another named Sequence, execute that. Else
     * execute the list of mediators (children) contained within this. If a referenced
     * named sequence mediator instance cannot be found at runtime, an exception is
     * thrown. This may occur due to invalid configuration of an erroneous runtime
     * change of the synapse configuration. It is the responsibility of the
     * SynapseConfiguration builder to ensure that dead references are not present.
     *
     * @param synCtx the synapse message
     * @return as per standard mediator result
     */
    public boolean mediate(MessageContext synCtx) {

        if (synCtx.getEnvironment().isDebuggerEnabled()) {
            if (super.divertMediationRoute(synCtx)) {
                return true;
            }
        }

        SynapseLog synLog = getLog(synCtx);

        if (sequenceType == SequenceType.NAMED) {
            CustomLogSetter.getInstance().setLogAppender(artifactContainerName);
        }

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Sequence "
                    + (this.getName() == null ? (key == null ? "<anonymous" : "key=<" + key) : "<"
                    + this.getName()) + ">");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        if (key == null) {

            // The onError sequence for handling errors which may occur during the
            // mediation through this sequence
            Mediator errorHandlerMediator = null;
            Integer statisticReportingIndex = null;
            if (RuntimeStatisticCollector.isStatisticsEnabled()) {
                statisticReportingIndex = reportOpenStatistics(synCtx, false);
            }
            // Setting Required property to reportForComponent the sequence aspects

            try {
                // push the errorHandler sequence into the current message as the fault handler
                if (errorHandler != null) {
                    errorHandlerMediator = synCtx.getSequence(errorHandler);

                    if (errorHandlerMediator != null) {
                        if (synLog.isTraceOrDebugEnabled()) {
                            synLog.traceOrDebug("Setting the onError handler : " +
                                    errorHandler + " for the sequence : " + this.getName());
                        }
                        synCtx.pushFaultHandler(
                                new MediatorFaultHandler(errorHandlerMediator));
                    } else {
                        synLog.auditWarn("onError handler : " + errorHandler + " for sequence : " +
                                this.getName() + " cannot be found");
                    }
                }

                // Add a new SeqContinuationState as we branched to new Sequence.
                boolean skipAddition = ContinuationStackManager.isSkipSeqContinuationStateAddition(synCtx);
                if (!skipAddition) {
                    if (dynamic && registryKey != null) {
                        ContinuationStackManager.addSeqContinuationState(synCtx, registryKey, sequenceType);
                    } else {
                        ContinuationStackManager.addSeqContinuationState(synCtx, this.getName(), sequenceType);
                    }
                }

                boolean result = super.mediate(synCtx);

                if (result && !skipAddition) {
                    // if flow completed remove the previously added SeqContinuationState
                    ContinuationStackManager.removeSeqContinuationState(synCtx, sequenceType);
                }

                // if we pushed an error handler, pop it from the fault stack
                // before we exit normally without an exception
                if (errorHandlerMediator != null) {
                    Stack faultStack = synCtx.getFaultStack();
                    if (faultStack != null && !faultStack.isEmpty()) {
                        Object o = faultStack.peek();
                        if (o instanceof MediatorFaultHandler &&
                                errorHandlerMediator.equals(
                                        ((MediatorFaultHandler) o).getFaultMediator())) {
                                faultStack.pop();
                        }
                    }
                }

                if (synLog.isTraceOrDebugEnabled()) {
                    if (synLog.isTraceTraceEnabled()) {
                        synLog.traceTrace("Message : " + synCtx.getEnvelope());
                    }

                    synLog.traceOrDebug(
                            "End : Sequence <" + (this.getName() == null ? "anonymous" : this.getName()) + ">");
                }


                return result;

            } finally {
                // End Statistics
                if (RuntimeStatisticCollector.isStatisticsEnabled()) {
                    reportCloseStatistics(synCtx, statisticReportingIndex);
                }
            }

        } else {
            String sequenceKey = key.evaluateValue(synCtx);
            //Mediator m = synCtx.getSequence(key);
            Mediator m = synCtx.getSequence(sequenceKey);
            if (m == null) {
                handleException("Sequence named " + key + " cannot be found", synCtx);

            } else {
                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("Executing with key " + key);
                }

                // Update the SeqContinuationState position with the mediator position of
                // sequence mediator in the sequence
                ContinuationStackManager.updateSeqContinuationState(synCtx, getMediatorPosition());

                boolean result = m.mediate(synCtx);

                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("End : Sequence key=<" + key + ">");
                }
                return result;
            }
        }
        return false;
    }

    public boolean mediate(MessageContext synCtx, ContinuationState continuationState) {

        SynapseLog synLog = getLog(synCtx);

        if (sequenceType == SequenceType.NAMED) {
            CustomLogSetter.getInstance().setLogAppender(artifactContainerName);
        }

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Mediating using the SeqContinuationState type : " +
                                ((SeqContinuationState) continuationState).getSeqType() +
                                " name : " +
                                ((SeqContinuationState) continuationState).getSeqName());
        }

        Mediator errorHandlerMediator = null;
        // push the errorHandler sequence into the current message as the fault handler
        if (errorHandler != null) {
            errorHandlerMediator = synCtx.getSequence(errorHandler);

            if (errorHandlerMediator != null) {
                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("Setting the onError handler : " +
                                        errorHandler + " for the sequence : " + this.getName());
                }
                synCtx.pushFaultHandler(
                        new MediatorFaultHandler(errorHandlerMediator));
            } else {
                synLog.auditWarn("onError handler : " + errorHandler + " for sequence : " +
                        this.getName() + " cannot be found");
            }
        }

        boolean result;
        if (!continuationState.hasChild()) {
            result = super.mediate(synCtx, continuationState.getPosition() + 1);
        } else {
            // if children exists first mediate from them starting from grandchild.
            do {
                FlowContinuableMediator mediator =
                        (FlowContinuableMediator) getChild(continuationState.getPosition());

                result = mediator.mediate(synCtx,
                                          continuationState.getChildContState());

                if (RuntimeStatisticCollector.isStatisticsEnabled()) {
                    ((Mediator) mediator).reportCloseStatistics(synCtx, null);
                }
                if (result) {
                    // if flow completed remove leaf child
                    continuationState.removeLeafChild();
                }
            } while (result && continuationState.hasChild());

            if (result) {
                // after mediating from children, mediate from current SeqContinuationState
                result = super.mediate(synCtx, continuationState.getPosition() + 1);
            }
        }

        if (result) {
            // if flow completed, remove top ContinuationState from stack
            ContinuationStackManager.popContinuationStateStack(synCtx);
        }

        // if we pushed an error handler, pop it from the fault stack
        // before we exit normally without an exception
        if (errorHandlerMediator != null) {
            Stack faultStack = synCtx.getFaultStack();
            if (faultStack != null && !faultStack.isEmpty()) {
                Object o = faultStack.peek();

                if (o instanceof MediatorFaultHandler &&
                    errorHandlerMediator.equals(
                            ((MediatorFaultHandler) o).getFaultMediator())) {
                    faultStack.pop();
                }
            }
        }
        return result;
    }

    /**
     * This method will ensure that each and every sequence wil only be initialized at most once
     * @param se - environment to be initialized
     */
    @Override
    public synchronized void init(SynapseEnvironment se) {
        if (!initialized) {
            synapseEnv = se;
            super.init(se);
            initialized = true;

            if (!isDynamic()) {
                // mark as available, if this is marked previously as unavailable in the environment
                se.clearUnavailabilityOfArtifact(this.getName());
            }

            if (key != null) {
                if (key.getKeyValue() != null) {

                    SequenceMediator sequenceMediator =
                            (SequenceMediator) se.getSynapseConfiguration().
                                    getSequence(key.getKeyValue());

                    if (sequenceMediator == null || sequenceMediator.isDynamic()) {
                        // undefined or dynamic sequences are treated as unavailable
                        // in the environment.
                        // At the time of their initialization, these will be marked as available.
                        se.addUnavailableArtifactRef(key.getKeyValue());
                    }
                } else {
                    // sequences referred by key-expressions are treated as unavailable at initialization
                    se.addUnavailableArtifactRef(key.getExpression().toString());
                }
            }
        }
    }

    @Override
    public synchronized void destroy() {
        if (initialized) {
            super.destroy();
            initialized = false;

            if (key != null) {
                // Clearing unavailable sequence references added by this sequence
                if (key.getKeyValue() != null) {

                    SequenceMediator sequenceMediator =
                            (SequenceMediator) synapseEnv.getSynapseConfiguration().
                                    getSequence(key.getKeyValue());

                    if (sequenceMediator == null || sequenceMediator.isDynamic()) {
                        synapseEnv.removeUnavailableArtifactRef(key.getKeyValue());
                    }

                } else {
                    synapseEnv.removeUnavailableArtifactRef(key.getExpression().toString());
                }
            }
        }
    }

    /**
     * To get the name of the sequence
     * @return the name of the sequence
     */
    public String getName() {
        String id = name;
        if (version != null) {
            id = id + "/" + version;
        }
        return id;
    }

    /**
     * setting the name of the sequence
     * @param name the name of the this sequence
     */
    public void setName(String name) {
        this.name = name;
    }

    public String getArtifactName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * To get the key which is used to pick the sequence definition from the local registry
     * @return  return the key which is used to pick the sequence definition from the local registry
     */
    public Value getKey() {
        return key;
    }

    /**
     * To set the local registry key in order to pick the sequence definition
     * @param key the local registry key
     */
    public void setKey(Value key) {
        this.key = key;
    }

    /**
     *
     * @return  Returns the errorhandler sequence name
     */
    public String getErrorHandler() {
        return errorHandler;
    }

    /**
     * @param errorHandler to used handle error will appear during the
     *        mediation through this sequence
     */
    public void setErrorHandler(String errorHandler) {
        this.errorHandler = errorHandler;
    }

    /**
     * Is this a dynamic sequence?
     * @return true if dynamic
     */
    public boolean isDynamic() {
        return dynamic;
    }

    /**
     * Mark this as a dynamic sequence
     * @param dynamic true if this is a dynamic sequence
     */
    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    /**
     * Return the registry key used to load this sequence dynamically
     * @return  registry key
     */
    public String getRegistryKey() {
        return registryKey;
    }

    /**
     * To get the registry key used to load this sequence dynamically
     * @param registryKey  returns the registry key which point to this sequence
     */
    public void setRegistryKey(String registryKey) {
        this.registryKey = registryKey;
    }

    public String getAuditId() {
        return getName();
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setArtifactContainerName (String name) {
        artifactContainerName = name;
    }

    public String getArtifactContainerName () {
        return artifactContainerName;
    }

    public boolean isEdited() {
        return isEdited;
    }

    public void setIsEdited(boolean isEdited) {
        this.isEdited = isEdited;
    }

    @Override
    public boolean isContentAware() {
        return false;
    }

    public void setSequenceType(SequenceType sequenceType) {
        this.sequenceType = sequenceType;
    }

    public String getSequenceNameForStatistics() {
        if (this.getName() != null) {
            return this.getName();
        } else {
            if (this.sequenceType != SequenceType.ANON) {
                return this.sequenceType.toString();
            } else {
                return SynapseConstants.ANONYMOUS_SEQUENCE;
            }
        }
    }

    @Override
    public Integer reportOpenStatistics(MessageContext messageContext, boolean isContentAltering) {
        if (key == null) {
            return OpenEventCollector.reportEntryEvent(messageContext, getSequenceNameForStatistics(),
                    getAspectConfiguration(), ComponentType.SEQUENCE);
        }
        return null;
    }

    @Override
    public void reportCloseStatistics(MessageContext messageContext, Integer currentIndex) {
        if (key == null) {
            CloseEventCollector
                    .tryEndFlow(messageContext, getSequenceNameForStatistics(), ComponentType.SEQUENCE,
                                     currentIndex, isContentAltering());
        }
    }

    @Override
    public void setComponentStatisticsId(ArtifactHolder holder) {
        String sequenceId = null;
//        if (sequenceType != SequenceType.ANON) {
            if (getAspectConfiguration() == null) {
                configure(new AspectConfiguration(this.getName()));
            }
            if (getKey()!= null) {
                sequenceId = StatisticIdentityGenerator
                        .getIdReferencingComponent(getKey().getKeyValue(), ComponentType.SEQUENCE, holder);
            } else {
                sequenceId = StatisticIdentityGenerator
                        .getIdForFlowContinuableMediator(getSequenceNameForStatistics(), ComponentType.SEQUENCE, holder);
            }
            getAspectConfiguration().setUniqueId(sequenceId);
//        }

        setStatisticIdForMediators(holder);

        if (sequenceType != SequenceType.ANON) {
            StatisticIdentityGenerator.reportingFlowContinuableEndEvent(sequenceId, ComponentType.SEQUENCE, holder);
        }
    }
}
