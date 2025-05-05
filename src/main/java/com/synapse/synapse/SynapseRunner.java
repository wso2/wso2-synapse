/*
 * Copyright (c) 2025, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.synapse.synapse;

import com.synapse.adapters.mediation.MediationEngine;
import com.synapse.core.artifacts.ConfigContext;
import com.synapse.core.artifacts.inbound.Inbound;
import com.synapse.core.deployers.Deployer;
import com.synapse.core.ports.InboundEndpoint;
import com.synapse.core.ports.InboundMessageMediator;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class SynapseRunner {

    public static final Log log = LogFactory.getLog(SynapseRunner.class);
    private static final ConfigContext configContext = ConfigContext.getInstance();

    public static void run() throws IOException {
        log.info("Synapse is running");

        Path resourcesPath = Path.of(Paths.get("").toAbsolutePath().toString()).resolve("artifacts");
        log.info("Resolved resources path: " + resourcesPath);

//        Axis2Server.start();

        InboundMessageMediator mediator = new MediationEngine(configContext);
        Deployer deployer = new Deployer(configContext, resourcesPath,mediator);
        deployer.deploy();
    }

    public static void stop() {
        try {
            Map<String, Inbound> inboundMap = configContext.getInboundMap();

            for (Map.Entry<String, Inbound> entry : inboundMap.entrySet()) {
                String inboundName = entry.getKey();
                InboundEndpoint inbound = entry.getValue().getInboundEndpoint();

                try {
                    inbound.stop();
                    log.info("Successfully stopped" + inboundName + "inbound endpoint");
                } catch (Exception e) {
                    log.error("Error stopping " + inboundName + " inbound endpoint");
                }
            }

            log.info("All inbound endpoints have been stopped successfully.");
        } catch (Exception e) {
            log.error("Error during shutdown: ", e);
        } finally {
            log.info("Shutdown process completed.");
        }
    }
}
