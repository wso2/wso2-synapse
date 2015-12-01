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

package org.apache.synapse.debug;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPHeader;
import org.apache.axiom.soap.SOAPHeaderBlock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.debug.constants.SynapseDebugCommandConstants;
import org.apache.synapse.debug.constants.SynapseDebugEventConstants;
import org.apache.synapse.debug.constructs.APIMediationFlowPoint;
import org.apache.synapse.debug.constructs.ConnectorMediationFlowPoint;
import org.apache.synapse.debug.constructs.MediationFlowState;
import org.apache.synapse.debug.constructs.SequenceMediationFlowPoint;
import org.apache.synapse.debug.constructs.SynapseMediationComponent;
import org.apache.synapse.debug.constructs.SynapseMediationFlowPoint;
import org.apache.synapse.debug.utils.APIDebugUtil;
import org.apache.synapse.debug.utils.ConnectorDebugUtil;
import org.apache.synapse.debug.utils.InboundEndpointDebugUtil;
import org.apache.synapse.debug.utils.ProxyDebugUtil;
import org.apache.synapse.debug.utils.SequenceDebugUtil;
import org.apache.synapse.debug.utils.TemplateDebugUtil;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Main class that integrates mediation debugging capabilities to Synapse Engine, Debug Manager
 * single instance is created to handle debugging centrally, by either persisting or retrieving
 * debug related information in the mediator level.
 * Relies on SynapseDebugInterface to communicate over TCP channels for commands and events.
 */
public class SynapseDebugManager {
    /* to ensure a single mediation flow at a given time */
    private static volatile ReentrantLock mediationFlowLock;
    /* to ensure a synchronization between mediation flow suspension and resumption */
    public static volatile Semaphore mediationFlowSem;
    private MessageContext synCtx;
    private SynapseDebugInterface debugInterface = null;
    private static SynapseDebugManager debugManagerInstance = null;
    private static SynapseDebugTCPListener debugTCPListener = null;
    private SynapseConfiguration synCfg;
    private SynapseEnvironment synEnv;
    private MediationFlowState medFlowState = MediationFlowState.IDLE;
    private boolean initialised = false;
    private static final Log log = LogFactory.getLog(SynapseDebugManager.class);

    protected SynapseDebugManager() {
        mediationFlowLock = new ReentrantLock();
        mediationFlowSem = new Semaphore(0);
    }

    public static SynapseDebugManager getInstance() {
        if (debugManagerInstance == null) {
            debugManagerInstance = new SynapseDebugManager();
        }
        return debugManagerInstance;
    }

    public void setMessageContext(MessageContext synCtx) {
        this.synCtx = synCtx;
    }

    /**
     * Initializes the debug manager single instance.
     *
     * @param synCfg                    reference to Synapse configuration
     * @param debugInterface            reference to interface which environment communicates
     * @param synEnv                    reference to environment
     * @param startListenAsynchronously start interacting with interface asynchronously
     */
    public void init(SynapseConfiguration synCfg,
                     SynapseDebugInterface debugInterface,
                     SynapseEnvironment synEnv,
                     boolean startListenAsynchronously) {
        if (synEnv.isDebugEnabled()) {
            this.synCfg = synCfg;
            this.debugInterface = debugInterface;
            this.synEnv = synEnv;
            if (!initialised) {
                initialised = true;
                debugTCPListener = new SynapseDebugTCPListener(this, this.debugInterface);
                debugTCPListener.setDebugModeInProgress(true);
                if (startListenAsynchronously) {
                    debugTCPListener.start();
                }
                //spawns a Listener thread from the main thread that initializes synapse environment
                if (log.isDebugEnabled()) {
                    log.debug("Initialized with Synapse Configuration...");
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Updated Synapse Configuration...");
                }
                this.advertiseDebugEvent(this.createDebugEvent(
                        SynapseDebugEventConstants.DEBUG_EVENT_CONFIGURATION_UPDATED).toString());
            }
        }

    }

    /**
     * Acquiring hold on this lock make sure that only one mediation flow
     * is due inside mediation engine
     */
    public void acquireMediationFlowLock() {
        mediationFlowLock.lock();
    }

    /**
     * Releasing hold on this lock make sure that next mediation flow is started after
     * completion of the previous
     */
    public void releaseMediationFlowLock() {
        mediationFlowLock.unlock();
    }

    /**
     * Shutdown debug manager instance and close communication channels for event and command.
     */
    public void shutdownDebugManager() {
        if (synEnv.isDebugEnabled()) {
            debugInterface.closeConnection();
            debugTCPListener.shutDownListener();
        }
    }


