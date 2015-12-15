/**
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.flowtracer;

public class MessageFlowTracerConstants {

    public static final String MESSAGE_FLOW_ID = "MESSAGE_FLOW_ID";
    public static final String MESSAGE_FLOW = "MESSAGE_FLOW";
    public static final String MESSAGE_FLOW_ENTRY_TYPE = "MESSAGE_FLOW_ENTRY_TYPE";
    public static final String MESSAGE_FLOW_TRACE_ENABLE = "message.flow.trace.enable";

    //Entry Types
    public  static final String ENTRY_TYPE_MAIN_SEQ = "Main Sequence:";
    public  static final String ENTRY_TYPE_PROXY_SERVICE = "Main Sequence";
    public  static final String ENTRY_TYPE_INBOUND_ENDPOINT = "Inbound Endpoint:";
    public  static final String ENTRY_TYPE_REST_API = "REST_API:";
}
