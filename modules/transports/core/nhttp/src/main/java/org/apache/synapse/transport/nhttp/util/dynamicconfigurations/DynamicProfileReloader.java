package org.apache.synapse.transport.nhttp.util.dynamicconfigurations;

/**
 * Abtract class to use as Profile Reloader Observers
 */
public abstract class DynamicProfileReloader {

    private long lastUpdatedtime;

    public abstract String getFilePath();

    public abstract void notifyFileUpdate();

    public long getLastUpdatedtime() {
        return lastUpdatedtime;
    }

    public void setLastUpdatedtime(long lastUpdatedtime) {
        this.lastUpdatedtime = lastUpdatedtime;
    }
}
