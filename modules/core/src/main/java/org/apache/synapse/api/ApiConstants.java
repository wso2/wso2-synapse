/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.api;

public class ApiConstants {

    private ApiConstants() {
        // Prevents Instantiation
    }

    public static String WS_PROTOCOL = "ws";
    public static String WSS_PROTOCOL = "wss";
    public static String HTTP_PROTOCOL = "http";
    public static String HTTPS_PROTOCOL = "https";
    public static String API_CALLER = "api.caller";

    /* Constants for inbound APIs */
    public static String BINDS_TO = "binds-to";
    public static String DEFAULT_BINDING_ENDPOINT_NAME = "default";
}
