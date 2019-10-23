/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 *  File Property loader can be used to load the file property variables.
 */
public class FilePropertyLoader {

    private static final Log log = LogFactory.getLog(FilePropertyLoader.class);
    private static final String CONF_LOCATION = "conf.location";
    private static final String SYNAPSE_PROPERTY_FILE = "synapse.properties";
    private static final String FILE_PROPERTY_PATH = "synapse.commons.file.properties.location";
    private static final String FILE_PROPERTY_FILENAME = "synapse.commons.file.properties.file.name";
    private static FilePropertyLoader fileLoaderInstance = new FilePropertyLoader();
    private static Map fileProperty;
    private static String fileValue;

    private FilePropertyLoader(){}

    public static FilePropertyLoader getFileLoaderInstance(){
        return fileLoaderInstance;
    }

    public static Map getFileProperty() {
        return fileProperty;
    }

    public static void setFileProperty(Map fileProperty) {
        FilePropertyLoader.fileProperty = fileProperty;
    }

    public static String getFileValue() {
        return fileValue;
    }

    public static void setFileValue(String input) {
        FilePropertyLoader.fileValue = (String) getFileProperty().get(input);
    }

    public static void loadPropertiesFile(){
        try {
            fileProperty = readFileProperties();

            if (fileProperty != null) {
                setFileProperty(fileProperty);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static Map readFileProperties() throws FileNotFoundException {

        Properties properties = MiscellaneousUtil.loadProperties(SYNAPSE_PROPERTY_FILE);
        String filePath = properties.getProperty(FILE_PROPERTY_PATH);
        String fileName = properties.getProperty(FILE_PROPERTY_FILENAME);

        if(filePath.equals("default")){
            filePath = System.getProperty(CONF_LOCATION);
        }

        File fileDir = new File(filePath + File.separator + fileName);
        boolean isFileExists = fileDir.exists();

        if (isFileExists) {
            InputStream in = new FileInputStream(filePath + File.separator + fileName);

            if (in == null) {
                return null;
            } else {
                try {
                    Properties rawProps = new Properties();
                    Map props = new HashMap();
                    rawProps.load(in);
                    for (Iterator it = rawProps.entrySet().iterator(); it.hasNext(); ) {
                        Map.Entry entry = (Map.Entry)it.next();
                        String strValue = (String)entry.getValue();
                        Object value;
                        if (strValue.equals("true")) {
                            value = Boolean.TRUE;
                        } else if (strValue.equals("false")) {
                            value = Boolean.FALSE;
                        } else {
                            try {
                                value = Integer.valueOf(strValue);
                            } catch (NumberFormatException ex) {
                                value = strValue;
                            }
                        }
                        props.put(entry.getKey(), value);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Loaded factory properties from " + fileName + ": " + props);
                    }
                    return props;
                } catch (IOException ex) {
                    log.error("Failed to read " + fileName, ex);
                    return null;
                } finally {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        // Ignore
                    }
                }
            }
        } else {
            return null;
        }
    }
}
