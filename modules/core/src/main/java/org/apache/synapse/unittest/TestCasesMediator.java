/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.unittest;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.util.UIDGenerator;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.http.HttpResponse;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.annotation.Contract;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HTTP;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.unittest.testcase.data.classes.TestCase;
import org.apache.synapse.util.xpath.ext.XpathExtensionUtil;

import java.io.IOException;
import java.net.URI;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.namespace.QName;

import static org.apache.synapse.unittest.Constants.DELETE_METHOD;
import static org.apache.synapse.unittest.Constants.EMPTY_VALUE;
import static org.apache.synapse.unittest.Constants.GET_METHOD;
import static org.apache.synapse.unittest.Constants.HEAD_METHOD;
import static org.apache.synapse.unittest.Constants.HTTPS_KEY;
import static org.apache.synapse.unittest.Constants.HTTPS_LOCALHOST_URL;
import static org.apache.synapse.unittest.Constants.HTTP_KEY;
import static org.apache.synapse.unittest.Constants.HTTP_LOCALHOST_URL;
import static org.apache.synapse.unittest.Constants.INPUT_PROPERTY_SCOPE_AXIS2;
import static org.apache.synapse.unittest.Constants.INPUT_PROPERTY_SCOPE_DEFAULT;
import static org.apache.synapse.unittest.Constants.INPUT_PROPERTY_SCOPE_TRANSPORT;
import static org.apache.synapse.unittest.Constants.JSON_FORMAT;
import static org.apache.synapse.unittest.Constants.OPTIONS_METHOD;
import static org.apache.synapse.unittest.Constants.PATCH_METHOD;
import static org.apache.synapse.unittest.Constants.POST_METHOD;
import static org.apache.synapse.unittest.Constants.PROXY_INVOKE_PREFIX_URL;
import static org.apache.synapse.unittest.Constants.PUT_METHOD;
import static org.apache.synapse.unittest.Constants.STRING_UTF8;
import static org.apache.synapse.unittest.Constants.TEST_CASE_INPUT_PROPERTY_NAME;
import static org.apache.synapse.unittest.Constants.TEST_CASE_INPUT_PROPERTY_SCOPE;
import static org.apache.synapse.unittest.Constants.TEST_CASE_INPUT_PROPERTY_VALUE;
import static org.apache.synapse.unittest.Constants.TEXT_FORMAT;
import static org.apache.synapse.unittest.Constants.TEXT_NAMESPACE;
import static org.apache.synapse.unittest.Constants.XML_FORMAT;


/**
 * Class is responsible for mediating incoming payload with relevant configuration.
 */
public class TestCasesMediator {

    private TestCasesMediator() {
    }

    private static Log log = LogFactory.getLog(UnitTestingExecutor.class.getName());
    private static int httpPassThruOperatingPort = Integer.parseInt(System.getProperty("http.nio.port"));
    private static int httpsPassThruOperatingPort = Integer.parseInt(System.getProperty("https.nio.port"));

    /**
     * Sequence mediation of receiving test cases using deployed sequence deployer.
     * set input properties for the Message Context
     *
     * @param currentTestCase current test case
     * @param synConfig       synapse configuration used to deploy sequenceDeployer
     * @param key             key of the sequence deployer
     * @return result of mediation and message context as a Map.Entry
     */
    static Map.Entry<Boolean, MessageContext> sequenceMediate(TestCase currentTestCase, SynapseConfiguration
            synConfig, String key) {
        Mediator sequenceMediator = synConfig.getSequence(key);
        MessageContext msgCtxt = createSynapseMessageContext(currentTestCase.getInputPayload(), synConfig);

        boolean mediationResult = false;

        if (msgCtxt != null) {
            mediationResult = sequenceMediator
                    .mediate(setInputMessageProperties(msgCtxt, currentTestCase.getPropertyMap()));
        }

        return new AbstractMap.SimpleEntry<>(mediationResult, msgCtxt);
    }

