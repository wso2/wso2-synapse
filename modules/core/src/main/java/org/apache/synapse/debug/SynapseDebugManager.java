/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements. See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership. The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
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
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.mediators.AbstractListMediator;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.template.InvokeMediator;
import org.apache.synapse.rest.API;
import org.apache.synapse.rest.Resource;
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
 * main class that handles mediation debugging in the server side
 * single instance is created handles debugging centrally using either persisting or retrieving debug information in the mediator level
 * relies on SynapseDebugInterface to communicate over TCP channel
 */

public class SynapseDebugManager {

    private static volatile ReentrantLock mediationFlowLock; //to ensure a single mediation flow at a given time
    public static volatile Semaphore mediationFlowSem;  //to ensure a synchronization between mediation flow suspension and resumption
    private MessageContext synCtx;
    private SynapseDebugInterface debugInterface=null;
    private static SynapseDebugManager debugManagerInstance = null;
    private static SynapseDebugTCPListener debugTCPListener=null;
    private SynapseConfiguration synCfg;
    private SynapseEnvironment synEnv;
    private MediationFlowState medFlowState= MediationFlowState.IDLE;
    private String msgReceiver;
    private String msgCallbackReceiver;
    private boolean initialised=false;
    private static final Log log = LogFactory.getLog(SynapseDebugManager.class);

   protected SynapseDebugManager() {
       mediationFlowLock= new ReentrantLock();
       mediationFlowSem = new Semaphore(0); // initial value of semaphore is set to zero
   }

   public static SynapseDebugManager getInstance() {
        if(debugManagerInstance == null) {
            debugManagerInstance = new SynapseDebugManager();
        }
        return debugManagerInstance;
    }

   public void setMessageContext(MessageContext synCtx){
        this.synCtx = synCtx;
    }


   /**
    *initializes the debug manager instance using
    * @param synCfg
    * @param debugInterface
    * @param synEnv
    */
   public void init(SynapseConfiguration synCfg, SynapseDebugInterface debugInterface,SynapseEnvironment synEnv ){
        if(synEnv.isDebugEnabled()){
            this.synCfg = synCfg;
            this.debugInterface = debugInterface;
            this.synEnv = synEnv;
            if(!initialised) {
                initialised=true;
                debugTCPListener=new SynapseDebugTCPListener(this,this.debugInterface);
                debugTCPListener.setDebugModeInProgress(true);
                //debugTCPListener.start();  //spawn a Listener thread from the main thread that initializes synapse environment
                log.info("Initialized with Synapse Configuration-Synapse Environment-Synapse Debug Interface");
            }else{
                log.info("Updated synapse configuration");
                this.advertiseDebugEvent(this.createDebugEvent(SynapseDebugEventConstants.DEBUG_EVENT_CONFIGURATION_UPDATED).toString());
            }
        }

    }

    /**
     *acquiring this lock make sure that only one mediation flow is happening inside mediation engine
     */
    public void acquireMediationFlowLock(){
        mediationFlowLock.lock();
    }


    /**
     *releasing this lock make sure that next mediation flow is started after completion of the previous
     */
    public void releaseMediationFlowLock(){
        mediationFlowLock.unlock();
    }

    /**
     *shutdown debug manager instance and close communication channels
     */
    public void shutdownDebugManager(){
        if(synEnv.isDebugEnabled()){
            debugInterface.closeConnection();
            debugTCPListener.shutDownListener();
        }
    }


    /**
     *transit the mediation flow state to the suspended from previous state
     *transiting to suspend state will put the calling thread to sleep as sem.down() is called
     */
    public void transitMediationFlowStateToSuspended() {
        if(synEnv.isDebugEnabled()){
            if(this.medFlowState== MediationFlowState.IDLE||this.medFlowState== MediationFlowState.ACTIVE){
                medFlowState= MediationFlowState.SUSPENDED;
                try {
                    mediationFlowSem.acquire();
                }catch (InterruptedException ex){
                    log.error("Unable to suspend the mediation flow thread",ex);
                }
            }
        }
    }

    /**
     *transit the mediation flow state to the active from previous state
     *transiting to  state will put the calling thread wakes as sem.up() is called
     */
    public void transitMediationFlowStateToActive(){
        if(synEnv.isDebugEnabled()){
            if(this.medFlowState== MediationFlowState.SUSPENDED){
                medFlowState= MediationFlowState.ACTIVE;
                mediationFlowSem.release();
            }
        }
    }


    /**
     *related to advertising mediation flow start point to the communication channel
     * @param messageReceiver
     * @param synCtx
     */
    public void advertiseMediationFlowStartPoint(String messageReceiver,MessageContext synCtx) {
        if (synEnv.isDebugEnabled()) {
            setMessageContext(synCtx);
            this.msgReceiver = messageReceiver;
            this.advertiseDebugEvent(this.createDebugEvent(SynapseDebugEventConstants.DEBUG_EVENT_STARTED, messageReceiver).toString());
            log.info("DEBUG EVENT - Mediation flow : " + SynapseDebugEventConstants.DEBUG_EVENT_STARTED + " from " + messageReceiver + " message receiver");
        }
    }

    /**
     *related to advertising mediation flow response callback point to the communication channel
     * @param messageCallbackReceiver
     * @param synCtx
     */
    public void advertiseMediationFlowCallbackPoint(String messageCallbackReceiver, MessageContext synCtx) {
        if (synEnv.isDebugEnabled()) {
            setMessageContext(synCtx);
            this.msgCallbackReceiver = messageCallbackReceiver;
            this.advertiseDebugEvent(this.createDebugEvent(SynapseDebugEventConstants.DEBUG_EVENT_CALLBACK, messageCallbackReceiver).toString());
            log.info("DEBUG EVENT - Mediation flow : " + SynapseDebugEventConstants.DEBUG_EVENT_CALLBACK + " from " + messageCallbackReceiver + " callback receiver");
        }
    }

    /**
     *related to advertising mediation flow terminating point to the communication channel
     */
    public void advertiseMediationFlowTerminatePoint(){
        if(synEnv.isDebugEnabled()) {
            this.advertiseDebugEvent(this.createDebugEvent(SynapseDebugEventConstants.DEBUG_EVENT_TERMINATED, msgReceiver, msgCallbackReceiver).toString());
            log.info("DEBUG EVENT - Mediation flow :" + SynapseDebugEventConstants.DEBUG_EVENT_TERMINATED + " from " + msgReceiver + " " + SynapseDebugEventConstants.DEBUG_EVENT_MESSAGE_RECEIVER + " to " + msgCallbackReceiver + " " + SynapseDebugEventConstants.DEBUG_EVENT_CALLBACK_RECEIVER);
        }
    }



    /**
     * advertise a mediation skip to the communication channel
     * @param skipPoint describes a unique point in the mediation flow
     * @param synCtx
     */
    public void advertiseMediationFlowSkip(MessageContext synCtx, SynapseMediationFlowPoint skipPoint){
        if(synEnv.isDebugEnabled()&&debugInterface!=null) {
            setMessageContext(synCtx);
            this.advertiseDebugEvent(this.createDebugMediationFlowPointHitEvent(false, skipPoint).toString());
            log.info("DEBUG EVENT - Mediation Flow: " + SynapseDebugEventConstants.DEBUG_EVENT_SKIPPED + " " + skipPoint.getMediatorReference().getType());
        }
    }

    /**
     * advertise a mediation breakpoint to the communication channel
     * @param breakPoint describes a unique point in the mediation flow
     * @param synCtx
     */
    public void advertiseMediationFlowBreakPoint(MessageContext synCtx, SynapseMediationFlowPoint breakPoint){
        if (synEnv.isDebugEnabled()) {
            setMessageContext(synCtx);
            this.advertiseDebugEvent(this.createDebugMediationFlowPointHitEvent(true, breakPoint).toString());
            log.info("DEBUG EVENT - Mediation flow: " + SynapseDebugEventConstants.DEBUG_EVENT_SUSPENDED_BREAKPOINT + " at mediator " + breakPoint.getMediatorReference().getType());
            this.transitMediationFlowStateToSuspended();
            this.advertiseDebugEvent(this.createDebugEvent(SynapseDebugEventConstants.DEBUG_EVENT_RESUMED_CLIENT).toString());
            log.info("DEBUG EVENT - Mediation flow: " + SynapseDebugEventConstants.DEBUG_EVENT_RESUMED_CLIENT);
        }

    }

