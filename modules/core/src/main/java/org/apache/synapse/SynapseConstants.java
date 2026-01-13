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

package org.apache.synapse;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMNamespace;
import javax.xml.namespace.QName;
import java.util.regex.Pattern;

/**
 * Global constants for the Apache Synapse project
 */
public final class SynapseConstants {

    /** Keyword synapse */
    public static final String SYNAPSE = "synapse";
    public static final String TRUE = "TRUE";
    /** The Synapse namespace */
    public static final String SYNAPSE_NAMESPACE = "http://ws.apache.org/ns/synapse";
    /** An OMNamespace object for the Synapse NS */
    public static final OMNamespace SYNAPSE_OMNAMESPACE =
            OMAbstractFactory.getOMFactory().createOMNamespace(SYNAPSE_NAMESPACE, "");
    /** An OMNamespace object for the Empty Namespace */
    public static final OMNamespace NULL_NAMESPACE = 
    	OMAbstractFactory.getOMFactory().createOMNamespace("", "");
    /** The associated xml file of registry */
    public static final String REGISTRY_FILE = "registry.xml";
    /** The name of the main sequence for message mediation */
    public static final String MAIN_SEQUENCE_KEY  = "main";
    /** The associated xml file of the default main sequence */
    public static final String MAIN_SEQUENCE_XML = "main.xml";
    /** The name of the fault sequence to execute on failures during mediation */
    public static final String FAULT_SEQUENCE_KEY = "fault";
    /** The associated xml file of the default fault sequence */
    public static final String FAULT_SEQUENCE_XML = "fault.xml";
    /** The name of the mandatory sequence to execute before the begining of the actual mediation */
    public static final String MANDATORY_SEQUENCE_KEY = "pre-mediate";
    /** The name prefix of the hidden sequence */
    public static final String PREFIX_HIDDEN_SEQUENCE_KEY = "_Hidden_Sequence_";

    /** The name of the Synapse service (used for message mediation) */
    public static final String SYNAPSE_SERVICE_NAME ="__SynapseService";
    /** The operation name used by the Synapse service (for message mediation) */
    public static final QName SYNAPSE_OPERATION_NAME = new QName("mediate");

    //- names of modules to be engaged at runtime -
    /** The Name of the WS-RM module */
    @Deprecated
    public static final String RM_MODULE_NAME = "sandesha2";
    /** The Name of the WS-A Addressing module */
    public static final String ADDRESSING_MODULE_NAME = "addressing";
    /** The Name of the WS-Security module */
    public static final String SECURITY_MODULE_NAME = "rampart";

    //- Standard headers that can be read as get-property('header')-
    /** Refers to the To header */
    public static final String HEADER_TO = "To";
    /** Refers to the From header */
    public static final String HEADER_FROM = "From";
    /** Refers to the FaultTo header */
    public static final String HEADER_FAULT = "FaultTo";
    /** Refers to the Action header */
    public static final String HEADER_ACTION = "Action";
    /** Refers to the ReplyTo header */
    public static final String HEADER_REPLY_TO = "ReplyTo";
    /** Refers to the RelatesTo header */
    public static final String HEADER_RELATES_TO = "RelatesTo";
    /** Refers to the MessageID header */
    public static final String HEADER_MESSAGE_ID = "MessageID";
    /** Refers to the property name for which the get-property function would return
     * true, if the message is a fault
     */
    public static final String PROPERTY_FAULT = "FAULT";
    /** Message format: pox, soap11, soap12 */
    public static final String PROPERTY_MESSAGE_FORMAT = "MESSAGE_FORMAT";
    /** WSDL operation name **/
    public static final String PROPERTY_OPERATION_NAME = "OperationName";
    /** WSDL operation namespace **/
    public static final String PROPERTY_OPERATION_NAMESPACE = "OperationNamespace";
    /** System time in milliseconds - the offset from epoch (i.e. System.currentTimeMillis) */
    public static final String SYSTEM_TIME = "SYSTEM_TIME";
    /** System date */
    public static final String SYSTEM_DATE = "SYSTEM_DATE";

    public static final String ADDRESSING_VERSION_FINAL = "final";
    public static final String ADDRESSING_VERSION_SUBMISSION = "submission";

    public static final String ADDRESSING_ADDED_BY_SYNAPSE = "AddressingAddedBySynapse";

    /** The Axis2 client options property name for the Rampart service policy */
    public static final String RAMPART_POLICY = "rampartPolicy";
    /** The Axis2 client options property name for the Rampart in message policy */
    public static final String RAMPART_IN_POLICY = "rampartInPolicy";
    /** The Axis2 client options property name for the Rampart out messsage policy */
    public static final String RAMPART_OUT_POLICY = "rampartOutPolicy";
    /** The Axis2 client options property name for the Sandesha policy */
	public static final String SANDESHA_POLICY = "sandeshaPolicy";
    /** ServerManager MBean category and id */
    public static final String SERVER_MANAGER_MBEAN = "ServerManager";
    public static final String RECEIVING_SEQUENCE = "RECEIVING_SEQUENCE";

    /** Service invoked by Call mediator */
    public static final String CONTINUATION_CALL = "continuation.call";

