package org.apache.synapse.transport.nhttp.util.dynamicconfigurations;

import org.apache.axis2.description.ParameterInclude;
import org.apache.synapse.transport.nhttp.util.dynamicconfigurations.DynamicProfileReloader;

/**
 * Profile re-loader for nhttp and pass-through SSL senders
 */
public class SenderProfileReloader extends DynamicProfileReloader {

    SSLProfileLoader sslProfileLoader;
    ParameterInclude transportOutDescription;

    public SenderProfileReloader(SSLProfileLoader profileLoader, ParameterInclude transport) {

        this.sslProfileLoader = profileLoader;
        this.transportOutDescription = transport;
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
        sslProfileLoader.reloadConfig(sslProfileLoader, transportOutDescription);
    }

    /**
     * Register this profile loader in notifier
     */
    private void registerListener(){
        FileUpdateNotificationHandler.registerListener(this);
    }


}

