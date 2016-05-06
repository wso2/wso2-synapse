package org.apache.synapse.transport.http.conn;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.Observable;

/**
 * This class is to hold debug information for wire level debug in transport level
 */
public class SynapseDebugInfoHolder extends Observable {
    private final Log log = LogFactory.getLog(SynapseDebugInfoHolder.class);

    public static final String SYNAPSE_WIRE_LOG_HOLDER_PROPERTY = "synapse.wire.log.holder";
    public static final String SYNAPSE_WIRE_LOG_MEDIATOR_ID_PROPERTY = "synapse.wire.log.mediator.id";
    public static final String DUMMY_MEDIATOR_ID = "{ \"dummyID\" : \"dummy\"}";
    private boolean isDebuggerEnabled = false;
    private static SynapseDebugInfoHolder debugInfoHolder;

    /**
     * Private constructor to make this singleton
     */
    private SynapseDebugInfoHolder() {

    }

    /**
     * get instance method which will return a single instance
     *
     * @return
     */
    public static synchronized SynapseDebugInfoHolder getInstance() {
        if (debugInfoHolder == null) {
            debugInfoHolder = new SynapseDebugInfoHolder();
        }
        return debugInfoHolder;
    }

    public void setDebuggerEnabled(boolean isDebuggerEnabled) {
        this.isDebuggerEnabled = isDebuggerEnabled;
    }

    public boolean isDebuggerEnabled() {
        return isDebuggerEnabled;
    }

    /**
     * This method will set the wirelog holder and will notify observers about that, so they can retrieve it
     *
     * @param wireLogHolder
     */
    public synchronized void setWireLogHolder(SynapseWireLogHolder wireLogHolder) {
        try {
            //cloning the wirelog holder so that normal flow can continue after event being fired
            SynapseWireLogHolder clonedHolder = wireLogHolder.deepClone();
            wireLogHolder.clear();
            setChanged();
            notifyObservers(clonedHolder);
        } catch (IOException e) {
            log.debug("Error cloning the wirelog holder object - " + e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            log.debug("Error cloning the wirelog holder object - " + e.getMessage(), e);
        }
    }
}
