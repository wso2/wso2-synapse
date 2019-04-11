/*
 Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import javafx.util.Pair;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMDocument;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.unittest.testcase.data.classes.TestCase;

import java.io.IOException;
import java.util.*;

import static org.apache.synapse.unittest.Constants.*;

/**
 * Class is responsible for mediating incoming payload with relevant configuration.
 */
public class TestCasesMediator {

    private TestCasesMediator() {
    }

    private static Logger logger = Logger.getLogger(UnitTestingExecutor.class.getName());

    /**
     * Sequence mediation of receiving test cases using deployed sequence deployer.
     * set input properties for the Message Context
     *
     * @param currentTestCase current test case
     * @param synConfig       synapse configuration used to deploy sequenceDeployer
     * @param key             key of the sequence deployer
     * @return result of mediation and message context as a Pair<>
     */
    static Pair<Boolean, MessageContext> sequenceMediate(TestCase currentTestCase, SynapseConfiguration synConfig,
                                                         String key) {
        Mediator sequenceMediator = synConfig.getSequence(key);
        MessageContext msgCtxt = createSynapseMessageContext(currentTestCase.getInputPayload(), synConfig);

        boolean mediationResult = false;

        if (msgCtxt != null) {
            mediationResult = sequenceMediator
                    .mediate(setInputMessageProperties(msgCtxt, currentTestCase.getPropertyMap()));
        }

        return new Pair<>(mediationResult, msgCtxt);
    }

    /**
     * Proxy service invoke using http client for receiving test cases.
     *
     * @param currentTestCase current test case
     * @param key             key of the proxy service
     * @return response received from the proxy service
     */
    static HttpResponse proxyServiceExecutor(TestCase currentTestCase, String key) throws IOException {

        String url = PROXY_INVOKE_PREFIX_URL + key;
        logger.info("Invoking URI - " + url);

        HttpClient clientConnector = HttpClientBuilder.create().build();
        HttpPost httpPost = new HttpPost(url);

        //set headers
        for (Map<String, String> property : currentTestCase.getPropertyMap()) {
            String scope = property.get(TEST_CASE_INPUT_PROPERTY_SCOPE);
            //Setting Synapse properties
            if (scope.equals(INPUT_PROPERTY_SCOPE_TRANSPORT)) {
                httpPost.setHeader(property.get(TEST_CASE_INPUT_PROPERTY_NAME),
                        property.get(TEST_CASE_INPUT_PROPERTY_VALUE));
            }
        }

        if (currentTestCase.getInputPayload() != null) {
            StringEntity postEntity = new StringEntity(currentTestCase.getInputPayload().trim());
            httpPost.setEntity(postEntity);
        }

        HttpResponse response = clientConnector.execute(httpPost);

        int responseCode = response.getStatusLine().getStatusCode();
        if (responseCode/100 == 2) {
            return response;
        } else {
            return null;
        }
    }

    /**
     * API resource invoke using http client for receiving test cases.
     *
     * @param currentTestCase current test case
     * @param resourceMethod  resource method
     * @param context         context of the resource
     * @return response received from the API resource
     */
    static HttpResponse apiResourceExecutor(TestCase currentTestCase, String context, String resourceMethod)
            throws IOException {

        String url;
        if (currentTestCase.getRequestPath() != null) {
            if (currentTestCase.getRequestPath().startsWith("/")) {
                url = API_INVOKE_PREFIX_URL + context + currentTestCase.getRequestPath();
            } else {
                url = API_INVOKE_PREFIX_URL + context + "/" + currentTestCase.getRequestPath();
            }

        } else {
            url = API_INVOKE_PREFIX_URL + context;
        }


        logger.info("Invoking URI - " + url);

        HttpClient clientConnector = HttpClientBuilder.create().build();
        HttpResponse response;

        switch (resourceMethod.toUpperCase(Locale.ENGLISH)) {
            case GET_METHOD:
                //set headers and execute
                response = clientConnector.execute(setGetHeaders(currentTestCase, url));
                break;

            case POST_METHOD:
                //set headers
                HttpPost httpPost = setPostHeaders(currentTestCase, url);

                StringEntity postEntity = new StringEntity(currentTestCase.getInputPayload());
                httpPost.setEntity(postEntity);
                response = clientConnector.execute(httpPost);
                break;

            case PUT_METHOD:
                //set headers
                HttpPut httpPut = setPutHeaders(currentTestCase, url);

                StringEntity putEntity = new StringEntity(currentTestCase.getInputPayload());
                httpPut.setEntity(putEntity);
                response = clientConnector.execute(httpPut);
                break;

            case DELETE_METHOD:
                HttpDelete httpDelete = new HttpDelete(url);
                response = clientConnector.execute(httpDelete);
                break;

            default:
                throw new ClientProtocolException("HTTP client can't find proper request method");
        }

        int responseCode = response.getStatusLine().getStatusCode();
        if (responseCode/100 == 2) {
            return response;
        } else {
            return null;
        }
    }


    /**
     * Set headers for get client.
     *
     * @param currentTestCase testcase data
     * @param url         Url of the service
     * @return get client with headers
     */
    private static HttpGet setGetHeaders(TestCase currentTestCase, String url) {
        HttpGet httpGet = new HttpGet(url);

        for (Map<String, String> property : currentTestCase.getPropertyMap()) {
            String scope = property.get(TEST_CASE_INPUT_PROPERTY_SCOPE);

            //Setting Synapse properties
            if (scope.equals(INPUT_PROPERTY_SCOPE_TRANSPORT)) {
                httpGet.setHeader(property.get(TEST_CASE_INPUT_PROPERTY_NAME),
                        property.get(TEST_CASE_INPUT_PROPERTY_VALUE));
            }
        }

        return httpGet;
    }