    public static final String SYNAPSE__FUNCTION__STACK = "_SYNAPSE_FUNCTION_STACK";
    public static final String SYNAPSE_WSDL_RESOLVER = "synapse.wsdl.resolver";
    public static final String SYNAPSE_SCHEMA_RESOLVER = "synapse.schema.resolver";
    /** Whether client request is a soap 11 request **/
    public static final String IS_CLIENT_DOING_SOAP11 = "IsClientDoingSOAP11";
    /** Whether client request is a REST request **/
    public static final String IS_CLIENT_DOING_REST = "IsClientDoingREST";
    /** Content-type of soap11 request **/
    public final static String SOAP11_CONTENT_TYPE  = "text/xml";
    /** Content-type of soap12 request **/
    public final static String SOAP12_CONTENT_TYPE  = "application/soap+xml";
    
    /**
     * Content-Type of XML request
     */
    public static final String XML_CONTENT_TYPE = "application/xml";
    /**
     * Content-Type property of Axis2MessageContext
     */
    public static final String AXIS2_PROPERTY_CONTENT_TYPE = "ContentType";
    public static final String SET_JMS_SVC_PARAMS_AS_MSG_CTX_PARAM = "SET_JMS_SVC_PARAMS_AS_MSG_CTX";
    public static final String INVOKE_MEDIATOR_ID = "INVOKE_MEDIATOR_ID";

    /** Parameter names in the axis2.xml that can be used to configure the synapse */
    public static final class Axis2Param {
        /** Synapse Configuration file location */
        public static final String SYNAPSE_CONFIG_LOCATION = "SynapseConfig.ConfigurationFile";
        /** Synapse Home directory */
        public static final String SYNAPSE_HOME = "SynapseConfig.HomeDirectory";
        /** Synapse resolve root */
        public static final String SYNAPSE_RESOLVE_ROOT = "SynapseConfig.ResolveRoot";
        /** Synapse server name */
        public static final String SYNAPSE_SERVER_NAME = "SynapseConfig.ServerName";
    }
    
    /** The name of the Parameter set on the Axis2Configuration to hold the Synapse Configuration */
    public static final String SYNAPSE_CONFIG = "synapse.config";
    /** EIP pattern name */
    public static final String INIT_EIP_PATTERN = "init";
    /** The name of the Parameter set on the Axis2Configuration to hold the Synapse Environment */
    public static final String SYNAPSE_ENV = "synapse.env";
    /** The name of the Parameter set on AxisConfiguration to hold the ServerContextInformation */
    public static final String SYNAPSE_SERVER_CTX_INFO = "synapse.server.context.info";
    /** The name of the Parameter set on AxisConfiguration to hold the ServerContextInformation */
    public static final String SYNAPSE_SERVER_CONFIG_INFO = "synapse.server.config.info";

    /** The name of the system property that will hold the Synapse home directory */
    public static final String SYNAPSE_HOME = "synapse.home";
    /** The name of the system property used to specify/override the Synapse config XML location */
    public static final String SYNAPSE_XML = "synapse.xml";
    /** The name of the system property used to specify/override the Synapse properties location */
    public static final String SYNAPSE_PROPERTIES = "synapse.properties";
    /** The name of the synapse handlers file */
    public static final String SYNAPSE_HANDLER_FILE = "synapse-handlers.xml";

    public static final String SEQUENCE_OBSERVERS_FILE = "sequence-observers.xml";

    /** the name of the property used for synapse library based class loading */
    public static final String SYNAPSE_LIB_LOADER = "synapse.lib.classloader";
    /** conf directory name **/
    public static final String CONF_DIRECTORY = "conf";
    /** the name of the property used for validate mediator redeployment mediation cache clearing */
    public static final String SYNAPSE_VALIDATE_MEDIATOR_REDEPLOYMENT_CACHE_CLEAR =
            "synapse.clear.mediation.cache.on.validate.mediator.deployment";

    // TO be used in JMS producer path to set the service logger name
    public static final String SERVICE_LOGGER_NAME = "service.logger.name";

    // hidden service parameter
    public static final String HIDDEN_SERVICE_PARAM = "hiddenService";

    // proxy services servicetype parameter
        /** service type parameter name */
        public static final String SERVICE_TYPE_PARAM_NAME = "serviceType";
        /** service type param value for the proxy services */
        public static final String PROXY_SERVICE_TYPE = "proxy";

    //- Synapse Message Context Properties -
        /** The Synapse MC property keep the response state */
        public static final String RESPONSE_STATE = "__SYNAPSE_RESPONSE_STATE__";
        /** The Synapse MC property name that holds the name of the Proxy service thats handling it */
        public static final String PROXY_SERVICE = "proxy.name";
        /** The Synapse MC property that marks it as a RESPONSE */
        public static final String RESPONSE = "RESPONSE";
        /** The Synapse MC property that indicates the in-transport */
        public static final String IN_TRANSPORT = "IN_TRANSPORT";
        /** The Synapse MC property that marks if the message was denied on the accessed transport */
        public static final String TRANSPORT_DENIED = "TRANSPORT_DENIED";
        /** The Synapse MC property that marks the message as a OUT_ONLY message */
        public static final String OUT_ONLY = "OUT_ONLY";
        /** The Synapse MC property that states that existing WS-A headers in the envelope should
        * be preserved */
        public static final String PRESERVE_WS_ADDRESSING = "PRESERVE_WS_ADDRESSING";
        /** The Synapse MC property that marks to Exception to be thrown on SOAPFault(Retry on SOAPFault)*/
        public static final String RETRY_ON_SOAPFAULT = "RETRY_ON_SOAPFAULT";
        /** The Synapse MC property name that collect payload forcefully when tracing enabled */
        public static final String FORCE_COLLECT_PAYLOAD = "FORCE_COLLECT_PAYLOAD";

