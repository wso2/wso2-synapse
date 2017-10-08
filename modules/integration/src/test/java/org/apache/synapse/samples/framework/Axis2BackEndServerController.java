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
import org.apache.axis2.AxisFault;
import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.clustering.ClusteringConstants;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.engine.ListenerManager;
import org.apache.axis2.engine.Phase;
import org.apache.axis2.phaseresolver.PhaseMetadata;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.samples.framework.config.SampleConfigConstants;

import java.util.List;

/**
 * Responsible for programatically starting up and shutting down
 * an Axis2 server instance in order to run a sample test.
 */
public class Axis2BackEndServerController extends AbstractBackEndServerController {

    private static final Log log = LogFactory.getLog(Axis2BackEndServerController.class);

    private static final int UNDEFINED      = 1;
    private static final int STARTING_UP    = 2;
    private static final int SERVER_ACTIVE  = 3;
    private static final int STARTUP_FAILED = 4;
    private static final int SHUTTING_DOWN  = 5;

    private String repoPath;
    private String axis2Xml;
    private String httpPort;
    private String httpsPort;
    private boolean counterEnabled;

    private int serverState = UNDEFINED;

    private final Axis2Server axis2Server;
    private Exception processException;
    private MessageCounter counter;

    public Axis2BackEndServerController(OMElement element) {
        super(element);
        String currentDir = SynapseTestUtils.getCurrentDir();
        repoPath = SynapseTestUtils.getParameter(element,
                SampleConfigConstants.TAG_BE_SERVER_CONF_AXIS2_REPO,
                currentDir + SampleConfigConstants.DEFAULT_BE_SERVER_CONF_AXIS2_REPO);
        axis2Xml = SynapseTestUtils.getParameter(element,
                SampleConfigConstants.TAG_BE_SERVER_CONF_AXIS2_XML,
                currentDir + SampleConfigConstants.DEFAULT_BE_SERVER_CONF_AXIS2_XML);
        httpPort = SynapseTestUtils.getParameter(element,
                SampleConfigConstants.TAG_BE_SERVER_CONF_AXIS2_HTTP_PORT, null);
        httpsPort = SynapseTestUtils.getParameter(element,
                SampleConfigConstants.TAG_BE_SERVER_CONF_AXIS2_HTTPS_PORT, null);

        counterEnabled = Boolean.parseBoolean(SynapseTestUtils.getParameter(element,
                SampleConfigConstants.TAG_BE_SERVER_CONF_AXIS2_COUNTER_ENABLED, "false"));

        axis2Server = new Axis2Server();
    }

    public String getAxis2Xml() {
        return axis2Xml;
    }

    public void setAxis2Xml(String axis2Xml) {
        this.axis2Xml = axis2Xml;
    }

    public boolean startProcess() {
        processException = null;

        synchronized (axis2Server) {
            axis2Server.start();
            while (serverState <= STARTING_UP) {
                try {
                    axis2Server.wait(1000); // Label: 1 [Wait for Axis2Server.run() to notify]
                } catch (InterruptedException e) {
                    log.error("Axis2 server startup was interrupted", e);
                    return false;
                }
            }
        }

        if (serverState == STARTUP_FAILED) {
            log.error("Axis2 server failed to start", processException);
            return false;
        }
        return true;
    }

    public boolean stopProcess() {
        if (serverState == SERVER_ACTIVE) {
            synchronized (axis2Server) {
                serverState = SHUTTING_DOWN;
                axis2Server.notifyAll(); // Notify Label: 2

                while (serverState > UNDEFINED) {
                    try {
                        axis2Server.wait(1000); // Label: 3 [Wait for Axis2Server.run() to notify]
                    } catch (InterruptedException e) {
                        log.warn("Axis2 server shutdown was interrupted", e);
                        return false;
                    }
                }
            }
            counter = null;
        }
        return true;
    }

    public int getMessageCount(String service, String operation) {
        if (counter != null) {
            return counter.getCount(service, operation);
        }
        return -1;
    }

    class Axis2Server extends Thread {

        Axis2Server() {
            super("axis2-" + serverName);
        }

        public void run() {processException = null;
            log.info("Starting up Axis2 server: " + serverName);

            ListenerManager listenerManager;
            ConfigurationContext configContext;

            try {
                listenerManager = new ListenerManager();
                configContext = ConfigurationContextFactory.
                        createConfigurationContextFromFileSystem(repoPath, axis2Xml);
                configure(configContext);
                listenerManager.startSystem(configContext);
                serverState = SERVER_ACTIVE;

            } catch (Exception e) {
                processException = e;
                serverState = STARTUP_FAILED;
                // start up failed...nothing more to do here
                return;
            } finally {
                synchronized (this) {
                    this.notifyAll(); // Notify Label: 1
                }
            }

            synchronized (this) {
                while (serverState < SHUTTING_DOWN) {
                    //wait for the tests
                    try {
                        this.wait(1000); // Label: 2 [Wait for the stop() to notify]
                    } catch (InterruptedException e) {
                        log.error("Axis2 server interrupted", e);
                    }
                }
            }

            log.info("Shutting down Axis2 server...");
            try {
                listenerManager.stop();
                listenerManager.destroy();
                configContext.terminate();
            } catch (Exception e) {
                log.warn("Error while shutting down Axis2 server", e);
            } finally {
                synchronized (this) {
                    serverState = UNDEFINED;
                    this.notifyAll(); // Notify Label: 3
                }
            }
        }

        private void configure(ConfigurationContext configContext) throws AxisFault {
            // setting System.setProperty does not work since all servers run on same jvm
            configContext.setProperty("server_name", serverName);

            if (httpPort != null) {
                TransportInDescription httpTrsIn = configContext.getAxisConfiguration().
                        getTransportsIn().get("http");
                httpTrsIn.getParameter("port").setValue(httpPort);
            }

            if (httpsPort != null) {
                TransportInDescription httpsTrsIn = configContext.getAxisConfiguration().
                        getTransportsIn().get("https");
                httpsTrsIn.getParameter("port").setValue(httpsPort);
            }

            ClusteringAgent clusteringAgent =
                    configContext.getAxisConfiguration().getClusteringAgent();
            String avoidInit = ClusteringConstants.Parameters.AVOID_INITIATION;
            if (clusteringAgent != null && clusteringAgent.getParameter(avoidInit) != null &&
                    ((String) clusteringAgent.getParameter(avoidInit).getValue()).
                            equalsIgnoreCase("true")) {
                clusteringAgent.setConfigurationContext(configContext);
                clusteringAgent.init();
            }

            if (counterEnabled) {
                log.info("Engaging server side message counter");
                List<Phase> phases = configContext.getAxisConfiguration().getInFlowPhases();
                for (Phase phase : phases) {
                    if (PhaseMetadata.PHASE_DISPATCH.equals(phase.getName())) {
                        counter = new MessageCounter();
                        phase.addHandler(counter);
                        break;
                    }
                }
            }
        }
    }
}
