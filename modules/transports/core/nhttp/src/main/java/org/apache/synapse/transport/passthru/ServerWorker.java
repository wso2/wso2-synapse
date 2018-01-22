/*
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

package org.apache.synapse.transport.passthru;

import java.io.OutputStream;
import java.net.InetAddress;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.xml.parsers.FactoryConfigurationError;


import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.soap.SOAPEnvelope;
import org.apache.axiom.soap.SOAPFactory;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11Factory;
import org.apache.axiom.util.UIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.builder.BuilderUtil;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.dispatchers.RequestURIBasedDispatcher;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.transport.RequestResponseTransport;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HTTPTransportUtils;
import org.apache.axis2.util.MessageContextBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.reactor.ssl.SSLIOSession;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.commons.util.ext.TenantInfoInitiator;
import org.apache.synapse.commons.util.ext.TenantInfoInitiatorProvider;
import org.apache.synapse.transport.customlogsetter.CustomLogSetter;
import org.apache.synapse.transport.http.conn.SynapseDebugInfoHolder;
import org.apache.synapse.transport.nhttp.HttpCoreRequestResponseTransport;
import org.apache.synapse.transport.nhttp.NHttpConfiguration;
import org.apache.synapse.transport.nhttp.NhttpConstants;
import org.apache.synapse.transport.nhttp.util.NhttpUtil;
import org.apache.synapse.transport.nhttp.util.RESTUtil;
import org.apache.synapse.transport.passthru.config.PassThroughConfiguration;
import org.apache.synapse.transport.passthru.config.SourceConfiguration;
import org.apache.synapse.transport.passthru.util.SourceResponseFactory;

/**
 * This is a worker thread for executing an incoming request in to the transport.
 */
public class ServerWorker implements Runnable {

  	private static final Log log = LogFactory.getLog(ServerWorker.class);
    /** the incoming message to be processed */
    private org.apache.axis2.context.MessageContext msgContext = null;
    /** the http request */
    private SourceRequest request = null;
    /** The configuration of the receiver */
    private SourceConfiguration sourceConfiguration = null;

    private static final String SOAP_ACTION_HEADER = "SOAPAction";
        
    /** WSDL processor for Get requests */
    private HttpGetRequestProcessor httpGetRequestProcessor = null;
    
    /** Weather we should do rest dispatching or not */
    private boolean isRestDispatching = true;
    
    private OutputStream os; //only used for WSDL  requests..
  
    public ServerWorker(final SourceRequest request,
                        final SourceConfiguration sourceConfiguration,final OutputStream os) {
        this.request = request;
        this.sourceConfiguration = sourceConfiguration;

        this.msgContext = createMessageContext(null, request);
        
        this.httpGetRequestProcessor = sourceConfiguration.getHttpGetRequestProcessor();
        
        this.os = os;

        // set these properties to be accessed by the engine
        msgContext.setProperty(
                PassThroughConstants.PASS_THROUGH_SOURCE_REQUEST, request);
        msgContext.setProperty(
                PassThroughConstants.PASS_THROUGH_SOURCE_CONFIGURATION, sourceConfiguration);
        msgContext.setProperty(PassThroughConstants.PASS_THROUGH_SOURCE_CONNECTION,
                request.getConnection());
        msgContext.setProperty(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY,
                               request.getConnection().getContext().getAttribute(SynapseDebugInfoHolder.SYNAPSE_WIRE_LOG_HOLDER_PROPERTY));
        request.getConnection().getContext().setAttribute(NhttpConstants.SERVER_WORKER_INIT_TIME,
                System.currentTimeMillis());
    }

