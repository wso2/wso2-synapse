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

package org.apache.synapse.unittest.testcase.data.classes;

import java.util.List;

public class MockService {

    private int port;
    private String serviceName;
    private String context;
    private List<ServiceResource> resources;

    /**
     * Get mock service port.
     *
     * @return mock service port as in descriptor data
     */
    public int getPort() {
        return port;
    }

    /**
     * Get mock services name.
     *
     * @return mock services stored index
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Get mock service context.
     *
     * @return mock service context as in descriptor data
     */
    public String getContext() {
        return context;
    }

    /**
     * Get mock service resources.
     *
     * @return mock service resource objects as in descriptor data
     */
    public List<ServiceResource> getResources() {
        return resources;
    }

    /**
     * Add mock service port inside the ArrayList.
     *
     * @param port service port as in descriptor data
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Add mock service name inside the map.
     *
     * @param serviceName service name as key
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * Add mock service path inside the ArrayList.
     *
     * @param context service path as in descriptor data
     */
    public void setContext(String context) {
        this.context = context;
    }

    /**
     * Set mock service resources array.
     *
     * @param resources resources as in descriptor data
     */
    public void setResources(List<ServiceResource> resources) {
        this.resources = resources;
    }
}
