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
package org.apache.synapse.endpoints;

import com.damnhandy.uri.template.UriTemplate;
import com.damnhandy.uri.template.impl.ExpressionParseException;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.mediators.MediatorProperty;
import org.apache.synapse.rest.RESTConstants;
import org.apache.synapse.util.xpath.SynapseXPath;

import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class HTTPEndpoint extends AbstractEndpoint {

    private UriTemplate uriTemplate;

    private String httpMethod;
    private SynapseXPath httpMethodExpression;

    private boolean legacySupport = false; // this is to support backward compatibility

    public static String legacyPrefix = "legacy-encoding:";

    /*Todo*/
    /*Do we need HTTP Headers here?*/


    public void onFault(MessageContext synCtx) {

        // is this really a fault or a timeout/connection close etc?
        if (isTimeout(synCtx)) {
            getContext().onTimeout();
        } else if (isSuspendFault(synCtx)) {
            getContext().onFault();
        }

        // this should be an ignored error if we get here
        setErrorOnMessage(synCtx, null, null);
        super.onFault(synCtx);
    }

    public void onSuccess() {
        if (getContext() != null) {
            getContext().onSuccess();
        }
    }

    public void send(MessageContext synCtx) {
        executeEpTypeSpecificFunctions(synCtx);
        if (getParentEndpoint() == null && !readyToSend()) {
            // if the this leaf endpoint is too a root endpoint and is in inactive
            informFailure(synCtx, SynapseConstants.ENDPOINT_ADDRESS_NONE_READY,
                    "Currently , Address endpoint : " + getContext());
        } else {
            super.send(synCtx);
        }
    }

    public void executeEpTypeSpecificFunctions(MessageContext synCtx) {
        processUrlTemplate(synCtx);
        processHttpMethod(synCtx);
    }

    private void processHttpMethod(MessageContext synCtx) {
        if (httpMethod != null) {
            super.getDefinition().setHTTPEndpoint(true);
            synCtx.setProperty(Constants.Configuration.HTTP_METHOD, httpMethod);
        }
        // Method is not a mandatory parameter for HttpEndpoint. So httpMethod can be null.
        // http method from incoming message is used as the http method
    }

    private String decodeString(String value) {
        if (value == null) {
            return "";
        }
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            if(log.isDebugEnabled()) {
                log.warn("Encoding is not supported", e);
            }
            return value;
        }
    }

    private void processUrlTemplate(MessageContext synCtx) throws ExpressionParseException {
        Map<String, Object> variables = new HashMap<String, Object>();

        /*The properties with uri.var.* are only considered for Outbound REST Endpoints*/
        Set propertySet = synCtx.getPropertyKeySet();

        // We have to create a local UriTemplate object, or else the UriTemplate.set(variables) call will fill up a list of variables and since uriTemplate
        // is not thread safe, this list won't be clearing.
        UriTemplate template = null;

        String evaluatedUri = "";

        // legacySupport for backward compatibility where URI Template decoding handled via HTTPEndpoint
        if (legacySupport) {
            for (Object propertyKey : propertySet) {
                if (propertyKey.toString() != null&&
                        (propertyKey.toString().startsWith(RESTConstants.REST_URI_VARIABLE_PREFIX)
                                || propertyKey.toString().startsWith(RESTConstants.REST_QUERY_PARAM_PREFIX))) {
                    Object objProperty = synCtx.getProperty(propertyKey.toString());
                    if (objProperty != null) {
                        if (objProperty instanceof String) {
                            variables.put(propertyKey.toString(),
                                          decodeString((String) synCtx.getProperty(propertyKey.toString())));
                        } else {
                            variables.put(propertyKey.toString(),
                                          decodeString(String.valueOf(synCtx.getProperty(propertyKey.toString()))));
                        }
                    }
                }
            }

            // Include properties defined at endpoint.
            Iterator endpointProperties = getProperties().iterator();
            while(endpointProperties.hasNext()) {
                MediatorProperty property = (MediatorProperty) endpointProperties.next();
                if(property.getName().toString() != null
                        && (property.getName().toString().startsWith(RESTConstants.REST_URI_VARIABLE_PREFIX) ||
                        property.getName().toString().startsWith(RESTConstants.REST_QUERY_PARAM_PREFIX) )) {
                    variables.put(property.getName(), decodeString((String) property.getValue()));
                }
            }

            template = UriTemplate.fromTemplate(uriTemplate.getTemplate());

            if(template != null) {
                template.set(variables);
            }

            if(variables.isEmpty()){
                evaluatedUri = template.getTemplate();
            } else {
                try {
                    // Decode needs to avoid replacing special characters(e.g %20 -> %2520) when creating URL.
                    String decodedString = URLDecoder.decode(template.expand(), "UTF-8");
                    URL url = new URL(decodedString);
                    URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(),
                            url.getPath(), url.getQuery(), url.getRef());// this to avoid url.toURI which causes exceptions
                    evaluatedUri = uri.toURL().toString();
                    if (log.isDebugEnabled()) {
                        log.debug("Expanded URL : " + evaluatedUri);
                    }
                } catch (URISyntaxException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Invalid URL syntax for HTTP Endpoint: " + this.getName(), e);
                    }
                    evaluatedUri = template.getTemplate();
                } catch(ExpressionParseException e) {
                    log.debug("No URI Template variables defined in HTTP Endpoint: " + this.getName());
                    evaluatedUri = template.getTemplate();
                } catch(MalformedURLException e) {
                    log.debug("Invalid URL for HTTP Endpoint: " + this.getName());
                    evaluatedUri = template.getTemplate();
                } catch(UnsupportedEncodingException e) {
                    log.debug("Exception while decoding the URL in HTTP Endpoint: " + this.getName());
                    evaluatedUri = template.getTemplate();
                }
            }

        } else { // URI Template encoding not handled by HTTP Endpoint, compliant with RFC6570
            for (Object propertyKey : propertySet) {
                if (propertyKey.toString() != null&&
                        (propertyKey.toString().startsWith(RESTConstants.REST_URI_VARIABLE_PREFIX)
                                || propertyKey.toString().startsWith(RESTConstants.REST_QUERY_PARAM_PREFIX))) {
                    Object objProperty =
                                         synCtx.getProperty(propertyKey.toString());
                    if (objProperty != null) {
                        if (objProperty instanceof String) {
                            variables.put(propertyKey.toString(),
                                          (String) synCtx.getProperty(propertyKey.toString()));
                        } else {
                            variables.put(propertyKey.toString(),
                                          (String) String.valueOf(synCtx.getProperty(propertyKey.toString())));
                        }
                    }
                }
            }

            // Include properties defined at endpoint.
            Iterator endpointProperties = getProperties().iterator();
            while(endpointProperties.hasNext()) {
                MediatorProperty property = (MediatorProperty) endpointProperties.next();
                if(property.getName().toString() != null
                        && (property.getName().toString().startsWith(RESTConstants.REST_URI_VARIABLE_PREFIX) ||
                        property.getName().toString().startsWith(RESTConstants.REST_QUERY_PARAM_PREFIX) )) {
                    variables.put(property.getName(), (String) property.getValue());
                }
            }

            String tmpl;
            // This is a special case as we want handle {uri.var.variable} having full URL (except as a path param or query param)
            // this was used in connectors Eg:- uri-template="{uri.var.variable}"
            if(uriTemplate.getTemplate().charAt(0)=='{' && uriTemplate.getTemplate().charAt(1)!='+'){
                tmpl = "{+" + uriTemplate.getTemplate().substring(1);
            } else {
                tmpl = uriTemplate.getTemplate();
            }
            template = UriTemplate.fromTemplate(tmpl);

            if(template != null) {
                template.set(variables);
            }

            if(variables.isEmpty()){
                evaluatedUri = template.getTemplate();
            } else {
                try {
                    URI uri = new URI(template.expand());
                    evaluatedUri = uri.toString();
                    if (log.isDebugEnabled()) {
                        log.debug("Expanded URL : " + evaluatedUri);
                    }
                } catch (URISyntaxException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Invalid URL syntax for HTTP Endpoint: " + this.getName(), e);
                    }
                    evaluatedUri = template.getTemplate();
                } catch(ExpressionParseException e) {
                    log.debug("No URI Template variables defined in HTTP Endpoint: " + this.getName());
                    evaluatedUri = template.getTemplate();
                }
            }
        }


        if (evaluatedUri != null) {
            synCtx.setTo(new EndpointReference(evaluatedUri));
            if (super.getDefinition() != null) {
                synCtx.setProperty(EndpointDefinition.DYNAMIC_URL_VALUE, evaluatedUri);
            }
        }
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public UriTemplate getUriTemplate() {
        return uriTemplate;
    }

    public SynapseXPath getHttpMethodExpression() {
        return httpMethodExpression;
    }

    public void setUriTemplate(UriTemplate uriTemplate) {
        this.uriTemplate = uriTemplate;
    }

    public void setHttpMethodExpression(SynapseXPath httpMethodExpression) {
        this.httpMethodExpression = httpMethodExpression;
    }

    public boolean isLegacySupport() {
        return legacySupport;
    }

    public void setLegacySupport(boolean legacySupport) {
        this.legacySupport = legacySupport;
    }
}
