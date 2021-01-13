/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.util.xpath;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

/*
This is a util class for managing keystore
 */
public class KeyStoreManager {

    private static final Log log = LogFactory.getLog(EncryptFunction.class);

    /**
     * Returns the keystore of the given file path
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
        try (FileInputStream keyStoreFileInputStream =  new FileInputStream(file)) {
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(keyStoreFileInputStream, keyStorePassword.toCharArray());
            return keyStore;
        } catch (IOException | NoSuchAlgorithmException | CertificateException e) {
            String errorMessage = String.format("Keystore file does not exist in the path as configured " +
                    "in '%s' property.", keyStoreFilePath);
            throw new KeyStoreException(errorMessage);
        }
    }

    /**
     * Returns the Certificate of the given alias
     *
     * @param keyStore the keystore
     * @param keyAlias the alias
     * @return Certificate
     * @throws KeyStoreException On error while get the certificate
     */
    public static Certificate getCertificateFromStore(KeyStore keyStore, String keyAlias) throws KeyStoreException {
        return keyStore.getCertificate(keyAlias);
    }

    /**
     * Returns the private key of the given alias
     *
     * @param keyStore the keystore
     * @param keyPassword the keystore password
     * @param keyAlias the alias
     * @return PrivateKey
     * @throws UnrecoverableKeyException,NoSuchAlgorithmException,KeyStoreException On error while get private key
     */
    public static PrivateKey getPrivateKeyFromKeyStore(KeyStore keyStore, String keyPassword, String keyAlias)
            throws UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        Key key = keyStore.getKey(keyAlias, keyPassword.toCharArray());
        return (key instanceof PrivateKey) ? (PrivateKey) key : null;
    }
}
