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

import com.damnhandy.uri.template.UriTemplate;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.Constants;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.commons.resolvers.ResolverFactory;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.endpoints.HTTPEndpoint;
import org.apache.synapse.endpoints.OAuthConfiguredHTTPEndpoint;
import org.apache.synapse.endpoints.oauth.OAuthException;
import org.apache.synapse.endpoints.oauth.OAuthHandler;
import org.apache.synapse.endpoints.oauth.OAuthUtils;
import org.apache.synapse.rest.RESTConstants;
import org.apache.synapse.util.CommentListUtil;

import javax.xml.namespace.QName;
import java.util.Properties;

public class HTTPEndpointFactory extends DefaultEndpointFactory {

    private static HTTPEndpointFactory instance = new HTTPEndpointFactory();

    public static HTTPEndpointFactory getInstance() {
        return instance;
    }

    @Override
    public EndpointDefinition createEndpointDefinition(OMElement elem) {
            DefinitionFactory fac = getEndpointDefinitionFactory();
        EndpointDefinition endpointDefinition;
        if (fac == null) {
            fac = new EndpointDefinitionFactory();
            endpointDefinition = fac.createDefinition(elem);
        } else{
            endpointDefinition = fac.createDefinition(elem);
        }
        extractSpecificEndpointProperties(endpointDefinition, elem);
        return endpointDefinition;
    }

    @Override
    protected Endpoint createEndpoint(OMElement epConfig, boolean anonymousEndpoint, Properties properties) {

        OMAttribute nameAttr = epConfig.getAttribute(
                new QName(XMLConfigConstants.NULL_NAMESPACE, "name"));

        String name = null;

        if (nameAttr != null) {
            name = nameAttr.getAttributeValue();
        }

        OMElement httpElement = epConfig.getFirstChildWithName(
                new QName(SynapseConstants.SYNAPSE_NAMESPACE, "http"));

        HTTPEndpoint httpEndpoint;

        OAuthHandler oAuthhandler = createOAuthHandler(httpElement, name);

        if (oAuthhandler != null) {
            httpEndpoint = new OAuthConfiguredHTTPEndpoint(oAuthhandler);
        } else {
            httpEndpoint = new HTTPEndpoint();
        }

        if (name != null) {
            httpEndpoint.setName(name);
        }

        if (httpElement != null) {
            EndpointDefinition definition = createEndpointDefinition(httpElement);


            OMAttribute uriTemplateAttr = httpElement.getAttribute(new QName("uri-template"));
            if (uriTemplateAttr != null) {

                String uriValue = uriTemplateAttr.getAttributeValue();
                uriValue = ResolverFactory.getInstance().getResolver(uriValue).resolve();
                //get object and and resolve
                    if (uriValue.startsWith(HTTPEndpoint.legacyPrefix)) {
                        httpEndpoint.setUriTemplate(UriTemplate.fromTemplate(
                                uriValue.substring(HTTPEndpoint.legacyPrefix.length())));
                        // Set the address to the initial template value.
                        definition.setAddress(uriValue.substring(HTTPEndpoint.legacyPrefix.length()));
                        httpEndpoint.setLegacySupport(true);
                    } else {

                        httpEndpoint.setUriTemplate(UriTemplate.fromTemplate(uriValue));
                        // Set the address to the initial template value.
                        definition.setAddress(uriValue);
                        httpEndpoint.setLegacySupport(false);
                    }


            }


            httpEndpoint.setDefinition(definition);
            processAuditStatus(definition, httpEndpoint.getName(), httpElement);

            OMAttribute methodAttr = httpElement.getAttribute(new QName("method"));
            if (methodAttr != null) {
                setHttpMethod(httpEndpoint, methodAttr.getAttributeValue());
            } else {
                // Method is not a mandatory parameter for HttpEndpoint. So methodAttr Can be null
                if (log.isDebugEnabled()) {
                    log.debug("Method is not specified for HttpEndpoint. " +
                              "Hence using the http method from incoming message");
                }
            }
        }

        processProperties(httpEndpoint, epConfig);
        CommentListUtil.populateComments(epConfig, httpEndpoint.getCommentsList());
        return httpEndpoint;
    }

    private void setHttpMethod(HTTPEndpoint httpEndpoint, String httpMethod) {
        if (httpMethod != null) {
            if (httpMethod.equalsIgnoreCase(Constants.Configuration.HTTP_METHOD_POST)) {
                httpEndpoint.setHttpMethod(Constants.Configuration.HTTP_METHOD_POST);
            } else if (httpMethod.equalsIgnoreCase(Constants.Configuration.HTTP_METHOD_GET)) {
                httpEndpoint.setHttpMethod(Constants.Configuration.HTTP_METHOD_GET);
            } else if (httpMethod.equalsIgnoreCase(Constants.Configuration.HTTP_METHOD_PUT)) {
                httpEndpoint.setHttpMethod(Constants.Configuration.HTTP_METHOD_PUT);
            } else if (httpMethod.equalsIgnoreCase(Constants.Configuration.HTTP_METHOD_DELETE)) {
                httpEndpoint.setHttpMethod(Constants.Configuration.HTTP_METHOD_DELETE);
            } else if (httpMethod.equalsIgnoreCase(Constants.Configuration.HTTP_METHOD_HEAD)) {
                httpEndpoint.setHttpMethod(Constants.Configuration.HTTP_METHOD_HEAD);
            } else if (httpMethod.equalsIgnoreCase(Constants.Configuration.HTTP_METHOD_PATCH)) {
                httpEndpoint.setHttpMethod(Constants.Configuration.HTTP_METHOD_PATCH);
            } else if (httpMethod.equalsIgnoreCase(RESTConstants.METHOD_OPTIONS)) {
                httpEndpoint.setHttpMethod(RESTConstants.METHOD_OPTIONS);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Invalid http method specified." +
                              "Hence using the http method from incoming message");
                }
            }
        }
    }

    /**
     * This method will return an OAuthHandler instance depending on the oauth configs or throw a synapse exception
     *
     * @param httpElement Element containing http configs
     * @param endpointName Name of the http endpoint
     * @return OAuthHandler instance if valid oauth configuration is found
     */
    private OAuthHandler createOAuthHandler(OMElement httpElement, String endpointName) {
        OAuthHandler handler = null;
        try {
            handler = OAuthUtils.getOAuthHandler(httpElement);
        } catch (OAuthException e) {
            if (endpointName == null) {
                endpointName = "";
            }
            handleException("Invalid OAuth configuration for endpoint " + endpointName);
        }
        return handler;
    }

}
