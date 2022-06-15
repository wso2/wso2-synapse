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

package org.apache.synapse.analytics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SequenceType;
import org.apache.synapse.ServerConfigurationInformation;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.analytics.elastic.ElasticsearchAnalyticsService;
import org.apache.synapse.analytics.schema.AnalyticsDataSchema;
import org.apache.synapse.analytics.schema.AnalyticsDataSchemaElement;
import org.apache.synapse.api.API;
import org.apache.synapse.commons.CorrelationConstants;
import org.apache.synapse.config.SynapsePropertiesLoader;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.endpoints.AbstractEndpoint;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.inbound.InboundEndpoint;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.rest.RESTConstants;
import org.apache.synapse.transport.netty.BridgeConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class AnalyticsPublisher {
    private static final Log log = LogFactory.getLog(AnalyticsPublisher.class);
    private static final Collection<AnalyticsService> registeredServices = new ArrayList<>();

    private static boolean analyticsDisabledForAPI;
    private static boolean analyticsDisabledForSequences;
    private static boolean analyticsDisabledForProxyServices;
    private static boolean analyticsDisabledForEndpoints;
    private static boolean analyticsDisabledForInboundEndpoints;
    private static boolean namedSequencesOnly;

    public static synchronized void init(ServerConfigurationInformation serverInfo) {
        AnalyticsDataSchema.updateServerMetadata(serverInfo);
        loadConfigurations();
        prepareAnalyticServices();
    }

    private static void loadConfigurations() {
        analyticsDisabledForAPI = !SynapsePropertiesLoader.getBooleanProperty(
                AnalyticsConstants.SynapseConfiguration.API_ANALYTICS_ENABLED, true);
        analyticsDisabledForSequences = !SynapsePropertiesLoader.getBooleanProperty(
                AnalyticsConstants.SynapseConfiguration.SEQUENCE_ANALYTICS_ENABLED, true);
        analyticsDisabledForProxyServices = !SynapsePropertiesLoader.getBooleanProperty(
                AnalyticsConstants.SynapseConfiguration.PROXY_SERVICE_ANALYTICS_ENABLED, true);
        analyticsDisabledForEndpoints = !SynapsePropertiesLoader.getBooleanProperty(
                AnalyticsConstants.SynapseConfiguration.ENDPOINT_ANALYTICS_ENABLED, true);
        analyticsDisabledForInboundEndpoints = !SynapsePropertiesLoader.getBooleanProperty(
                AnalyticsConstants.SynapseConfiguration.INBOUND_ENDPOINT_ANALYTICS_ENABLED, true);
        AnalyticsPublisher.setNamedSequencesOnly(SynapsePropertiesLoader.getBooleanProperty(
                AnalyticsConstants.SynapseConfiguration.NAMED_SEQUENCES_ONLY, false));
    }

    private static void prepareAnalyticServices() {
        registerService(ElasticsearchAnalyticsService.getInstance());
    }

    public static void registerService(AnalyticsService service) {
        if (!service.isEnabled()) {
            return;
        }
        log.info(String.format("Registering analytics service %s", service.getClass().getSimpleName()));
        registeredServices.add(service);
    }

    public static void deregisterService(AnalyticsService service) {
        if (registeredServices.contains(service)) {
            log.info(String.format("Deregistering analytics service %s", service.getClass().getSimpleName()));
            registeredServices.remove(service);
        } else {
            log.warn(String.format("Failed to Deregister analytics service %s. Reason: Not found",
                    service.getClass().getSimpleName()));
        }
    }

    public static void publishAnalytic(AnalyticsDataSchemaElement payload) {
        AnalyticsDataSchema analyticsDataSchemaInst = new AnalyticsDataSchema(payload);

        registeredServices.forEach(service -> {
            if (service.isEnabled()) {
                service.publish(analyticsDataSchemaInst);
            }
        });
    }

    public static void publishApiAnalytics(MessageContext synCtx) {
        if (analyticsDisabledForAPI) {
            return;
        }

        if (!(synCtx instanceof Axis2MessageContext)) {
            if (log.isDebugEnabled()) {
                log.debug("Ignoring non-Axis2MessageContext message for ApiAnalytics");
            }
            return;
        }

        AnalyticsDataSchemaElement analyticPayload = generateAnalyticsObject(synCtx, API.class);

        AnalyticsDataSchemaElement apiDetails = new AnalyticsDataSchemaElement();
        apiDetails.setAttribute(AnalyticsConstants.EnvelopDef.API,
                synCtx.getProperty(RESTConstants.SYNAPSE_REST_API));
        apiDetails.setAttribute(AnalyticsConstants.EnvelopDef.SUB_REQUEST_PATH,
                synCtx.getProperty(RESTConstants.REST_SUB_REQUEST_PATH));
        apiDetails.setAttribute(AnalyticsConstants.EnvelopDef.API_CONTEXT,
                synCtx.getProperty(RESTConstants.REST_API_CONTEXT));
        apiDetails.setAttribute(AnalyticsConstants.EnvelopDef.METHOD,
                synCtx.getProperty(RESTConstants.REST_METHOD));
        apiDetails.setAttribute(AnalyticsConstants.EnvelopDef.TRANSPORT,
                synCtx.getProperty(SynapseConstants.TRANSPORT_IN_NAME));
        analyticPayload.setAttribute(AnalyticsConstants.EnvelopDef.API_DETAILS, apiDetails);
        attachHttpProperties(analyticPayload, synCtx);

        publishAnalytic(analyticPayload);
    }

    public static void publishSequenceMediatorAnalytics(MessageContext synCtx, SequenceMediator sequence) {
        if (analyticsDisabledForSequences) {
            return;
        }

        if (!(synCtx instanceof Axis2MessageContext)) {
            if (log.isDebugEnabled()) {
                log.debug("Ignoring non-Axis2MessageContext message for SequenceMediatorAnalytics");
            }
            return;
        }

        if (isNamedSequencesOnly() && !SequenceType.NAMED.equals(sequence.getSequenceType())) {
            return;
        }

        AnalyticsDataSchemaElement analyticsPayload = generateAnalyticsObject(synCtx, SequenceMediator.class);

        AnalyticsDataSchemaElement sequenceDetails = new AnalyticsDataSchemaElement();
        sequenceDetails.setAttribute(AnalyticsConstants.EnvelopDef.SEQUENCE_TYPE, sequence.getSequenceType().toString());
        if (sequence.getSequenceType() == SequenceType.NAMED) {
            sequenceDetails.setAttribute(AnalyticsConstants.EnvelopDef.SEQUENCE_NAME, sequence.getName());
        } else {
            sequenceDetails.setAttribute(
                    AnalyticsConstants.EnvelopDef.SEQUENCE_NAME, sequence.getSequenceNameForStatistics());
            switch (sequence.getSequenceType()) {
                case API_INSEQ:
                case API_OUTSEQ:
                case API_FAULTSEQ:
                    sequenceDetails.setAttribute(AnalyticsConstants.EnvelopDef.SEQUENCE_API_CONTEXT,
                            synCtx.getProperty(RESTConstants.REST_API_CONTEXT));
                    sequenceDetails.setAttribute(AnalyticsConstants.EnvelopDef.SEQUENCE_API,
                            synCtx.getProperty(RESTConstants.SYNAPSE_REST_API));
                    sequenceDetails.setAttribute(AnalyticsConstants.EnvelopDef.SEQUENCE_API_SUB_REQUEST_PATH,
                            synCtx.getProperty(RESTConstants.REST_SUB_REQUEST_PATH));
                    sequenceDetails.setAttribute(AnalyticsConstants.EnvelopDef.SEQUENCE_API_METHOD,
                            synCtx.getProperty(RESTConstants.REST_METHOD));
                    break;
                case PROXY_INSEQ:
                case PROXY_OUTSEQ:
                case PROXY_FAULTSEQ:
                    sequenceDetails.setAttribute(AnalyticsConstants.EnvelopDef.SEQUENCE_PROXY_NAME,
                            synCtx.getProperty(SynapseConstants.PROXY_SERVICE));
                    break;
                case ANON:
                    break;
            }
        }

        analyticsPayload.setAttribute(AnalyticsConstants.EnvelopDef.SEQUENCE_DETAILS, sequenceDetails);
        publishAnalytic(analyticsPayload);
    }

    public static void publishProxyServiceAnalytics(MessageContext synCtx, ProxyService proxyServiceDef) {
        if (analyticsDisabledForProxyServices) {
            return;
        }

        if (!(synCtx instanceof Axis2MessageContext)) {
            if (log.isDebugEnabled()) {
                log.debug("Ignoring non-Axis2MessageContext message for ProxyServiceAnalytics");
            }
            return;
        }

        AnalyticsDataSchemaElement analyticsPayload = generateAnalyticsObject(synCtx, ProxyService.class);

        analyticsPayload.setAttribute(AnalyticsConstants.EnvelopDef.PROXY_SERVICE_TRANSPORT,
                synCtx.getProperty(SynapseConstants.TRANSPORT_IN_NAME));
        analyticsPayload.setAttribute(AnalyticsConstants.EnvelopDef.PROXY_SERVICE_IS_DOING_REST,
                synCtx.getProperty(SynapseConstants.IS_CLIENT_DOING_REST));
        analyticsPayload.setAttribute(AnalyticsConstants.EnvelopDef.PROXY_SERVICE_IS_DOING_SOAP11,
                synCtx.getProperty(SynapseConstants.IS_CLIENT_DOING_SOAP11));

        AnalyticsDataSchemaElement proxyServiceDetails = new AnalyticsDataSchemaElement();
        proxyServiceDetails.setAttribute(AnalyticsConstants.EnvelopDef.PROXY_SERVICE_NAME, proxyServiceDef.getName());
        analyticsPayload.setAttribute(AnalyticsConstants.EnvelopDef.PROXY_SERVICE_DETAILS, proxyServiceDetails);
        attachHttpProperties(analyticsPayload, synCtx);

        publishAnalytic(analyticsPayload);
    }

    public static void publishEndpointAnalytics(MessageContext synCtx, EndpointDefinition endpointDef) {
        if (analyticsDisabledForEndpoints) {
            return;
        }

        if (!(synCtx instanceof Axis2MessageContext)) {
            if (log.isDebugEnabled()) {
                log.debug("Ignoring non-Axis2MessageContext message for EndpointAnalytics");
            }
            return;
        }

        AnalyticsDataSchemaElement analyticsPayload = generateAnalyticsObject(synCtx, Endpoint.class);

        AnalyticsDataSchemaElement endpointDetails = new AnalyticsDataSchemaElement();
        String endpointName;
        boolean isAnonymous = false;
        if ((endpointDef.leafEndpoint instanceof AbstractEndpoint) &&
                ((AbstractEndpoint) endpointDef.leafEndpoint).isAnonymous()) {
            endpointName = SynapseConstants.ANONYMOUS_ENDPOINT;
            isAnonymous = true;
        } else {
            endpointName = endpointDef.leafEndpoint.getName();
        }
        endpointDetails.setAttribute(AnalyticsConstants.EnvelopDef.ENDPOINT_NAME, endpointName);
        endpointDetails.setAttribute(AnalyticsConstants.EnvelopDef.ENDPOINT_IS_ANONYMOUS, isAnonymous);
        analyticsPayload.setAttribute(AnalyticsConstants.EnvelopDef.ENDPOINT_DETAILS, endpointDetails);

        publishAnalytic(analyticsPayload);
    }

    public static void publishInboundEndpointAnalytics(MessageContext synCtx, InboundEndpoint endpointDef) {
        if (analyticsDisabledForInboundEndpoints) {
            return;
        }

        if (endpointDef == null) {
            if (log.isDebugEnabled()) {
                log.debug("Ignoring analytics for unknown InboundEndpoint");
            }
            return;
        }

        if (!(synCtx instanceof Axis2MessageContext)) {
            if (log.isDebugEnabled()) {
                log.debug("Ignoring non-Axis2MessageContext message for InboundEndpointAnalytics");
            }
            return;
        }

        AnalyticsDataSchemaElement analyticsPayload = generateAnalyticsObject(synCtx, InboundEndpoint.class);

        AnalyticsDataSchemaElement inboundEndpointDetails = new AnalyticsDataSchemaElement();
        inboundEndpointDetails.setAttribute(
                AnalyticsConstants.EnvelopDef.INBOUND_ENDPOINT_NAME, endpointDef.getName());
        inboundEndpointDetails.setAttribute(
                AnalyticsConstants.EnvelopDef.INBOUND_ENDPOINT_PROTOCOL, endpointDef.getProtocol());
        analyticsPayload.setAttribute(
                AnalyticsConstants.EnvelopDef.INBOUND_ENDPOINT_DETAILS, inboundEndpointDetails);
        attachHttpProperties(analyticsPayload, synCtx);

        publishAnalytic(analyticsPayload);
    }

    private static AnalyticsDataSchemaElement generateAnalyticsObject(MessageContext synCtx, Class<?> entityClass) {
        AnalyticsDataSchemaElement analyticPayload = new AnalyticsDataSchemaElement();
        analyticPayload.setAttribute(AnalyticsConstants.EnvelopDef.ENTITY_TYPE, entityClass.getSimpleName());
        analyticPayload.setAttribute(AnalyticsConstants.EnvelopDef.ENTITY_CLASS_NAME, entityClass.getName());
        analyticPayload.setAttribute(AnalyticsConstants.EnvelopDef.FAULT_RESPONSE, synCtx.isFaultResponse());
        analyticPayload.setAttribute(AnalyticsConstants.EnvelopDef.MESSAGE_ID, synCtx.getMessageID());
        analyticPayload.setAttribute(AnalyticsConstants.EnvelopDef.CORRELATION_ID,
                synCtx.getProperty(CorrelationConstants.CORRELATION_ID));
        analyticPayload.setAttribute(AnalyticsConstants.EnvelopDef.LATENCY, synCtx.getLatency());

        AnalyticsDataSchemaElement metadata = new AnalyticsDataSchemaElement();
        Axis2MessageContext axis2mc = (Axis2MessageContext) synCtx;
        for (Map.Entry<String, Object> entry : axis2mc.getAnalyticsMetadata().entrySet()) {
            if (entry.getValue() == null) {
                continue; // Logstash fails at null
            }
            metadata.setAttribute(entry.getKey(), entry.getValue());
        }

        analyticPayload.setAttribute(AnalyticsConstants.EnvelopDef.METADATA, metadata);
        return analyticPayload;
    }

    private static void attachHttpProperties(AnalyticsDataSchemaElement payload, MessageContext synCtx) {

        org.apache.axis2.context.MessageContext axisCtx = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        if (axisCtx == null) {
            return;
        }

        payload.setAttribute(AnalyticsConstants.EnvelopDef.REMOTE_HOST,
                axisCtx.getProperty(BridgeConstants.REMOTE_HOST));
        payload.setAttribute(AnalyticsConstants.EnvelopDef.CONTENT_TYPE,
                axisCtx.getProperty(BridgeConstants.CONTENT_TYPE_HEADER));
        payload.setAttribute(AnalyticsConstants.EnvelopDef.HTTP_METHOD,
                axisCtx.getProperty(BridgeConstants.HTTP_METHOD));
    }

    public static boolean isNamedSequencesOnly() {
        return namedSequencesOnly;
    }

    public static void setNamedSequencesOnly(boolean namedSequencesOnly) {
        AnalyticsPublisher.namedSequencesOnly = namedSequencesOnly;
    }
}
