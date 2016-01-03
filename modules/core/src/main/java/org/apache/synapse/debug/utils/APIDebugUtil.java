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
import org.apache.synapse.debug.constants.SynapseDebugCommandConstants;
import org.apache.synapse.debug.SynapseDebugManager;
import org.apache.synapse.debug.constructs.SynapseMediationComponent;
import org.apache.synapse.debug.constructs.SynapseMediationFlowPoint;
import org.apache.synapse.debug.constructs.SynapseSequenceType;
import org.apache.synapse.debug.constructs.APIMediationFlowPoint;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.rest.API;
import org.apache.synapse.rest.Resource;

/**
 * Utility class that handle persisting API related breakpoint/skip information at mediator level as
 * we maintain debug related information at mediator level
 */
public class APIDebugUtil {

    private static SynapseDebugManager debugManager = SynapseDebugManager.getInstance();
    private static final Log log = LogFactory.getLog(APIDebugUtil.class);

    /**
     * Registers/Un-registers a breakpoint, point where mediation flow get suspended
     *
     * @param synCfg Synapse configuration
     * @param mapping either resource url-mapping or uri-template
     * @param method resource http method
     * @param sequenceType Synapse sequence type
     * @param apiKey name of the API
     * @param position array of integers that uniquely specifies a point in mediation route
     * @param registerMode specify whether register or un register
     */
    public static void registerAPISequenceMediationFlowBreakPoint(SynapseConfiguration synCfg,
                                                                  String mapping,
                                                                  String method,
                                                                  String sequenceType,
                                                                  String apiKey,
                                                                  int[] position,
                                                                  boolean registerMode) {
        SynapseSequenceType synapseSequenceType = SynapseSequenceType.valueOf(sequenceType.toUpperCase());
        APIMediationFlowPoint breakPoint = new APIMediationFlowPoint();
        breakPoint.setSynapseMediationComponent(SynapseMediationComponent.SEQUENCE);
        breakPoint.setKey(apiKey);
        breakPoint.setMediatorPosition(position);
        breakPoint.setSynapseSequenceType(synapseSequenceType);
        breakPoint.setSequenceBaseType(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API);
        breakPoint.setResourceMapping(mapping);
        breakPoint.setResourceHTTPMethod(method);
        Mediator seqMediator = null;
        Resource api_resource = null;
        API api = synCfg.getAPI(apiKey);
        if (api == null) {
            if (log.isDebugEnabled()) {
                log.debug("Non existing API for the key " + breakPoint.getKey());
            }
            debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                    SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_API_NOT_FOUND).toString());
            return;
        }
        Resource[] resource_array = api.getResources();
        for (int counter = 0; counter < resource_array.length; counter++) {
            if (resource_array[counter].getDispatcherHelper() != null && mapping != null) {
                if (mapping.equals(resource_array[counter].getDispatcherHelper().getString())) {
                    for (String m1 : resource_array[counter].getMethods()) {
                        if (m1.equals(method)) {
                            api_resource = resource_array[counter];
                            break;
                        }
                    }
                    if (api_resource != null) {
                        break;
                    }
                }
            } else if (resource_array[counter].getDispatcherHelper() == null && mapping == null) {
                for (String m1 : resource_array[counter].getMethods()) {
                    if (m1.equals(method)) {
                        api_resource = resource_array[counter];
                        break;
                    }
                }
                if (api_resource != null) {
                    break;
                }
            }
        }
        if (api_resource != null) {
            if (synapseSequenceType.equals(SynapseSequenceType.API_INSEQ)) {
                seqMediator = api_resource.getInSequence();
            } else if (synapseSequenceType.equals(SynapseSequenceType.API_OUTSEQ)) {
                seqMediator = api_resource.getOutSequence();
            } else if (synapseSequenceType.equals(SynapseSequenceType.API_FAULTSEQ)) {
                seqMediator = api_resource.getFaultSequence();
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Resource not found for the API key " + breakPoint.getKey()
                        + " resource mapping "
                        + breakPoint.getResourceMapping() + " resource HTTP method "
                        + breakPoint.getResourceHTTPMethod());
            }
            debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                    SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_API_RESOURCE_NOT_FOUND).toString());
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
                            log.info("Registered breakpoint at mediator position " +
                                    logMediatorPosition(breakPoint) +
                                    " for API key " + breakPoint.getKey() + " resource mapping " +
                                    breakPoint.getResourceMapping() +
                                    " resource HTTP method " + breakPoint.getResourceHTTPMethod() +
                                    " sequence type " + breakPoint.getSynapseSequenceType().toString());
                        }
                        debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(true, null)
                                .toString());
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Failed register breakpoint. Already breakpoint enabled at mediator position " +
                                    logMediatorPosition(breakPoint) +
                                    " for API key " + breakPoint.getKey() + " resource mapping " +
                                    breakPoint.getResourceMapping() +
                                    " resource HTTP method " + breakPoint.getResourceHTTPMethod() +
                                    " sequence type " + breakPoint.getSynapseSequenceType().toString());
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
                            log.debug("Unregistered breakpoint at mediator position " +
                                    logMediatorPosition(breakPoint) +
                                    " for API key " + breakPoint.getKey() + " resource mapping " +
                                    breakPoint.getResourceMapping() +
                                    " resource HTTP method " + breakPoint.getResourceHTTPMethod() +
                                    " sequence type " + breakPoint.getSynapseSequenceType().toString());
                        }
                        debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(true, null)
                                .toString());
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Failed unregister breakpoint. Already breakpoint disabled at mediator position " +
                                    logMediatorPosition(breakPoint) +
                                    " for API key " + breakPoint.getKey() + " resource mapping " +
                                    breakPoint.getResourceMapping() +
                                    " resource HTTP method " + breakPoint.getResourceHTTPMethod() +
                                    " sequence type " + breakPoint.getSynapseSequenceType().toString());
                        }
                        debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                                SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_BREAKPOINT_DISABLED)
                                .toString());
                    }
                }
            } else {
                if (registerMode) {
                    if (log.isDebugEnabled()) {
                        log.debug("Failed register breakpoint. Non existing mediator position at " +
                                logMediatorPosition(breakPoint) +
                                " for API key " + breakPoint.getKey() + " resource mapping " +
                                breakPoint.getResourceMapping() +
                                " resource HTTP method " + breakPoint.getResourceHTTPMethod() +
                                " sequence type " + breakPoint.getSynapseSequenceType().toString());
                    }
                    debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                            SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION)
                            .toString());
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Failed unregister breakpoint. Non existing mediator position at " +
                                logMediatorPosition(breakPoint) +
                                " for API key " + breakPoint.getKey() + " resource mapping " +
                                breakPoint.getResourceMapping() +
                                " resource HTTP method " + breakPoint.getResourceHTTPMethod() +
                                " sequence type " + breakPoint.getSynapseSequenceType().toString());
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
                            " for API key " + breakPoint.getKey() + " resource mapping " +
                            breakPoint.getResourceMapping() +
                            " resource HTTP method " + breakPoint.getResourceHTTPMethod());
                }
                debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                        SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_SEQUENCE).toString());
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Failed unregister breakpoint. Non existing sequence " +
                            breakPoint.getSynapseSequenceType().toString() +
                            " for API key " + breakPoint.getKey() + " resource mapping " +
                            breakPoint.getResourceMapping() +
                            " resource HTTP method " + breakPoint.getResourceHTTPMethod());
                }
                debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                        SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_SEQUENCE)
                        .toString());

            }
        }
    }

    /**
     * Registers/Un-registers a skip, point where mediator disables from the mediation flow
     *
     * @param synCfg Synapse configuration
     * @param mapping either resource url-mapping or uri-template
     * @param method resource http method
     * @param seqType Synapse sequence type
     * @param apiKey name of the API
     * @param position array of integers that uniquely specifies a point in mediation route
     * @param registerMode specify whether register or un register
     */
    public static void registerAPISequenceMediationFlowSkip(SynapseConfiguration synCfg,
                                                            String mapping,
                                                            String method,
                                                            String seqType,
                                                            String apiKey,
                                                            int[] position,
                                                            boolean registerMode) {
        SynapseSequenceType synapseSequenceType = SynapseSequenceType.valueOf(seqType.toUpperCase());
        APIMediationFlowPoint skipPoint = new APIMediationFlowPoint();
        skipPoint.setSynapseMediationComponent(SynapseMediationComponent.SEQUENCE);
        skipPoint.setKey(apiKey);
        skipPoint.setMediatorPosition(position);
        skipPoint.setSynapseSequenceType(synapseSequenceType);
        skipPoint.setSequenceBaseType(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API);
        skipPoint.setResourceMapping(mapping);
        skipPoint.setResourceHTTPMethod(method);
        Mediator seqMediator = null;
        Resource api_resource = null;
        API api = synCfg.getAPI(apiKey);
        if (api == null) {
            if (log.isDebugEnabled()) {
                log.debug("Non existing API for the key " + skipPoint.getKey());
            }
            debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                    SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_API_NOT_FOUND).toString());
            return;
        }
        Resource[] resource_array = api.getResources();
        for (int counter = 0; counter < resource_array.length; counter++) {
            if (resource_array[counter].getDispatcherHelper() != null && mapping != null) {
                if (mapping.equals(resource_array[counter].getDispatcherHelper().getString())) {
                    for (String m1 : resource_array[counter].getMethods()) {
                        if (m1.equals(method)) {
                            api_resource = resource_array[counter];
                            break;
                        }
                    }
                    if (api_resource != null) {
                        break;
                    }
                }
            } else if (resource_array[counter].getDispatcherHelper() == null && mapping == null) {
                for (String m1 : resource_array[counter].getMethods()) {
                    if (m1.equals(method)) {
                        api_resource = resource_array[counter];
                        break;
                    }
                }
                if (api_resource != null) {
                    break;
                }
            }
        }
        if (api_resource != null) {
            if (synapseSequenceType.equals(SynapseSequenceType.API_INSEQ)) {
                seqMediator = api_resource.getInSequence();
            } else if (synapseSequenceType.equals(SynapseSequenceType.API_OUTSEQ)) {
                seqMediator = api_resource.getOutSequence();
            } else if (synapseSequenceType.equals(SynapseSequenceType.API_FAULTSEQ)) {
                seqMediator = api_resource.getFaultSequence();
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Resource not found for the API key " + skipPoint.getKey()
                        + " resource mapping "
                        + skipPoint.getResourceMapping() + " resource HTTP method "
                        + skipPoint.getResourceHTTPMethod());
            }
            debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                    SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_API_RESOURCE_NOT_FOUND).toString());
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
                                    " for API key " + skipPoint.getKey() + " resource mapping "
                                    + skipPoint.getResourceMapping() +
                                    " resource HTTP method " + skipPoint.getResourceHTTPMethod() +
                                    " sequence type " + skipPoint.getSynapseSequenceType().toString());
                        }
                        debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(true, null)
                                .toString());
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Failed register skip. Already skip enabled at mediator position " +
                                    logMediatorPosition(skipPoint) + " for API key " + skipPoint.getKey()
                                    + " resource mapping " +
                                    skipPoint.getResourceMapping() + " resource HTTP method "
                                    + skipPoint.getResourceHTTPMethod() +
                                    " sequence type " + skipPoint.getSynapseSequenceType().toString());
                        }
                        debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                                SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_SKIP_ENABLED).toString());
                    }
                } else {
                    if (((AbstractMediator) current_mediator).isSkipEnabled()) {
                        ((AbstractMediator) current_mediator).setSkipEnabled(false);
                        ((AbstractMediator) current_mediator).unregisterMediationFlowPoint();
                        if (log.isDebugEnabled()) {
                            log.debug("Unregistered skip at mediator position " + logMediatorPosition(skipPoint) +
                                    " for API key " + skipPoint.getKey() + " resource mapping "
                                    + skipPoint.getResourceMapping() +
                                    " resource HTTP method " + skipPoint.getResourceHTTPMethod() +
                                    " sequence type " + skipPoint.getSynapseSequenceType().toString());
                        }
                        debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(true, null)
                                .toString());
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Failed unregister skip. Already skip disabled at mediator position "
                                    + logMediatorPosition(skipPoint) +
                                    " for API key " + skipPoint.getKey() + " resource mapping "
                                    + skipPoint.getResourceMapping() +
                                    " resource HTTP method " + skipPoint.getResourceHTTPMethod() +
                                    " sequence type " + skipPoint.getSynapseSequenceType().toString());
                        }
                        debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                                SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_SKIP_DISABLED).toString());
                    }
                }
            } else {
                if (registerMode) {
                    if (log.isDebugEnabled()) {
                        log.debug("Failed register skip. Non existing mediator position at "
                                + logMediatorPosition(skipPoint) +
                                " for API key " + skipPoint.getKey() + " resource mapping "
                                + skipPoint.getResourceMapping() +
                                " resource HTTP method " + skipPoint.getResourceHTTPMethod() +
                                " sequence type " + skipPoint.getSynapseSequenceType().toString());
                    }
                    debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                            SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION)
                            .toString());
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Failed unregister skip. Non existing mediator position at "
                                + logMediatorPosition(skipPoint) +
                                " for API key " + skipPoint.getKey() + " resource mapping "
                                + skipPoint.getResourceMapping() +
                                " resource HTTP method " + skipPoint.getResourceHTTPMethod() +
                                " sequence type " + skipPoint.getSynapseSequenceType().toString());
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
                            " for API key " + skipPoint.getKey() + " resource mapping " +
                            skipPoint.getResourceMapping() +
                            " resource HTTP method " + skipPoint.getResourceHTTPMethod());
                }
                debugManager.advertiseCommandResponse(debugManager.createDebugCommandResponse(false,
                        SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_SEQUENCE).toString());
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Failed unregister skip. Non existing sequence " +
                            skipPoint.getSynapseSequenceType().toString() +
                            " for API key " + skipPoint.getKey() + " resource mapping " +
                            skipPoint.getResourceMapping() +
                            " resource HTTP method " + skipPoint.getResourceHTTPMethod());
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
