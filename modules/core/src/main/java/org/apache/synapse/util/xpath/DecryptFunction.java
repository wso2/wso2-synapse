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
import org.jaxen.Context;
import org.jaxen.Function;
import org.jaxen.FunctionCallException;
import org.jaxen.function.StringFunction;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.List;

/*
Xpath function to decrypt based on keystore on Asymmetric keys
 */
public class DecryptFunction implements Function {
    private static final Log log = LogFactory.getLog(DecryptFunction.class);
    private static final String DEFAULT_ALGORITHM = "RSA";
    private static final String DEFAULT_KEYSTORE_TYPE = "JKS";

    @Override
    public Object call(Context context, List args) throws FunctionCallException {
        boolean debugOn = log.isDebugEnabled();

        if (args == null) {
            if (debugOn) {
                log.debug("Missing arguments in the function call");
            }
            return SynapseXPathConstants.NULL_STRING;
        }
        int size = args.size();
        if (size == 4) {
            String encryptedText = StringFunction.evaluate(args.get(0), context.getNavigator());
            String keyStore = StringFunction.evaluate(args.get(1), context.getNavigator());
            String keyStorePassword = StringFunction.evaluate(args.get(2), context.getNavigator());
            String keyStoreAlias = StringFunction.evaluate(args.get(3), context.getNavigator());
            return decrypt(encryptedText.getBytes(), keyStore, keyStorePassword, keyStoreAlias, DEFAULT_KEYSTORE_TYPE,
                        DEFAULT_ALGORITHM);
        }
        if (size == 5) {
            String encryptedText = StringFunction.evaluate(args.get(0), context.getNavigator());
            String keyStore = StringFunction.evaluate(args.get(1), context.getNavigator());
            String keyStorePassword = StringFunction.evaluate(args.get(2), context.getNavigator());
            String keyStoreAlias = StringFunction.evaluate(args.get(3), context.getNavigator());
            String keyStoreType = StringFunction.evaluate(args.get(4), context.getNavigator());
            return decrypt(encryptedText.getBytes(), keyStore, keyStorePassword, keyStoreAlias, keyStoreType,
                        DEFAULT_ALGORITHM);
        }
        if (size == 6) {
            String encryptedText = StringFunction.evaluate(args.get(0), context.getNavigator());
            String keyStore = StringFunction.evaluate(args.get(1), context.getNavigator());
            String keyStorePassword = StringFunction.evaluate(args.get(2), context.getNavigator());
            String keyStoreAlias = StringFunction.evaluate(args.get(3), context.getNavigator());
            String keyStoreType = StringFunction.evaluate(args.get(4), context.getNavigator());
            String algorithm = StringFunction.evaluate(args.get(5), context.getNavigator());
            return decrypt(encryptedText.getBytes(), keyStore, keyStorePassword, keyStoreAlias, keyStoreType,
                    algorithm);
        }
        if (debugOn) {
            log.debug("Missing arguments in the function call");
        }
        // return empty string if the arguments are wrong
        return SynapseXPathConstants.NULL_STRING;
    }

    /**
     * Decrypt a given cipher text
     *
     * @param encryptedText The encrypted bytes to be decrypted
     * @return The plain text string
     * @throws FunctionCallException On error during decryption
     */
    private String decrypt(byte[] encryptedText, String keyStorePath, String keyStorePassword, String alias,
                          String keyStoreType, String algorithm) throws FunctionCallException {

        if (encryptedText == null) {
            throw new FunctionCallException("Encrypted text can't be null.");
        }
        try {
            Cipher cipher = Cipher.getInstance(algorithm);
            KeyStore keyStore = KeyStoreManager.getKeyStore(keyStorePath, keyStorePassword, keyStoreType);
            Certificate certificate = KeyStoreManager.getCertificateFromStore(keyStore, alias);
            if (log.isDebugEnabled()) {
                log.debug("Certificate used for encrypting : " + certificate);
            }
            cipher.init(Cipher.DECRYPT_MODE, KeyStoreManager.getPrivateKeyFromKeyStore(keyStore, keyStorePassword, alias));
            byte[] decodedText = Base64.getDecoder().decode(encryptedText);
            byte[] cipherText = cipher.doFinal(decodedText);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Successfully encrypted data using the algorithm '%s'", algorithm));
            }
            return new String(cipherText);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | KeyStoreException | InvalidKeyException
                | IllegalBlockSizeException | BadPaddingException | UnrecoverableKeyException e) {
            throw new FunctionCallException("An error occurred while encrypting data.", e);
        }
    }
}
