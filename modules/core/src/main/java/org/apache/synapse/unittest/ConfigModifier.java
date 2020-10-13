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

import org.apache.log4j.Logger;
import org.apache.synapse.unittest.testcase.data.holders.MockServiceData;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.apache.synapse.unittest.Constants.HTTP;
import static org.apache.synapse.unittest.Constants.SERVICE_HOST;


/**
 * Class responsible for modify the endpoint configuration data if mock-service data exists.
 * creates mock services and start those as in descriptor data.
 */
public class ConfigModifier {

    public ConfigModifier() {
    }

    private static Logger log = Logger.getLogger(ConfigModifier.class.getName());

    public static Map unitTestMockEndpointMap = new HashMap();

    /**
     * Method parse the artifact data received and replaces actual endpoint urls with mock urls.
     * Call mock service creator with relevant mock service data.
     * Thread waits until all mock services are starts by checking port availability
     *
     * @param mockServiceData mock service data received from descriptor data
     * @return return a exception of error occurred while creating mock services
     */
    static String mockServiceLoader(MockServiceData mockServiceData) {

        ArrayList<Integer> mockServicePorts = new ArrayList<>();

        for (int i = 0; i < mockServiceData.getMockServicesCount(); i++) {
            String endpointName = mockServiceData.getMockServices(i).getServiceName();

            int serviceElementIndex = mockServiceData.getServiceNameIndex(endpointName);
            int port = mockServiceData.getMockServices(serviceElementIndex).getPort();
            String context = mockServiceData.getMockServices(serviceElementIndex).getContext();
            String serviceURL = HTTP + SERVICE_HOST + ":" + port + context;

            mockServicePorts.add(port);
            unitTestMockEndpointMap.put(endpointName, serviceURL);

            log.info("Mock service creator ready to start service for " + endpointName);
            MockServiceCreator.startMockServiceServer(endpointName, SERVICE_HOST, port, context,
                    mockServiceData.getMockServices(serviceElementIndex).getResources());
        }

        //check services are ready to serve by checking the ports
        if (!mockServicePorts.isEmpty()) {
            try {
                checkServiceStatus(mockServicePorts);
            } catch (IOException e) {
                log.error("Error occurred in checking services are ready to serve in given ports", e);
            }
        }
        return null;
    }

    /**
     * Check services are ready to serve in given ports.
     *
     * @param mockServicePorts mock service port array
     */
    private static void checkServiceStatus(ArrayList<Integer> mockServicePorts) throws IOException {
        log.info("Thread waiting for mock service(s) starting");

        for (int port : mockServicePorts) {
            boolean isAvailable = true;
            long timeoutExpiredMs = System.currentTimeMillis() + 5000;
            while (isAvailable) {
                long waitMillis = timeoutExpiredMs - System.currentTimeMillis();
                isAvailable = checkPortAvailability(port);

                if (waitMillis <= 0) {
                    // timeout expired
                    throw new IOException("Connection refused for service in port - " + port);
                }
            }
        }

        log.info("Mock service(s) are started with given ports");
    }

    /**
     * Thread wait until all services are started by checking the ports.
     *
     * @param port mock service port
     * @return boolean value of port availability
     */
    private static boolean checkPortAvailability(int port) {
        boolean isAvailable;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port));
            isAvailable = false;
        } catch (IOException e) {
            isAvailable = true;
        }

        return isAvailable;
    }

}
