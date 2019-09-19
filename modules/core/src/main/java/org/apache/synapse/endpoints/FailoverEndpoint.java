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

package org.apache.synapse.endpoints;

import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.AxisFault;
import org.apache.axis2.databinding.types.soapencoding.Integer;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.aspects.ComponentType;
import org.apache.synapse.aspects.flow.statistics.collectors.CloseEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.OpenEventCollector;
import org.apache.synapse.aspects.flow.statistics.collectors.RuntimeStatisticCollector;
import org.apache.synapse.config.SynapsePropertiesLoader;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.Pipe;
import org.apache.synapse.transport.passthru.util.RelayUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;

/**
 * FailoverEndpoint can have multiple child endpoints. It will always try to send messages to
 * current endpoint. If the current endpoint is failing, it gets another active endpoint from the
 * list and make it the current endpoint. Then the message is sent to the current endpoint and if
 * it fails, above procedure repeats until there are no active endpoints. If all endpoints are
 * failing and parent endpoint is available, this will delegate the problem to the parent endpoint.
 * If parent endpoint is not available it will pop the next FaultHandler and delegate the problem
 * to that.
 */
public class FailoverEndpoint extends AbstractEndpoint {

    /** Endpoint for which is currently used */
    private Endpoint currentEndpoint = null;

    /** The fail-over mode supported by this endpoint. By default we do dynamic fail-over */
    private boolean dynamic = true;

    /**
     * Check if buildMessage attribute explicitly mentioned in config
     */
    private boolean isBuildMessageAttAvailable = false;

    /**
     * Overwrite the global synapse property build.message.on.failover.enable. Default false.
     */
    private boolean buildMessageAtt = false;

    /** check message need to be built before sending */
    private boolean buildMessage = false;

    public void init(SynapseEnvironment synapseEnvironment) {
        if (!initialized) {
            super.init(synapseEnvironment);
            buildMessage = Boolean.parseBoolean(
                    SynapsePropertiesLoader.getPropertyValue(SynapseConstants.BUILD_MESSAGE_ON_FAILOVER, "false"));
        }
    }

    public void send(MessageContext synCtx) {
        if (RuntimeStatisticCollector.isStatisticsEnabled()) {
            java.lang.Integer currentIndex = null;
            boolean retry = (synCtx.getProperty(SynapseConstants.LAST_ENDPOINT) != null);
            if ((getDefinition() != null) && !retry) {
                currentIndex = OpenEventCollector.reportChildEntryEvent(synCtx, getReportingName(),
                        ComponentType.ENDPOINT, getDefinition().getAspectConfiguration(), true);
            }
            try {
                sendMessage(synCtx);
            } finally {
                if (currentIndex != null) {
                    CloseEventCollector.closeEntryEvent(synCtx, getReportingName(),
                            ComponentType.MEDIATOR, currentIndex, false);
                }
            }
        } else {
            sendMessage(synCtx);
        }
    }

