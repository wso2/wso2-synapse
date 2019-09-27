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

package org.apache.synapse.transport.passthru;

public class PassThroughConstants {

    public static final int DEFAULT_IO_THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    public static final int DEFAULT_MAX_CONN_PER_HOST_PORT = Integer.MAX_VALUE;
    
    public static final String REQUEST_MESSAGE_CONTEXT = "REQUEST_MESSAGE_CONTEXT";
    public static final String CONNECTION_POOL = "CONNECTION_POOL";
    public static final String TUNNEL_HANDLER = "TUNNEL_HANDLER";
    public static final String CONNECTION_INIT_TIME = "CONNECTION_INIT_TIME";
    public static final String CONNECTION_RELEASE_TIME = "CONNECTION_RELEASE_TIME";

    public static final String TRUE = "TRUE";

    public static final String FAULT_MESSAGE = "FAULT_MESSAGE"; // corresponds with BaseConstants
    public static final String FAULTS_AS_HTTP_200 = "FAULTS_AS_HTTP_200";
    public static final String SC_ACCEPTED = "SC_ACCEPTED";
    public static final String HTTP_SC = "HTTP_SC";
    public static final String FORCE_HTTP_1_0 = "FORCE_HTTP_1.0";
    public static final String DISABLE_CHUNKING = "DISABLE_CHUNKING";
    public static final String FULL_URI = "FULL_URI";
    public static final String NO_KEEPALIVE = "NO_KEEPALIVE";
    public static final String DISABLE_KEEPALIVE = "http.connection.disable.keepalive";
    public static final String IGNORE_SC_ACCEPTED = "IGNORE_SC_ACCEPTED";
    public static final String FORCE_SC_ACCEPTED = "FORCE_SC_ACCEPTED";
    public static final String DISCARD_ON_COMPLETE = "DISCARD_ON_COMPLETE";

    public static final String SERVICE_URI_LOCATION = "ServiceURI";

    public static final String WSDL_EPR_PREFIX = "WSDLEPRPrefix";

    public static final String EPR_TO_SERVICE_NAME_MAP = "service.epr.map";
    public static final String NON_BLOCKING_TRANSPORT = "NonBlockingTransport";
    public static final String SERIALIZED_BYTES = "SerializedBytes";

    public static final String CONTENT_TYPE = "CONTENT_TYPE";
    // This property can be used to remove character encode. By default character encoding is enabled in the ESB profile.
    // If this property is set to 'false', the 'CHARACTER_SET_ENCODING' property cannot be used.
    public static final String SET_CHARACTER_ENCODING = "setCharacterEncoding";

    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    public static final String CONTENT_TYPE_MULTIPART_RELATED = "multipart/related";
    public static final String CONTENT_TYPE_MULTIPART_FORM_DATA = "multipart/form-data";

    public final static String JSON_CONTENT_TYPE = "application/json";

    public static final String HIDDEN_SERVICE_PARAM_NAME = "hiddenService";

    /** An Axis2 message context property indicating a transport send failure */
    public static final String SENDING_FAULT = "SENDING_FAULT";
    /** The message context property name which holds the error code for the last encountered exception */
    public static final String ERROR_CODE = "ERROR_CODE";
    /** The MC property name which holds the error message for the last encountered exception */
    public static final String ERROR_MESSAGE = "ERROR_MESSAGE";
    /** The message context property name which holds the error detail (stack trace) for the last encountered exception */
    public static final String ERROR_DETAIL = "ERROR_DETAIL";
    /** The message context property name which holds the exception (if any) for the last encountered exception */
    public static final String ERROR_EXCEPTION = "ERROR_EXCEPTION";
    /** An Axis2 message context property that hols the raw payload when an error occurs while building message */
    public static final String RAW_PAYLOAD = "RAW_PAYLOAD";

    // ********** DO NOT CHANGE THESE UNLESS CORRESPONDING SYNAPSE CONSTANT ARE CHANGED ************

    public static final String REST_URL_POSTFIX = "REST_URL_POSTFIX";
    public static final String SERVICE_PREFIX = "SERVICE_PREFIX";
    public static final String NO_ENTITY_BODY = "NO_ENTITY_BODY";

    protected static final String PASS_THROUGH_TRANSPORT_WORKER_POOL =
            "PASS_THROUGH_TRANSPORT_WORKER_POOL";
    protected static final String PASS_THROUGH_SOURCE_CONFIGURATION =
            "PASS_THROUGH_SOURCE_CONFIGURATION";
    protected static final String PASS_THROUGH_SOURCE_CONNECTION = "pass-through.Source-Connection";
    protected static final String PASS_THROUGH_SOURCE_REQUEST = "pass-through.Source-Request";

