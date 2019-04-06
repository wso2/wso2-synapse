/**
 *  Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.transport.passthru.util;

import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.SOAPMessageFormatter;
import org.apache.axis2.util.MessageProcessorSelector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpVersion;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.Pipe;
import org.apache.synapse.transport.passthru.TargetRequest;
import org.apache.synapse.transport.passthru.config.PassThroughConfiguration;
import org.apache.synapse.transport.passthru.config.TargetConfiguration;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
//import org.apache.axis2.util.MessageProcessorSelector;


public class TargetRequestFactory {
    
	private static Log log = LogFactory.getLog(TargetRequestFactory.class);

    public static TargetRequest create(MessageContext msgContext,
                                       HttpRoute route, 
                                       TargetConfiguration configuration) throws AxisFault {
        try {
            String httpMethod = (String) msgContext.getProperty(
                    Constants.Configuration.HTTP_METHOD);
            if (httpMethod == null) {
                httpMethod = "POST";
            }

            // basic request
            Boolean noEntityBody = (Boolean) msgContext.getProperty(PassThroughConstants.NO_ENTITY_BODY);
            
            if(msgContext.getEnvelope().getBody().getFirstElement() != null){
            	noEntityBody  =false;
            }

            EndpointReference epr = PassThroughTransportUtils.getDestinationEPR(msgContext);
            URL url = new URL(epr.getAddress());
            TargetRequest request = new TargetRequest(configuration, route, url, httpMethod,
                    noEntityBody == null || !noEntityBody);

            //this code block is needed to replace the host header in service chaining with REQUEST_HOST_HEADER
            //adding host header since it is not available in response message.
            //otherwise Host header will not replaced after first call
            if (msgContext.getProperty(NhttpConstants.REQUEST_HOST_HEADER) != null) {
                Object headers = msgContext.getProperty(MessageContext.TRANSPORT_HEADERS);
                if(headers != null) {
                    Map headersMap = (Map) headers;
                    if (!headersMap.containsKey(HTTPConstants.HEADER_HOST)) {
                        headersMap.put(HTTPConstants.HEADER_HOST
                                , msgContext.getProperty(NhttpConstants.REQUEST_HOST_HEADER));
                    }
                }
            }

            // headers
            PassThroughTransportUtils.removeUnwantedHeaders(msgContext, configuration);


            Object o = msgContext.getProperty(MessageContext.TRANSPORT_HEADERS);

            if (o != null && o instanceof Map) {
                Map headers = (Map) o;
                for (Object entryObj : headers.entrySet()) {
                    Map.Entry entry = (Map.Entry) entryObj;
                    if (entry.getValue() != null && entry.getKey() instanceof String &&
                        entry.getValue() instanceof String) {
                        if (HTTPConstants.HEADER_HOST.equalsIgnoreCase((String) entry.getKey())
                            && !configuration.isPreserveHttpHeader(HTTPConstants.HEADER_HOST)) {
                            if (msgContext.getProperty(NhttpConstants.REQUEST_HOST_HEADER) != null) {
                                request.addHeader((String) entry.getKey(),
                                                  (String) msgContext.getProperty(NhttpConstants.REQUEST_HOST_HEADER));
                            }

                        } else {
                            request.addHeader((String) entry.getKey(), (String) entry.getValue());
                        }
                    }
                }
            }

			String cType = getContentType(msgContext, configuration.isPreserveHttpHeader(HTTP.CONTENT_TYPE));
			if (cType != null && !httpMethod.equals(HTTPConstants.HTTP_METHOD_GET)
					&& RelayUtils.shouldOverwriteContentType(msgContext, request)) {
				String messageType = (String) msgContext.getProperty(NhttpConstants.MESSAGE_TYPE);
				if (messageType != null) {
					boolean builderInvoked = false;
					final Pipe pipe = (Pipe) msgContext
							.getProperty(PassThroughConstants.PASS_THROUGH_PIPE);
					if (pipe != null) {
						builderInvoked = Boolean.TRUE.equals(msgContext
								.getProperty(PassThroughConstants.MESSAGE_BUILDER_INVOKED));
					}

					// if multipart related message type and unless if message
					// not get build we should
					// skip of setting formatter specific content Type
					if (messageType.indexOf(HTTPConstants.MEDIA_TYPE_MULTIPART_RELATED) == -1
							&& messageType.indexOf(HTTPConstants.MEDIA_TYPE_MULTIPART_FORM_DATA) == -1) {
						Map msgCtxheaders = (Map) o;
						if (msgCtxheaders != null && !cType.isEmpty()) {
							msgCtxheaders.put(HTTP.CONTENT_TYPE, cType);
						}
						request.addHeader(HTTP.CONTENT_TYPE, cType);
					}

					// if messageType is related to multipart and if message
					// already built we need to set new
					// boundary related content type at Content-Type header
					if (builderInvoked
							&& (((messageType.indexOf(HTTPConstants.MEDIA_TYPE_MULTIPART_RELATED) != -1) 
									|| (messageType.indexOf(HTTPConstants.MEDIA_TYPE_MULTIPART_FORM_DATA) != -1)))) {
						request.addHeader(HTTP.CONTENT_TYPE, cType);
					}

				} else {
					request.addHeader(HTTP.CONTENT_TYPE, cType);
				}

			}

            // version
            String forceHttp10 = (String) msgContext.getProperty(PassThroughConstants.FORCE_HTTP_1_0);
            if ("true".equals(forceHttp10)) {
                request.setVersion(HttpVersion.HTTP_1_0);
            }

            // keep alive
            String noKeepAlive = (String) msgContext.getProperty(PassThroughConstants.NO_KEEPALIVE);
            if ("true".equals(noKeepAlive) || PassThroughConfiguration.getInstance().isKeepAliveDisabled()) {
                request.setKeepAlive(false);
            }

            // port
            int port = url.getPort();
            request.setPort(port);

            // chunk
            String disableChunking = (String) msgContext.getProperty(
                    PassThroughConstants.DISABLE_CHUNKING);
            if ("true".equals(disableChunking)) {
                request.setChunk(false);
            }

            // full url
            String fullUrl = (String) msgContext.getProperty(PassThroughConstants.FULL_URI);
            if ("true".equals(fullUrl)) {
                request.setFullUrl(true);                
            }
            
            // Add excess respsonse header.
            String excessProp = NhttpConstants.EXCESS_TRANSPORT_HEADERS;
            Map excessHeaders = (Map) msgContext.getProperty(excessProp);
            if (excessHeaders != null) {
                    for (Iterator iterator = excessHeaders.keySet().iterator(); iterator.hasNext();) {
                            String key = (String) iterator.next();
                            for (String excessVal : (Collection<String>) excessHeaders.get(key)) {
                                    request.addHeader(key, (String) excessVal);
                            }
                    }
            }

            return request;
        } catch (MalformedURLException e) {
            handleException("Invalid to address" + msgContext.getTo().getAddress(), e);
        }

        return null;
    }

    private static String getContentType(MessageContext msgCtx, boolean isContentTypePreservedHeader) throws AxisFault {

        Object trpHeaders = msgCtx.getProperty(MessageContext.TRANSPORT_HEADERS);

        //If incoming transport isn't HTTP, transport headers can be null. Therefore null check is required
        // and if headers not null check whether request comes with Content-Type header before preserving Content-Type
        //Need to avoid this for multipart headers, need to add MIME Boundary property
        if (trpHeaders != null && trpHeaders instanceof Map && ((Map) trpHeaders).
                get(HTTPConstants.HEADER_CONTENT_TYPE) != null && isContentTypePreservedHeader && !isMultipartContent
                (((Map) trpHeaders).get(HTTPConstants.HEADER_CONTENT_TYPE).toString())) {
            if (msgCtx.getProperty(Constants.Configuration.CONTENT_TYPE) != null) {
                return (String) msgCtx.getProperty(Constants.Configuration.CONTENT_TYPE);
            } else if (msgCtx.getProperty(Constants.Configuration.MESSAGE_TYPE) != null) {
                return (String) msgCtx.getProperty(Constants.Configuration.MESSAGE_TYPE);
            }
        }

        MessageFormatter formatter = MessageProcessorSelector.getMessageFormatter(msgCtx);
        OMOutputFormat format = PassThroughTransportUtils.getOMOutputFormat(msgCtx);
        
        if (formatter != null) {
            String contentType= formatter.getContentType(msgCtx, format, msgCtx.getSoapAction());
            return contentType;
            
        } else {
            String contentType = (String) msgCtx.getProperty(Constants.Configuration.CONTENT_TYPE);
            if (contentType != null) {
                return contentType;
            } else {
                return new SOAPMessageFormatter().getContentType(
                        msgCtx, format,  msgCtx.getSoapAction());
            }
        }
    }

    /**
     * Throws an AxisFault if an error occurs at this level
     * @param s a message describing the error
     * @param e original exception leads to the error condition
     * @throws org.apache.axis2.AxisFault wrapping the original exception
     */
    private static void handleException(String s, Exception e) throws AxisFault {
        log.error(s, e);
        throw new AxisFault(s, e);
    }

    /**
     * Check whether the content type is multipart or not
     * @param contentType
     * @return true for multipart content types
     */
    public static boolean isMultipartContent(String contentType) {
        if (contentType.contains(HTTPConstants.MEDIA_TYPE_MULTIPART_FORM_DATA)
            || contentType.contains(HTTPConstants.HEADER_ACCEPT_MULTIPART_RELATED)) {
            return true;
        }
        return false;
    }
}