        /**
         * The name of the property which specifies the operation name that is
         * invoked by an endpoint
        */
        public static final String ENDPOINT_OPERATION = "endpoint.operation";
        /** Synapse MC property that holds the url of the named endpoint which message is sent out **/
        public static final String ENDPOINT_PREFIX = "ENDPOINT_PREFIX";

        //-- error handling --
        /** An Axis2 message context property indicating a transport send failure */
        public static final String SENDING_FAULT = "SENDING_FAULT";
        /** The message context property name which holds the error code for the last encountered exception */
        public static final String ERROR_CODE = "ERROR_CODE";
        /** The message context property name which holds the error code for the last encountered exception
        * without any protocol state specific alterations*/
        public static final String BASE_ERROR_CODE = "BASE_ERROR_CODE";
        /** The message context property name which holds the http protocol state upon a failure*/
        public static final String PROTOCOL_STATE_ON_FAILURE = "PROTOCOL_STATE_ON_FAILURE";
        /** The MC property name which holds the error message for the last encountered exception */
        public static final String ERROR_MESSAGE = "ERROR_MESSAGE";
        /** The message context property name which holds the error detail (stack trace) for the last encountered exception */
        public static final String ERROR_DETAIL = "ERROR_DETAIL";
        /** The message context property name which holds the exception (if any) for the last encountered exception */
        public static final String ERROR_EXCEPTION = "ERROR_EXCEPTION";
        /** The default/generic error code */
        public static final int DEFAULT_ERROR= 0;

    /** An Axis2 message context property that indicates the maximum time to spend on sending the message */
    public static final String SEND_TIMEOUT = "SEND_TIMEOUT";

    /** Fault Handler which hold the last sequence fault handler */
    public static final String LAST_SEQ_FAULT_HANDLER = "LAST_SEQ_FAULT_HANDLER";

    /** Indicate whether the sequence is content aware or not */
    public static final String IS_SEQ_CONTENT_AWARE = "IS_SEQ_CONTENT_AWARE";

    //- Axis2 Message Context Properties used by Synapse -
    /** an axis2 message context property set to hold the relates to for POX responses */
    public static final String RELATES_TO_FOR_POX = "synapse.RelatesToForPox";
    public static final String CLIENT_API_NON_BLOCKING = "ClientApiNonBlocking";
    public static final String ENABLE_CLIENT_API_NON_BLOCKING_MODE = "enable_client_api_nonblocking_mode";

    /** an axis2 message context property set to indicate this is a response message for Synapse */
    public static final String ISRESPONSE_PROPERTY = "synapse.isresponse";


    //- aspects constants -
        /** Tracing logger name */
        public static final String TRACE_LOGGER ="TRACE_LOGGER";
        public static final String SERVICE_LOGGER_PREFIX ="SERVICE_LOGGER.";
        public static final String API_LOGGER_PREFIX ="API_LOGGER.";

        /** The tracing state -off */
        public static final int TRACING_OFF =0;
        /** The tracing state-on */
        public static final int TRACING_ON =1;
        /** The tracing state-unset */
        public static final int TRACING_UNSET=2;

        public static final String STATISTICS_STACK ="synapse.statistics.stack";     
        
        public static final String SYNAPSE_STATISTICS_STATE = "synapse.statistics.state";
        public static final String SYNAPSE_TRACE_STATE = "synapse.trace.state";

        public static final String SYNAPSE_ASPECT_CONFIGURATION = "synapse.aspects.configuration";

        public static final String SYNAPSE_ASPECTS ="synapse.aspects";

    //- handling of timed out events from the callbacks -
        /** The System property that states the duration at which the timeout handler runs */
        public static final String TIMEOUT_HANDLER_INTERVAL = "synapse.timeout_handler_interval";

        /**
         * Interval for activating the timeout handler for cleaning up expired requests. Note that
         * there can be an error as large as the value of the interval. But for smaller intervals
         * and larger timeouts this error is negligilble.
         */
        public static final long DEFAULT_TIMEOUT_HANDLER_INTERVAL = 15000;

        /**
         * The default endpoint suspend duration on failure (i hour)
         */
        public static final long DEFAULT_ENDPOINT_SUSPEND_TIME = 30 * 1000;

        /**
         * This is a system wide interval for handling otherwise non-expiring callbacks to
         * ensure system stability over a period of time 
         */
        public static final String GLOBAL_TIMEOUT_INTERVAL = "synapse.global_timeout_interval";

        /**
         * this is the timeout for otherwise non-expiring callbacks
         * to ensure system stability over time
         */
        public static final long DEFAULT_GLOBAL_TIMEOUT = 24 * 60 * 60 * 1000;