    /**
     * handles main command processing in using line of string received from the command channel
     * registering/un registering breakpoints and skips as well as mediation level data acquire or set
     * strings are expected to be JSON over defined protocol
     * @param debug_line
     */
    public void processDebugCommand(String debug_line){
        try {
            JSONObject parsed_debug_line = new JSONObject(debug_line);
            String command="";
            if(parsed_debug_line.has(SynapseDebugCommandConstants.DEBUG_COMMAND)){
               command=parsed_debug_line.getString(SynapseDebugCommandConstants.DEBUG_COMMAND);
            }else{
                return;
            }
           //String[] command = debug_line.split("\\s+");
           if (command.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_CLEAR)) {
                String skipOrBreakPointOrProperty=parsed_debug_line.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_ARGUMENT);
               if(skipOrBreakPointOrProperty.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY)){
                   String propertyContext=parsed_debug_line.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT);
                   JSONObject property_arguments = parsed_debug_line.getJSONObject(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY);
                   this.addMediationFlowPointProperty(propertyContext,property_arguments,false);
               }else {
                   String mediation_component = parsed_debug_line.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT);
                   JSONObject med_component_arguments = parsed_debug_line.getJSONObject(mediation_component);

                   if (skipOrBreakPointOrProperty.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_BREAKPOINT)) {
                       //log.info("DEBUG COMMAND - " + SynapseDebugCommandConstants.DEBUG_COMMAND_CLEAR + " " + SynapseDebugCommandConstants.DEBUG_COMMAND_BREAKPOINT);
                       this.registerMediationFlowPoint(mediation_component, med_component_arguments, true, false);
                   } else if (skipOrBreakPointOrProperty.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_SKIP)) {
                       // log.info("DEBUG COMMAND - " + SynapseDebugCommandConstants.DEBUG_COMMAND_CLEAR + " " + SynapseDebugCommandConstants.DEBUG_COMMAND_SKIP);
                       this.registerMediationFlowPoint(mediation_component, med_component_arguments, false, false);
                   }
               }
            } else if (command.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_GET)) {
               String propertyOrProperties=parsed_debug_line.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_ARGUMENT);
               String propertyContext=parsed_debug_line.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT);
               JSONObject property_arguments=null;
               if(propertyOrProperties.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY)) {
                  property_arguments = parsed_debug_line.getJSONObject(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY);
               }

                //log.info("DEBUG COMMAND - " + SynapseDebugCommandConstants.DEBUG_COMMAND_LOG + " " + arguments[0]);
                this.acquireMediationFlowPointProperties(propertyOrProperties,propertyContext, property_arguments);
            } else if (command.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_RESUME)) {
                //log.info("DEBUG COMMAND - " + SynapseDebugCommandConstants.DEBUG_COMMAND_RESUME);
                this.debugResume();
            } else if (command.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_SET)) {
               String skipOrBreakPointOrProperty=parsed_debug_line.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_ARGUMENT);
               if(skipOrBreakPointOrProperty.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY)){
                   String propertyContext=parsed_debug_line.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT);
                   JSONObject property_arguments = parsed_debug_line.getJSONObject(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY);
                   this.addMediationFlowPointProperty(propertyContext,property_arguments,true);
               }else {
                   String mediation_component = parsed_debug_line.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT);
                   JSONObject med_component_arguments = parsed_debug_line.getJSONObject(mediation_component);
                   if (skipOrBreakPointOrProperty.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_BREAKPOINT)) {
                       //  log.info("DEBUG COMMAND - " + SynapseDebugCommandConstants.DEBUG_COMMAND_SET + " " + SynapseDebugCommandConstants.DEBUG_COMMAND_BREAKPOINT);
                       this.registerMediationFlowPoint(mediation_component, med_component_arguments, true, true);
                   } else if (skipOrBreakPointOrProperty.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_SKIP)) {
                       // log.info("DEBUG COMMAND - " + SynapseDebugCommandConstants.DEBUG_COMMAND_SET + " " + SynapseDebugCommandConstants.DEBUG_COMMAND_SKIP);
                       this.registerMediationFlowPoint(mediation_component, med_component_arguments, false, true);
                   }
               }
            } else {
               log.info("Command not found");
               try {
                   this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_COMMAND_NOT_FOUND).toString());
               }catch (JSONException e){
                   log.error("Unable to advertise command response",e);
               }
           }
        }catch (JSONException ex) {
            log.error("Unable to process debug command");
        }
    }

    /**
     * handles registering/un registering breakpoints and skips as well as mediation level data acquire or set
     * @param mediation_component sequence connector or either template
     * @param med_component_arguments defines mediation component
     * @param isBreakpoint either breakpoint or skip
     * @param registerMode either register or un register
     */
    public void registerMediationFlowPoint( String mediation_component,JSONObject med_component_arguments,boolean isBreakpoint,boolean registerMode){
        try {
            if (mediation_component.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_CONNECTOR)) {
                String connector_key = med_component_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_CONNECTOR_KEY);
                String connector_method_name = med_component_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_CONNECTOR_METHOD);
                String component_mediator_position=med_component_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION);
                String[] mediator_position_array = component_mediator_position.split("\\s+");
                int[] med_pos=new int[mediator_position_array.length];
                for(int counter=0;counter<mediator_position_array.length;counter++){
                    med_pos[counter]=Integer.valueOf(mediator_position_array[counter]);
                }
                //String template_key=SynapseDebugManagerConstants.CONNECTOR_PACKAGE+"."+connector_key+"."+connector_method_name;
               // Mediator mediation_component_template=synCfg.getSequenceTemplate(template_key);
                if(isBreakpoint){
                    registerConnectorMediationFlowBreakPoint(connector_key,connector_method_name,med_pos,registerMode);
                }else{
                    registerConnectorMediationFlowSkip(connector_key,connector_method_name,med_pos,registerMode);
                }

            } else if (mediation_component.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE)) {
                if((!med_component_arguments.has(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY))&&(!med_component_arguments.has(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API)))
                {

                    String sequence_key = med_component_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_KEY);
                    String sequence_type = med_component_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_TYPE);
                    String component_mediator_position = med_component_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION);
                    String[] mediator_position_array = component_mediator_position.split("\\s+");
                    int[] med_pos = new int[mediator_position_array.length];
                    for (int counter = 0; counter < mediator_position_array.length; counter++) {
                        med_pos[counter] = Integer.valueOf(mediator_position_array[counter]);
                    }
                    if (isBreakpoint) {
                        registerSequenceMediationFlowBreakPoint(sequence_type, sequence_key, med_pos, registerMode);
                    } else {
                        registerSequenceMediationFlowSkip(sequence_type, sequence_key, med_pos, registerMode);
                    }
                }else if (med_component_arguments.has(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY)){
                    JSONObject proxy_arguments=med_component_arguments.getJSONObject(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY);
                    String proxy_key=proxy_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY_KEY);
                    String sequence_type = proxy_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_TYPE);
                    String component_mediator_position = proxy_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION);
                    String[] mediator_position_array = component_mediator_position.split("\\s+");
                    int[] med_pos = new int[mediator_position_array.length];
                    for (int counter = 0; counter < mediator_position_array.length; counter++) {
                        med_pos[counter] = Integer.valueOf(mediator_position_array[counter]);
                    }
                    if (isBreakpoint) {
                        registerProxySequenceMediationFlowBreakPoint(sequence_type, proxy_key, med_pos,registerMode);
                    } else {
                        registerProxySequenceMediationFlowSkip(sequence_type, proxy_key, med_pos, registerMode);
                    }

                }else if (med_component_arguments.has(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API)){
                    JSONObject api_arguments=med_component_arguments.getJSONObject(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API);
                    JSONObject resource_arguments=api_arguments.getJSONObject(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE);
                    String mapping=null;
                    if(resource_arguments.has(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE_URI_TEMPLATE)){
                        mapping=resource_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE_URI_TEMPLATE);
                    }else if (resource_arguments.has(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE_URL_MAPPING)){
                        mapping=resource_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE_URL_MAPPING);
                    }
                    String method=resource_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE_METHOD);
                    String api_key=api_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_KEY);
                    String sequence_type = api_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_TYPE);
                    String component_mediator_position = api_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION);
                    String[] mediator_position_array = component_mediator_position.split("\\s+");
                    int[] med_pos = new int[mediator_position_array.length];
                    for (int counter = 0; counter < mediator_position_array.length; counter++) {
                        med_pos[counter] = Integer.valueOf(mediator_position_array[counter]);
                    }
                    if (isBreakpoint) {
                        registerAPISequenceMediationFlowBreakPoint(mapping,method,sequence_type, api_key, med_pos,registerMode);
                    } else {
                        registerAPISequenceMediationFlowSkip(mapping,method,sequence_type, api_key, med_pos, registerMode);
                    }
                }

            } else if (mediation_component.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_TEMPLATE)) {
                String template_key = med_component_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_TEMPLATE_KEY);
                String component_mediator_position=med_component_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION);
                String[] mediator_position_array = component_mediator_position.split("\\s+");
                int[] med_pos=new int[mediator_position_array.length];
                for(int counter=0;counter<mediator_position_array.length;counter++){
                    med_pos[counter]=Integer.valueOf(mediator_position_array[counter]);
                }
                //Mediator mediation_component_template=synCfg.getSequenceTemplate(template_key);
                if(isBreakpoint){
                    registerTemplateMediationFlowBreakPoint(template_key,med_pos,registerMode);
                }else{
                    registerTemplateMediationFlowSkip(template_key,med_pos,registerMode);
                }
            }


        }catch (JSONException ex){
            log.error("Unable to register mediation flow point",ex);
            try {
                this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_UNABLE_TO_REGISTER_FLOW_POINT).toString());
            }catch (JSONException e){
                log.error("Unable to advertise command response",e);
            }
        }
    }


   /*
   {
       "command":"set|clear",
       "command-argument":"skip",
       "mediation-component":"sequence",
       "sequence":{
                            "api":{
                                      "api-key" : " example_api|....",
                                      "resource":{
                                                     "methods":"GET|POST| GET POST",
                                                     "uri-template|url-mapping":"/books/{bookid}|/books........"
                                                 }
                                      "sequence-type": "api_inseq|api_outseq|...",
                                      "mediator-postion" : " 0 1 2 3 ….."
                                  }
                   }
   }
   */
    public void registerAPISequenceMediationFlowSkip(String mapping, String method, String seqType,String apiKey,int[] position,boolean registerMode){
        SynapseSequenceType synapseSequenceType = SynapseSequenceType.valueOf(seqType.toUpperCase());
        SynapseMediationFlowPoint skipPoint=new SynapseMediationFlowPoint();
        skipPoint.setSynapseMediationComponent(SynapseMediationComponent.SEQUENCE);
        skipPoint.setKey(apiKey);
        skipPoint.setMediatorPosition(position);
        skipPoint.setSynapseSequenceType(synapseSequenceType);
        skipPoint.setSequenceMediationComponentIdentifier(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API);
        skipPoint.setAPIIdentifierMapping(mapping);
        skipPoint.setAPIIdentifierMethod(method);
        Mediator seqMediator=null;
        Resource api_resource=null;
        API api=synCfg.getAPI(apiKey);
        if(api==null){
            log.error("API not found");
            try {
                this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_API_NOT_FOUND).toString());
            }catch (JSONException e){
                log.error("Unable to advertise command response",e);
            }
            return;
        }
        Resource[] resource_array=api.getResources();
        for (int counter=0;counter<resource_array.length;counter ++){
            if(mapping.equals(resource_array[counter].getDispatcherHelper().getString())){
                for(String m1 : resource_array[counter].getMethods()){
                    if(m1.equals(method)){
                        api_resource=resource_array[counter];
                        break;
                    }
                }
                if(api_resource!=null){
                    break;
                }
            }
        }
        if(api_resource!=null) {
            if (synapseSequenceType.equals(SynapseSequenceType.API_INSEQ)) {
                seqMediator = api_resource.getInSequence();
            } else if (synapseSequenceType.equals(SynapseSequenceType.API_OUTSEQ)) {
                seqMediator = api_resource.getOutSequence();
            } else if (synapseSequenceType.equals(SynapseSequenceType.API_FAULTSEQ)) {
                seqMediator = api_resource.getFaultSequence();
            }
        }else{

            log.error("API resource not found");
            try {
                this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_API_RESOURCE_NOT_FOUND).toString());
            }catch (JSONException e){
                log.error("Unable to advertise command response",e);
            }
            return;
        }
        if(seqMediator!=null) {
            Mediator current_mediator = null;
            for (int counter = 0; counter < position.length; counter++) {
                if(counter==0){
                    current_mediator = ((AbstractListMediator) seqMediator).getChild(position[counter]);
                }
                if(current_mediator!=null&&counter!=0) {
                    if(current_mediator instanceof InvokeMediator){
                        current_mediator=synCfg.getSequenceTemplate(((InvokeMediator)current_mediator).getTargetTemplate());
                    }
                    current_mediator = ((AbstractListMediator) current_mediator).getChild(position[counter]);
                }
            }
            if(current_mediator!=null) {
                skipPoint.setMediatorReference(current_mediator);
                if(registerMode) {
                    if (!((AbstractMediator) current_mediator).isSkipEnabled()) {
                        ((AbstractMediator) current_mediator).setSkipEnabled(true);
                        ((AbstractMediator) current_mediator).registerMediationFlowPoint(skipPoint);
                        log.info("DEBUG EVENT - Mediation flow : Registered skip at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(true,null).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    } else {
                        log.error("Failed register skip - Already skip enabled at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_SKIP_ENABLED).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }
                }else{
                    if(((AbstractMediator) current_mediator).isSkipEnabled()) {
                        ((AbstractMediator) current_mediator).setSkipEnabled(false);
                        ((AbstractMediator) current_mediator).unregisterMediationFlowPoint();
                        log.info("DEBUG EVENT - Mediation flow : Unregistered skip at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(true,null).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }else{
                        log.error("Failed unregister skip - Already skip disabled at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_SKIP_DISABLED).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }

                }
            }else {
                if(registerMode) {
                    log.error("Failed register skip - Non existing mediator position");
                    try {
                        this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION).toString());
                    }catch (JSONException e){
                        log.error("Unable to advertise command response",e);
                    }
                }else{
                    log.error("Failed unregister skip - Non existing mediator position");
                    try {
                        this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION).toString());
                    }catch (JSONException e){
                        log.error("Unable to advertise command response",e);
                    }
                }
            }
        }else{
            if(registerMode) {
                log.error("Failed register skip - Non existing sequence");
                try {
                    this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_SEQUENCE).toString());
                }catch (JSONException e){
                    log.error("Unable to advertise command response",e);
                }
            }else {
                log.error("Failed unregister skip - Non existing sequence");
                try {
                    this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_SEQUENCE).toString());
                }catch (JSONException e){
                    log.error("Unable to advertise command response",e);
                }
            }
        }
    }


  /*
   {
       "command":"set|clear",
       "command-argument":"breakpoint",
       "mediation-component":"sequence",
       "sequence":{
                            "api":{
                                      "api-key" : " example_api|....",
                                      "resource":{
                                                     "methods":"GET|POST| GET POST",
                                                     "uri-template|url-mapping":"/books/{bookid}|/books........"
                                                 }
                                      "sequence-type": "api_inseq|api_outseq|...",
                                      "mediator-position" : " 0 1 2 3 ….."
                                  }
                   }
   }
   */
   public void registerAPISequenceMediationFlowBreakPoint(String mapping, String method,String sequenceType,String apiKey,int[] position,boolean registerMode){
        SynapseSequenceType synapseSequenceType = SynapseSequenceType.valueOf(sequenceType.toUpperCase());
        SynapseMediationFlowPoint breakPoint=new SynapseMediationFlowPoint();
        breakPoint.setSynapseMediationComponent(SynapseMediationComponent.SEQUENCE);
        breakPoint.setKey(apiKey);
        breakPoint.setMediatorPosition(position);
        breakPoint.setSynapseSequenceType(synapseSequenceType);
        breakPoint.setSequenceMediationComponentIdentifier(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API);
        breakPoint.setAPIIdentifierMapping(mapping);
        breakPoint.setAPIIdentifierMethod(method);
        Mediator seqMediator=null;
        Resource api_resource=null;
        API api=synCfg.getAPI(apiKey);
        if(api==null){
            log.error("API not found");
            try {
                this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_API_NOT_FOUND).toString());
            }catch (JSONException e){
                log.error("Unable to advertise command response",e);
            }
            return;
        }
        Resource[] resource_array=api.getResources();
        for (int counter=0;counter<resource_array.length;counter ++){
            if(mapping.equals(resource_array[counter].getDispatcherHelper().getString())){
                  for(String m1 : resource_array[counter].getMethods()){
                      if(m1.equals(method)){
                          api_resource=resource_array[counter];
                          break;
                      }
                  }
                  if(api_resource!=null){
                      break;
                  }
            }
        }
        if(api_resource!=null) {
            if (synapseSequenceType.equals(SynapseSequenceType.API_INSEQ)) {
                seqMediator = api_resource.getInSequence();
            } else if (synapseSequenceType.equals(SynapseSequenceType.API_OUTSEQ)) {
                seqMediator = api_resource.getOutSequence();
            } else if (synapseSequenceType.equals(SynapseSequenceType.API_FAULTSEQ)) {
                seqMediator = api_resource.getFaultSequence();
            }
        }else{
            log.error("API resource not found");
            try {
                this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_API_RESOURCE_NOT_FOUND).toString());
            }catch (JSONException e){
                log.error("Unable to advertise command response",e);
            }
            return;
        }
        if(seqMediator!=null) {
            Mediator current_mediator = null;
            for (int counter = 0; counter < position.length; counter++) {
                if(counter==0){
                    current_mediator = ((AbstractListMediator) seqMediator).getChild(position[counter]);
                }
                if(current_mediator!=null&&counter!=0) {
                    if(current_mediator instanceof InvokeMediator){
                        current_mediator=synCfg.getSequenceTemplate(((InvokeMediator)current_mediator).getTargetTemplate());
                    }
                    current_mediator = ((AbstractListMediator) current_mediator).getChild(position[counter]);
                }
            }
            if(current_mediator!=null) {
                breakPoint.setMediatorReference(current_mediator);
                if(registerMode) {
                    if (!((AbstractMediator) current_mediator).isBreakPoint()) {
                        ((AbstractMediator) current_mediator).setBreakPoint(true);
                        ((AbstractMediator) current_mediator).registerMediationFlowPoint(breakPoint);
                        log.info("DEBUG EVENT - Mediation flow : Registered breakpoint at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(true,null).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    } else {
                        log.error("Failed register breakpoint - Already breakpoint enabled at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_BREAKPOINT_ENABLED).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }
                }else{
                    if(((AbstractMediator)current_mediator).isBreakPoint()) {
                        ((AbstractMediator) current_mediator).setBreakPoint(false);
                        ((AbstractMediator) current_mediator).unregisterMediationFlowPoint();
                        log.info("DEBUG EVENT - Mediation flow : Unregistered breakpoint at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(true,null).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }else{
                        log.error("Failed unregister breakpoint - Already breakpoint disabled at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_BREAKPOINT_DISABLED).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }

                }
            }else {
                if(registerMode) {
                    log.error("Failed register breakpoint - Non existing mediator position");
                    try {
                        this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION).toString());
                    }catch (JSONException e){
                        log.error("Unable to advertise command response",e);
                    }
                }else{
                    log.error("Failed unregister breakpoint - Non existing mediator position");
                    try {
                        this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION).toString());
                    }catch (JSONException e){
                        log.error("Unable to advertise command response",e);
                    }
                }
            }
        }else{
            if(registerMode) {
                log.error("Failed register breakpoint - Non existing sequence");
                try {
                    this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_SEQUENCE).toString());
                }catch (JSONException e){
                    log.error("Unable to advertise command response",e);
                }
            }else{
                log.error("Failed unregister breakpoint - Non existing sequence");
                try {
                    this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_SEQUENCE).toString());
                }catch (JSONException e){
                    log.error("Unable to advertise command response",e);
                }

            }
        }
    }


   /*
   {
           "command":"set|clear",
           "command-argument":"breakpoint",
           "mediation-component":"sequence",
           "sequence":{
                       "proxy":{
                                     "proxy-key" : " example_proxy|....",
                                     "sequence-type": "proxy_inseq|proxy_outseq|...",
                                     "mediator-position" : " 0 1 2 3 ….."
                               }
                      }
    }
   */
    public void registerProxySequenceMediationFlowBreakPoint(String sequenceType,String proxyKey,int[] position,boolean registerMode){
        SynapseSequenceType synapseSequenceType = SynapseSequenceType.valueOf(sequenceType.toUpperCase());
        SynapseMediationFlowPoint breakPoint=new SynapseMediationFlowPoint();
        breakPoint.setSynapseMediationComponent(SynapseMediationComponent.SEQUENCE);
        breakPoint.setKey(proxyKey);
        breakPoint.setMediatorPosition(position);
        breakPoint.setSequenceMediationComponentIdentifier(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY);
        breakPoint.setSynapseSequenceType(synapseSequenceType);
        Mediator seqMediator=null;
        ProxyService proxy=null;
        proxy=synCfg.getProxyService(proxyKey);
        if(proxy!=null) {
            if (synapseSequenceType.equals(SynapseSequenceType.PROXY_INSEQ)) {
                seqMediator = proxy.getTargetInLineInSequence();
            } else if (synapseSequenceType.equals(SynapseSequenceType.PROXY_OUTSEQ)) {
                seqMediator = proxy.getTargetInLineOutSequence();
            } else if (synapseSequenceType.equals(SynapseSequenceType.PROXY_FAULTSEQ)) {
                seqMediator = proxy.getTargetInLineFaultSequence();
            }
        }else{
            log.error("Proxy not found");
            try {
                this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_PROXY_NOT_FOUND).toString());
            }catch (JSONException e){
                log.error("Unable to advertise command response",e);
            }
            return;
        }

        if(seqMediator!=null) {
            Mediator current_mediator = null;
            for (int counter = 0; counter < position.length; counter++) {
                if(counter==0){
                    current_mediator = ((AbstractListMediator) seqMediator).getChild(position[counter]);
                }
                if(current_mediator!=null&&counter!=0) {
                    if(current_mediator instanceof InvokeMediator){
                        current_mediator=synCfg.getSequenceTemplate(((InvokeMediator)current_mediator).getTargetTemplate());
                    }
                    current_mediator = ((AbstractListMediator) current_mediator).getChild(position[counter]);
                }
            }
            if(current_mediator!=null) {
                breakPoint.setMediatorReference(current_mediator);
                if(registerMode) {
                    if (!((AbstractMediator) current_mediator).isBreakPoint()) {
                        ((AbstractMediator) current_mediator).setBreakPoint(true);
                        ((AbstractMediator) current_mediator).registerMediationFlowPoint(breakPoint);
                        log.info("DEBUG EVENT - Mediation flow : Registered breakpoint at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(true,null).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    } else {
                        log.error("Failed register breakpoint - Already breakpoint enabled at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_BREAKPOINT_ENABLED).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }
                }else{
                    if(((AbstractMediator)current_mediator).isBreakPoint()) {
                        ((AbstractMediator) current_mediator).setBreakPoint(false);
                        ((AbstractMediator) current_mediator).unregisterMediationFlowPoint();
                        log.info("DEBUG EVENT - Mediation flow : Unregistered breakpoint at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(true,null).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }else{
                        log.error("Failed unregister breakpoint - Already breakpoint disabled at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_BREAKPOINT_DISABLED).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }

                }
            }else {
                if(registerMode) {
                    log.error("Failed register breakpoint - Non existing mediator position");
                    try {
                        this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION).toString());
                    }catch (JSONException e){
                        log.error("Unable to advertise command response",e);
                    }
                }else{
                    log.error("Failed unregister breakpoint - Non existing mediator position");
                    try {
                        this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION).toString());
                    }catch (JSONException e){
                        log.error("Unable to advertise command response",e);
                    }
                }
            }
        }else{
            if(registerMode) {
                log.error("Failed register breakpoint - Non existing sequence");
                try {
                    this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_SEQUENCE).toString());
                }catch (JSONException e){
                    log.error("Unable to advertise command response",e);
                }
            }else{
                log.error("Failed unregister breakpoint - Non existing sequence");
                try {
                    this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_SEQUENCE).toString());
                }catch (JSONException e){
                    log.error("Unable to advertise command response",e);
                }

            }
        }
    }


  /*
   {
           "command":"set|clear",
           "command-argument":"skip",
           "mediation-component":"sequence",
           "sequence":{
                       "proxy":{
                                     "proxy-key" : " example_proxy|....",
                                     "sequence-type": "proxy_inseq|proxy_outseq|...",
                                     "mediator-position" : " 0 1 2 3 ….."
                               }
                      }
    }
   */
    public void registerProxySequenceMediationFlowSkip(String seqType,String proxyKey,int[] position,boolean registerMode){
        SynapseSequenceType synapseSequenceType = SynapseSequenceType.valueOf(seqType.toUpperCase());
        SynapseMediationFlowPoint skipPoint=new SynapseMediationFlowPoint();
        skipPoint.setSynapseMediationComponent(SynapseMediationComponent.SEQUENCE);
        skipPoint.setKey(proxyKey);
        skipPoint.setMediatorPosition(position);
        skipPoint.setSequenceMediationComponentIdentifier(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY);
        skipPoint.setSynapseSequenceType(synapseSequenceType);
        Mediator seqMediator=null;
        ProxyService proxy=null;
        proxy=synCfg.getProxyService(proxyKey);
        if(proxy!=null) {
            if (synapseSequenceType.equals(SynapseSequenceType.PROXY_INSEQ)) {
                seqMediator = proxy.getTargetInLineInSequence();
            } else if (synapseSequenceType.equals(SynapseSequenceType.PROXY_OUTSEQ)) {
                seqMediator = proxy.getTargetInLineOutSequence();
            } else if (synapseSequenceType.equals(SynapseSequenceType.PROXY_FAULTSEQ)) {
                seqMediator = proxy.getTargetInLineFaultSequence();
            }
        }else{
            log.error("Proxy not found");
            try {
                this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_PROXY_NOT_FOUND).toString());
            }catch (JSONException e){
                log.error("Unable to advertise command response",e);
            }
            return;
        }
        if(seqMediator!=null) {
            Mediator current_mediator = null;
            for (int counter = 0; counter < position.length; counter++) {
                if(counter==0){
                    current_mediator = ((AbstractListMediator) seqMediator).getChild(position[counter]);
                }
                if(current_mediator!=null&&counter!=0) {
                    if(current_mediator instanceof InvokeMediator){
                        current_mediator=synCfg.getSequenceTemplate(((InvokeMediator)current_mediator).getTargetTemplate());
                    }
                    current_mediator = ((AbstractListMediator) current_mediator).getChild(position[counter]);
                }
            }
            if(current_mediator!=null) {
                skipPoint.setMediatorReference(current_mediator);
                if(registerMode) {
                    if (!((AbstractMediator) current_mediator).isSkipEnabled()) {
                        ((AbstractMediator) current_mediator).setSkipEnabled(true);
                        ((AbstractMediator) current_mediator).registerMediationFlowPoint(skipPoint);
                        log.info("DEBUG EVENT - Mediation flow : Registered skip at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(true,null).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    } else {
                        log.error("Failed register skip - Already skip enabled at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_SKIP_ENABLED).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }
                }else{
                    if(((AbstractMediator) current_mediator).isSkipEnabled()) {
                        ((AbstractMediator) current_mediator).setSkipEnabled(false);
                        ((AbstractMediator) current_mediator).unregisterMediationFlowPoint();
                        log.info("DEBUG EVENT - Mediation flow : Unregistered skip at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(true,null).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }else{
                        log.error("Failed unregister skip - Already skip disabled at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_SKIP_DISABLED).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }

                }
            }else {
                if(registerMode) {
                    log.error("Failed register skip - Non existing mediator position");
                    try {
                        this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION).toString());
                    }catch (JSONException e){
                        log.error("Unable to advertise command response",e);
                    }
                }else{
                    log.error("Failed unregister skip - Non existing mediator position");
                    try {
                        this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION).toString());
                    }catch (JSONException e){
                        log.error("Unable to advertise command response",e);
                    }
                }
            }
        }else{
            if(registerMode) {
                log.error("Failed register skip - Non existing sequence");
                try {
                    this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_SEQUENCE).toString());
                }catch (JSONException e){
                    log.error("Unable to advertise command response",e);
                }
            }else {
                log.error("Failed unregister skip - Non existing sequence");
                try {
                    this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_SEQUENCE).toString());
                }catch (JSONException e){
                    log.error("Unable to advertise command response",e);
                }
            }
        }
    }


   /*
    {
            "command":"set|clear",
            "command-argument":"breakpoint",
            "mediation-component":"connector",
            "connector":{
                            "connector-key": "jira|mailman|...... ",
                            "method-name" : " getUsers|deleteUser|....",
                            "mediator-position" : " 0 1 2 3 ….."
                        }

    }
    */
   public void registerConnectorMediationFlowBreakPoint(String connectorKey,String connectorMethod,int[] position,boolean registerMode){
        SynapseMediationFlowPoint breakPoint=new SynapseMediationFlowPoint();
        breakPoint.setSynapseMediationComponent(SynapseMediationComponent.CONNECTOR);
        breakPoint.setKey(connectorKey);
        breakPoint.setConnectorMediationComponentMethod(connectorMethod);
        breakPoint.setMediatorPosition(position);
        Mediator templateMediator=null;
        String template_key= SynapseDebugManagerConstants.CONNECTOR_PACKAGE+"."+connectorKey+"."+connectorMethod;
        templateMediator=synCfg.getSequenceTemplate(template_key);
        if(templateMediator!=null) {
            Mediator current_mediator = null;
            for (int counter = 0; counter < position.length; counter++) {
                if(counter==0){
                    current_mediator = ((AbstractListMediator) templateMediator).getChild(position[counter]);
                }
                if(current_mediator!=null&&counter!=0) {
                    if(current_mediator instanceof InvokeMediator){
                        current_mediator=synCfg.getSequenceTemplate(((InvokeMediator)current_mediator).getTargetTemplate());
                    }
                    current_mediator = ((AbstractListMediator) current_mediator).getChild(position[counter]);
                }
            }
            if(current_mediator!=null) {
                breakPoint.setMediatorReference(current_mediator);
                if(registerMode) {
                    if (!((AbstractMediator) current_mediator).isBreakPoint()) {
                        ((AbstractMediator) current_mediator).setBreakPoint(true);
                        ((AbstractMediator) current_mediator).registerMediationFlowPoint(breakPoint);
                        log.info("DEBUG EVENT - Mediation flow : Registered breakpoint at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(true,null).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    } else {
                        log.error("Failed register breakpoint - Already breakpoint enabled at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_BREAKPOINT_ENABLED).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }
                }else{
                    if(((AbstractMediator)current_mediator).isBreakPoint()) {
                        ((AbstractMediator) current_mediator).setBreakPoint(false);
                        ((AbstractMediator) current_mediator).unregisterMediationFlowPoint();
                        log.info("DEBUG EVENT - Mediation flow : Unregistered breakpoint at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(true,null).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }else{
                        log.error("Failed unregister breakpoint - Already breakpoint disabled at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_BREAKPOINT_DISABLED).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }

                }
            }else {
                if(registerMode) {
                    log.error("Failed register breakpoint - Non existing mediator position");
                    try {
                        this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION).toString());
                    }catch (JSONException e){
                        log.error("Unable to advertise command response",e);
                    }
                }else{
                    log.error("Failed unregister breakpoint - Non existing mediator position");
                    try {
                        this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION).toString());
                    }catch (JSONException e){
                        log.error("Unable to advertise command response",e);
                    }
                }
            }
        }else{
            if(registerMode) {
                log.error("Failed register breakpoint - Non existing template");
                try {
                    this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_TEMPLATE).toString());
                }catch (JSONException e){
                    log.error("Unable to advertise command response",e);
                }
            }else{
                log.error("Failed unregister breakpoint - Non existing template");
                try {
                    this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_TEMPLATE).toString());
                }catch (JSONException e){
                    log.error("Unable to advertise command response",e);
                }

            }
        }

    }


    /*
    {
            "command":"set|clear",
            "command-argument":"skip",
            "mediation-component":"connector",
            "connector":{
                            "connector-key": "jira|mailman|...... ",
                            "method-name" : " getUsers|deleteUser|....",
                            "mediator-position" : " 0 1 2 3 ….."
                        }

    }
    */
    public void registerConnectorMediationFlowSkip(String connectorKey,String connectorMethod,int[] position,boolean registerMode){
        SynapseMediationFlowPoint skipPoint=new SynapseMediationFlowPoint();
        skipPoint.setSynapseMediationComponent(SynapseMediationComponent.CONNECTOR);
        skipPoint.setKey(connectorKey);
        skipPoint.setConnectorMediationComponentMethod(connectorMethod);
        skipPoint.setMediatorPosition(position);
        Mediator templateMediator=null;
        String template_key= SynapseDebugManagerConstants.CONNECTOR_PACKAGE+"."+connectorKey+"."+connectorMethod;
        templateMediator=synCfg.getSequenceTemplate(template_key);
        if(templateMediator!=null) {
            Mediator current_mediator = null;
            for (int counter = 0; counter < position.length; counter++) {
                if(counter==0){
                    current_mediator = ((AbstractListMediator) templateMediator).getChild(position[counter]);
                }
                if(current_mediator!=null&&counter!=0) {
                    if(current_mediator instanceof InvokeMediator){
                        current_mediator=synCfg.getSequenceTemplate(((InvokeMediator)current_mediator).getTargetTemplate());
                    }
                    current_mediator = ((AbstractListMediator) current_mediator).getChild(position[counter]);
                }
            }
            if(current_mediator!=null) {
                skipPoint.setMediatorReference(current_mediator);
                if(registerMode) {
                    if (!((AbstractMediator) current_mediator).isSkipEnabled()) {
                        ((AbstractMediator) current_mediator).setSkipEnabled(true);
                        ((AbstractMediator) current_mediator).registerMediationFlowPoint(skipPoint);
                        log.info("DEBUG EVENT - Mediation flow : Registered skip at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(true,null).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    } else {
                        log.error("Failed register skip - Already skip enabled at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_SKIP_ENABLED).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }
                }else{
                    if(((AbstractMediator) current_mediator).isSkipEnabled()) {
                        ((AbstractMediator) current_mediator).setSkipEnabled(false);
                        ((AbstractMediator) current_mediator).unregisterMediationFlowPoint();
                        log.info("DEBUG EVENT - Mediation flow : Unregistered skip at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(true,null).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }else{
                        log.error("Failed unregister skip - Already skip disabled at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_SKIP_DISABLED).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }

                }
            }else {
                if(registerMode) {
                    log.error("Failed register skip - Non existing mediator position");
                    try {
                        this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION).toString());
                    }catch (JSONException e){
                        log.error("Unable to advertise command response",e);
                    }
                }else{
                    log.error("Failed unregister skip - Non existing mediator position");
                    try {
                        this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION).toString());
                    }catch (JSONException e){
                        log.error("Unable to advertise command response",e);
                    }
                }
            }
        }else{
            if(registerMode) {
                log.error("Failed register skip - Non existing template");
                try {
                    this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_TEMPLATE).toString());
                }catch (JSONException e){
                    log.error("Unable to advertise command response",e);
                }
            }else{
                log.error("Failed unregister skip - Non existing template");
                try {
                    this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_TEMPLATE).toString());
                }catch (JSONException e){
                    log.error("Unable to advertise command response",e);
                }

            }
        }

    }


   /*
   {
         "command":"set|clear",
         "command-argument":"skip",
         "mediation-component":"template",
         "template":{
                          "template-key" : "hello_world_template|....",
                          "mediator-position" : " 0 1 2 3 ….."
                    }
   }
   */
    public void registerTemplateMediationFlowSkip(String templateKey,int[] position,boolean registerMode) {
        SynapseMediationFlowPoint skipPoint = new SynapseMediationFlowPoint();
        skipPoint.setSynapseMediationComponent(SynapseMediationComponent.TEMPLATE);
        skipPoint.setKey(templateKey);
        skipPoint.setMediatorPosition(position);
        Mediator templateMediator = null;
        templateMediator = synCfg.getSequenceTemplate(templateKey);
        if(templateMediator!=null) {
            Mediator current_mediator = null;
            for (int counter = 0; counter < position.length; counter++) {
                if(counter==0){
                    current_mediator = ((AbstractListMediator) templateMediator).getChild(position[counter]);
                }
                if(current_mediator!=null&&counter!=0) {
                    if(current_mediator instanceof InvokeMediator){
                        current_mediator=synCfg.getSequenceTemplate(((InvokeMediator)current_mediator).getTargetTemplate());
                    }
                    current_mediator = ((AbstractListMediator) current_mediator).getChild(position[counter]);
                }
            }
            if(current_mediator!=null) {
                skipPoint.setMediatorReference(current_mediator);
                if(registerMode) {
                    if (!((AbstractMediator) current_mediator).isSkipEnabled()) {
                        ((AbstractMediator) current_mediator).setSkipEnabled(true);
                        ((AbstractMediator) current_mediator).registerMediationFlowPoint(skipPoint);
                        log.info("DEBUG EVENT - Mediation flow : Registered skip at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(true,null).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    } else {
                        log.error("Failed register skip - Already skip enabled at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_SKIP_ENABLED).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }
                }else{
                    if(((AbstractMediator) current_mediator).isSkipEnabled()) {
                        ((AbstractMediator) current_mediator).setSkipEnabled(false);
                        ((AbstractMediator) current_mediator).unregisterMediationFlowPoint();
                        log.info("DEBUG EVENT - Mediation flow : Unregistered skip at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(true,null).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }else{
                        log.error("Failed unregister skip - Already skip disabled at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_SKIP_DISABLED).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }

                }
            }else {
                if(registerMode) {
                    log.error("Failed register skip - Non existing mediator position");
                    try {
                        this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION).toString());
                    }catch (JSONException e){
                        log.error("Unable to advertise command response",e);
                    }
                }else{
                    log.error("Failed unregister skip - Non existing mediator position");
                    try {
                        this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION).toString());
                    }catch (JSONException e){
                        log.error("Unable to advertise command response",e);
                    }
                }
            }
        }else{
            if(registerMode) {
                log.error("Failed register skip - Non existing template");
                try {
                    this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_TEMPLATE).toString());
                }catch (JSONException e){
                    log.error("Unable to advertise command response",e);
                }
            }else{
                log.error("Failed unregister skip - Non existing template");
                try {
                    this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_TEMPLATE).toString());
                }catch (JSONException e){
                    log.error("Unable to advertise command response",e);
                }

            }
        }

    }


  /*
   {
         "command":"set|clear",
         "command-argument":"breakpoint",
         "mediation-component":"template",
         "template":{
                          "template-key" : "hello_world_template|....",
                          "mediator-postion" : " 0 1 2 3 ….."
                    }
   }
   */
    public void registerTemplateMediationFlowBreakPoint(String templateKey,int[] position,boolean registerMode){
        SynapseMediationFlowPoint breakPoint=new SynapseMediationFlowPoint();
        breakPoint.setSynapseMediationComponent(SynapseMediationComponent.TEMPLATE);
        breakPoint.setKey(templateKey);
        breakPoint.setMediatorPosition(position);
        Mediator templateMediator=null;
        templateMediator=synCfg.getSequenceTemplate(templateKey);
        if(templateMediator!=null) {
            Mediator current_mediator = null;
            for (int counter = 0; counter < position.length; counter++) {
                if(counter==0){
                    current_mediator = ((AbstractListMediator) templateMediator).getChild(position[counter]);
                }
                if(current_mediator!=null&&counter!=0) {
                    if(current_mediator instanceof InvokeMediator){
                        current_mediator=synCfg.getSequenceTemplate(((InvokeMediator)current_mediator).getTargetTemplate());
                    }
                    current_mediator = ((AbstractListMediator) current_mediator).getChild(position[counter]);
                }
            }
            if(current_mediator!=null) {
                breakPoint.setMediatorReference(current_mediator);
                if(registerMode) {
                    if (!((AbstractMediator) current_mediator).isBreakPoint()) {
                        ((AbstractMediator) current_mediator).setBreakPoint(true);
                        ((AbstractMediator) current_mediator).registerMediationFlowPoint(breakPoint);
                        log.info("DEBUG EVENT - Mediation flow : Registered breakpoint at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(true,null).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    } else {
                        log.error("Failed register breakpoint - Already breakpoint enabled at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_BREAKPOINT_ENABLED).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }
                }else{
                    if(((AbstractMediator)current_mediator).isBreakPoint()) {
                        ((AbstractMediator) current_mediator).setBreakPoint(false);
                        ((AbstractMediator) current_mediator).unregisterMediationFlowPoint();
                        log.info("DEBUG EVENT - Mediation flow : Unregistered breakpoint at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(true,null).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }else{
                        log.error("Failed unregister breakpoint - Already breakpoint disabled at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_BREAKPOINT_DISABLED).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }

                }
            }else {
                if(registerMode) {
                    log.error("Failed register breakpoint - Non existing mediator position");
                    try {
                        this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION).toString());
                    }catch (JSONException e){
                        log.error("Unable to advertise command response",e);
                    }
                }else{
                    log.error("Failed unregister breakpoint - Non existing mediator position");
                    try {
                        this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION).toString());
                    }catch (JSONException e){
                        log.error("Unable to advertise command response",e);
                    }
                }
            }
        }else{
            if(registerMode) {
                log.error("Failed register breakpoint - Non existing template");
                try {
                    this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_TEMPLATE).toString());
                }catch (JSONException e){
                    log.error("Unable to advertise command response",e);
                }
            }else{
                log.error("Failed unregister breakpoint - Non existing template");
                try {
                    this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_TEMPLATE).toString());
                }catch (JSONException e){
                    log.error("Unable to advertise command response",e);
                }

            }
        }

    }


    public void advertiseCommandResponse(String commandResponse){
        if(synEnv.isDebugEnabled()){
            debugInterface.getPortListenWriter().println(commandResponse);
            debugInterface.getPortListenWriter().flush();
        }
    }


   public void advertiseDebugEvent(String event){
        if(synEnv.isDebugEnabled()){
            debugInterface.getPortSendWriter().println(event);
            debugInterface.getPortSendWriter().flush();
        }
    }


    /*
    {
         "command":"set|clear",
         "command-argument":"skip",
         "mediation-component":"sequence",
         "sequence":{
                             "sequence-type": "named",
                             "sequence-key" : " main|fault....",
                             "mediator-position" : " 0 1 2 3 ….."
                    }
    }
    */
    public void registerSequenceMediationFlowSkip(String seqType,String seqKey,int[] position,boolean registerMode){
        SynapseSequenceType synapseSequenceType = SynapseSequenceType.valueOf(seqType.toUpperCase());
        SynapseMediationFlowPoint skipPoint=new SynapseMediationFlowPoint();
        skipPoint.setSynapseMediationComponent(SynapseMediationComponent.SEQUENCE);
        skipPoint.setKey(seqKey);
        skipPoint.setMediatorPosition(position);
        skipPoint.setSynapseSequenceType(synapseSequenceType);
        skipPoint.setSequenceMediationComponentIdentifier(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE);
        Mediator seqMediator=null;
        if(synapseSequenceType.equals(SynapseSequenceType.NAMED)){
            seqMediator=synCfg.getSequence(seqKey);
        }
        if(seqMediator!=null) {
            Mediator current_mediator = null;
            for (int counter = 0; counter < position.length; counter++) {
                if(counter==0){
                    current_mediator = ((AbstractListMediator) seqMediator).getChild(position[counter]);
                }
                if(current_mediator!=null&&counter!=0) {
                    if(current_mediator instanceof InvokeMediator){
                        current_mediator=synCfg.getSequenceTemplate(((InvokeMediator)current_mediator).getTargetTemplate());
                    }
                    current_mediator = ((AbstractListMediator) current_mediator).getChild(position[counter]);
                }
            }
            if(current_mediator!=null) {
                skipPoint.setMediatorReference(current_mediator);
                if(registerMode) {
                    if (!((AbstractMediator) current_mediator).isSkipEnabled()) {
                        ((AbstractMediator) current_mediator).setSkipEnabled(true);
                        ((AbstractMediator) current_mediator).registerMediationFlowPoint(skipPoint);
                        log.info("DEBUG EVENT - Mediation flow : Registered skip at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(true,null).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    } else {
                        log.error("Failed register skip - Already skip enabled at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_SKIP_ENABLED).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }
                }else{
                    if(((AbstractMediator) current_mediator).isSkipEnabled()) {
                        ((AbstractMediator) current_mediator).setSkipEnabled(false);
                        ((AbstractMediator) current_mediator).unregisterMediationFlowPoint();
                        log.info("DEBUG EVENT - Mediation flow : Unregistered skip at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(true,null).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }else{
                        log.error("Failed unregister skip - Already skip disabled at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_SKIP_DISABLED).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }

                }
            }else {
                if(registerMode) {
                    log.error("Failed register skip - Non existing mediator position");
                    try {
                        this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION).toString());
                    }catch (JSONException e){
                        log.error("Unable to advertise command response",e);
                    }
                }else{
                    log.error("Failed unregister skip - Non existing mediator position");
                    try {
                        this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION).toString());
                    }catch (JSONException e){
                        log.error("Unable to advertise command response",e);
                    }
                }
            }
        }else{
            if(registerMode) {
                log.error("Failed register skip - Non existing sequence");
                try {
                    this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_SEQUENCE).toString());
                }catch (JSONException e){
                    log.error("Unable to advertise command response",e);
                }
            }else {
                log.error("Failed unregister skip - Non existing sequence");
                try {
                    this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_SEQUENCE).toString());
                }catch (JSONException e){
                    log.error("Unable to advertise command response",e);
                }
            }
        }
    }


   /*
    {
         "command":"set|clear",
         "command-argument":"breakpoint",
         "mediation-component":"sequence",
         "sequence":{
                             "sequence-type": "named",
                             "sequence-key" : " main|fault....",
                             "mediator-position" : " 0 1 2 3 ….."
                    }
    }
    */
    public void registerSequenceMediationFlowBreakPoint(String sequenceType,String seqKey,int[] position,boolean registerMode){
        SynapseSequenceType synapseSequenceType = SynapseSequenceType.valueOf(sequenceType.toUpperCase());
        SynapseMediationFlowPoint breakPoint=new SynapseMediationFlowPoint();
        breakPoint.setSynapseMediationComponent(SynapseMediationComponent.SEQUENCE);
        breakPoint.setKey(seqKey);
        breakPoint.setMediatorPosition(position);
        breakPoint.setSynapseSequenceType(synapseSequenceType);
        breakPoint.setSequenceMediationComponentIdentifier(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE);
        Mediator seqMediator=null;
        if(synapseSequenceType.equals(SynapseSequenceType.NAMED)){
            seqMediator=synCfg.getSequence(seqKey);
        }
        if(seqMediator!=null) {
            Mediator current_mediator = null;
            for (int counter = 0; counter < position.length; counter++) {
                if(counter==0){
                    current_mediator = ((AbstractListMediator) seqMediator).getChild(position[counter]);
                }
                if(current_mediator!=null&&counter!=0) {
                    if(current_mediator instanceof InvokeMediator){
                        current_mediator=synCfg.getSequenceTemplate(((InvokeMediator)current_mediator).getTargetTemplate());
                    }
                    current_mediator = ((AbstractListMediator) current_mediator).getChild(position[counter]);
                }
            }
            if(current_mediator!=null) {
                breakPoint.setMediatorReference(current_mediator);
                if(registerMode) {
                    if (!((AbstractMediator) current_mediator).isBreakPoint()) {
                        ((AbstractMediator) current_mediator).setBreakPoint(true);
                        ((AbstractMediator) current_mediator).registerMediationFlowPoint(breakPoint);
                        log.info("DEBUG EVENT - Mediation flow : Registered breakpoint at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(true,null).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    } else {
                        log.error("Failed register breakpoint - Already breakpoint enabled at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_BREAKPOINT_ENABLED).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }
                }else{
                    if(((AbstractMediator)current_mediator).isBreakPoint()) {
                        ((AbstractMediator) current_mediator).setBreakPoint(false);
                        ((AbstractMediator) current_mediator).unregisterMediationFlowPoint();
                        log.info("DEBUG EVENT - Mediation flow : Unregistered breakpoint at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(true,null).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }else{
                        log.error("Failed unregister breakpoint - Already breakpoint disabled at mediator position");
                        try {
                            this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_ALREADY_BREAKPOINT_DISABLED).toString());
                        }catch (JSONException e){
                            log.error("Unable to advertise command response",e);
                        }
                    }

                }
            }else {
                if(registerMode) {
                    log.error("Failed register breakpoint - Non existing mediator position");
                    try {
                        this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION).toString());
                    }catch (JSONException e){
                        log.error("Unable to advertise command response",e);
                    }
                }else{
                    log.error("Failed unregister breakpoint - Non existing mediator position");
                    try {
                        this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION).toString());
                    }catch (JSONException e){
                        log.error("Unable to advertise command response",e);
                    }
                }
            }
        }else{
            if(registerMode) {
                log.error("Failed register breakpoint - Non existing sequence");
                try {
                    this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_SEQUENCE).toString());
                }catch (JSONException e){
                    log.error("Unable to advertise command response",e);
                }
            }else{
                log.error("Failed unregister breakpoint - Non existing sequence");
                try {
                    this.advertiseCommandResponse(createDebugCommandResponse(false, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_NON_EXISTING_SEQUENCE).toString());
                }catch (JSONException e){
                    log.error("Unable to advertise command response",e);
                }

            }
        }
    }


    public void debugResume(){
        this.transitMediationFlowStateToActive();
        try {
            this.advertiseCommandResponse(createDebugCommandResponse(true,null).toString());
        }catch (JSONException e){
            log.error("Unable to advertise command response",e);
        }
    }


  /*
        positive
        {
              "command-response":"successful"
        }
        negative
        {
               "command-response":"failed",
               "failed-reason":"non-existing sequence|non-existing mediator position|already set breakpoint|...."
        }
   */
   public JSONObject createDebugCommandResponse(boolean isPositive,String failedReason) throws JSONException {
        JSONObject response=null;
        response = new JSONObject();
        if (isPositive) {
            response.put(SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_SUCCESSFUL);
        }else{
            response.put(SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE, SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_FAILED);
            response.put(SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_FAILED_REASON,failedReason);
        }

       return response;
    }


   /*
    {
         "event":"breakpoint|skip",
         "mediation-component":"sequence",
          "sequence":{
                             "sequence-type": "named",
                             "sequence-key" : " main|fault....",
                             "mediator-position" : " 0 1 2 3 ….."
                      }
     }

     {
         "event":"breakpoint|skip",
         "mediation-component":"template",
         "template":{
                                "template-key" : "hello_world_template|....",
                                "mediator-position" : " 0 1 2 3 …"
                    }
     }

     {
         "event":"breakpoint|skip",
          "mediation-component":"connector",
          "connector":{
                                     "connector-key": "jira|mailman|...... ",
                                     "method-name" : " getUsers|deleteUser|....",
                                     "mediator-postion" : " 0 1 2 3 ….."
                      }
     }
    */
    public JSONObject createDebugMediationFlowPointHitEvent(boolean isBreakpoint,SynapseMediationFlowPoint point) {
           JSONObject event=null;

            try {
                event = new JSONObject();
                if (isBreakpoint) {
                    event.put(SynapseDebugEventConstants.DEBUG_EVENT, SynapseDebugEventConstants.DEBUG_EVENT_BREAKPOINT);
                } else {
                    event.put(SynapseDebugEventConstants.DEBUG_EVENT, SynapseDebugEventConstants.DEBUG_EVENT_SKIP);
                }
                JSONObject parameters = new JSONObject();
                if (point.getSynapseMediationComponent().equals(SynapseMediationComponent.SEQUENCE)) {
                    event.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT, SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE);
                    if (point.getSequenceMediationComponentIdentifier().equals(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE)) {
                        parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_KEY, point.getKey());
                        parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_TYPE, point.getSynapseSequenceType().toString().toLowerCase());
                        parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION, toString(point.getMediatorPosition()));
                        event.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE, parameters);
                    } else if (point.getSequenceMediationComponentIdentifier().equals(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY)) {
                        JSONObject proxy_parameters = new JSONObject();
                        proxy_parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY_KEY, point.getKey());
                        proxy_parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_TYPE, point.getSynapseSequenceType().toString().toLowerCase());
                        proxy_parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION, toString(point.getMediatorPosition()));
                        parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY, proxy_parameters);
                        event.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE, parameters);
                    } else if (point.getSequenceMediationComponentIdentifier().equals(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API)) {
                        JSONObject api_parameters = new JSONObject();
                        api_parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_KEY, point.getKey());
                        JSONObject resource = new JSONObject();
                        resource.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE_MAPPING, point.getAPIIdentifierMapping());
                        resource.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE_METHOD, point.getAPIIdentifierMethod());
                        api_parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE, resource);
                        api_parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_TYPE, point.getSynapseSequenceType().toString().toLowerCase());
                        api_parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION, toString(point.getMediatorPosition()));
                        parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API, api_parameters);
                        event.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE, parameters);
                    }

                } else if (point.getSynapseMediationComponent().equals(SynapseMediationComponent.TEMPLATE)) {
                    event.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT, SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_TEMPLATE);
                    parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_TEMPLATE_KEY, point.getKey());
                    parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION, toString(point.getMediatorPosition()));
                    event.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_TEMPLATE, parameters);
                } else if (point.getSynapseMediationComponent().equals(SynapseMediationComponent.CONNECTOR)) {
                    event.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT, SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_CONNECTOR);
                    parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_CONNECTOR_KEY, point.getKey());
                    parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_CONNECTOR_METHOD, point.getConnectorMediationComponentMethod());
                    parameters.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION, toString(point.getMediatorPosition()));
                    event.put(SynapseDebugCommandConstants.DEBUG_COMMAND_MEDIATION_COMPONENT_CONNECTOR, parameters);

                }
            }catch (JSONException ex){
                log.error("Failed to create debug event in JSON format",ex);
            }


        return event;
    }


    /*
    {
         "event":"suspended|resume|.....",
    }
     */
    public JSONObject createDebugEvent (String eventString) {
        JSONObject event=null;
        try {
            event = new JSONObject();
            event.put(SynapseDebugEventConstants.DEBUG_EVENT, eventString);
        }catch (JSONException ex){
            log.error("Failed to create debug event in JSON format",ex);
        }
        return event;
    }


    /*
    {
         "event":"suspended|resume|.....",
         "message-receiver":"synapse|proxy|.....",
    }
     */
    public JSONObject createDebugEvent(String eventString,String messageReceiver){
        JSONObject event=null;
        try {
            event = new JSONObject();
            event.put(SynapseDebugEventConstants.DEBUG_EVENT, eventString);
            event.put(SynapseDebugEventConstants.DEBUG_EVENT_MESSAGE_RECEIVER, messageReceiver);
        }catch (JSONException ex){
            log.error("Failed to create debug event in JSON format",ex);
        }
        return event;
    }


    /*
    {
         "event":"suspended|resume|.....",
         "message-receiver":"synapse|proxy|.....",
         "callback-receiver":"synapse|.....",
    }
     */
    public JSONObject createDebugEvent(String eventString,String messageReceiver,String callbackReceiver){
        JSONObject event=null;
        try{
           event = new JSONObject();
           event.put(SynapseDebugEventConstants.DEBUG_EVENT,eventString);
           event.put(SynapseDebugEventConstants.DEBUG_EVENT_MESSAGE_RECEIVER,messageReceiver);
           event.put(SynapseDebugEventConstants.DEBUG_EVENT_CALLBACK_RECEIVER,callbackReceiver);
        }catch (JSONException ex){
            log.error("Failed to create debug event in JSON format",ex);
        }
        return event;
    }



    public String toString(int[] position){
        String positionString="";
        for(int counter=0;counter<position.length;counter++){
            positionString=positionString.concat(String.valueOf(position[counter])).concat(" ");
        }
        return  positionString;
   }


   /*
    {
         "command":"get",
         "command-argument":"properties",
         "context":"synapse(default)|axis2|transport|axis2-client|operation",
    }
    {
         "command":"get",
         "command-argument":"property",
         "context":"synapse(default)|axis2|transport|axis2-client|operation",
         "property":{
                                 "property-name":"HTTP_SC|.......",
                    }
    }
    */
    public void acquireMediationFlowPointProperties(String propertyOrProperties,String propertyContext, JSONObject property_arguments){
       try {
        if(propertyOrProperties.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTIES)) {
            if(propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_ALL)) {
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
            }else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2)){
                JSONObject data_axis2 = getAxis2Properties();
                JSONObject data_axis2_prop = new JSONObject();
                data_axis2_prop.put(SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_PROPERTY_CONTEXT_AXIS2, data_axis2);
                debugInterface.getPortListenWriter().println(data_axis2_prop.toString());
                debugInterface.getPortListenWriter().flush();
            }else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_SYNAPSE)||propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_DEFAULT)){
                JSONObject data_synapse = new JSONObject(((Axis2MessageContext) synCtx).getProperties());
                JSONObject data_synapse_prop = new JSONObject();
                data_synapse_prop.put(SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_PROPERTY_CONTEXT_SYNAPSE, data_synapse);
                debugInterface.getPortListenWriter().println(data_synapse_prop.toString());
                debugInterface.getPortListenWriter().flush();
            }else if(propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2CLIENT)){
                JSONObject data_axis2 = new JSONObject(((Axis2MessageContext) synCtx).getAxis2MessageContext().getOptions().getProperties());
                JSONObject data_axis2_prop = new JSONObject();
                data_axis2_prop.put(SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_PROPERTY_CONTEXT_AXIS2CLIENT, data_axis2);
                debugInterface.getPortListenWriter().println(data_axis2_prop.toString());
                debugInterface.getPortListenWriter().flush();
            }else if(propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_TRANSPORT)){
                JSONObject data_axis2 = new JSONObject((Map)((Axis2MessageContext) synCtx).getAxis2MessageContext().getProperty(
                        org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS));
                JSONObject data_axis2_prop = new JSONObject();
                data_axis2_prop.put(SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_PROPERTY_CONTEXT_AXIS2TRANSPORT, data_axis2);
                debugInterface.getPortListenWriter().println(data_axis2_prop.toString());
                debugInterface.getPortListenWriter().flush();
            }else if(propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_OPERATION)){
                JSONObject data_axis2 = new JSONObject(((Axis2MessageContext) synCtx).getAxis2MessageContext().getOperationContext().getProperties());
                JSONObject data_axis2_prop = new JSONObject();
                data_axis2_prop.put(SynapseDebugCommandConstants.DEBUG_COMMAND_RESPONSE_PROPERTY_CONTEXT_AXIS2OPERATION, data_axis2);
                debugInterface.getPortListenWriter().println(data_axis2_prop.toString());
                debugInterface.getPortListenWriter().flush();
            }
        }else if(propertyOrProperties.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY)){
           if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2)){
               JSONObject data_axis2 = getAxis2Properties();
               Object result=null;
               if(data_axis2.has(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME))){
                    result=data_axis2.get(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME));
               }
               JSONObject json_result=new JSONObject();
               json_result.put(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME),result);
               debugInterface.getPortListenWriter().println(json_result.toString());
               debugInterface.getPortListenWriter().flush();

            }else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_SYNAPSE)||propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_DEFAULT)){
               JSONObject data_synapse = new JSONObject(((Axis2MessageContext) synCtx).getProperties());
               Object result=null;
               if(data_synapse.has(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME))){
                   result=data_synapse.getJSONObject(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME));
               }
               JSONObject json_result=new JSONObject();
               json_result.put(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME),result);
               debugInterface.getPortListenWriter().println(json_result.toString());
               debugInterface.getPortListenWriter().flush();

            }else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2CLIENT)){
               JSONObject data_axis2 = new JSONObject(((Axis2MessageContext) synCtx).getAxis2MessageContext().getOptions().getProperties());
               Object result=null;
               if(data_axis2.has(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME))){
                   result=data_axis2.get(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME));
               }
               JSONObject json_result=new JSONObject();
               json_result.put(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME),result);
               debugInterface.getPortListenWriter().println(json_result.toString());
               debugInterface.getPortListenWriter().flush();

           }else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_TRANSPORT)){
               JSONObject data_axis2 = new JSONObject((Map)((Axis2MessageContext) synCtx).getAxis2MessageContext().getProperty(
                       org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS));
               Object result=null;
               if(data_axis2.has(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME))){
                   result=data_axis2.get(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME));
               }
               JSONObject json_result=new JSONObject();
               json_result.put(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME),result);
               debugInterface.getPortListenWriter().println(json_result.toString());
               debugInterface.getPortListenWriter().flush();

           }else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_OPERATION)){
               JSONObject data_axis2 = new JSONObject(((Axis2MessageContext) synCtx).getAxis2MessageContext().getOperationContext().getProperties());
               Object result=null;
               if(data_axis2.has(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME))){
                   result=data_axis2.get(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME));
               }
               JSONObject json_result=new JSONObject();
               json_result.put(property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME),result);
               debugInterface.getPortListenWriter().println(json_result.toString());
               debugInterface.getPortListenWriter().flush();
           }
        }
        }catch(JSONException ex){
           log.error("Failed to acquire property in the scope: "+propertyContext,ex);
        }
    }




    public JSONObject getAxis2Properties() throws JSONException {
      JSONObject result=new JSONObject();
        result.put("To",synCtx.getTo() != null ? synCtx.getTo().getAddress() : "" );
        result.put("From",synCtx.getFrom() != null ? synCtx.getFrom().getAddress() : "" );
        result.put("WSAction",synCtx.getWSAAction() != null ? synCtx.getWSAAction() : "" );
        result.put("SOAPAction",synCtx.getSoapAction() != null ? synCtx.getSoapAction() : "" );
        result.put("ReplyTo",synCtx.getReplyTo() != null ? synCtx.getReplyTo().getAddress() : "" );
        result.put("MessageID",synCtx.getMessageID() != null ? synCtx.getMessageID() : "" );
        result.put("Direction",synCtx.isResponse()?  "response" : "request" );
        result.put("Envelope",synCtx.getEnvelope() != null?  synCtx.getEnvelope().toString() : "" );
        JSONObject soapHeader=new JSONObject();
        if (synCtx.getEnvelope() != null) {
            SOAPHeader header = synCtx.getEnvelope().getHeader();
            if (header != null) {
                for (Iterator iter = header.examineAllHeaderBlocks(); iter.hasNext();) {
                    Object o = iter.next();
                    if (o instanceof SOAPHeaderBlock) {
                        SOAPHeaderBlock headerBlk = (SOAPHeaderBlock) o;
                        soapHeader.put(headerBlk.getLocalName(),headerBlk.getText());
                    } else if (o instanceof OMElement) {
                        OMElement headerElem = (OMElement) o;
                        soapHeader.put(headerElem.getLocalName(),headerElem.getText());

                    }
                }
            }
        }
        result.put("SoapHeader",soapHeader);
        JSONObject transportHeader=new JSONObject((Map)((Axis2MessageContext) synCtx).getAxis2MessageContext().getProperty(
                org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS));
        result.put("TransportHeaders",transportHeader);
        result.put("ExcessTransportHeaders",((Axis2MessageContext) synCtx).getAxis2MessageContext().getProperty("EXCESS_TRANSPORT_HEADERS"));
        result.put("MessageType",((Axis2MessageContext) synCtx).getAxis2MessageContext().getProperty("messageType"));
        result.put("ContentType",((Axis2MessageContext) synCtx).getAxis2MessageContext().getProperty("ContentType"));
        return result;

    }

    /*
    {
         "command":"set|clear",
         "command-argument":"property",
         "context":"synapse(default)|axis2|transport|axis2-client|operation",
         "property":{
                                 "property-name":"HTTP_SC|.......",
                                 "property-value":"HTTP_SC|......."
                    }

    }
    */

    public void addMediationFlowPointProperty(String propertyContext, JSONObject property_arguments,boolean isActionSet){
        try {
            String property_key=property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_NAME);
            String property_value=property_arguments.getString(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_VALUE);//bug
            if (isActionSet) {

                if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_DEFAULT) || propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_SYNAPSE)) {
                    //Setting property into the  Synapse Context
                    synCtx.setProperty(property_key, property_value);

                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2)
                        && synCtx instanceof Axis2MessageContext) {
                    //Setting property into the  Axis2 Message Context
                    Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                    org.apache.axis2.context.MessageContext axis2MessageCtx =
                            axis2smc.getAxis2MessageContext();
                    axis2MessageCtx.setProperty(property_key, property_value);
                    if (org.apache.axis2.Constants.Configuration.MESSAGE_TYPE.equals(property_key)) {
                        axis2MessageCtx.setProperty(org.apache.axis2.Constants.Configuration.CONTENT_TYPE, property_value);
                        Object o = axis2MessageCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
                        Map _headers = (Map) o;
                        if (_headers != null) {
                            _headers.remove(HTTP.CONTENT_TYPE);
                            _headers.put(HTTP.CONTENT_TYPE, property_value);
                        }
                    }

                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2CLIENT)
                        && synCtx instanceof Axis2MessageContext) {
                    //Setting property into the  Axis2 Message Context client options
                    Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                    org.apache.axis2.context.MessageContext axis2MessageCtx =
                            axis2smc.getAxis2MessageContext();
                    axis2MessageCtx.getOptions().setProperty(property_key, property_value);

                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_TRANSPORT)
                        && synCtx instanceof Axis2MessageContext) {
                    //Setting Transport Headers
                    Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                    org.apache.axis2.context.MessageContext axis2MessageCtx =
                            axis2smc.getAxis2MessageContext();
                    Object headers = axis2MessageCtx.getProperty(
                            org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

                    if (headers != null && headers instanceof Map) {
                        Map headersMap = (Map) headers;
                        headersMap.put(property_key, property_value);
                    }
                    if (headers == null) {
                        Map headersMap = new HashMap();
                        headersMap.put(property_key, property_value);
                        axis2MessageCtx.setProperty(
                                org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS,
                                headersMap);
                    }
                }else if(propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_OPERATION)
                        && synCtx instanceof Axis2MessageContext){
                    //Setting Transport Headers
                    Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                    org.apache.axis2.context.MessageContext axis2MessageCtx =
                            axis2smc.getAxis2MessageContext();
                    axis2smc.getAxis2MessageContext().getOperationContext().setProperty(property_key, property_value);
                }

            } else {
                if (propertyContext == null || XMLConfigConstants.SCOPE_DEFAULT.equals(propertyContext)) {
                    //Removing property from the  Synapse Context
                    Set pros = synCtx.getPropertyKeySet();
                    if (pros != null) {
                        pros.remove(property_key);
                    }

                } else if ((propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2) ||
                        propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2CLIENT))
                        && synCtx instanceof Axis2MessageContext) {

                    //Removing property from the Axis2 Message Context
                    Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                    org.apache.axis2.context.MessageContext axis2MessageCtx =
                            axis2smc.getAxis2MessageContext();
                    axis2MessageCtx.removeProperty(property_key);

                } else if (propertyContext.equals(SynapseDebugCommandConstants.DEBUG_COMMAND_PROPERTY_CONTEXT_TRANSPORT)
                        && synCtx instanceof Axis2MessageContext) {
                    // Removing transport headers
                    Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
                    org.apache.axis2.context.MessageContext axis2MessageCtx =
                            axis2smc.getAxis2MessageContext();
                    Object headers = axis2MessageCtx.getProperty(
                            org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
                    if (headers != null && headers instanceof Map) {
                        Map headersMap = (Map) headers;
                        headersMap.remove(property_key);
                    } else {

                    }
                }
            }

        } catch (JSONException e) {
            log.error("Failed to set or remove property in the scope: "+propertyContext,e);
        }
        try {
            this.advertiseCommandResponse(createDebugCommandResponse(true,null).toString());                   //need to error handle
        }catch (JSONException e){
            log.error("Unable to advertise command response",e);
        }
    }

}
