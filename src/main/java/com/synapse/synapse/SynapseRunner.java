package com.synapse.synapse;

import com.synapse.adapters.mediation.MediationEngine;
import com.synapse.core.artifacts.ConfigContext;
import com.synapse.core.deployers.Deployer;
import com.synapse.core.ports.InboundMessageMediator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SynapseRunner {
    public static void run() throws IOException {
        System.out.println("Synapse is running...");

        ConfigContext configContext = ConfigContext.getInstance();

        Path resourcesPath = Path.of(Paths.get("").toAbsolutePath().toString()).resolve("artifacts");
        System.out.println("Resolved resources path: " + resourcesPath);

        Axis2Server.start();

        InboundMessageMediator mediator = new MediationEngine(configContext);
        Deployer deployer = new Deployer(configContext, resourcesPath,mediator);
        deployer.deploy();
    }
}
