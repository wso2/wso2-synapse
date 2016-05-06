package org.apache.synapse.transport.http.conn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class is used to store wirelog information for all the relevant mediators and request wirelog, response wirelog as well
 */
public class SynapseWireLogHolder implements Serializable {
    private String proxyName = null;
    private String apiName = null;
    private String resourceUrlString = null;
    private StringBuilder requestWireLog;
    private StringBuilder responseWireLog;

    //key mediatorID
    private Map<String, SynapseBackEndWireLogs> backEndRequestResponse = new HashMap<>(1);

    private PHASE phase;

    public SynapseWireLogHolder() {
        this.phase = PHASE.SOURCE_REQUEST_READY;
        requestWireLog = new StringBuilder();
        responseWireLog = new StringBuilder();
    }

    public String getProxyName() {
        return proxyName;
    }

    public void setProxyName(String proxyName) {
        this.proxyName = proxyName;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getResourceUrlString() {
        return resourceUrlString;
    }

    public void setResourceUrlString(String resourceUrlString) {
        this.resourceUrlString = resourceUrlString;
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

    public PHASE getPhase() {
        return phase;
    }

    public synchronized void setPhase(PHASE phase) {
        this.phase = phase;
    }

    public synchronized void appendBackEndWireLog(RequestType type, String wireLog, String mediatorId) {
        SynapseBackEndWireLogs backEndWireLogs = backEndRequestResponse.get(mediatorId);
        if (backEndWireLogs == null) {
            backEndWireLogs = new SynapseBackEndWireLogs();
            backEndWireLogs.setMediatorID(mediatorId);
        }
        if (type.equals(RequestType.REQUEST)) {
            backEndWireLogs.appendRequestWireLog(wireLog);
        } else if (type.equals(RequestType.RESPONSE)) {
            backEndWireLogs.appendResponseWireLog(wireLog);
        }
        backEndRequestResponse.put(mediatorId, backEndWireLogs);
    }

    public Map<String, SynapseBackEndWireLogs> getBackEndRequestResponse() {
        return backEndRequestResponse;
    }

    public synchronized void clear() {
        this.requestWireLog.setLength(0);
        this.responseWireLog.setLength(0);
        this.backEndRequestResponse.clear();
        this.phase = PHASE.SOURCE_REQUEST_READY;
    }

    /**
     * These are the phases to determine request responses in wire level
     */
    public enum PHASE {
        SOURCE_REQUEST_READY, //source request gets hit the ESB
        SOURCE_REQUEST_DONE, //source request read done
        TARGET_REQUEST_READY, //back end request ready to be sent to back end
        TARGET_REQUEST_DONE, //back end request sent
        TARGET_RESPONSE_READY, //back end response received to esb
        TARGET_RESPONSE_DONE, //back end response read
        SOURCE_RESPONSE_READY, //source response ready to be sent to client
        SOURCE_RESPONSE_DONE //source response sent
    }

    public enum RequestType {
        REQUEST,
        RESPONSE
    }

    /**
     * This method is to clone wirelog holder object
     *
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public SynapseWireLogHolder deepClone() throws IOException, ClassNotFoundException {
        ObjectOutputStream wireLogHolderOutputStream = null;
        ObjectInputStream wireLogHolderInputStream = null;
        try {
            ByteArrayOutputStream binaryOutputStream =
                    new ByteArrayOutputStream();
            wireLogHolderOutputStream = new ObjectOutputStream(binaryOutputStream);
            wireLogHolderOutputStream.writeObject(this);
            wireLogHolderOutputStream.flush();
            ByteArrayInputStream binaryInputStream =
                    new ByteArrayInputStream(binaryOutputStream.toByteArray());
            wireLogHolderInputStream = new ObjectInputStream(binaryInputStream);

            return (SynapseWireLogHolder) wireLogHolderInputStream.readObject();
        } finally {
            wireLogHolderOutputStream.close();
            wireLogHolderInputStream.close();
        }
    }
}
