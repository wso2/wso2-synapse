package org.apache.synapse.transport.nhttp.util.dynamicconfigurations;

import org.apache.axis2.description.ParameterInclude;

/**
 * Profile re-loader for nhttp and pass-through SSL receivers
 */
public class ListenerProfileReloader extends DynamicProfileReloader {

    SSLProfileLoader sslProfileLoader;
    ParameterInclude transportInDescription;

    public ListenerProfileReloader(SSLProfileLoader profileLoader, ParameterInclude transport) {

        this.sslProfileLoader = profileLoader;
        this.transportInDescription = transport;
        registerListener();

    }

    /**
     * Get configuration file path from given transport description
     * @return String file path
     */
    public String getFilePath(){
        return null;
    }

    /**
     * Notification method triggers by FileUpdateNotifier
     */
    public void notifyFileUpdate(){
        sslProfileLoader.reloadConfig(sslProfileLoader, transportInDescription);
    }

    /**
     * Register this profile loader in notifier
     */
    private void registerListener(){
        FileUpdateNotificationHandler.registerListener(this);
    }



}