/*
*  Licensed to the Apache Software Foundation (ASF) under one
*  or more contributor license agreements.  See the NOTICE file
*  distributed with this work for additional information
*  regarding copyright ownership.  The ASF licenses this file
*  to you under the Apache License, Version 2.0 (the
*  "License"); you may not use this file except in compliance
*  with the License.  You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/

package org.apache.synapse.transport.passthru.jmx;

import org.apache.synapse.commons.jmx.MBeanRegistrar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * <p>LatencyView provides statistical information related to the latency (overhead) incurred by
 * the Synapse NHTTP transport, when mediating messages back and forth. Statistics are available
 * under two main categories, namely short term data and long term data. Short term data is
 * statistical information related to the last 15 minutes of execution and these metrics are
 * updated every 5 seconds. Long term data is related to the last 24 hours of execution and
 * they are updated every 5 minutes. Two timer tasks and a single threaded scheduled executor
 * is used to perform these periodic calculations.</p>
 *
 * <p>Latency calculation for a single invocation is carried out by taking timestamps on
 * following events:</p>
 *
 * <ul>
 *  <li>t1 - Receiving a new request (ServerHandler#requestReceived)</li>
 *  <li>t2 - Obtaining a connection to forward the request (ClientHandler#processConnection)</li>
 *  <li>t3 - Reading the complete response from the backend server (ClientHandler#inputReady)</li>
 *  <li>t4 - Writing the complete response to the client (ServerHandler#outputReady)</li>
 * <ul>
 *
 * <p>Having taken these timestamps, the latency for the invocation is calculated as follows:<br/>
 *    Latency = (t4 - t1) - (t3 - t2)
 * </p>
 *
 */
public class LatencyView implements LatencyViewMBean {

    private static final int SMALL_DATA_COLLECTION_PERIOD = 5;
    private static final int LARGE_DATA_COLLECTION_PERIOD = 5 * 60;

    /** Keeps track of th last reported latency value */
    private LatencyParameter lastLatency = new LatencyParameter(true);

    /** -Following are used to calculate BackEnd(Be) latency - */
    /** Keeps track of th last reported BE latency value */
    private LatencyParameter lastLatencyBe = new LatencyParameter(true);

    private LatencyParameter serverDecodeLatency;

    private LatencyParameter serverEncodeLatency;

    private LatencyParameter clientEncodeLatency;

    private LatencyParameter clientDecodeLatency;

    private LatencyParameter serverWorkerWaitTime;

    private LatencyParameter clientWorkerWaitTime;

    private LatencyParameter requestMediationLatency;

    private LatencyParameter responseMediationLatency;

    private List<LatencyParameter> latencies = new ArrayList<LatencyParameter>(10);

    /** Scheduled executor on which data collectors are executed */
    private ScheduledExecutorService scheduler;

    private Date resetTime = Calendar.getInstance().getTime();

    private String latencyMode;
    private String name;

    /**
     * Implementation of LatencyMbean
     * @param latencyMode S2S enabled or not
     * @param isHttps Is Secured
     */
    public LatencyView(final String latencyMode, boolean isHttps) {
        this(latencyMode, isHttps, "", false);
    }

