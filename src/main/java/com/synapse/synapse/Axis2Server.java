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

import com.synapse.core.artifacts.api.API;
import com.synapse.core.artifacts.api.APIDeploymentService;
import com.synapse.core.ports.InboundMessageMediator;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.SimpleHTTPServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Paths;

public class Axis2Server {

    private static final Logger logger = LogManager.getLogger(Axis2Server.class);

    private static final int PORT = 8290;
    private static SimpleHTTPServer server;
    private static APIDeploymentService apiDeploymentService;

    public static void start(InboundMessageMediator mediator) {
        try {
            logger.info("Starting Axis2 HTTP Server on port " + PORT);

            String repoPath = Paths.get("").toAbsolutePath().resolve("repository").toString();
            String axis2Xml = repoPath + "/conf/axis2.xml";

            ConfigurationContext configContext = ConfigurationContextFactory
                    .createConfigurationContextFromFileSystem(repoPath, axis2Xml);

            server = new SimpleHTTPServer(configContext, PORT);
            
            // Initialize API deployment service
            apiDeploymentService = new APIDeploymentService(configContext, mediator);
            
            server.start();
            
            // Deploy all APIs
            apiDeploymentService.deployAPIs();

            logger.info("Axis2 HTTP Server started on port " + PORT);
        } catch (Exception e) {
            logger.error("Failed to start Axis2 server", e);
        }
    }

    public static void deployAPI(API api) {
        if (apiDeploymentService != null) {
            try {
                apiDeploymentService.deployAPI(api);
                logger.info("Deployed API: {}", api.getName());
            } catch (Exception e) {
                logger.error("Failed to deploy API: {}", api.getName(), e);
            }
        } 
        else {
            logger.error("Cannot deploy API: API deployment service not initialized");
        }
    }

    public static void undeployAPI(String apiName) {
        
        if (apiDeploymentService != null) {
            apiDeploymentService.undeployAPI(apiName);
        }
    }

    public static void stop() {
        if (server != null) {
            server.stop();
//            System.out.println("Axis2 HTTP Server stopped.");
            logger.info("Axis2 HTTP Server stopped.");

        }
    }
}
