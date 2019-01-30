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
package org.wso2.SynapseUnitTestClient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.lang.System;

public class SynapseServer {

    /**
     * This is the agent class to startup a synapse server instance
     */
    private static final String DEFAULT_SYNAPSE_HOME_LOCATION = ".";
    public static final String INTEGRATION_SYNAPSE_XML = "integration-synapse.xml";

    private static final Log log = LogFactory.getLog(SynapseServer.class);

    private Process process;

    @GET
    @Path("/start")
    public synchronized void startServer() {

        if (process == null) {
            try {
                String synapseHomeLocation = getSynapseHome();

                File synapseHome = Paths.get(synapseHomeLocation).toFile();

                String[] cmdArray;
                // For Windows
                if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                    cmdArray = new String[]{"cmd.exe", "/c", synapseHomeLocation + File.separator + "bin" + File.separator +
                            "synapse.bat", "-synapseConfig", synapseHomeLocation + File.separator + "repository"
                            + File.separator + "conf" + File.separator + INTEGRATION_SYNAPSE_XML};
                } else {
                    // For Unix
                    cmdArray = new String[]{"sh", synapseHomeLocation + File.separator + "bin" + File.separator +
                            "synapse.sh", "-synapseConfig", synapseHomeLocation + File.separator + "repository"
                            + File.separator + "conf" + File.separator + INTEGRATION_SYNAPSE_XML};
                }

                process = Runtime.getRuntime().exec(cmdArray, null, synapseHome);

            } catch (Exception ex) {
                log.error("Error while starting synapse server", ex);
            }
        }

    }

    private String getSynapseHome() {

        return System.getProperty("synapse.home", DEFAULT_SYNAPSE_HOME_LOCATION);
    }

    @GET
    @Path("/stop")
    public synchronized void stopServer() {

        if (process != null) {
            try {
                String synapseKillCommand = getSynapseHome() + File.separator + "bin" + File.separator + "synapse-stop.sh";
                Runtime.getRuntime().exec(synapseKillCommand);
            } catch (IOException e) {
                log.error("Error while stopping synapse server", e);
            }
            process = null;
        }
    }

}