        /**
         * don't do anything for response timeouts. this means infinite timeout. this is the default
         * action, if the timeout configuration is not explicitly set.
         */
        public static final int NONE = 100;

        /** Discard the callback if the timeout for the response is expired */
        public static final int DISCARD = 101;

        /**
         * Discard the callback and activate specified fault sequence if the timeout for the response
         * is expired
         */
        public static final int DISCARD_AND_FAULT = 102;

        /**
         * Error codes for message sending. We go with closest HTTP fault codes.
         */
        public static final int HANDLER_TIME_OUT = 101504;

    //- Endpoints processing constants -
    /** Property name to store the last endpoint through which the message has flowed */
    public static final String LAST_ENDPOINT = "last_endpoint";

    /** Property name to store the endpoint_log that stores the history */
    public static final String ENDPOINT_LOG = "endpoint_log";     
    
    /** A name to use for anonymous endpoints */
    public static final String ANONYMOUS_ENDPOINT = "AnonymousEndpoint";

    /**A name to use for anonymous proxyservice  */
    public static final String ANONYMOUS_PROXYSERVICE = "AnonymousProxyService";

    /**A name to use for anonymous API  */
    public static final String ANONYMOUS_API = "AnonymousApi";

    /* Constants related to the SAL endpoints */

    public static final String PROP_SAL_ENDPOINT_FIRST_MESSAGE_IN_SESSION
            = "synapse.sal.first_message_in_session";

    public static final String PROP_SAL_ENDPOINT_ENDPOINT_LIST
            = "synapse.sal.endpoint.list";
    
    public static final String PROP_SAL_CURRENT_SESSION_INFORMATION
            = "synapse.sal.endpoint.current.sessioninformation";

    public static final String PROP_SAL_ENDPOINT_CURRENT_ENDPOINT_LIST
            = "synapse.sal.current.endpoint.list";

    public static final String PROP_SAL_ENDPOINT_CURRENT_MEMBER
            = "synapse.sal.current.member";

    public static final String PROP_SAL_ENDPOINT_CURRENT_DISPATCHER
            = "synape.sal.endpoints.dispatcher";   

    public static final String PROP_SAL_ENDPOINT_DEFAULT_SESSION_TIMEOUT
            = "synapse.sal.endpoints.sesssion.timeout.default";    

    public static final long SAL_ENDPOINTS_DEFAULT_SESSION_TIMEOUT = 120000;
    

    /** A name to use for anonymous sequences in the sequence stack */
    public static final String ANONYMOUS_SEQUENCE = "AnonymousSequence";

    /** String to be used as the separator when defining resource IDs for statistics */
    public static final String STATISTICS_KEY_SEPARATOR = "__";

    /** Message format values in EndpointDefinition. Used by address, wsdl endpoints */
    public static final String FORMAT_POX = "pox";
    public static final String FORMAT_GET = "get";
    public static final String FORMAT_SOAP11 = "soap11";
    public static final String FORMAT_SOAP12 = "soap12";    
    public static final String FORMAT_REST = "rest";

    // - Blocking Message Sender Constants
    public static final String BLOCKING_SENDER_ERROR = "blocking.sender.error";
    public static final String SET_ROLLBACK_ONLY = "SET_ROLLBACK_ONLY";
    public static final String SET_REQUEUE_ON_ROLLBACK = "SET_REQUEUE_ON_ROLLBACK";
    public static final String ACK_ON_SUCCESS = "ACKNOWLEDGE";
    public static final String MESSAGING_CALLBACK_CONFIGS = "messaging_callback";
    public static final String CALLBACK_CONTROLLED_ACK = "callback_controlled_ack_enabled";
    public static final String PRESERVE_PAYLOAD_ON_TIMEOUT = "preserve_payload_on_timeout";
    public static final String ACKNOWLEDGEMENT_DECISION = "ACK_DECISION";

    public static final String HTTP_SC = "HTTP_SC";
    public static final String HTTP_SENDER_STATUSCODE = "transport.http.statusCode";
    public static final String BLOCKING_SENDER_PRESERVE_REQ_HEADERS =
            "BLOCKING_SENDER_PRESERVE_REQ_HEADERS";
    public static final String DISABLE_CHUNKING = "DISABLE_CHUNKING";
    public static final String NO_DEFAULT_CONTENT_TYPE = "NoDefaultContentType";
    // Synapse property to store Blocking Message Sender to do blocking invocation
    public static final String BLOCKING_MSG_SENDER = "blockingMsgSender";

    /** Synapse server instance name */
    public static final String SERVER_NAME = "serverName";

    /** Root for relative path */
    public static final String RESOLVE_ROOT = "resolve.root";

    public static final String CLASS_MEDIATOR_LOADERS = "CLASS_MEDIATOR_LOADERS";

    /* The deployment mode */
    public static final String DEPLOYMENT_MODE = "deployment.mode";

    /* URL connection read timeout and connection timeout */

    public static final int DEFAULT_READTIMEOUT = 100000;

    public static final int DEFAULT_CONNECTTIMEOUT = 20000;

    public static final String READTIMEOUT = "synapse.connection.read_timeout";

