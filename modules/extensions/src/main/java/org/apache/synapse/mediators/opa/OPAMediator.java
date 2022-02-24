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
import org.apache.http.HttpStatus;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2Sender;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.transport.nhttp.NhttpConstants;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class OPAMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(OPAMediator.class);

    private String serverUrl = null;
    private String accessKey = null;
    private String policy = null;
    private String rule = null;
    private String requestGeneratorClassName = null;
    private Map<String, Object> advancedProperties = new HashMap<String, Object>();

    public void init() {

    }

    @Override
    public boolean mediate(MessageContext messageContext) {

        try {
            OPARequestGenerator requestGenerator = getRequestGenerator(requestGeneratorClassName);
            String opaPayload = requestGenerator.createRequest(policy, rule, advancedProperties, messageContext);

            String evaluatingPolicyUrl = serverUrl + "/" + policy;
            if (rule != null) {
                evaluatingPolicyUrl += "/" + rule;
            }

            String opaResponseString = OPAClient.publish(evaluatingPolicyUrl, opaPayload, accessKey);
            if (requestGenerator.handleResponse(policy, rule, opaResponseString, messageContext)) {
                return true;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Access revoked for the API request by the OPA policy. Policy payload " + opaPayload
                            + " and OPA response " + opaResponseString);
                }
                throw new OPASecurityException(OPASecurityException.ACCESS_REVOKED,
                        OPASecurityException.ACCESS_REVOKED_MESSAGE);
            }
        } catch (OPASecurityException e) {
            handleAuthFailure(messageContext, e);
        }
        return false;
    }

    private OPARequestGenerator getRequestGenerator(String className) throws OPASecurityException {

        Class<?> requestGeneratorClassObject = null;
        try {
            if (className == null) {
                className = "org.apache.synapse.mediators.opa.OPASynapseRequestGenerator";
                if (log.isDebugEnabled()) {
                    log.debug("Request generator class not found. Default generator used.");
                }
            }
            requestGeneratorClassObject = Class.forName(className);
            Constructor<?> constructor = requestGeneratorClassObject.getConstructor();
            return (OPARequestGenerator) constructor.newInstance();
        } catch (NoSuchMethodException | ClassNotFoundException | InstantiationException | IllegalAccessException
                | InvocationTargetException e) {
            log.error("An error occurred while creating the request generator.", e);
            throw new OPASecurityException(OPASecurityException.MEDIATOR_ERROR,
                   OPASecurityException.MEDIATOR_ERROR_MESSAGE);
        }
    }

    private void handleAuthFailure(MessageContext messageContext, OPASecurityException e) {

        int status;
        String errorMessage;
        if (e.getErrorCode() == OPASecurityException.MEDIATOR_ERROR
                || e.getErrorCode() == OPASecurityException.OPA_RESPONSE_ERROR) {
            // OPA response error occurs when the policy is not defined in the opa end. This is considered as an
            // internal server error
            status = HttpStatus.SC_INTERNAL_SERVER_ERROR;
            errorMessage = "Internal Sever Error";
        } else if (e.getErrorCode() == OPASecurityException.ACCESS_REVOKED) {
            status = HttpStatus.SC_FORBIDDEN;
            errorMessage = "Forbidden";
        } else if (e.getErrorCode() == OPASecurityException.OPA_REQUEST_ERROR) {
            status = HttpStatus.SC_BAD_REQUEST;
            errorMessage = "Bad Request";
        } else {
            status = HttpStatus.SC_UNAUTHORIZED;
            errorMessage = "Unauthorized";
        }

        messageContext.setProperty(SynapseConstants.ERROR_CODE, status);
        messageContext.setProperty(SynapseConstants.ERROR_MESSAGE, errorMessage);
        messageContext.setProperty(SynapseConstants.ERROR_DETAIL, e.getMessage());
        messageContext.setProperty(SynapseConstants.ERROR_EXCEPTION, e);

        Mediator sequence = messageContext.getSequence("_auth_failure_handler_");
        if (sequence != null && !sequence.mediate(messageContext)) {
            // If needed user should be able to prevent the rest of the fault handling
            // logic from getting executed
            return;
        }

        org.apache.axis2.context.MessageContext axis2MC = ((Axis2MessageContext) messageContext).
                getAxis2MessageContext();
        axis2MC.setProperty(NhttpConstants.HTTP_SC, status);
        Axis2Sender.sendBack(messageContext);
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
