/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.deployers;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for PriorityExecutorDeployer
 */
public class PriorityExecutorDeployerTest {
    /**
     * Testing the deployment of an endpoint
     *
     * @throws Exception
     */
    @Test
    public void testDeploy() throws Exception {
        String inputXML = "<priority-executor name=\"TestExec\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                + "         <queues>"
                + "            <queue size=\"100\" priority=\"1\"/>"
                + "            <queue size=\"100\" priority=\"10\"/>"
                + "         </queues>"
                + "        </priority-executor>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        PriorityExecutorDeployer priorityExecutorDeployer = new PriorityExecutorDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        priorityExecutorDeployer.init(cfgCtx);
        String response = priorityExecutorDeployer.deploySynapseArtifact(inputElement, "sampleFile", null);

        Assert.assertEquals("Priority executor not deployed!", "TestExec", response);
    }

    /**
     * Test updating a priority executor
     *
     * @throws Exception
     */
    @Test
    public void testUpdate() throws Exception {
        String inputXML = "<priority-executor name=\"TestExec\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                + "         <queues>"
                + "            <queue size=\"100\" priority=\"1\"/>"
                + "            <queue size=\"100\" priority=\"10\"/>"
                + "         </queues>"
                + "        </priority-executor>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        PriorityExecutorDeployer priorityExecutorDeployer = new PriorityExecutorDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        priorityExecutorDeployer.init(cfgCtx);
        priorityExecutorDeployer.deploySynapseArtifact(inputElement, "sampleFile", null);

        String inputUpdateXML = "<priority-executor name=\"TestExecUpdated\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                + "         <queues>"
                + "            <queue size=\"100\" priority=\"1\"/>"
                + "            <queue size=\"100\" priority=\"10\"/>"
                + "         </queues>"
                + "        </priority-executor>";

        OMElement updatedElement = AXIOMUtil.stringToOM(inputUpdateXML);

        String response = priorityExecutorDeployer.updateSynapseArtifact(updatedElement, "sampleUpdateFile", "TestExec", null);

        Assert.assertEquals("Priority executor not updated!", "TestExecUpdated", response);
    }

    /**
     * Test undeploying a priority executor
     *
     * @throws Exception
     */
    @Test
    public void testUndeploy() throws Exception {
        String inputXML = "<priority-executor name=\"TestExec\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                + "         <queues>"
                + "            <queue size=\"100\" priority=\"1\"/>"
                + "            <queue size=\"100\" priority=\"10\"/>"
                + "         </queues>"
                + "        </priority-executor>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        PriorityExecutorDeployer priorityExecutorDeployer = new PriorityExecutorDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        priorityExecutorDeployer.init(cfgCtx);
        priorityExecutorDeployer.deploySynapseArtifact(inputElement, "sampleFile", null);
        Assert.assertNotNull("Priority executor not deployed!", synapseConfiguration.getPriorityExecutors().get("TestExec"));

        priorityExecutorDeployer.undeploySynapseArtifact("TestExec");
        Assert.assertNull("Priority executor cannot be undeployed", synapseConfiguration.getPriorityExecutors().get("TestExec"));
    }
}