    public static final String CONNECTTIMEOUT = "synapse.connection.connect_timeout";

    /** chunk size and chunk length configuration parameters */
    public static final int DEFAULT_THRESHOLD_CHUNKS = 8;

    public static final int DEFAULT_CHUNK_SIZE = 1024;

    public static final String DEFAULT_TEMPFILE_PREFIX = "tmp_";

    public static final String DEFAULT_TEMPFILE_SUFIX = ".dat";

    public static final String THRESHOLD_CHUNKS = "synapse.temp_data.chunk.threshold";
    
    public static final String CHUNK_SIZE = "synapse.temp_data.chunk.size";

    public static final String TEMP_FILE_PREFIX = "synapse.tempfile.prefix";
    
    public static final String TEMP_FILE_SUFIX = "synapse.tempfile.sufix";

    public static final String DOING_FAIL_OVER = "synapse.doing.failover";

    // to be a help for stat collection
    public static final String SENDING_REQUEST = "synapse.internal.request.sending";

    public static final String SYNAPSE_STARTUP_TASK_SCHEDULER = "synapse.startup.taskscheduler";

    public static final String SYNAPSE_STARTUP_TASK_DESCRIPTIONS_REPOSITORY =
            "synapse.startup.taskdescriptions.repository";

    /** proxy server configurations used for retrieving configuration resources over a HTTP proxy */
    public static final String SYNPASE_HTTP_PROXY_HOST = "synapse.http.proxy.host";
    public static final String SYNPASE_HTTP_PROXY_PORT = "synapse.http.proxy.port";
    public static final String SYNPASE_HTTP_PROXY_USER = "synapse.http.proxy.user";
    public static final String SYNPASE_HTTP_PROXY_PASSWORD = "synapse.http.proxy.password";
    public static final String SYNAPSE_HTTP_PROXY_EXCLUDED_HOSTS =
            "synapse.http.proxy.excluded.hosts";

    // host and ip synapse is running 
    public static final String SERVER_HOST = "SERVER_HOST";

    public static final String SERVER_IP = "SERVER_IP";

    // Property name. If this property is false synapse will not remove the processed headers
    public static final String PRESERVE_PROCESSED_HEADERS = "preserveProcessedHeaders";
    // Property name for preserving the envelope before sending
    public static final String PRESERVE_ENVELOPE = "PRESERVE_ENVELOPE";
    // Property name for preserving the envelope in case of loadbalance failover endpoint with content aware scenario
    public static final String LB_FO_ENDPOINT_ORIGINAL_MESSAGE = "LB_FO_ENDPOINT_ORIGINAL_MESSAGE";

    // Known transport error codes
    public static final int RCV_IO_ERROR_SENDING     = 101000;
    public static final int RCV_IO_ERROR_RECEIVING   = 101001;

    public static final int SND_IO_ERROR_SENDING     = 101500;
    public static final int SND_IO_ERROR_RECEIVING   = 101501;

    public static final int NHTTP_CONNECTION_FAILED           = 101503;
    public static final int NHTTP_CONNECTION_TIMEOUT          = 101504;
    public static final int NHTTP_CONNECTION_CLOSED           = 101505;
    public static final int NHTTP_PROTOCOL_VIOLATION          = 101506;
    public static final int NHTTP_CONNECT_CANCEL              = 101507;
    public static final int NHTTP_CONNECT_TIMEOUT             = 101508;
    public static final int NHTTP_RESPONSE_PROCESSING_FAILURE = 101510;

    // Endpoint failures
    public static final int ENDPOINT_LB_NONE_READY   = 303000;
    public static final int ENDPOINT_RL_NONE_READY   = 303000;
    public static final int ENDPOINT_FO_NONE_READY   = 303000;
    public static final int ENDPOINT_ADDRESS_NONE_READY = 303001;
    public static final int ENDPOINT_WSDL_NONE_READY = 303002;
    public static final int ENDPOINT_AUTH_FAILURE = 303003;
    // Failure on endpoint in the session 
    public static final int ENDPOINT_SAL_NOT_READY = 309001;
    public static final int ENDPOINT_SAL_INVALID_PATH = 309002;
    public static final int ENDPOINT_SAL_FAILED_SESSION = 309003;

    // endpoints, non fatal warnings etc
    public static final int ENDPOINT_LB_FAIL_OVER    = 303100;
    public static final int ENDPOINT_FO_FAIL_OVER    = 304100;

    // referring real endpoint is null
    public static final int ENDPOINT_IN_DIRECT_NOT_READY = 305100;

    // callout operation failed
    public static final int CALLOUT_OPERATION_FAILED    = 401000;

    // Blocking call operation failure
    public static final int BLOCKING_CALL_OPERATION_FAILED    = 401001;

    // Blocking sender operation failure
    public static final int BLOCKING_SENDER_OPERATION_FAILED    = 401002;

    public static final int NON_BLOCKING_CALL_OPERATION_FAILED = 401003;

    public static final String FORCE_ERROR_PROPERTY = "FORCE_ERROR_ON_SOAP_FAULT";
    public static final int ENDPOINT_CUSTOM_ERROR = 500000;

    // Error code for XML/JSON parsing errors
    public static final int MESSAGE_PARSING_ERROR = 601000;

