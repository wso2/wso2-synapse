/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.synapse.transport.netty.config;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.axis2.transport.base.threads.WorkerPoolFactory;
import org.apache.http.protocol.HTTP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * This class has common configurations for both sender and receiver.
 */
public abstract class BaseConfiguration {

    protected ConfigurationContext configurationContext;
    /**
     * Weather User-Agent header coming from client should be preserved.
     */
    protected boolean preserveUserAgentHeader = false;
    /**
     * Weather Server header coming from server should be preserved.
     */
    protected boolean preserveServerHeader = true;
    /**
     * Http headers which should be preserved.
     */
    protected List<String> preserveHttpHeaders;
    /**
     * The thread pool for executing the messages passing through.
     */
    private WorkerPool workerPool;

    NettyConfiguration conf = NettyConfiguration.getInstance();

    public BaseConfiguration(ConfigurationContext configurationContext) {

        this.configurationContext = configurationContext;
    }

    public void build() throws AxisFault {

        workerPool = WorkerPoolFactory.getWorkerPool(
                conf.getWorkerPoolCoreSize(),
                conf.getWorkerPoolMaxSize(),
                conf.getWorkerThreadKeepaliveSec(),
                conf.getWorkerPoolQueueLen(),
                NettyConfiguration.HTTP_WORKER_THREAD_GROUP_NAME,
                NettyConfiguration.HTTP_WORKER_THREAD_ID);
    }

    /**
     * Check whether to preserve or not the given HTTP header.
     *
     * @param headerName HTTP header name to be checked for preserving status
     * @return preserving status of the given HTTP header
     */
    public boolean isPreserveHttpHeader(String headerName) {

        if (Objects.isNull(headerName) || Objects.isNull(preserveHttpHeaders) || preserveHttpHeaders.isEmpty()) {
            return false;
        }
        return preserveHttpHeaders.contains(headerName.toUpperCase());
    }

    /**
     * Populate preserve HTTP headers from comma separate string.
     *
     * @param preserveHeaders Comma separated preserve enableD HTTP headers
     */
    protected void populatePreserveHTTPHeaders(String preserveHeaders) {

        preserveHttpHeaders = new ArrayList<>();
        if (Objects.nonNull(preserveHeaders) && !preserveHeaders.isEmpty()) {
            String[] presHeaders = preserveHeaders.trim().toUpperCase().split(",");
            if (presHeaders.length > 0) {
                preserveHttpHeaders.addAll(Arrays.asList(presHeaders));
            }
        }

        if (preserveServerHeader && !preserveHttpHeaders.contains(HTTP.SERVER_HEADER.toUpperCase())) {
            preserveHttpHeaders.add(HTTP.SERVER_HEADER.toUpperCase());
        }

        if (preserveUserAgentHeader && !preserveHttpHeaders.contains(HTTP.USER_AGENT.toUpperCase())) {
            preserveHttpHeaders.add(HTTP.USER_AGENT.toUpperCase());
        }
    }

    public List<String> getPreserveHttpHeaders() {

        return preserveHttpHeaders;
    }

    public WorkerPool getWorkerPool() {

        return workerPool;
    }

    public ConfigurationContext getConfigurationContext() {

        return configurationContext;
    }
}
