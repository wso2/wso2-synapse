package org.apache.synapse.transport.http.conn;

/**
 * Created by rajith on 4/7/16.
 */
public class SynapseBackEndWireLogs {
    private String mediatorID;
    private String medComponent;
    private String mediatorKey;
    private int[] mediatorPosition;
    private String requestWireLog;
    private String responseWireLog;

    public String getMediatorID() {
        return mediatorID;
    }

    public void setMediatorID(String mediatorID) {
        this.mediatorID = mediatorID;
    }


    public String getMedComponent() {
        return medComponent;
    }

    public void setMedComponent(String medComponent) {
        this.medComponent = medComponent;
    }

    public String getMediatorKey() {
        return mediatorKey;
    }

    public void setMediatorKey(String mediatorKey) {
        this.mediatorKey = mediatorKey;
    }

    public int[] getMediatorPosition() {
        return mediatorPosition;
    }

    public void setMediatorPosition(int[] mediatorPosition) {
        this.mediatorPosition = mediatorPosition;
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
}
