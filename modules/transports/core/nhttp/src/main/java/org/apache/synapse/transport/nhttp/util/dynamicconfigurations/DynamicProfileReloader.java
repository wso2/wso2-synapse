/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.transport.nhttp.util.dynamicconfigurations;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ParameterInclude;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.nhttp.NhttpConstants;

import javax.xml.namespace.QName;

/**
 * Abstract class to use as Profile Reloader Subscribers. FileUpdateNotificationHandler will notify
 * this once the event is triggered.
 */
public abstract class DynamicProfileReloader {

    private static final Log LOG = LogFactory.getLog(DynamicProfileReloader.class);

    private long lastUpdatedtime;

    private long fileMonitoringInterval = NhttpConstants.DYNAMIC_PROFILE_RELOAD_DEFAULT_INTERVAL;

    private String filePath;

    protected abstract void notifyFileUpdate();

    protected FileUpdateNotificationHandler fileUpdateNotificationHandler;

    /**
     * Returns Last Updated Time of the File
     * @return Long time in milliseconds
     */
    public long getLastUpdatedtime() {
        return this.lastUpdatedtime;
    }

    public String getFilePath() {
        return this.filePath;
    }

    /**
     * Set file path
     *
     * @param filePath String file path
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Set Last Updated time of the file
     *
     * @param lastUpdatedtime Long time in milliseconds
     */
    public void setLastUpdatedtime(long lastUpdatedtime) {
        this.lastUpdatedtime = lastUpdatedtime;
    }

    /**
     * Set the configuration file path from custom SSL config
     *
     * @param transportOut TransportOutDescription of the configuration
     * @return File Path String
     */
    protected String extractConfigurationFilePath(ParameterInclude transportOut) {
        String path = null;
        Parameter profilePathParam = transportOut.getParameter("SSLProfilesConfigPath");

        if (profilePathParam != null) {
            OMElement pathElement = profilePathParam.getParameterElement();
            path = pathElement.getFirstChildWithName(new QName("filePath")).getText();
        }
        return path;
    }

    /**
     * Set SSL Profile configuration loading interval from Axis2 config
     *
     * @param transportOut TransportOutDescription of the configuration
     * @return Long value of the interval in milliseconds
     */
    protected long extractSleepInterval(ParameterInclude transportOut) {
        Parameter profilePathParam = transportOut.getParameter("sslprofilesloadinginterval");

        if (profilePathParam != null) {
            fileMonitoringInterval = Long.parseLong(profilePathParam.getValue().toString());
        }
        return fileMonitoringInterval;
    }


    /**
     * Register this Profile Loader in FileUpdateNotificationHandler for notifications
     *
     * @param transportDescription Transport In/Out Description of the configuration
     */
    protected void registerListener(ParameterInclude transportDescription) {
        long configurationLoadingInterval = extractSleepInterval(transportDescription);
        String filePath = extractConfigurationFilePath(transportDescription);

        fileUpdateNotificationHandler = new FileUpdateNotificationHandler(configurationLoadingInterval);

        if (filePath != null) {
            setFilePath(filePath);
            setLastUpdatedtime(System.currentTimeMillis());
            fileUpdateNotificationHandler.registerListener(this);
        } else {
            LOG.debug("Configuration File path is not configured and SSL Profiles will not be loaded dynamically in " + this.getClass().getName());
        }
    }
}
