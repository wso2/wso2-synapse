package org.apache.synapse.commons.crypto;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This is a util class to provide required functions.
 */
public class Util {
    private static Log log = LogFactory.getLog(Util.class);

    /**
     * Helper method to load properties file.
     *
     * @param filePath
     * @return properties
     */
    public static Properties loadProperties(String filePath) {
        Properties properties = new Properties();
        File dataSourceFile = new File(filePath);
        if (!dataSourceFile.exists()) {
            return properties;
        }

        InputStream in = null;
        try {
            in = new FileInputStream(dataSourceFile);
            properties.load(in);
        } catch (IOException e) {
            String msg = "Error loading properties from a file at :" + filePath;
            log.warn(msg, e);
            return properties;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {

                }
            }
        }
        return properties;
    }

    /**
     * Helper method to validate store password and key password
     *
     * @param identityStorePass
     * @param identityKeyPass
     * @return if valid true, false otherwise
     */
    public static boolean validatePasswords(String identityStorePass,
                                      String identityKeyPass) {
        boolean isValid = false;
        if (identityStorePass != null && !"".equals(identityStorePass) &&
            identityKeyPass != null && !"".equals(identityKeyPass)) {
            if (log.isDebugEnabled()) {
                log.debug("Identity Store Password " +
                          "and Identity Store private key Password cannot be found.");
            }
            isValid = true;
        }
        return isValid;
    }

    /**
     * Helper method to decide encode decode types.
     *
     * @param value
     * @param defaultValue
     * @return type
     */
    public static EncodeDecodeTypes getEncodeDecodeType(String value, EncodeDecodeTypes defaultValue) {
        if (value != null && !value.isEmpty() && value.equals(EncodeDecodeTypes.BASE64)) {
            return EncodeDecodeTypes.BASE64;
        } else if (value != null && !value.isEmpty() && value.equals(EncodeDecodeTypes.BIGINTEGER16)) {
            return EncodeDecodeTypes.BIGINTEGER16;
        } else if (value != null && !value.isEmpty() && value.equals(EncodeDecodeTypes.HEX)){
            return EncodeDecodeTypes.HEX;
        } else {
            return defaultValue;
        }
    }

    /**
     * Helper method to append encrypted parts to the response.
     *
     * @param value
     * @param toAppend
     * @return response
     */
    public static byte[] append(byte[] value, byte[] toAppend) {
        byte[] response = new byte[value.length + toAppend.length];
        for (int i = 0; i < value.length; i++) {
            response[i] = value[i];
        }
        for (int i = 0; i < toAppend.length; i++) {
            response[i + value.length] = toAppend[i];
        }
        return response;
    }
}
