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

import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.Pipe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FailoverEndpointOnHttpStatusCode can have multiple child endpoints. When http status code
 * support for a particular failover group, after recieving response if response error http status code
 * and one of the failover group configured http status code is matching the original request will send to the
 * next active endpoint. If next endpoint is not active then the request will be sent to a next available
 * endpoint if there is any. This procedure repeats until there are no active endpoints.
 */
public class FailoverEndpointOnHttpStatusCode extends FailoverEndpoint {

    public void send(MessageContext synCtx) {


        List<Endpoint> endpointList = null;

        /**Get endpoint list from MC*/
        if (synCtx.getProperty(SynapseConstants.ENDPOINT_LIST) != null) {
            endpointList = (List<Endpoint>) synCtx.getProperty(SynapseConstants.ENDPOINT_LIST);
        }

        Map<String, Integer> mEndpointLog = null;
        if (synCtx.getProperty(SynapseConstants.LAST_ENDPOINT) == null) {

            if (log.isDebugEnabled()) {
                log.debug(this + " Building the SoapEnvelope");
            }
            // If not yet a retry, we have to build the envelope since we need to support failover
            synCtx.getEnvelope().build();
            mEndpointLog = new HashMap<String, Integer>();
            synCtx.setProperty(SynapseConstants.ENDPOINT_LOG, mEndpointLog);
        } else {

            mEndpointLog = (Map<String, Integer>) synCtx.getProperty(SynapseConstants.ENDPOINT_LOG);
        }
        evaluateProperties(synCtx);


        int i = (Integer) synCtx.getProperty(SynapseConstants.CURRENT_ENDPOINT_INDEX) + 1;
        while (i < endpointList.size()) {

            /**Get next endpoint in the list*/
            Endpoint nextEndpoint = endpointList.get(i);

            if (nextEndpoint.readyToSend()) {

                if (metricsMBean != null) {
                    metricsMBean.reportSendingFault(SynapseConstants.ENDPOINT_FO_FAIL_OVER);
                }

                /**Register a fault handler, called when error occured at the endpoint*/
                FailoverFaultHandler failoverFaultHandler = new FailoverFaultHandler();
                synCtx.pushFaultHandler(failoverFaultHandler);

                if (nextEndpoint instanceof AbstractEndpoint) {
                    org.apache.axis2.context.MessageContext axisMC = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
                    axisMC.getEnvelope().buildWithAttachments();
                    Pipe pipe = (Pipe) axisMC.getProperty(PassThroughConstants.PASS_THROUGH_PIPE);

                    if (pipe != null) {
                        pipe.forceSetSerializationRest();
                    }
                    //allow the message to be content aware if the given message comes via PT
                    if (axisMC.getProperty(PassThroughConstants.PASS_THROUGH_PIPE) != null) {
                        ((AbstractEndpoint) nextEndpoint).setContentAware(true);
                        ((AbstractEndpoint) nextEndpoint).setForceBuildMC(true);

                        if (nextEndpoint instanceof TemplateEndpoint && ((TemplateEndpoint) nextEndpoint).getRealEndpoint() != null) {
                            if (((TemplateEndpoint) nextEndpoint).getRealEndpoint() instanceof AbstractEndpoint) {
                                ((AbstractEndpoint) ((TemplateEndpoint) nextEndpoint).getRealEndpoint()).setContentAware(true);
                                ((AbstractEndpoint) ((TemplateEndpoint) nextEndpoint).getRealEndpoint()).setForceBuildMC(true);
                            }
                        }
                    }
                }

                if (nextEndpoint.getName() != null) {
                    mEndpointLog.put(nextEndpoint.getName(), null);
                }

                if (log.isDebugEnabled()) {
                    log.debug(this + " Send message to the next endpoint");
                }

                /**Avoid cloining the message context further*/
                synCtx.setProperty(SynapseConstants.CLONE_THIS_MSG, false);

                /**Set the position of current endpoint*/
                synCtx.setProperty(SynapseConstants.CURRENT_ENDPOINT_INDEX, i);

                /**List of endpoints*/
                synCtx.setProperty(SynapseConstants.ENDPOINT_LIST, endpointList);

                nextEndpoint.send(synCtx);
                break;
            }
            i++;
        }
    }
}
