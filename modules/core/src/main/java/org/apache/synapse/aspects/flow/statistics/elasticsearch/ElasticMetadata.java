/*
 * Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.aspects.flow.statistics.elasticsearch;

import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.commons.CorrelationConstants;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.mediators.base.SequenceMediator;

import java.util.HashMap;
import java.util.Map;
import org.apache.synapse.rest.RESTConstants;
import org.apache.synapse.transport.netty.BridgeConstants;

public class ElasticMetadata {
    private final SynapseConfiguration synapseConfiguration;
    private final boolean faultResponse;
    private final String messageId;
    private final Map<String, Object> contextEntries;
    private final Map<String, Object> contextProperties;

    public ElasticMetadata(MessageContext msgCtx) {
        this.synapseConfiguration = msgCtx.getConfiguration();
        this.faultResponse = msgCtx.isFaultResponse();
        this.messageId = msgCtx.getMessageID();
        this.contextEntries = msgCtx.getContextEntries();
        this.contextProperties = populateContextProperties((Axis2MessageContext) msgCtx);
    }

    public ElasticMetadata(SynapseConfiguration synapseConfiguration, boolean faultResponse, String messageId,
                           Map<String, Object> contextEntries, Map<String, Object> contextProperties) {
        this.synapseConfiguration = synapseConfiguration;
        this.faultResponse = faultResponse;
        this.messageId = messageId;
        this.contextEntries = contextEntries;
        this.contextProperties = contextProperties;
    }

    public SynapseConfiguration getSynapseConfiguration() {
        return synapseConfiguration;
    }

    public boolean isFaultResponse() {
        return faultResponse;
    }

    public String getMessageId() {
        return messageId;
    }

    public Map<String, Object> getContextEntries() {
        return contextEntries;
    }

    public SequenceMediator getSequence(String key) {
        Object o = getContextEntries().get(key);
        if (o instanceof SequenceMediator) {
            return (SequenceMediator) o;
        }

        Mediator mediator = getSynapseConfiguration().getSequence(key);
        if (mediator instanceof SequenceMediator) {
            return (SequenceMediator) mediator;
        }
        return null;
    }

    public Endpoint getEndpoint(String key) {
        Object o = getContextEntries().get(key);
        if (o instanceof Endpoint) {
            return (Endpoint) o;
        }

        return getSynapseConfiguration().getEndpoint(key);
    }

    public Object getProperty(String key) {
        return this.contextProperties.get(key);
    }

    public HashMap<String, Object> getAnalyticsMetadata() {
        //noinspection unchecked
        return (HashMap<String, Object>) getProperty(SynapseConstants.ANALYTICS_METADATA);
    }

    public boolean isValid() {
        return this.contextProperties != null &&
                this.contextEntries != null &&
                this.synapseConfiguration != null;
    }

    /**
     * Populating context-properties manually to de-reference the axis2 message context from
     * ElasticMetadata.
     *
     * @param ctx Axis2MessageContext
     * @return map with a copy of only required properties.
     */
    private Map<String, Object> populateContextProperties(Axis2MessageContext ctx) {
        Map<String, Object> props = new HashMap<>();
        String[] propNames = {RESTConstants.SYNAPSE_REST_API, RESTConstants.REST_SUB_REQUEST_PATH,
            RESTConstants.REST_API_CONTEXT, RESTConstants.REST_METHOD, BridgeConstants.HTTP_METHOD,
            SynapseConstants.TRANSPORT_IN_NAME, CorrelationConstants.CORRELATION_ID,
            SynapseConstants.IS_CLIENT_DOING_REST, SynapseConstants.IS_CLIENT_DOING_SOAP11,
            BridgeConstants.REMOTE_HOST, BridgeConstants.CONTENT_TYPE_HEADER,
            SynapseConstants.ANALYTICS_METADATA};
        for (String propName : propNames) {
            props.put(propName, ctx.getProperty(propName));
        }
        return props;
    }
}