    private void sendMessage(MessageContext synCtx) {

        logSetter();
        if (log.isDebugEnabled()) {
            log.debug("Failover Endpoint : " + getName());
        }

       if (getContext().isState(EndpointContext.ST_OFF)) {
            informFailure(synCtx, SynapseConstants.ENDPOINT_FO_NONE_READY,
                    "Failover endpoint : " + getName() != null ? getName() : SynapseConstants.ANONYMOUS_ENDPOINT + " - is inactive");
            return;
        }

        boolean isARetry = false;
        Map<String,Integer>mEndpointLog = null;
        if (synCtx.getProperty(SynapseConstants.LAST_ENDPOINT) == null) {
            if (log.isDebugEnabled()) {
                log.debug(this + " Building the SoapEnvelope");
            }
            //preserving the payload to send next endpoint if needed
            // If buildMessage attribute available in failover config it is honoured, else global property is considered
            if (isBuildMessageAttAvailable) {
                if (buildMessageAtt) {
                    buildMessage(synCtx);
                }
            } else if (buildMessage) {
                buildMessage(synCtx);
            }
            synCtx.getEnvelope().buildWithAttachments();
            //If the endpoint failed during the sending, we need to keep the original envelope and reuse that for other endpoints
            if (Boolean.TRUE.equals(((Axis2MessageContext) synCtx).getAxis2MessageContext().getProperty(
                    PassThroughConstants.MESSAGE_BUILDER_INVOKED))) {
                synCtx.setProperty(SynapseConstants.LB_FO_ENDPOINT_ORIGINAL_MESSAGE, synCtx.getEnvelope());
            }
            mEndpointLog = new HashMap<String,Integer>();
            synCtx.setProperty(SynapseConstants.ENDPOINT_LOG, mEndpointLog);
        } else {
            isARetry = true;
            mEndpointLog = (Map<String,Integer>)synCtx.getProperty(SynapseConstants.ENDPOINT_LOG);
        }

        if (getChildren().isEmpty()) {
            informFailure(synCtx, SynapseConstants.ENDPOINT_FO_NONE_READY,
                    "FailoverLoadbalance endpoint : " + getName() + " - no child endpoints");
            return;
        }

        // evaluate the endpoint properties
        evaluateProperties(synCtx);

        if (dynamic) {
            // Dynamic fail-over mode - Switch to a backup endpoint when an error occurs
            // in the primary endpoint. But switch back to the primary as soon as it becomes
            // active again.

            boolean foundEndpoint = false;
            for (Endpoint endpoint : getChildren()) {
                if (endpoint.readyToSend()) {
                    foundEndpoint = true;
                    if (isARetry && metricsMBean != null) {
                        metricsMBean.reportSendingFault(SynapseConstants.ENDPOINT_FO_FAIL_OVER);
                    }
                    synCtx.pushFaultHandler(this);
                    if(endpoint instanceof AbstractEndpoint){
                    	org.apache.axis2.context.MessageContext axisMC = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
                    	Pipe pipe = (Pipe) axisMC.getProperty(PassThroughConstants.PASS_THROUGH_PIPE);
                    	if(pipe != null){
                    		 pipe.forceSetSerializationRest();
                    	}
                    	//allow the message to be content aware if the given message comes via PT
						if (axisMC.getProperty(PassThroughConstants.PASS_THROUGH_PIPE) != null) {
							((AbstractEndpoint) endpoint).setContentAware(true);
							((AbstractEndpoint) endpoint).setForceBuildMC(true);

							if(endpoint instanceof TemplateEndpoint && ((TemplateEndpoint)endpoint).getRealEndpoint() != null){
								if(((TemplateEndpoint)endpoint).getRealEndpoint() instanceof AbstractEndpoint){
									((AbstractEndpoint)((TemplateEndpoint)endpoint).getRealEndpoint()).setContentAware(true);
									((AbstractEndpoint)((TemplateEndpoint)endpoint).getRealEndpoint()).setForceBuildMC(true);
								}
							}
						}
                    }
                    if(endpoint.getName() != null){
                    	mEndpointLog.put(endpoint.getName(), null);
                    }
                    endpoint.send(synCtx);
                    break;
                }
            }

            if (!foundEndpoint) {
                String msg = "Failover endpoint : " +
                        (getName() != null ? getName() : SynapseConstants.ANONYMOUS_ENDPOINT) +
                        " - no ready child endpoints";
                log.warn(msg);
                informFailure(synCtx, SynapseConstants.ENDPOINT_FO_NONE_READY, msg);
            }

        } else {
            // Static fail-over mode - Switch to a backup endpoint when an error occurs
            // in the primary endpoint. Keep sending messages to the backup endpoint until
            // an error occurs in that endpoint.

            if (currentEndpoint == null) {
                currentEndpoint = getChildren().get(0);
            }

            if (currentEndpoint.readyToSend()) {
                if (isARetry && metricsMBean != null) {
                    metricsMBean.reportSendingFault(SynapseConstants.ENDPOINT_FO_FAIL_OVER);
                }
                synCtx.pushFaultHandler(this);
                currentEndpoint.send(synCtx);

            } else {
                boolean foundEndpoint = false;
                for (Endpoint endpoint : getChildren()) {
                    if (endpoint.readyToSend()) {
                        foundEndpoint = true;
                        currentEndpoint = endpoint;
                        if (isARetry && metricsMBean != null) {
                            metricsMBean.reportSendingFault(SynapseConstants.ENDPOINT_FO_FAIL_OVER);
                        }
                        synCtx.pushFaultHandler(this);
                        currentEndpoint.send(synCtx);
                        break;
                    }
                }

                if (!foundEndpoint) {
                    String msg = "Failover endpoint : " +
                            (getName() != null ? getName() : SynapseConstants.ANONYMOUS_ENDPOINT) +
                            " - no ready child endpoints";
                    log.warn(msg);
                    informFailure(synCtx, SynapseConstants.ENDPOINT_FO_NONE_READY, msg);
                }
            }
        }
    }

