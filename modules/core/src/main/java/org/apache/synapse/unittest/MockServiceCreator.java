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

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.emulator.RequestProcessor;
import org.apache.synapse.commons.emulator.core.Emulator;
import org.apache.synapse.commons.emulator.http.HTTPProtocolEmulator;
import org.apache.synapse.commons.emulator.http.dsl.HttpConsumerContext;
import org.apache.synapse.commons.emulator.http.dsl.dto.consumer.IncomingMessage;
import org.apache.synapse.commons.emulator.http.dsl.dto.consumer.OutgoingMessage;
import org.apache.synapse.unittest.testcase.data.classes.ServiceResource;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.apache.synapse.commons.emulator.http.dsl.dto.consumer.IncomingMessage.request;
import static org.apache.synapse.commons.emulator.http.dsl.dto.consumer.OutgoingMessage.response;
import static org.apache.synapse.unittest.Constants.HEAD_METHOD;
import static org.apache.synapse.unittest.Constants.OPTIONS_METHOD;
import static org.apache.synapse.unittest.Constants.POST_METHOD;
import static org.apache.synapse.unittest.Constants.PATCH_METHOD;


/**
 * Class is responsible for creating mock services as request in descriptor data.
 */
class MockServiceCreator {

    private static Log log = LogFactory.getLog(MockServiceCreator.class.getName());
    private static List<HTTPProtocolEmulator> emulatorServiceList = new ArrayList<>();
    private MockServiceCreator() {
    }

    /**
     * Start service for given parameters using emulator.
     *
     * @param mockServiceName endpoint name given in descriptor data
     * @param host            domain of the url
     * @param context         path of the url
     * @param resources       resources of the service
     */
    static void startMockServiceServer(String mockServiceName, String host, int port, String context,
                                              List<ServiceResource> resources) {

        try {
            HTTPProtocolEmulator httpEmulator = new Emulator().getHttpProtocolEmulator();
            emulatorServiceList.add(httpEmulator);
            HttpConsumerContext emulator = httpEmulator
                    .consumer()
                    .host(host)
                    .port(port)
                    .context(context);

            for (ServiceResource resource : resources) {
                routeThroughResourceMethod(resource, emulator);
            }

            emulator.operations().start();
            log.info("Mock service started for " + mockServiceName + " in - http://" + host + ":" + port + context);
        } catch (Exception e) {
            log.error("Error in initiating mock service named " + mockServiceName, e);
        }

    }

    /**
     * Start service for given parameters using emulator.
     *
     * @param resource mock service resource data
     * @param emulator HttpConsumerContext emulator object
     */
    private static void routeThroughResourceMethod(ServiceResource resource, HttpConsumerContext emulator) {
        int serviceResponseStatusCode = resource.getStatusCode();
        HttpResponseStatus responseStatus = HttpResponseStatus.valueOf(serviceResponseStatusCode);

        String serviceMethod = resource.getMethod();
        String serviceSubContext = resource.getSubContext();
        Map.Entry<String, Map<String, String>> checkQueryParamEntry = splitQueryParams(serviceSubContext);
        serviceSubContext = checkQueryParamEntry.getKey();
        Map<String, String> queryParams = checkQueryParamEntry.getValue();
        String serviceRequestPayload = "";
        if (resource.getRequestPayload() != null) {
            serviceRequestPayload = RequestProcessor.trimStrings(resource.getRequestPayload());
        }

        String serviceResponsePayload = resource.getResponsePayload();
        List<Map.Entry<String, String>> requestHeaders = new ArrayList<>();
        List<Map.Entry<String, String>> responseHeaders = new ArrayList<>();

        if (resource.getRequestHeaders() != null) {
            requestHeaders = resource.getRequestHeaders();
        }

        if (resource.getResponseHeaders() != null) {
            responseHeaders = resource.getResponseHeaders();
        }

        IncomingMessage incomingMessage;

        switch (serviceMethod.toUpperCase()) {

            case POST_METHOD:
                incomingMessage = request().withMethod(HttpMethod.POST)
                        .withBody(serviceRequestPayload).withPath(serviceSubContext);
                break;

            case PATCH_METHOD:
                incomingMessage = request().withMethod(HttpMethod.PATCH)
                                           .withBody(serviceRequestPayload).withPath(serviceSubContext);
                break;

            case OPTIONS_METHOD:
                incomingMessage = request().withMethod(HttpMethod.OPTIONS)
                                           .withBody(serviceRequestPayload).withPath(serviceSubContext);
                break;

            case HEAD_METHOD:
                incomingMessage = request().withMethod(HttpMethod.HEAD)
                                           .withBody(serviceRequestPayload).withPath(serviceSubContext);
                break;

            default:
                incomingMessage = request().withMethod(HttpMethod.GET).withPath(serviceSubContext);
                break;
        }

        for (Map.Entry<String, String> queryParam : queryParams.entrySet()) {
            incomingMessage.withQueryParameter(queryParam.getKey(), queryParam.getValue());
        }
        for (Map.Entry<String, String> header : requestHeaders) {
            incomingMessage.withHeader(header.getKey(), header.getValue());
        }
        emulator.when(incomingMessage);

        //adding headers of response
        OutgoingMessage outGoingMessage =
                response().withBody(serviceResponsePayload).withStatusCode(responseStatus);
        for (Map.Entry<String, String> header : responseHeaders) {
            outGoingMessage.withHeader(header.getKey(), header.getValue());
        }
        emulator.respond(outGoingMessage);
    }

    /**
     * Stop all services created from the emulator by checking thread-id.
     */
    static void stopServices() {
        for (HTTPProtocolEmulator emulatorService : emulatorServiceList) {
            emulatorService.shutdown();
        }

        emulatorServiceList.clear();
    }

    /**
     * Splits query params from the URL if exists.
     *
     * @param serviceSubContext mock service sub context
     * @return subContext and query params as map entry
     */
    private static Map.Entry<String, Map<String, String>> splitQueryParams(String serviceSubContext) {
        Map<String, String> queryParams = new LinkedHashMap<>();
        try {
            URL url = new URL(Constants.HTTP + Constants.SERVICE_HOST + serviceSubContext);
            String query = url.getQuery();
            serviceSubContext = url.getPath();
            if (query == null) {
                return new AbstractMap.SimpleEntry<>(serviceSubContext, queryParams);
            }
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf('=');
                queryParams.put(URLDecoder.decode(pair.substring(0, idx), Constants.STRING_UTF8),
                        URLDecoder.decode(pair.substring(idx + 1), Constants.STRING_UTF8));
            }
        } catch (UnsupportedEncodingException e) {
            log.error("Error while checking query params", e);
        } catch (MalformedURLException e) {
            log.error("Error while creating URL of the mock service sub context", e);
        }
        return new AbstractMap.SimpleEntry<>(serviceSubContext, queryParams);
    }
}
