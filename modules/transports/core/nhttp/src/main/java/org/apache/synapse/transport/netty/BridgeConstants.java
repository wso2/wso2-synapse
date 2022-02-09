/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.synapse.transport.netty;

/**
 * {@code BridgeConstants} contains the constants related to netty axis2 bridge.
 */
public class BridgeConstants {

    public static final String TRANSPORT_HTTPWS = "httpws";
    public static final String TRANSPORT_HTTPSWSS = "httpswss";

    public static final String PORT_PARAM = "port";
    public static final String HOSTNAME_PARAM = "hostname";
    public static final String HTTP_PROTOCOL_VERSION_PARAM = "protocolVersion";

    public static final String BRIDGE_LOG_PREFIX = "[Bridge] ";

    public static final String VALUE_TRUE = "true";
    public static final String VALUE_FALSE = "false";
    public static final String TRUE = "TRUE";

    public static final String NO_ENTITY_BODY = "NO_ENTITY_BODY";
    public static final String PROTOCOL = "PROTOCOL";

    public static final String REMOTE_HOST = "REMOTE_HOST";

    public static final String HTTP_METHOD = "HTTP_METHOD";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String SOAP_ACTION_HEADER = "SOAPAction";
    public static final String TRANSFER_ENCODING = "Transfer-Encoding";

    public static final String CONTENT_LEN = "Content-Length";

    public static final String REST_URL_POSTFIX = "REST_URL_POSTFIX";
    public static final String SERVICE_PREFIX = "SERVICE_PREFIX";
    public static final String FAULT_MESSAGE = "FAULT_MESSAGE";

    public static final String REST_REQUEST_CONTENT_TYPE = "synapse.internal.rest.contentType";
    public static final String HTTP_CARBON_MESSAGE = "HTTP_CARBON_MESSAGE";
    public static final String HTTP_CLIENT_REQUEST_CARBON_MESSAGE = "HTTP_CLIENT_REQUEST_CARBON_MESSAGE";
    public static final String TRANSPORT_MESSAGE_HANDLER = "transport.message.handler";

    public static final String MESSAGE_BUILDER_INVOKED = "message.builder.invoked";

    public static final long NO_CONTENT_LENGTH_FOUND = -1;
    public static final short ONE_BYTE = 1;

    // This is similar to isDoingREST  - if the request contains a REST (i.e. format=POX | GET | REST) call,
    // then we set this to TRUE
    public static final String INVOKED_REST = "invokedREST";
    public static final String NON_BLOCKING_TRANSPORT = "NonBlockingTransport";

    public static final String CONTENT_TYPE_APPLICATION_OCTET_STREAM = "application/octet-stream";
    public static final String JSON_CONTENT_TYPE = "application/json";
    public static final String RELAY_EARLY_BUILD = "relay_early_build";
    public static final String HTTP_SOURCE_CONFIGURATION = "HTTP_SOURCE_CONFIGURATION";
    public static final String HTTP_TARGET_CONFIGURATION = "HTTP_TARGET_CONFIGURATION";
    public static final String POOLED_BYTE_BUFFER_FACTORY = "POOLED_BYTE_BUFFER_FACTORY";
    public static final String MESSAGE_OUTPUT_FORMAT = "MESSAGE_OUTPUT_FORMAT";
    public static final String FORCE_SOAP_FAULT = "FORCE_SOAP_FAULT";
    public static final String NIO_ACK_REQUESTED = "NIO-ACK-Requested";
    public static final String WSDL_REQUEST_HANDLED = "WSDL_REQUEST_HANDLED";
    // used to define the default content type as a parameter in the axis2.xml
    public static final String DEFAULT_REQUEST_CONTENT_TYPE = "DEFAULT_REQUEST_CONTENT_TYPE";
    public static final String VALID_CACHED_RESPONSE = "VALID_CACHED_RESPONSE";
    public static final String ETAG_HEADER = "ETag";
    public static final String PRE_LOCATION_HEADER = "PRE_LOCATION_HEADER";
    public static final String FULL_URI = "FULL_URI";
    public static final String INTERNAL_EXCEPTION_ORIGIN = "_INTERNAL_EXCEPTION_ORIGIN";
    public static final String INTERNAL_ORIGIN_ERROR_HANDLER = "TARGET_ERROR_HANDLER";
    public static final String LOCATION = "Location";
    public static final String CONF_LOCATION = "conf.location";
    public static final String BUFFERED_INPUT_STREAM = "bufferedInputStream";

