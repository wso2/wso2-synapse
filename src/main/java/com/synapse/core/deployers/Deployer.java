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


package com.synapse.core.deployers;

import com.synapse.adapters.inbound.InboundFactory;
import com.synapse.core.artifacts.ConfigContext;
import com.synapse.core.artifacts.Sequence;
import com.synapse.core.artifacts.api.API;
import com.synapse.core.artifacts.inbound.Inbound;
import com.synapse.core.artifacts.inbound.Parameter;
import com.synapse.core.artifacts.utils.Position;
import com.synapse.core.domain.InboundConfig;
import com.synapse.core.ports.InboundEndpoint;
import com.synapse.core.ports.InboundMessageMediator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Deployer {

    private static final Logger log = LogManager.getLogger(Deployer.class);

    private final ConfigContext ctx;
    private final Path basePath;
    private final InboundMessageMediator inboundMediator;

    public Deployer(ConfigContext ctx, Path basePath, InboundMessageMediator inboundMediator) {
        this.ctx = ctx;
        this.basePath = basePath;
        this.inboundMediator = inboundMediator;
    }

    public void deploy() throws IOException {

        File baseDir = basePath.toFile();
        if (!baseDir.exists() || !baseDir.isDirectory()) {
            log.error("Base path does not exist or is not a directory: {}", basePath);
            return;
        }

        List<String> artifactTypes = List.of("Sequences", "APIs", "Inbounds");

        for (String artifactType : artifactTypes) {

            File folder = new File(baseDir, artifactType);
//            if (!folder.exists() || !folder.isDirectory()) {
//                System.err.println("Directory does not exist or is not a directory: " + folder);
//                continue;
//            }

            File[] files = folder.listFiles((dir, name) -> name.endsWith(".xml"));
            if (files == null || files.length == 0) {
                log.info("No XML files found in: {}", folder);
                continue;
            }

            for (File file : files) {
                try {
                    String xmlData = Files.readString(file.toPath());
                    switch (artifactType) {
                        case "APIs":
                            deployAPIs(file.getName(), xmlData);
                            break;
                        case "Sequences":
                            deploySequences(file.getName(), xmlData);
                            break;
                        case "Inbounds":
                            deployInbounds(file.getName(), xmlData);
                            break;
                    }
                } catch (IOException e) {
                    log.error("Error reading file: {} - {}", file.getAbsolutePath(), e.getMessage());
                } catch (Exception e) {
                    log.error("XML error while processing file: {} - {}", file.getAbsolutePath(), e.getMessage());
                }
            }
        }
    }

    private void deployInbounds(String fileName, String xmlData) throws Exception {
        Position position = new Position();
        position.setFileName(fileName);

        InboundDeployer inboundDeployer = new InboundDeployer();
        Inbound newInbound = inboundDeployer.unmarshal(xmlData, position);
        ctx.addInbound(newInbound);
        log.info("Inbound deployed: {}", newInbound.getName());

        Map<String, String> parametersMap = new HashMap<>();
            for (Parameter param : newInbound.getParameters()) {
                parametersMap.put(param.getName(), param.getValue());
            }

            InboundConfig inboundConfig = new InboundConfig(
                    newInbound.getName(),
                    newInbound.getProtocol(),
                    parametersMap,
                    newInbound.getSequence(),
                    newInbound.getOnError()
            );

        InboundEndpoint inboundEndpoint = InboundFactory.newInbound(inboundConfig);
//        inboundEndpoint.start(inboundMediator);

//        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
//        executor.submit(() -> {
//            try {
//                    inboundEndpoint.start(inboundMediator);
//            } catch (Exception e) {
//                System.err.println("Error starting inbound endpoint: " + e.getMessage());
//            }
//        });

//        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
//            executor.submit(() -> {
//                try {
//                    inboundEndpoint.start(inboundMediator);
//                } catch (Exception e) {
//                    System.err.println("Error starting inbound endpoint: " + e.getMessage());
//                }
//            });
//        }
        try {
            inboundEndpoint.start(inboundMediator);
        } catch (Exception e) {
            log.error("Error starting inbound endpoint: {}", e.getMessage());
        }

    }

    public void deploySequences(String fileName, String xmlData) throws IOException, XMLStreamException {
        Position position = new Position();
        position.setFileName(fileName);

        SequenceDeployer sequenceDeployer = new SequenceDeployer();
        Sequence newSequence = sequenceDeployer.unmarshal(xmlData, position);
        ctx.addSequence(newSequence);
        log.info("Sequence deployed: {}", newSequence.getName());
    }

    public void deployAPIs(String fileName, String xmlData) throws XMLStreamException {
        Position position = new Position();
        position.setFileName(fileName);

        APIDeployer apiDeployer = new APIDeployer();
        API newApi = apiDeployer.unmarshal(xmlData, position);
        ctx.addAPI(newApi);
        log.info("API deployed: {}", newApi.getName());
    }
}
