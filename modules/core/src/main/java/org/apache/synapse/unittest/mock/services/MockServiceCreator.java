/*
 * *
 *  * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *  *
 *  * WSO2 Inc. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.apache.synapse.unittest.mock.services;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.log4j.Logger;
import org.apache.synapse.unittest.mock.services.core.Emulator;
import org.apache.synapse.unittest.mock.services.core.EmulatorType;
import org.apache.synapse.unittest.mock.services.http.dsl.HttpConsumerContext;

import javax.xml.ws.spi.http.HttpContext;

import static org.apache.synapse.unittest.Constants.GET_METHOD;
import static org.apache.synapse.unittest.Constants.POST_METHOD;
import static org.apache.synapse.unittest.mock.services.http.dsl.dto.consumer.IncomingMessage.request;
import static org.apache.synapse.unittest.mock.services.http.dsl.dto.consumer.OutgoingMessage.response;


/**
 * Class is responsible for creating mock services as request in descriptor data.
 */
public class MockServiceCreator {

    private static Logger logger = Logger.getLogger(MockServiceCreator.class.getName());
    private static boolean isMockServiceCreated = false;

    private MockServiceCreator() {}
    /**
     * Start service for given parameters using emulator.
     *
     * @param mockServiceName endpoint name given in descriptor data
     * @param host domain of the url
     * @param path path of the url
     * @param serviceMethod service method of the service
     * @param inputPayload service expected input payload
     * @param responseBody expected response of the service
     */
    public static void startServer(String mockServiceName, String host, int port, String path, String serviceMethod,
                                   String inputPayload, String responseBody) {

        //set flag to mock service started
        if (!isMockServiceCreated) {
            isMockServiceCreated = true;
        }

        try {
            switch (serviceMethod.toUpperCase()) {
                case GET_METHOD :
                    Emulator.getHttpEmulator()
                            .consumer()
                            .host(host)
                            .port(port)
                            .context(path)
                            .when(request().withMethod(HttpMethod.GET))
                            .respond(response().withBody(responseBody).withStatusCode(HttpResponseStatus.OK))
                            .operations().start();
                    break;

                case POST_METHOD :
                    Emulator.getHttpEmulator()
                            .consumer()
                            .host(host)
                            .port(port)
                            .context(path)
                            .when(request().withMethod(HttpMethod.POST).withBody(inputPayload))
                            .respond(response().withBody(responseBody).withStatusCode(HttpResponseStatus.OK))
                            .operations().start();
                    break;

                default:
                    Emulator.getHttpEmulator()
                            .consumer()
                            .host(host)
                            .port(port)
                            .context(path)
                            .when(request().withMethod(HttpMethod.GET))
                            .respond(response().withBody(responseBody).withStatusCode(HttpResponseStatus.OK))
                            .operations().start();
            }

            logger.info("Mock service started for " + mockServiceName + "as [" + serviceMethod.toUpperCase() +
                            "] in - http://" + host + ":" + port + path);


        } catch (Exception e) {
            logger.error("Error in initiating mock service named " + mockServiceName , e);
        }

    }

    /**
     * Stop all services created from the emulator by checking thread-id.
     */
    public static void stopServices() {
        if (isMockServiceCreated) {
            new Emulator().shutdown(EmulatorType.HTTP_CONSUMER);
            isMockServiceCreated = false;
        }
    }
}
