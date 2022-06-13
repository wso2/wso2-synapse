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

package org.apache.synapse.continuation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ContinuationState;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SequenceType;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.OpenTelemetryManager;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.OpenTelemetryManagerHolder;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.inbound.InboundEndpoint;
import org.apache.synapse.mediators.MediatorFaultHandler;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.api.API;
import org.apache.synapse.rest.RESTConstants;
import org.apache.synapse.api.Resource;

import java.util.Set;
import java.util.Stack;

/**
 * This is the utility class which manages ContinuationState Stack.
 * <p/>
 * All operations for the stack done by mediators are done through this manager class in order to
 * easily control the operations from a central place.
 *
 */
public class ContinuationStackManager {

    private static Log log = LogFactory.getLog(ContinuationStackManager.class);

    public static final String SKIP_CONTINUATION_STATE = "SKIP_CONTINUATION_STATE";

    /**
     * Add new SeqContinuationState to the stack.
     * This should be done when branching to a new Sequence
     *
     * @param synCtx  Message Context
     * @param seqName Name of the branching sequence
     * @param seqType Sequence Type
     */
    public static void addSeqContinuationState(MessageContext synCtx, String seqName,
                                               SequenceType seqType) {
        if (synCtx.isContinuationEnabled() && !SequenceType.ANON.equals(seqType)) {
            //ignore Anonymous type sequences
            synCtx.pushContinuationState(new SeqContinuationState(seqType, seqName));
            if (RuntimeStatisticCollector.isOpenTelemetryEnabled()) {
                OpenTelemetryManager openTelemetryManager = OpenTelemetryManagerHolder.getOpenTelemetryManager();
                if (openTelemetryManager != null) {
                    openTelemetryManager.getHandler().handleStateStackInsertion(synCtx, seqName, seqType);
                }
            }
        }
    }

    /**
     * Check whether sequence continuation state addition need to be skipped
     *
     * @param synCtx  message context
     * @return whether sequence continuation state addition need to be skipped
     */
    public static boolean isSkipSeqContinuationStateAddition(MessageContext synCtx) {
        Boolean isSkipContinuationState = (Boolean) synCtx.getProperty(SKIP_CONTINUATION_STATE);
        if (isSkipContinuationState != null && isSkipContinuationState) {
            Set keySet = synCtx.getPropertyKeySet();
            if (keySet != null) {
                keySet.remove(SKIP_CONTINUATION_STATE);
            }
            return true;
        }
        return false;
    }

    /**
     * Remove top SeqContinuationState from the stack.
     * This should be done when returning from a Sequence branch.
     *
     * @param synCtx Message Context
     */
    public static void removeSeqContinuationState(MessageContext synCtx, SequenceType seqType) {
        if (synCtx.isContinuationEnabled() && !synCtx.getContinuationStateStack().isEmpty()) {
            if (!SequenceType.ANON.equals(seqType)) {
                ContinuationStackManager.popContinuationStateStack(synCtx);
            } else {
                removeReliantContinuationState(synCtx);
            }
        }
    }

    /**
     * Update SeqContinuationState with the current mediator position in the sequence.
     * SeqContinuationState should be updated when branching to a new flow
     * using a FlowContinuableMediator
     *
     * @param synCtx Message Context
     */
    public static void updateSeqContinuationState(MessageContext synCtx, int position) {
		if (synCtx.isContinuationEnabled()) {
            ContinuationState seqContState = ContinuationStackManager.peakContinuationStateStack(synCtx);
			if (seqContState != null) {
				seqContState.getLeafChild().setPosition(position);
			} else {
				// Ideally we should not get here.
				log.warn("Continuation Stack is empty. Probably due to a configuration issue");
			}
		}
    }

    /**
     * Add a ReliantContinuationState to the top SeqContinuationState in the stack.
     * This should be done when branching to a sub branch using FlowContinuableMediators
     * except Sequence Mediator
     *
     * @param synCtx    Message Context
     * @param subBranch Sub branch id
     */
    public static void addReliantContinuationState(MessageContext synCtx, int subBranch,
                                                   int position) {
        if (synCtx.isContinuationEnabled()) {
            ContinuationState seqContState = ContinuationStackManager.peakContinuationStateStack(synCtx);
            if (seqContState != null) {
                seqContState.getLeafChild().setPosition(position);
                seqContState.addLeafChild(new ReliantContinuationState(subBranch));
            } else {
                // Ideally we should not get here.
                log.warn("Continuation Stack is empty. Probably due to a configuration issue");
            }
        }
    }

