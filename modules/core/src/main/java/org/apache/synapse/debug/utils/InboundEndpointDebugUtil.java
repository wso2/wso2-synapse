/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.debug.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.debug.SynapseDebugManager;
import org.apache.synapse.debug.constants.SynapseDebugCommandConstants;
import org.apache.synapse.debug.constructs.SequenceMediationFlowPoint;
import org.apache.synapse.debug.constructs.SynapseMediationComponent;
import org.apache.synapse.debug.constructs.SynapseMediationFlowPoint;
import org.apache.synapse.debug.constructs.SynapseSequenceType;
import org.apache.synapse.inbound.InboundEndpoint;
import org.apache.synapse.mediators.AbstractMediator;

/**
 * Utility class that handle persisting Inbound related breakpoint/skip information at mediator level as
 * we maintain debug related information at mediator level
 */
public class InboundEndpointDebugUtil {

    private static SynapseDebugManager debugManager = SynapseDebugManager.getInstance();
    private static final Log log = LogFactory.getLog(InboundEndpointDebugUtil.class);

    /**
     * Registers/Un-registers a breakpoint, point where mediation flow get suspended
     *
     * @param synCfg Synapse configuration
     * @param sequenceType Synapse sequence type
     * @param inboundKey name of the Inbound Endpoint
     * @param position array of integers that uniquely specifies a point in mediation route
     * @param registerMode specify whether register or un register
     */
    public static void registerInboundSequenceMediationFlowBreakPoint(SynapseConfiguration synCfg,
                                                                      String sequenceType,
                                                                      String inboundKey,
                                                                      int[] position,
                                                                      boolean registerMode) {
        SynapseSequenceType synapseSequenceType = SynapseSequenceType.valueOf(sequenceType.toUpperCase());
        SequenceMediationFlowPoint breakPoint = new SequenceMediationFlowPoint();
        breakPoint.setSynapseMediationComponent(SynapseMediationComponent.SEQUENCE);
        breakPoint.setKey(inboundKey);
        breakPoint.setMediatorPosition(position);
        breakPoint.setSequenceBaseType(SynapseDebugCommandConstants.
                DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_INBOUND);
        breakPoint.setSynapseSequenceType(synapseSequenceType);
        Mediator seqMediator = null;
        InboundEndpoint inbound = null;
        inbound = synCfg.getInboundEndpoint(inboundKey);
        if (inbound != null) {
            if (synapseSequenceType.equals(SynapseSequenceType.INBOUND_SEQ)) {
                seqMediator = synCfg.getSequence(inbound.getInjectingSeq());
            } else if (synapseSequenceType.equals(SynapseSequenceType.INBOUND_FAULTSEQ)) {
                seqMediator = synCfg.getSequence(inbound.getOnErrorSeq());
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Non existing Inbound Endpoint for key " + breakPoint.getKey());
            }
            debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                    SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_INBOUND_NOT_FOUND).toString());
            return;
        }

        if (seqMediator != null) {
            Mediator current_mediator = null;
            current_mediator = MediatorTreeTraverseUtil.getMediatorReference(synCfg, seqMediator, position);
            if (current_mediator != null) {
                breakPoint.setMediatorReference(current_mediator);
                if (registerMode) {
                    if (!((AbstractMediator) current_mediator).isBreakPoint()) {
                        ((AbstractMediator) current_mediator).setBreakPoint(true);
                        ((AbstractMediator) current_mediator).registerMediationFlowPoint(breakPoint);
                        if (log.isDebugEnabled()) {
                            log.debug("Registered breakpoint at mediator position " + logMediatorPosition(breakPoint) +
                                    " for Inbound Endpoint key " + breakPoint.getKey() + " sequence " +
                                    breakPoint.getSynapseSequenceType().toString());
                        }
                        debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(true, null).toString());

                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Failed register breakpoint. Already breakpoint enabled at mediator position " +
                                    logMediatorPosition(breakPoint) + " for Inbound Endpoint key " + breakPoint.getKey() +
                                    " sequence " + breakPoint.getSynapseSequenceType().toString());
                        }
                        debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                                SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_BREAKPOINT_ENABLED)
                                .toString());
                    }
                } else {
                    if (((AbstractMediator) current_mediator).isBreakPoint()) {
                        ((AbstractMediator) current_mediator).unregisterMediationFlowPoint();
                        ((AbstractMediator) current_mediator).setBreakPoint(false);
                        if (log.isDebugEnabled()) {
                            log.debug("Unregistered breakpoint at mediator position " + logMediatorPosition(breakPoint) +
                                    " for Inbound Endpoint key " + breakPoint.getKey() + " sequence " +
                                    breakPoint.getSynapseSequenceType().toString());
                        }
                        debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(true, null).toString());

                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Failed unregister breakpoint. Already breakpoint disabled at mediator position " +
                                    logMediatorPosition(breakPoint) + " for Inbound Endpoint key " + breakPoint.getKey() +
                                    " sequence " + breakPoint.getSynapseSequenceType().toString());
                        }
                        debugManager.advertiseCommandResponse(debugManager
                                .createDebugCommandResponse(false, SynapseDebugCommandConstants
                                        .DEBUG_COMMAND_RESPONSE_ALREADY_BREAKPOINT_DISABLED).toString());
                    }
                }
            } else {
                if (registerMode) {
                    if (log.isDebugEnabled()) {
                        log.debug("Failed register breakpoint. Non existing mediator position at " +
                                logMediatorPosition(breakPoint) + " for Inbound Endpoint key " +
                                breakPoint.getKey() + " sequence " + breakPoint.getSynapseSequenceType().toString());
                    }
                    debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                            SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION)
                            .toString());
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Failed unregister breakpoint. Non existing mediator position at " +
                                logMediatorPosition(breakPoint) + " for Inbound Endpoint key " + breakPoint.getKey() +
                                " sequence " + breakPoint.getSynapseSequenceType().toString());
                    }
                    debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                            SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION)
                            .toString());
                }
            }
        } else {
            if (registerMode) {
                if (log.isDebugEnabled()) {
                    log.debug("Failed register breakpoint. Non existing sequence " +
                            breakPoint.getSynapseSequenceType().toString() +
                            " for Inbound Endpoint key " + breakPoint.getKey());
                }
                debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                        SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_SEQUENCE)
                        .toString());
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Failed unregister breakpoint. Non existing sequence " +
                            breakPoint.getSynapseSequenceType().toString() +
                            " for Inbound Endpoint key " + breakPoint.getKey());
                }
                debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                        SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_SEQUENCE)
                        .toString());

            }
        }
    }

    /**
     * Registers/Un-registers a skip, point where mediator disables from mediation flow
     *
     * @param synCfg Synapse configuration
     * @param seqType Synapse sequence type
     * @param inboundKey name of the Inbound Endpoint
     * @param position array of integers that uniquely specifies a point in mediation route
     * @param registerMode specify whether register or un register
     */
    public static void registerInboundSequenceMediationFlowSkip(SynapseConfiguration synCfg,
                                                                String seqType,
                                                                String inboundKey,
                                                                int[] position,
                                                                boolean registerMode) {
        SynapseSequenceType synapseSequenceType = SynapseSequenceType.valueOf(seqType.toUpperCase());
        SequenceMediationFlowPoint skipPoint = new SequenceMediationFlowPoint();
        skipPoint.setSynapseMediationComponent(SynapseMediationComponent.SEQUENCE);
        skipPoint.setKey(inboundKey);
        skipPoint.setMediatorPosition(position);
        skipPoint.setSequenceBaseType(SynapseDebugCommandConstants.
                DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_INBOUND);
        skipPoint.setSynapseSequenceType(synapseSequenceType);
        Mediator seqMediator = null;
        InboundEndpoint inbound = null;
        inbound = synCfg.getInboundEndpoint(inboundKey);
        if (inbound != null) {
            if (synapseSequenceType.equals(SynapseSequenceType.INBOUND_SEQ)) {
                seqMediator = synCfg.getSequence(inbound.getInjectingSeq());
            } else if (synapseSequenceType.equals(SynapseSequenceType.INBOUND_FAULTSEQ)) {
                seqMediator = synCfg.getSequence(inbound.getOnErrorSeq());
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Non existing Inbound Endpoint for key " + skipPoint.getKey());
            }
            debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                    SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_INBOUND_NOT_FOUND).toString());
            return;
        }
        if (seqMediator != null) {
            Mediator current_mediator = null;
            current_mediator = MediatorTreeTraverseUtil.getMediatorReference(synCfg, seqMediator, position);
            if (current_mediator != null) {
                skipPoint.setMediatorReference(current_mediator);
                if (registerMode) {
                    if (!((AbstractMediator) current_mediator).isSkipEnabled()) {
                        ((AbstractMediator) current_mediator).setSkipEnabled(true);
                        ((AbstractMediator) current_mediator).registerMediationFlowPoint(skipPoint);
                        if (log.isDebugEnabled()) {
                            log.debug("Registered skip at mediator position " + logMediatorPosition(skipPoint) +
                                    " for Inbound Endpoint key " + skipPoint.getKey() +
                                    " sequence " + skipPoint.getSynapseSequenceType().toString());
                        }
                        debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(true, null).toString());
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Failed register skip. Already skip enabled at mediator position " +
                                    logMediatorPosition(skipPoint) +
                                    " for Inbound Endpoint key " + skipPoint.getKey() +
                                    " sequence " + skipPoint.getSynapseSequenceType().toString());
                        }
                        debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                                SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_SKIP_ENABLED).toString());
                    }
                } else {
                    if (((AbstractMediator) current_mediator).isSkipEnabled()) {
                        ((AbstractMediator) current_mediator).unregisterMediationFlowPoint();
                        ((AbstractMediator) current_mediator).setSkipEnabled(false);
                        if (log.isDebugEnabled()) {
                            log.debug("Unregistered skip at mediator position " + logMediatorPosition(skipPoint) +
                                    " for Inbound Endpoint key " + skipPoint.getKey() +
                                    " sequence " + skipPoint.getSynapseSequenceType().toString());
                        }
                        debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(true, null)
                                .toString());
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Failed unregister skip. Already skip disabled at mediator position " +
                                    logMediatorPosition(skipPoint) +
                                    " for Inbound Endpoint key " + skipPoint.getKey() +
                                    " sequence " + skipPoint.getSynapseSequenceType().toString());
                        }
                        debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                                SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_SKIP_DISABLED)
                                .toString());
                    }
                }
            } else {
                if (registerMode) {
                    if (log.isDebugEnabled()) {
                        log.debug("Failed register skip. Non existing mediator position at " +
                                logMediatorPosition(skipPoint) +
                                " for Inbound Endpoint key " + skipPoint.getKey() +
                                " sequence " + skipPoint.getSynapseSequenceType().toString());
                    }
                    debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                            SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION)
                            .toString());
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Failed unregister skip. Non existing mediator position at " +
                                logMediatorPosition(skipPoint) +
                                " for Inbound Endpoint key " + skipPoint.getKey() +
                                " sequence " + skipPoint.getSynapseSequenceType().toString());
                    }
                    debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                            SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION)
                            .toString());
                }
            }
        } else {
            if (registerMode) {
                if (log.isDebugEnabled()) {
                    log.debug("Failed register skip. Non existing sequence " +
                            skipPoint.getSynapseSequenceType().toString() +
                            " for Inbound Endpoint key " + skipPoint.getKey());
                }
                debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                        SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_SEQUENCE).toString());

            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Failed unregister skip. Non existing sequence " +
                            skipPoint.getSynapseSequenceType().toString() +
                            " for Inbound Endpoint key " + skipPoint.getKey());
                }
                debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                        SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_SEQUENCE).toString());

            }
        }
    }

    protected static String logMediatorPosition(SynapseMediationFlowPoint flowPoint) {
        String position = "";
        for (int count = 0; count < flowPoint.getMediatorPosition().length; count++) {
            if (count != 0) {
                position = position.concat("-->");
            }
            position = position.concat("(" +
                    String.valueOf(flowPoint.getMediatorPosition()[count]) + ")");
        }
        return position;
    }

}
