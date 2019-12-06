/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.

 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.util.resolver;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Check the secret alias match with the vaultLookupPattern regex.
 */
public class SecureVaultResolver {

    private static Log log = LogFactory.getLog(CustomWSDLLocator.class);

    private SecureVaultResolver(){}

    /** regex for secure vault expression */
    private static final String SECURE_VAULT_REGEX = "\\{(wso2:vault-lookup\\('(.*?)'\\))\\}";

    private static Pattern vaultLookupPattern = Pattern.compile(SECURE_VAULT_REGEX);

    /**
     * Check the secret alias match with the vaultLookupPattern regex.
     *
     * @param aliasSecret Value of secret alias
     * @return boolean state of the pattern existence.
     */
    public static boolean checkVaultLookupPattersExists(String aliasSecret) {
        Matcher lookupMatcher = vaultLookupPattern.matcher(aliasSecret);
        return lookupMatcher.find();
    }

    /**
     * Convert the secret alias to the actual password using synapse message context.
     *
     * @param synapseEnvironment synapse environment
     * @param value Value of password from DBMediator
     * @return the actual password from the Secure Vault Password Management.
     */
    public static String resolve(SynapseEnvironment synapseEnvironment, String value) {
        //Password can be null, it is optional
        if (value == null) {
            return null;
        }
        Matcher lookupMatcher = vaultLookupPattern.matcher(value);
        String resolvedValue = value;
        if (lookupMatcher.find()) {
            Value expression = null;
            //getting the expression with out curly brackets
            String expressionStr = lookupMatcher.group(1);
            try {
                expression = new Value(new SynapseXPath(expressionStr));
            } catch (JaxenException e) {
                throw new SynapseException("Error while building the expression : " + expressionStr, e);
            }
            resolvedValue = expression.evaluateValue(synapseEnvironment.createMessageContext());
            if (StringUtils.isEmpty(resolvedValue)) {
                log.warn("Found Empty value for expression : " + expression.getExpression());
                resolvedValue = "";
            }
        }
        return resolvedValue;
    }
}
