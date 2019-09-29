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
import org.apache.axis2.addressing.EndpointReference;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.config.SynapseConfiguration;
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
import org.apache.synapse.transport.http.conn.SynapseBackEndWireLogs;
import org.apache.synapse.transport.http.conn.SynapseDebugInfoHolder;
import org.apache.synapse.transport.http.conn.SynapseWireLogHolder;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Main class that integrates mediation debugging capabilities to Synapse Engine, Debug Manager
 * single instance is created to handle debugging centrally, by either persisting or retrieving
 * debug related information in the mediator level.
 * Relies on SynapseDebugInterface to communicate over TCP channels for commands and events.
 */
public class SynapseDebugManager implements Observer {
    private static final java.lang.String METHOD_ARRAY_SEPERATOR = ",";
    private static final String EMPTY_STRING = "";
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
    private Map addedPropertyValuesMap;

    protected SynapseDebugManager() {
        mediationFlowLock = new ReentrantLock();
        mediationFlowSem = new Semaphore(0);
        addedPropertyValuesMap = new HashMap<MessageContext, Map<String, Set<String>>>();
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
        if (synEnv.isDebuggerEnabled()) {
            this.synCfg = synCfg;
            this.debugInterface = debugInterface;
            this.synEnv = synEnv;
            SynapseDebugInfoHolder.getInstance().setDebuggerEnabled(true);
            SynapseDebugInfoHolder.getInstance().addObserver(this);
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
                this.advertiseDebugEvent(
                        this.createDebugEvent(SynapseDebugEventConstants.DEBUG_EVENT_CONFIGURATION_UPDATED).toString());
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
        if (synEnv.isDebuggerEnabled()) {
            debugInterface.closeConnection();
            debugTCPListener.shutDownListener();
        }
    }

    /**
     * Transit the mediation flow state to the SUSPENDED from previous UNKNOWN state
     * Transiting to SUSPENDED state will put the calling thread to sleep as sem.down() is called
     */
    public void transitMediationFlowStateToSuspended() {
        if (synEnv.isDebuggerEnabled()) {
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
        if (synEnv.isDebuggerEnabled()) {
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
        if (synEnv.isDebuggerEnabled()) {
            setMessageContext(synCtx);
            this.advertiseDebugEvent(this.createDebugEvent(SynapseDebugEventConstants.DEBUG_EVENT_STARTED).toString());
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
        if (synEnv.isDebuggerEnabled()) {
            this.advertiseDebugEvent(
                    this.createDebugEvent(SynapseDebugEventConstants.DEBUG_EVENT_TERMINATED).toString());
            if (log.isDebugEnabled()) {
                log.debug("Mediation flow terminated for id " + synCtx.getMessageID());
            }
            String axis2ContextKey = getAxis2MessagePropertiesKey(
                    ((Axis2MessageContext) synCtx).getAxis2MessageContext());
            if (addedPropertyValuesMap.containsKey(axis2ContextKey)) {
                addedPropertyValuesMap.remove(axis2ContextKey);
            }
        }
    }

    /**
     * advertise a mediation skip to the communication channel
     *
     * @param skipPoint describes a unique point in the mediation flow
     * @param synCtx    message context
     */
    public void advertiseMediationFlowSkip(MessageContext synCtx, SynapseMediationFlowPoint skipPoint) {
        if (synEnv.isDebuggerEnabled() && debugInterface != null) {
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
     * @param synCtx     message context
     */
    public void advertiseMediationFlowBreakPoint(MessageContext synCtx, SynapseMediationFlowPoint breakPoint) {
        if (synEnv.isDebuggerEnabled()) {
            setMessageContext(synCtx);
            this.advertiseDebugEvent(this.createDebugMediationFlowPointHitEvent(true, breakPoint).toString());
            if (log.isDebugEnabled()) {
                log.debug("Mediation flow suspended at " + logMediatorPosition(breakPoint));
            }
            this.transitMediationFlowStateToSuspended();
            this.advertiseDebugEvent(
                    this.createDebugEvent(SynapseDebugEventConstants.DEBUG_EVENT_RESUMED_CLIENT).toString());
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
            log = log.concat(" " + ((SequenceMediationFlowPoint) flowPoint).getSequenceBaseType().toString() + " "
                    + flowPoint.getKey());
            log = log.concat(" sequence " + ((SequenceMediationFlowPoint) flowPoint).getSynapseSequenceType().toString()
                    .toLowerCase());
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
    public void processDebugCommand(String debug_line) throws IOException {
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
                    JSONObject med_component_arguments = parsed_debug_line.getJSONObject(mediation_component);
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
                    JSONObject med_component_arguments = parsed_debug_line.getJSONObject(mediation_component);
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
    public void registerMediationFlowPoint(String mediation_component, JSONObject med_component_arguments,
            boolean isBreakpoint, boolean registerMode) {
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
                    ConnectorDebugUtil
                            .registerConnectorMediationFlowBreakPoint(synCfg, connector_key, connector_method_name,
                                    med_pos, registerMode);
                } else {
                    ConnectorDebugUtil
                            .registerConnectorMediationFlowSkip(synCfg, connector_key, connector_method_name, med_pos,
                                    registerMode);
                }

            } else if (mediation_component
                    .equals(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE)) {
                if ((!med_component_arguments
                        .has(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY))
                        && (!med_component_arguments
                        .has(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API))
                        && (!med_component_arguments
                        .has(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_INBOUND))) {
                    String sequence_key = med_component_arguments
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_KEY);
                    String sequence_type = med_component_arguments
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_TYPE);
                    String component_mediator_position = med_component_arguments.getString(
                            SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION);
                    String[] mediator_position_array = component_mediator_position.split("\\s+");
                    int[] med_pos = new int[mediator_position_array.length];
                    for (int counter = 0; counter < mediator_position_array.length; counter++) {
                        med_pos[counter] = Integer.valueOf(mediator_position_array[counter]);
                    }
                    if (isBreakpoint) {
                        SequenceDebugUtil
                                .registerSequenceMediationFlowBreakPoint(synCfg, sequence_type, sequence_key, med_pos,
                                        registerMode);
                    } else {
                        SequenceDebugUtil
                                .registerSequenceMediationFlowSkip(synCfg, sequence_type, sequence_key, med_pos,
                                        registerMode);
                    }
                } else if (med_component_arguments
                        .has(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY)) {
                    JSONObject proxy_arguments = med_component_arguments.getJSONObject(
                            SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY);
                    String proxy_key = proxy_arguments.getString(
                            SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY_KEY);
                    String sequence_type = proxy_arguments
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_TYPE);
                    String component_mediator_position = proxy_arguments.getString(
                            SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION);
                    String[] mediator_position_array = component_mediator_position.split("\\s+");
                    int[] med_pos = new int[mediator_position_array.length];
                    for (int counter = 0; counter < mediator_position_array.length; counter++) {
                        med_pos[counter] = Integer.valueOf(mediator_position_array[counter]);
                    }
                    if (isBreakpoint) {
                        ProxyDebugUtil
                                .registerProxySequenceMediationFlowBreakPoint(synCfg, sequence_type, proxy_key, med_pos,
                                        registerMode);
                    } else {
                        ProxyDebugUtil.registerProxySequenceMediationFlowSkip(synCfg, sequence_type, proxy_key, med_pos,
                                registerMode);
                    }

                } else if (med_component_arguments
                        .has(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_INBOUND)) {
                    JSONObject inbound_arguments = med_component_arguments.getJSONObject(
                            SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_INBOUND);
                    String inbound_key = inbound_arguments.getString(
                            SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_INBOUND_KEY);
                    String sequence_type = inbound_arguments
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_TYPE);
                    String component_mediator_position = inbound_arguments.getString(
                            SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION);
                    String[] mediator_position_array = component_mediator_position.split("\\s+");
                    int[] med_pos = new int[mediator_position_array.length];
                    for (int counter = 0; counter < mediator_position_array.length; counter++) {
                        med_pos[counter] = Integer.valueOf(mediator_position_array[counter]);
                    }
                    if (isBreakpoint) {
                        InboundEndpointDebugUtil
                                .registerInboundSequenceMediationFlowBreakPoint(synCfg, sequence_type, inbound_key,
                                        med_pos, registerMode);
                    } else {
                        InboundEndpointDebugUtil
                                .registerInboundSequenceMediationFlowSkip(synCfg, sequence_type, inbound_key, med_pos,
                                        registerMode);
                    }

                } else if (med_component_arguments
                        .has(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API)) {
                    JSONObject api_arguments = med_component_arguments
                            .getJSONObject(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API);
                    JSONObject resource_arguments = api_arguments.getJSONObject(
                            SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE);
                    String mapping = null;
                    if (resource_arguments
                            .has(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE_URI_TEMPLATE)) {
                        mapping = resource_arguments.getString(
                                SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE_URI_TEMPLATE);
                    } else if (resource_arguments
                            .has(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE_URL_MAPPING)) {
                        mapping = resource_arguments.getString(
                                SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE_URL_MAPPING);
                    }
                    String method = resource_arguments.getString(
                            SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE_METHOD);
                    String[] methodArray = method.split(METHOD_ARRAY_SEPERATOR);
                    String api_key = api_arguments
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_KEY);
                    String sequence_type = api_arguments
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_TYPE);
                    String component_mediator_position = api_arguments.getString(
                            SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION);
                    String[] mediator_position_array = component_mediator_position.split("\\s+");
                    int[] med_pos = new int[mediator_position_array.length];
                    for (int counter = 0; counter < mediator_position_array.length; counter++) {
                        med_pos[counter] = Integer.valueOf(mediator_position_array[counter]);
                    }
                    for (String resourceMethod : methodArray) {
                        if (isBreakpoint) {
                            APIDebugUtil.registerAPISequenceMediationFlowBreakPoint(synCfg, mapping, resourceMethod,
                                    sequence_type, api_key, med_pos, registerMode);
                        } else {
                            APIDebugUtil.registerAPISequenceMediationFlowSkip(synCfg, mapping, resourceMethod,
                                    sequence_type, api_key, med_pos, registerMode);
                        }
                    }
                }

            } else if (mediation_component
                    .equals(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_TEMPLATE)) {
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
                    TemplateDebugUtil
                            .registerTemplateMediationFlowBreakPoint(synCfg, template_key, med_pos, registerMode);
                } else {
                    TemplateDebugUtil.registerTemplateMediationFlowSkip(synCfg, template_key, med_pos, registerMode);
                }
            }
        } catch (JSONException ex) {
            log.error("Unable to register mediation flow point", ex);
            this.advertiseCommandResponse(createDebugCommandResponse(false,
                    SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_UNABLE_TO_REGISTER_FLOW_POINT).toString());
        }
    }

    public void advertiseCommandResponse(String commandResponse) {
        if (synEnv.isDebuggerEnabled()) {
            debugInterface.getPortListenWriter().println(commandResponse);
            debugInterface.getPortListenWriter().flush();
        }
    }

    public void advertiseDebugEvent(String event) {
        if (synEnv.isDebuggerEnabled()) {
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
                    proxy_parameters
                            .put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY_KEY,
                                    point.getKey());
                    proxy_parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_TYPE,
                            ((SequenceMediationFlowPoint) point).getSynapseSequenceType().toString().toLowerCase());
                    proxy_parameters
                            .put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION,
                                    toString(point.getMediatorPosition()));
                    parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY,
                            proxy_parameters);
                    event.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE, parameters);
                } else if (((SequenceMediationFlowPoint) point).getSequenceBaseType()
                        .equals(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_INBOUND)) {
                    JSONObject inbound_parameters = new JSONObject();
                    inbound_parameters
                            .put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_INBOUND_KEY,
                                    point.getKey());
                    inbound_parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_TYPE,
                            ((SequenceMediationFlowPoint) point).getSynapseSequenceType().toString().toLowerCase());
                    inbound_parameters
                            .put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION,
                                    toString(point.getMediatorPosition()));
                    parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_INBOUND,
                            inbound_parameters);
                    event.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE, parameters);
                } else if (((SequenceMediationFlowPoint) point).getSequenceBaseType()
                        .equals(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API)) {
                    JSONObject api_parameters = new JSONObject();
                    api_parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_KEY,
                            point.getKey());
                    JSONObject resource = new JSONObject();
                    resource.put(
                            SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE_MAPPING,
                            ((APIMediationFlowPoint) point).getResourceMapping());
                    resource.put(
                            SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE_METHOD,
                            ((APIMediationFlowPoint) point).getResourceHTTPMethod());
                    api_parameters
                            .put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE,
                                    resource);
                    api_parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_TYPE,
                            ((SequenceMediationFlowPoint) point).getSynapseSequenceType().toString().toLowerCase());
                    api_parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION,
                            toString(point.getMediatorPosition()));
                    parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API,
                            api_parameters);
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
                parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_CONNECTOR_KEY,
                        point.getKey());
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

    /**
     * This method is to generate json string which can be used to identify mediator position for wire logs
     *
     * @param point
     * @return
     */
    public JSONObject createDebugMediationFlowPointJSONForWireLogs(SynapseMediationFlowPoint point) {
        JSONObject flowPointJson = null;

        try {
            flowPointJson = new JSONObject();
            JSONObject parameters = new JSONObject();
            if (point.getSynapseMediationComponent().equals(SynapseMediationComponent.SEQUENCE)) {
                flowPointJson.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT,
                          SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE);

                if (((SequenceMediationFlowPoint) point).getSequenceBaseType()
                        .equals(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE)) {
                    parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_KEY,
                                   point.getKey());
                    parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_TYPE,
                                   ((SequenceMediationFlowPoint) point).getSynapseSequenceType().toString().toLowerCase());
                    parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION,
                                   toString(point.getMediatorPosition()));
                    flowPointJson.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE, parameters);
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
                    flowPointJson.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE, parameters);
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
                    flowPointJson.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE, parameters);
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
                    flowPointJson.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE, parameters);
                }
            } else if (point.getSynapseMediationComponent().equals(SynapseMediationComponent.TEMPLATE)) {
                flowPointJson.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT,
                          SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_TEMPLATE);
                parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_TEMPLATE_KEY,
                               point.getKey());
                parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION,
                               toString(point.getMediatorPosition()));
                flowPointJson.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_TEMPLATE, parameters);
            } else if (point.getSynapseMediationComponent().equals(SynapseMediationComponent.CONNECTOR)) {
                flowPointJson.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT,
                          SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_CONNECTOR);
                parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_CONNECTOR_KEY, point.getKey());
                parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_CONNECTOR_METHOD,
                               ((ConnectorMediationFlowPoint) point).getConnectorMediationComponentMethod());
                parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION,
                               toString(point.getMediatorPosition()));
                flowPointJson.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_CONNECTOR, parameters);
            }
        } catch (JSONException ex) {
            log.error("Failed to create debug flowPointJson in JSON format", ex);
        }
        return flowPointJson;
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
        return positionString.trim();
    }

    public void acquireMediationFlowPointProperties(String propertyOrProperties,
                                                    String propertyContext,
                                                    JSONObject property_arguments) throws IOException {
        if ((!(this.medFlowState == MediationFlowState.SUSPENDED)) & (propertyContext != null & !propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_WIRE))) {
            this.advertiseCommandResponse(createDebugCommandResponse(false,
                    SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_UNABLE_TO_ACQUIRE_MESSAGE_CONTEXT_PROPERTIES)
                    .toString());
            return;
        }
        try {
            if (propertyOrProperties.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTIES)) {
                if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_ALL)) {
                    JSONObject data_axis2 = getAxis2Properties();
                    JSONObject data_synapse = new JSONObject(((Axis2MessageContext) synCtx).getProperties());
                    JSONObject data_axis2_prop = new JSONObject();
                    JSONObject data_synapse_prop = new JSONObject();
                    data_axis2_prop.put(SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_PROPERTY_CONTEXT_AXIS2,
                            data_axis2);
                    data_synapse_prop.put(SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_PROPERTY_CONTEXT_SYNAPSE,
                            data_synapse);
                    JSONArray data_array = new JSONArray();
                    data_array.put(data_axis2_prop);
                    data_array.put(data_synapse_prop);
                    debugInterface.getPortListenWriter().println(data_array.toString());
                    debugInterface.getPortListenWriter().flush();
                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2)) {
                    JSONObject data_axis2 = getAxis2Properties();
                    JSONObject data_axis2_prop = new JSONObject();
                    data_axis2_prop.put(SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_PROPERTY_CONTEXT_AXIS2,
                            data_axis2);
                    debugInterface.getPortListenWriter().println(data_axis2_prop.toString());
                    debugInterface.getPortListenWriter().flush();
                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_SYNAPSE)
                        || propertyContext
                        .equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_DEFAULT)) {
                    JSONObject data_synapse = new JSONObject(((Axis2MessageContext) synCtx).getProperties());
                    JSONObject data_synapse_prop = new JSONObject();
                    data_synapse_prop.put(SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_PROPERTY_CONTEXT_SYNAPSE,
                            data_synapse);
                    debugInterface.getPortListenWriter().println(data_synapse_prop.toString());
                    debugInterface.getPortListenWriter().flush();
                } else if (propertyContext
                        .equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2CLIENT)) {
                    JSONObject data_axis2 = new JSONObject(
                            ((Axis2MessageContext) synCtx).getAxis2MessageContext().getOptions().getProperties());
                    JSONObject data_axis2_prop = new JSONObject();
                    data_axis2_prop
                            .put(SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_PROPERTY_CONTEXT_AXIS2CLIENT,
                                    data_axis2);
                    debugInterface.getPortListenWriter().println(data_axis2_prop.toString());
                    debugInterface.getPortListenWriter().flush();
                } else if (propertyContext
                        .equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_TRANSPORT)) {
                    JSONObject data_axis2 = new JSONObject((Map) ((Axis2MessageContext) synCtx).getAxis2MessageContext()
                            .getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS));
                    JSONObject data_axis2_prop = new JSONObject();
                    data_axis2_prop
                            .put(SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_PROPERTY_CONTEXT_AXIS2TRANSPORT,
                                    data_axis2);
                    debugInterface.getPortListenWriter().println(data_axis2_prop.toString());
                    debugInterface.getPortListenWriter().flush();
                } else if (propertyContext
                        .equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_OPERATION)) {
                    JSONObject data_axis2 = new JSONObject(
                            ((Axis2MessageContext) synCtx).getAxis2MessageContext().getOperationContext()
                                    .getProperties());
                    JSONObject data_axis2_prop = new JSONObject();
                    data_axis2_prop
                            .put(SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_PROPERTY_CONTEXT_AXIS2OPERATION,
                                    data_axis2);
                    debugInterface.getPortListenWriter().println(data_axis2_prop.toString());
                    debugInterface.getPortListenWriter().flush();
                }
            } else if (propertyOrProperties.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY)) {
                if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2)) {
                    JSONObject data_axis2 = getAxis2Properties();
                    Object result = null;
                    if (data_axis2.has(property_arguments
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME))) {
                        result = data_axis2.get(property_arguments
                                .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME));
                    }
                    JSONObject json_result = new JSONObject();
                    json_result
                            .put(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME),
                                    result);
                    debugInterface.getPortListenWriter().println(json_result.toString());
                    debugInterface.getPortListenWriter().flush();

                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_SYNAPSE)
                        || propertyContext
                        .equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_DEFAULT)) {
                    JSONObject data_synapse = new JSONObject(((Axis2MessageContext) synCtx).getProperties());
                    Object result = null;
                    if (data_synapse.has(property_arguments
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME))) {
                        result = data_synapse.getJSONObject(
                                property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME));
                    }
                    JSONObject json_result = new JSONObject();
                    json_result
                            .put(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME),
                                    result);
                    debugInterface.getPortListenWriter().println(json_result.toString());
                    debugInterface.getPortListenWriter().flush();

                } else if (propertyContext
                        .equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2CLIENT)) {
                    JSONObject data_axis2 = new JSONObject(
                            ((Axis2MessageContext) synCtx).getAxis2MessageContext().getOptions().getProperties());
                    Object result = null;
                    if (data_axis2.has(property_arguments
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME))) {
                        result = data_axis2.get(property_arguments
                                .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME));
                    }
                    JSONObject json_result = new JSONObject();
                    json_result
                            .put(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME),
                                    result);
                    debugInterface.getPortListenWriter().println(json_result.toString());
                    debugInterface.getPortListenWriter().flush();

                } else if (propertyContext
                        .equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_TRANSPORT)) {
                    JSONObject data_axis2 = new JSONObject((Map) ((Axis2MessageContext) synCtx).getAxis2MessageContext()
                            .getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS));
                    Object result = null;
                    if (data_axis2.has(property_arguments
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME))) {
                        result = data_axis2.get(property_arguments
                                .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME));
                    }
                    JSONObject json_result = new JSONObject();
                    json_result
                            .put(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME),
                                    result);
                    debugInterface.getPortListenWriter().println(json_result.toString());
                    debugInterface.getPortListenWriter().flush();

                } else if (propertyContext
                        .equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_OPERATION)) {
                    JSONObject data_axis2 = new JSONObject(
                            ((Axis2MessageContext) synCtx).getAxis2MessageContext().getOperationContext()
                                    .getProperties());
                    Object result = null;
                    if (data_axis2.has(property_arguments
                            .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME))) {
                        result = data_axis2.get(property_arguments
                                .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME));
                    }
                    JSONObject json_result = new JSONObject();
                    json_result
                            .put(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME),
                                    result);
                    debugInterface.getPortListenWriter().println(json_result.toString());
                    debugInterface.getPortListenWriter().flush();
                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_WIRE)) {
                    SynapseWireLogHolder synapseWireLogHolder = (synCtx != null) ? (SynapseWireLogHolder) ((Axis2MessageContext) synCtx).getAxis2MessageContext()
                            .getProperty(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY) : null;
                    JSONObject wireLog = createWireLogResponse(synapseWireLogHolder);
                    debugInterface.getPortListenWriter().println(wireLog.toString());
                    debugInterface.getPortListenWriter().flush();
                    log.debug("wirelog sent to devstudio - " + wireLog.toString());
                }
            }
        } catch (JSONException ex) {
            log.error("Failed to acquire property in the scope: " + propertyContext, ex);
        }
    }

    /**
     * Helper method to create the wirelog response to send to dev studio side
     *
     * @param synapseWireLogHolder
     * @return
     * @throws JSONException
     */
    private JSONObject createWireLogResponse(SynapseWireLogHolder synapseWireLogHolder) throws JSONException {
        JSONObject wireLog = new JSONObject();
        JSONArray mediatorLogs = new JSONArray();
        //key mediator id json object, value SynapseBackEndWireLogs object
        Map<JSONObject, SynapseBackEndWireLogs> wireLogsMap = new HashMap<JSONObject, SynapseBackEndWireLogs>();

        if (synapseWireLogHolder != null) {
            constructWireLogMap(synapseWireLogHolder, wireLogsMap);
        }

        fillWireLogJsonArray(wireLogsMap, mediatorLogs);

        wireLog.put(SynapseDebugCommandConstants.WIRELOGS, mediatorLogs);
        return wireLog;
    }

    /**
     * Helper method to fill wirelogs json array using the wirelogs map which was filled using SynapseWireLogHolder object
     *
     * @param wireLogsMap
     * @param mediatorLogs
     * @throws JSONException
     */
    private void fillWireLogJsonArray(Map<JSONObject, SynapseBackEndWireLogs> wireLogsMap, JSONArray mediatorLogs)
            throws JSONException {
        for (SynapseBackEndWireLogs synapseBackEndWireLog : wireLogsMap.values()) {
            JSONObject mediatorId = new JSONObject(synapseBackEndWireLog.getMediatorID());

            JSONObject backEndWireLogEntry = new JSONObject();
            backEndWireLogEntry.put(SynapseDebugCommandConstants.MEDIATOR_ID, mediatorId);

            JSONObject backEndWireLogs = new JSONObject();
            backEndWireLogs.put(SynapseDebugCommandConstants.REQUEST_WIRE_LOG, synapseBackEndWireLog.getRequestWireLog());
            backEndWireLogs.put(SynapseDebugCommandConstants.RESPONSE_WIRE_LOG, synapseBackEndWireLog.getResponseWireLog());

            backEndWireLogEntry.put(SynapseDebugCommandConstants.WIRE_LOG_ENTRY, backEndWireLogs);

            mediatorLogs.put(backEndWireLogEntry);
        }
    }

    /**
     * Fill hash map with backendWireLogEntries using given SynapseWireLogHolder object, key of the map
     * being mediatorId json object and value being SynapseBackEndWireLogs object
     *
     * @param wireLogHolder
     * @param wireLogsMap
     * @throws JSONException
     */
    private void constructWireLogMap(SynapseWireLogHolder wireLogHolder, Map<JSONObject, SynapseBackEndWireLogs> wireLogsMap) throws JSONException {
        //create request response wirelog (this is the wirelog of the initial request and final response)
        JSONObject reqResMedId = new JSONObject();
        reqResMedId.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT, SynapseDebugCommandConstants.REQUEST_RESPONSE);
        if (wireLogHolder.getProxyName() != null && !wireLogHolder.getProxyName().isEmpty()) {
            reqResMedId.put(SynapseDebugCommandConstants.TYPE, SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY);
            reqResMedId.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY_KEY, wireLogHolder.getProxyName());
        } else if (wireLogHolder.getApiName() != null && !wireLogHolder.getApiName().isEmpty()) {
            reqResMedId.put(SynapseDebugCommandConstants.TYPE, SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API);
            reqResMedId.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_KEY, wireLogHolder.getApiName());
            if (wireLogHolder.getResourceUrlString() != null && !wireLogHolder.getResourceUrlString().isEmpty()) {
                reqResMedId.put(SynapseDebugCommandConstants.REST_RESOURCE_URL_STRING, wireLogHolder.getResourceUrlString());
            }
        } else {
            //todo need to check such situation exist or not
        }

        SynapseBackEndWireLogs reqResWireLog = wireLogsMap.get(reqResMedId);
        if (reqResWireLog == null) {
            reqResWireLog = new SynapseBackEndWireLogs();
            reqResWireLog.setMediatorID(reqResMedId.toString());
        }
        if (reqResWireLog.getRequestWireLog() == null || reqResWireLog.getRequestWireLog().isEmpty()) {
            reqResWireLog.appendRequestWireLog(wireLogHolder.getRequestWireLog());
        }
        if (reqResWireLog.getResponseWireLog() == null || reqResWireLog.getResponseWireLog().isEmpty()) {
            reqResWireLog.appendResponseWireLog(wireLogHolder.getResponseWireLog());
        }
        wireLogsMap.put(reqResMedId, reqResWireLog);
        //fill the map with back end call wirelogs
        for (SynapseBackEndWireLogs synapseBackEndWireLog : wireLogHolder.getBackEndRequestResponse().values()) {
            JSONObject mediatorId = new JSONObject(synapseBackEndWireLog.getMediatorID());
            JSONObject dummyId = new JSONObject(SynapseDebugInfoHolder.DUMMY_MEDIATOR_ID);
            //dummy id's will be neglected when sending wire Logs to developer studio
            if (mediatorId != null && !mediatorId.toString().equalsIgnoreCase(dummyId.toString())) {
                SynapseBackEndWireLogs backEndWireLogEntry = wireLogsMap.get(mediatorId);
                if (backEndWireLogEntry == null) {
                    backEndWireLogEntry = synapseBackEndWireLog;
                } else {
                    if (backEndWireLogEntry.getRequestWireLog() == null || backEndWireLogEntry.getRequestWireLog().isEmpty()) {
                        backEndWireLogEntry.appendRequestWireLog(synapseBackEndWireLog.getRequestWireLog());
                    }
                    if (backEndWireLogEntry.getResponseWireLog() == null || backEndWireLogEntry.getResponseWireLog().isEmpty()) {
                        backEndWireLogEntry.appendResponseWireLog(synapseBackEndWireLog.getResponseWireLog());
                    }
                }
                wireLogsMap.put(mediatorId, backEndWireLogEntry);
            }
        }
    }

    protected JSONObject getAxis2Properties() throws JSONException, IOException {
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
        result.put(SynapseDebugCommandConstants.AXIS2_PROPERTY_DIRECTION, synCtx.isResponse() ? "response" : "request");
        Object messageTypeProperty = ((Axis2MessageContext) synCtx).getAxis2MessageContext().getProperty("messageType");
        if (messageTypeProperty != null && ((String) messageTypeProperty).contains("json")) {
            InputStream jsonPayloadStream = JsonUtil
                    .getJsonPayload(((Axis2MessageContext) synCtx).getAxis2MessageContext());
            if (jsonPayloadStream != null) {
                StringWriter writer = new StringWriter();
                String encoding = null;
                IOUtils.copy(jsonPayloadStream, writer, encoding);
                String jsonPayload = writer.toString();
                result.put(SynapseDebugCommandConstants.AXIS2_PROPERTY_ENVELOPE,
                        jsonPayload != null ? jsonPayload : synCtx.getEnvelope().toString());
            } else {
                result.put(SynapseDebugCommandConstants.AXIS2_PROPERTY_ENVELOPE,
                        synCtx.getEnvelope() != null ? synCtx.getEnvelope().toString() : "");
            }
        } else {
            result.put(SynapseDebugCommandConstants.AXIS2_PROPERTY_ENVELOPE,
                    synCtx.getEnvelope() != null ? synCtx.getEnvelope().toString() : "");
        }
        String axis2MessageContextKey = getAxis2MessagePropertiesKey(
                ((Axis2MessageContext) synCtx).getAxis2MessageContext());
        if (addedPropertyValuesMap.containsKey(axis2MessageContextKey)) {
            Map scopePropertyMap = (Map) addedPropertyValuesMap.get(axis2MessageContextKey);
            if (scopePropertyMap.containsKey(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2)) {
                Set<String> propertyKeySet = (Set<String>) scopePropertyMap
                        .get(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2);
                for (String key : propertyKeySet) {
                    result.put(key, ((Axis2MessageContext) synCtx).getAxis2MessageContext().getProperty(key));
                }
            }
        }
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
        result.put(SynapseDebugCommandConstants.AXIS2_PROPERTY_EXCESS_TRANSPORT_HEADERS,
                ((Axis2MessageContext) synCtx).getAxis2MessageContext().getProperty("EXCESS_TRANSPORT_HEADERS"));
        result.put(SynapseDebugCommandConstants.AXIS2_PROPERTY_MESSAGE_TYPE,
                ((Axis2MessageContext) synCtx).getAxis2MessageContext().getProperty("messageType"));
        result.put(SynapseDebugCommandConstants.AXIS2_PROPERTY_CONTENT_TYPE,
                ((Axis2MessageContext) synCtx).getAxis2MessageContext().getProperty("ContentType"));
        return result;

    }

    public void addMediationFlowPointProperty(String propertyContext, JSONObject property_arguments,
            boolean isActionSet) {
        try {
            String propertyKey = property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME);
            if (isActionSet) {
                String propertyValue = property_arguments
                        .getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_VALUE);
                if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_DEFAULT)
                        || propertyContext
                        .equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_SYNAPSE)) {
                    synCtx.setProperty(propertyKey, propertyValue);

                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2)
                        && synCtx instanceof Axis2MessageContext) {
                    Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                    org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
                    setAxis2Property(propertyKey, propertyValue, axis2MessageCtx);
                    if (org.apache.axis2.Constants.Configuration.MESSAGE_TYPE.equalsIgnoreCase(propertyKey)) {
                        setAxis2Property(org.apache.axis2.Constants.Configuration.CONTENT_TYPE, propertyValue,
                                axis2MessageCtx);
                        Object o = axis2MessageCtx
                                .getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
                        Map headers = (Map) o;
                        if (headers != null) {
                            headers.remove(HTTP.CONTENT_TYPE);
                            headers.put(HTTP.CONTENT_TYPE, propertyValue);
                        }
                    }
                } else if (
                        propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2CLIENT)
                                && synCtx instanceof Axis2MessageContext) {
                    Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                    org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
                    axis2MessageCtx.getOptions().setProperty(propertyKey, propertyValue);
                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_TRANSPORT)
                        && synCtx instanceof Axis2MessageContext) {
                    Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                    org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
                    Object headers = axis2MessageCtx
                            .getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
                    if (headers != null && headers instanceof Map) {
                        Map headersMap = (Map) headers;
                        headersMap.put(propertyKey, propertyValue);
                    }
                    if (headers == null) {
                        Map headersMap = new HashMap();
                        headersMap.put(propertyKey, propertyValue);
                        axis2MessageCtx
                                .setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, headersMap);
                    }
                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_OPERATION)
                        && synCtx instanceof Axis2MessageContext) {
                    Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                    axis2smc.getAxis2MessageContext().getOperationContext().setProperty(propertyKey, propertyValue);
                }
            } else {
                if (propertyContext == null || SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_DEFAULT
                        .equals(propertyContext) || SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_SYNAPSE
                        .equals(propertyContext)) {
                    Set pros = synCtx.getPropertyKeySet();
                    if (pros != null) {
                        pros.remove(propertyKey);
                    }
                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2)
                        && synCtx instanceof Axis2MessageContext) {
                    Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                    org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
                    axis2MessageCtx.removeProperty(propertyKey);
                } else if (
                        propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2CLIENT)
                                && synCtx instanceof Axis2MessageContext) {
                    Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                    org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
                    axis2MessageCtx.getOptions().setProperty(propertyKey,EMPTY_STRING);
                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_TRANSPORT)
                        && synCtx instanceof Axis2MessageContext) {
                    Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                    org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
                    Object headers = axis2MessageCtx
                            .getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
                    if (headers != null && headers instanceof Map) {
                        Map headersMap = (Map) headers;
                        headersMap.remove(propertyKey);
                    }
                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_OPERATION)
                        && synCtx instanceof Axis2MessageContext) {
                    Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                    axis2smc.getAxis2MessageContext().getOperationContext().removeProperty(propertyKey);
                } else {
                    log.error("Failed to set or remove property in the scope " + propertyContext);
                    this.advertiseCommandResponse(createDebugCommandResponse(false,
                            SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_UNABLE_TO_ALTER_MESSAGE_CONTEXT_PROPERTY)
                            .toString());
                }
            }
        } catch (JSONException e) {
            log.error("Failed to set or remove property in the scope " + propertyContext, e);
            this.advertiseCommandResponse(createDebugCommandResponse(false,
                    SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_UNABLE_TO_ALTER_MESSAGE_CONTEXT_PROPERTY)
                    .toString());
        }
        this.advertiseCommandResponse(createDebugCommandResponse(true, null).toString());
    }

    private void setAxis2Property(String propertyKey, String propertyValue,
            org.apache.axis2.context.MessageContext axis2MessageCtx) {
        //Do not change the Envelope, SoapHeaders and Transport Headers
        switch (propertyKey) {
        case SynapseDebugCommandConstants.AXIS2_PROPERTY_TO:
            axis2MessageCtx.setTo(new EndpointReference(propertyValue));
            break;
        case SynapseDebugCommandConstants.AXIS2_PROPERTY_FROM:
            axis2MessageCtx.setFrom(new EndpointReference(propertyValue));
            break;
        case SynapseDebugCommandConstants.AXIS2_PROPERTY_WSACTION:
            axis2MessageCtx.setWSAAction(propertyValue);
            break;
        case SynapseDebugCommandConstants.AXIS2_PROPERTY_SOAPACTION:
            axis2MessageCtx.setSoapAction(propertyValue);
            break;
        case SynapseDebugCommandConstants.AXIS2_PROPERTY_REPLY_TO:
            axis2MessageCtx.setReplyTo(new EndpointReference(propertyValue));
            break;
        case SynapseDebugCommandConstants.AXIS2_PROPERTY_MESSAGE_ID:
            axis2MessageCtx.setMessageID(propertyValue);
            break;
        case SynapseDebugCommandConstants.AXIS2_PROPERTY_MESSAGE_TYPE:
            axis2MessageCtx.setProperty("messageType", propertyValue);
            break;
        case SynapseDebugCommandConstants.AXIS2_PROPERTY_DIRECTION:
            if ("response".equalsIgnoreCase(propertyValue)) {
                synCtx.setResponse(true);
            } else if ("request".equalsIgnoreCase(propertyValue)) {
                synCtx.setResponse(false);
            } else {
                log.warn("unknown axis2 direction : " + propertyValue);
            }
            break;
        default:
            //MessageType, ContentType, ExcessTransportHeaders and other properties
            axis2MessageCtx.setProperty(propertyKey, propertyValue);
            Map<String, Set<String>> scopePropertiesMap;
            Set<String> axis2PropertyKeySet;
            String axis2MessageCtxKey = getAxis2MessagePropertiesKey(axis2MessageCtx);
            if (addedPropertyValuesMap.containsKey(axis2MessageCtxKey)) {
                scopePropertiesMap = (Map) addedPropertyValuesMap.get(axis2MessageCtxKey);

                if (scopePropertiesMap.containsKey(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2)) {
                    axis2PropertyKeySet = scopePropertiesMap
                            .get(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2);
                } else {
                    axis2PropertyKeySet = new HashSet<>();
                }
                axis2PropertyKeySet.add(propertyKey);
            } else {
                scopePropertiesMap = new HashMap<>();
                axis2PropertyKeySet = new HashSet<>();
                axis2PropertyKeySet.add(propertyKey);
            }
            scopePropertiesMap
                    .put(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2, axis2PropertyKeySet);
            addedPropertyValuesMap.put(axis2MessageCtxKey, scopePropertiesMap);
        }

    }

    private String getAxis2MessagePropertiesKey(org.apache.axis2.context.MessageContext axis2MessageCtx) {
        String axis2MessageCtxKey = axis2MessageCtx.toString();
        return axis2MessageCtxKey;
    }
    @Override
    public void update(Observable o, Object arg) {
        if (synEnv.isDebuggerEnabled()) {
            try {
                //create wirelogs json object and send it to developer studio(this is sent via event port)
                SynapseWireLogHolder synapseWireLogHolder = (SynapseWireLogHolder) arg;
                JSONObject wireLog = createWireLogResponse(synapseWireLogHolder);
                debugInterface.getPortSendWriter().println(wireLog);
                debugInterface.getPortSendWriter().flush();
                log.debug("wire log event got triggered and sent the event to developer studio");
            } catch (JSONException ex) {
                log.error("Failed to create debug event in JSON format", ex);
            }
        }
    }
}
