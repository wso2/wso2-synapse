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
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.TreeMap;

/**
 * Default implementation of the {@link OPARequestGenerator}.
 */
public class OPASynapseRequestGenerator implements OPARequestGenerator {

    public static final String HTTP_METHOD_STRING = "HTTP_METHOD";
    public static final String API_BASEPATH_STRING = "TransportInURL";
    private static final Log log = LogFactory.getLog(OPASynapseRequestGenerator.class);

    @Override
    public String generateRequest(String policyName, String rule, Map<String, Object> additionalParameters,
                                  MessageContext messageContext) throws OPASecurityException {

        org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext)
                .getAxis2MessageContext();
        TreeMap<String, String> transportHeadersMap = (TreeMap<String, String>) axis2MessageContext
                .getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

        String requestOriginIP = OPAUtils.getRequestIp(axis2MessageContext);
        String requestMethod = (String) axis2MessageContext.getProperty(HTTP_METHOD_STRING);
        String requestPath = (String) axis2MessageContext.getProperty(API_BASEPATH_STRING);

        JSONObject inputPayload = new JSONObject();
        JSONObject opaPayload = new JSONObject();

        opaPayload.put("requestOrigin", requestOriginIP);
        opaPayload.put("method", requestMethod);
        opaPayload.put("path", requestPath);
        opaPayload.put("transportHeaders", new JSONObject(transportHeadersMap));

        if (additionalParameters.get("additionalMCProperties") != null) {
            String additionalMCPropertiesString = (String) additionalParameters.get("additionalMCProperties");
            String[] additionalMCProperties = additionalMCPropertiesString.split(",");
            for (String mcProperty : additionalMCProperties) {
                if (messageContext.getProperty(mcProperty) != null) {
                    opaPayload.put(mcProperty, messageContext.getProperty(mcProperty));
                }
            }
        }

        inputPayload.put("input", opaPayload);
        return inputPayload.toString();
    }

    @Override
    public boolean handleResponse(String policyName, String rule, String opaResponse, Map<String,
            Object> additionalParameters, MessageContext messageContext) throws OPASecurityException {

        if (opaResponse.equals("{}")) {
            log.error("Empty result received for the OPA policy " + policyName + " for rule " + rule);
            throw new OPASecurityException(OPASecurityException.INTERNAL_ERROR,
                    "Empty result received for the OPA policy " + policyName + " for rule " + rule);
        } else {
            try {
                JSONObject responseObject = new JSONObject(opaResponse);
                if (rule != null) {
                    return responseObject.getBoolean("result");
                } else {
                    // If a rule is not specified, default allow rule is considered
                    JSONObject resultObjectFromAllow = (JSONObject)responseObject.get("allow");
                    return resultObjectFromAllow.getBoolean("result");
                }
            } catch (JSONException e) {
                log.error("Error parsing OPA JSON response, the field \"result\" not found or not a Boolean", e);
                throw new OPASecurityException(OPASecurityException.INTERNAL_ERROR,
                        OPASecurityException.INTERNAL_ERROR_MESSAGE, e);
            }
        }
    }
}
