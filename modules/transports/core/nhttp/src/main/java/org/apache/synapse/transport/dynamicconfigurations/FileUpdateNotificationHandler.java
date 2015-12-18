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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.nhttp.NhttpConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Periodically checks on configuration file updates and notify respective listeners
 */
public class FileUpdateNotificationHandler extends TimerTask {

    private static final Log log = LogFactory.getLog(FileUpdateNotificationHandler.class);

    private long fileReadInterval = NhttpConstants.DYNAMIC_PROFILE_RELOAD_DEFAULT_INTERVAL;

    private List<DynamicProfileReloader> profileReloaders;

    /**
     * Constructor with file read interval as the input
     *
     * @param sleepInterval long value in milliseconds
     */
    public FileUpdateNotificationHandler(long sleepInterval) {

        //Cannot configure a value less than the minimum interval
        if (sleepInterval > fileReadInterval) {
            fileReadInterval = sleepInterval;
        }
        profileReloaders = new ArrayList<DynamicProfileReloader>();
        scheduleTimer(fileReadInterval);
    }

    @Override
    public void run() {
        long recordedLastUpdatedTime;
        long latestLastUpdatedTime;
        String filePath;
        File configFile;

        for (DynamicProfileReloader profileLoader : profileReloaders) {

            recordedLastUpdatedTime = profileLoader.getLastUpdatedtime();
            filePath = profileLoader.getFilePath();

            if (filePath != null) {
                if(!profileLoader.isInvokedFromSchedule()){
                    if(log.isDebugEnabled()) {
                        log.debug("Bypass the scheduled loading cycle of SSL profile since " +
                                  "already loaded from JMX invocation : file path - " + filePath);
                    }
                    profileLoader.setInvokedFromSchedule(true);
                    profileLoader.setLastUpdatedtime(System.currentTimeMillis());
                    continue;
                }

                configFile = new File(filePath);

                try {
                    latestLastUpdatedTime = configFile.lastModified();

                    if (latestLastUpdatedTime > recordedLastUpdatedTime) {
                        profileLoader.setLastUpdatedtime(latestLastUpdatedTime);
                        //Notify file update to the respective file loader
                        profileLoader.notifyFileUpdate(true);
                    }
                } catch (Exception e) {
                    if(log.isWarnEnabled()) {
                        log.warn("Error loading last modified time for the SSL config file. Updates " +
                                  "will not be loaded from " + filePath);
                    }
                }
            }
        }
    }

    /**
     * Register listeners for file update notifications
     *
     * @param dynamicProfileReloader Listener to be notified
     */
    public void registerListener(DynamicProfileReloader dynamicProfileReloader) {
        profileReloaders.add(dynamicProfileReloader);
    }

    /**
     * Schedule the handler with given interval to check files for changes
     *
     * @param interval Long time interval for the timer
     */
    private void scheduleTimer(long interval) {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(this, interval, interval);
    }
}