    // Fail-safe mode properties
    public static final String FAIL_SAFE_MODE_STATUS = "failsafe.mode.enable";
    public static final String FAIL_SAFE_MODE_ALL = "all";
    public static final String FAIL_SAFE_MODE_PROXY_SERVICES = "proxyservices";
    public static final String FAIL_SAFE_MODE_EP = "endpoints";
    public static final String FAIL_SAFE_MODE_LOCALENTRIES = "localentries";
    public static final String FAIL_SAFE_MODE_SEQUENCES = "sequences";
    public static final String FAIL_SAFE_MODE_EVENT_SOURCE = "eventsources";
    public static final String FAIL_SAFE_MODE_EXECUTORS = "executors";
    public static final String FAIL_SAFE_MODE_TEMPLATES = "templates";
    public static final String FAIL_SAFE_MODE_MESSAGE_PROCESSORS = "messageprocessors";
    public static final String FAIL_SAFE_MODE_MESSAGE_STORES = "messagestores";
    public static final String FAIL_SAFE_MODE_API = "api";
    public static final String FAIL_SAFE_MODE_INBOUND_ENDPOINT = "inboundendpoint";
    public static final String FAIL_SAFE_MODE_IMPORTS = "import";
    public static final String FAIL_SAFE_MODE_TASKS = "task";
    public static final String FAIL_SAFE_MODE_REGISTRY = "registry";
    public static final String FAIL_SAFE_MODE_TASK_MANAGER = "taskManager";

    //fall back XPATH support (default javax.xml style xpath processing which can support XPATH 2.0)
    public static final String FAIL_OVER_DOM_XPATH_PROCESSING = "synapse.xpath.dom.failover.enabled";

    //Streaming XPATH Support
    public static final String STREAMING_XPATH_PROCESSING = "synapse.streaming.xpath.enabled";

    //Streaming Json Path
    public static final String STREAMING_JSONPATH_PROCESSING = "synapse.streaming.jsonpath.enabled";

    //Enable message building when doing failover
    public static final String BUILD_MESSAGE_ON_FAILOVER = "build.message.on.failover.enable";

    /**
     * Message content property of incoming transport-in name
     */
    public static final String TRANSPORT_IN_NAME = "TRANSPORT_IN_NAME";
    
    // Synapse config path
    public static final String SYNAPSE_CONFIGS = "synapse-configs";
    public static final String DEFAULT_DIR = "default";
    public static final String SEQUENCES_FOLDER = "sequences";

    //Inbound endpoint
    public static final String IS_INBOUND = "isInbound";
    public static final String IS_CXF_WS_RM="is_cxf_ws_rm";
    public static final String INBOUND_PROXY_SERVICE_PARAM="inbound.only";
    public static final String INBOUND_JMS_PROTOCOL = "INBOUND_JMS_PROTOCOL";
    public static final String INBOUND_ENDPOINT_NAME = "inbound.endpoint.name";
    public static final String TASK_NAME = "task.name";
    public static final String INBOUND_STATISTIC_ENABLE = "inbound.statistic.enable";
    public static final String ANNONYMOUS_INBOUND_ENDPOINT_NAME = "AnonymousInboundEndpoint";
    
    //Sandesha
    public static final String SANDESHA2_SEQUENCE_KEY = "Sandesha2SequenceKey";


    //Synapse concurrency throttling
    public static final String SYNAPSE_CONCURRENCY_THROTTLE =
            "synapse.concurrency.throttle";
    public static final String SYNAPSE_CONCURRENCY_THROTTLE_KEY =
            "synapse.concurrency.throttle.key";
    public static final String SYNAPSE_CONCURRENT_ACCESS_CONTROLLER =
            "synapse.concurrent.access.controller";
    public static final String SYNAPSE_CONCURRENT_ACCESS_REPLICATOR =
            "synapse.concurrent.access.replicator";

    /**
     * Keeps the state whether the request is accepted or not
     */
    public static final String SYNAPSE_IS_CONCURRENT_ACCESS_ALLOWED = "synapse.is.concurrent.access.allowed";

    //String constants to identity the type of the timeout
    public enum ENDPOINT_TIMEOUT_TYPE { ENDPOINT_TIMEOUT, GLOBAL_TIMEOUT, HTTP_CONNECTION_TIMEOUT};

    // URL pattern
    public static final Pattern URL_PATTERN = Pattern.compile("[a-zA-Z0-9]+://.*");

    // Password pattern
    public static final Pattern PASSWORD_PATTERN = Pattern.compile(":(?:[^/]+)@");

    public static final String NO_KEEPALIVE = "NO_KEEPALIVE";

    // Common property for all artifacts
    public static final String ARTIFACT_NAME = "ARTIFACT_NAME";

    // Keeps the state whether the error flow was executed previously
    public static final String IS_ERROR_COUNT_ALREADY_PROCESSED = "IS_ERROR_COUNT_ALREADY_PROCESSED";

    //This synapse property will be read in the mediation layer to decide whether to save artifacts to a local
    // directory or not. By default this property is set to true.
    public static final String STORE_ARTIFACTS_LOCALLY = "synapse.artifacts.file.storage.enabled";

