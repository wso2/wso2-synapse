/*
 Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.unittest.data.holders;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock service data holder class in unit test framework.
 */
public class MockServiceData {

    private int mockServicesCount = 0;
    private Map<String, Integer> serviceNameMap = new HashMap<>();
    private ArrayList<Integer> servicePort = new ArrayList<>();
    private ArrayList<String> serviceContext = new ArrayList<>();
    private ArrayList<String> serviceType = new ArrayList<>();
    private ArrayList<String> serviceRequestPayload = new ArrayList<>();
    private ArrayList<List<Pair<String,String>>> serviceRequestHeaders = new ArrayList<>();
    private ArrayList<String> serviceResponsePayload = new ArrayList<>();
    private ArrayList<List<Pair<String,String>>> serviceResponseHeaders = new ArrayList<>();

    /**
     * Get mock services count.
     *
     * @return mock services count in descriptor data
     */
    public int getMockServicesCount() {
        return this.mockServicesCount;
    }

    /**
     * Get mock services index from the stored service name map.
     *
     * @param serviceName service name
     * @return mock services stored index
     */
    public int getServiceNameIndex(String serviceName) {
        return serviceNameMap.get(serviceName);
    }

    /**
     * Get mock service port.
     *
     * @param elementIndex service stored index
     * @return mock service port as in descriptor data
     */
    public int getServicePort(int elementIndex) {
        return servicePort.get(elementIndex);
    }

    /**
     * Get mock service context.
     *
     * @param elementIndex service stored index
     * @return mock service context as in descriptor data
     */
    public String getServiceContext(int elementIndex) {
        return serviceContext.get(elementIndex);
    }

    /**
     * Get mock service type.
     *
     * @param elementIndex service stored index
     * @return mock service type as in descriptor data
     */
    public String getServiceType(int elementIndex) {
        return serviceType.get(elementIndex);
    }

    /**
     * Get mock service input payload.
     *
     * @param elementIndex service stored index
     * @return mock service input payload as in descriptor data
     */
    public String getServiceRequestPayload(int elementIndex) {
        return serviceRequestPayload.get(elementIndex);
    }

    /**
     * Get mock service response.
     *
     * @param elementIndex service stored index
     * @return mock service response as in descriptor data
     */
    public String getServiceResponsePayload(int elementIndex) {
        return serviceResponsePayload.get(elementIndex);
    }

    /**
     * Get mock service request headers.
     *
     * @param elementIndex service stored index
     * @return mock service request headers as in descriptor data
     */
    public List<Pair<String,String>> getServiceRequestHeaders(int elementIndex) {
        return serviceRequestHeaders.get(elementIndex);
    }

    /**
     * Get mock service response headers.
     *
     * @param elementIndex service stored index
     * @return mock service response headers as in descriptor data
     */
    public List<Pair<String,String>> getServiceResponseHeaders(int elementIndex) {
        return serviceResponseHeaders.get(elementIndex);
    }

    /**
     * Add mock service name inside the map.
     *
     * @param serviceName service name as key
     * @param indexValue  storing index as value
     */
    public void addServiceName(String serviceName, int indexValue) {
        serviceNameMap.put(serviceName, indexValue);
    }

    /**
     * Add mock service port inside the ArrayList.
     *
     * @param servicePort service port as in descriptor data
     */
    public void addServicePort(int servicePort) {
        this.servicePort.add(servicePort);
    }

    /**
     * Add mock service path inside the ArrayList.
     *
     * @param serviceContext service path as in descriptor data
     */
    public void addServiceContext(String serviceContext) {
        this.serviceContext.add(serviceContext);
    }

    /**
     * Add mock service type inside the ArrayList.
     *
     * @param serviceType service type as in descriptor data
     */
    public void addServiceType(String serviceType) {
        this.serviceType.add(serviceType);
    }

    /**
     * Add mock service input payload inside the ArrayList.
     *
     * @param servicePayload service input payload as in descriptor data
     */
    public void addServiceRequestPayload(String servicePayload) {
        this.serviceRequestPayload.add(servicePayload);
    }

    /**
     * Add mock service response inside the ArrayList.
     *
     * @param serviceResponse service response as in descriptor data
     */
    public void addServiceResponsePayload(String serviceResponse) {
        this.serviceResponsePayload.add(serviceResponse);
    }

    /**
     * Set mock service count.
     *
     * @param mockServiceCount mock service count as in descriptor data
     */
    public void setMockServicesCount(int mockServiceCount) {
        this.mockServicesCount = mockServiceCount;
    }

    /**
     * Get mock service headers.
     *
     * @param requestHeader service request headers
     */
    public void addServiceRequestHeaders(ArrayList<Pair<String,String>> requestHeader) {
        serviceRequestHeaders.add(requestHeader);
    }

    /**
     * Get mock service headers.
     *
     * @param responseHeader service request headers
     */
    public void addServiceResponseHeaders(ArrayList<Pair<String,String>> responseHeader) {
        serviceResponseHeaders.add(responseHeader);
    }

    /**
     * Check is service name is exist inside the map.
     *
     * @param serviceName service name request for check
     * @return return true of value exist otherwise false
     */
    public boolean isServiceNameExist(String serviceName) {
        boolean isExist = false;

        if (serviceNameMap.containsKey(serviceName)) {
            isExist = true;
        }

        return isExist;
    }
}
