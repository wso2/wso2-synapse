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

import org.apache.http.HttpStatus;
import org.apache.synapse.Mediator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2Sender;
import org.apache.synapse.transport.nhttp.NhttpConstants;

import java.util.TreeMap;

public class OPAUtils {

    /**
     * Get the request originated IP from the message content
     *
     * @param axis2MessageContext Axis2 message context
     * @return IP address as a string
     */
    public static String getRequestIp(org.apache.axis2.context.MessageContext axis2MessageContext) {

        //Set transport headers of the message
        TreeMap<String, String> transportHeaderMap = (TreeMap<String, String>) axis2MessageContext
                .getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        // Assigning an Empty String so that when doing comparisons, .equals method can be used without explicitly
        // checking for nullity.
        String remoteIP = "";
        //Check whether headers map is null and x forwarded for header is present
        if (transportHeaderMap != null) {
            remoteIP = transportHeaderMap.get("X-Forwarded-For");
        }

        //Setting IP of the client by looking at x forded for header and  if it's empty get remote address
        if (remoteIP != null && !remoteIP.isEmpty()) {
            if (remoteIP.indexOf(",") > 0) {
                remoteIP = remoteIP.substring(0, remoteIP.indexOf(","));
            }
        } else {
            remoteIP = (String) axis2MessageContext.getProperty(org.apache.axis2.context.MessageContext.REMOTE_ADDR);
        }
        if (remoteIP.indexOf(":") > 0) {
            remoteIP = remoteIP.substring(0, remoteIP.indexOf(":"));
        }
        return remoteIP;
    }

    /**
     * Handle the policy failure. This can be an internal error or access revoked by the policy
     *
     * @param messageContext Message context
     * @param e              OPASecurityException
     */
    public static void handlePolicyFailure(MessageContext messageContext, OPASecurityException e) {

        int status;
        String errorMessage;
        if (e.getErrorCode() == OPASecurityException.ACCESS_REVOKED) {
            status = HttpStatus.SC_FORBIDDEN;
            errorMessage = "Forbidden";
        } else {
            status = HttpStatus.SC_INTERNAL_SERVER_ERROR;
            errorMessage = "Internal Sever Error";
        }

        messageContext.setProperty("HTTP_RESPONSE_STATUS_CODE", status);
        messageContext.setProperty(SynapseConstants.ERROR_CODE, e.getErrorCode());
        messageContext.setProperty(SynapseConstants.ERROR_MESSAGE, errorMessage);
        messageContext.setProperty(SynapseConstants.ERROR_EXCEPTION, e);

        Mediator sequence = messageContext.getSequence("_opa_policy_failure_handler_");
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
}
