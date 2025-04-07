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
            System.err.println("Base path does not exist or is not a directory: " + basePath);
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
                System.out.println("No XML files found in: " + folder);
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
                    System.err.println("Error reading file: " + file.getAbsolutePath() + " - " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("XML error while processing file: " + file.getAbsolutePath() + " - " + e.getMessage());
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
        System.out.println("Inbound deployed: " + newInbound.getName());

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
            System.err.println("Error starting inbound endpoint: " + e.getMessage());
        }

    }

    public void deploySequences(String fileName, String xmlData) throws IOException, XMLStreamException {
        Position position = new Position();
        position.setFileName(fileName);

        SequenceDeployer sequenceDeployer = new SequenceDeployer();
        Sequence newSequence = sequenceDeployer.unmarshal(xmlData, position);
        ctx.addSequence(newSequence);
        System.out.println("Sequence deployed: " + newSequence.getName());
    }

    public void deployAPIs(String fileName, String xmlData) throws XMLStreamException {
        Position position = new Position();
        position.setFileName(fileName);

        APIDeployer apiDeployer = new APIDeployer();
        API newApi = apiDeployer.unmarshal(xmlData, position);
        ctx.addAPI(newApi);
        System.out.println("API deployed: " + newApi.getName());
    }
}