    public void run() {
        try {
            CustomLogSetter.getInstance().clearThreadLocalContent();
            TenantInfoInitiator tenantInfoInitiator = TenantInfoInitiatorProvider.getTenantInfoInitiator();
            if (tenantInfoInitiator != null) {
                tenantInfoInitiator.initTenantInfo(request.getUri());
            }
            request.getConnection().getContext().setAttribute(NhttpConstants.SERVER_WORKER_START_TIME, System.currentTimeMillis());
            if (log.isDebugEnabled()) {
                log.debug("Starting a new Server Worker instance");
            }
            String method =
                    request.getRequest() != null ? request.getRequest().getRequestLine().getMethod().toUpperCase() : "";

            processHttpRequestUri(msgContext, method);

            //For requests to fetch wsdl, return the message flow without going through the normal flow
            if (isRequestToFetchWSDL()) {
                return;
            }

            //need special case to handle REST
            boolean isRest = isRESTRequest(msgContext, method);

            //should be process normally
            if (!isRest) {
                if (request.isEntityEnclosing()) {
                    processEntityEnclosingRequest(msgContext, true);
                } else {
                    processNonEntityEnclosingRESTHandler(null, msgContext, true);
                }
            } else {
                String contentTypeHeader = request.getHeaders().get(HTTP.CONTENT_TYPE);
                SOAPEnvelope soapEnvelope = this.handleRESTUrlPost(contentTypeHeader);
                processNonEntityEnclosingRESTHandler(soapEnvelope, msgContext, true);
            }

            sendAck(msgContext);
        } finally {
            cleanup();
        }
    }

    private boolean isRequestToFetchWSDL() {
        //if WSDL done then moved out rather than hand over to entity handle methods.
        SourceContext info = (SourceContext) request.getConnection().getContext().
                getAttribute(SourceContext.CONNECTION_INFORMATION);
        if (info != null && info.getState().equals(ProtocolState.WSDL_RESPONSE_DONE) ||
            (msgContext.getProperty(PassThroughConstants.WSDL_GEN_HANDLED) != null &&
             Boolean.TRUE.equals((msgContext.getProperty(PassThroughConstants.WSDL_GEN_HANDLED))))) {
            return true;
        }
        return false;
    }

