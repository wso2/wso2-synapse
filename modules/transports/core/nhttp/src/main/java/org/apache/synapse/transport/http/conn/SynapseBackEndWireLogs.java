package org.apache.synapse.transport.http.conn;

import java.io.Serializable;

/**
 * This class holds wirelog information per mediator
 */
public class SynapseBackEndWireLogs implements Serializable {
    private String mediatorID;  //stringified json object of mediator id(this was the one which was sent by developer studio side when adding a breakpoint
    private StringBuilder requestWireLog;
    private StringBuilder responseWireLog;

    public SynapseBackEndWireLogs() {
        requestWireLog = new StringBuilder();
        responseWireLog = new StringBuilder();
    }

    public String getMediatorID() {
        return mediatorID;
    }

    public void setMediatorID(String mediatorID) {
        this.mediatorID = mediatorID;
    }

    public String getRequestWireLog() {
        return requestWireLog.toString();
    }

    public void appendRequestWireLog(String requestWireLog) {
        this.requestWireLog.append(requestWireLog);
    }

    public String getResponseWireLog() {
        return responseWireLog.toString();
    }

    public void appendResponseWireLog(String responseWireLog) {
        this.responseWireLog.append(responseWireLog);
    }
}
