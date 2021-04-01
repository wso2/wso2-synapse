/*
 * Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.apache.synapse.commons;

/**
 * constants for correlation logging
 */
public class CorrelationConstants {

    //property to set the correlation id value in message context and http context
    public static final String CORRELATION_ID = "correlation_id";
    //property to set the correlation ID as a MDC property in log4J
    public static final String CORRELATION_MDC_PROPERTY = "Correlation-ID";
    public static final String CORRELATION_LOGS_SYS_PROPERTY = "enableCorrelationLogs";
}
