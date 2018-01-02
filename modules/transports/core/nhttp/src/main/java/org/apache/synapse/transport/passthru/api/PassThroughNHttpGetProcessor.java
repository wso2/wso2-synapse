/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.transport.passthru.api;

import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.entity.ContentOutputStream;
import org.apache.http.nio.util.SimpleOutputBuffer;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.transport.nhttp.NHttpConfiguration;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.transport.passthru.HttpGetRequestProcessor;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.ProtocolState;
import org.apache.synapse.transport.passthru.SourceContext;
import org.apache.synapse.transport.passthru.SourceHandler;
import org.apache.ws.commons.schema.XmlSchema;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class PassThroughNHttpGetProcessor implements HttpGetRequestProcessor {

	private static final Log log = LogFactory.getLog(PassThroughNHttpGetProcessor.class);

	 
	private static final String LOCATION = "Location";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String TEXT_HTML = "text/html";
	private static final String TEXT_XML = "text/xml";
	private static final String PASS_THROUGH_RESPONSE_SOURCE_BUFFER = "synapse.response-source-buffer";
	private static final String GET_REQUEST_HANDLED = "GET_REQUEST_HANDLED";

	protected ConfigurationContext cfgCtx;

	protected SourceHandler sourceHandler;

	public void init(ConfigurationContext cfgCtx, SourceHandler handler)
			throws AxisFault {

		this.cfgCtx = cfgCtx;
		this.sourceHandler = handler;
	}

	public void process(HttpRequest request, HttpResponse response,
			MessageContext msgContext, NHttpServerConnection conn,
			OutputStream ostream, boolean isRestDispatching) {

		String uri = request.getRequestLine().getUri();
		String serviceName = getServiceName(request);

		Map<String, String> parameters = new HashMap<String, String>();
		int pos = uri.indexOf("?");
		if (pos != -1) {
			msgContext.setTo(new EndpointReference(uri.substring(0, pos)));
			StringTokenizer st = new StringTokenizer(uri.substring(pos + 1),
                                                     "&");
			while (st.hasMoreTokens()) {
				String param = st.nextToken();
				pos = param.indexOf("=");
				if (pos != -1) {
					parameters.put(param.substring(0, pos),
							param.substring(pos + 1));
				} else {
					parameters.put(param, null);
				}
			}
		} else {
			msgContext.setTo(new EndpointReference(uri));
		}

        SimpleOutputBuffer outputBuffer = (SimpleOutputBuffer) conn.getContext().getAttribute(
                PASS_THROUGH_RESPONSE_SOURCE_BUFFER);
        ContentOutputStream os = new ContentOutputStream(outputBuffer);

		if (isServiceListBlocked(uri)) {
            sendResponseAndFinish(response, HttpStatus.SC_FORBIDDEN, conn, os, msgContext);
		} else if (uri.equals("/favicon.ico")) {
			response.addHeader(LOCATION, "http://ws.apache.org/favicon.ico");
            sendResponseAndFinish(response, HttpStatus.SC_MOVED_PERMANENTLY, conn, os, msgContext);
		} else if (serviceName != null && parameters.containsKey("wsdl")) {
			generateWsdl(response, msgContext, conn, os, serviceName, parameters);
		} else if (serviceName != null && parameters.containsKey("wsdl2")) {
			generateWsdl2(response, msgContext, conn, os, serviceName);
		} else if (serviceName != null && parameters.containsKey("xsd")) {
			generateXsd(response, msgContext, conn, os, serviceName, parameters);
		} else {
			msgContext.setProperty(PassThroughConstants.REST_GET_DELETE_INVOKE, true);
		}
	}

    private void sendResponseAndFinish(HttpResponse response, int status,
                                       NHttpServerConnection conn, OutputStream os,
                                       MessageContext msgContext) {
        response.setStatusCode(status);
        SourceContext.updateState(conn, ProtocolState.WSDL_RESPONSE_DONE);
        sourceHandler.commitResponseHideExceptions(conn, response);
        closeOutputStream(os);
        msgContext.setProperty(GET_REQUEST_HANDLED, Boolean.TRUE);
    }

    private void sendResponseAndFinish(HttpResponse response, byte[] data,
                                       NHttpServerConnection conn, OutputStream os,
                                       MessageContext msgContext) throws IOException {
        SourceContext.updateState(conn, ProtocolState.WSDL_RESPONSE_DONE);
        sourceHandler.commitResponseHideExceptions(conn, response);
        write(conn, os, data);
        closeOutputStream(os);
        msgContext.setProperty(GET_REQUEST_HANDLED, Boolean.TRUE);
    }

	private void closeOutputStream(OutputStream os) {
		try {
			os.flush();
			os.close();
		} catch (IOException ignore) {
		}
	}

	/**
	 * Generate WSDL.
	 *
	 * @param response
	 *            HttpResponse
	 * @param msgContext
	 *            MessageContext
	 * @param conn
	 *            NHttpServerConnection
	 * @param os
	 *            OutputStream
	 * @param serviceName
	 *            service name
	 * @param parameters
	 *            parameters
	 */
	protected void generateWsdl(HttpResponse response,
			MessageContext msgContext, NHttpServerConnection conn,
			OutputStream os, String serviceName,
			Map<String, String> parameters) {
		AxisService service = cfgCtx.getAxisConfiguration().getServices().get(serviceName);
		if (service != null) {
			try {
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				String parameterValue = parameters.get("wsdl");
				if (parameterValue == null) {
					service.printWSDL(output, getIpAddress());
				} else {
					// here the parameter value should be the wsdl file name
					service.printUserWSDL(output, parameterValue);
				}

                response.addHeader(CONTENT_TYPE, TEXT_XML);
                sendResponseAndFinish(response, output.toByteArray(), conn, os, msgContext);

			} catch (Exception e) {
				handleBrowserException(response, msgContext, conn, os,
						"Error generating ?wsdl output for service : " + serviceName, e);
			}
		} else {
            if (log.isDebugEnabled()) {
                log.debug("Unable to find service: " + serviceName + " for WSDL generation.");
            }
			msgContext.setProperty(PassThroughConstants.REST_GET_DELETE_INVOKE, true);
		}
	}

	/**
	 * Generate WSDL2.
	 * 
	 * @param response
	 *            HttpResponse
	 * @param msgContext
	 *            MessageContext
	 * @param conn
	 *            NHttpServerConnection
	 * @param os
	 *            OutputStream
	 * @param serviceName
	 *            service name
	 */
	protected void generateWsdl2(HttpResponse response,
			MessageContext msgContext, NHttpServerConnection conn,
			OutputStream os, String serviceName) {
		AxisService service = cfgCtx.getAxisConfiguration().getServices()
                                    .get(serviceName);
		if (service != null) {
			String parameterValue = (String) service
					.getParameterValue("serviceType");
			if ("proxy".equals(parameterValue)
					&& !isWSDLProvidedForProxyService(service)) {
				handleBrowserException(response, msgContext, conn, os,
						"No WSDL was provided for the Service " + serviceName
								+ ". A WSDL cannot be generated.", null);
			}
			try {
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				service.printWSDL2(output, getIpAddress());
				response.addHeader(CONTENT_TYPE, TEXT_XML);
                sendResponseAndFinish(response, output.toByteArray(), conn, os, msgContext);
			} catch (Exception e) {
				handleBrowserException(response, msgContext, conn, os,
						"Error generating ?wsdl2 output for service : "
								+ serviceName, e);
			}
		} else {
			msgContext.setProperty(PassThroughConstants.REST_GET_DELETE_INVOKE, true);
		}
	}

	/**
	 * Returns the service name.
	 * 
	 * @param request
	 *            HttpRequest
	 * @return service name as a String
	 */
	protected String getServiceName(HttpRequest request) {
		String uri = request.getRequestLine().getUri();

		String servicePath = cfgCtx.getServiceContextPath();
		if (!servicePath.startsWith("/")) {
			servicePath = "/" + servicePath;
		}

		String serviceName = null;
		if (uri.startsWith(servicePath)) {
			serviceName = uri.substring(servicePath.length());
			if (serviceName.startsWith("/")) {
				serviceName = serviceName.substring(1);
			}
			if (serviceName.contains("?")) {
				serviceName = serviceName
						.substring(0, serviceName.indexOf("?"));
			}
		} else {
			// this may be a custom URI
			String incomingURI = request.getRequestLine().getUri();

			Map serviceURIMap = (Map) cfgCtx
					.getProperty(NhttpConstants.EPR_TO_SERVICE_NAME_MAP);
			if (serviceURIMap != null) {
				Set keySet = serviceURIMap.keySet();
				for (Object key : keySet) {
					if (incomingURI.toLowerCase().contains(
							((String) key).toLowerCase())) {
						return (String) serviceURIMap.get(key);
					}
				}
			}
		}

		if (serviceName != null) {
			int opnStart = serviceName.indexOf("/");
			if (opnStart != -1) {
				serviceName = serviceName.substring(0, opnStart);
			}
		}
		return serviceName;
	}

	/**
	 * Generates Schema.
	 *
	 * @param response
	 *            HttpResponse
	 * @param msgContext
	 *            Current MessageContext
	 * @param conn
	 *            NHttpServerConnection
	 * @param os
	 *            OutputStream
	 * @param serviceName
	 *            service name
	 * @param parameters
	 *            url parameters
	 */
	protected void generateXsd(HttpResponse response,
			MessageContext msgContext, NHttpServerConnection conn,
			OutputStream os, String serviceName,
			Map<String, String> parameters) {
		if (parameters.get("xsd") == null || "".equals(parameters.get("xsd"))) {
			AxisService service = cfgCtx.getAxisConfiguration().getServices()
                                        .get(serviceName);
			if (service != null) {
				try {
					ByteArrayOutputStream output = new ByteArrayOutputStream();
					service.printSchema(output);
					response.addHeader(CONTENT_TYPE, TEXT_XML);
                    sendResponseAndFinish(response, output.toByteArray(), conn, os, msgContext);
				} catch (Exception e) {
					handleBrowserException(response, msgContext, conn, os,
							"Error generating ?xsd output for service : "
									+ serviceName, e);
				}
			} else {
				msgContext.setProperty(PassThroughConstants.REST_GET_DELETE_INVOKE, true);
			}

		} else {
			// cater for named XSDs - check for the xsd name
			String schemaName = parameters.get("xsd");
			AxisService service = cfgCtx.getAxisConfiguration().getServices()
                                        .get(serviceName);

			if (service != null) {
				// run the population logic just to be sure
				service.populateSchemaMappings();
				// write out the correct schema
				Map schemaTable = service.getSchemaMappingTable();
				XmlSchema schema = (XmlSchema) schemaTable.get(schemaName);
				if (schema == null) {
					int dotIndex = schemaName.indexOf('.');
					if (dotIndex > 0) {
						String schemaKey = schemaName.substring(0, dotIndex);
						schema = (XmlSchema) schemaTable.get(schemaKey);
					}
				}
				// schema found - write it to the stream
				if (schema != null) {
					try {
						ByteArrayOutputStream output = new ByteArrayOutputStream();
						schema.write(output);
						response.addHeader(CONTENT_TYPE, TEXT_XML);
						sourceHandler.commitResponseHideExceptions(conn,
								response);
                        write(conn, os, output.toByteArray());
						closeOutputStream(os);
                        msgContext.setProperty(GET_REQUEST_HANDLED, Boolean.TRUE);
					} catch (Exception e) {
						handleBrowserException(response, msgContext, conn, os,
								"Error generating named ?xsd output for service : "
										+ serviceName, e);
					}

				} else {
					// no schema available by that name - send 404
					response.setStatusCode(HttpStatus.SC_NOT_FOUND);
					closeOutputStream(os);
                    msgContext.setProperty(GET_REQUEST_HANDLED, Boolean.TRUE);
				}
			} else {
				msgContext.setProperty(PassThroughConstants.REST_GET_DELETE_INVOKE, true);
			}
		}
	}

	/**
     * Handles browser exception.
     *
     * @param response HttpResponse
     * @param conn     NHttpServerConnection
     * @param os       OutputStream
     * @param msg      message
     * @param e        Exception
     */
    protected void handleBrowserException(HttpResponse response, MessageContext msgContext,
                                          NHttpServerConnection conn, OutputStream os,
                                          String msg, Exception e) {
        if (e == null) {
            log.error(msg);
        } else {
            log.error(msg, e);
        }

        if (!response.containsHeader(HTTP.TRANSFER_ENCODING)) {
            response.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
            response.setReasonPhrase(msg);
            response.addHeader(CONTENT_TYPE, TEXT_HTML);
            sourceHandler.commitResponseHideExceptions(conn, response);
            try {
                write(conn, os, msg.getBytes());
                os.close();
            } catch (IOException ignore) {
            }
        }

        if (conn != null) {
            try {
                conn.shutdown();
            } catch (IOException ignore) {
            }
        }
        msgContext.setProperty(GET_REQUEST_HANDLED, Boolean.TRUE);
    }
    
    
	/**
	 * Is the incoming URI is requesting service list and
	 * http.block_service_list=true in nhttp.properties
	 * 
	 * @param incomingURI
	 *            incoming URI
	 * @return whether to proceed with incomingURI
	 */
	protected boolean isServiceListBlocked(String incomingURI) {
		String isBlocked = NHttpConfiguration.getInstance().isServiceListBlocked();

		return (("/services").equals(incomingURI) || ("/services" + "/")
				.equals(incomingURI)) && Boolean.parseBoolean(isBlocked);
	}
	
	
    /**
     * Checks whether a wsdl is provided for a proxy service.
     *
     * @param service AxisService
     * @return whether the wsdl is provided or not
     */
    protected boolean isWSDLProvidedForProxyService(AxisService service) {
        boolean isWSDLProvided = false;
        if (service.getParameterValue(WSDLConstants.WSDL_4_J_DEFINITION) != null ||
                service.getParameterValue(WSDLConstants.WSDL_20_DESCRIPTION) != null) {
            isWSDLProvided = true;
        }
        return isWSDLProvided;
    }

	
	   /**
     * Whatever this method returns as the IP is ignored by the actual http/s listener when
     * its getServiceEPR is invoked. This was originally copied from axis2
     *
     * @return Returns String.
     * @throws java.net.SocketException if the socket can not be accessed
     */
    protected static String getIpAddress() throws SocketException {
        Enumeration e = NetworkInterface.getNetworkInterfaces();
        String address = "127.0.0.1";

        while (e.hasMoreElements()) {
            NetworkInterface networkInterface = (NetworkInterface) e.nextElement();
            Enumeration addresses = networkInterface.getInetAddresses();

            while (addresses.hasMoreElements()) {
                InetAddress ip = (InetAddress) addresses.nextElement();
                if (!ip.isLoopbackAddress() && isIP(ip.getHostAddress())) {
                    return ip.getHostAddress();
                }
            }
        }
        return address;
    }
    
    protected static boolean isIP(String hostAddress) {
        return hostAddress.split("[.]").length == 4;
    }

    private void write(NHttpServerConnection conn, OutputStream os,
                       byte[] data) throws IOException {
        synchronized (conn.getContext()) {
            // The SimpleOutputBuffer on which this output stream is based is not thread safe.
            // Explicit synchronization required.
            // Do not worry about running out of buffer space.
            // SimpleOutputBuffer expands to fit the data.
            os.write(data);
        }
    }

}
