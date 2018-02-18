/*
* Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.mediators;

import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.mediators.clients.AxisOperationClient;
import org.apache.synapse.samples.framework.Axis2BackEndServerController;
import org.apache.synapse.samples.framework.SynapseProcessController;
import org.apache.synapse.samples.framework.ProcessController;
import org.apache.synapse.samples.framework.SynapseTestUtils;
import org.apache.synapse.samples.framework.clients.StockQuoteSampleClient;
import org.apache.synapse.samples.framework.config.Axis2ClientConfiguration;
import org.apache.synapse.samples.framework.config.SampleConfigConstants;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class MediatorTestCase extends TestCase {

    protected final Log log = LogFactory.getLog(this.getClass());

    private SynapseProcessController synapseController;
    private List<ProcessController> backendServerControllers;
    private Axis2ClientConfiguration clientConfig;

    public void loadConfiguration(String configFileName) {
        // Parse the sample descriptor
        OMElement sampleConfig = loadDescriptorInfoFile(configFileName);
        if (sampleConfig == null) {
            fail("Failed to load the configuration: " + configFileName);
        }

        // Load Synapse, backend server and client configurations
        synapseController = initSynapseConfigInfo(sampleConfig);
        backendServerControllers = initBackEndServersConfigInfo(sampleConfig);
        if (backendServerControllers == null) {
            fail("Failed to load backend server configurations for the test " + this.getName());
        }
        clientConfig = initClientConfigInfo(sampleConfig);
    }

    /**
     * Executed before this test case. That means, this will be executed before each test.
     * Loads all configuration info and starts the servers.
     */
    public void setUp() {
        // Start backend servers
        for (ProcessController controller : backendServerControllers) {
            if (!controller.startProcess()) {
                doCleanup();
                fail("Error starting the server: " + controller.getServerName());
            }
        }

        // Start Synapse
        if (!synapseController.startProcess()) {
            doCleanup();
            fail("Error starting synapse server");
        }
    }

    /**
     * Executed after this test case. That means, This will be executed after each test
     */
    public void tearDown() {
        doCleanup();
    }

    /**
     * shutting down servers, cleaning temp files
     */
    private void doCleanup() {
        if (synapseController != null) {
            log.debug("Stopping Synapse");
            synapseController.stopProcess();
        }

        List<ProcessController> removed = new ArrayList<ProcessController>();
        for (ProcessController bsc : backendServerControllers) {
            if (bsc instanceof Axis2BackEndServerController) {
                log.info("Stopping Server: " + bsc.getServerName());
                bsc.stopProcess();
                removed.add(bsc);
            }
        }

        for (ProcessController bsc : removed) {
            backendServerControllers.remove(bsc);
        }

        for (ProcessController bsc : backendServerControllers) {
            log.info("Stopping Server: " + bsc.getServerName());
            bsc.stopProcess();
        }
    }

    /**
     * Reads the specific descriptor file for the particular sample
     * from resource directory
     *
     * @return true if the configuration was loaded successfully
     */
    private OMElement loadDescriptorInfoFile(String configFileName) {
        String sampleDescriptor = configFileName;
        if (log.isDebugEnabled()) {
            log.debug("Reading configuration from " + sampleDescriptor);
        }

        try {
            InputStream in = this.getClass().getResourceAsStream(sampleDescriptor);
            if (in == null) {
                fail("Cannot read configuration file");
            }
            OMXMLParserWrapper builder = OMXMLBuilderFactory.createOMBuilder(in);
            return builder.getDocumentElement();
        } catch (Exception e) {
            log.error("Error loading test configuration", e);
            return null;
        }
    }

    /**
     * Reads and initializes Synapse specific configuration information from descriptor
     *
     * @param config Sample descriptor
     * @return A SynapseProcessController instance
     */
    private SynapseProcessController initSynapseConfigInfo(OMElement config) {
        OMElement synapseConfig = config.getFirstChildWithName(
                new QName(SampleConfigConstants.TAG_SYNAPSE_CONF));
        return new SynapseProcessController(0, synapseConfig);
    }

    /**
     * Reads and initializes backend server specific configuration information from descriptor
     *
     * @param config Sample descriptor
     * @return a List of ProcessController instances
     */
    private List<ProcessController> initBackEndServersConfigInfo(OMElement config) {
        log.debug("Initializing configuration information for backend servers...");
        List<ProcessController> controllers = new ArrayList<ProcessController>();
        OMElement backendServersConfig = config.getFirstChildWithName(
                new QName(SampleConfigConstants.TAG_BE_SERVER_CONF));
        if (backendServersConfig == null) {
            log.warn("No backend servers defined");
            return null;
        }

        Iterator backendServers = backendServersConfig.getChildElements();
        while (backendServers.hasNext()) {
            OMElement backendServer = (OMElement) backendServers.next();
            ProcessController controller = SynapseTestUtils.createController(backendServer);
            if (controller != null) {
                controllers.add(controller);
            } else {
                log.error("Unrecognized backend server configuration: " + backendServer.getLocalName());
                return null;
            }
        }

        return controllers;
    }

    /**
     * Reads and stores client specific configuration information from descriptor
     *
     * @param config Sample descriptor
     * @return An Axis2ClientConfiguration instance
     */
    private Axis2ClientConfiguration initClientConfigInfo(OMElement config) {
        Axis2ClientConfiguration clientConfig = new Axis2ClientConfiguration();

        String currentDir = SynapseTestUtils.getCurrentDir();
        clientConfig.setAxis2Xml(SynapseTestUtils.getParameter(config,
                SampleConfigConstants.TAG_CLIENT_CONF_AXIS2_XML,
                FilenameUtils.normalize(currentDir + SampleConfigConstants.DEFAULT_CLIENT_CONF_AXIS2_XML)));
        clientConfig.setClientRepo(SynapseTestUtils.getParameter(config,
                SampleConfigConstants.TAG_CLIENT_CONF_REPO,
                FilenameUtils.normalize(currentDir + SampleConfigConstants.DEFAULT_CLIENT_CONF_REPO)));
        return clientConfig;
    }

    protected List<ProcessController> getBackendServerControllers() {
        return backendServerControllers;
    }

    public StockQuoteSampleClient getStockQuoteClient() {
        return new StockQuoteSampleClient(clientConfig);
    }

    public AxisOperationClient getAxisOperationClient() {
        return new AxisOperationClient(clientConfig);
    }

    protected Axis2BackEndServerController getAxis2Server() {
        for (ProcessController server : backendServerControllers) {
            if (server instanceof Axis2BackEndServerController) {
                return (Axis2BackEndServerController) server;
            }
        }
        return null;
    }
}
