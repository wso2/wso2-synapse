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

package org.apache.synapse.samples.framework.config;

/**
 * Sample configuration constants
 */
public class SampleConfigConstants {

    public static final String ATTR_SERVER_ID = "id";

    //xml tag names
    public static final String TAG_SAMPLE_ID = "sampleID";
    public static final String TAG_SAMPLE_NAME = "sampleName";

    public static final String TAG_SYNAPSE_CONF = "synapseConfig";
    public static final String TAG_SYNAPSE_CONF_AXIS2_REPO = "axis2Repo";
    public static final String TAG_SYNAPSE_CONF_AXIS2_XML = "axis2Xml";
    public static final String TAG_SYNAPSE_CONF_XML = "synapseXml";

    public static final String TAG_BE_SERVER_CONF = "backEndServerConfig";
    public static final String TAG_BE_SERVER_CONF_AXIS2_SERVER = "axis2Server";
    public static final String TAG_BE_SERVER_CONF_JMS_BROKER = "jmsBroker";
    public static final String TAG_BE_SERVER_CONF_DERBY_SERVER = "derbyServer";
    public static final String TAG_BE_SERVER_CONF_ECHO_SERVER = "echoServer";
    public static final String TAG_BE_SERVER_CONF_QFIX_EXECUTOR = "fixExecutor";

    public static final String TAG_BE_SERVER_CONF_AXIS2_REPO = "axis2Repo";
    public static final String TAG_BE_SERVER_CONF_AXIS2_XML = "axis2Xml";
    public static final String TAG_BE_SERVER_CONF_AXIS2_HTTP_PORT = "httpPort";
    public static final String TAG_BE_SERVER_CONF_AXIS2_HTTPS_PORT = "httpsPort";
    public static final String TAG_BE_SERVER_CONF_AXIS2_COUNTER_ENABLED = "counterEnabled";

    public static final String TAG_BE_SERVER_CONF_ECHO_HTTP_PORT = "httpPort";
    public static final String TAG_BE_SERVER_CONF_ECHO_HEADERS = "echoHeaders";

    public static final String TAG_BE_SERVER_CONF_DERBY_PORT = "dbPort";

    public static final String TAG_BE_SERVER_CONF_JMS_PROVIDER_URL = "providerURL";
    public static final String TAG_BE_SERVER_CONF_JMS_INITIAL_NAMING_FACTORY = "initialNamingFactory";

    public static final String TAG_CLIENT_CONF = "clientConfig";
    public static final String TAG_CLIENT_CONF_REPO = "clientRepo";
    public static final String TAG_CLIENT_CONF_AXIS2_XML = "axis2Xml";
    public static final String TAG_CLIENT_CONF_FILENAME = "fileName";

    public static final String TAG_ENABLE_CLUSTERING = "enableClustering";

    //default values
    public static final String DEFAULT_SERVER_ID = "default";

    public static final String DEFAULT_SYNAPSE_CONF_AXIS2_XML =
            "modules/integration/target/test_repos/synapse/conf/axis2_def.xml";
    public static final String DEFAULT_SYNAPSE_CONF_AXIS2_REPO =
            "modules/integration/target/test_repos/synapse";

    public static final String DEFAULT_BE_SERVER_CONF_AXIS2_XML =
            "modules/integration/target/test_repos/axis2Server/conf/axis2_def.xml";
    public static final String DEFAULT_BE_SERVER_CONF_AXIS2_REPO =
            "modules/integration/target/test_repos/axis2Server";

    public static final String DEFAULT_BE_SERVER_CONF_DERBY_PORT = "1527";

    public static final String DEFAULT_BE_SERVER_CONF_JMS_PROVIDER_URL = "tcp://localhost:61616";
    public static final String DEFAULT_BE_SERVER_CONF_JMS_INITIAL_NAMING_FACTORY =
            "org.apache.activemq.jndi.ActiveMQInitialContextFactory";

    public static final String DEFAULT_CLIENT_CONF_REPO =
            "modules/integration/target/test_repos/axis2Client";
    public static final String DEFAULT_CLIENT_CONF_FILENAME =
            "./repository/samples/resources/asf-logo.gif";
    public static final String DEFAULT_CLIENT_CONF_AXIS2_XML =
            "modules/integration/target/test_repos/axis2Client/conf/axis2_def.xml";


}