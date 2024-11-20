/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.synapse.commons.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.SynapseCommonsException;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * File Property loader can be used to load the file property variables.
 */
public class FilePropertyLoader {

    private static final Log LOG = LogFactory.getLog(FilePropertyLoader.class);
    private static final String CONF_LOCATION = "conf.location";
    private static final String FILE_PROPERTY_PATH = "properties.file.path";
    private static final String DEFAULT_PROPERTY_FILE = "file.properties";
    private static final String FILE_SYNC_INTERVAL = "file.properties.sync.interval";
    private static final String FILE_CANNOT_BE_FOUND_ERROR = "File cannot found in ";
    private String propertiesFilePath;
    private long lastModifiedTimestamp;
    private Map<String, String> propertyMap;

    private static FilePropertyLoader fileLoaderInstance;

    private FilePropertyLoader() {
        init();
        loadPropertiesFile();
        String fileSyncIntervalString = System.getProperty(FILE_SYNC_INTERVAL);
        if (StringUtils.isNotEmpty(fileSyncIntervalString)) {
            try {
                int syncInterval = Integer.parseInt(fileSyncIntervalString);
                if (syncInterval > 0) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("File syncing enabled with interval " + syncInterval);
                    }
                    Executors.newSingleThreadScheduledExecutor(
                            new ThreadFactoryBuilder().setNameFormat("FilePropertySyncTask-%d").build())
                            .scheduleAtFixedRate(
                                    () -> {
                                        try {
                                            loadPropertiesFile();
                                            if (LOG.isDebugEnabled()) {
                                                LOG.debug("File property sync task completed");
                                            }
                                        } catch (Exception e) {
                                            LOG.error("Error while syncing properties file ", e);
                                        }
                                    }, syncInterval, syncInterval, TimeUnit.SECONDS);
                }
            } catch (NumberFormatException e) {
                LOG.warn("Dropping system property " + FILE_SYNC_INTERVAL + " with incorrect value specified. File "
                         + "property syncing will be disabled. ");
            }
        }
    }

    public static FilePropertyLoader getInstance() {
        if (Objects.nonNull(fileLoaderInstance)) {
            return fileLoaderInstance;
        }
        synchronized (FilePropertyLoader.class) {
            if (Objects.isNull(fileLoaderInstance)) {
                fileLoaderInstance = new FilePropertyLoader();
            }
            return fileLoaderInstance;
        }
    }

    public String getValue(String input) {
        return propertyMap.get(input);
    }

    private void loadPropertiesFile() throws SynapseCommonsException {
        File file = new File(propertiesFilePath);
        if (file.exists()) {
            if (file.lastModified() > lastModifiedTimestamp) {
                try (InputStream in = Files.newInputStream(Paths.get(propertiesFilePath))) {
                    Properties rawProps = new Properties();
                    Map<String, String> tempPropertyMap = new HashMap<>();
                    rawProps.load(in);
                    for (Map.Entry<Object, Object> propertyEntry : rawProps.entrySet()) {
                        String strValue = (String) propertyEntry.getValue();
                        tempPropertyMap.put((String) propertyEntry.getKey(), strValue);
                    }
                    propertyMap = tempPropertyMap;
                    lastModifiedTimestamp = file.lastModified();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Synced properties from " + propertiesFilePath);
                    }
                } catch (IOException ex) {
                    throw new SynapseCommonsException("Failed to read " + propertiesFilePath, ex);
                }
            }
        } else {
            throw new SynapseCommonsException(FILE_CANNOT_BE_FOUND_ERROR + propertiesFilePath);
        }
    }

    private void init() {
        String filePath = System.getProperty(FILE_PROPERTY_PATH);

        if (null == filePath || filePath.isEmpty()) {
            throw new SynapseCommonsException(FILE_PROPERTY_PATH + " is empty or null");
        }
        if (("default").equals(filePath)) {
            propertiesFilePath = System.getProperty(CONF_LOCATION) + File.separator + DEFAULT_PROPERTY_FILE;
        } else {
            propertiesFilePath = filePath;
        }

        File file = new File(propertiesFilePath);
        if (!file.exists()) {
            throw new SynapseCommonsException(FILE_CANNOT_BE_FOUND_ERROR + filePath);
        }
        propertyMap = new HashMap<>();
    }
}
