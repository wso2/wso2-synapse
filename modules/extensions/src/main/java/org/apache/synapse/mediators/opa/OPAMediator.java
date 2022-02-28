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
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.mediators.AbstractMediator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class OPAMediator extends AbstractMediator implements ManagedLifecycle {

    private static final Log log = LogFactory.getLog(OPAMediator.class);
    private static final String defaultRequestGenerator = "org.apache.synapse.mediators.opa.OPASynapseRequestGenerator";

    private String serverUrl = null;
    private String accessKey = null;
    private String policy = null;
    private String rule = null;
    private String requestGeneratorClassName = null;
    private Map<String, String> additionalParameters = new HashMap<String, String>();
    private OPARequestGenerator requestGenerator = null;
    private OPAClient opaClient;

    @Override
    public boolean mediate(MessageContext messageContext) {

        try {
            String opaPayload = requestGenerator.generateRequest(policy, rule, additionalParameters, messageContext);

            String evaluatingPolicyUrl = serverUrl + "/" + policy;
            if (rule != null) {
                evaluatingPolicyUrl += "/" + rule;
            }

            String opaResponseString = opaClient.publish(evaluatingPolicyUrl, opaPayload, accessKey);
            if (requestGenerator.handleResponse(policy, rule, opaResponseString, additionalParameters, messageContext)) {
                return true;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Access revoked for the API request by the OPA policy.OPA response " + opaResponseString);
                }
                log.warn("Access revoked for the API request by the OPA policy.");
                throw new OPASecurityException(OPASecurityException.ACCESS_REVOKED,
                        OPASecurityException.ACCESS_REVOKED_MESSAGE);
            }
        } catch (OPASecurityException e) {
            OPAUtils.handlePolicyFailure(messageContext, e);
        }
        return false;
    }

    private OPARequestGenerator getRequestGenerator(String className) throws OPASecurityException {

        try {
            if (className == null) {
                className = defaultRequestGenerator;
                if (log.isDebugEnabled()) {
                    log.debug("Request generator class not found. Default generator used.");
                }
            }
            Class<?> requestGeneratorClassObject = Class.forName(className);
            Constructor<?> constructor = requestGeneratorClassObject.getConstructor();
            return (OPARequestGenerator) constructor.newInstance();
        } catch (NoSuchMethodException | ClassNotFoundException | InstantiationException | IllegalAccessException
                | InvocationTargetException e) {
            log.error("An error occurred while creating the request generator for " + className, e);
            throw new OPASecurityException(OPASecurityException.INTERNAL_ERROR,
                   OPASecurityException.INTERNAL_ERROR_MESSAGE);
        }
    }

    public String getServerUrl() {

        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {

        this.serverUrl = serverUrl;
    }

    public String getAccessKey() {

        return accessKey;
    }

    public void setAccessKey(String accessKey) {

        this.accessKey = accessKey;
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

    public Map<String, String> getAdditionalParameters() {

        return additionalParameters;
    }

    public void setAdditionalParameters(Map<String, String> additionalParameters) {

        this.additionalParameters = additionalParameters;
    }

    public void addAdditionalParameter(String parameter, String value) {

        this.additionalParameters.put(parameter, value);
    }

    @Override
    public void init(SynapseEnvironment se) {
        try {
            requestGenerator = getRequestGenerator(requestGeneratorClassName);
            opaClient = new OPAClient(serverUrl, additionalParameters);
        } catch (OPASecurityException e) {
            throw new SynapseException("Error when initializing the OPA Mediator", e);
        }
    }

    @Override
    public void destroy() {

    }
}