    public static final int DEFAULT_MAX_FAILOVER_RETRIES = -1; //Default set to unlimited retries
    public static final String MAX_FAILOVER_RETRIES_CONFIG = "maximum.failover.retries";
    public static final String SUSPEND_DURATION_ON_MAX_FAILOVER_CONFIG = "suspend.duration.on.maximum.failover";

    public static final String MAX_FAILOVER_RECUSIVE_RETRIES_CONFIG = "maximum.failover.recursive.retries";
    public static final String SUSPEND_DURATION_ON_MAX_RECURSIVE_FAILOVER_CONFIG =
            "suspend.duration.on.maximum.recursive.failover";
    public static final String EXCLUDE_PAYLOAD_DETAILS_FROM_ERROR = "exclude.payload.details.from.error";

    /**
     * Synapse Configuration holder property name, used for handling synapse import deployments
     */
    public static final String SYNAPSE_CONFIGURATION = "SynapseConfiguration";

    //Jaeger SpanID tracerID constants
    public static final String JAEGER_TRACE_ID = "jaeger_trace_id";
    public static final String JAEGER_SPAN_ID = "jaeger_span_id";

    public static final String ANALYTICS_METADATA = "ANALYTICS_METADATA";

    // Constants for the HTTP Connection
    public static final String ENDPOINT_IDENTIFIER = "_INTERNAL_ENDPOINT_REFERENCE";
    public static final String BASIC_AUTH = "Basic Auth";
    public static final String OAUTH = "OAuth";
    public static final String OAUTH_GRANT_TYPE_AUTHORIZATION_CODE = "Authorization Code";
    public static final String OAUTH_GRANT_TYPE_CLIENT_CREDENTIALS = "Client Credentials";
    public static final String OAUTH_GRANT_TYPE_PASSWORD = "Password";

    public static final String NAME = "name";
    public static final String AUTH_TYPE = "authType";
    public static final String OAUTH_AUTHORIZATION_MODE = "oauthAuthorizationMode";
    public static final String OAUTH_GRANT_TYPE = "oauthGrantType";
    public static final String TIMEOUT_DURATION = "timeoutDuration";
    public static final String TIMEOUT_ACTION = "timeoutAction";
    public static final String SUSPEND_ERROR_CODES = "suspendErrorCodes";
    public static final String SUSPEND_INITIAL_DURATION = "suspendInitialDuration";
    public static final String SUSPEND_MAXIMUM_DURATION = "suspendMaximumDuration";
    public static final String SUSPEND_PROGRESSION_FACTOR = "suspendProgressionFactor";
    public static final String RETRY_ERROR_CODES = "retryErrorCodes";
    public static final String RETRY_COUNT = "retryCount";
    public static final String RETRY_DELAY = "retryDelay";

    // Constants for statistics details
    public static final String STATISTICS_METADATA = "statisticsMetadata";
    public static final String INBOUND_PORT = "inboundPort";
    public static final String PORT = "port";
    public static final String HOSTNAME = "host";
    public static final String CONNECTION = "connection";
    public static final String FILE_URI = "fileURI";
    public static final String TOPIC = "topic";
    public static final String QUEUE = "queue";

    public static final String TRACE = "trace";
    public static final String STATISTICS = "statistics";
    public static final String MISCELLANEOUS_DESCRIPTION = "miscellaneousDescription";
    public static final String MISCELLANEOUS_PROPERTIES = "miscellaneousProperties";
    public static final String QUALITY_OF_SERVICE_ADDRESS_OPTION = "qualityServiceAddressOption";
    public static final String QUALITY_OF_SERVICE_ADDRESS_VERSION = "qualityServiceAddressVersion";
    public static final String QUALITY_OF_SERVICE_ADDRESS_SEPARATE_LISTENER = "qualityServiceAddressSeparateListener";

    public static final String QUALITY_OF_SERVICE_SECURITY_OPTION = "qualityServiceSecurityOption";
    public static final String QUALITY_OF_SERVICE_SECURITY_INBOUND_OUTBOUND_POLICY_OPTION = "qualityServiceSecurityInboundOutboundPolicyOption";
    public static final String QUALITY_OF_SERVICE_SECURITY_INBOUND_POLICY_KEY = "qualityServiceSecurityInboundPolicyKey";
    public static final String QUALITY_OF_SERVICE_SECURITY_OUTBOUND_POLICY_KEY = "qualityServiceSecurityOutboundPolicyKey";
    public static final String QUALITY_OF_SERVICE_SECURITY_POLICY_KEY = "qualityServiceSecurityPolicyKey";

