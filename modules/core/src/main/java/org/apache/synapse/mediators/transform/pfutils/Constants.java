/*
 *Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */


package org.apache.synapse.mediators.transform.pfutils;

public class Constants {

    private Constants() {

    }

    public static final int XML_PAYLOAD_TYPE = 0;
    public static final int JSON_PAYLOAD_TYPE = 1;
    public static final int TEXT_PAYLOAD_TYPE = 2;
    public static final int NOT_SUPPORTING_PAYLOAD_TYPE = -1;
    public static final String REGEX_TEMPLATE_TYPE = "DEFAULT";
    public static final String FREEMARKER_TEMPLATE_TYPE = "FREEMARKER";
    public static final String PAYLOAD_INJECTING_NAME = "payload";
    public static final String ARGS_INJECTING_NAME = "args";
    public static final String ARGS_INJECTING_PREFIX = "arg";
    public static final String CTX_PROPERTY_INJECTING_NAME = "ctx";
    public static final String AXIS2_PROPERTY_INJECTING_NAME = "axis2";
    public static final String TRANSPORT_PROPERTY_INJECTING_NAME = "trp";
    public static final String JSON_TYPE = "json";
    public static final String XML_TYPE = "xml";
    public static final String TEXT_TYPE = "text";

}
