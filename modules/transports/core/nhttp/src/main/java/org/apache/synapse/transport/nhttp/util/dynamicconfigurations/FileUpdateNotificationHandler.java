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
public class FileUpdateNotificationHandler extends TimerTask{

    private Log log = LogFactory.getLog(this.getClass());

    private long fileReadInterval;

    private List<DynamicProfileReloader> profileReloaders;

    /**
     * Constructor with file read interval as the input
     *
     * @param sleepInterval long value in milliseconds
     */
    public FileUpdateNotificationHandler(long sleepInterval) {

        fileReadInterval = NhttpConstants.DYNAMIC_PROFILE_RELOAD_DEFAULT_INTERVAL;
        //Cannot configure a value less than the minimum interval
        if (sleepInterval > NhttpConstants.DYNAMIC_PROFILE_RELOAD_MIN_INTERVAL) {
            this.fileReadInterval = sleepInterval;
        }
        profileReloaders = new ArrayList<DynamicProfileReloader>();
        scheduleTimer(fileReadInterval);
    }

    @Override
    public void run(){

        long recordedLastUpdatedTime;
        long latestLastUpdatedTime;
        File configFile;

        for (DynamicProfileReloader profileLoader : profileReloaders) {

            recordedLastUpdatedTime = profileLoader.getLastUpdatedtime();
            String filePath = profileLoader.getFilePath();

            if (filePath != null) {
                configFile = new File(filePath);

                try {
                    latestLastUpdatedTime = configFile.lastModified();

                    if (latestLastUpdatedTime > recordedLastUpdatedTime) {
                        //Notify file update to the respective file loader
                        profileLoader.notifyFileUpdate();
                        profileLoader.setLastUpdatedtime(latestLastUpdatedTime);
                    }
                } catch (Exception e) {
                    log.debug("Error loading last modified time for the SSL config file. Updates will not be loaded from " + filePath);
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
        timer.scheduleAtFixedRate(this, 0, interval);
    }
}
