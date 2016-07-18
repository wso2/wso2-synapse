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

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.wso2.securevault.CipherFactory;
import org.wso2.securevault.CipherOperationMode;
import org.wso2.securevault.DecryptionProvider;
import org.wso2.securevault.commons.MiscellaneousUtil;
import org.wso2.securevault.definition.CipherInformation;
import org.wso2.securevault.definition.IdentityKeyStoreInformation;
import org.wso2.securevault.definition.KeyStoreInformationFactory;
import org.wso2.securevault.keystore.IdentityKeyStoreWrapper;

import java.security.Security;
import java.util.Properties;

/**
 * This class will provide the required methods to decrypt given encrypted parameter value.
 */
public class CryptoUtil {
    private static Log log = LogFactory.getLog(CryptoUtil.class);
    private DecryptionProvider baseCipher;
    private boolean isInitialized = false;
    private EncodeDecodeTypes inType = null;
    private EncodeDecodeTypes outType = null;
    private String algorithm = null;

    /**
     * Public constructor
     *
     * @param secureVaultProperties
     * @throws org.apache.axis2.AxisFault
     */
    public CryptoUtil(Properties secureVaultProperties) throws AxisFault {
        init(secureVaultProperties);
    }

    /**
     * Method to initialise crypto util. which will generate the required chiper etc.
     *
     * @param secureVaultProperties
     * @throws org.apache.axis2.AxisFault
     */
    public void init(Properties secureVaultProperties) throws AxisFault {
        //Create a KeyStore Information  for private key entry KeyStore
        IdentityKeyStoreInformation identityInformation =
                KeyStoreInformationFactory.createIdentityKeyStoreInformation(secureVaultProperties);
        String identityKeyPass = null;
        String identityStorePass = null;
        if (identityInformation != null) {
            identityKeyPass = identityInformation
                    .getKeyPasswordProvider().getResolvedSecret();
            identityStorePass = identityInformation
                    .getKeyStorePasswordProvider().getResolvedSecret();
        }
        if (!Util.validatePasswords(identityStorePass, identityKeyPass)) {
            if (log.isDebugEnabled()) {
                log.info("Either Identity or Trust keystore password is mandatory" +
                         " in order to initialized secret manager.");
            }
            throw new AxisFault("Error inititialising cryptoutil, required parameters not provided");
        }
        IdentityKeyStoreWrapper identityKeyStoreWrapper = new IdentityKeyStoreWrapper();
        identityKeyStoreWrapper.init(identityInformation, identityKeyPass);
        algorithm = MiscellaneousUtil.getProperty(secureVaultProperties,
                                                         CryptoConstants.CIPHER_ALGORITHM,
                                                         CryptoConstants.CIPHER_ALGORITHM_DEFAULT);
        String provider = MiscellaneousUtil.getProperty(secureVaultProperties,
                                                        CryptoConstants.SECURITY_PROVIDER, null);
        String cipherType = MiscellaneousUtil.getProperty(secureVaultProperties,
                                                          CryptoConstants.CIPHER_TYPE, null);


        String inTypeString = MiscellaneousUtil.getProperty(secureVaultProperties, CryptoConstants.INPUT_ENCODE_TYPE,
                                                            null);
        inType = Util.getEncodeDecodeType(inTypeString, EncodeDecodeTypes.BASE64);


        String outTypeString = MiscellaneousUtil.getProperty(secureVaultProperties, CryptoConstants.OUTPUT_ENCODE_TYPE,
                                                             null);
        outType = Util.getEncodeDecodeType(outTypeString, null);

        CipherInformation cipherInformation = new CipherInformation();
        cipherInformation.setAlgorithm(algorithm);
        cipherInformation.setCipherOperationMode(CipherOperationMode.DECRYPT);
        cipherInformation.setType(cipherType);
        cipherInformation.setInType(null); //skipping decoding encoding in securevault
        cipherInformation.setOutType(null); //skipping decoding encoding in securevault
        if (provider != null && !provider.isEmpty()) {
            if (CryptoConstants.BOUNCY_CASTLE_PROVIDER.equals(provider)) {
                Security.addProvider(new BouncyCastleProvider());
                cipherInformation.setProvider(provider);
            }
            //todo need to add other providers if there are any.
        }
        baseCipher = CipherFactory.createCipher(cipherInformation, identityKeyStoreWrapper);
        isInitialized = true;
    }

    /**
     * Method used to decrypt and encode or decode accordingly.
     *
     * @param encryptedBytes
     * @return response
     */
    public byte[] decrypt(byte[] encryptedBytes) {
        if (inType != null) {
            encryptedBytes = EncodeDecodeHelper.decode(encryptedBytes, inType);
        }
        byte[] response;
        response = baseCipher.decrypt(encryptedBytes);
        if (outType != null) {
            response = EncodeDecodeHelper.encode(response, outType);
        }
        return response;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

}
