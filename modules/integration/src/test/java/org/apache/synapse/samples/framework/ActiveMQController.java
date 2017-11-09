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

package org.apache.synapse.samples.framework;

import org.apache.activemq.broker.BrokerService;
import org.apache.axiom.om.OMElement;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.samples.framework.config.SampleConfigConstants;

/**
 * Responsible for starting up and shutting down
 * a JMS broker instance in order to run a sample test.
 */
public class ActiveMQController extends AbstractBackEndServerController {

    private static final Log log = LogFactory.getLog(ActiveMQController.class);

    private String providerURL;

    private BrokerService broker;

    public ActiveMQController(OMElement element) {
        super(element);
        providerURL = SynapseTestUtils.getParameter(element,
                SampleConfigConstants.TAG_BE_SERVER_CONF_JMS_PROVIDER_URL,
                SampleConfigConstants.DEFAULT_BE_SERVER_CONF_JMS_PROVIDER_URL);
    }

    public boolean startProcess() {
        try {
            //using embedded jms broker
            broker = new BrokerService();
            // configure the broker
            broker.setBrokerName("synapseSampleBroker");
            broker.addConnector(providerURL);
            broker.start();
            log.info("JMSServerController: Broker is Successfully started. continuing tests");
            return true;
        } catch (Exception e) {
            log.error("There was an error starting JMS broker: " + serverName, e);
            return false;
        }
    }

    public boolean stopProcess() {
        try {
            broker.stop();
            FileUtils.deleteQuietly(broker.getDataDirectoryFile());
            return true;
        } catch (Exception e) {
            log.error("Error while shutting down the broker", e);
            return false;
        }
    }

}