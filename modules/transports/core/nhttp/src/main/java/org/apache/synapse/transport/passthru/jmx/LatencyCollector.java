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


import org.apache.http.protocol.HttpContext;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.transport.passthru.PassThroughConstants;

public class LatencyCollector {

    private long backendLatency = 0;
    private long latency = 0;

    private long serverDecodeLatency = 0;

    private long clientEncodeLatency = 0;

    private long clientDecodeLatency = 0;

    private long serverEncodeLatency = 0;

    private long serverWorkerQueuedTime = 0;

    private long clientWorkerQueuedTime = 0;

    private long serverWorkerLatency = 0;

    private long clientWorkerLatency = 0;

    public LatencyCollector(HttpContext context, boolean isS2S) {
        Object o1, o2, o3, o4;
        o1 = context.getAttribute(PassThroughConstants.REQ_ARRIVAL_TIME);
        o2 = context.getAttribute(PassThroughConstants.REQ_DEPARTURE_TIME);
        if (isS2S) {
            o3 = context.getAttribute(PassThroughConstants.RES_HEADER_ARRIVAL_TIME);
            o4 = context.getAttribute(PassThroughConstants.RES_TO_CLIENT_WRITE_START_TIME);
        } else {
            o3 = context.getAttribute(PassThroughConstants.RES_ARRIVAL_TIME);
            o4 = context.getAttribute(PassThroughConstants.RES_DEPARTURE_TIME);
        }
        if (o1 != null && o2 != null && o3 != null && o4 != null) {
            long tReqArrival = (Long) o1;
            long tReqDeparture = (Long) o2;
            long tResArrival = (Long) o3;
            long tResDeparture = (Long) o4;

            backendLatency = (tResArrival - tReqDeparture);
            latency = (tResDeparture - tReqArrival) - backendLatency;
        }
        o1 = context.getAttribute(PassThroughConstants.REQ_FROM_CLIENT_READ_START_TIME);
        o2 = context.getAttribute(PassThroughConstants.REQ_FROM_CLIENT_READ_END_TIME);
        if (o1 != null && o2 != null) {
            serverDecodeLatency = (Long) o2 - (Long) o1;
        }
        o1 = context.getAttribute(PassThroughConstants.REQ_TO_BACKEND_WRITE_START_TIME);
        o2 = context.getAttribute(PassThroughConstants.REQ_TO_BACKEND_WRITE_END_TIME);
        if (o1 != null && o2 != null) {
            clientEncodeLatency = (Long) o2 - (Long) o1;
        }
        o1 = context.getAttribute(PassThroughConstants.RES_FROM_BACKEND_READ_START_TIME);
        o2 = context.getAttribute(PassThroughConstants.RES_FROM_BACKEND_READ_END_TIME);
        if (o1 != null && o2 != null) {
            clientDecodeLatency = (Long) o2 - (Long) o1;
        }
        o1 = context.getAttribute(PassThroughConstants.RES_TO_CLIENT_WRITE_START_TIME);
        o2 = context.getAttribute(PassThroughConstants.RES_TO_CLIENT_WRITE_END_TIME);
        if (o1 != null && o2 != null) {
            serverEncodeLatency = (Long) o2 - (Long) o1;
        }
        o1 = context.getAttribute(PassThroughConstants.SERVER_WORKER_START_TIME);
        o2 = context.getAttribute(PassThroughConstants.SERVER_WORKER_INIT_TIME);
        if (o1 != null && o2 != null) {
            serverWorkerQueuedTime = (Long) o1 - (Long) o2;
        }
        o1 = context.getAttribute(PassThroughConstants.CLIENT_WORKER_START_TIME);
        o2 = context.getAttribute(PassThroughConstants.CLIENT_WORKER_INIT_TIME);
        if (o1 != null && o2 != null) {
            clientWorkerQueuedTime = (Long) o1 - (Long) o2;
        }
        o1 = context.getAttribute(PassThroughConstants.REQ_TO_BACKEND_WRITE_START_TIME);
        o2 = context.getAttribute(PassThroughConstants.SERVER_WORKER_START_TIME);
        if (o1 != null && o2 != null) {
            serverWorkerLatency = (Long) o1 - (Long) o2;
        }
        o1 = context.getAttribute(PassThroughConstants.RES_TO_CLIENT_WRITE_START_TIME);
        o2 = context.getAttribute(PassThroughConstants.CLIENT_WORKER_START_TIME);
        if (o1 != null && o2 != null) {
            clientWorkerLatency = (Long) o1 - (Long) o2;
        }
    }

    public long getBackendLatency() {
        return backendLatency;
    }

    public long getLatency() {
        return latency;
    }

    public long getServerDecodeLatency() {
        return serverDecodeLatency;
    }

    public long getServerEncodeLatency() {
        return serverEncodeLatency;
    }

    public long getClientEncodeLatency() {
        return clientEncodeLatency;
    }

    public long getClientDecodeLatency() {
        return clientDecodeLatency;
    }

    public long getServerWorkerQueuedTime() {
        return serverWorkerQueuedTime;
    }

    public long getClientWorkerQueuedTime() {
        return clientWorkerQueuedTime;
    }

    public long getServerWorkerLatency() {
        return serverWorkerLatency;
    }

    public long getClientWorkerLatency() {
        return clientWorkerLatency;
    }

    public static void clearTimestamps(HttpContext context) {
        if (context == null) {
            return;
        }
        context.removeAttribute(NhttpConstants.REQ_ARRIVAL_TIME);
        context.removeAttribute(NhttpConstants.REQ_DEPARTURE_TIME);
        context.removeAttribute(NhttpConstants.RES_HEADER_ARRIVAL_TIME);
        context.removeAttribute(NhttpConstants.RES_TO_CLIENT_WRITE_START_TIME);
        context.removeAttribute(NhttpConstants.RES_ARRIVAL_TIME);
        context.removeAttribute(NhttpConstants.RES_DEPARTURE_TIME);
        context.removeAttribute(NhttpConstants.REQ_FROM_CLIENT_READ_START_TIME);
        context.removeAttribute(NhttpConstants.REQ_FROM_CLIENT_READ_END_TIME);
        context.removeAttribute(NhttpConstants.REQ_TO_BACKEND_WRITE_START_TIME);
        context.removeAttribute(NhttpConstants.REQ_TO_BACKEND_WRITE_END_TIME);
        context.removeAttribute(NhttpConstants.RES_FROM_BACKEND_READ_START_TIME);
        context.removeAttribute(NhttpConstants.RES_FROM_BACKEND_READ_END_TIME);
        context.removeAttribute(NhttpConstants.RES_TO_CLIENT_WRITE_END_TIME);
        context.removeAttribute(NhttpConstants.SERVER_WORKER_START_TIME);
        context.removeAttribute(NhttpConstants.SERVER_WORKER_INIT_TIME);
        context.removeAttribute(NhttpConstants.CLIENT_WORKER_START_TIME);
        context.removeAttribute(NhttpConstants.CLIENT_WORKER_INIT_TIME);
    }

}
