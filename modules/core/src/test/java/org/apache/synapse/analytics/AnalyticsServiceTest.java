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

import com.damnhandy.uri.template.UriTemplate;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SequenceType;
import org.apache.synapse.ServerConfigurationInformation;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.analytics.schema.AnalyticsDataSchema;
import org.apache.synapse.analytics.schema.AnalyticsDataSchemaElement;
import org.apache.synapse.api.API;
import org.apache.synapse.api.Resource;
import org.apache.synapse.api.rest.RestRequestHandler;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.SequenceMediatorFactory;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.config.xml.endpoints.HTTPEndpointFactory;
import org.apache.synapse.core.axis2.*;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.endpoints.HTTPEndpoint;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.builtin.PropertyMediator;
import org.apache.synapse.transport.netty.BridgeConstants;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import javax.xml.stream.XMLStreamException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Properties;

public class AnalyticsServiceTest extends TestCase {

    private static final String SERVER_INFO_SERVER_NAME = "wso2.dev";
    private static final String SERVER_INFO_HOST_NAME = "dev.local";
    private static final String SERVER_INFO_IP_ADDRESS = "1.2.3.4";
    private static final String SERVER_INFO_PUBLISHER_ID = "WSO2_UNIT_TEST";

    private static final String TEST_API_NAME = "TestAPI";
    private static final String TEST_API_CONTEXT = "/test";
    private static final String TEST_API_URL = "/test/admin?search=wso2";
    private static final String TEST_API_METHOD = "GET";
    private static final String TEST_API_PROTOCOL = "https";
    private static final String TEST_SEQUENCE_NAME = "TestSequenceName";
    private static final String TEST_ENDPOINT_NAME = "TestEndpointName";
    private static final String TEST_REMOTE_HOST = "127.0.0.1";
    private static final String TEST_CONTENT_TYPE = "application/json";

    private static final int CURRENT_SCHEMA_VERSION = 1;
    private final SimpleAnalyticsService service = new SimpleAnalyticsService();
    private boolean oneTimeSetupComplete = false;
    private Axis2MessageContext messageContext = null;
    private Axis2SynapseEnvironment synapseEnvironment = null;

    private void oneTimeSetup() throws Exception {
        if (oneTimeSetupComplete) {
            return;
        }

        ServerConfigurationInformation sysConfig = new ServerConfigurationInformation();
        sysConfig.setServerName(SERVER_INFO_SERVER_NAME);
        sysConfig.setHostName(SERVER_INFO_HOST_NAME);
        sysConfig.setIpAddress(SERVER_INFO_IP_ADDRESS);
        AnalyticsPublisher.init(sysConfig);
        AnalyticsDataSchema.setPublisherId(SERVER_INFO_PUBLISHER_ID);
        synapseEnvironment = PowerMockito.mock(Axis2SynapseEnvironment.class);
        ConfigurationContext axis2ConfigurationContext = new ConfigurationContext(new AxisConfiguration());
        axis2ConfigurationContext.getAxisConfiguration().addParameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment);
        Mockito.when(synapseEnvironment.getAxis2ConfigurationContext()).thenReturn(axis2ConfigurationContext);

        messageContext = (Axis2MessageContext) getMessageContext();

