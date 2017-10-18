/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.synapse.commons.crypto;

import org.apache.commons.lang.StringUtils;
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
     * <p>
     * Helper method to validate store password and key password
     * </p>
     * <p>
     * <b>Note : </b> this method will validate whether both the private key and the identity store password is
     * present, if it's not present the validation will fail.
     * </p>
     *
     * @param identityStorePass password of the identity store
     * @param identityKeyPass   identify store private key password
     * @return if valid true, false otherwise
     */
    public static boolean validatePasswords(String identityStorePass,
                                            String identityKeyPass) {
        boolean isValid = false;

        if (identityStorePass != null && !StringUtils.EMPTY.equals(identityStorePass) &&
                identityKeyPass != null && !StringUtils.EMPTY.equals(identityKeyPass)) {
            isValid = true;
        } else {
            //This fix is done in relation to the jira product-ei-543
            //The severity of the log will be kept as debug, since the caller will indicate the error to the user
            if (log.isDebugEnabled()) {
                log.debug("Identity store password and/or identity store private key password cannot be found.");
            }
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
