package org.apache.synapse.transport.http.conn;

import java.util.Observable;

/**
 * Created by rajith on 3/31/16.
 */
public class SynapseDebugInfoHolder extends Observable {
    private boolean isDebugEnabled = false;
    private String wireLog;
    private static SynapseDebugInfoHolder debugInfoHolder;

    private SynapseDebugInfoHolder() {

    }

    public static synchronized SynapseDebugInfoHolder getInstance() {
        if (debugInfoHolder == null) {
            debugInfoHolder = new SynapseDebugInfoHolder();
        }
        return debugInfoHolder;
    }

    public void setDebugEnabled(boolean isDebugEnabled) {
        this.isDebugEnabled = isDebugEnabled;
    }

    public boolean isDebugEnabled() {
        return isDebugEnabled;
    }

    public String getWireLog() {
        return wireLog;
    }

    public void setWireLog(String wireLog) {
        this.wireLog = wireLog;
        setChanged();
        notifyObservers();
    }

//    private void fireEvent() {
//        setChanged();
//    }
}
