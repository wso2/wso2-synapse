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
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.debug.constants.SynapseDebugCommandConstants;
import org.apache.synapse.debug.SynapseDebugManager;
import org.apache.synapse.debug.constructs.SynapseMediationComponent;
import org.apache.synapse.debug.constructs.SequenceMediationFlowPoint;
import org.apache.synapse.debug.constructs.SynapseMediationFlowPoint;
import org.apache.synapse.debug.constructs.SynapseSequenceType;
import org.apache.synapse.mediators.AbstractMediator;

/**
 * Utility class that handle persisting Proxy related breakpoint/skip information at mediator level as
 * we maintain debug related information at mediator level
 */
public class ProxyDebugUtil {

    private static SynapseDebugManager debugManager = SynapseDebugManager.getInstance();
    private static final Log log = LogFactory.getLog(ProxyDebugUtil.class);

    /**
     * Registers/Un-registers a breakpoint, point where mediation flow get suspended
     *
     * @param synCfg Synapse configuration
     * @param sequenceType Synapse sequence type
     * @param proxyKey name of the Proxy Service
     * @param position array of integers that uniquely specifies a point in mediation route
     * @param registerMode specify whether register or un register
     */
    public static void registerProxySequenceMediationFlowBreakPoint(SynapseConfiguration synCfg,
                                                                    String sequenceType,
                                                                    String proxyKey,
                                                                    int[] position,
                                                                    boolean registerMode) {
        SynapseSequenceType synapseSequenceType = SynapseSequenceType.valueOf(sequenceType.toUpperCase());
        SequenceMediationFlowPoint breakPoint = new SequenceMediationFlowPoint();
        breakPoint.setSynapseMediationComponent(SynapseMediationComponent.SEQUENCE);
        breakPoint.setKey(proxyKey);
        breakPoint.setMediatorPosition(position);
        breakPoint.setSequenceBaseType(SynapseDebugCommandConstants.
                DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY);
        breakPoint.setSynapseSequenceType(synapseSequenceType);
        Mediator seqMediator = null;
        ProxyService proxy = null;
        proxy = synCfg.getProxyService(proxyKey);
        if (proxy != null) {
            if (synapseSequenceType.equals(SynapseSequenceType.PROXY_INSEQ)) {
                seqMediator = proxy.getTargetInLineInSequence();
            } else if (synapseSequenceType.equals(SynapseSequenceType.PROXY_OUTSEQ)) {
                seqMediator = proxy.getTargetInLineOutSequence();
            } else if (synapseSequenceType.equals(SynapseSequenceType.PROXY_FAULTSEQ)) {
                seqMediator = proxy.getTargetInLineFaultSequence();
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Non existing Proxy for the key " + breakPoint.getKey());
            }
            debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                    SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_PROXY_NOT_FOUND).toString());
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
                            log.debug("Registered breakpoint at mediator position " + logMediatorPosition(breakPoint)
                                    + " for Proxy key " + breakPoint.getKey() + " Sequence "
                                    + breakPoint.getSynapseSequenceType().toString());
                        }
                        debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(true, null).toString());

                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Failed register breakpoint. Already breakpoint enabled at mediator position "
                                    + logMediatorPosition(breakPoint) + " for Proxy key " + breakPoint.getKey()
                                    + " Sequence " + breakPoint.getSynapseSequenceType().toString());
                        }
                        debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                                SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_BREAKPOINT_ENABLED)
                                .toString());
                    }
                } else {
                    if (((AbstractMediator) current_mediator).isBreakPoint()) {
                        ((AbstractMediator) current_mediator).setBreakPoint(false);
                        ((AbstractMediator) current_mediator).unregisterMediationFlowPoint();
                        if (log.isDebugEnabled()) {
                            log.debug("Unregistered breakpoint at mediator position "
                                    + logMediatorPosition(breakPoint)
                                    + " for Proxy key " + breakPoint.getKey() + " Sequence "
                                    + breakPoint.getSynapseSequenceType().toString());
                        }
                        debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(true, null).toString());

                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Failed unregister breakpoint. Already breakpoint disabled at mediator position "
                                    + logMediatorPosition(breakPoint)
                                    + " for Proxy key " + breakPoint.getKey() + " Sequence "
                                    + breakPoint.getSynapseSequenceType().toString());
                        }
                        debugManager.advertiseCommandResponse(debugManager
                                .createDebugCommandResponse(false, SynapseDebugCommandConstants
                                        .DEBUG_COMMAND_RESPONSE_ALREADY_BREAKPOINT_DISABLED).toString());
                    }
                }
            } else {
                if (registerMode) {
                    if (log.isDebugEnabled()) {
                        log.debug("Failed register breakpoint. Non existing mediator position at "
                                + logMediatorPosition(breakPoint)
                                + " for Proxy key " + breakPoint.getKey()
                                + " Sequence " + breakPoint.getSynapseSequenceType().toString());
                    }
                    debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                            SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION)
                            .toString());
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Failed unregister breakpoint. Non existing mediator position at "
                                + logMediatorPosition(breakPoint)
                                + " for Proxy key " + breakPoint.getKey()
                                + " Sequence " + breakPoint.getSynapseSequenceType().toString());
                    }
                    debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                            SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION)
                            .toString());
                }
            }
        } else {
            if (registerMode) {
                if (log.isDebugEnabled()) {
                    log.debug("Failed register breakpoint. Non existing sequence "
                            + breakPoint.getSynapseSequenceType().toString()
                            + " for Proxy key " + breakPoint.getKey());
                }
                debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                        SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_SEQUENCE)
                        .toString());
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Failed unregister breakpoint. Non existing sequence "
                            + breakPoint.getSynapseSequenceType().toString()
                            + " for Proxy key " + breakPoint.getKey());
                }
                debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                        SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_SEQUENCE)
                        .toString());
            }
        }
    }

    /**
     * Registers/Un-registers a skip, point where mediator disables from medation flow
     *
     * @param synCfg Synapse configuration
     * @param seqType Synapse sequence type
     * @param proxyKey name of the Proxy Service
     * @param position array of integers that uniquely specifies a point in mediation route
     * @param registerMode specify whether register or un register
     */
    public static void registerProxySequenceMediationFlowSkip(SynapseConfiguration synCfg,
                                                              String seqType,
                                                              String proxyKey,
                                                              int[] position,
                                                              boolean registerMode) {
        SynapseSequenceType synapseSequenceType = SynapseSequenceType.valueOf(seqType.toUpperCase());
        SequenceMediationFlowPoint skipPoint = new SequenceMediationFlowPoint();
        skipPoint.setSynapseMediationComponent(SynapseMediationComponent.SEQUENCE);
        skipPoint.setKey(proxyKey);
        skipPoint.setMediatorPosition(position);
        skipPoint.setSequenceBaseType(SynapseDebugCommandConstants.
                DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY);
        skipPoint.setSynapseSequenceType(synapseSequenceType);
        Mediator seqMediator = null;
        ProxyService proxy = null;
        proxy = synCfg.getProxyService(proxyKey);
        if (proxy != null) {
            if (synapseSequenceType.equals(SynapseSequenceType.PROXY_INSEQ)) {
                seqMediator = proxy.getTargetInLineInSequence();
            } else if (synapseSequenceType.equals(SynapseSequenceType.PROXY_OUTSEQ)) {
                seqMediator = proxy.getTargetInLineOutSequence();
            } else if (synapseSequenceType.equals(SynapseSequenceType.PROXY_FAULTSEQ)) {
                seqMediator = proxy.getTargetInLineFaultSequence();
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Non existing Proxy for key " + skipPoint.getKey());
            }
            debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                    SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_PROXY_NOT_FOUND).toString());
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
                            log.debug("Registered skip at mediator position " + logMediatorPosition(skipPoint)
                                    + " for Proxy key " + skipPoint.getKey() + " Sequence "
                                    + skipPoint.getSynapseSequenceType().toString());
                        }
                        debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(true, null)
                                .toString());
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Failed register skip. Already skip enabled at mediator position "
                                    + logMediatorPosition(skipPoint)
                                    + " for Proxy key " + skipPoint.getKey() + " Sequence "
                                    + skipPoint.getSynapseSequenceType().toString());
                        }
                        debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                                SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_SKIP_ENABLED).toString());
                    }
                } else {
                    if (((AbstractMediator) current_mediator).isSkipEnabled()) {
                        ((AbstractMediator) current_mediator).setSkipEnabled(false);
                        ((AbstractMediator) current_mediator).unregisterMediationFlowPoint();
                        if (log.isDebugEnabled()) {
                            log.debug("Unregistered skip at mediator position " + logMediatorPosition(skipPoint)
                                    + " for Proxy key " + skipPoint.getKey() + " Sequence "
                                    + skipPoint.getSynapseSequenceType().toString());
                        }
                        debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(true, null)
                                .toString());
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Failed unregister skip. Already skip disabled at mediator position "
                                    + logMediatorPosition(skipPoint)
                                    + " for Proxy key " + skipPoint.getKey() + " Sequence "
                                    + skipPoint.getSynapseSequenceType().toString());
                        }
                        debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                                SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_SKIP_DISABLED)
                                .toString());
                    }
                }
            } else {
                if (registerMode) {
                    if (log.isDebugEnabled()) {
                        log.debug("Failed register skip. Non existing mediator position at "
                                + logMediatorPosition(skipPoint)
                                + " for Proxy key " + skipPoint.getKey() + " Sequence "
                                + skipPoint.getSynapseSequenceType().toString());
                    }
                    debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                            SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION)
                            .toString());
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Failed unregister skip. Non existing mediator position at "
                                + logMediatorPosition(skipPoint)
                                + " for Proxy key " + skipPoint.getKey() + " Sequence "
                                + skipPoint.getSynapseSequenceType().toString());
                    }
                    debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                            SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION)
                            .toString());
                }
            }
        } else {
            if (registerMode) {
                if (log.isDebugEnabled()) {
                    log.debug("Failed register skip. Non existing sequence "
                            + skipPoint.getSynapseSequenceType().toString()
                            + " for Proxy key " + skipPoint.getKey());
                }
                debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                        SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_SEQUENCE).toString());

            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Failed unregister skip. Non existing sequence "
                            + skipPoint.getSynapseSequenceType().toString()
                            + " for Proxy key " + skipPoint.getKey());
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
