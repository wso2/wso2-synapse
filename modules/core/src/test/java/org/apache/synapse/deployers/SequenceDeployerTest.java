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

import java.util.Properties;

/**
 * Test class for SequenceDeployer
 */
public class SequenceDeployerTest {
    /**
     * Testing the deployment of a sequence
     *
     * @throws Exception
     */
    @Test
    public void testDeploy() throws Exception {
        String inputXML = "<sequence name=\"TestSequence\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                + "             <log/>"
                + "         </sequence>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        SequenceDeployer sequenceDeployer = new SequenceDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        sequenceDeployer.init(cfgCtx);
        String response = sequenceDeployer.deploySynapseArtifact(inputElement, "sampleFile", new Properties());

        Assert.assertEquals("Sequence not deployed!", "TestSequence", response);
    }

    /**
     * Test updating a sequence
     *
     * @throws Exception
     */
    @Test
    public void testUpdate() throws Exception {
        String inputXML = "<sequence name=\"TestSequence\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                + "             <log/>"
                + "         </sequence>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        SequenceDeployer sequenceDeployer = new SequenceDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        sequenceDeployer.init(cfgCtx);
        sequenceDeployer.deploySynapseArtifact(inputElement, "sampleFile", new Properties());

        String inputUpdateXML = "<sequence name=\"TestSequenceUpdated\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                + "                  <log/>"
                + "              </sequence>";

        OMElement updatedElement = AXIOMUtil.stringToOM(inputUpdateXML);

        String response = sequenceDeployer.updateSynapseArtifact(updatedElement, "sampleUpdateFile", "TestSequence", new Properties());

        Assert.assertEquals("Sequence not updated!", "TestSequenceUpdated", response);
    }

    /**
     * Test undeploying a sequence
     *
     * @throws Exception
     */
    @Test
    public void testUndeploy() throws Exception {
        String inputXML = "<sequence name=\"TestSequence\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                + "             <log/>"
                + "         </sequence>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        SequenceDeployer sequenceDeployer = new SequenceDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        sequenceDeployer.init(cfgCtx);
        sequenceDeployer.deploySynapseArtifact(inputElement, "sampleFile", new Properties());
        Assert.assertNotNull("Sequence not deployed!", synapseConfiguration.getSequence("TestSequence"));

        sequenceDeployer.undeploySynapseArtifact("TestSequence");
        Assert.assertNull("Sequence cannot be undeployed", synapseConfiguration.getSequence("TestSequence"));
    }
}