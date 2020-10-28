/**
 *  Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.transport.passthru.config;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.ParameterInclude;
import org.apache.axis2.transport.base.threads.WorkerPool;
import org.apache.axis2.transport.base.threads.WorkerPoolFactory;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.jmx.PassThroughTransportMetricsCollector;
import org.apache.synapse.transport.passthru.util.BufferFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class has common configurations for both sender and receiver.
 */
public abstract class BaseConfiguration {

    /**
     * Configurations given by axis2.xml
     */
    protected ParameterInclude parameters = null;

    /** The thread pool for executing the messages passing through */
    private WorkerPool workerPool = null;

    /** The Axis2 ConfigurationContext */
    protected ConfigurationContext configurationContext = null;

    /** Default http parameters */
    protected HttpParams httpParams = null;

    protected IOReactorConfig ioReactorConfig = null;

    protected BufferFactory bufferFactory = null;

    /** Weather User-Agent header coming from client should be preserved */
    protected boolean preserveUserAgentHeader = false;
    /** Weather Server header coming from server should be preserved */
    protected boolean preserveServerHeader = true;
    /** Http headers which should be preserved */
    protected List<String> preserveHttpHeaders;


    private PassThroughTransportMetricsCollector metrics = null;

    private int iOBufferSize;

    protected PassThroughConfiguration conf = PassThroughConfiguration.getInstance();

    private Boolean correlationLoggingEnabled = false;

    private static final String PASSTHROUGH_THREAD_GROUP = "Pass-through Message Processing Thread Group";
    private static final String PASSTHROUGH_THREAD_ID ="PassThroughMessageProcessor";

    private Integer socketTimeout = null;
    private Integer connectionTimeout = null;

    public BaseConfiguration(ConfigurationContext configurationContext,
                             ParameterInclude parameters,
                             WorkerPool workerPool,
                             PassThroughTransportMetricsCollector metrics) {
        this.parameters = parameters;
        this.workerPool = workerPool;
        this.configurationContext = configurationContext;
        this.metrics = metrics;
    }

    public void build() throws AxisFault {
        iOBufferSize = conf.getIOBufferSize();

        if (workerPool == null) {
            workerPool = WorkerPoolFactory.getWorkerPool(
                            conf.getWorkerPoolCoreSize(),
                            conf.getWorkerPoolMaxSize(),
                            conf.getWorkerThreadKeepaliveSec(),
                            conf.getWorkerPoolQueueLen(),
                            PASSTHROUGH_THREAD_GROUP,
                            PASSTHROUGH_THREAD_ID);
        }

        httpParams = buildHttpParams();
        ioReactorConfig = buildIOReactorConfig();
        String sysCorrelationStatus = System.getProperty(PassThroughConstants.CORRELATION_LOGS_SYS_PROPERTY);
        if (sysCorrelationStatus != null) {
            correlationLoggingEnabled = sysCorrelationStatus.equalsIgnoreCase("true");
        }

        bufferFactory = new BufferFactory(iOBufferSize, new HeapByteBufferAllocator(), 512);
    }


    public WorkerPool getWorkerPool(int workerPoolCoreSize, int workerPoolMaxSize,
                                    int workerThreadKeepaliveSec, int workerPoolQueuLen,
                                    String threadGroupName, String threadgroupID) {
        if (threadGroupName == null) {
            threadGroupName = PASSTHROUGH_THREAD_GROUP;
        }
        if (threadgroupID == null) {
            threadgroupID = PASSTHROUGH_THREAD_ID;
        }
        if (workerPoolCoreSize == 0) {
            workerPoolCoreSize = conf.getWorkerPoolCoreSize();
        }
        if (workerPoolMaxSize == 0) {
            workerPoolMaxSize = conf.getWorkerPoolMaxSize();
        }
        if (workerThreadKeepaliveSec == 0) {
            workerThreadKeepaliveSec = conf.getWorkerThreadKeepaliveSec();
        }
        if (workerPoolQueuLen == 0) {
            workerPoolQueuLen = conf.getWorkerPoolQueueLen();
        }
        return WorkerPoolFactory.getWorkerPool(workerPoolCoreSize, workerPoolMaxSize,
                                               workerThreadKeepaliveSec, workerPoolQueuLen,
                                               threadGroupName, threadgroupID);
    }



