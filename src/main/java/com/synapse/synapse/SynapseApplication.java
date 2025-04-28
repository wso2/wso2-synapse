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

import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SynapseApplication {

    private static final Logger logger = LogManager.getLogger(SynapseApplication.class);

    public static void main(String[] args) throws IOException {

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down SynapeApplication...");
//            SynapseRunner.stop();
            logger.info("Shutdown complete");
        }));

        logger.info("SynapseApplication started");
        SynapseRunner.run();
    }
}
