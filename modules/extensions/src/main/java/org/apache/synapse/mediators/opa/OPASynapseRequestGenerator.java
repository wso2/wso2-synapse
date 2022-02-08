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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.axis2.util.JavaUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.json.JSONObject;

import java.util.Map;
import java.util.TreeMap;

public class OPASynapseRequestGenerator implements OPARequestGenerator {

    public static final String HTTP_METHOD_STRING = "HTTP_METHOD";
    public static final String API_BASEPATH_STRING = "TransportInURL";
    static final String HTTP_VERSION_CONNECTOR = ".";
    private static final Log log = LogFactory.getLog(OPASynapseRequestGenerator.class);

    @Override
    public String createRequest(String policyName, String rule, Map<String, Object> advancedProperties,
                                MessageContext messageContext) throws OPASecurityException {

        org.apache.axis2.context.MessageContext axis2MessageContext = ((Axis2MessageContext) messageContext)
                .getAxis2MessageContext();
        TreeMap<String, String> transportHeadersMap = (TreeMap<String, String>) axis2MessageContext
                .getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

        String requestOriginIP = OPAUtils.getIp(axis2MessageContext);
        String requestMethod = (String) axis2MessageContext.getProperty(HTTP_METHOD_STRING);
        String requestPath = (String) axis2MessageContext.getProperty(API_BASEPATH_STRING);
        String requestHttpVersion = OPAUtils.getHttpVersion(axis2MessageContext);

        JSONObject opaPayload = new JSONObject();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String transportHeadersJson = gson.toJson(transportHeadersMap);

        opaPayload.put("requestOrigin", requestOriginIP);
        opaPayload.put("method", requestMethod);
        opaPayload.put("path", requestPath);
        opaPayload.put("httpVersion", requestHttpVersion);
        opaPayload.put("transportHeaders", transportHeadersJson);

        return opaPayload.toString();
    }

    @Override
    public boolean handleResponse(String policyName, String rule, String opaResponse, MessageContext messageContext)
            throws OPASecurityException {

        if (opaResponse.equals("{}")) {
            if (log.isDebugEnabled()) {
                log.debug("Empty result received for the rule " + rule + " of policy " + policyName);
            }
            throw new OPASecurityException(OPASecurityException.OPA_RESPONSE_ERROR,
                    "Empty result received for the OPA policy rule");
        } else {
            JSONObject responseObject = new JSONObject(opaResponse);
            Object resultObject = responseObject.get(rule);
            if (resultObject != null) {
                if (JavaUtils.isTrueExplicitly(resultObject)) {
                    return true;
                } else {
                    throw new OPASecurityException(OPASecurityException.ACCESS_REVOKED, "Access revoked");
                }
            }
            throw new OPASecurityException(OPASecurityException.OPA_RESPONSE_ERROR,
                    "Specified rule is not included in the OPA server response");
        }
    }

}