    protected static final String PASS_THROUGH_TARGET_CONNECTION = "pass-through.Target-Connection";
    protected static final String PASS_THROUGH_TARGET_RESPONSE = "pass-through.Target-Response";

    public static final String PASS_THROUGH_PIPE = "pass-through.pipe";

    // used to define the default content type as a parameter in the axis2.xml
    public static final String REQUEST_CONTENT_TYPE = "DEFAULT_REQUEST_CONTENT_TYPE";

    // This is a workaround  for  axis2 RestUtils behaviour
    public static final String REST_REQUEST_CONTENT_TYPE = "synapse.internal.rest.contentType";

    public static final String MESSAGE_BUILDER_INVOKED = "message.builder.invoked";

    // This is similar to isDoingREST  - if the request contains a REST (i.e. format=POX | GET | REST) call, then we set this to TRUE
    public static final String INVOKED_REST = "invokedREST";

    // Use this to make PassThroughHttpSender set the Message Formatter's writeTo() preserve boolean value
    public static final String FORMATTER_PRESERVE = "chunkedFormatterPreserve";     
    
    public static final String CLONE_PASS_THROUGH_PIPE_REQUEST = "clone_pass-through.pipe_connected";
    
    /**
     * Name of the .mar file
     */
    public final static String SECURITY_MODULE_NAME = "rampart";
    
    public final static String REST_GET_DELETE_INVOKE ="rest_get_delete_invoke";

    public final static String FORCE_POST_PUT_NOBODY ="FORCE_POST_PUT_NOBODY";

    public final static String PASSTROUGH_MESSAGE_LENGTH ="PASSTROUGH_MESSAGE_LENGTH";
    
	public static final String CONF_LOCATION = "conf.location";

    public static final String LOCATION = "Location";
    
	public static final String BUFFERED_INPUT_STREAM = "bufferedInputStream";
	
	//JMX statistic calculation Constants
	public static final String REQ_ARRIVAL_TIME = "REQ_ARRIVAL_TIME";
	public static final String REQ_DEPARTURE_TIME = "REQ_DEPARTURE_TIME";
	public static final String RES_ARRIVAL_TIME = "RES_ARRIVAL_TIME";
	public static final String RES_HEADER_ARRIVAL_TIME = "RES_HEADER_ARRIVAL_TIME";
	public static final String RES_DEPARTURE_TIME = "RES_DEPARTURE_TIME";

    public static final String REQ_FROM_CLIENT_READ_START_TIME = "REQ_FROM_CLIENT_READ_START_TIME";
    public static final String REQ_FROM_CLIENT_READ_END_TIME = "REQ_FROM_CLIENT_READ_END_TIME";

    public static final String REQ_TO_BACKEND_WRITE_START_TIME = "REQ_TO_BACKEND_WRITE_START_TIME";
    public static final String REQ_TO_BACKEND_WRITE_END_TIME = "REQ_TO_BACKEND_WRITE_END_TIME";

    public static final String RES_FROM_BACKEND_READ_START_TIME = "RES_FROM_BACKEND_READ_START_TIME";
    public static final String RES_FROM_BACKEND_READ_END_TIME = "RES_FROM_BACKEND_READ_END_TIME";

    public static final String RES_TO_CLIENT_WRITE_START_TIME = "RES_TO_CLIENT_WRITE_START_TIME";
    public static final String RES_TO_CLIENT_WRITE_END_TIME = "RES_TO_CLIENT_WRITE_END_TIME";

    public static final String SERVER_WORKER_INIT_TIME = "SERVER_WORKER_INIT_TIME";
    public static final String SERVER_WORKER_START_TIME = "SERVER_WORKER_START_TIME";

    public static final String CLIENT_WORKER_INIT_TIME = "CLIENT_WORKER_INIT_TIME";
    public static final String CLIENT_WORKER_START_TIME = "CLIENT_WORKER_START_TIME";

    public static final String SYNAPSE_PASSTHROUGH_LATENCY_ADVANCE_VIEW = "synapse.passthrough.latency_view.enable_advanced_view";
    public static final String SYNAPSE_PASSTHROUGH_S2SLATENCY_ADVANCE_VIEW = "synapse.passthrough.s2slatency_view.enable_advanced_view";
    public static final String PASSTHROUGH_LATENCY_VIEW = "PassthroughLatencyView";
    public static final String PASSTHROUGH_S2SLATENCY_VIEW = "PassthroughS2SLatencyView";
    public static final String PASSTHOUGH_HTTP_SERVER_WORKER = "PassthroughHttpServerWorker";

    public static final String MESSAGE_OUTPUT_FORMAT = "MESSAGE_OUTPUT_FORMAT";
	
	public static final String FORCE_SOAP_FAULT = "FORCE_SOAP_FAULT";
	