        oneTimeSetupComplete = true;
    }

    protected MessageContext getMessageContext() throws Exception {
        API api = new API(TEST_API_NAME, TEST_API_CONTEXT);
        String url = TEST_API_URL;
        Resource resource = new Resource();
        api.addResource(resource);

        SynapseConfiguration synapseConfig = new SynapseConfiguration();
        synapseConfig.addAPI(api.getName(), api);

        MessageContext synCtx = TestUtils.createSynapseMessageContext("<foo/>", synapseConfig);
        org.apache.axis2.context.MessageContext msgCtx = ((Axis2MessageContext) synCtx).
                getAxis2MessageContext();
        msgCtx.setIncomingTransportName(TEST_API_PROTOCOL);
        msgCtx.setProperty(Constants.Configuration.HTTP_METHOD, TEST_API_METHOD);
        msgCtx.setProperty(Constants.Configuration.TRANSPORT_IN_URL, url);
        msgCtx.setProperty(BridgeConstants.REMOTE_HOST, TEST_REMOTE_HOST);
        msgCtx.setProperty(BridgeConstants.CONTENT_TYPE_HEADER, TEST_CONTENT_TYPE);
        msgCtx.setProperty(NhttpConstants.REST_URL_POSTFIX, url.substring(1));
        msgCtx.setConfigurationContext(new ConfigurationContext(new AxisConfiguration()));
        return synCtx;
    }

    @Override
    protected void setUp() throws Exception {
        oneTimeSetup();
        service.enableService();
        AnalyticsPublisher.registerService(service);
        AnalyticsPublisher.setNamedSequencesOnly(false);
    }

    @Override
    protected void tearDown() throws Exception {
        AnalyticsPublisher.deregisterService(service);
        service.clear();
    }

    public void testServiceEnabledState() {
        AnalyticsDataSchemaElement analyticData = new AnalyticsDataSchemaElement();
        analyticData.setAttribute("testProperty", "HelloWorld");

        service.enableService();
        AnalyticsPublisher.publishAnalytic(analyticData);
        assertTrue(service.isEnabled());
        assertEquals(1, service.getAvailableAnalyticsCount());
        service.clear();

        service.disableService();
        AnalyticsPublisher.publishAnalytic(analyticData);
        assertFalse(service.isEnabled());
        assertEquals(0, service.getAvailableAnalyticsCount());
    }

    public void testBasicAnalyticsSchema() {
        AnalyticsDataSchemaElement analyticData = new AnalyticsDataSchemaElement();
        analyticData.setAttribute("testProperty", "HelloWorld");

        AnalyticsPublisher.publishAnalytic(analyticData);
        assertTrue(service.isEnabled());
        assertEquals(1, service.getAvailableAnalyticsCount());
        verifySchema(service.fetchAnalytic(), AnalyticPayloadType.NON_STANDARD);
    }

    public void testSequenceAnalytics() {
        SequenceMediator seq = new SequenceMediator();
        seq.setName(TEST_SEQUENCE_NAME);

        seq.mediate(messageContext);
        assertEquals(1, service.getAvailableAnalyticsCount());
        verifySchema(service.fetchAnalytic(), AnalyticPayloadType.SEQUENCE);

        seq.setSequenceType(SequenceType.PROXY_INSEQ);
        AnalyticsPublisher.setNamedSequencesOnly(true);
        seq.mediate(messageContext);
        assertEquals(0, service.getAvailableAnalyticsCount());
    }

    public void testApiResourceAnalytics() {
        RestRequestHandler handler = new RestRequestHandler();
        handler.process(messageContext);
        assertEquals(1, service.getAvailableAnalyticsCount());
        verifySchema(service.fetchAnalytic(), AnalyticPayloadType.API);
    }

    public void testEndpointAnalytics() throws XMLStreamException {
        HTTPEndpointFactory factory = new HTTPEndpointFactory();
        OMElement em = AXIOMUtil.stringToOM(
                "<endpoint><http method=\"GET\" uri-template=\"https://wso2.com\"/></endpoint>");
        EndpointDefinition ep1 = factory.createEndpointDefinition(em);
        HTTPEndpoint httpEndpoint = new HTTPEndpoint();
        httpEndpoint.setName(TEST_ENDPOINT_NAME);
        httpEndpoint.setHttpMethod(TEST_API_METHOD);
        httpEndpoint.setDefinition(ep1);
        httpEndpoint.setUriTemplate(UriTemplate.fromTemplate("https://wso2.com"));
        httpEndpoint.init(synapseEnvironment);
        messageContext.setEnvironment(synapseEnvironment);
        httpEndpoint.send(messageContext);
        assertEquals(1, service.getAvailableAnalyticsCount());
        verifySchema(service.fetchAnalytic(), AnalyticPayloadType.ENDPOINT);
    }

    public void testProxyServiceAnalytics() throws XMLStreamException, AxisFault {
        ProxyServiceMessageReceiver proxyServiceMessageReceiver = new ProxyServiceMessageReceiver();
        ProxyService proxyService = new ProxyService("TestProxy");
        OMElement sequenceAsOM = AXIOMUtil.stringToOM("<inSequence xmlns=\"http://ws.apache.org/ns/synapse\">\n"
                + "      </inSequence>");
        proxyService.setTargetInLineInSequence(new SequenceMediatorFactory().
                createAnonymousSequence(sequenceAsOM, new Properties()));

        proxyServiceMessageReceiver.setProxy(proxyService);

        MessageContextCreatorForAxis2.setSynConfig(new SynapseConfiguration());
        MessageContextCreatorForAxis2.setSynEnv(synapseEnvironment);
        messageContext.setEnvironment(synapseEnvironment);

        proxyServiceMessageReceiver.receive(messageContext.getAxis2MessageContext());
        assertEquals(2, service.getAvailableAnalyticsCount());
        verifySchema(service.fetchAnalytic(), AnalyticPayloadType.SEQUENCE);
        verifySchema(service.fetchAnalytic(), AnalyticPayloadType.PROXY_SERVICE);
    }

    public void testAnalyticsMetadata() {
        SequenceMediator seq = new SequenceMediator();
        seq.setName(TEST_SEQUENCE_NAME);

        PropertyMediator analyticBoolean = new PropertyMediator();
        analyticBoolean.setName("analyticBoolean");
        analyticBoolean.setAction(PropertyMediator.ACTION_SET);
        analyticBoolean.setScope(XMLConfigConstants.SCOPE_ANALYTICS);
        analyticBoolean.setValue(String.valueOf(true));

        PropertyMediator analyticDouble = new PropertyMediator();
        analyticDouble.setName("analyticDouble");
        analyticDouble.setAction(PropertyMediator.ACTION_SET);
        analyticDouble.setScope(XMLConfigConstants.SCOPE_ANALYTICS);
        analyticDouble.setValue(String.valueOf(Double.MAX_VALUE));

        PropertyMediator analyticFloat = new PropertyMediator();
        analyticFloat.setName("analyticFloat");
        analyticFloat.setAction(PropertyMediator.ACTION_SET);
        analyticFloat.setScope(XMLConfigConstants.SCOPE_ANALYTICS);
        analyticFloat.setValue(String.valueOf(Float.MAX_VALUE));

        PropertyMediator analyticInteger = new PropertyMediator();
        analyticInteger.setName("analyticInteger");
        analyticInteger.setAction(PropertyMediator.ACTION_SET);
        analyticInteger.setScope(XMLConfigConstants.SCOPE_ANALYTICS);
        analyticInteger.setValue(String.valueOf(Integer.MAX_VALUE));

        PropertyMediator analyticLong = new PropertyMediator();
        analyticLong.setName("analyticLong");
        analyticLong.setAction(PropertyMediator.ACTION_SET);
        analyticLong.setScope(XMLConfigConstants.SCOPE_ANALYTICS);
        analyticLong.setValue(String.valueOf(Long.MAX_VALUE));

        PropertyMediator analyticShort = new PropertyMediator();
        analyticShort.setName("analyticShort");
        analyticShort.setAction(PropertyMediator.ACTION_SET);
        analyticShort.setScope(XMLConfigConstants.SCOPE_ANALYTICS);
        analyticShort.setValue(String.valueOf(Short.MAX_VALUE));

        PropertyMediator analyticString = new PropertyMediator();
        analyticString.setName("analyticString");
        analyticString.setAction(PropertyMediator.ACTION_SET);
        analyticString.setScope(XMLConfigConstants.SCOPE_ANALYTICS);
        analyticString.setValue("WSO2");

        seq.addChild(analyticBoolean);
        seq.addChild(analyticDouble);
        seq.addChild(analyticFloat);
        seq.addChild(analyticInteger);
        seq.addChild(analyticLong);
        seq.addChild(analyticShort);
        seq.addChild(analyticString);

        seq.mediate(messageContext);
        assertEquals(1, service.getAvailableAnalyticsCount());
        JsonObject publishedAnalytic = service.fetchAnalytic();
        verifySchema(publishedAnalytic, AnalyticPayloadType.SEQUENCE);

        JsonObject metadata = publishedAnalytic.getAsJsonObject(AnalyticsConstants.EnvelopDef.PAYLOAD)
                .getAsJsonObject(AnalyticsConstants.EnvelopDef.METADATA);

        assertTrue(metadata.has("analyticBoolean"));
        assertTrue(metadata.get("analyticBoolean").getAsBoolean());

        assertTrue(metadata.has("analyticDouble"));
        assertEquals(Double.MAX_VALUE, metadata.get("analyticDouble").getAsDouble());

        assertTrue(metadata.has("analyticFloat"));
        assertEquals(Float.MAX_VALUE, metadata.get("analyticFloat").getAsFloat());

        assertTrue(metadata.has("analyticInteger"));
        assertEquals(Integer.MAX_VALUE, metadata.get("analyticInteger").getAsInt());

        assertTrue(metadata.has("analyticLong"));
        assertEquals(Long.MAX_VALUE, metadata.get("analyticLong").getAsLong());

        assertTrue(metadata.has("analyticShort"));
        assertEquals(Short.MAX_VALUE, metadata.get("analyticShort").getAsShort());

        assertTrue(metadata.has("analyticString"));
        assertEquals("WSO2", metadata.get("analyticString").getAsString());
    }

    private void verifySchema(JsonObject analytic, @NotNull AnalyticPayloadType payloadType) {
        assertNotNull(analytic);
        verifySchemaVersion(analytic.get(AnalyticsConstants.EnvelopDef.SCHEMA_VERSION));
        verifyServerInfo(analytic.get(AnalyticsConstants.EnvelopDef.SERVER_INFO));
        verifyTimestamp(analytic.get(AnalyticsConstants.EnvelopDef.TIMESTAMP));

        JsonElement payloadElement = analytic.get(AnalyticsConstants.EnvelopDef.PAYLOAD);
        switch (payloadType) {
            case PROXY_SERVICE:
                verifyProxyServicePayload(payloadElement);
                break;
            case ENDPOINT:
                verifyEndpointPayload(payloadElement);
                break;
            case API:
                verifyApiResourcePayload(payloadElement);
                break;
            case SEQUENCE:
                verifySequencePayload(payloadElement);
                break;
            default:
                assertTrue(payloadElement.isJsonObject());
        }
    }

    private void verifyServerInfo(JsonElement serverInfoElement) {
        assertNotNull(serverInfoElement);
        assertTrue(serverInfoElement.isJsonObject());

        JsonObject dataObject = serverInfoElement.getAsJsonObject();
        assertTrue(dataObject.has(AnalyticsConstants.ServerMetadataFieldDef.HOST_NAME));
        assertEquals(SERVER_INFO_HOST_NAME,
                dataObject.get(AnalyticsConstants.ServerMetadataFieldDef.HOST_NAME).getAsString());
        assertTrue(dataObject.has(AnalyticsConstants.ServerMetadataFieldDef.SERVER_NAME));
        assertEquals(SERVER_INFO_SERVER_NAME,
                dataObject.get(AnalyticsConstants.ServerMetadataFieldDef.SERVER_NAME).getAsString());
        assertTrue(dataObject.has(AnalyticsConstants.ServerMetadataFieldDef.IP_ADDRESS));
        assertEquals(SERVER_INFO_IP_ADDRESS,
                dataObject.get(AnalyticsConstants.ServerMetadataFieldDef.IP_ADDRESS).getAsString());
        assertTrue(dataObject.has(AnalyticsConstants.ServerMetadataFieldDef.PUBLISHER_ID));
        assertEquals(SERVER_INFO_PUBLISHER_ID,
                dataObject.get(AnalyticsConstants.ServerMetadataFieldDef.PUBLISHER_ID).getAsString());
    }

    private void verifyTimestamp(JsonElement timestampElement) {
        assertNotNull(timestampElement);

        try {
            Instant.parse(timestampElement.getAsString());
        } catch (DateTimeParseException e) {
            fail("timestamp should be in ISO8601 format. Found: " + timestampElement.getAsString());
        }
    }

    private void verifySchemaVersion(JsonElement schemaVersionElement) {
        assertNotNull(schemaVersionElement);
        assertEquals(CURRENT_SCHEMA_VERSION, schemaVersionElement.getAsInt());
    }

    private void verifySequencePayload(JsonElement payloadElement) {
        assertNotNull(payloadElement);
        assertTrue(payloadElement.isJsonObject());

        JsonObject payload = payloadElement.getAsJsonObject();
        verifyCommonPayloadFields(payload);
        assertTrue(payload.has(AnalyticsConstants.EnvelopDef.SEQUENCE_DETAILS));
        assertTrue(payload.get(AnalyticsConstants.EnvelopDef.SEQUENCE_DETAILS).isJsonObject());

        JsonObject sequenceDetails = payload.get(AnalyticsConstants.EnvelopDef.SEQUENCE_DETAILS).getAsJsonObject();
        assertTrue(sequenceDetails.has(AnalyticsConstants.EnvelopDef.SEQUENCE_NAME));
        assertTrue(sequenceDetails.has(AnalyticsConstants.EnvelopDef.SEQUENCE_TYPE));
    }

    private void verifyApiResourcePayload(JsonElement payloadElement) {
        assertNotNull(payloadElement);
        assertTrue(payloadElement.isJsonObject());

        JsonObject payload = payloadElement.getAsJsonObject();
        verifyCommonPayloadFields(payload);

        assertTrue(payload.has(AnalyticsConstants.EnvelopDef.REMOTE_HOST));
        assertTrue(payload.has(AnalyticsConstants.EnvelopDef.CONTENT_TYPE));
        assertTrue(payload.has(AnalyticsConstants.EnvelopDef.HTTP_METHOD));
        assertEquals(TEST_API_METHOD, payload.get(AnalyticsConstants.EnvelopDef.HTTP_METHOD).getAsString());

        assertTrue(payload.has(AnalyticsConstants.EnvelopDef.API_DETAILS));
        assertTrue(payload.get(AnalyticsConstants.EnvelopDef.API_DETAILS).isJsonObject());

        JsonObject apiDetails = payload.get(AnalyticsConstants.EnvelopDef.API_DETAILS).getAsJsonObject();
        assertTrue(apiDetails.has(AnalyticsConstants.EnvelopDef.API));
        assertEquals(TEST_API_NAME, apiDetails.get(AnalyticsConstants.EnvelopDef.API).getAsString());
        assertTrue(apiDetails.has(AnalyticsConstants.EnvelopDef.SUB_REQUEST_PATH));
        assertTrue(apiDetails.has(AnalyticsConstants.EnvelopDef.API_CONTEXT));
        assertEquals(TEST_API_CONTEXT, apiDetails.get(AnalyticsConstants.EnvelopDef.API_CONTEXT).getAsString());
        assertTrue(apiDetails.has(AnalyticsConstants.EnvelopDef.METHOD));
        assertEquals(TEST_API_METHOD, apiDetails.get(AnalyticsConstants.EnvelopDef.METHOD).getAsString());
    }

    private void verifyEndpointPayload(JsonElement payloadElement) {
        assertNotNull(payloadElement);
        assertTrue(payloadElement.isJsonObject());

        JsonObject payload = payloadElement.getAsJsonObject();
        verifyCommonPayloadFields(payload);

        assertTrue(payload.has(AnalyticsConstants.EnvelopDef.ENDPOINT_DETAILS));
        JsonObject endpointDetails = payload.get(AnalyticsConstants.EnvelopDef.ENDPOINT_DETAILS).getAsJsonObject();
        assertTrue(endpointDetails.has(AnalyticsConstants.EnvelopDef.ENDPOINT_NAME));
        assertEquals(TEST_ENDPOINT_NAME, endpointDetails.get(AnalyticsConstants.EnvelopDef.ENDPOINT_NAME).getAsString());
        assertTrue(endpointDetails.has(AnalyticsConstants.EnvelopDef.ENDPOINT_IS_ANONYMOUS));
        assertFalse(endpointDetails.get(AnalyticsConstants.EnvelopDef.ENDPOINT_IS_ANONYMOUS).getAsBoolean());
    }

    private void verifyProxyServicePayload(JsonElement payloadElement) {
        assertNotNull(payloadElement);
        assertTrue(payloadElement.isJsonObject());

        JsonObject payload = payloadElement.getAsJsonObject();
        verifyCommonPayloadFields(payload);

        assertTrue(payload.has(AnalyticsConstants.EnvelopDef.PROXY_SERVICE_DETAILS));
        JsonObject proxyServiceDetails = payload.get(AnalyticsConstants.EnvelopDef.PROXY_SERVICE_DETAILS).getAsJsonObject();
        assertTrue(proxyServiceDetails.has(AnalyticsConstants.EnvelopDef.PROXY_SERVICE_NAME));
    }

    private void verifyCommonPayloadFields(JsonObject payload) {
        assertTrue(payload.has(AnalyticsConstants.EnvelopDef.ENTITY_TYPE));
        assertTrue(payload.has(AnalyticsConstants.EnvelopDef.ENTITY_CLASS_NAME));
        assertTrue(payload.has(AnalyticsConstants.EnvelopDef.FAULT_RESPONSE));
        assertTrue(payload.has(AnalyticsConstants.EnvelopDef.LATENCY));
        assertTrue(payload.has(AnalyticsConstants.EnvelopDef.METADATA));
    }

    enum AnalyticPayloadType {
        PROXY_SERVICE,
        ENDPOINT,
        API,
        SEQUENCE,
        NON_STANDARD
    }
}
