package org.apache.synapse.transport.http.conn;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Created by rajith on 4/5/16.
 */
public class SynapseWireLogHolder {
    private String serviceName;
    private String requestWireLog;
    private String responseWireLog;

    private String mediatorId;
    //key mediatorID
    private Map<String, SynapseBackEndWireLogs> backEndRequestResponse = new HashMap<>(1);

    private PHASE phase;

    public SynapseWireLogHolder() {
        this.phase = PHASE.INIT;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getRequestWireLog() {
        return requestWireLog;
    }

    public void setRequestWireLog(String requestWireLog) {
        this.requestWireLog = requestWireLog;
    }

    public String getResponseWireLog() {
        return responseWireLog;
    }

    public void setResponseWireLog(String responseWireLog) {
        this.responseWireLog = responseWireLog;
    }

    public String getMediatorId() {
        return mediatorId;
    }

    public void setMediatorId(String mediatorId) {
        this.mediatorId = mediatorId;
    }

    public PHASE getPhase() {
        return phase;
    }

    public void setPhase(PHASE phase) {
        this.phase = phase;
    }

    public void insertBackEndWireLog(RequestType type, String wireLog) {
        SynapseBackEndWireLogs backEndWireLogs = backEndRequestResponse.get(mediatorId);
        if (backEndWireLogs == null) {
            backEndWireLogs = new SynapseBackEndWireLogs();
            backEndWireLogs.setMediatorID(mediatorId);
        }

        if (type.equals(RequestType.REQUEST)) {
            backEndWireLogs.setRequestWireLog(wireLog);
        } else if (type.equals(RequestType.RESPONSE)) {
            backEndWireLogs.setResponseWireLog(wireLog);
        }
        backEndRequestResponse.put(mediatorId, backEndWireLogs);
    }

    public Map<String, SynapseBackEndWireLogs> getBackEndRequestResponse() {
        return backEndRequestResponse;
    }

    public enum PHASE {
        INIT, //request get hit
        REQUEST_RECEIVED, //after the request read in wire
        REQUEST_READY, //back end request ready
        REQUEST_SENT, //back end request sent
        RESPONSE_RECEIVED, //back end response received
        RESPONSE_READY, //response ready for client
        DONE //finished
    }

    public enum RequestType {
        REQUEST,
        RESPONSE
    }
}
