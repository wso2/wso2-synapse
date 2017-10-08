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

import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ServerConfigurationInformation;
import org.apache.synapse.ServerManager;
import org.apache.synapse.samples.framework.config.SampleConfigConstants;

/**
 * Responsible for starting up and shutting down
 * a synapse server instance in order to run a sample test.
 */
public class SynapseProcessController implements ProcessController {

    private static final Log log = LogFactory.getLog(SynapseProcessController.class);

    private static final int UNDEFINED      = 1;
    private static final int STARTING_UP    = 2;
    private static final int SERVER_ACTIVE  = 3;
    private static final int STARTUP_FAILED = 4;
    private static final int SHUTTING_DOWN  = 5;

    private ServerConfigurationInformation information;
    private boolean clusteringEnabled;

    private int serverState = UNDEFINED;

    private final SynapseServer synapseServer;
    private Exception processException;

    public SynapseProcessController(int sampleId, OMElement element) {
        String synapseHome = SynapseTestUtils.getCurrentDir();
        String synapseXml = SynapseTestUtils.getRequiredParameter(element,
                SampleConfigConstants.TAG_SYNAPSE_CONF_XML);
        String repoPath = SynapseTestUtils.getParameter(element,
                SampleConfigConstants.TAG_SYNAPSE_CONF_AXIS2_REPO,
                SampleConfigConstants.DEFAULT_SYNAPSE_CONF_AXIS2_REPO);
        String axis2Xml = SynapseTestUtils.getParameter(element,
                SampleConfigConstants.TAG_SYNAPSE_CONF_AXIS2_XML,
                SampleConfigConstants.DEFAULT_SYNAPSE_CONF_AXIS2_XML);
        String serverName = "Synapse" + sampleId;

        clusteringEnabled = Boolean.parseBoolean(SynapseTestUtils.getParameter(element,
                SampleConfigConstants.TAG_ENABLE_CLUSTERING, "false"));

        information = new ServerConfigurationInformation();
        information.setSynapseHome(synapseHome);
        information.setSynapseXMLLocation(synapseXml);
        information.setServerName(serverName);
        information.setAxis2Xml(axis2Xml);
        information.setResolveRoot(repoPath);
        information.setAxis2RepoLocation(repoPath);

        synapseServer = new SynapseServer();
    }

    public boolean isClusteringEnabled() {
        return clusteringEnabled;
    }

    public String getAxis2Xml() {
        return information.getAxis2Xml();
    }

    public void setAxis2Xml(String path) {
        information.setAxis2Xml(path);
    }

    public boolean startProcess() {
        processException = null;

        synchronized (synapseServer) {
            synapseServer.start();
            while (serverState <= STARTING_UP) {
                try {
                    synapseServer.wait(1000);
                } catch (InterruptedException e) {
                    log.error("Synapse startup was interrupted", e);
                    return false;
                }
            }
        }

        if (serverState == STARTUP_FAILED) {
            log.error("Synapse failed to start", processException);
            return false;
        }
        return true;
    }

    public boolean stopProcess() {
        if (serverState == SERVER_ACTIVE) {
            synchronized (synapseServer) {
                serverState = SHUTTING_DOWN;
                synapseServer.notifyAll();

                while (serverState > UNDEFINED) {
                    try {
                        synapseServer.wait(1000);
                    } catch (InterruptedException e) {
                        log.warn("Synapse shutdown was interrupted", e);
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public String getServerName() {
        return information.getServerName();
    }

    private class SynapseServer extends Thread {

        SynapseServer() {
            super(information.getServerName().toLowerCase());
        }

        public void run() {
            log.info("Starting up Synapse...");

            ServerManager manager = new ServerManager();
            try {
                manager.init(information, null);
                manager.start();
                serverState = SERVER_ACTIVE;
            } catch (Exception e) {
                processException = e;
                serverState = STARTUP_FAILED;
                return;
            } finally {
                synchronized (this) {
                    this.notifyAll();
                }
            }

            synchronized (this) {
                while (serverState < SHUTTING_DOWN) {
                    //wait for the tests
                    try {
                        this.wait(1000);
                    } catch (InterruptedException e) {
                        log.error("Axis2 server interrupted", e);
                    }
                }
            }

            log.info("Shutting down Synapse...");
            try {
                manager.shutdown();
            } finally {
                synchronized (this) {
                    serverState = UNDEFINED;
                    this.notifyAll();
                }
            }
        }
    }

}