    /**
     * Proxy service invoke using http client for receiving test cases.
     *
     * @param currentTestCase current test case
     * @param key             key of the proxy service
     * @return response received from the proxy service
     */
    static Map.Entry<String, HttpResponse> proxyServiceExecutor(TestCase currentTestCase, String proxyTransportMethod,
                                                                String key) throws IOException {

        String url;

        //check transport port whether http or https
        if (proxyTransportMethod.equalsIgnoreCase(HTTP_KEY)) {
            url = HTTP_LOCALHOST_URL + httpPassThruOperatingPort + PROXY_INVOKE_PREFIX_URL + key;
        } else if (proxyTransportMethod.equalsIgnoreCase(HTTPS_KEY)) {
            url = HTTPS_LOCALHOST_URL + httpsPassThruOperatingPort + PROXY_INVOKE_PREFIX_URL + key;
        } else {
            return new AbstractMap.SimpleEntry<>("'" + proxyTransportMethod + "' transport is not supported", null);
        }

        log.info("Invoking URI - " + url);

        HttpClient clientConnector = HttpClientBuilder.create().build();
        HttpResponse response;
        HttpPost httpPost = new HttpPost(url);
        setTransportHeaders(httpPost, currentTestCase);
        setBody(httpPost, currentTestCase);

        try {
            response = clientConnector.execute(httpPost);
        } catch (IOException e) {
            throw new IOException("Proxy service invoked URL - " + url + "\n" + e);
        }

        return new AbstractMap.SimpleEntry<>(url, response);
    }

    /**
     * API resource invoke using http client for receiving test cases.
     *
     * @param currentTestCase current test case
     * @param resourceMethod  resource method
     * @param context         context of the resource
     * @return response received from the API resource
     */
    static Map.Entry<String, HttpResponse> apiResourceExecutor(TestCase currentTestCase, String context,
                                                               String resourceMethod, String protocolType) throws IOException {

        String urlPrefix = "";
        if (protocolType.equals(HTTP_KEY)) {
            urlPrefix = HTTP_LOCALHOST_URL + httpPassThruOperatingPort ;
        } else if (protocolType.equals(HTTPS_KEY)) {
            urlPrefix = HTTPS_LOCALHOST_URL + httpsPassThruOperatingPort;
        } else {
            return new AbstractMap.SimpleEntry<>("'" + urlPrefix + "' transport is not supported", null);
        }


        String url;
        if (currentTestCase.getRequestPath() != null) {
            if (currentTestCase.getRequestPath().equals("/")) {
                url = urlPrefix + context;
            } else if (currentTestCase.getRequestPath().startsWith("/")) {
                url = urlPrefix + context + currentTestCase.getRequestPath();
            } else {
                url = urlPrefix + context + "/" + currentTestCase.getRequestPath();
            }

        } else {
            url = urlPrefix + context;
        }


        log.info("Invoking URI - " + url);

        HttpClient clientConnector = HttpClientBuilder.create().build();
        HttpResponse response;
        String resourceHTTPMethod = resourceMethod.toUpperCase(Locale.ENGLISH);
        String invokeUrlWithMethod = resourceHTTPMethod + ": " + url;
        try {
            switch (resourceHTTPMethod) {
                case GET_METHOD:
                    //GET - entity non-enclosing
                    HttpGet httpGet = new HttpGet(url);
                    setTransportHeaders(httpGet, currentTestCase);
                    response = clientConnector.execute(httpGet);
                    break;

                case POST_METHOD:
                    //POST - entity enclosing
                    HttpPost httpPost = new HttpPost(url);
                    setTransportHeaders(httpPost, currentTestCase);
                    setBody(httpPost, currentTestCase);
                    response = clientConnector.execute(httpPost);
                    break;

                case PUT_METHOD:
                    //PUT - entity enclosing
                    HttpPut httpPut = new HttpPut(url);
                    setTransportHeaders(httpPut, currentTestCase);
                    setBody(httpPut, currentTestCase);
                    response = clientConnector.execute(httpPut);
                    break;

                case DELETE_METHOD:
                    //DELETE - entity enclosing
                    HttpDeleteWithBody httpDelete = setDeleteWithBody(currentTestCase, url);
                    response = clientConnector.execute(httpDelete);
                    break;

                case OPTIONS_METHOD:
                    //OPTIONS - entity non-enclosing
                    HttpOptions httpOptions = new HttpOptions(url);
                    setTransportHeaders(httpOptions, currentTestCase);
                    response = clientConnector.execute(httpOptions);
                    break;

                case HEAD_METHOD:
                    //HEAD - entity non-enclosing
                    HttpHead httpHead = new HttpHead(url);
                    setTransportHeaders(httpHead, currentTestCase);
                    response = clientConnector.execute(httpHead);
                    break;

                case PATCH_METHOD:
                    //PATCH - entity enclosing
                    HttpPatch httpPatch = new HttpPatch(url);
                    setTransportHeaders(httpPatch, currentTestCase);
                    setBody(httpPatch, currentTestCase);
                    response = clientConnector.execute(httpPatch);
                    break;

                default:
                    throw new ClientProtocolException("HTTP client can't find proper request method");
            }
        } catch (IOException e) {
            throw new IOException("API invoked URL - " + invokeUrlWithMethod + "\n" + e);
        }

        return new AbstractMap.SimpleEntry<>(invokeUrlWithMethod, response);
    }

