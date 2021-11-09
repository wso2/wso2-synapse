/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.

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

package org.apache.synapse.endpoints.auth.basicauth;

import org.apache.axiom.util.base64.Base64Utils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.auth.AuthConstants;
import org.apache.synapse.endpoints.auth.AuthException;
import org.apache.synapse.endpoints.auth.AuthHandler;
import org.apache.synapse.endpoints.auth.AuthUtils;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class is used to handle Basic Authorization.
 */
public class BasicAuthHandler implements AuthHandler {

    private final String username;
    private final String password;

    public BasicAuthHandler(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public String getAuthType() {
        return AuthConstants.BASIC_AUTH;
    }

    @Override
    public void setAuthHeader(MessageContext messageContext) throws AuthException {

        String basicAuthToken = getEncodedCredentials(messageContext);
        Object transportHeaders = ((Axis2MessageContext) messageContext)
                .getAxis2MessageContext().getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        if (transportHeaders != null && transportHeaders instanceof Map) {
            Map transportHeadersMap = (Map) transportHeaders;
            transportHeadersMap.put(AuthConstants.AUTHORIZATION_HEADER, AuthConstants.BASIC + basicAuthToken);
        } else {
            Map<String, Object> transportHeadersMap = new TreeMap<>(new Comparator<String>() {
                public int compare(String o1, String o2) {
                    return o1.compareToIgnoreCase(o2);
                }
            });
            transportHeadersMap.put(AuthConstants.AUTHORIZATION_HEADER, AuthConstants.BASIC + basicAuthToken);
            ((Axis2MessageContext) messageContext).getAxis2MessageContext()
                    .setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, transportHeadersMap);
        }
    }

    private String getEncodedCredentials(MessageContext messageContext) throws AuthException {
        return Base64Utils.encode((AuthUtils.resolveExpression(username, messageContext) + ":" +
                AuthUtils.resolveExpression(password, messageContext)).getBytes());
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
