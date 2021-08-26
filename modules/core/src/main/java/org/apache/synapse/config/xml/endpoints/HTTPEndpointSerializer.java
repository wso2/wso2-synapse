/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.config.xml.endpoints;


import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.endpoints.HTTPEndpoint;
import org.apache.synapse.endpoints.OAuthConfiguredHTTPEndpoint;
import org.apache.synapse.endpoints.oauth.AuthorizationCodeHandler;
import org.apache.synapse.endpoints.oauth.ClientCredentialsHandler;
import org.apache.synapse.endpoints.oauth.OAuthConstants;

import java.util.Map;

public class HTTPEndpointSerializer extends DefaultEndpointSerializer {
    @Override
    protected OMElement serializeEndpoint(Endpoint endpoint) {
        if (!(endpoint instanceof HTTPEndpoint)) {
            handleException("Invalid endpoint type.");
        }

        fac = OMAbstractFactory.getOMFactory();
        OMElement endpointElement
                = fac.createOMElement("endpoint", SynapseConstants.SYNAPSE_OMNAMESPACE);
        HTTPEndpoint httpEndpoint = (HTTPEndpoint) endpoint;
        EndpointDefinition epDefinitionHttp = httpEndpoint.getDefinition();
        OMElement httpEPElement = serializeEndpointDefinition(epDefinitionHttp);

        if (httpEndpoint.getHttpMethod() != null) {
            httpEPElement.addAttribute(
                    fac.createOMAttribute("method", null, httpEndpoint.getHttpMethod()));
        }

        if (httpEndpoint.getUriTemplate() != null) {
            if (httpEndpoint.isLegacySupport()) {
                httpEPElement.addAttribute(
                        fac.createOMAttribute("uri-template", null, HTTPEndpoint.legacyPrefix + httpEndpoint.getUriTemplate().getTemplate()));
            } else {
        	    httpEPElement.addAttribute(
                    fac.createOMAttribute("uri-template", null, httpEndpoint.getUriTemplate().getTemplate()));
            }
        }

        endpointElement.addChild(httpEPElement);

        if (endpoint instanceof OAuthConfiguredHTTPEndpoint) {
            httpEPElement.addChild(serializeOAuthConfiguration((OAuthConfiguredHTTPEndpoint) endpoint));
        }

        // serialize the properties
        serializeProperties(httpEndpoint, endpointElement);

        serializeCommonAttributes(endpoint, endpointElement);

        return endpointElement;
    }

    @Override
    public OMElement serializeEndpointDefinition(EndpointDefinition endpointDefinition) {

        OMElement element = fac.createOMElement("http", SynapseConstants.SYNAPSE_OMNAMESPACE);
        EndpointDefinitionSerializer serializer = new EndpointDefinitionSerializer();
        serializer.serializeEndpointDefinition(endpointDefinition, element);

        serializeSpecificEndpointProperties(endpointDefinition, element);
        return element;
    }

    /**
     * This method returns an OMElement containing the OAuth configuration
     *
     * @param oAuthConfiguredHTTPEndpoint OAuth Configured HTTP Endpoint
     * @return OMElement containing the OAuth configuration
     */
    private OMElement serializeOAuthConfiguration(OAuthConfiguredHTTPEndpoint oAuthConfiguredHTTPEndpoint) {

        OMElement authentication = fac.createOMElement(
                OAuthConstants.AUTHENTICATION,
                SynapseConstants.SYNAPSE_OMNAMESPACE);

        OMElement oauth = fac.createOMElement(
                OAuthConstants.OAUTH,
                SynapseConstants.SYNAPSE_OMNAMESPACE);
        authentication.addChild(oauth);

        if (oAuthConfiguredHTTPEndpoint.getOauthHandler() instanceof AuthorizationCodeHandler) {
            oauth.addChild(serializeAuthorizationCodeConfigurationElements(
                    (AuthorizationCodeHandler) oAuthConfiguredHTTPEndpoint.getOauthHandler()));
        }

        if (oAuthConfiguredHTTPEndpoint.getOauthHandler() instanceof ClientCredentialsHandler) {
            oauth.addChild(serializeClientCredentialsConfigurationElements(
                    (ClientCredentialsHandler) oAuthConfiguredHTTPEndpoint.getOauthHandler()));
        }

        return authentication;
    }

