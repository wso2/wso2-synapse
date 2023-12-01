/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.synapse.transport.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Properties;

public class ConfigurationBuilderUtil {

    private static final Log LOGGER = LogFactory.getLog(ConfigurationBuilderUtil.class);

    /**
     * Get an int property that tunes pass-through http transport. Prefer system properties
     *
     * @param name name of the system/config property
     * @param def  default value to return if the property is not set
     * @return the value of the property to be used
     */
    public static Integer getIntProperty(String name, Integer def, Properties props) {
        String val = System.getProperty(name);
        if (val == null) {
            val = props.getProperty(name);
        }

        if (val != null) {
            int intVal;
            try {
                intVal = Integer.valueOf(val);
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid pass-through http tuning property value. " + name +
                        " must be an integer");
                return def;
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Using pass-through http tuning parameter : " + name + " = " + val);
            }
            return intVal;
        }

        return def;
    }

    /**
     * Return true if user has configured an int property that tunes pass-through http transport.
     *
     * @param name  name of the system/config property
     * @return      true if property is configured
     */
    public static boolean isIntPropertyConfigured(String name, Properties props) {

        String val = System.getProperty(name);
        if (val == null) {
            val = props.getProperty(name);
        }

        if (val != null) {
            try {
                Integer.parseInt(val);
            } catch (NumberFormatException e) {
                LOGGER.warn("Incorrect pass-through http tuning property value. " + name +
                        " must be an integer");
                return false;
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Using configured pass-through http tuning property value for : " + name);
            }
            return true;
        }

        return false;
    }

    /**
     * Get an int property that tunes pass-through http transport. Prefer system properties
     *
     * @param name name of the system/config property
     * @return the value of the property, null if the property is not found
     */
    public static Integer getIntProperty(String name, Properties props) {
        return getIntProperty(name, null, props);
    }

    public static Long getLongProperty(String name, Long def, Properties props) {
        String val = System.getProperty(name);
        if (val == null) {
            val = props.getProperty(name);
        }

        if (val != null) {
            long longVal;
            try {
                longVal = Long.valueOf(val);
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid pass-through http tuning property value. " + name +
                        " must be an integer");
                return def;
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Using pass-through http tuning parameter : " + name + " = " + val);
            }
            return longVal;
        }

        return def;
    }

    /**
     * Get a boolean property that tunes pass-through http transport. Prefer system properties
     *
     * @param name name of the system/config property
     * @param def  default value to return if the property is not set
     * @return the value of the property to be used
     */
    public static Boolean getBooleanProperty(String name, Boolean def, Properties props) {
        String val = System.getProperty(name);
        if (val == null) {
            val = props.getProperty(name);
        }

        if (val != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Using pass-through http tuning parameter : " + name + " = " + val);
            }
            return Boolean.valueOf(val);
        }

        return def;
    }

    /**
     * Get a Boolean property that tunes pass-through http transport. Prefer system properties
     *
     * @param name name of the system/config property
     * @return the value of the property, null if the property is not found
     */
    public static Boolean getBooleanProperty(String name, Properties props) {
        return getBooleanProperty(name, null, props);
    }

    /**
     * Get a String property that tunes pass-through http transport. Prefer system properties
     *
     * @param name name of the system/config property
     * @param def  default value to return if the property is not set
     * @return the value of the property to be used
     */
    public static String getStringProperty(String name, String def, Properties props) {
        String val = System.getProperty(name);
        if (val == null) {
            val = props.getProperty(name);
        }

        return val == null ? def : val;
    }

    /**
     * Returns the keystore of the given file path.
     *
     * @param keyStoreFilePath the keystore file path
     * @param keyStorePassword the keystore password
     * @param keyStoreType the keystore type
     * @return KeyStore
     * @throws KeyStoreException On error while creating keystore
     */
    public static KeyStore getKeyStore(String keyStoreFilePath, String keyStorePassword, String keyStoreType)
            throws KeyStoreException {

        String file = new File(keyStoreFilePath).getAbsolutePath();
        try (FileInputStream keyStoreFileInputStream = new FileInputStream(file)) {
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(keyStoreFileInputStream, keyStorePassword.toCharArray());
            return keyStore;
        } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
            String errorMessage = String.format("Keystore file does not exist in the path as configured " +
                    "in '%s' property.", keyStoreFilePath);
            throw new KeyStoreException(errorMessage);
        }
    }

}
