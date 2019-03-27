/*
 Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.unittest;

/**
 * Constants for unit testing framework for synapse.
 */
public class Constants {

    private Constants() {}

    static final String TYPE_SEQUENCE = "sequence";
    static final String TYPE_PROXY = "proxy";
    static final String TYPE_API = "api";
    static final String TYPE_ENDPOINT = "endpoint";
    static final String TYPE_LOCAL_ENTRY = "localEntry";

    static final String API_CONTEXT = "context";
    static final String RESOURCE_METHODS = "methods";
    static final String ARTIFACT_TYPE = "artifact-type";
    static final String ARTIFACT_NAME_ATTRIBUTE = "name";
    static final String ARTIFACT_KEY_ATTRIBUTE = "key";
    static final String ARTIFACT_NAME = "artifact-name";
    static final String ARTIFACT = "artifact";
    static final String TEST_ARTIFACT = "test-artifact";
    static final String SUPPORTIVE_ARTIFACTS = "supportive-artifacts";
    static final String ARTIFACTS = "artifacts";

    static final String TEST_CASES = "test-cases";
    static final String TEST_CASES_COUNT = "test-cases-count";
    static final String INPUT_PAYLOAD = "input-payload";
    static final String ASSERTION = "assertion";
    static final String ASSERT_EXPECTED_PROPERTIES = "expected-properties";
    static final String ASSERT_EXPECTED_PAYLOAD = "expected-payload";

    static final String SERVICE_NAME = "service-name";
    static final String SERVICE_HOST = "localhost";
    static final String SERVICE_PORT = "port";
    static final String SERVICE_CONTEXT = "context";
    static final String SERVICE_RESOURCES = "resources";
    static final String SERVICE_RESOURCE = "resource";
    static final String SERVICE_RESOURCE_SUBCONTEXT = "sub-context";
    static final String SERVICE_RESOURCE_METHOD = "method";
    static final String SERVICE_RESOURCE_REQUEST = "request";
    static final String SERVICE_RESOURCE_RESPONSE = "response";
    static final String SERVICE_RESOURCE_PAYLOAD = "payload";
    static final String SERVICE_RESOURCE_HEADERS = "headers";
    static final String SERVICE_RESOURCE_HEADER_NAME = "name";
    static final String SERVICE_RESOURCE_HEADER_VALUE = "value";

    static final String SERVICE_TYPE = "mock-service-type";
    static final String SERVICE_RESPONSE = "mock-service-expected-response";
    static final String MOCK_SERVICES = "mock-services";
    static final String END_POINT = "endpoint";
    public static final String URI = "uri";
    static final String URI_TEMPLATE = "uri-template";
    public static final String METHOD = "method";

    public static final String HTTP = "http://";
    public static final String GET_METHOD = "GET";
    public static final String POST_METHOD = "POST";
    static final String PUT_METHOD = "PUT";
    static final String DELETE_METHOD = "DELETE";

    static final String WHITESPACE_REGEX = "\\s(?=(\"[^\"]*\"|[^\"])*$)";

}