    /**
     * Set headers for post client.
     *
     * @param currentTestCase testcase data
     * @param url         Url of the service
     * @return post client with headers
     */
    private static HttpPost setPostHeaders(TestCase currentTestCase, String url) {
        HttpPost httpPost = new HttpPost(url);
        //set headers
        for (Map<String, String> property : currentTestCase.getPropertyMap()) {
            String scope = property.get(TEST_CASE_INPUT_PROPERTY_SCOPE);

            //Setting Synapse properties
            if (scope.equals(INPUT_PROPERTY_SCOPE_TRANSPORT)) {
                httpPost.setHeader(property.get(TEST_CASE_INPUT_PROPERTY_NAME),
                        property.get(TEST_CASE_INPUT_PROPERTY_VALUE));
            }
        }

        return httpPost;
    }

    /**
     * Set headers for put client.
     *
     * @param currentTestCase testcase data
     * @param url         Url of the service
     * @return put client with headers
     */
    private static HttpPut setPutHeaders(TestCase currentTestCase, String url) {
        HttpPut httpPut = new HttpPut(url);
        //set headers
        for (Map<String, String> property : currentTestCase.getPropertyMap()) {
            String scope = property.get(TEST_CASE_INPUT_PROPERTY_SCOPE);

            //Setting Synapse properties
            if (scope.equals(INPUT_PROPERTY_SCOPE_TRANSPORT)) {
                httpPut.setHeader(property.get(TEST_CASE_INPUT_PROPERTY_NAME),
                        property.get(TEST_CASE_INPUT_PROPERTY_VALUE));
            }
        }

        return httpPut;
    }

    /**
     * Creating message context using input payload and the synapse configuration.
     *
     * @param payload received input payload for particular test case
     * @param config  synapse configuration used for deploy the sequence deployer
     * @return message context
     */
    private static MessageContext createSynapseMessageContext(String payload, SynapseConfiguration config) {

        MessageContext synMc = null;
        try {
            org.apache.axis2.context.MessageContext mc =
                    new org.apache.axis2.context.MessageContext();
            AxisConfiguration axisConfig = config.getAxisConfiguration();
            if (axisConfig == null) {
                axisConfig = new AxisConfiguration();
                config.setAxisConfiguration(axisConfig);
            }
            ConfigurationContext cfgCtx = new ConfigurationContext(axisConfig);
            SynapseEnvironment env = new Axis2SynapseEnvironment(cfgCtx, config);

            synMc = new Axis2MessageContext(mc, config, env);
            SOAPEnvelope envelope =
                    OMAbstractFactory.getSOAP11Factory().getDefaultEnvelope();
            OMDocument omDoc =
                    OMAbstractFactory.getSOAP11Factory().createOMDocument();
            omDoc.addChild(envelope);

            envelope.getBody().addChild(createOMElement(payload));

            synMc.setEnvelope(envelope);
        } catch (Exception e) {
            logger.error(e);
        }

        return synMc;
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
     * Set input property values in MessageContext
     *
     * @param msgCtxt message context
     * @param properties input property values
     * @return message context with input properties
     */
    private static MessageContext setInputMessageProperties(MessageContext msgCtxt,
                                                            List<Map<String, String>> properties) {

        try {
            for (Map<String, String> property : properties) {
                String scope = property.get(TEST_CASE_INPUT_PROPERTY_SCOPE);

                //Setting Synapse properties
                switch (scope) {

                    case INPUT_PROPERTY_SCOPE_DEFAULT:
                        msgCtxt.setProperty(property.get(TEST_CASE_INPUT_PROPERTY_NAME),
                                property.get(TEST_CASE_INPUT_PROPERTY_VALUE));
                        break;

                    case INPUT_PROPERTY_SCOPE_AXIS2:
                        //Setting Axis2 properties
                        Axis2MessageContext axis2smc = (Axis2MessageContext) msgCtxt;
                        org.apache.axis2.context.MessageContext axis2MessageCtx =
                                axis2smc.getAxis2MessageContext();
                        axis2MessageCtx.setProperty(property.get(TEST_CASE_INPUT_PROPERTY_NAME),
                                property.get(TEST_CASE_INPUT_PROPERTY_VALUE));
                        break;

                    case INPUT_PROPERTY_SCOPE_TRANSPORT:
                        //Setting Transport Headers
                        Axis2MessageContext axis2mc = (Axis2MessageContext) msgCtxt;
                        org.apache.axis2.context.MessageContext axis2MessageCtxt =
                                axis2mc.getAxis2MessageContext();
                        Object headers = axis2MessageCtxt.getProperty(
                                org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);


                        if (headers != null) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> headersMap = (Map) headers;
                            headersMap.put(property.get(TEST_CASE_INPUT_PROPERTY_NAME),
                                    property.get(TEST_CASE_INPUT_PROPERTY_VALUE));
                        }
                        if (headers == null) {
                            Map<String, Object> headersMap = new TreeMap<>(new Comparator<String>() {
                                public int compare(String o1, String o2) {
                                    return o1.compareToIgnoreCase(o2);
                                }
                            });
                            headersMap.put(property.get(TEST_CASE_INPUT_PROPERTY_NAME),
                                    property.get(TEST_CASE_INPUT_PROPERTY_VALUE));
                            axis2MessageCtxt.setProperty(
                                    org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS,
                                    headersMap);
                        }

                        break;

                    default:
                        throw new IOException("Property scope not defined for sequences");
                }
            }
        } catch (Exception e) {
            logger.error("Error while setting properties to the Message Context", e);
        }

        return msgCtxt;
    }
}