    public void onChildEndpointFail(Endpoint endpoint, MessageContext synMessageContext) {
        //If there is a failure in child endpoint, restore the original message envelope from the message context
        if (synMessageContext.getProperty(SynapseConstants.LB_FO_ENDPOINT_ORIGINAL_MESSAGE) != null) {
            try {
                synMessageContext.setEnvelope(
                        (SOAPEnvelope) synMessageContext.getProperty(SynapseConstants.LB_FO_ENDPOINT_ORIGINAL_MESSAGE));
            } catch (AxisFault ex) {
                log.error("Couldn't restore the original message to the failover endpoint", ex);
            }
        }
        logOnChildEndpointFail(endpoint, synMessageContext);
        if (((AbstractEndpoint)endpoint).isRetry(synMessageContext)) {
            if (log.isDebugEnabled()) {
                log.debug(this + " Retry Attempt for Request with [Message ID : " +
                        synMessageContext.getMessageID() + "], [To : " +
                        synMessageContext.getTo() + "]");
            }
            send(synMessageContext);
        } else {
            String msg = "Failover endpoint : " +
                    (getName() != null ? getName() : SynapseConstants.ANONYMOUS_ENDPOINT) +
                    " - one of the child endpoints encounterd a non-retry error, " +
                    "not sending message to another endpoint";
            log.warn(msg);
            informFailure(synMessageContext, SynapseConstants.ENDPOINT_FO_NONE_READY, msg);
        }
    }

    public boolean readyToSend() {
        if (getContext().isState(EndpointContext.ST_OFF)) {
            return false;
        }

        for (Endpoint endpoint : getChildren()) {
            if (endpoint.readyToSend()) {
                currentEndpoint = endpoint;
                return true;
            }
        }
        return false;
    }

    protected void createJsonRepresentation() {
        endpointJson = new JSONObject();
        endpointJson.put(NAME_JSON_ATT, getName());
        endpointJson.put(TYPE_JSON_ATT, "Failover Endpoint");
        endpointJson.put(CHILDREN_JSON_ATT, getEndpointChildrenAsJson(getChildren()));
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public boolean isBuildMessageAtt() {
        return buildMessageAtt;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    /**
     * Set buildMessage Attribute from failover config
     * @param buildMessage true or false
     */
    public void setBuildMessageAtt(boolean buildMessage) {
        this.buildMessageAtt = buildMessage;
    }

    /**
     * Set whether failover config has the buildMessage config
     * @param available true or false
     */
    public void setBuildMessageAttAvailable(boolean available) {
        this.isBuildMessageAttAvailable = available;
    }

    /**
     * Build the message
     * @param synCtx Synapse Context
     */
    private void buildMessage(MessageContext synCtx) {
        try {
            RelayUtils.buildMessage(((Axis2MessageContext) synCtx).getAxis2MessageContext());
        } catch (IOException | XMLStreamException ex) {
            handleException("Error while building the message", ex);

        }
    }
}