    /**
     * Remove a ReliantContinuationState from the top SeqContinuationState in the stack.
     * This should be done when returning back from a sub branch of a FlowContinuableMediator.
     *
     * @param synCtx MessageContext
     */
    public static void removeReliantContinuationState(MessageContext synCtx) {
		if (synCtx.isContinuationEnabled()) {
            ContinuationState seqContState = ContinuationStackManager.peakContinuationStateStack(synCtx);
			if (seqContState != null) {
				seqContState.removeLeafChild();
			} else {
				// Ideally we should not get here.
				log.warn("Continuation Stack is empty. Probably due to a configuration issue");
			}
		}
    }

    /**
     * Get a clone of a SeqContinuationState
     *
     * @param oriSeqContinuationState original SeqContinuationState
     * @return cloned SeqContinuationState
     */
    public static SeqContinuationState getClonedSeqContinuationState(
            SeqContinuationState oriSeqContinuationState) {
        SeqContinuationState clone =
                new SeqContinuationState(oriSeqContinuationState.getSeqType(),
                                         oriSeqContinuationState.getSeqName());
        clone.setPosition(oriSeqContinuationState.getPosition());
        if (oriSeqContinuationState.hasChild()) {
            clone.setChildContState(getClonedReliantContState(
                    oriSeqContinuationState.getChildContState()));
        }
        return clone;
    }

    /*
     * Get a clone of the ReliantContinuationState
     */
    private static ReliantContinuationState getClonedReliantContState(
            org.apache.synapse.ContinuationState continuationState) {

        ReliantContinuationState oriConstState =
                (ReliantContinuationState) continuationState;
        ReliantContinuationState clone =
                new ReliantContinuationState(oriConstState.getSubBranch());

        clone.setPosition(oriConstState.getPosition());
        if (oriConstState.hasChild()) {
            clone.setChildContState(
                    getClonedReliantContState(oriConstState.getChildContState()));
        }
        return clone;
    }

    /**
     * Remove all ContinuationStates from ContinuationState Stack
     * @param synCtx MessageContext
     */
    public static void clearStack(MessageContext synCtx) {
        Stack<ContinuationState> continuationStack = synCtx.getContinuationStateStack();
        if (synCtx.isContinuationEnabled()) {
            synchronized (continuationStack){
                continuationStack.clear();
                if (RuntimeStatisticCollector.isOpenTelemetryEnabled()) {
                    OpenTelemetryManager openTelemetryManager = OpenTelemetryManagerHolder.getOpenTelemetryManager();
                    if (openTelemetryManager != null) {
                        openTelemetryManager.getHandler().handleStateStackClearance(synCtx);
                    }
                }
            }
        }
    }

    /**
     * Peek from Continuation Stack
     * @return ContinuationState
     */
    public static ContinuationState peakContinuationStateStack(MessageContext synCtx){
        Stack<ContinuationState> continuationStack = synCtx.getContinuationStateStack();
        synchronized (continuationStack) {
            if (!continuationStack.isEmpty()) {
                return continuationStack.peek();
            } else {
                return null;
            }
        }
    }

    /**
     * Pop from Continuation Stack
     */
    public static void popContinuationStateStack(MessageContext synCtx){
        Stack<ContinuationState> continuationStack = synCtx.getContinuationStateStack();
        synchronized (continuationStack) {
            if (!continuationStack.isEmpty()) {
                ContinuationState poppedContinuationState = continuationStack.pop();
                if (RuntimeStatisticCollector.isOpenTelemetryEnabled()) {
                    OpenTelemetryManager openTelemetryManager = OpenTelemetryManagerHolder.getOpenTelemetryManager();
                    if (openTelemetryManager != null) {
                        openTelemetryManager.getHandler()
                                .handleStateStackRemoval(poppedContinuationState, synCtx);
                    }
                }
            }
        }
    }

