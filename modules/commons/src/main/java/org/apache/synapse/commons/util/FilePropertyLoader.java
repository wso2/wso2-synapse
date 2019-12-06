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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.SynapseCommonsException;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * File Property loader can be used to load the file property variables.
 */
public class FilePropertyLoader {

    private static final Log LOG = LogFactory.getLog(FilePropertyLoader.class);
    private static final String CONF_LOCATION = "conf.location";
    public static final String FILE_PROPERTY_PATH = "properties.file.path";
    private static final String DEFAULT_PROPERTY_FILE = "file.properties";
    private Map propertyMap;

    private static FilePropertyLoader fileLoaderInstance;

    public static FilePropertyLoader getInstance() {
        if ( null == fileLoaderInstance) {
            fileLoaderInstance = new FilePropertyLoader();
            fileLoaderInstance.loadPropertiesFile();
        }
        return fileLoaderInstance;
    }

    public String getValue(String input) {
        return (String) propertyMap.get(input);
    }

    private void loadPropertiesFile() throws SynapseCommonsException {

        String filePath = System.getProperty(FILE_PROPERTY_PATH);

        if ( null == filePath || filePath.isEmpty()) {
            throw new SynapseCommonsException(FILE_PROPERTY_PATH + " is empty or null");
        }
        if (("default").equals(filePath)) {
            filePath = System.getProperty(CONF_LOCATION) + File.separator + DEFAULT_PROPERTY_FILE;
        }

        File file = new File(filePath);
        boolean isFileExists = file.exists();

        if (isFileExists) {
            try (InputStream in = new FileInputStream(filePath)) {
                Properties rawProps = new Properties();
                propertyMap = new HashMap();
                rawProps.load(in);
                for (Map.Entry<Object, Object> propertyEntry : rawProps.entrySet()) {
                    String strValue = (String) propertyEntry.getValue();
                    propertyMap.put(propertyEntry.getKey(), strValue);
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Loaded factory properties from " + filePath + ": " + propertyMap);
                }
            } catch (IOException ex) {
                throw new SynapseCommonsException("Failed to read " + filePath, ex);
            }
        } else {
            throw new SynapseCommonsException("File cannot found in " + filePath);
        }
    }
}
