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
import org.apache.synapse.endpoints.BasicAuthConfiguredHTTPEndpoint;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.endpoints.HTTPEndpoint;
import org.apache.synapse.endpoints.OAuthConfiguredHTTPEndpoint;
import org.apache.synapse.endpoints.auth.AuthConstants;

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

        // serialize authentication configurations
        serializeAuthConfiguration(endpoint, httpEPElement);

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
     * This method serializes authentication configuration.
     *
     * @param endpoint endpoint
     * @param httpEPElement HTTP Endpoint configuration OMElement
     */
    private void serializeAuthConfiguration(Endpoint endpoint, OMElement httpEPElement) {

        OMElement authentication = fac.createOMElement(AuthConstants.AUTHENTICATION,
                SynapseConstants.SYNAPSE_OMNAMESPACE);

        if (endpoint instanceof OAuthConfiguredHTTPEndpoint) {
            serializeOAuthConfiguration(authentication, (OAuthConfiguredHTTPEndpoint) endpoint);
            httpEPElement.addChild(authentication);
        } else if (endpoint instanceof BasicAuthConfiguredHTTPEndpoint) {
            serializeBasicAuthConfiguration(authentication, (BasicAuthConfiguredHTTPEndpoint) endpoint);
            httpEPElement.addChild(authentication);
        }
    }

    /**
     * This method returns an OMElement containing the Auth configuration.
     *
     * @param authentication Authentication OMElement
     * @param oAuthConfiguredHTTPEndpoint OAuth Configured HTTP Endpoint
     */
    private void serializeOAuthConfiguration(OMElement authentication, OAuthConfiguredHTTPEndpoint oAuthConfiguredHTTPEndpoint) {

        OMElement oauth = fac.createOMElement(AuthConstants.OAUTH, SynapseConstants.SYNAPSE_OMNAMESPACE);
        authentication.addChild(oauth);

        oauth.addChild(oAuthConfiguredHTTPEndpoint.getOauthHandler().serializeOAuthConfiguration(fac));
    }

    /**
     * This method returns an OMElement containing the Auth configuration.
     *
     * @param authentication Authentication OMElement
     * @param basicAuthConfiguredHTTPEndpoint BasicAuth Configured HTTP Endpoint
     */
    private void serializeBasicAuthConfiguration(OMElement authentication,
                                                 BasicAuthConfiguredHTTPEndpoint basicAuthConfiguredHTTPEndpoint) {

        OMElement basicAuth = fac.createOMElement(AuthConstants.BASIC_AUTH, SynapseConstants.SYNAPSE_OMNAMESPACE);
        authentication.addChild(basicAuth);

        basicAuth.addChild(createOMElementWithValue(AuthConstants.BASIC_AUTH_USERNAME,
                basicAuthConfiguredHTTPEndpoint.getBasicAuthHandler().getUsername()));
        basicAuth.addChild(createOMElementWithValue(AuthConstants.BASIC_AUTH_PASSWORD,
                basicAuthConfiguredHTTPEndpoint.getBasicAuthHandler().getPassword()));
    }

    /**
     * This method returns an OMElement containing the elementValue encapsulated by the elementName.
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