    /**
     *  Implementation of LatencyMbean
     * @param latencyMode S2S enabled or not
     * @param isHttps Is Secured
     * @param namePostfix NamePostfix
     * @param showAdvancedParameters Enabling advanced latency capturing
     */
    public LatencyView(final String latencyMode, boolean isHttps, final String namePostfix, final boolean showAdvancedParameters) {
        this.latencyMode = latencyMode;
        name = "nio-http" + (isHttps ? "s" : "") + namePostfix;

        scheduler =  Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                return new Thread(r, latencyMode + "-" + name + "-latency-view");
            }
        });

        scheduler.scheduleAtFixedRate(new ShortTermDataCollector(), SMALL_DATA_COLLECTION_PERIOD,
                SMALL_DATA_COLLECTION_PERIOD, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(new LongTermDataCollector(), LARGE_DATA_COLLECTION_PERIOD,
                LARGE_DATA_COLLECTION_PERIOD, TimeUnit.SECONDS);
        boolean registered = false;
        try {
            registered = MBeanRegistrar.getInstance().registerMBean(this, this.latencyMode, name);
        } finally {
            if (!registered) {
                scheduler.shutdownNow();
            }
        }
        registerAllLatencies(showAdvancedParameters);
    }

    public void destroy() {
        MBeanRegistrar.getInstance().unRegisterMBean(latencyMode, name);
        if (!scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    /**
     * Report the timestamp values captured during mediating messages back and forth
     *
     * @param reqArrival The request arrival time
     * @param reqDeparture The request departure time (backend connection establishment)
     * @param resArrival The resoponse arrival time
     * @param resDeparture The response departure time
     */
    private void notifyTimes(long reqArrival, long reqDeparture,
                             long resArrival, long resDeparture) {
        long latencyBe = (resArrival - reqDeparture);
        long latency = (resDeparture - reqArrival) + latencyBe;
        lastLatency.update(latency);
        lastLatencyBe.update(latencyBe);
    }

    public void notifyTimes(LatencyCollector collector) {
        lastLatency.update(collector.getLatency());
        lastLatencyBe.update(collector.getBackendLatency());
        serverDecodeLatency.update(collector.getServerDecodeLatency());
        clientEncodeLatency.update(collector.getClientEncodeLatency());
        clientDecodeLatency.update(collector.getClientDecodeLatency());
        serverEncodeLatency.update(collector.getServerEncodeLatency());
        serverWorkerWaitTime.update(collector.getServerWorkerQueuedTime());
        clientWorkerWaitTime.update(collector.getClientWorkerQueuedTime());
        requestMediationLatency.update(collector.getServerWorkerLatency());
        responseMediationLatency.update(collector.getClientWorkerLatency());
    }

    private void registerAllLatencies(boolean recordAdditionalLatencies) {
        latencies.add(lastLatency);
        latencies.add(lastLatencyBe);
        serverDecodeLatency = new LatencyParameter(recordAdditionalLatencies);
        serverEncodeLatency = new LatencyParameter(recordAdditionalLatencies);
        clientEncodeLatency = new LatencyParameter(recordAdditionalLatencies);
        clientDecodeLatency = new LatencyParameter(recordAdditionalLatencies);
        serverWorkerWaitTime = new LatencyParameter(recordAdditionalLatencies);
        clientWorkerWaitTime = new LatencyParameter(recordAdditionalLatencies);
        requestMediationLatency = new LatencyParameter(recordAdditionalLatencies);
        responseMediationLatency = new LatencyParameter(recordAdditionalLatencies);
        latencies.add(serverDecodeLatency);
        latencies.add(clientEncodeLatency);
        latencies.add(clientDecodeLatency);
        latencies.add(serverEncodeLatency);
        latencies.add(serverWorkerWaitTime);
        latencies.add(clientWorkerWaitTime);
        latencies.add(requestMediationLatency);
        latencies.add(responseMediationLatency);
    }

    public double getAvg_Latency() {
        return lastLatency.getAllTimeAverage();
    }

    public double getAvg_Client_To_Esb_RequestReadTime() {
        return serverDecodeLatency.getAllTimeAverage();
    }

    public double getAvg_Esb_To_BackEnd_RequestWriteTime() {
        return clientEncodeLatency.getAllTimeAverage();
    }

    public double getAvg_BackEnd_To_Esb_ResponseReadTime() {
        return clientDecodeLatency.getAllTimeAverage();
    }

    public double getAvg_Esb_To_Client_ResponseWriteTime() {
        return serverEncodeLatency.getAllTimeAverage();
    }

    public double get1m_Avg_Client_To_Esb_RequestReadTime() {
        return serverDecodeLatency.getAverageLatency1m();
    }

    public double get1m_Avg_Esb_To_BackEnd_RequestWriteTime() {
        return clientEncodeLatency.getAverageLatency1m();
    }

    public double get1m_Avg_BackEnd_To_Esb_ResponseReadTime() {
        return clientDecodeLatency.getAverageLatency1m();
    }

    public double get1m_Avg_Esb_To_Client_ResponseWriteTime() {
        return serverEncodeLatency.getAverageLatency1m();
    }

    public double get5m_Avg_Client_To_Esb_RequestReadTime() {
        return serverDecodeLatency.getAverageLatency5m();
    }

    public double get5m_Avg_Esb_To_BackEnd_RequestWriteTime() {
        return clientEncodeLatency.getAverageLatency5m();
    }

    public double get5m_Avg_BackEnd_To_Esb_ResponseReadTime() {
        return clientDecodeLatency.getAverageLatency5m();
    }

    public double get5m_Avg_Esb_To_Client_ResponseWriteTime() {
        return serverEncodeLatency.getAverageLatency5m();
    }

    public double get15m_Avg_Client_To_Esb_RequestReadTime() {
        return serverDecodeLatency.getAverageLatency15m();
    }

    public double get15m_Avg_Esb_To_BackEnd_RequestWriteTime() {
        return clientEncodeLatency.getAverageLatency15m();
    }

    public double get15m_Avg_BackEnd_To_Esb_ResponseReadTime() {
        return clientDecodeLatency.getAverageLatency15m();
    }

    public double get15m_Avg_Esb_To_Client_ResponseWriteTime() {
        return serverEncodeLatency.getAverageLatency15m();
    }

    public double getAvg_ClientWorker_QueuedTime() {
        return clientWorkerWaitTime.getAllTimeAverage();
    }

    public double get1m_Avg_ClientWorker_QueuedTime() {
        return clientWorkerWaitTime.getAverageLatency1m();
    }

    public double get5m_Avg_ClientWorker_QueuedTime() {
        return clientWorkerWaitTime.getAverageLatency5m();
    }

    public double get15m_Avg_ClientWorker_QueuedTime() {
        return clientWorkerWaitTime.getAverageLatency15m();
    }

    public double getAvg_ServerWorker_QueuedTime() {
        return serverWorkerWaitTime.getAllTimeAverage();
    }

    public double get1m_Avg_ServerWorker_QueuedTime() {
        return serverWorkerWaitTime.getAverageLatency1m();
    }

    public double get5m_Avg_ServerWorker_QueuedTime() {
        return serverWorkerWaitTime.getAverageLatency5m();
    }

    public double get15m_Avg_ServerWorker_QueuedTime() {
        return serverWorkerWaitTime.getAverageLatency15m();
    }

    public double get1m_Avg_Latency() {
        return lastLatency.getAverageLatency1m();
    }

    public double get5m_Avg_Latency() {
        return lastLatency.getAverageLatency5m();
    }

    public double get15m_Avg_Latency() {
        return lastLatency.getAverageLatency15m();
    }

    public double get1h_Avg_Latency() {
        return lastLatency.getAverageLatency1h();
    }

    public double get8h_Avg_Latency() {
        return lastLatency.getAverageLatency8h();
    }

    public double get24h_Avg_Latency() {
        return lastLatency.getAverageLatency24h();
    }

    public double getAvg_Latency_BackEnd() {
        return lastLatencyBe.getAllTimeAverage();
    }

    public double get1m_Avg_Latency_BackEnd() {
        return lastLatencyBe.getAverageLatency1m();
    }

    public double get5m_Avg_Latency_BackEnd() {
        return lastLatencyBe.getAverageLatency5m();
    }

    public double get15m_Avg_Latency_BackEnd() {
        return lastLatencyBe.getAverageLatency15m();
    }

    public double get1h_Avg_Latency_BackEnd() {
        return lastLatencyBe.getAverageLatency1h();
    }

    public double get8h_Avg_Latency_BackEnd() {
        return lastLatencyBe.getAverageLatency8h();
    }

    public double get24h_Avg_Latency_BackEnd() {
        return lastLatencyBe.getAverageLatency24h();
    }

    public double get1h_Avg_Client_To_Esb_RequestReadTime() {
        return serverDecodeLatency.getAverageLatency1h();
    }

    public double get1h_Avg_Esb_To_BackEnd_RequestWriteTime() {
        return clientEncodeLatency.getAverageLatency1h();
    }

    public double get1h_Avg_BackEnd_To_Esb_ResponseReadTime() {
        return clientDecodeLatency.getAverageLatency1h();
    }

    public double get1h_Avg_Esb_To_Client_ResponseWriteTime() {
        return serverEncodeLatency.getAverageLatency1h();
    }

    public double get1h_Avg_ServerWorker_QueuedTime() {
        return serverWorkerWaitTime.getAverageLatency1h();
    }

    public double get1h_Avg_ClientWorker_QueuedTime() {
        return clientWorkerWaitTime.getAverageLatency1h();
    }

    public double get8h_Avg_Client_To_Esb_RequestReadTime() {
        return serverDecodeLatency.getAverageLatency8h();
    }

    public double get8h_Avg_Esb_To_BackEnd_RequestWriteTime() {
        return clientEncodeLatency.getAverageLatency8h();
    }

    public double get8h_Avg_BackEnd_To_Esb_ResponseReadTime() {
        return clientDecodeLatency.getAverageLatency8h();
    }

    public double get8h_Avg_Esb_To_Client_ResponseWriteTime() {
        return serverEncodeLatency.getAverageLatency8h();
    }

    public double get8h_Avg_ServerWorker_QueuedTime() {
        return serverWorkerWaitTime.getAverageLatency8h();
    }

    public double get8h_Avg_ClientWorker_QueuedTime() {
        return clientWorkerWaitTime.getAverageLatency8h();
    }

    public double get24h_Avg_Client_To_Esb_RequestReadTime() {
        return serverDecodeLatency.getAverageLatency24h();
    }

    public double get24h_Avg_Esb_To_BackEnd_RequestWriteTime() {
        return clientEncodeLatency.getAverageLatency24h();
    }

    public double get24h_Avg_BackEnd_To_Esb_ResponseReadTime() {
        return clientDecodeLatency.getAverageLatency24h();
    }

    public double get24h_Avg_Esb_To_Client_ResponseWriteTime() {
        return serverEncodeLatency.getAverageLatency24h();
    }

    public double get24h_Avg_ServerWorker_QueuedTime() {
        return serverWorkerWaitTime.getAverageLatency24h();
    }

    public double get24h_Avg_ClientWorker_QueuedTime() {
        return clientWorkerWaitTime.getAverageLatency24h();
    }

    public double getAvg_request_Mediation_Latency() {
        return requestMediationLatency.getAllTimeAverage();
    }

    public double getAvg_response_Mediation_Latency() {
        return responseMediationLatency.getAllTimeAverage();
    }

    public double get1m_Avg_request_Mediation_Latency() {
        return requestMediationLatency.getAverageLatency1m();
    }

    public double get1m_Avg_response_Mediation_Latency() {
        return responseMediationLatency.getAverageLatency1m();
    }

    public double get5m_Avg_request_Mediation_Latency() {
        return requestMediationLatency.getAverageLatency5m();
    }

    public double get5m_Avg_response_Mediation_Latency() {
        return responseMediationLatency.getAverageLatency5m();
    }

    public double get15m_Avg_request_Mediation_Latency() {
        return requestMediationLatency.getAverageLatency15m();
    }

    public double get15m_Avg_response_Mediation_Latency() {
        return responseMediationLatency.getAverageLatency15m();
    }

    public double get1h_Avg_request_Mediation_Latency() {
        return requestMediationLatency.getAverageLatency1h();
    }

    public double get1h_Avg_response_Mediation_Latency() {
        return responseMediationLatency.getAverageLatency1h();
    }

    public double get8h_Avg_request_Mediation_Latency() {
        return requestMediationLatency.getAverageLatency8h();
    }

    public double get8h_Avg_response_Mediation_Latency() {
        return responseMediationLatency.getAverageLatency8h();
    }

    public double get24h_Avg_request_Mediation_Latency() {
        return requestMediationLatency.getAverageLatency24h();
    }

    public double get24h_Avg_response_Mediation_Latency() {
        return responseMediationLatency.getAverageLatency24h();
    }

    public void reset() {
        for (LatencyParameter latency : latencies) {
            latency.reset();
        }
        resetTime = Calendar.getInstance().getTime();
    }

    public Date getLastResetTime() {
        return resetTime;
    }

    private class ShortTermDataCollector implements Runnable {
        public void run() {
            for (LatencyParameter latency : latencies) {
                latency.updateCache();
            }
        }
    }

    private class LongTermDataCollector implements Runnable {
        public void run() {
            for (LatencyParameter latency : latencies) {
                latency.updateLongTermCache();
            }
        }
    }
}
