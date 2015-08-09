/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.transport.dynamicconfigurations;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.ParameterInclude;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;

/**
 * Abstract class to use as Profile Reloader Subscribers. FileUpdateNotificationHandler will notify
 * this once the event is triggered.
 */
public abstract class DynamicProfileReloader {

    private static final Log log = LogFactory.getLog(DynamicProfileReloader.class);

    /* XML parameter name for dynamic profiles in Axis2 config */
    private final String PROFILE_CONFIG_NAME = "dynamicSSLProfilesConfig";

    /* XML element name for dynamic profiles configuration path in Axis2 config */
    private final String PATH_CONFIG_NAME = "filePath";

    /* XML element name for dynamic profiles file read interval in Axis2 config */
    private final String INTERVAL_CONFIG_NAME = "fileReadInterval";

    private boolean invokedFromSchedule = true;

    private long lastUpdatedtime;

    private String filePath;

    public abstract void notifyFileUpdate(boolean isScheduled);

    protected FileUpdateNotificationHandler fileUpdateNotificationHandler;

    /**
     * Returns Last Updated Time of the File
     * @return Long time in milliseconds
     */
    public long getLastUpdatedtime() {
        return this.lastUpdatedtime;
    }

    /**
     * Returns File Path of the dynamic SSL profiles
     *
     * @return String file path
     */
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
     * Check whether the file is invoked from scheduled task
     *
     * @return true if invoked from schedule, false otherwise
     */
    public boolean isInvokedFromSchedule() {
        return invokedFromSchedule;
    }

    /**
     * Set whether the file is invoked from schedule
     *
     * @param invokedFromSchedule true if invoked from schedule, false otherwise
     */
    public void setInvokedFromSchedule(boolean invokedFromSchedule) {
        this.invokedFromSchedule = invokedFromSchedule;
    }

    /**
     * Set the configuration file path from custom SSL config
     *
     * @param transportOut TransportOutDescription of the configuration
     * @return File Path String
     */
    protected String extractConfigurationFilePath(ParameterInclude transportOut) {
        String path = null;
        Parameter profileParam = transportOut.getParameter(PROFILE_CONFIG_NAME);
        //No Separate configuration file configured. Therefore using Axis2 Configuration
        if (profileParam != null) {
            OMElement profileParamElem = profileParam.getParameterElement();
            path = profileParamElem.getFirstChildWithName(new QName(PATH_CONFIG_NAME)).getText();

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
        long fileReadInterval = -1;
        Parameter profileParam = transportOut.getParameter(PROFILE_CONFIG_NAME);
        //No Separate configuration file configured. Therefore using Axis2 Configuration
        if (profileParam != null) {
            OMElement profileParamElem = profileParam.getParameterElement();
            String interval = profileParamElem.getFirstChildWithName(new QName(INTERVAL_CONFIG_NAME)).getText();
            if (interval != null) {
                fileReadInterval = Long.parseLong(interval);
            }
        }
        return fileReadInterval;
    }

    /**
     * Register this Profile Loader in FileUpdateNotificationHandler for notifications
     *
     * @param transportDescription Transport In/Out Description of the configuration
     */
    protected boolean registerListener(ParameterInclude transportDescription) {
        boolean notificationHandlerStarted = false;
        long configurationLoadingInterval = extractSleepInterval(transportDescription);
        String filePath = extractConfigurationFilePath(transportDescription);
        //Create File Update Notification Handler only if file path is configured
        if (filePath != null) {
            fileUpdateNotificationHandler = new FileUpdateNotificationHandler(configurationLoadingInterval);
            setFilePath(filePath);
            setLastUpdatedtime(System.currentTimeMillis());

            fileUpdateNotificationHandler.registerListener(this);
            notificationHandlerStarted = true;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Configuration File path is not configured and SSL Profiles will not be loaded " +
                          "dynamically in " + this.getClass().getName());
            }
        }
        return notificationHandlerStarted;
    }

    /**
     * Get actual class name from comprehensive class name
     *
     * @param completeClassName complete class name String
     * @return instance name String
     */
    public String getClassName(String completeClassName) {
        String absoluteClassName = null;
        if (completeClassName != null) {
            absoluteClassName = completeClassName.substring(completeClassName.lastIndexOf(".") + 1);
        }
        return absoluteClassName;
    }
}
