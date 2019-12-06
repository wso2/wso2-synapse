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
import org.apache.log4j.Logger;
import org.apache.synapse.commons.emulator.RequestProcessor;
import org.apache.synapse.commons.emulator.core.Emulator;
import org.apache.synapse.commons.emulator.http.HTTPProtocolEmulator;
import org.apache.synapse.commons.emulator.http.dsl.HttpConsumerContext;
import org.apache.synapse.commons.emulator.http.dsl.dto.consumer.IncomingMessage;
import org.apache.synapse.commons.emulator.http.dsl.dto.consumer.OutgoingMessage;
import org.apache.synapse.unittest.testcase.data.classes.ServiceResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.apache.synapse.commons.emulator.http.dsl.dto.consumer.IncomingMessage.request;
import static org.apache.synapse.commons.emulator.http.dsl.dto.consumer.OutgoingMessage.response;
import static org.apache.synapse.unittest.Constants.GET_METHOD;
import static org.apache.synapse.unittest.Constants.POST_METHOD;


/**
 * Class is responsible for creating mock services as request in descriptor data.
 */
class MockServiceCreator {

    private static Logger log = Logger.getLogger(MockServiceCreator.class.getName());
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


        switch (serviceMethod.toUpperCase()) {
            case GET_METHOD:
                //adding headers of request
                IncomingMessage incomingMessage =
                        request().withMethod(HttpMethod.GET).withPath(serviceSubContext);
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
                break;

            case POST_METHOD:
                //adding headers of request
                incomingMessage = request().withMethod(HttpMethod.POST)
                        .withBody(serviceRequestPayload).withPath(serviceSubContext);
                for (Map.Entry<String, String> header : requestHeaders) {
                    incomingMessage.withHeader(header.getKey(), header.getValue());
                }
                emulator.when(incomingMessage);

                //adding headers of response
                outGoingMessage =
                        response().withBody(serviceResponsePayload).withStatusCode(responseStatus);
                for (Map.Entry<String, String> header : responseHeaders) {
                    outGoingMessage.withHeader(header.getKey(), header.getValue());
                }

                emulator.respond(outGoingMessage);
                break;

            default:
                //adding headers of request
                incomingMessage = request().withMethod(HttpMethod.GET).withPath(serviceSubContext);
                for (Map.Entry<String, String> header : requestHeaders) {
                    incomingMessage.withHeader(header.getKey(), header.getValue());
                }
                emulator.when(incomingMessage);

                //adding headers of response
                outGoingMessage =
                        response().withBody(serviceResponsePayload).withStatusCode(responseStatus);
                for (Map.Entry<String, String> header : responseHeaders) {
                    outGoingMessage.withHeader(header.getKey(), header.getValue());
                }

                emulator.respond(outGoingMessage);
                break;
        }
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
}