	/**
	 * Method will setup the necessary parameters for the rest url post action
	 * 
	 * @param
	 * @return
	 * @throws FactoryConfigurationError
	 */
	public SOAPEnvelope handleRESTUrlPost(String contentTypeHdr) throws FactoryConfigurationError {
	    SOAPEnvelope soapEnvelope = null;
        String contentType = contentTypeHdr != null ? TransportUtils.getContentType(contentTypeHdr, msgContext) : null;
        // When POST request doesn't contain a Content-Type,
        // recipient should consider it as application/octet-stream (rfc2616)
        if (contentType == null || contentType.isEmpty()) {
            contentType = PassThroughConstants.APPLICATION_OCTET_STREAM;
        }
        if (HTTPConstants.MEDIA_TYPE_X_WWW_FORM.equals(contentType) ||
                (PassThroughConstants.APPLICATION_OCTET_STREAM.equals(contentType) && contentTypeHdr == null)) {
            msgContext.setTo(new EndpointReference(request.getRequest().getRequestLine().getUri()));
	        msgContext.setProperty(Constants.Configuration.CONTENT_TYPE,contentType);
	        String charSetEncoding = BuilderUtil.getCharSetEncoding(contentType);
		    msgContext.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING, charSetEncoding);
	        try {
	            RESTUtil.dispatchAndVerify(msgContext);
	        } catch (AxisFault e1) {
	        	log.error("Error while building message for REST_URL request",e1);
	        }
	        
	       
			try {
				/**
				 * This reverseProxyMode was introduce to avoid the LB exposing
				 * it's own web service when REST call was initiated
				 */
				boolean reverseProxyMode = NHttpConfiguration.getInstance().isReverseProxyMode();
				AxisService axisService = null;
				if (!reverseProxyMode) {
					RequestURIBasedDispatcher requestDispatcher = new RequestURIBasedDispatcher();
					axisService = requestDispatcher.findService(msgContext);
				}				

				// the logic determines which service dispatcher to get invoke,
				// this will be determine
				// based on parameter defines at disableRestServiceDispatching,
				// and if super tenant invoke, with isTenantRequest
				// identifies whether the request to be dispatch to custom REST
				// Dispatcher Service.

                boolean isCustomRESTDispatcher = false;
                String requestURI = request.getRequest().getRequestLine().getUri();
                if (requestURI.matches(NHttpConfiguration.getInstance().getRestUriApiRegex())
                        || requestURI.matches(NHttpConfiguration.getInstance().getRestUriProxyRegex())) {
                    isCustomRESTDispatcher = true;
                }

                if (!isCustomRESTDispatcher) {
                    if (axisService == null) {
                        String defaultSvcName = NHttpConfiguration.getInstance().getNhttpDefaultServiceName();
                        axisService = msgContext.getConfigurationContext().getAxisConfiguration().
                                getService(defaultSvcName);
                        msgContext.setAxisService(axisService);
                    }
                } else {
                    String multiTenantDispatchService = PassThroughConfiguration.getInstance().getRESTDispatchService();
                    axisService = msgContext.getConfigurationContext().getAxisConfiguration().getService(multiTenantDispatchService);
                    msgContext.setAxisService(axisService);
                }
            } catch (AxisFault e) {
				handleException("Error processing " + request.getMethod() + " request for : " + request.getUri(), e);
			}
			
			
	        try {
	        	 soapEnvelope = TransportUtils.createSOAPMessage(msgContext, null, contentType);
	           } catch (Exception e) {
	        	log.error("Error while building message for REST_URL request");
	        }
	       //msgContext.setProperty(Constants.Configuration.CONTENT_TYPE,"application/xml");  
	       msgContext.setProperty(Constants.Configuration.MESSAGE_TYPE,HTTPConstants.MEDIA_TYPE_APPLICATION_XML);   
	    }
	    return soapEnvelope;
    }

    public  void sendAck(MessageContext msgContext) {
        String respWritten = "";
        if (msgContext.getOperationContext() != null) {
            respWritten = (String) msgContext.getOperationContext().getProperty(
                    Constants.RESPONSE_WRITTEN);
        }
        
        if(msgContext.getProperty(PassThroughConstants.FORCE_SOAP_FAULT) != null){
        	respWritten ="SKIP";
        }
        
        boolean respWillFollow = !Constants.VALUE_TRUE.equals(respWritten)
                && !"SKIP".equals(respWritten);
        boolean ack = (((RequestResponseTransport) msgContext.getProperty(
                    RequestResponseTransport.TRANSPORT_CONTROL)).getStatus()
                    == RequestResponseTransport.RequestResponseTransportStatus.ACKED);
        boolean forced = msgContext.isPropertyTrue(NhttpConstants.FORCE_SC_ACCEPTED);
        boolean nioAck = msgContext.isPropertyTrue("NIO-ACK-Requested", false);
        if (respWillFollow || ack || forced || nioAck) {
            NHttpServerConnection conn = request.getConnection();
            SourceResponse sourceResponse;
            if (!nioAck) {
                msgContext.removeProperty(MessageContext.TRANSPORT_HEADERS);
                sourceResponse = SourceResponseFactory.create(msgContext,
                        request, sourceConfiguration);
                sourceResponse.setStatus(HttpStatus.SC_ACCEPTED);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Sending ACK response with status "
                            + msgContext.getProperty(NhttpConstants.HTTP_SC)
                            + ", for MessageID : " + msgContext.getMessageID());
                }
                sourceResponse = SourceResponseFactory.create(msgContext,
                        request, sourceConfiguration);
                sourceResponse.setStatus(Integer.parseInt(
                        msgContext.getProperty(NhttpConstants.HTTP_SC).toString()));
            }

            SourceContext.setResponse(conn, sourceResponse);
            ProtocolState state = SourceContext.getState(conn);
            if (state != null && state.compareTo(ProtocolState.REQUEST_DONE) <= 0) {
                conn.requestOutput();
            } else {
                SourceContext.updateState(conn, ProtocolState.CLOSED);
                sourceConfiguration.getSourceConnections().shutDownConnection(conn);
            }
        }
    }

    public void processNonEntityEnclosingRESTHandler(SOAPEnvelope soapEnvelope, MessageContext msgContext, boolean injectToAxis2Engine) {
        String soapAction = request.getHeaders().get(SOAP_ACTION_HEADER);
        if ((soapAction != null) && soapAction.startsWith("\"") && soapAction.endsWith("\"")) {
            soapAction = soapAction.substring(1, soapAction.length() - 1);
        }

        msgContext.setSoapAction(soapAction);
        msgContext.setTo(new EndpointReference(request.getUri()));
        msgContext.setServerSide(true);
        msgContext.setDoingREST(true);
        if (!(request.isEntityEnclosing())) {
        	msgContext.setProperty(PassThroughConstants.NO_ENTITY_BODY, Boolean.TRUE);
        }
        
        try {
        	if(soapEnvelope == null){
        		 msgContext.setEnvelope(new SOAP11Factory().getDefaultEnvelope());
        	}else{
        		 msgContext.setEnvelope(soapEnvelope);
        	}


            if (injectToAxis2Engine) {
                AxisEngine.receive(msgContext);
            }
        } catch (AxisFault axisFault) {
            handleException("Error processing " + request.getMethod() +
                " request for : " + request.getUri(), axisFault);
        } catch (Exception e) {
            handleException(
                    "Error processing " + request.getMethod() + " request for : "
                            + request.getUri() + ". Error detail: " + e.getMessage() + ". ", e);          
        }
    }

    public  void processEntityEnclosingRequest(MessageContext msgContext, boolean injectToAxis2Engine) {
        try {
            String contentTypeHeader = request.getHeaders().get(HTTP.CONTENT_TYPE);
            contentTypeHeader = contentTypeHeader != null ? contentTypeHeader : inferContentType();

            String charSetEncoding = null;
            String contentType = null;

			if (contentTypeHeader != null) {
				charSetEncoding = BuilderUtil.getCharSetEncoding(contentTypeHeader);
				contentType = TransportUtils.getContentType(contentTypeHeader, msgContext);
			}
            // get the contentType of char encoding
            if (charSetEncoding == null) {
                charSetEncoding = MessageContext.DEFAULT_CHAR_SET_ENCODING;
            }
            String method = request.getRequest() != null ? request.getRequest().getRequestLine().getMethod().toUpperCase():"";
            

  

            msgContext.setTo(new EndpointReference(request.getUri()));
            msgContext.setProperty(HTTPConstants.HTTP_METHOD, method);
            msgContext.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING, charSetEncoding);
            msgContext.setServerSide(true);
            
            msgContext.setProperty(Constants.Configuration.CONTENT_TYPE, contentTypeHeader);
            msgContext.setProperty(Constants.Configuration.MESSAGE_TYPE, contentType);

            if (contentTypeHeader == null || HTTPTransportUtils.isRESTRequest(contentTypeHeader) || isRest(contentTypeHeader)) {
                msgContext.setProperty(PassThroughConstants.REST_REQUEST_CONTENT_TYPE, contentType);
                msgContext.setDoingREST(true);
                SOAPEnvelope soapEnvelope = this.handleRESTUrlPost(contentTypeHeader);
                msgContext.setProperty(PassThroughConstants.PASS_THROUGH_PIPE, request.getPipe());
                processNonEntityEnclosingRESTHandler(soapEnvelope, msgContext, injectToAxis2Engine);
    			return;
            } else {
                String soapAction = request.getHeaders().get(SOAP_ACTION_HEADER);

                int soapVersion = HTTPTransportUtils.
                        initializeMessageContext(msgContext, soapAction,
                                request.getUri(), contentTypeHeader);
                SOAPEnvelope envelope;

                if (soapVersion == 1) {
                    SOAPFactory fac = OMAbstractFactory.getSOAP11Factory();
                    envelope = fac.getDefaultEnvelope();
                } else if (soapVersion == 2) {
                    SOAPFactory fac = OMAbstractFactory.getSOAP12Factory();
                    envelope = fac.getDefaultEnvelope();
                } else {
                    SOAPFactory fac = OMAbstractFactory.getSOAP12Factory();
                    envelope = fac.getDefaultEnvelope();
                }

                if ((soapAction != null) && soapAction.startsWith("\"") && soapAction.endsWith("\"")) {
                    soapAction = soapAction.substring(1, soapAction.length() - 1);
                    msgContext.setSoapAction(soapAction);
                }

                msgContext.setEnvelope(envelope);
            }
            
           
            msgContext.setProperty(PassThroughConstants.PASS_THROUGH_PIPE, request.getPipe());
            if (injectToAxis2Engine) {
                AxisEngine.receive(msgContext);
            }
        } catch (AxisFault axisFault) {
            handleException("Error processing " + request.getMethod() +
                " request for : " + request.getUri(), axisFault);
        } catch (Exception e) {
            handleException(
                    "Error processing " + request.getMethod() + " request for : "
                            + request.getUri() + ". Error detail: " + e.getMessage() + ". ", e);
        }
    }
    
    
    private boolean isRest(String contentType) {
        return contentType != null &&
                contentType.indexOf(SOAP11Constants.SOAP_11_CONTENT_TYPE) == -1 &&
                contentType.indexOf(SOAP12Constants.SOAP_12_CONTENT_TYPE) == -1;
    }



    /**
     * Create an Axis2 message context for the given http request. The request may be in the
     * process of being streamed
     *
     * @param request the http request to be used to create the corresponding Axis2 message context
     * @return the Axis2 message context created
     */
    public MessageContext createMessageContext(MessageContext msgContext, SourceRequest request) {
    	
    	Map excessHeaders = request.getExcessHeaders();
        ConfigurationContext cfgCtx = sourceConfiguration.getConfigurationContext();

        if (msgContext == null) {
            msgContext = new MessageContext();
        }
        msgContext.setMessageID(UIDGenerator.generateURNString());

        // Axis2 spawns a new threads to send a message if this is TRUE - and it has to
        // be the other way
        msgContext.setProperty(MessageContext.CLIENT_API_NON_BLOCKING,
                Boolean.FALSE);
        msgContext.setConfigurationContext(cfgCtx);

//        msgContext.setTransportOut(cfgCtx.getAxisConfiguration()
//                .getTransportOut(Constants.TRANSPORT_HTTP));
//        msgContext.setTransportIn(cfgCtx.getAxisConfiguration()
//                .getTransportIn(Constants.TRANSPORT_HTTP));
//        msgContext.setIncomingTransportName(Constants.TRANSPORT_HTTP);
//        msgContext.setProperty(Constants.OUT_TRANSPORT_INFO, this);
        
        NHttpServerConnection conn = request.getConnection();
        
        if (sourceConfiguration.getScheme().isSSL()) {
            msgContext.setTransportOut(cfgCtx.getAxisConfiguration()
                .getTransportOut(Constants.TRANSPORT_HTTPS));
            msgContext.setTransportIn(cfgCtx.getAxisConfiguration()
                .getTransportIn(Constants.TRANSPORT_HTTPS));
			msgContext.setIncomingTransportName(sourceConfiguration.getInDescription() != null?
			                                    sourceConfiguration.getInDescription().getName(): Constants.TRANSPORT_HTTPS);

            SSLIOSession ssliosession = (SSLIOSession) (conn.getContext()).getAttribute(SSLIOSession.SESSION_KEY);
            //set SSL certificates to message context if SSLVerifyClient parameter is set
            if (ssliosession != null && msgContext.getTransportIn() != null
                && msgContext.getTransportIn().getParameter(NhttpConstants.SSL_VERIFY_CLIENT) != null) {
                try {
                    msgContext.setProperty(NhttpConstants.SSL_CLIENT_AUTH_CERT_X509,
                                           ssliosession.getSSLSession().getPeerCertificateChain());
                } catch (SSLPeerUnverifiedException e) {
                    //Peer Certificate Chain may not be available always.(in case of Mutual SSL is not enabled)
                    if (log.isTraceEnabled()) {
                        log.trace("Peer certificate chain is not available for MsgContext " + msgContext.getMessageID());
                    }
                }
            }
        } else {
            msgContext.setTransportOut(cfgCtx.getAxisConfiguration()
                .getTransportOut(Constants.TRANSPORT_HTTP));
            msgContext.setTransportIn(cfgCtx.getAxisConfiguration()
                .getTransportIn(Constants.TRANSPORT_HTTP));
            msgContext.setIncomingTransportName(sourceConfiguration.getInDescription() != null?
                                               	sourceConfiguration.getInDescription().getName(): Constants.TRANSPORT_HTTP);
                                                 
        }

        msgContext.setProperty(Constants.OUT_TRANSPORT_INFO, this);
        msgContext.setServerSide(true);
        msgContext.setProperty(
                Constants.Configuration.TRANSPORT_IN_URL, request.getUri());

        // http transport header names are case insensitive
        Map<String, String> headers = new TreeMap<String, String>(new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });

        Set<Map.Entry<String, String>> entries = request.getHeaders().entrySet();
        for (Map.Entry<String, String> entry : entries) {
            headers.put(entry.getKey(), entry.getValue());
        }
        msgContext.setProperty(MessageContext.TRANSPORT_HEADERS, headers);
        msgContext.setProperty(NhttpConstants.EXCESS_TRANSPORT_HEADERS, excessHeaders);

        // Following section is required for throttling to work
        
        if (conn instanceof HttpInetConnection) {
            HttpInetConnection netConn = (HttpInetConnection) conn;
            InetAddress remoteAddress = netConn.getRemoteAddress();
            if (remoteAddress != null) {
                msgContext.setProperty(
                        MessageContext.REMOTE_ADDR, remoteAddress.getHostAddress());
                msgContext.setProperty(
                        NhttpConstants.REMOTE_HOST, NhttpUtil.getHostName(remoteAddress));
            }
        }

        msgContext.setProperty(RequestResponseTransport.TRANSPORT_CONTROL,
                new HttpCoreRequestResponseTransport(msgContext));

        return msgContext;
    }

    private void handleException(String msg, Exception e) {
        if (e == null) {
            log.error(msg);
        } else {
            log.error(msg, e);
        }

        if (e == null) {
            e = new Exception(msg);
        }

        try {
            MessageContext faultContext = MessageContextBuilder
                    .createFaultMessageContext(msgContext, e);
            msgContext.setProperty(PassThroughConstants.FORCE_SOAP_FAULT,
                    Boolean.TRUE);
            AxisEngine.sendFault(faultContext);

        } catch (Exception ex) {
            NHttpServerConnection conn = request.getConnection();
            SourceResponse sourceResponse;
            msgContext.removeProperty(MessageContext.TRANSPORT_HEADERS);
            if (log.isDebugEnabled()) {
                log.debug("Sending ACK response with status "
                        + msgContext.getProperty(NhttpConstants.HTTP_SC)
                        + ", for MessageID : " + msgContext.getMessageID());
            }
            sourceResponse = SourceResponseFactory.create(msgContext, request,
                    sourceConfiguration);
            sourceResponse.addHeader(HTTP.CONTENT_TYPE, "text/html");
            sourceResponse.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);

            Pipe pipe = new Pipe(sourceConfiguration.getBufferFactory()
                    .getBuffer(), "Test", sourceConfiguration);
            msgContext
                    .setProperty(PassThroughConstants.PASS_THROUGH_PIPE, pipe);
            msgContext.setProperty(
                    PassThroughConstants.MESSAGE_BUILDER_INVOKED, Boolean.TRUE);
            pipe.attachConsumer(conn);
            sourceResponse.connect(pipe);

            OutputStream os = pipe.getOutputStream();

            try {
                String body = "<html><body><h1>"
                        + "Failed to process the request" + "</h1><p>" + msg
                        + "</p>";
                if (e != null) {
                    body = body + "<p>" + e.getMessage() + "</p></body></html>";
                }
                if (ex != null) {
                    body = body + "<p>" + ex.getMessage()
                            + "</p></body></html>";
                }
                os.write(body.getBytes());
                os.flush();
                os.close();
            } catch (Exception ignore) {
            }

            pipe.setSerializationCompleteWithoutData(true);

            SourceContext.setResponse(conn, sourceResponse);
            ProtocolState state = SourceContext.getState(conn);
            if (state != null
                    && state.compareTo(ProtocolState.REQUEST_DONE) <= 0) {
                conn.requestOutput();
            } else {
                SourceContext.updateState(conn, ProtocolState.CLOSED);
                sourceConfiguration.getSourceConnections().shutDownConnection(
                        conn,true);
            }

        }
    }

    private String inferContentType() {
        Map<String, String> headers = request.getHeaders();
        for (String header : headers.keySet()) {
            if (HTTP.CONTENT_TYPE.equalsIgnoreCase(header)) {
                return headers.get(header);
            }
        }
        Parameter param = sourceConfiguration.getConfigurationContext().getAxisConfiguration().
                getParameter(PassThroughConstants.REQUEST_CONTENT_TYPE);
        if (param != null) {
            return param.getValue().toString();
        }
        return null;
    }

    protected MessageContext getRequestContext() {
        return msgContext;
    }

    /**
     * @return Get SourceRequest Processed by ServerWorker
     */
    public SourceRequest getSourceRequest() {
        return request;
    }

    /**
     * @param request Set SourceRequest to be processed by ServerWorker
     */
    public void setSourceRequest(SourceRequest request) {
        this.request = request;
    }

    /**
     * Adding REST related properties to the message context if request is  REST
     * @param msgContext Axis2MessageContext of the request
     * @param method HTTP Method of the request
     * @return whether request is REST or SOAP
     */
    public boolean isRESTRequest(MessageContext msgContext, String method){
        if(msgContext.getProperty
                (PassThroughConstants.REST_GET_DELETE_INVOKE) != null && (Boolean)msgContext.getProperty
                                                                         (PassThroughConstants.REST_GET_DELETE_INVOKE)){
            msgContext.setProperty(HTTPConstants.HTTP_METHOD, method);
            msgContext.setServerSide(true);
            msgContext.setDoingREST(true);
            return true;
        }
        return false;
    }

    /**
     * Get Uri of underlying SourceRequest and  calculate service prefix and add to message context
     * create response buffers for  HTTP GET, DELETE, OPTION and HEAD methods
     * @param msgContext Axis2MessageContext of the request
     * @param method HTTP Method of the request
     */
    public void processHttpRequestUri(MessageContext msgContext, String method){
        String servicePrefixIndex="://";
        ConfigurationContext cfgCtx = sourceConfiguration.getConfigurationContext();
        msgContext.setProperty(Constants.Configuration.HTTP_METHOD, request.getMethod());


        //String uri = request.getUri();
        String oriUri = request.getUri();
        String restUrlPostfix = NhttpUtil.getRestUrlPostfix(oriUri, cfgCtx.getServicePath());

        String servicePrefix = oriUri.substring(0, oriUri.indexOf(restUrlPostfix));
        if (servicePrefix.indexOf(servicePrefixIndex) == -1) {
            HttpInetConnection inetConn = (HttpInetConnection) request.getConnection();
            InetAddress localAddr = inetConn.getLocalAddress();
            if (localAddr != null) {
                servicePrefix = sourceConfiguration.getScheme().getName() + servicePrefixIndex +
                        localAddr.getHostAddress() + ":" + inetConn.getLocalPort() + servicePrefix;
            }
        }

        msgContext.setProperty(PassThroughConstants.SERVICE_PREFIX, servicePrefix);
        msgContext.setTo(new EndpointReference(restUrlPostfix));
        msgContext.setProperty(PassThroughConstants.REST_URL_POSTFIX, restUrlPostfix);

        if (PassThroughConstants.HTTP_GET.equals(method) ||  PassThroughConstants.HTTP_HEAD.equals(method)||
                PassThroughConstants.HTTP_OPTIONS.equals(method)) {
            HttpResponse response = sourceConfiguration.getResponseFactory().newHttpResponse(
                    request.getVersion(), HttpStatus.SC_OK,
                    request.getConnection().getContext());

            // create a basic HttpEntity using the source channel of the response pipe
            BasicHttpEntity entity = new BasicHttpEntity();
            if (request.getVersion().greaterEquals(HttpVersion.HTTP_1_1)) {
                entity.setChunked(true);
            }
            response.setEntity(entity);

            httpGetRequestProcessor.process(request.getRequest(), response,msgContext,
                    request.getConnection(), os, isRestDispatching);
        }
    }

    /**
     * Get the shared pass-through source configuration
     * @return source configuration
     */
    public SourceConfiguration getSourceConfiguration() {
        return sourceConfiguration;
    }

    /**
     * Perform cleanup of ServerWorker
     */
    private void cleanup () {
        //clean threadLocal variables
        MessageContext.destroyCurrentMessageContext();
        //clean tenantInfo
        TenantInfoInitiator tenantInfoInitiator = TenantInfoInitiatorProvider.getTenantInfoInitiator();
        if (tenantInfoInitiator != null) {
            tenantInfoInitiator.cleanTenantInfo();
        }

    }

}