    /**
     * Set a payload to Entity Enclosing Request.
     *
     * @param request HttpEntityEnclosingRequestBase
     * @param currentTestCase current test case
     */
    public static void setBody(HttpEntityEnclosingRequestBase request, TestCase currentTestCase) {
        String patchPayload = currentTestCase.getInputPayload();
        if (patchPayload == null) {
            patchPayload = EMPTY_VALUE;
        }
        StringEntity patchEntity = new StringEntity(patchPayload, getCharSetEncoding(currentTestCase));
        request.setEntity(patchEntity);
    }

    /**
     * Propagate transport properties of the test case as headers in the Http request.
     *
     * @param request Http request
     * @param currentTestCase current test case
     */
    private static void setTransportHeaders(HttpRequestBase request, TestCase currentTestCase) {

        for (Map<String, String> property : currentTestCase.getPropertyMap()) {
            String scope = property.get(TEST_CASE_INPUT_PROPERTY_SCOPE);
            //Setting Synapse properties
            if (scope.equals(INPUT_PROPERTY_SCOPE_TRANSPORT)) {
                request.setHeader(property.get(TEST_CASE_INPUT_PROPERTY_NAME),
                        property.get(TEST_CASE_INPUT_PROPERTY_VALUE));
            }
        }
    }

    /**
     * Set headers for delete client.
     *
     * @param currentTestCase testcase data
     * @param url             Url of the service
     * @return HttpDeleteWithBody client with headers and body if exist
     * @throws IOException while creating payload from StringEntity
     */
    private static HttpDeleteWithBody setDeleteWithBody(TestCase currentTestCase, String url) throws IOException {
        HttpDeleteWithBody httpDelete = new HttpDeleteWithBody(url);
        setTransportHeaders(httpDelete, currentTestCase);
        setBody(httpDelete, currentTestCase);
        return httpDelete;
    }

    /**
     * Creating message context using input payload and the synapse configuration.
     *
     * @param payload       received input payload for particular test case
     * @param synapseConfig synapse configuration used for deploy the sequence deployer
     * @return message context
     */
    private static MessageContext createSynapseMessageContext(String payload, SynapseConfiguration synapseConfig) {

        MessageContext synapseMessageContext = null;
        try {
            org.apache.axis2.context.MessageContext messageContext =
                    new org.apache.axis2.context.MessageContext();
            AxisConfiguration axisConfig = synapseConfig.getAxisConfiguration();
            if (axisConfig == null) {
                axisConfig = new AxisConfiguration();
                synapseConfig.setAxisConfiguration(axisConfig);
            }
            ConfigurationContext configurationContext = new ConfigurationContext(axisConfig);
            SynapseEnvironment env = new Axis2SynapseEnvironment(configurationContext, synapseConfig);
            XpathExtensionUtil.initXpathExtensions(env);

            //Create custom Axis2MessageContext with required data and SOAP1.1 envelop
            synapseMessageContext = new Axis2MessageContext(messageContext, synapseConfig, env);
            synapseMessageContext.setContinuationEnabled(true);
            synapseMessageContext.setMessageID(UIDGenerator.generateURNString());
            synapseMessageContext.setEnvelope(OMAbstractFactory.getSOAP11Factory().createSOAPEnvelope());
            SOAPEnvelope envelope = synapseMessageContext.getEnvelope();
            envelope.addChild(OMAbstractFactory.getSOAP12Factory().createSOAPBody());
            ((Axis2MessageContext) synapseMessageContext).getAxis2MessageContext()
                    .setConfigurationContext(configurationContext);
            ((Axis2MessageContext) synapseMessageContext).getAxis2MessageContext()
                    .setOperationContext(new OperationContext(new InOutAxisOperation(), new ServiceContext()));

            if (payload != null) {
                Map.Entry<String, String> inputPayload = CommonUtils.checkInputStringFormat(payload);
                String inputPayloadType = inputPayload.getKey();
                String trimmedInputPayload = inputPayload.getValue();

                if (inputPayloadType.equals(XML_FORMAT)) {
                    envelope.getBody().addChild(createOMElement(trimmedInputPayload));
                } else if (inputPayloadType.equals(JSON_FORMAT)) {
                    org.apache.axis2.context.MessageContext axis2MessageContext =
                            ((Axis2MessageContext) synapseMessageContext).getAxis2MessageContext();
                    envelope.getBody().addChild(
                            JsonUtil.getNewJsonPayload(axis2MessageContext, trimmedInputPayload, true, true));
                } else if (inputPayloadType.equals(TEXT_FORMAT)) {
                    envelope.getBody().addChild(getTextElement(trimmedInputPayload));
                }
            }
        } catch (Exception e) {
            log.error("Exception while creating synapse message context", e);
        }

        return synapseMessageContext;
    }