    /**
     * Transit the mediation flow state to the SUSPENDED from previous UNKNOWN state
     * Transiting to SUSPENDED state will put the calling thread to sleep as sem.down() is called
     */
    public void transitMediationFlowStateToSuspended() {
        if (synEnv.isDebugEnabled()) {
            if (this.medFlowState == MediationFlowState.IDLE
                    || this.medFlowState == MediationFlowState.ACTIVE) {
                medFlowState = MediationFlowState.SUSPENDED;
                try {
                    mediationFlowSem.acquire();
                } catch (InterruptedException ex) {
                    log.error("Unable to suspend the mediation flow thread", ex);
                }
            }
        }
    }

    /**
     * Transit the mediation flow state to the ACTIVE from previous UNKNOWN state
     * Transiting to ACTIVE state will put the calling thread awakes as sem.up() is called
     */
    public void transitMediationFlowStateToActive() {
        if (synEnv.isDebugEnabled()) {
            if (this.medFlowState == MediationFlowState.SUSPENDED) {
                medFlowState = MediationFlowState.ACTIVE;
                mediationFlowSem.release();
            }
        }
    }

    /**
     * Related to advertising the point where mediation flow starts.
     *
     * @param synCtx message context
     */
    public void advertiseMediationFlowStartPoint(MessageContext synCtx) {
        if (synEnv.isDebugEnabled()) {
            setMessageContext(synCtx);
            this.advertiseDebugEvent(this.createDebugEvent
                    (SynapseDebugEventConstants.DEBUG_EVENT_STARTED).toString());
            if (log.isDebugEnabled()) {
                log.debug("Mediation flow started for id " + synCtx.getMessageID());
            }
        }
    }

    /**
     * related to advertising mediation flow terminating point to the communication channel
     *
     * @param synCtx message context
     */
    public void advertiseMediationFlowTerminatePoint(MessageContext synCtx) {
        if (synEnv.isDebugEnabled()) {
            this.advertiseDebugEvent(this.createDebugEvent(SynapseDebugEventConstants.DEBUG_EVENT_TERMINATED).toString());
            if (log.isDebugEnabled()) {
                log.debug("Mediation flow terminated for id " + synCtx.getMessageID());
            }
        }
    }

    /**
     * advertise a mediation skip to the communication channel
     *
     * @param skipPoint describes a unique point in the mediation flow
     * @param synCtx  message context
     */
    public void advertiseMediationFlowSkip(MessageContext synCtx, SynapseMediationFlowPoint skipPoint) {
        if (synEnv.isDebugEnabled() && debugInterface != null) {
            setMessageContext(synCtx);
            this.advertiseDebugEvent(this.createDebugMediationFlowPointHitEvent(false, skipPoint).toString());
            if (log.isDebugEnabled()) {
                log.debug("Mediation Flow skipped at " + logMediatorPosition(skipPoint));
            }
        }
    }

    /**
     * advertise a mediation breakpoint to the communication channel
     *
     * @param breakPoint describes a unique point in the mediation flow
     * @param synCtx message context
     */
    public void advertiseMediationFlowBreakPoint(MessageContext synCtx, SynapseMediationFlowPoint breakPoint) {
        if (synEnv.isDebugEnabled()) {
            setMessageContext(synCtx);
            this.advertiseDebugEvent(this.createDebugMediationFlowPointHitEvent(true, breakPoint).toString());
            if (log.isDebugEnabled()) {
                log.debug("Mediation flow suspended at " + logMediatorPosition(breakPoint));
            }
            this.transitMediationFlowStateToSuspended();
            this.advertiseDebugEvent(this.createDebugEvent(SynapseDebugEventConstants.DEBUG_EVENT_RESUMED_CLIENT).toString());
            if (log.isDebugEnabled()) {
                log.info("Mediation flow resumed from suspension at " + logMediatorPosition(breakPoint));
            }
        }
    }

    protected String logMediatorPosition(SynapseMediationFlowPoint flowPoint) {
        String log = "";
        String position = "";
        for (int count = 0; count < flowPoint.getMediatorPosition().length; count++) {
            if (count != 0) {
                position = position.concat("-->");
            }
            position = position.concat("(" +
                    String.valueOf(flowPoint.getMediatorPosition()[count]) + ")");
        }
        log = log.concat("mediator position " + position);
        if (flowPoint instanceof SequenceMediationFlowPoint) {
            log = log.concat(" " + ((SequenceMediationFlowPoint) flowPoint).getSequenceBaseType()
                    .toString() + " " + flowPoint.getKey());
            log = log.concat(" sequence " + ((SequenceMediationFlowPoint) flowPoint).getSynapseSequenceType()
                    .toString().toLowerCase());
        } else {
            log = log.concat(" " + flowPoint.getSynapseMediationComponent().toString().toLowerCase() + " " +
                    flowPoint.getKey());
        }
        return log;
    }