    // move later
    public static final String CARBON_SERVER_XML_NAMESPACE = "http://wso2.org/projects/carbon/carbon.xml";

    public static final String CHUNKING_CONFIG = "chunking_config";

    public static final String DEFAULT_VERSION_HTTP_1_1 = "HTTP/1.1";
    public static final float HTTP_1_1 = 1.1f;
    public static final float HTTP_1_0 = 1.0f;
    public static final String HTTP_2_0 = "2.0";
    public static final String HTTP_VERSION_PREFIX = "HTTP/";
    public static final String HTTP_1_0_VERSION = "1.0";
    public static final String HTTP_1_1_VERSION = "1.1";
    public static final String HTTP_2_0_VERSION = "2.0";

    public static final String AUTO = "AUTO";
    public static final String ALWAYS = "ALWAYS";
    public static final String NEVER = "NEVER";

    public static final String PROTOCOL_HTTP = "http";
    public static final String PROTOCOL_HTTPS = "https";

    private static final String LOCAL_HOST = "localhost";
    public static final String HTTP_DEFAULT_HOST = "0.0.0.0";
    public static final String TO = "TO";
    public static final String HTTP_HOST = "host";
    public static final String HTTP_PORT = "port";

    // Logging related runtime parameter names
    public static final String HTTP_TRACE_LOG = "http.tracelog";
    public static final String HTTP_TRACE_LOG_ENABLED = "http.tracelog.enabled";
    public static final String HTTP_ACCESS_LOG = "http.accesslog";
    public static final String HTTP_ACCESS_LOG_ENABLED = "http.accesslog.enabled";

    public static final String KEY_STORE = "keystore";
    public static final String TRUST_STORE = "truststore";
    public static final String SSL_VERIFY_CLIENT = "SSLVerifyClient";
    public static final String SSL_PROTOCOL = "SSLProtocol";
    public static final String HTTPS_PROTOCOL = "HttpsProtocols";
    public static final String CLIENT_REVOCATION = "CertificateRevocationVerifier";
    public static final String PREFERRED_CIPHERS = "PreferredCiphers";
    public static final String SSL_SESSION_TIMEOUT = "SessionTimeout";
    public static final String SSL_HANDSHAKE_TIMEOUT = "HandshakeTimeout";
    public static final String HOSTNAME_VERIFIER = "HostnameVerifier";
    public static final String STORE_LOCATION = "Location";
    public static final String TYPE = "Type";
    public static final String PASSWORD = "Password";
    public static final String KEY_PASSWORD = "KeyPassword";
    public static final String NO_VALIDATE_CERT = "novalidatecert";
    public static final String SSL_ENABLED_PROTOCOLS = "sslEnabledProtocols";
    public static final String CIPHERS = "ciphers";
    public static final String CACHE_SIZE = "CacheSize";
    public static final String CACHE_DELAY = "CacheDelay";
    public static final String TLS_PROTOCOL = "TLS";

    //Http method type constants to be used in synapse
    public static final String HTTP_HEAD = "HEAD";
    public static final String HTTP_GET = "GET";
    public static final String HTTP_POST = "POST";
    public static final String HTTP_DELETE = "DELETE";
    public static final String HTTP_PUT = "PUT";
    public static final String HTTP_OPTIONS = "OPTIONS";
    public static final String HTTP_CONNECT = "CONNECT";

    public static final String HTTP_STATUS_CODE_SENT_FROM_BACKEND = "HTTP_STATUS_CODE_SENT_FROM_BACKEND";
    public static final String HTTP_REASON_PHRASE_SENT_FROM_BACKEND = "HTTP_REASON_PHRASE_SENT_FROM_BACKEND";

