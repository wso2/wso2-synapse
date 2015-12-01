/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.debug.constants;

public class SynapseDebugCommandConstants {

    public static final String DEBUG_COMMAND = "command";
    public static final String DEBUG_COMMAND_ARGUMENT = "command-argument";
    public static final String DEBUG_COMMAND_MEDIATION_COMPONENT = "mediation-component";
    public static final String DEBUG_COMMAND_MEDIATION_COMPONENT_TEMPLATE = "template";
    public static final String DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE = "sequence";
    public static final String DEBUG_COMMAND_MEDIATION_COMPONENT_CONNECTOR = "connector";
    public static final String DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY = "proxy";
    public static final String DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API = "api";
    public static final String DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_INBOUND = "inbound";
    public static final String DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE = "resource";
    public static final String DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE_METHOD = "method";
    public static final String DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE_URI_TEMPLATE = "uri-template";
    public static final String DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE_URL_MAPPING = "url-mapping";
    public static final String DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_RESOURCE_MAPPING = "mapping";
    public static final String DEBUG_COMMAND_MEDIATION_COMPONENT_CONNECTOR_KEY = "connector-key";
    public static final String DEBUG_COMMAND_MEDIATION_COMPONENT_CONNECTOR_METHOD = "method-name";
    public static final String DEBUG_COMMAND_MEDIATION_COMPONENT_MEDIATOR_POSITION = "mediator-position";
    public static final String DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_TYPE = "sequence-type";
    public static final String DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_KEY = "sequence-key";
    public static final String DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_PROXY_KEY = "proxy-key";
    public static final String DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_API_KEY = "api-key";
    public static final String DEBUG_COMMAND_MEDIATION_COMPONENT_SEQUENCE_INBOUND_KEY = "inbound-key";
    public static final String DEBUG_COMMAND_MEDIATION_COMPONENT_TEMPLATE_KEY = "template-key";
    public static final String DEBUG_COMMAND_CLEAR = "clear";
    public static final String DEBUG_COMMAND_GET = "get";
    public static final String DEBUG_COMMAND_PROPERTY = "property";
    public static final String DEBUG_COMMAND_PROPERTY_NAME = "property-name";
    public static final String DEBUG_COMMAND_PROPERTY_VALUE = "property-value";
    public static final String DEBUG_COMMAND_PROPERTIES = "properties";
    public static final String DEBUG_COMMAND_PROPERTY_CONTEXT = "context";
    public static final String DEBUG_COMMAND_PROPERTY_CONTEXT_ALL = "all";
    public static final String DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2 = "axis2";
    public static final String DEBUG_COMMAND_PROPERTY_CONTEXT_SYNAPSE = "synapse";
    public static final String DEBUG_COMMAND_PROPERTY_CONTEXT_DEFAULT = "default";
    public static final String DEBUG_COMMAND_PROPERTY_CONTEXT_AXIS2CLIENT = "axis2-client";
    public static final String DEBUG_COMMAND_PROPERTY_CONTEXT_TRANSPORT = "transport";
    public static final String DEBUG_COMMAND_PROPERTY_CONTEXT_OPERATION = "operation";
    public static final String DEBUG_COMMAND_RESPONSE_PROPERTY_CONTEXT_AXIS2 = "axis2-properties";
    public static final String DEBUG_COMMAND_RESPONSE_PROPERTY_CONTEXT_SYNAPSE = "synapse-properties";
    public static final String DEBUG_COMMAND_RESPONSE_PROPERTY_CONTEXT_AXIS2CLIENT = "axis2Client-properties";
    public static final String DEBUG_COMMAND_RESPONSE_PROPERTY_CONTEXT_AXIS2TRANSPORT = "axis2Transport-properties";
    public static final String DEBUG_COMMAND_RESPONSE_PROPERTY_CONTEXT_AXIS2OPERATION = "axis2Operation-properties";
    public static final String DEBUG_COMMAND_EXIT = "exit";
    public static final String DEBUG_COMMAND_RESUME = "resume";
    public static final String DEBUG_COMMAND_STEP = "step";
    public static final String DEBUG_COMMAND_SET = "set";
    public static final String DEBUG_COMMAND_BREAKPOINT = "breakpoint";
    public static final String DEBUG_COMMAND_SKIP = "skip";
    public static final String DEBUG_COMMAND_SUCCESS = "ok";
    public static final String DEBUG_COMMAND_FAILED = "failed";
    public static final String DEBUG_COMMAND_RESPONSE = "command-response";
    public static final String DEBUG_COMMAND_RESPONSE_SUCCESSFUL = "successful";
    public static final String DEBUG_COMMAND_RESPONSE_FAILED = "failed";
    public static final String DEBUG_COMMAND_RESPONSE_FAILED_REASON = "failed-reason";
    public static final String DEBUG_COMMAND_RESPONSE_COMMAND_NOT_FOUND = "command not found";
    public static final String DEBUG_COMMAND_RESPONSE_UNABLE_TO_REGISTER_FLOW_POINT = "unable to register mediation flow point";
    public static final String DEBUG_COMMAND_RESPONSE_API_RESOURCE_NOT_FOUND = "api resource not found";
    public static final String DEBUG_COMMAND_RESPONSE_API_NOT_FOUND = "api not found";
    public static final String DEBUG_COMMAND_RESPONSE_PROXY_NOT_FOUND = "proxy not found";
    public static final String DEBUG_COMMAND_RESPONSE_INBOUND_NOT_FOUND = "inbound not found";
    public static final String DEBUG_COMMAND_RESPONSE_ALREADY_SKIP_ENABLED = "already skip enabled at mediator position";
    public static final String DEBUG_COMMAND_RESPONSE_ALREADY_SKIP_DISABLED = "already skip disabled at mediator position";
    public static final String DEBUG_COMMAND_RESPONSE_ALREADY_BREAKPOINT_ENABLED = "already breakpoint enabled at mediator position";
    public static final String DEBUG_COMMAND_RESPONSE_ALREADY_BREAKPOINT_DISABLED = "already breakpoint disabled at mediator position";
    public static final String DEBUG_COMMAND_RESPONSE_NON_EXISTING_MEDIATOR_POSITION = "non existing mediator position";
    public static final String DEBUG_COMMAND_RESPONSE_NON_EXISTING_SEQUENCE = "non existing sequence";
    public static final String DEBUG_COMMAND_RESPONSE_NON_EXISTING_TEMPLATE = "non existing template";

    public static final String AXIS2_PROPERTY_TO = "To";
    public static final String AXIS2_PROPERTY_FROM = "From";
    public static final String AXIS2_PROPERTY_WSACTION = "WSAction";
    public static final String AXIS2_PROPERTY_SOAPACTION = "SOAPAction";
    public static final String AXIS2_PROPERTY_REPLY_TO = "ReplyTo";
    public static final String AXIS2_PROPERTY_MESSAGE_ID = "MessageID";
    public static final String AXIS2_PROPERTY_DIRECTION = "Direction";
    public static final String AXIS2_PROPERTY_ENVELOPE = "Envelope";
    public static final String AXIS2_PROPERTY_SOAPHEADER = "SoapHeader";
    public static final String AXIS2_PROPERTY_TRANSPORT_HEADERS = "TransportHeaders";
    public static final String AXIS2_PROPERTY_EXCESS_TRANSPORT_HEADERS = "ExcessTransportHeaders";
    public static final String AXIS2_PROPERTY_MESSAGE_TYPE = "MessageType";
    public static final String AXIS2_PROPERTY_CONTENT_TYPE = "ContentType";

}