    /**
     * Retrieve the sequence from Continuation state which message should be injected to.
     *
     * @param seqContState SeqContinuationState which contain the sequence information
     * @param synCtx       message context
     * @return sequence which message should be injected to
     */
    public static SequenceMediator retrieveSequence(MessageContext synCtx,
                                                    SeqContinuationState seqContState) {
        SequenceMediator sequence = null;

        switch (seqContState.getSeqType()) {
            case NAMED: {
                sequence = (SequenceMediator) synCtx.getSequence(seqContState.getSeqName());
                if (sequence == null) {
                    // This can happen only if someone delete the sequence while running
                    handleException("Sequence : " + seqContState.getSeqName() + " not found");
                }
                break;
            }
            case PROXY_INSEQ: {
                String proxyName = (String) synCtx.getProperty(SynapseConstants.PROXY_SERVICE);
                ProxyService proxyService = synCtx.getConfiguration().getProxyService(proxyName);
                if (proxyService != null) {
                    sequence = proxyService.getTargetInLineInSequence();
                } else {
                    handleException("Proxy Service :" + proxyName + " not found");
                }
                break;
            }
            case API_INSEQ: {
                String apiName = (String) synCtx.getProperty(RESTConstants.SYNAPSE_REST_API);
                String resourceName = (String) synCtx.getProperty(RESTConstants.SYNAPSE_RESOURCE);
                API api = synCtx.getEnvironment().getSynapseConfiguration().getAPI(apiName);
                if (api != null) {
                    Resource resource = api.getResource(resourceName);
                    if (resource != null) {
                        sequence = resource.getInSequence();
                    } else {
                        handleException("Resource : " + resourceName + " not found");
                    }
                } else {
                    handleException("REST API : " + apiName + " not found");
                }
                break;
            }
            case PROXY_OUTSEQ: {
                String proxyName = (String) synCtx.getProperty(SynapseConstants.PROXY_SERVICE);
                ProxyService proxyService = synCtx.getConfiguration().getProxyService(proxyName);
                if (proxyService != null) {
                    sequence = proxyService.getTargetInLineOutSequence();
                } else {
                    handleException("Proxy Service :" + proxyName + " not found");
                }
                break;
            }
            case API_OUTSEQ: {
                String apiName = (String) synCtx.getProperty(RESTConstants.SYNAPSE_REST_API);
                String resourceName = (String) synCtx.getProperty(RESTConstants.SYNAPSE_RESOURCE);
                API api = synCtx.getEnvironment().getSynapseConfiguration().getAPI(apiName);
                if (api != null) {
                    Resource resource = api.getResource(resourceName);
                    if (resource != null) {
                        sequence = resource.getOutSequence();
                    } else {
                        handleException("Resource : " + resourceName + " not found");
                    }
                } else {
                    handleException("REST API : " + apiName + " not found");
                }
                break;
            }
            case PROXY_FAULTSEQ: {
                String proxyName = (String) synCtx.getProperty(SynapseConstants.PROXY_SERVICE);
                ProxyService proxyService = synCtx.getConfiguration().getProxyService(proxyName);
                if (proxyService != null) {
                    sequence = proxyService.getTargetInLineFaultSequence();
                } else {
                    handleException("Proxy Service :" + proxyName + " not found");
                }
                break;
            }
            case API_FAULTSEQ: {
                String apiName = (String) synCtx.getProperty(RESTConstants.SYNAPSE_REST_API);
                String resourceName = (String) synCtx.getProperty(RESTConstants.SYNAPSE_RESOURCE);
                API api = synCtx.getEnvironment().getSynapseConfiguration().getAPI(apiName);
                if (api != null) {
                    Resource resource = api.getResource(resourceName);
                    if (resource != null) {
                        sequence = resource.getFaultSequence();
                    } else {
                        handleException("Resource : " + resourceName + " not found");
                    }
                } else {
                    handleException("REST API : " + apiName + " not found");
                }
                break;
            }
        }

        return sequence;
    }