    /**
     * This method returns an OMElement containing the Client Credentials configuration
     *
     * @param clientCredentialsHandler ClientCredentialsHandler of the OAuth Configured HTTP Endpoint
     * @return OMElement containing the Client Credentials configuration
     */
    private OMElement serializeClientCredentialsConfigurationElements(
            ClientCredentialsHandler clientCredentialsHandler) {

        OMElement clientCredentials = fac.createOMElement(
                OAuthConstants.CLIENT_CREDENTIALS,
                SynapseConstants.SYNAPSE_OMNAMESPACE);

        clientCredentials.addChild(
                createOMElementWithValue(OAuthConstants.OAUTH_CLIENT_ID, clientCredentialsHandler.getClientId()));
        clientCredentials.addChild(createOMElementWithValue(OAuthConstants.OAUTH_CLIENT_SECRET,
                clientCredentialsHandler.getClientSecret()));
        clientCredentials
                .addChild(
                        createOMElementWithValue(OAuthConstants.TOKEN_API_URL, clientCredentialsHandler.getTokenUrl()));
        if (clientCredentialsHandler.getRequestParametersMap() != null &&
                clientCredentialsHandler.getRequestParametersMap().size() > 0) {
            OMElement requestParameters = createOMRequestParams(clientCredentialsHandler.getRequestParametersMap());
            clientCredentials.addChild(requestParameters);
        }
        return clientCredentials;
    }

    /**
     * Create an OMElement for request parameter map.
     *
     * @param requestParametersMap input parameter map.
     * @return OMElement of parameter map.
     */
    private OMElement createOMRequestParams(Map<String, String> requestParametersMap) {
        OMElement requestParameters =
                fac.createOMElement("requestParameters", SynapseConstants.SYNAPSE_OMNAMESPACE);
        for (Map.Entry<String, String> entry : requestParametersMap.entrySet()) {
            OMElement parameter = fac.createOMElement("parameter", SynapseConstants.SYNAPSE_OMNAMESPACE);
            parameter.addAttribute("name", entry.getKey(), null);
            parameter.setText(entry.getValue());
            requestParameters.addChild(parameter);
        }
        return requestParameters;
    }

    /**
     * This method returns an OMElement containing the Authorization Code configuration
     *
     * @param authorizationCodeHandler AuthorizationCodeHandler of the OAuth Configured HTTP Endpoint
     * @return OMElement containing the Authorization Code configuration
     */
    private OMElement serializeAuthorizationCodeConfigurationElements(
            AuthorizationCodeHandler authorizationCodeHandler) {

        OMElement clientCredentials = fac.createOMElement(
                OAuthConstants.AUTHORIZATION_CODE,
                SynapseConstants.SYNAPSE_OMNAMESPACE);

        clientCredentials.addChild(createOMElementWithValue(OAuthConstants.OAUTH_CLIENT_ID,
                authorizationCodeHandler.getClientId()));
        clientCredentials.addChild(createOMElementWithValue(OAuthConstants.OAUTH_CLIENT_SECRET,
                authorizationCodeHandler.getClientSecret()));
        clientCredentials.addChild(
                createOMElementWithValue(OAuthConstants.OAUTH_REFRESH_TOKEN, authorizationCodeHandler.getRefreshToken()));
        clientCredentials
                .addChild(
                        createOMElementWithValue(OAuthConstants.TOKEN_API_URL, authorizationCodeHandler.getTokenUrl()));
        if (authorizationCodeHandler.getRequestParametersMap() != null &&
                authorizationCodeHandler.getRequestParametersMap().size() > 0) {
            OMElement requestParameters = createOMRequestParams(authorizationCodeHandler.getRequestParametersMap());
            clientCredentials.addChild(requestParameters);
        }
        return clientCredentials;
    }

    /**
     * This method returns an OMElement containing the elementValue encapsulated by the elementName
     *
     * @param elementName  Name of the OMElement
     * @param elementValue Value of the OMElement
     * @return OMElement containing the value encapsulated by the elementName
     */
    private OMElement createOMElementWithValue(String elementName, String elementValue) {

        OMElement element = fac.createOMElement(elementName, SynapseConstants.SYNAPSE_OMNAMESPACE);
        element.setText(elementValue);
        return element;
    }
}
