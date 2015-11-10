/**
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

public class SQLQueries {

    public static final String INSERT_MESSAGE_FLOW_INFO_ENTRY = "INSERT INTO "+MessageFlowTracerConstants.TABLE_MESSAGE_FLOW_INFO
            +" (MessageId, Component, ComponentId, Response, Start, Payload, Properties, Timestamp) VALUES (?,?,?,?,?,?,?,?)";

    public static final String INSERT_MESSAGE_FLOWS_ENTRY = "INSERT INTO "+MessageFlowTracerConstants.TABLE_MESSAGE_FLOWS
            + " (MessageId, FlowTrace, EntryType, TimeStamp) VALUES(?,?,?,?)";

    public static final String GET_MESSAGE_FLOW_TRACE = "SELECT * FROM "+MessageFlowTracerConstants.TABLE_MESSAGE_FLOWS+" WHERE messageId = ? ";

    public static final String GET_ALL_MESSAGE_FLOWS = "SELECT distinct ID,MessageId,EntryType,TimeStamp FROM " + MessageFlowTracerConstants.TABLE_MESSAGE_FLOWS+" ORDER BY ID";

    public static final String GET_COMPONENT_INFO = "SELECT * FROM " + MessageFlowTracerConstants.TABLE_MESSAGE_FLOW_INFO+" WHERE MessageId = ? ORDER BY ID";

    public static final String DELETE_ALL = "DELETE FROM ";
}
