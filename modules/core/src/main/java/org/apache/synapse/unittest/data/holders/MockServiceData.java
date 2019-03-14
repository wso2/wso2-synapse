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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Mock service data holder class in unit test framework.
 */
public class MockServiceData extends Thread {

    private int mockServicesCount = 0;
    private Map<String, Integer> serviceNameMap = new HashMap<String, Integer>();
    private ArrayList<String> serviceHost = new ArrayList<String>();
    private ArrayList<Integer> servicePort = new ArrayList<Integer>();
    private ArrayList<String> servicePath = new ArrayList<String>();
    private ArrayList<String> serviceType = new ArrayList<String>();
    private ArrayList<String> servicePayload = new ArrayList<String>();
    private ArrayList<String> serviceResponse = new ArrayList<String>();

    /**
     * Get mock services count.
     *
     * @return mock services count in descriptor file
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
     * Get mock service host.
     *
     * @param elementIndex service stored index
     * @return mock service host as in descriptor file
     */
    public String getServiceHost(int elementIndex) {
        return serviceHost.get(elementIndex);
    }

    /**
     * Get mock service port.
     *
     * @param elementIndex service stored index
     * @return mock service port as in descriptor file
     */
    public int getServicePort(int elementIndex) {
        return servicePort.get(elementIndex);
    }

    /**
     * Get mock service path.
     *
     * @param elementIndex service stored index
     * @return mock service path as in descriptor file
     */
    public String getServicePath(int elementIndex) {
        return servicePath.get(elementIndex);
    }

    /**
     * Get mock service type.
     *
     * @param elementIndex service stored index
     * @return mock service type as in descriptor file
     */
    public String getServiceType(int elementIndex) {
        return serviceType.get(elementIndex);
    }

    /**
     * Get mock service input payload.
     *
     * @param elementIndex service stored index
     * @return mock service input payload as in descriptor file
     */
    public String getServicePayload(int elementIndex) {
        return servicePayload.get(elementIndex);
    }

    /**
     * Get mock service response.
     *
     * @param elementIndex service stored index
     * @return mock service response as in descriptor file
     */
    public String getServiceResponse(int elementIndex) {
        return serviceResponse.get(elementIndex);
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
     * Add mock service host inside the ArrayList.
     *
     * @param serviceHost service host as in descriptor file
     */
    public void addServiceHost(String serviceHost) {
        this.serviceHost.add(serviceHost);
    }

    /**
     * Add mock service port inside the ArrayList.
     *
     * @param servicePort service port as in descriptor file
     */
    public void addServicePort(int servicePort) {
        this.servicePort.add(servicePort);
    }

    /**
     * Add mock service path inside the ArrayList.
     *
     * @param servicePath service path as in descriptor file
     */
    public void addServicePath(String servicePath) {
        this.servicePath.add(servicePath);
    }

    /**
     * Add mock service type inside the ArrayList.
     *
     * @param serviceType service type as in descriptor file
     */
    public void addServiceType(String serviceType) {
        this.serviceType.add(serviceType);
    }

    /**
     * Add mock service input payload inside the ArrayList.
     *
     * @param servicePayload service input payload as in descriptor file
     */
    public void addServicePayload(String servicePayload) {
        this.servicePayload.add(servicePayload);
    }

    /**
     * Add mock service response inside the ArrayList.
     *
     * @param serviceResponse service response as in descriptor file
     */
    public void addServiceResponse(String serviceResponse) {
        this.serviceResponse.add(serviceResponse);
    }

    /**
     * Set mock service count.
     *
     * @param mockServiceCount mock service count as in descriptor file
     */
    public void setMockServicesCount(int mockServiceCount) {
        this.mockServicesCount = mockServiceCount;
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
