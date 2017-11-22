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
 * Test class for EndpointDeployer
 */
public class EndpointDeployerTest {
    /**
     * Testing the deployment of an endpoint
     *
     * @throws Exception
     */
    @Test
    public void testDeploy() throws Exception {
        String inputXML = "<endpoint name = \"sampleEP\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                + "<address uri=\"http://localhost:9000/services/SimpleStockQuoteService\" >" + "</address>"
                + "</endpoint>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        EndpointDeployer endpointDeployer = new EndpointDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        endpointDeployer.init(cfgCtx);
        String response = endpointDeployer.deploySynapseArtifact(inputElement, "sampleFile", null);

        Assert.assertEquals("Endpoint not deployed!", "sampleEP", response);
    }

    /**
     * Test updating an endpoint
     *
     * @throws Exception
     */
    @Test
    public void testUpdate() throws Exception {
        String inputXML = "<endpoint name = \"sampleEP\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                + "<address uri=\"http://localhost:9000/services/SimpleStockQuoteService\" >" + "</address>"
                + "</endpoint>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        EndpointDeployer endpointDeployer = new EndpointDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        endpointDeployer.init(cfgCtx);
        endpointDeployer.deploySynapseArtifact(inputElement, "sampleFile", null);

        String inputUpdateXML = "<endpoint name = \"sampleUpdatedEP\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                + "<address uri=\"http://localhost:9000/services/SimpleStockQuoteService\" >" + "</address>"
                + "</endpoint>";

        OMElement updatedElement = AXIOMUtil.stringToOM(inputUpdateXML);

        String response = endpointDeployer.updateSynapseArtifact(updatedElement, "sampleUpdateFile", "sampleEP", null);

        Assert.assertEquals("Endpoint not updated!", "sampleUpdatedEP", response);
    }

    /**
     * Test undeploying an endpoint
     *
     * @throws Exception
     */
    @Test
    public void testUndeploy() throws Exception {
        String inputXML = "<endpoint name = \"sampleEP\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                + "<address uri=\"http://localhost:9000/services/SimpleStockQuoteService\" >" + "</address>"
                + "</endpoint>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        EndpointDeployer endpointDeployer = new EndpointDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        endpointDeployer.init(cfgCtx);
        endpointDeployer.deploySynapseArtifact(inputElement, "sampleFile", null);
        Assert.assertNotNull("Endpoint not deployed!", synapseConfiguration.getEndpoint("sampleEP"));

        endpointDeployer.undeploySynapseArtifact("sampleEP");
        Assert.assertNull("Endpoint cannot be undeployed", synapseConfiguration.getEndpoint("sampleEP"));
    }
}