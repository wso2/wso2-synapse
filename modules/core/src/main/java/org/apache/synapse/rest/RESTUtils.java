/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.apache.synapse.rest;

import org.apache.axis2.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.rest.dispatch.RESTDispatcher;
import org.apache.synapse.rest.dispatch.DefaultDispatcher;
import org.apache.synapse.rest.dispatch.URLMappingBasedDispatcher;
import org.apache.synapse.rest.dispatch.URITemplateBasedDispatcher;
import org.apache.synapse.transport.nhttp.NhttpConstants;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class RESTUtils {

    private static final Log log = LogFactory.getLog(RESTUtils.class);

    private static final List<RESTDispatcher> dispatchers = new ArrayList<RESTDispatcher>();

    static {
        dispatchers.add(new URLMappingBasedDispatcher());
        dispatchers.add(new URITemplateBasedDispatcher());
        dispatchers.add(new DefaultDispatcher());
    }

    public static String trimSlashes(String url) {
        if (url.startsWith("/")) {
            url = url.substring(1);
        }
        if (url.startsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    public static String trimTrailingSlashes(String url) {
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    public static String getFullRequestPath(MessageContext synCtx) {
        Object obj = synCtx.getProperty(RESTConstants.REST_FULL_REQUEST_PATH);
        if (obj != null) {
            return (String) obj;
        }

        org.apache.axis2.context.MessageContext msgCtx = ((Axis2MessageContext) synCtx).
                getAxis2MessageContext();
        String url = (String) msgCtx.getProperty(Constants.Configuration.TRANSPORT_IN_URL);
        if (url == null) {
            url = (String) synCtx.getProperty(NhttpConstants.SERVICE_PREFIX);
        }

        if (url.startsWith("http://") || url.startsWith("https://")) {
            try {
                url = new URL(url).getFile();
            } catch (MalformedURLException e) {
                handleException("Request URL: " + url + " is malformed", e);
            }
        }
        synCtx.setProperty(RESTConstants.REST_FULL_REQUEST_PATH, url);
        return url;
    }

    /**
     * Populate Message context properties for the query parameters extracted from the url
     *
     * @param synCtx MessageContext of the request
     */
    public static void populateQueryParamsToMessageContext(MessageContext synCtx) {

        String path = getFullRequestPath(synCtx);
        String method = (String) synCtx.getProperty(RESTConstants.REST_METHOD);

        int queryIndex = path.indexOf('?');
        if (queryIndex != -1) {
            String query = path.substring(queryIndex + 1);
            String[] entries = query.split(RESTConstants.QUERY_PARAM_DELIMITER);
            String name = null;
            String value;
            for (String entry : entries) {
                int index = entry.indexOf('=');
                if (index != -1) {
                    try {
                        name = entry.substring(0, index);
                        value = URLDecoder.decode(entry.substring(index + 1),
                                RESTConstants.DEFAULT_ENCODING);
                        synCtx.setProperty(RESTConstants.REST_QUERY_PARAM_PREFIX + name, value);
                    } catch (UnsupportedEncodingException uee) {
                        handleException("Error processing " + method + " request for : " + path, uee);
                    } catch (IllegalArgumentException e) {
                        String errorMessage = "Error processing " + method + " request for : " + path
                                + " due to an error in the request sent by the client";
                        synCtx.setProperty(SynapseConstants.ERROR_CODE, HttpStatus.SC_BAD_REQUEST);
                        synCtx.setProperty(SynapseConstants.ERROR_MESSAGE, errorMessage);
                        org.apache.axis2.context.MessageContext inAxisMsgCtx =
                                ((Axis2MessageContext) synCtx).getAxis2MessageContext();
                        inAxisMsgCtx.setProperty(SynapseConstants.HTTP_SC, HttpStatus.SC_BAD_REQUEST);
                        handleException(errorMessage, e);
                    }
                } else {
                    // If '=' sign isn't present in the entry means that the '&' character is part of
                    // the query parameter value. If so query parameter value should be updated appending
                    // the remaining characters.
                    String existingValue = (String) synCtx.getProperty(RESTConstants.REST_QUERY_PARAM_PREFIX + name);
                    value = RESTConstants.QUERY_PARAM_DELIMITER + entry;
                    synCtx.setProperty(RESTConstants.REST_QUERY_PARAM_PREFIX + name, existingValue + value);
                }
            }
        }
    }

    public static String getSubRequestPath(MessageContext synCtx) {
        return (String) synCtx.getProperty(RESTConstants.REST_SUB_REQUEST_PATH);
    }

    public static List<RESTDispatcher> getDispatchers() {
        return dispatchers;
    }

    private static void handleException(String msg, Throwable t) {
        log.error(msg, t);
        throw new SynapseException(msg, t);
    }

}
