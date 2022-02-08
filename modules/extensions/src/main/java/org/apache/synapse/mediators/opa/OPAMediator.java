/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.mediators.opa;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.AbstractMediator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class OPAMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(OPAMediator.class);

    private String serverUrl = null;
    private String accessToken = null;
    private String policy = null;
    private String rule = "allow";
    private String requestGeneratorClassName = "org.apache.synapse.mediators.opa.OPASynapseRequestGenerator";
    private Map<String, Object> advancedProperties = new HashMap<String, Object>();

    public void init() {

    }

    @Override
    public boolean mediate(MessageContext messageContext) {

        OPARequestGenerator requestGenerator = null;
        String opaResponseString = null;
        try {
            try {
                Class<?> requestGeneratorClassObject = Class.forName(requestGeneratorClassName);
                Constructor<?> constructor = requestGeneratorClassObject.getConstructor();
                requestGenerator = (OPARequestGenerator) constructor.newInstance();
            } catch (NoSuchMethodException | ClassNotFoundException | InstantiationException | IllegalAccessException
                    | InvocationTargetException e) {
                throw new OPASecurityException(OPASecurityException.MEDIATOR_ERROR,
                        "Cannot initialize the provided request generator", e);
            }

            String opaPayload = requestGenerator.createRequest(policy, rule, advancedProperties, messageContext);
            String evaluatingPolicyUrl = serverUrl + "/" + policy + "/" + rule;

            opaResponseString = OPAClient.publish(evaluatingPolicyUrl, opaPayload, accessToken);
            return requestGenerator.handleResponse(policy, rule, opaResponseString, messageContext);
        } catch (OPASecurityException e) {
            handleException("Rejected from opa policy." + e.getMessage(), messageContext);
        }
        return false;
    }

    public String getServerUrl() {

        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {

        this.serverUrl = serverUrl;
    }

    public String getAccessToken() {

        return accessToken;
    }

    public void setAccessToken(String accessToken) {

        this.accessToken = accessToken;
    }

    public String getRequestGeneratorClassName() {

        return requestGeneratorClassName;
    }

    public void setRequestGeneratorClassName(String requestGeneratorClassName) {

        this.requestGeneratorClassName = requestGeneratorClassName;
    }

    public String getPolicy() {

        return policy;
    }

    public void setPolicy(String policy) {

        this.policy = policy;
    }

    public String getRule() {

        return rule;
    }

    public void setRule(String rule) {

        this.rule = rule;
    }

    public Map<String, Object> getAdvancedProperties() {

        return advancedProperties;
    }

    public void setAdvancedProperties(Map<String, Object> advancedProperties) {

        this.advancedProperties = advancedProperties;
    }

    public void addAdvancedProperty(String property, Object value) {

        this.advancedProperties.put(property, value);
    }
}
