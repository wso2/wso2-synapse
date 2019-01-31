package org.apache.synapse.core.axis2;

import org.apache.synapse.unittest.Agent;

public class Initializer extends Thread {
    public void run(){
        Agent agent = Agent.getInstance();
        agent.initialize();
    }

}
