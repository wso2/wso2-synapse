package org.apache.synapse.transport.nhttp.util;

import org.apache.http.protocol.HttpContext;
import org.apache.synapse.transport.nhttp.NhttpConstants;

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
        o1 = context.getAttribute(NhttpConstants.REQ_ARRIVAL_TIME);
        o2 = context.getAttribute(NhttpConstants.REQ_DEPARTURE_TIME);
        if (isS2S) {
            o3 = context.getAttribute(NhttpConstants.RES_HEADER_ARRIVAL_TIME);
            o4 = context.getAttribute(NhttpConstants.RES_TO_CLIENT_WRITE_START_TIME);
        } else {
            o3 = context.getAttribute(NhttpConstants.RES_ARRIVAL_TIME);
            o4 = context.getAttribute(NhttpConstants.RES_DEPARTURE_TIME);
        }
        if (o1 != null && o2 != null && o3 != null && o4 != null) {
            long tReqArrival = (Long) o1;
            long tReqDeparture = (Long) o2;
            long tResArrival = (Long) o3;
            long tResDeparture = (Long) o4;

            backendLatency = (tResArrival - tReqDeparture);
            latency = (tResDeparture - tReqArrival) - backendLatency;
        }
        o1 = context.getAttribute(NhttpConstants.REQ_FROM_CLIENT_READ_START_TIME);
        o2 = context.getAttribute(NhttpConstants.REQ_FROM_CLIENT_READ_END_TIME);
        long reqestFromClientReadEndTime = 0;
        if (o1 != null && o2 != null) {
            reqestFromClientReadEndTime = (Long) o2;
            serverDecodeLatency = reqestFromClientReadEndTime - (Long) o1;
        }
        o1 = context.getAttribute(NhttpConstants.REQ_TO_BACKEND_WRITE_START_TIME);
        o2 = context.getAttribute(NhttpConstants.REQ_TO_BACKEND_WRITE_END_TIME);
        if (o1 != null && o2 != null) {
           clientEncodeLatency = (Long) o2 - (Long) o1;
        }
        o1 = context.getAttribute(NhttpConstants.RES_FROM_BACKEND_READ_START_TIME);
        o2 = context.getAttribute(NhttpConstants.RES_FROM_BACKEND_READ_END_TIME);
        long responseFromBEReadEndTime = 0;
        if (o1 != null && o2 != null) {
            responseFromBEReadEndTime = (Long) o2;
            clientDecodeLatency = responseFromBEReadEndTime - (Long) o1;
        }
        o1 = context.getAttribute(NhttpConstants.RES_TO_CLIENT_WRITE_START_TIME);
        o2 = context.getAttribute(NhttpConstants.RES_TO_CLIENT_WRITE_END_TIME);
        if (o1 != null && o2 != null) {
            serverEncodeLatency = (Long) o2 - (Long) o1;
        }
        long serverWorkerStartTime = 0;
        o1 = context.getAttribute(NhttpConstants.SERVER_WORKER_START_TIME);
        o2 = context.getAttribute(NhttpConstants.SERVER_WORKER_INIT_TIME);
        if (o1 != null && o2 != null) {
            serverWorkerStartTime = (Long) o1;
            serverWorkerQueuedTime = serverWorkerStartTime - (Long) o2;
        }
        o1 = context.getAttribute(NhttpConstants.CLIENT_WORKER_START_TIME);
        o2 = context.getAttribute(NhttpConstants.CLIENT_WORKER_INIT_TIME);
        long clientWorkerStartTime = 0;
        if (o1 != null && o2 != null) {
            clientWorkerStartTime = (Long) o1;
            clientWorkerQueuedTime = clientWorkerStartTime - (Long) o2;
        }
        o1 = context.getAttribute(NhttpConstants.REQ_TO_BACKEND_WRITE_START_TIME);
        o2 = context.getAttribute(NhttpConstants.SERVER_WORKER_START_TIME);
        if (o1 != null && o2 != null) {
            serverWorkerLatency = (Long) o1 - (Long) o2;
        }
        o1 = context.getAttribute(NhttpConstants.RES_TO_CLIENT_WRITE_START_TIME);
        o2 = context.getAttribute(NhttpConstants.CLIENT_WORKER_START_TIME);
        if (o1 != null && o2 != null) {
            clientWorkerLatency = (Long) o1 - (Long) o2;
        }
        if (clientWorkerQueuedTime > 0) {
            if (responseFromBEReadEndTime > clientWorkerStartTime) {
                if (!isS2S) {
                    backendLatency = backendLatency - clientWorkerQueuedTime;
                }
                clientDecodeLatency = clientDecodeLatency - clientWorkerQueuedTime;
            }
        }
        if (serverWorkerQueuedTime > 0) {
            if (reqestFromClientReadEndTime > serverWorkerStartTime) {
                serverDecodeLatency = serverDecodeLatency - serverWorkerQueuedTime;
            }
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