    /**
     * handles main command processing in using line of string received from the command channel
     * registering/un registering breakpoints and skips as well as mediation level data acquire or set
     * strings are expected to be JSON over defined protocol
     *
     * @param debug_line string in JSON format which is communicated via command channel
     */
    public void processDebugCommand(String debug_line) {
        try {
            JSONObject parsed_debug_line = new JSONObject(debug_line);
            String command = "";
            if (parsed_debug_line.has(SynapseDebugCommandConstants.DEBUG_COMMAND)) {
                command = parsed_debug_line.getString(SynapseDebugCommandConstants.DEBUG_COMMAND);
            } else {
                return;
            }
            if (command.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_CLEAR)) {
                String skipOrBreakPointOrProperty = parsed_debug_line
                        .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_ARGUMENT);
                if (skipOrBreakPointOrProperty.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY)) {
                    String propertyContext = parsed_debug_line
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT);
                    JSONObject property_arguments = parsed_debug_line
                            .getJSONObject(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY);
                    this.addMediationFlowPointProperty(propertyContext, property_arguments, false);
                } else {
                    String mediation_component = parsed_debug_line
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT);
                    JSONObject med_component_arguments = parsed_debug_line
                            .getJSONObject(mediation_component);
                    if (skipOrBreakPointOrProperty.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_BREAKPOINT)) {
                        this.registerMediationFlowPoint(mediation_component, med_component_arguments, true, false);
                    } else if (skipOrBreakPointOrProperty.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_SKIP)) {
                        this.registerMediationFlowPoint(mediation_component, med_component_arguments, false, false);
                    }
                }
            } else if (command.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_GET)) {
                String propertyOrProperties = parsed_debug_line
                        .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_ARGUMENT);
                String propertyContext = parsed_debug_line
                        .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT);
                JSONObject property_arguments = null;
                if (propertyOrProperties.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY)) {
                    property_arguments = parsed_debug_line
                            .getJSONObject(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY);
                }
                this.acquireMediationFlowPointProperties(propertyOrProperties, propertyContext, property_arguments);
            } else if (command.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_RESUME)) {
                this.debugResume();
            } else if (command.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_SET)) {
                String skipOrBreakPointOrProperty = parsed_debug_line
                        .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_ARGUMENT);
                if (skipOrBreakPointOrProperty.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY)) {
                    String propertyContext = parsed_debug_line
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT);
                    JSONObject property_arguments = parsed_debug_line
                            .getJSONObject(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY);
                    this.addMediationFlowPointProperty(propertyContext, property_arguments, true);
                } else {
                    String mediation_component = parsed_debug_line
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT);
                    JSONObject med_component_arguments = parsed_debug_line
                            .getJSONObject(mediation_component);
                    if (skipOrBreakPointOrProperty.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_BREAKPOINT)) {
                        this.registerMediationFlowPoint(mediation_component, med_component_arguments, true, true);
                    } else if (skipOrBreakPointOrProperty.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_SKIP)) {
                        this.registerMediationFlowPoint(mediation_component, med_component_arguments, false, true);
                    }
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Debug command not found");
                }
                this.advertiseCommandResponse(createDebugCommandResponse(false,
                        SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_COMMAND_NOT_FOUND).toString());
            }
        } catch (JSONException ex) {
            log.error("Unable to process debug command", ex);
        }
    }

    /**
     * handles registering/un registering breakpoints and skips as well as mediation level data acquire or set
     *
     * @param mediation_component     sequence connector or either template
     * @param med_component_arguments defines mediation component
     * @param isBreakpoint            either breakpoint or skip
     * @param registerMode            either register or un register
     */
    public void registerMediationFlowPoint(String mediation_component,
                                           JSONObject med_component_arguments,
                                           boolean isBreakpoint,
                                           boolean registerMode) {
        try {
            if (mediation_component.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_CONNECTOR)) {
                String connector_key = med_component_arguments
                        .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_CONNECTOR_KEY);
                String connector_method_name = med_component_arguments
                        .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_CONNECTOR_METHOD);
                String component_mediator_position = med_component_arguments
                        .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION);
                String[] mediator_position_array = component_mediator_position.split("\\s+");
                int[] med_pos = new int[mediator_position_array.length];
                for (int counter = 0; counter < mediator_position_array.length; counter++) {
                    med_pos[counter] = Integer.valueOf(mediator_position_array[counter]);
                }
                if (isBreakpoint) {
                    ConnectorDebugUtil.registerConnectorMediationFlowBreakPoint(synCfg, connector_key,
                            connector_method_name, med_pos, registerMode);
                } else {
                    ConnectorDebugUtil.registerConnectorMediationFlowSkip(synCfg, connector_key,
                            connector_method_name, med_pos, registerMode);
                }

            } else if (mediation_component.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE)) {
                if ((!med_component_arguments.has(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY))
                        && (!med_component_arguments.has(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API))
                        && (!med_component_arguments.has(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_INBOUND))) {
                    String sequence_key = med_component_arguments
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_KEY);
                    String sequence_type = med_component_arguments
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_TYPE);
                    String component_mediator_position = med_component_arguments
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION);
                    String[] mediator_position_array = component_mediator_position.split("\\s+");
                    int[] med_pos = new int[mediator_position_array.length];
                    for (int counter = 0; counter < mediator_position_array.length; counter++) {
                        med_pos[counter] = Integer.valueOf(mediator_position_array[counter]);
                    }
                    if (isBreakpoint) {
                        SequenceDebugUtil.registerSequenceMediationFlowBreakPoint(synCfg, sequence_type,
                                sequence_key, med_pos, registerMode);
                    } else {
                        SequenceDebugUtil.registerSequenceMediationFlowSkip(synCfg, sequence_type,
                                sequence_key, med_pos, registerMode);
                    }
                } else if (med_component_arguments.has(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY)) {
                    JSONObject proxy_arguments = med_component_arguments
                            .getJSONObject(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY);
                    String proxy_key = proxy_arguments
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY_KEY);
                    String sequence_type = proxy_arguments
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_TYPE);
                    String component_mediator_position = proxy_arguments
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION);
                    String[] mediator_position_array = component_mediator_position.split("\\s+");
                    int[] med_pos = new int[mediator_position_array.length];
                    for (int counter = 0; counter < mediator_position_array.length; counter++) {
                        med_pos[counter] = Integer.valueOf(mediator_position_array[counter]);
                    }
                    if (isBreakpoint) {
                        ProxyDebugUtil.registerProxySequenceMediationFlowBreakPoint(synCfg, sequence_type,
                                proxy_key, med_pos, registerMode);
                    } else {
                        ProxyDebugUtil.registerProxySequenceMediationFlowSkip(synCfg, sequence_type,
                                proxy_key, med_pos, registerMode);
                    }

                } else if (med_component_arguments.has(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_INBOUND)) {
                    JSONObject inbound_arguments = med_component_arguments
                            .getJSONObject(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_INBOUND);
                    String inbound_key = inbound_arguments
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_INBOUND_KEY);
                    String sequence_type = inbound_arguments
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_TYPE);
                    String component_mediator_position = inbound_arguments
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION);
                    String[] mediator_position_array = component_mediator_position.split("\\s+");
                    int[] med_pos = new int[mediator_position_array.length];
                    for (int counter = 0; counter < mediator_position_array.length; counter++) {
                        med_pos[counter] = Integer.valueOf(mediator_position_array[counter]);
                    }
                    if (isBreakpoint) {
                        InboundEndpointDebugUtil.registerInboundSequenceMediationFlowBreakPoint(synCfg, sequence_type,
                                inbound_key, med_pos, registerMode);
                    } else {
                        InboundEndpointDebugUtil.registerInboundSequenceMediationFlowSkip(synCfg, sequence_type,
                                inbound_key, med_pos, registerMode);
                    }

                } else if (med_component_arguments.has(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API)) {
                    JSONObject api_arguments = med_component_arguments
                            .getJSONObject(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API);
                    JSONObject resource_arguments = api_arguments
                            .getJSONObject(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE);
                    String mapping = null;
                    if (resource_arguments.has(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE_URI_TEMPLATE)) {
                        mapping = resource_arguments
                                .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE_URI_TEMPLATE);
                    } else if (resource_arguments.has(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE_URL_MAPPING)) {
                        mapping = resource_arguments
                                .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE_URL_MAPPING);
                    }
                    String method = resource_arguments
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE_METHOD);
                    String api_key = api_arguments
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_KEY);
                    String sequence_type = api_arguments
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_TYPE);
                    String component_mediator_position = api_arguments
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION);
                    String[] mediator_position_array = component_mediator_position.split("\\s+");
                    int[] med_pos = new int[mediator_position_array.length];
                    for (int counter = 0; counter < mediator_position_array.length; counter++) {
                        med_pos[counter] = Integer.valueOf(mediator_position_array[counter]);
                    }
                    if (isBreakpoint) {
                        APIDebugUtil.registerAPISequenceMediationFlowBreakPoint(synCfg, mapping, method,
                                sequence_type, api_key, med_pos, registerMode);
                    } else {
                        APIDebugUtil.registerAPISequenceMediationFlowSkip(synCfg, mapping, method,
                                sequence_type, api_key, med_pos, registerMode);
                    }
                }

            } else if (mediation_component.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_TEMPLATE)) {
                String template_key = med_component_arguments
                        .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_TEMPLATE_KEY);
                String component_mediator_position = med_component_arguments
                        .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION);
                String[] mediator_position_array = component_mediator_position.split("\\s+");
                int[] med_pos = new int[mediator_position_array.length];
                for (int counter = 0; counter < mediator_position_array.length; counter++) {
                    med_pos[counter] = Integer.valueOf(mediator_position_array[counter]);
                }
                if (isBreakpoint) {
                    TemplateDebugUtil.registerTemplateMediationFlowBreakPoint(synCfg, template_key,
                            med_pos, registerMode);
                } else {
                    TemplateDebugUtil.registerTemplateMediationFlowSkip(synCfg, template_key,
                            med_pos, registerMode);
                }
            }
        } catch (JSONException ex) {
            log.error("Unable to register mediation flow point", ex);
            this.advertiseCommandResponse(createDebugCommandResponse(false,
                    SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_UNABLE_TO_REGISTER_FLOW_POINT).toString());
        }
    }

    public void advertiseCommandResponse(String commandResponse) {
        if (synEnv.isDebugEnabled()) {
            debugInterface.getPortListenWriter().println(commandResponse);
            debugInterface.getPortListenWriter().flush();
        }
    }

    public void advertiseDebugEvent(String event) {
        if (synEnv.isDebugEnabled()) {
            debugInterface.getPortSendWriter().println(event);
            debugInterface.getPortSendWriter().flush();
        }
    }

    public void debugResume() {
        this.transitMediationFlowStateToActive();
        this.advertiseCommandResponse(createDebugCommandResponse(true, null).toString());
    }

    public JSONObject createDebugCommandResponse(boolean isPositive, String failedReason) {
        JSONObject response = null;
        response = new JSONObject();
        try {
            if (isPositive) {
                response.put(SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE,
                        SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_SUCCESSFUL);
            } else {
                response.put(SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE,
                        SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_FAILED);
                response.put(SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_FAILED_REASON, failedReason);
            }
        } catch (JSONException e) {
            log.error("Unable to advertise command response", e);
        }
        return response;
    }

    public JSONObject createDebugMediationFlowPointHitEvent(boolean isBreakpoint, SynapseMediationFlowPoint point) {
        JSONObject event = null;

        try {
            event = new JSONObject();
            if (isBreakpoint) {
                event.put(SynapseDebugEventConstants.DEBUG_EVENT, SynapseDebugEventConstants.DEBUG_EVENT_BREAKPOINT);
            } else {
                event.put(SynapseDebugEventConstants.DEBUG_EVENT, SynapseDebugEventConstants.DEBUG_EVENT_SKIP);
            }
            JSONObject parameters = new JSONObject();
            if (point.getSynapseMediationComponent().equals(SynapseMediationComponent.SEQUENCE)) {
                event.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT,
                        SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE);

                if (((SequenceMediationFlowPoint) point).getSequenceBaseType()
                        .equals(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE)) {
                    parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_KEY,
                            point.getKey());
                    parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_TYPE,
                            ((SequenceMediationFlowPoint) point).getSynapseSequenceType().toString().toLowerCase());
                    parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION,
                            toString(point.getMediatorPosition()));
                    event.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE, parameters);
                } else if (((SequenceMediationFlowPoint) point).getSequenceBaseType()
                        .equals(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY)) {
                    JSONObject proxy_parameters = new JSONObject();
                    proxy_parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY_KEY,
                            point.getKey());
                    proxy_parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_TYPE,
                            ((SequenceMediationFlowPoint) point).getSynapseSequenceType().toString().toLowerCase());
                    proxy_parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION,
                            toString(point.getMediatorPosition()));
                    parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY, proxy_parameters);
                    event.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE, parameters);
                } else if (((SequenceMediationFlowPoint) point).getSequenceBaseType()
                        .equals(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_INBOUND)) {
                    JSONObject inbound_parameters = new JSONObject();
                    inbound_parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_INBOUND_KEY,
                            point.getKey());
                    inbound_parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_TYPE,
                            ((SequenceMediationFlowPoint) point).getSynapseSequenceType().toString().toLowerCase());
                    inbound_parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION,
                            toString(point.getMediatorPosition()));
                    parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_INBOUND, inbound_parameters);
                    event.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE, parameters);
                } else if (((SequenceMediationFlowPoint) point).getSequenceBaseType()
                        .equals(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API)) {
                    JSONObject api_parameters = new JSONObject();
                    api_parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_KEY,
                            point.getKey());
                    JSONObject resource = new JSONObject();
                    resource.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE_MAPPING,
                            ((APIMediationFlowPoint) point).getResourceMapping());
                    resource.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE_METHOD,
                            ((APIMediationFlowPoint) point).getResourceHTTPMethod());
                    api_parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE, resource);
                    api_parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_TYPE,
                            ((SequenceMediationFlowPoint) point).getSynapseSequenceType().toString().toLowerCase());
                    api_parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION,
                            toString(point.getMediatorPosition()));
                    parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API, api_parameters);
                    event.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE, parameters);
                }
            } else if (point.getSynapseMediationComponent().equals(SynapseMediationComponent.TEMPLATE)) {
                event.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT,
                        SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_TEMPLATE);
                parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_TEMPLATE_KEY,
                        point.getKey());
                parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION,
                        toString(point.getMediatorPosition()));
                event.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_TEMPLATE, parameters);
            } else if (point.getSynapseMediationComponent().equals(SynapseMediationComponent.CONNECTOR)) {
                event.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT,
                        SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_CONNECTOR);
                parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_CONNECTOR_KEY, point.getKey());
                parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_CONNECTOR_METHOD,
                        ((ConnectorMediationFlowPoint) point).getConnectorMediationComponentMethod());
                parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION,
                        toString(point.getMediatorPosition()));
                event.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_CONNECTOR, parameters);
            }
        } catch (JSONException ex) {
            log.error("Failed to create debug event in JSON format", ex);
        }
        return event;
    }

    public JSONObject createDebugEvent(String eventString) {
        JSONObject event = null;
        try {
            event = new JSONObject();
            event.put(SynapseDebugEventConstants.DEBUG_EVENT, eventString);
        } catch (JSONException ex) {
            log.error("Failed to create debug event in JSON format", ex);
        }
        return event;
    }

    protected String toString(int[] position) {
        String positionString = "";
        for (int counter = 0; counter < position.length; counter++) {
            positionString = positionString.concat(String.valueOf(position[counter])).concat(" ");
        }
        return positionString;
    }

    public void acquireMediationFlowPointProperties(String propertyOrProperties,
                                                    String propertyContext,
                                                    JSONObject property_arguments) {
        try {
            if (propertyOrProperties.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTIES)) {
                if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_ALL)) {
                    JSONObject data_axis2 = getAxis2Properties();
                    JSONObject data_synapse = new JSONObject(((Axis2MessageContext) synCtx).getProperties());
                    JSONObject data_axis2_prop = new JSONObject();
                    JSONObject data_synapse_prop = new JSONObject();
                    data_axis2_prop.put(SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_PROPERTY_CONTEXT_AXIS2, data_axis2);
                    data_synapse_prop.put(SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_PROPERTY_CONTEXT_SYNAPSE, data_synapse);
                    JSONArray data_array = new JSONArray();
                    data_array.put(data_axis2_prop);
                    data_array.put(data_synapse_prop);
                    debugInterface.getPortListenWriter().println(data_array.toString());
                    debugInterface.getPortListenWriter().flush();
                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2)) {
                    JSONObject data_axis2 = getAxis2Properties();
                    JSONObject data_axis2_prop = new JSONObject();
                    data_axis2_prop.put(SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_PROPERTY_CONTEXT_AXIS2, data_axis2);
                    debugInterface.getPortListenWriter().println(data_axis2_prop.toString());
                    debugInterface.getPortListenWriter().flush();
                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_SYNAPSE)
                        || propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_DEFAULT)) {
                    JSONObject data_synapse = new JSONObject(((Axis2MessageContext) synCtx).getProperties());
                    JSONObject data_synapse_prop = new JSONObject();
                    data_synapse_prop.put(SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_PROPERTY_CONTEXT_SYNAPSE, data_synapse);
                    debugInterface.getPortListenWriter().println(data_synapse_prop.toString());
                    debugInterface.getPortListenWriter().flush();
                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2CLIENT)) {
                    JSONObject data_axis2 = new JSONObject(((Axis2MessageContext) synCtx).getAxis2MessageContext().getOptions().getProperties());
                    JSONObject data_axis2_prop = new JSONObject();
                    data_axis2_prop.put(SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_PROPERTY_CONTEXT_AXIS2CLIENT, data_axis2);
                    debugInterface.getPortListenWriter().println(data_axis2_prop.toString());
                    debugInterface.getPortListenWriter().flush();
                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_TRANSPORT)) {
                    JSONObject data_axis2 = new JSONObject((Map) ((Axis2MessageContext) synCtx).getAxis2MessageContext().getProperty(
                            org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS));
                    JSONObject data_axis2_prop = new JSONObject();
                    data_axis2_prop.put(SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_PROPERTY_CONTEXT_AXIS2TRANSPORT, data_axis2);
                    debugInterface.getPortListenWriter().println(data_axis2_prop.toString());
                    debugInterface.getPortListenWriter().flush();
                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_OPERATION)) {
                    JSONObject data_axis2 = new JSONObject(((Axis2MessageContext) synCtx).getAxis2MessageContext().getOperationContext().getProperties());
                    JSONObject data_axis2_prop = new JSONObject();
                    data_axis2_prop.put(SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_PROPERTY_CONTEXT_AXIS2OPERATION, data_axis2);
                    debugInterface.getPortListenWriter().println(data_axis2_prop.toString());
                    debugInterface.getPortListenWriter().flush();
                }
            } else if (propertyOrProperties.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY)) {
                if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2)) {
                    JSONObject data_axis2 = getAxis2Properties();
                    Object result = null;
                    if (data_axis2.has(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME))) {
                        result = data_axis2.get(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME));
                    }
                    JSONObject json_result = new JSONObject();
                    json_result.put(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME), result);
                    debugInterface.getPortListenWriter().println(json_result.toString());
                    debugInterface.getPortListenWriter().flush();

                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_SYNAPSE)
                        || propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_DEFAULT)) {
                    JSONObject data_synapse = new JSONObject(((Axis2MessageContext) synCtx).getProperties());
                    Object result = null;
                    if (data_synapse.has(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME))) {
                        result = data_synapse.getJSONObject(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME));
                    }
                    JSONObject json_result = new JSONObject();
                    json_result.put(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME), result);
                    debugInterface.getPortListenWriter().println(json_result.toString());
                    debugInterface.getPortListenWriter().flush();

                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2CLIENT)) {
                    JSONObject data_axis2 = new JSONObject(((Axis2MessageContext) synCtx).getAxis2MessageContext().getOptions().getProperties());
                    Object result = null;
                    if (data_axis2.has(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME))) {
                        result = data_axis2.get(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME));
                    }
                    JSONObject json_result = new JSONObject();
                    json_result.put(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME), result);
                    debugInterface.getPortListenWriter().println(json_result.toString());
                    debugInterface.getPortListenWriter().flush();

                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_TRANSPORT)) {
                    JSONObject data_axis2 = new JSONObject((Map) ((Axis2MessageContext) synCtx).getAxis2MessageContext().getProperty(
                            org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS));
                    Object result = null;
                    if (data_axis2.has(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME))) {
                        result = data_axis2.get(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME));
                    }
                    JSONObject json_result = new JSONObject();
                    json_result.put(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME), result);
                    debugInterface.getPortListenWriter().println(json_result.toString());
                    debugInterface.getPortListenWriter().flush();

                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_OPERATION)) {
                    JSONObject data_axis2 = new JSONObject(((Axis2MessageContext) synCtx).getAxis2MessageContext().getOperationContext().getProperties());
                    Object result = null;
                    if (data_axis2.has(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME))) {
                        result = data_axis2.get(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME));
                    }
                    JSONObject json_result = new JSONObject();
                    json_result.put(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME), result);
                    debugInterface.getPortListenWriter().println(json_result.toString());
                    debugInterface.getPortListenWriter().flush();
                }
            }
        } catch (JSONException ex) {
            log.error("Failed to acquire property in the scope: " + propertyContext, ex);
        }
    }

    protected JSONObject getAxis2Properties() throws JSONException {
        JSONObject result = new JSONObject();
        result.put(SynapseDebugCommandConstants.AXIS2_PROPERTY_TO,
                synCtx.getTo() != null ? synCtx.getTo().getAddress() : "");
        result.put(SynapseDebugCommandConstants.AXIS2_PROPERTY_FROM,
                synCtx.getFrom() != null ? synCtx.getFrom().getAddress() : "");
        result.put(SynapseDebugCommandConstants.AXIS2_PROPERTY_WSACTION,
                synCtx.getWSAAction() != null ? synCtx.getWSAAction() : "");
        result.put(SynapseDebugCommandConstants.AXIS2_PROPERTY_SOAPACTION,
                synCtx.getSoapAction() != null ? synCtx.getSoapAction() : "");
        result.put(SynapseDebugCommandConstants.AXIS2_PROPERTY_REPLY_TO,
                synCtx.getReplyTo() != null ? synCtx.getReplyTo().getAddress() : "");
        result.put(SynapseDebugCommandConstants.AXIS2_PROPERTY_MESSAGE_ID,
                synCtx.getMessageID() != null ? synCtx.getMessageID() : "");
        result.put(SynapseDebugCommandConstants.AXIS2_PROPERTY_DIRECTION,
                synCtx.isResponse() ? "response" : "request");
        result.put(SynapseDebugCommandConstants.AXIS2_PROPERTY_ENVELOPE,
                synCtx.getEnvelope() != null ? synCtx.getEnvelope().toString() : "");
        JSONObject soapHeader = new JSONObject();
        if (synCtx.getEnvelope() != null) {
            SOAPHeader header = synCtx.getEnvelope().getHeader();
            if (header != null) {
                for (Iterator iter = header.examineAllHeaderBlocks(); iter.hasNext(); ) {
                    Object o = iter.next();
                    if (o instanceof SOAPHeaderBlock) {
                        SOAPHeaderBlock headerBlk = (SOAPHeaderBlock) o;
                        soapHeader.put(headerBlk.getLocalName(), headerBlk.getText());
                    } else if (o instanceof OMElement) {
                        OMElement headerElem = (OMElement) o;
                        soapHeader.put(headerElem.getLocalName(), headerElem.getText());

                    }
                }
            }
        }
        result.put(SynapseDebugCommandConstants.AXIS2_PROPERTY_SOAPHEADER, soapHeader);
        JSONObject transportHeader = new JSONObject((Map) ((Axis2MessageContext) synCtx)
                .getAxis2MessageContext().getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS));
        result.put(SynapseDebugCommandConstants.AXIS2_PROPERTY_TRANSPORT_HEADERS,
                transportHeader);
        result.put(SynapseDebugCommandConstants.AXIS2_PROPERTY_EXCESS_TRANSPORT_HEADERS,
                ((Axis2MessageContext) synCtx).getAxis2MessageContext().getProperty("EXCESS_TRANSPORT_HEADERS"));
        result.put(SynapseDebugCommandConstants.AXIS2_PROPERTY_MESSAGE_TYPE,
                ((Axis2MessageContext) synCtx).getAxis2MessageContext().getProperty("messageType"));
        result.put(SynapseDebugCommandConstants.AXIS2_PROPERTY_CONTENT_TYPE,
                ((Axis2MessageContext) synCtx).getAxis2MessageContext().getProperty("ContentType"));
        return result;

    }

    public void addMediationFlowPointProperty(String propertyContext,
                                              JSONObject property_arguments,
                                              boolean isActionSet) {
        try {
            String property_key = property_arguments
                    .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME);
            String property_value = property_arguments
                    .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_VALUE);
            if (isActionSet) {
                if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_DEFAULT)
                        || propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_SYNAPSE)) {
                    synCtx.setProperty(property_key, property_value);

                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2)
                        && synCtx instanceof Axis2MessageContext) {
                    Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                    org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
                    axis2MessageCtx.setProperty(property_key, property_value);
                    if (org.apache.axis2.Constants.Configuration.MESSAGE_TYPE.equals(property_key)) {
                        axis2MessageCtx.setProperty(org.apache.axis2.Constants.Configuration.CONTENT_TYPE, property_value);
                        Object o = axis2MessageCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
                        Map headers = (Map) o;
                        if (headers != null) {
                            headers.remove(HTTP.CONTENT_TYPE);
                            headers.put(HTTP.CONTENT_TYPE, property_value);
                        }
                    }
                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2CLIENT)
                        && synCtx instanceof Axis2MessageContext) {
                    Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                    org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
                    axis2MessageCtx.getOptions().setProperty(property_key, property_value);
                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_TRANSPORT)
                        && synCtx instanceof Axis2MessageContext) {
                    Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                    org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
                    Object headers = axis2MessageCtx
                            .getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
                    if (headers != null && headers instanceof Map) {
                        Map headersMap = (Map) headers;
                        headersMap.put(property_key, property_value);
                    }
                    if (headers == null) {
                        Map headersMap = new HashMap();
                        headersMap.put(property_key, property_value);
                        axis2MessageCtx
                                .setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, headersMap);
                    }
                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_OPERATION)
                        && synCtx instanceof Axis2MessageContext) {
                    Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                    axis2smc.getAxis2MessageContext().getOperationContext().setProperty(property_key, property_value);
                }
            } else {
                if (propertyContext == null || XMLConfigConstants.SCOPE_DEFAULT.equals(propertyContext)) {
                    Set pros = synCtx.getPropertyKeySet();
                    if (pros != null) {
                        pros.remove(property_key);
                    }
                } else if ((propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2) ||
                        propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2CLIENT))
                        && synCtx instanceof Axis2MessageContext) {
                    Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                    org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
                    axis2MessageCtx.removeProperty(property_key);
                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_TRANSPORT)
                        && synCtx instanceof Axis2MessageContext) {
                    Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                    org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
                    Object headers = axis2MessageCtx
                            .getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
                    if (headers != null && headers instanceof Map) {
                        Map headersMap = (Map) headers;
                        headersMap.remove(property_key);
                    }
                }
            }
        } catch (JSONException e) {
            log.error("Failed to set or remove property in the scope " + propertyContext, e);
        }
        this.advertiseCommandResponse(createDebugCommandResponse(true, null).toString());
    }

}
