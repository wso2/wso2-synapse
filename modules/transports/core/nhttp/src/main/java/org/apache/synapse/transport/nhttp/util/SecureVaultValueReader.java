/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.transport.nhttp.util;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.commons.crypto.CryptoConstants;
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecureVaultException;
import org.wso2.securevault.commons.MiscellaneousUtil;

import javax.xml.namespace.QName;

/**
 * This class provides the functionality to read the value from SecureVault.
 */
public class SecureVaultValueReader {

    public static String getSecureVaultValue(SecretResolver secretResolver, OMElement paramElement) {
        String value = null;
        if (paramElement != null) {
            if (secretResolver == null) {
                throw new SecureVaultException("Cannot resolve secret password because axis2 secret resolver " +
                                               "is null");
            }
            value = MiscellaneousUtil.resolve(paramElement, secretResolver);
        }
        return value;
    }
}
