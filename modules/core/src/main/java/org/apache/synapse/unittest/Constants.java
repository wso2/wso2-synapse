/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

    private Constants() {
    }

    //deployer constants
    static final String TYPE_SEQUENCE = "sequence";
    static final String TYPE_PROXY = "proxy";
    static final String TYPE_API = "api";
    static final String TYPE_ENDPOINT = "endpoint";
    static final String TYPE_LOCAL_ENTRY = "localEntry";

    //artifact key word constants
    static final String API_CONTEXT = "context";
    static final String NAME_ATTRIBUTE = "name";
    static final String ARTIFACT_TRANSPORTS_ATTRIBUTE = "transports";
    static final String ARTIFACT_KEY_ATTRIBUTE = "key";
    static final String ARTIFACT = "artifact";
    static final String TEST_ARTIFACT = "test-artifact";
    static final String SUPPORTIVE_ARTIFACTS = "supportive-artifacts";
    static final String ARTIFACTS = "artifacts";

    //registry resources key word constants
    static final String REGISTRY_RESOURCES = "registry-resources";
    static final String REGISTRY_NAME = "file-name";
    static final String REGISTRY_PATH = "registry-path";
    static final String REGISTRY_MEDIA_TYPE = "media-type";
    static final String LOCAL_REGISTRY_TYPE = "local";
    static final String GOVERNANCE_REGISTRY_TYPE = "gov";
    static final String CONFIGURATION_REGISTRY_TYPE = "conf";

    //connector resource key word constants
    static final String CONNECTOR_RESOURCES = "connector-resources";
    static final String CONNECTOR_TEST_FOLDER = "test";

    //test case key word constants
    static final String TEST_CASES = "test-cases";
    static final String TEST_CASE_REQUEST_PATH = "request-path";
    static final String TEST_CASE_REQUEST_METHOD = "request-method";
    static final String TEST_CASE_INPUT = "input";
    static final String TEST_CASE_INPUT_PAYLOAD = "payload";
    static final String TEST_CASE_INPUT_PROPERTIES = "properties";
    static final String TEST_CASE_INPUT_PROPERTY_NAME = "name";
    static final String TEST_CASE_INPUT_PROPERTY_VALUE = "value";
    static final String TEST_CASE_INPUT_PROPERTY_SCOPE = "scope";
    static final String TEST_CASE_ASSERTIONS = "assertions";
    static final String TEST_CASE_ASSERTION_EQUALS = "assertEquals";
    static final String TEST_CASE_ASSERTION_NOTNULL = "assertNotNull";
    static final String ASSERTION_ACTUAL = "actual";
    static final String ASSERTION_EXPECTED = "expected";
    static final String ASSERTION_MESSAGE = "message";
    static final String INPUT_PROPERTY_SCOPE_DEFAULT = "default";
    static final String INPUT_PROPERTY_SCOPE_AXIS2 = "axis2";
    static final String INPUT_PROPERTY_SCOPE_TRANSPORT = "transport";
    static final String INPUT_PROPERTY_BODY = "$body";
    static final String INPUT_PROPERTY_CONTEXT = "$ctx";
    static final String INPUT_PROPERTY_AXIS2 = "$axis2";
    static final String INPUT_PROPERTY_TRANSPORT = "$trp";

    //mock service key word constants
    static final String MOCK_SERVICES = "mock-services";
    static final String SERVICE_NAME = "service-name";
    static final String SERVICE_HOST = "localhost";
    static final String SERVICE_PORT = "port";
    static final String SERVICE_CONTEXT = "context";
    static final String SERVICE_RESOURCES = "resources";
    static final String SERVICE_RESOURCE_SUBCONTEXT = "sub-context";
    static final String SERVICE_RESOURCE_METHOD = "method";
    static final String SERVICE_RESOURCE_REQUEST = "request";
    static final String SERVICE_RESOURCE_RESPONSE = "response";
    static final String SERVICE_RESOURCE_PAYLOAD = "payload";
    static final String SERVICE_RESOURCE_RESPONSE_CODE = "status-code";
    static final String SERVICE_RESOURCE_HEADERS = "headers";
    static final String SERVICE_RESOURCE_HEADER_NAME = "name";
    static final String SERVICE_RESOURCE_HEADER_VALUE = "value";
    static final String END_POINT = "endpoint";
    public static final String URI = "uri";
    static final String URI_TEMPLATE = "uri-template";
    public static final String HTTP = "http://";
    static final String GET_METHOD = "GET";
    static final String POST_METHOD = "POST";
    static final String PUT_METHOD = "PUT";
    static final String DELETE_METHOD = "DELETE";

    //api/proxy invoke constants
    static final String HTTP_LOCALHOST_URL = "http://localhost:";
    static final String HTTP_KEY = "http";
    static final String HTTPS_KEY = "https";
    static final String HTTPS_LOCALHOST_URL = "https://localhost:";
    static final String PROXY_INVOKE_PREFIX_URL = "/services/";

    //test summary report constants
    static final String PASSED_KEY = "PASSED";
    static final String FAILED_KEY = "FAILED";
    static final String SKIPPED_KEY = "SKIPPED";
    static final String DEPLOYMENT_STATUS = "deploymentStatus";
    static final String DEPLOYMENT_EXCEPTION = "deploymentException";
    static final String DEPLOYMENT_DESCRIPTION = "deploymentDescription";
    static final String TEST_CASE_NAME = "testCaseName";
    static final String MEDIATION_STATUS = "mediationStatus";
    static final String MEDIATION_EXCEPTION = "mediationException";
    static final String CURRENT_TESTCASE = "currentTestCase";
    static final String ASSERTION_EXCEPTION = "exception";
    static final String ASSERTION_STATUS = "assertionStatus";

    //parameter constants
    static final String PRAM_TEMP_DIR = "java.io.tmpdir";

    //common constants
    static final String NEW_LINE = "\n";
}
