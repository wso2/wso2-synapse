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
 * Test class for LocalEntryDeployer
 */
public class LocalEntryDeployerTest {
    /**
     * Testing the deployment of a local entry
     *
     * @throws Exception
     */
    @Test
    public void testDeploy() throws Exception {
        String inputXML = "<localEntry key=\"TestEntry\" xmlns=\"http://ws.apache.org/ns/synapse\">0.1</localEntry>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        LocalEntryDeployer localEntryDeployer = new LocalEntryDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        localEntryDeployer.init(cfgCtx);
        String response = localEntryDeployer.deploySynapseArtifact(inputElement, "sampleFile", null);

        Assert.assertEquals("Local Entry not deployed!", "TestEntry", response);
    }

    /**
     * Test updating a local entry
     *
     * @throws Exception
     */
    @Test
    public void testUpdate() throws Exception {
        String inputXML = "<localEntry key=\"TestEntry\" xmlns=\"http://ws.apache.org/ns/synapse\">0.1</localEntry>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        LocalEntryDeployer localEntryDeployer = new LocalEntryDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        localEntryDeployer.init(cfgCtx);
        localEntryDeployer.deploySynapseArtifact(inputElement, "sampleFile", null);

        String inputUpdateXML = "<localEntry key=\"TestEntryUpdated\" xmlns=\"http://ws.apache.org/ns/synapse\">0.1</localEntry>";

        OMElement updatedElement = AXIOMUtil.stringToOM(inputUpdateXML);

        String response = localEntryDeployer.updateSynapseArtifact(updatedElement, "sampleUpdateFile", "TestEntry", null);

        Assert.assertEquals("Local Entry not updated!", "TestEntryUpdated", response);
    }

    /**
     * Test undeploying a local entry
     *
     * @throws Exception
     */
    @Test
    public void testUndeploy() throws Exception {
        String inputXML = "<localEntry key=\"TestEntry\" xmlns=\"http://ws.apache.org/ns/synapse\">0.1</localEntry>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        LocalEntryDeployer localEntryDeployer = new LocalEntryDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        localEntryDeployer.init(cfgCtx);
        localEntryDeployer.deploySynapseArtifact(inputElement, "sampleFile", null);
        Assert.assertNotNull("Local Entry not deployed!", synapseConfiguration.getEntry("TestEntry"));

        localEntryDeployer.undeploySynapseArtifact("TestEntry");
        Assert.assertNull("Local Entry cannot be undeployed", synapseConfiguration.getEntry("TestEntry"));
    }
}