    /**
     * An Axis2 message context property indicating a transport send failure.
     */
    public static final String SENDING_FAULT = "SENDING_FAULT";
    /**
     * The message context property name which holds the error code for the last encountered exception.
     */
    public static final String ERROR_CODE = "ERROR_CODE";
    /**
     * The MC property name which holds the error message for the last encountered exception.
     */
    public static final String ERROR_MESSAGE = "ERROR_MESSAGE";
    /**
     * The message context property name which holds the error detail (stack trace) for the last encountered exception.
     */
    public static final String ERROR_DETAIL = "ERROR_DETAIL";
    /**
     * The message context property name which holds the exception (if any) for the last encountered exception.
     */
    public static final String ERROR_EXCEPTION = "ERROR_EXCEPTION";
    /**
     * An Axis2 message context property that hols the raw payload when an error occurs while building message.
     */
    public static final String RAW_PAYLOAD = "RAW_PAYLOAD";

    /**
     * When ESB receives a soap fault as a HTTP 500 message, ESB will forward this fault to client with status code 200.
     */
    public static final String FAULTS_AS_HTTP_200 = "FAULTS_AS_HTTP_200";

    public static final String SC_ACCEPTED = "SC_ACCEPTED";

    /**
     * The HTTP status code.
     */
    public static final String HTTP_SC = "HTTP_SC";

    /**
     * HTTP response's Reason- Phrase that is sent by the backend. For example, if the backend sends
     * the response's status as HTTP/1.1 200 OK, then the value of HTTP_SC_DESC is OK.
     */
    public static final String HTTP_SC_DESC = "HTTP_SC_DESC";

    /**
     * Force HTTP 1.0 for outgoing HTTP messages.
     */
    public static final String FORCE_HTTP_1_0 = "FORCE_HTTP_1.0";

    /**
     * If you set this to true, it disables HTTP chunking for outgoing messages.
     */
    public static final String DISABLE_CHUNKING = "DISABLE_CHUNKING";

    /**
     * The value of this property will be set as the HTTP host header of outgoing request.
     */
    public static final String REQUEST_HOST_HEADER = "REQUEST_HOST_HEADER";

    /**
     * Disables HTTP keep alive for outgoing requests.
     */
    public static final String NO_KEEPALIVE = "NO_KEEPALIVE";

    /**
     * This property makes the outgoing URL of the ESB a complete URL.
     * This is important when we talk through a Proxy Server.
     */
    public static final String POST_TO_URI = "POST_TO_URI";

    /**
     * If the request sent by the client contains the ‘Content-Length’ header, this property allows the ESB to send
     * the request with the content length (without HTTP chunking) to the back end server.
     */
    public static final String FORCE_HTTP_CONTENT_LENGTH = "FORCE_HTTP_CONTENT_LENGTH";

    /**
     * When set to true, this property forces a 202 HTTP response to the client immediately after the current
     * execution thread finishes, so that the client stops waiting for a response.
     */
    public static final String FORCE_SC_ACCEPTED = "FORCE_SC_ACCEPTED";

    public static final String IGNORE_SC_ACCEPTED = "IGNORE_SC_ACCEPTED";

    /**
     * This property determines whether the HTTP Etag should be enabled for the request or not.
     * HTTP Etag is a mechanism provided by HTTP for Web cache validation.
     */
    public static final String HTTP_ETAG_ENABLED = "HTTP_ETAG";

    // This property can be used to remove character encode. By default character encoding is enabled in the ESB
    // profile. If this property is set to 'false', the 'CHARACTER_SET_ENCODING' property cannot be used.
    public static final String SET_CHARACTER_ENCODING = "setCharacterEncoding";

    /**
     * A message context property indicating "TRUE". This will set on success scenarios.
     */
    public static final String HTTP_202_RECEIVED = "HTTP_202_RECEIVED";


    public static final String DEFAULT_OUTBOUND_USER_AGENT = "WSO2-HTTP-Synapse-Transport";

}
