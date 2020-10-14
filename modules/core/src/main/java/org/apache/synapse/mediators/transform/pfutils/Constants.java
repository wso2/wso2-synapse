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

package org.apache.synapse.mediators.transform.pfutils;

public class Constants {

    public final static int XML_PAYLOAD_TYPE = 0;
    public final static int JSON_PAYLOAD_TYPE = 1;
    public final static int TEXT_PAYLOAD_TYPE = 2;
    public final static int NOT_SUPPORTING_PAYLOAD_TYPE = -1;

    public final static String REGEX_TEMPLATE_TYPE = "REGEX";
    public final static String FREEMARKER_TEMPLATE_TYPE = "FREEMARKER";

    public final static String PAYLOAD_INJECTING_NAME = "payload";
    public final static String ARGS_INJECTING_NAME = "args";
    public final static String ARGS_INJECTING_PREFIX = "arg";
    public final static String CTX_PROPERTY_INJECTING_NAME = "ctx";
    public final static String AXIS2_PROPERTY_INJECTING_NAME = "axis2";
    public final static String TRANSPORT_PROPERTY_INJECTING_NAME = "trp";

    public static final String JSON_TYPE = "json";
    public static final String XML_TYPE = "xml";
    public static final String TEXT_TYPE = "text";

}