    /**
     * Push fault handler for the received continuation call response.
     *
     * @param seqContState SeqContinuationState which contain the sequence information
     * @param synCtx       message context
     */
    public static void pushFaultHandler(MessageContext synCtx, SeqContinuationState seqContState) {
        switch (seqContState.getSeqType()) {
            case NAMED: {
                pushRootFaultHandlerForSequence(synCtx);
                break;
            }
            case PROXY_INSEQ: {
                String proxyName = (String) synCtx.getProperty(SynapseConstants.PROXY_SERVICE);
                ProxyService proxyService = synCtx.getConfiguration().getProxyService(proxyName);
                if (proxyService != null) {
                    proxyService.registerFaultHandler(synCtx);
                } else {
                    handleException("Proxy Service :" + proxyName + " not found");
                }
                break;
            }
            case API_INSEQ: {
                String apiName = (String) synCtx.getProperty(RESTConstants.SYNAPSE_REST_API);
                String resourceName = (String) synCtx.getProperty(RESTConstants.SYNAPSE_RESOURCE);
                API api = synCtx.getEnvironment().getSynapseConfiguration().getAPI(apiName);
                if (api != null) {
                    Resource resource = api.getResource(resourceName);
                    if (resource != null) {
                        resource.registerFaultHandler(synCtx);
                    } else {
                        handleException("Resource : " + resourceName + " not found");
                    }
                } else {
                    handleException("REST API : " + apiName + " not found");
                }
                break;
            }
            case PROXY_OUTSEQ: {
                String proxyName = (String) synCtx.getProperty(SynapseConstants.PROXY_SERVICE);
                ProxyService proxyService = synCtx.getConfiguration().getProxyService(proxyName);
                if (proxyService != null) {
                    proxyService.registerFaultHandler(synCtx);
                } else {
                    handleException("Proxy Service :" + proxyName + " not found");
                }
                break;
            }
            case API_OUTSEQ: {
                String apiName = (String) synCtx.getProperty(RESTConstants.SYNAPSE_REST_API);
                String resourceName = (String) synCtx.getProperty(RESTConstants.SYNAPSE_RESOURCE);
                API api = synCtx.getEnvironment().getSynapseConfiguration().getAPI(apiName);
                if (api != null) {
                    Resource resource = api.getResource(resourceName);
                    if (resource != null) {
                        resource.registerFaultHandler(synCtx);
                    } else {
                        handleException("Resource : " + resourceName + " not found");
                    }
                } else {
                    handleException("REST API : " + apiName + " not found");
                }
                break;
            }
        }
    }

    /**
     * Find the correct root fault handler for named sequences.
     * 
     * If the message is initiated from a proxy, we need to assign the proxy fault sequence.
     * If the message is initiated from a API Resource, we need to assign the resource fault sequence.
     *
     * @param synCtx message context
     */
    private static void pushRootFaultHandlerForSequence(MessageContext synCtx) {

        // For Proxy services
        String proxyName = (String) synCtx.getProperty(SynapseConstants.PROXY_SERVICE);
        if (proxyName != null && !"".equals(proxyName)) {
            ProxyService proxyService = synCtx.getConfiguration().getProxyService(proxyName);
            if (proxyService != null) {
                proxyService.registerFaultHandler(synCtx);
            } else {
                handleException("Proxy service : " + proxyName + " not found");
            }
            return;
        }

        // For APIs
        String apiName = (String) synCtx.getProperty(RESTConstants.SYNAPSE_REST_API);
        if (apiName != null && !"".equals(apiName)) {
            API api = synCtx.getEnvironment().getSynapseConfiguration().getAPI(apiName);
            if (api != null) {
                String resourceName = (String) synCtx.getProperty(RESTConstants.SYNAPSE_RESOURCE);
                Resource resource = api.getResource(resourceName);
                if (resource != null) {
                    resource.registerFaultHandler(synCtx);
                } else {
                    handleException("Resource : " + resourceName + " not found");
                }
            } else {
                handleException("REST API : " + apiName + " not found");
            }
            return;
        }

        // For Inbound Endpoints
        if (synCtx.getProperty(SynapseConstants.IS_INBOUND) != null) {
            String inboundEndpointName = (String) synCtx.getProperty(SynapseConstants.INBOUND_ENDPOINT_NAME);
            if (inboundEndpointName != null) {
                InboundEndpoint inboundEndpoint = synCtx.getConfiguration().getInboundEndpoint(inboundEndpointName);
                if (inboundEndpoint != null) {
                    String errorSeqName = inboundEndpoint.getOnErrorSeq();
                    if (errorSeqName != null) {
                        SequenceMediator errorSeq = (SequenceMediator) synCtx.getConfiguration().getSequence(errorSeqName);
                        if (errorSeq !=  null) {
                            synCtx.pushFaultHandler(new MediatorFaultHandler(errorSeq));
                            return;
                        } else {
                            handleException("Sequence Mediator : " + errorSeqName + "not found");
                        }
                    }
                } else {
                    handleException("Inbound Endpoint : " + inboundEndpointName + "not found");
                }
            }
        }
        //For main sequence/MessageInjector etc, push the default fault handler
        synCtx.pushFaultHandler(new MediatorFaultHandler(synCtx.getFaultSequence()));

    }

    private static void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

}
