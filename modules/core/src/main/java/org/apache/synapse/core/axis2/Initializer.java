package org.apache.synapse.core.axis2;

import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.unittest.Agent;

public class Initializer extends Thread {

    public SynapseConfiguration synapseConfiguration;
    private static Initializer initializer = null;

    public static synchronized Initializer getInstance() {

        if (initializer == null) {
            initializer = new Initializer();
        }
        return initializer;
    }

    public void run() {

        Agent agent = Agent.getInstance();
        agent.initialize();
    }

    public SynapseConfiguration getSynapseConfiguration() {

        return this.synapseConfiguration;
    }

    public void setSynapseConfiguration(SynapseConfiguration synapseConfiguration) {

        this.synapseConfiguration = synapseConfiguration;

    }
}
