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

package org.apache.synapse.unittest.testcase.data.holders;

import org.apache.synapse.unittest.testcase.data.classes.MockService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Mock service data holder class in unit test framework.
 */
public class MockServiceData {

    private int mockServicesCount = 0;
    private Map<String, Integer> serviceNameMap = new HashMap<>();
    private ArrayList<MockService> mockServices = new ArrayList<>();

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
     * Get mock service from index of service.
     *
     * @param indexOfService service index
     * @return mock service
     */
    public MockService getMockServices(int indexOfService) {
        return mockServices.get(indexOfService);
    }

    /**
     * Set mock services index from the stored service name map.
     *
     * @param serviceName service name
     * @param serviceIndex service index
     */
    public void setServiceNameIndex(String serviceName, int serviceIndex) {
        serviceNameMap.put(serviceName, serviceIndex);
    }

    /**
     * Add mock service.
     *
     * @param service mock service
     */
    public void addMockServices(MockService service) {
        mockServices.add(service);
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
