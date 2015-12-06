/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.transport.dynamicconfigurations.jmx;

import org.apache.synapse.transport.dynamicconfigurations.DynamicProfileReloader;

/**
 * SSL Profile Invoker which is used with JMX support for Dynamic Profile Re-loading
 */
public class SSLProfileInvoker implements SSLProfileInvokerMBean {

    private String configFilePath;
    private DynamicProfileReloader dynamicProfileReloader;

    public SSLProfileInvoker(DynamicProfileReloader dynamicProfileReloader) {
        this.dynamicProfileReloader = dynamicProfileReloader;
        this.configFilePath = dynamicProfileReloader.getFilePath();
    }

    /**
     * Invoke notify method of DynamicProfileReloader
     */
    public void notifyFileUpdate() {
        dynamicProfileReloader.notifyFileUpdate(false);
    }

    /**
     * Returns configured file path
     *
     * @return String file path
     */
    public String getConfigFilePath() {
        return configFilePath;
    }
}