    public int getIOBufferSize() {
        return iOBufferSize;
    }

    public WorkerPool getWorkerPool() {
        return workerPool;
    }

    public ConfigurationContext getConfigurationContext() {
        return configurationContext;
    }

    protected HttpParams buildHttpParams() {
        HttpParams params = new BasicHttpParams();
        params.
                setIntParameter(HttpConnectionParams.SO_TIMEOUT, getSocketTimeout()).
                setIntParameter(HttpConnectionParams.CONNECTION_TIMEOUT, getConnectionTimeout()).
                setIntParameter(HttpConnectionParams.SOCKET_BUFFER_SIZE,
                        conf.getIntProperty(HttpConnectionParams.SOCKET_BUFFER_SIZE, 8 * 1024)).
                setParameter(HttpProtocolParams.ORIGIN_SERVER,
                        conf.getStringProperty(HttpProtocolParams.ORIGIN_SERVER, "WSO2-PassThrough-HTTP")).
                setParameter(HttpProtocolParams.USER_AGENT,
                        conf.getStringProperty(HttpProtocolParams.USER_AGENT, "Synapse-PT-HttpComponents-NIO"));
//                setParameter(HttpProtocolParams.HTTP_ELEMENT_CHARSET,
//                        conf.getStringProperty(HttpProtocolParams.HTTP_ELEMENT_CHARSET, HTTP.DEFAULT_PROTOCOL_CHARSET));//TODO:This does not works with HTTPCore 4.3

        return params;
    }

    protected IOReactorConfig buildIOReactorConfig() {
        IOReactorConfig config = new IOReactorConfig();
        config.setIoThreadCount(conf.getIOThreadsPerReactor());
        config.setSoTimeout(getSocketTimeout());
        config.setConnectTimeout(getConnectionTimeout());
        config.setTcpNoDelay(conf.getBooleanProperty(HttpConnectionParams.TCP_NODELAY, true));
        config.setSoLinger(conf.getIntProperty(HttpConnectionParams.SO_LINGER, -1));
        config.setSoReuseAddress(conf.getBooleanProperty(HttpConnectionParams.SO_REUSEADDR, false));
        config.setInterestOpQueued(conf.getBooleanProperty("http.nio.interest-ops-queueing", false));
        config.setSelectInterval(conf.getIntProperty("http.nio.select-interval", 1000));
        return config;
    }
    
    public BufferFactory getBufferFactory() {
        return bufferFactory;
    }

    public PassThroughTransportMetricsCollector getMetrics() {
        return metrics;
    }

    public Boolean isCorrelationLoggingEnabled() { return correlationLoggingEnabled; }

    private Integer getSocketTimeout() {
        if (socketTimeout != null) {
            return socketTimeout;
        }
        socketTimeout = conf.getIntProperty(HttpConnectionParams.SO_TIMEOUT, 60000);
        return socketTimeout;
    }

    private Integer getConnectionTimeout() {
        if (connectionTimeout != null) {
            return connectionTimeout;
        }
        connectionTimeout = conf.getIntProperty(HttpConnectionParams.CONNECTION_TIMEOUT, getSocketTimeout() / 2);
        return connectionTimeout;
    }

    /**
     * Check preserving status of the given http header name
     *
     * @param headerName http header name which need to check preserving status
     * @return preserving status of the given http header
     */
    public boolean isPreserveHttpHeader(String headerName) {

        if (preserveHttpHeaders == null || preserveHttpHeaders.isEmpty() || headerName == null) {
            return false;
        }
        return preserveHttpHeaders.contains(headerName.toUpperCase());
    }

    public List<String> getPreserveHttpHeaders() {
        return preserveHttpHeaders;
    }

    /**
     * Populate preserve http headers from comma separate string
     *
     * @param preserveHeaders Comma separated preserve enable http headers
     */
    protected void populatePreserveHttpHeaders(String preserveHeaders) {

        preserveHttpHeaders = new ArrayList<String>();
        if (preserveHeaders != null && !preserveHeaders.isEmpty()) {
            String[] presHeaders = preserveHeaders.trim().toUpperCase().split(",");
            if (presHeaders != null && presHeaders.length > 0) {
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
}
