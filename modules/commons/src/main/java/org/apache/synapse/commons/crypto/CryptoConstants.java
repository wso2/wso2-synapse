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

/**
 * This is a interface to hold required constant values.
 */
public interface CryptoConstants {
    /* Private key entry KeyStore location */
    public final static String IDENTITY_KEY_STORE = "keystore.identity.location";
    /* User name for access keyStore*/
    public final static String IDENTITY_KEY_STORE_USER_NAME = "keystore.identity.store.username";
    /* Password for access keyStore*/
    public final static String IDENTITY_KEY_STORE_PASSWORD = "keystore.identity.store.password";
    /* Alias for private key entry KeyStore */
    public final static String IDENTITY_KEY_STORE_ALIAS = "keystore.identity.alias";
    /* Private key entry KeyStore type  */
    public final static String IDENTITY_KEY_STORE_TYPE = "keystore.identity.type";
    public final static String IDENTITY_KEY_STORE_PARAMETERS = "keystore.identity.parameters";

    /* User name for get private key*/
    public final static String IDENTITY_KEY_USER_NAME = "keystore.identity.key.username";
    /* Password for get private key*/
    public final static String IDENTITY_KEY_PASSWORD = "keystore.identity.key.password";
    /* Cipher algorithm to be used*/
    public final static String CIPHER_ALGORITHM = "cipher.algorithm";
    /* Default Cipher algorithm to be used*/
    public final static String CIPHER_ALGORITHM_DEFAULT = "RSA";

    public final static String PROPERTIES_FILE_PATH_DEFAULT = "secureVault.properties";
    /**
     * Cipher type ('symmetric' or 'asymmetric')
     */
    public final static String CIPHER_TYPE = "cipher.type";
    /**
     * Security provider, can use providers like BouncyCastle.
     */
    public final static String SECURITY_PROVIDER = "security.provider";
    /**
     * encode type of the given value to be encoded
     */
    public final static String INPUT_ENCODE_TYPE = "input.encode.type";
    /**
     * encode type of the final outcome.
     */
    public final static String OUTPUT_ENCODE_TYPE = "output.encode.type";

    /**
     * BouncyCastleProvider "BC"
     */
    public final static String BOUNCY_CASTLE_PROVIDER = "BC";


    /**
     * Secure vault namespace to be used when resolving axis2 config passwords.
     */
    public final static String SECUREVAULT_NAMESPACE = "http://org.wso2.securevault/configuration";
    /**
     * Secure vault alias attribute name.
     */
    public final static String SECUREVAULT_ALIAS_ATTRIBUTE = "secretAlias";
}