    public static final String ENDPOINT = "endpoint";
    public static final String HTTP = "http";
    public static final String ENABLE = "enable";
    public static final String URI_TEMPLATE = "uri-template";
    public static final String ENABLE_ADDRESSING = "enableAddressing";
    public static final String ENABLE_SECURITY = "enableSec";
    public static final String SEPARATE_LISTENER = "separateListener";
    public static final String VERSION = "version";
    public static final String POLICY = "policy";
    public static final String INBOUND_POLICY = "inboundPolicy";
    public static final String OUTBOUND_POLICY = "outboundPolicy";
    public static final String TIMEOUT = "timeout";
    public static final String DURATION = "duration";
    public static final String RESPONSE_ACTION = "responseAction";
    public static final String NEVER = "never";
    public static final String SUSPEND_ON_FAILURE = "suspendOnFailure";
    public static final String ERROR_CODES = "errorCodes";
    public static final String INITIAL_DURATION = "initialDuration";
    public static final String MAXIMUM_DURATION = "maximumDuration";
    public static final String PROGRESSION_FACTOR = "progressionFactor";
    public static final String MARK_FOR_SUSPENSION = "markForSuspension";
    public static final String RETRIES_BEFORE_SUSPENSION = "retriesBeforeSuspension";
    public static final String AUTHENTICATION = "authentication";
    public static final String DESCRIPTION = "description";
    public static final String AUTHORIZATION_CODE = "authorizationCode";
    public static final String PASSWORD_CREDENTIALS = "passwordCredentials";
    public static final String CLIENT_CREDENTIALS = "clientCredentials";
    public static final String TOKEN_URL = "tokenUrl";
    public static final String CLIENT_ID = "clientId";
    public static final String CLIENT_SECRET = "clientSecret";
    public static final String OAUTH_CONNECTION_TIMEOUT = "oauthConnectionTimeout";
    public static final String OAUTH_CONNECTION_REQUEST_TIMEOUT = "oauthConnectionRequestTimeout";
    public static final String OAUTH_SOCKET_TIMEOUT = "oauthSocketTimeout";
    public static final String CONNECTION_TIMEOUT = "connectionTimeout";
    public static final String CONNECTION_REQUEST_TIMEOUT = "connectionRequestTimeout";
    public static final String SOCKET_TIMEOUT = "socketTimeout";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String REFRESH_TOKEN = "refreshToken";
    public static final String AUTH_MODE = "authMode";
    public static final String ADDITIONAL_PROPERTIES = "additionalProperties";
    public static final String REQUEST_PARAMETERS = "requestParameters";
    public static final String BASIC_AUTH_TAG = "basicAuth";
    public static final String OAUTH_TAG = "oauth";
    public static final String SCATTER_MESSAGES = "SCATTER_MESSAGES";
    public static final String CONTINUE_FLOW_TRIGGERED_FROM_MEDIATOR_WORKER = "CONTINUE_FLOW_TRIGGERED_FROM_MEDIATOR_WORKER";
    public static final String RESPONSE_VARIABLE = "responseVariable";
    public static final String OVERWRITE_BODY = "overwriteBody";
    public static final String ORIGINAL_PAYLOAD = "ORIGINAL_PAYLOAD";
    public static final String BEFORE_INVOKE_TEMPLATE = "BEFORE_INVOKE_TEMPLATE";

    public static final String DEFAULT_ERROR_TYPE = "ANY";
    public static final String ERROR_STATS_REPORTED = "ERROR_STATS_REPORTED";

    public static final String ENABLE_DTD_FORCEFULLY = "payloadFactory.forcefully.enable.DTD";
    public static final String ENABLE_EXTERNAL_ENTITY_FORCEFULLY
        = "payloadFactory.forcefully.enable.external.entity";

    public static final String JSONPATH_IGNORE_NOT_FOUND_ERROR
        = "synapse.jsonpath.ignore.not.found.error";

    public static final String SYNAPSE_ARTIFACT_DEPENDENCIES = "synapse.artifact.dependencies";
    public static final String SYNAPSE_ARTIFACT_IDENTIFIER = "synapse.artifact.identifier";
    public static final String SYNAPSE_ARTIFACT_VERSIONED_DEPLOYMENT = "synapse.artifact.versioned.deployment";
    public static final String CONNECTOR_ARTIFACT = "CONNECTOR_ARTIFACT";
    public static final String APPEND_ARTIFACT_IDENTIFIER = "APPEND_ARTIFACT_IDENTIFIER";
    public static final String EXPOSE_VERSIONED_SERVICES = "expose.versioned.services";

    public static final String SKIP_MAIN_SEQUENCE = "SKIP_MAIN_SEQUENCE";
    // Global endpoint properties and defaults
    public static final String GLOBAL_ENDPOINT_SUSPEND_DURATION = "synapse.global_endpoint_suspend_duration";
    public static final String DEFAULT_GLOBAL_ENDPOINT_SUSPEND_DURATION = "-1";
    public static final String GLOBAL_ENDPOINT_SUSPEND_PROGRESSION_FACTOR =
            "synapse.global_endpoint_suspend_progression_factor";
    public static final String DEFAULT_GLOBAL_ENDPOINT_SUSPEND_PROGRESSION_FACTOR = "1.0";

    // Property to override endpoint configurations with global values
    // When this property is set to true, endpoint suspend durations and
    // progression factors will be overridden with the global values
    public static final String OVERRIDE_ENDPOINT_SUSPEND_CONFIG_WITH_GLOBAL_VALUES =
            "synapse.override_endpoint_suspend_config_with_global_values";

    /**
     * Name of the custom data provider class that can add new properties to the elastic search
     * properties.
     */
    public static final String ELASTICSEARCH_CUSTOM_DATA_PROVIDER_CLASS
        = "analytics.custom_data_provider_class";
}
