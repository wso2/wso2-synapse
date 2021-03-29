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
 * Test class for APIDeployer
 */
public class APIDeployerTest {
    /**
     * Testing the deployment of an API
     *
     * @throws Exception
     */
    @Test
    public void testDeploy() throws Exception {
        String inputXML = "<api name=\"TestAPI\" context=\"/order\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                + "<resource url-mapping=\"/list\" inSequence=\"seq1\" outSequence=\"seq2\" xmlns=\"http://ws.apache.org/ns/synapse\"/>"
                + "</api>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        APIDeployer apiDeployer = new APIDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        apiDeployer.init(cfgCtx);
        String response = apiDeployer.deploySynapseArtifact(inputElement, "sampleFile", new Properties());
        Assert.assertEquals("API not deployed!", "TestAPI", response);
    }

    /**
     * Test updating an API
     *
     * @throws Exception
     */
    @Test
    public void testUpdate() throws Exception {
        String inputXML = "<api name=\"TestAPI\" context=\"/order\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                + "<resource url-mapping=\"/list\" inSequence=\"seq1\" outSequence=\"seq2\" xmlns=\"http://ws.apache.org/ns/synapse\"/>"
                + "</api>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        APIDeployer apiDeployer = new APIDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        apiDeployer.init(cfgCtx);
        apiDeployer.deploySynapseArtifact(inputElement, "sampleFile", new Properties());

        String inputUpdatedXML =
                "<api name=\"TestAPIUpdated\" context=\"/orderUpdated\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                        + "<resource url-mapping=\"/list\" inSequence=\"seq1\" outSequence=\"seq2\" xmlns=\"http://ws.apache.org/ns/synapse\"/>"
                        + "</api>";

        OMElement inputUpdatedElement = AXIOMUtil.stringToOM(inputUpdatedXML);
        String response = apiDeployer.updateSynapseArtifact(inputUpdatedElement, "sampleUpdatedFile", "TestAPI", new Properties());
        Assert.assertEquals("API not updated!", "TestAPIUpdated", response);
    }

    /**
     * Test undeploying an API
     *
     * @throws Exception
     */
    @Test
    public void testUndeploy() throws Exception {
        String inputXML = "<api name=\"TestAPI\" context=\"/order\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                + "<resource url-mapping=\"/list\" inSequence=\"seq1\" outSequence=\"seq2\" xmlns=\"http://ws.apache.org/ns/synapse\"/>"
                + "</api>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        APIDeployer apiDeployer = new APIDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        apiDeployer.init(cfgCtx);
        apiDeployer.deploySynapseArtifact(inputElement, "sampleFile", new Properties());
        Assert.assertNotNull("API not deployed!", synapseConfiguration.getAPI("TestAPI"));

        apiDeployer.undeploySynapseArtifact("TestAPI");
        Assert.assertNull("Api cannot be undeployed!", synapseConfiguration.getAPI("TestAPI"));
    }
}