	public static final String FORCE_PASSTHROUGH_BUILDER = "force.passthrough.builder";
	
	public static final String WSDL_GEN_HANDLED = "WSDL_GEN_HANDLED";
	
	public static final String COPY_CONTENT_LENGTH_FROM_INCOMING="COPY_CONTENT_LENGTH_FROM_INCOMING";
	
	public static final String ORGINAL_CONTEN_LENGTH="ORGINAL_CONTEN_LENGTH";

	public static final String WAIT_BUILDER_IN_STREAM_COMPLETE="WAIT_BUILDER_IN_STREAM_COMPLETE"; 
	
	public static final String BUILDER_OUTPUT_STREAM="BUILDER_OUTPUT_STREAM";

    // Enable the SOAP trace facility to PassThrough
    public static final String TRACE_SOAP_MESSAGE = "wso2tracer";

    //if this property is true in response path, it mean that client sent Accept-Encoding=gzip header
    public static final String REQUEST_ACCEPTS_GZIP ="REQUEST_ACCEPTS_GZIP" ;
    
    public static final String HTTP_SC_DESC = "HTTP_SC_DESC";
    
    public static final String RELAY_EARLY_BUILD  ="relay_early_build";

    public static final String HTTP_CONTENT_TYPE = "Content-type";
    public static final String HTTP_CONTENT_LENGTH = "Content-Length";

    // Used to determine the configured proxy profile
    public static final String PROXY_PROFILE_TARGET_HOST = "PROXY_PROFILE_TARGET_HOST";

    public static final String PROXY_BASIC_REALM = "BASIC realm=\"proxy\"";

    public static final String HTTP_PROXY_HOST = "http.proxyHost";
    public static final String HTTP_PROXY_PORT = "http.proxyPort";
    public static final String HTTP_PROXY_USERNAME = "http.proxy.username";
    public static final String HTTP_PROXY_PASSWORD = "http.proxy.password";
    public static final String HTTP_NON_PROXY_HOST = "http.nonProxyHosts";
    public static final String HTTP_ETAG_ENABLED = "HTTP_ETAG";

    public static final String ENABLE_WS_ADDRESSING ="enforceWSAddressing";
    
    public static final String SERVER_WORKER_THREAD_ID = "SERVER_WORKER_THREAD_ID";

    //Http method type constants to be used in synapse
    public static final String HTTP_HEAD = "HEAD";
    public static final String HTTP_GET = "GET";
    public static final String HTTP_POST = "POST";
    public static final String HTTP_DELETE = "DELETE";
    public static final String HTTP_PUT = "PUT";
    public static final String HTTP_OPTIONS = "OPTIONS";
    public static final String HTTP_CONNECT = "CONNECT";

    //Constant to specify the socket timeout
    public static final String HTTP_SOCKET_TIMEOUT = "HTTP_SOCKET_TIMEOUT";

    public static final String ORIGINAL_HTTP_SC = "ORIGINAL_STATUS_CODE";
    public static final String ORIGINAL_HTTP_REASON_PHRASE = "HTTP_REASON_PHRASE";


    public static final String MESSAGE_SIZE_VALIDATION_SUM = "MESSAGE_SIZE_VALIDATION_SUM";
    public static final String SOURCE_CONNECTION_DROPPED = "SOURCE_CONNECTION_DROPPED";

    /*
     * Denotes application/octet-stream content-type
     */
    public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    /*
     * System property to configure verification timeout (iterative verification) in seconds for port.
     */
    public static final String SYSTEMPROP_PORT_CLOSE_VERIFY_TIMEOUT = "synapse.transport.portCloseVerifyTimeout";

    //Validate the bad formed xml message by building the whole xml document.
    public static final String FORCE_XML_MESSAGE_VALIDATION = "force.xml.message.validation";

    //Check for invalid json message by parsing the input message
    public static final String FORCE_JSON_MESSAGE_VALIDATION = "force.json.message.validation";

    /**
     * constants for correlation logging
     */
    //correlation enable/disable state property in axis2 message context
    public static final String CORRELATION_LOG_STATE_PROPERTY = "correlationLogState";
    //property to set the correlation id value in message context and http context
    public static final String CORRELATION_ID = "correlation_id";
    //system property to enable/disable correlation logging
    public static final String CORRELATION_LOGS_SYS_PROPERTY = "enableCorrelationLogs";
    //property to set the correlation ID as a MDC property in log4J
    public static final String CORRELATION_MDC_PROPERTY = "Correlation-ID";
    //correlation logger name in log4J properties
    public static final String CORRELATION_LOGGER = "correlation";
    //default header that carries the correlation ID. Header name is configurable at passthru-http.properties
    public static final String CORRELATION_DEFAULT_HEADER = "activityid";
}
