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

import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.samples.framework.clients.MTOMSwASampleClient;
import org.apache.synapse.samples.framework.clients.StockQuoteSampleClient;
import org.apache.synapse.samples.framework.config.Axis2ClientConfiguration;
import org.apache.synapse.samples.framework.config.SampleConfigConstants;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This is the class from which all sample tests are derived. Loads and stores necessary
 * configuration information. Starts the mediation engine and backend server(s) before each test.
 * Shuts down running servers after a test is complete.
 */
public abstract class SynapseTestCase extends TestCase {

    protected final Log log = LogFactory.getLog(this.getClass());

    private int sampleId;
    private String sampleName;

    private SynapseProcessController synapseController;
    private List<ProcessController> backendServerControllers;
    private Axis2ClientConfiguration clientConfig;

    protected SynapseTestCase(int sampleId) {
        if (log.isDebugEnabled()) {
            log.debug("Creating SynapseTestCase for test " + sampleId);
        }
        this.sampleId = sampleId;
        System.setProperty("java.io.tmpdir", FilenameUtils.normalize(
                SynapseTestUtils.getCurrentDir() + "modules/integration/target/temp"));
        loadConfiguration();
    }

    private void loadConfiguration() {
        // Parse the sample descriptor
        OMElement sampleConfig = loadDescriptorInfoFile();
        if (sampleConfig == null) {
            fail("Failed to load the sample configuration for sample: " + sampleId);
        }

        // Verify sample ID
        OMElement sampleIdElt = sampleConfig.getFirstChildWithName(
                new QName(SampleConfigConstants.TAG_SAMPLE_ID));
        if (sampleIdElt == null || sampleIdElt.getText() == null || "".equals(sampleIdElt.getText())) {
            fail("Sample ID not specified in the descriptor");
        } else if (this.sampleId != Integer.parseInt(sampleIdElt.getText())) {
            fail("Sample ID in the descriptor does not match the current test case");
        }

        // Load sample name
        OMElement sampleNameElt = sampleConfig.getFirstChildWithName(
                new QName(SampleConfigConstants.TAG_SAMPLE_NAME));
        if (sampleNameElt == null || sampleNameElt.getText() == null || "".equals(sampleNameElt.getText())) {
            fail("Sample name not specified in the descriptor");
        } else {
            this.sampleName = sampleNameElt.getText();
        }

        // Load Synapse, backend server and client configurations
        synapseController = initSynapseConfigInfo(sampleConfig);
        backendServerControllers = initBackEndServersConfigInfo(sampleConfig);
        if (backendServerControllers == null) {
            fail("Failed to load backend server configurations for the sample " + sampleId);
        }
        clientConfig = initClientConfigInfo(sampleConfig);

        if (synapseController.isClusteringEnabled()) {
            assertTrue("Could not properly configure clustering", configureClustering());
        }
    }

    /**
     * Executed before this test case. That means, this will be executed before each test.
     * Loads all configuration info and starts the servers.
     */
    public void setUp() {
        // Print a short intro to the console, so the console output is more readable
        String title = "Sample " + sampleId + ": " + sampleName;
        String underline = "";
        for (int i = 0; i < title.length(); i++) {
            underline += "=";
        }
        System.out.println("\n\n" + title);
        System.out.println(underline);

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
        log.info("Sample " + sampleId + " is finished");
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

        //cleaning up temp dir
        try {
            FileUtils.cleanDirectory(new File(System.getProperty("java.io.tmpdir")));
        } catch (IOException e) {
            log.warn("Error while cleaning temp directory", e);
        }
    }

    /**
     * Reads the specific descriptor file for the particular sample
     * from resource directory
     *
     * @return true if the configuration was loaded successfully
     */
    private OMElement loadDescriptorInfoFile() {
        String sampleDescriptor = "/sample" + sampleId + ".xml";
        if (log.isDebugEnabled()) {
            log.debug("Reading sample descriptor file from " + sampleDescriptor);
        }

        try {
            InputStream in = this.getClass().getResourceAsStream(sampleDescriptor);
            if (in == null) {
                fail("Cannot read sample descriptor file");
            }
            OMXMLParserWrapper builder = OMXMLBuilderFactory.createOMBuilder(in);
            return builder.getDocumentElement();
        } catch (Exception e) {
            log.error("Error loading test descriptor", e);
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
        return new SynapseProcessController(sampleId, synapseConfig);
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

    private boolean configureClustering() {
        try {
            String ip = SynapseTestUtils.getIPAddress();
            if (ip == null || ip.length() == 0) {
                log.fatal("Could not detect an active IP address");
                return false;
            }
            log.info("Using the IP: " + ip);

            String synapseAxis2Xml = synapseController.getAxis2Xml();
            String axis2Config = FileUtils.readFileToString(new File(synapseAxis2Xml));
            String modifiedSynapseAxis2 = SynapseTestUtils.replace(axis2Config, "${replace.me}", ip);
            File tempSynapseAxis2 = File.createTempFile("axis2Syn-", "xml");
            tempSynapseAxis2.deleteOnExit();
            FileUtils.writeStringToFile(tempSynapseAxis2, modifiedSynapseAxis2);
            synapseController.setAxis2Xml(tempSynapseAxis2.getAbsolutePath());

            for (ProcessController controller : backendServerControllers) {
                if (controller instanceof Axis2BackEndServerController) {
                    Axis2BackEndServerController axis2Controller = (Axis2BackEndServerController) controller;
                    String beAxis2Xml = axis2Controller.getAxis2Xml();
                    String beAxis2Config = FileUtils.readFileToString(new File(beAxis2Xml));
                    String modifiedBEAxis2 = SynapseTestUtils.replace(beAxis2Config, "${replace.me}", ip);
                    File tempBEAxis2 = File.createTempFile("axis2BE-", "xml");
                    tempBEAxis2.deleteOnExit();
                    FileUtils.writeStringToFile(tempBEAxis2, modifiedBEAxis2);
                    axis2Controller.setAxis2Xml(tempBEAxis2.getAbsolutePath());
                }
            }
            return true;

        } catch (Exception e) {
            log.error("Error configuring clustering", e);
            return false;
        }
    }

    protected List<ProcessController> getBackendServerControllers() {
        return backendServerControllers;
    }

    public StockQuoteSampleClient getStockQuoteClient() {
        return new StockQuoteSampleClient(clientConfig);
    }

    public MTOMSwASampleClient getMTOMSwASampleClient() {
        return new MTOMSwASampleClient(clientConfig);
    }

    protected void assertResponseReceived(SampleClientResult result) {
        assertTrue("Client did not receive the expected response", result.responseReceived());
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