    /**
     * Creating OMElement for payload.
     *
     * @param xml input payload
     * @return payload in OMElement type
     */
    public static OMElement createOMElement(String xml) {
        return SynapseConfigUtils.stringToOM(xml);
    }

    /**
     * Set input property values in MessageContext.
     *
     * @param messageContext message context
     * @param properties     input property values
     * @return message context with input properties
     */
    private static MessageContext setInputMessageProperties(MessageContext messageContext,
                                                            List<Map<String, String>> properties) {

        try {
            for (Map<String, String> property : properties) {
                String scope = property.get(TEST_CASE_INPUT_PROPERTY_SCOPE);

                Axis2MessageContext axis2MessageContext = (Axis2MessageContext) messageContext;
                org.apache.axis2.context.MessageContext axis2MessageCtx =
                        axis2MessageContext.getAxis2MessageContext();

                //Setting Synapse properties
                switch (scope) {

                    case INPUT_PROPERTY_SCOPE_DEFAULT:
                        messageContext.setProperty(property.get(TEST_CASE_INPUT_PROPERTY_NAME),
                                property.get(TEST_CASE_INPUT_PROPERTY_VALUE));
                        break;

                    case INPUT_PROPERTY_SCOPE_AXIS2:
                        //Setting Axis2 properties
                        axis2MessageCtx.setProperty(property.get(TEST_CASE_INPUT_PROPERTY_NAME),
                                property.get(TEST_CASE_INPUT_PROPERTY_VALUE));
                        break;

                    case INPUT_PROPERTY_SCOPE_TRANSPORT:
                        //Setting Transport Headers
                        Object headers = axis2MessageCtx.getProperty(
                                org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

                        Map<String, Object> headersMap;
                        if (headers != null) {
                            headersMap = (Map) headers;
                        } else {
                            // Add the transport headers to the message context
                            headersMap = new TreeMap<>(new Comparator<String>() {
                                public int compare(String o1, String o2) {

                                    return o1.compareToIgnoreCase(o2);
                                }
                            });
                            axis2MessageCtx.setProperty(
                                    org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS,
                                    headersMap);
                        }
                        headersMap.put(property.get(TEST_CASE_INPUT_PROPERTY_NAME),
                                property.get(TEST_CASE_INPUT_PROPERTY_VALUE));
                        break;

                    default:
                        throw new IOException("Property scope not defined for sequences");
                }
            }
        } catch (Exception e) {
            log.error("Error while setting properties to the Message Context", e);
        }

        return messageContext;
    }

    /**
     * Create OMElement for the text input payload with a namespace.
     *
     * @param content input message
     * @return OMElement of input message
     */
    private static OMElement getTextElement(String content) {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMElement textElement = factory.createOMElement(new QName(TEXT_NAMESPACE, "text"));
        if (content == null) {
            content = EMPTY_VALUE;
        }
        textElement.setText(content);
        return textElement;
    }

    /**
     * Retrieves the charset encoding type by the content-type header.
     *
     * @param currentTestCase current test case
     * @return String charset
     */
    private static String getCharSetEncoding(TestCase currentTestCase) {

        String charSetEncoding = STRING_UTF8;
        ContentType contentType = null;
        for (Map<String, String> property : currentTestCase.getPropertyMap()) {
            if (property.get(TEST_CASE_INPUT_PROPERTY_NAME) != null
                    && property.get(TEST_CASE_INPUT_PROPERTY_NAME).equals(HTTP.CONTENT_TYPE)) {
                contentType = ContentType.parse(property.get(TEST_CASE_INPUT_PROPERTY_VALUE));
                break;
            }
        }
        if (contentType != null && contentType.getCharset() != null) {
            charSetEncoding = contentType.getCharset().name();
        }
        return charSetEncoding;
    }

    @Contract(threading = ThreadingBehavior.UNSAFE)
    static class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {
        static final String METHOD_NAME = "DELETE";

        public String getMethod() {
            return METHOD_NAME;
        }

        HttpDeleteWithBody(final String uri) {
            super();
            setURI(URI.create(uri));
        }
    }
}
