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
 * Test class for TemplateDeployer
 */
public class TemplateDeployerTest {
    /**
     * Testing the deployment of an endpoint template
     *
     * @throws Exception
     */
    @Test
    public void testDeployForEndpoint() throws Exception {
        String inputXML = "<template name = \"TestTemplate\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                +"              <endpoint name = \"sampleEP\" >"
                +"                  <address uri=\"http://localhost:9000/services/SimpleStockQuoteService\" >" + "</address>"
                +"              </endpoint>"
                +"          </template>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        TemplateDeployer templateDeployer = new TemplateDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        templateDeployer.init(cfgCtx);
        String response = templateDeployer.deploySynapseArtifact(inputElement, "sampleFile", new Properties());

        Assert.assertEquals("Endpoint template not deployed!", "TestTemplate", response);
    }

    /**
     * Testing the deployment of a sequence template
     *
     * @throws Exception
     */
    @Test
    public void testDeployForSequence() throws Exception {
        String inputXML = "<template name = \"TestTemplate\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                +"              <sequence name=\"TestSequenceUpdated\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                + "                  <log/>"
                + "              </sequence>"
                +"          </template>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        TemplateDeployer templateDeployer = new TemplateDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        templateDeployer.init(cfgCtx);
        String response = templateDeployer.deploySynapseArtifact(inputElement, "sampleFile", new Properties());

        Assert.assertEquals("Sequence template not deployed!", "TestTemplate", response);
    }

    /**
     * Test updating an endpoint
     *
     * @throws Exception
     */
    @Test
    public void testUpdateForEndpoint() throws Exception {
        String inputXML = "<template name = \"TestTemplate\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                +"              <endpoint name = \"sampleEP\" >"
                +"                  <address uri=\"http://localhost:9000/services/SimpleStockQuoteService\" >" + "</address>"
                +"              </endpoint>"
                +"          </template>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        TemplateDeployer templateDeployer = new TemplateDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        templateDeployer.init(cfgCtx);
        templateDeployer.deploySynapseArtifact(inputElement, "sampleFile", null);

        String inputUpdateXML = "<template name = \"TestTemplateUpdated\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                +"              <endpoint name = \"sampleEP\" >"
                +"                  <address uri=\"http://localhost:9000/services/SimpleStockQuoteService\" >" + "</address>"
                +"              </endpoint>"
                +"          </template>";

        OMElement updatedElement = AXIOMUtil.stringToOM(inputUpdateXML);

        String response = templateDeployer.updateSynapseArtifact(updatedElement, "sampleUpdateFile", "TestTemplate", null);

        Assert.assertEquals("Endpoint template not updated!", "TestTemplateUpdated", response);
    }

    /**
     * Test updating an endpoint
     *
     * @throws Exception
     */
    @Test
    public void testUpdateForSequence() throws Exception {
        String inputXML = "<template name = \"TestTemplate\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                +"              <sequence name=\"TestSequenceUpdated\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                + "                  <log/>"
                + "              </sequence>"
                +"          </template>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        TemplateDeployer templateDeployer = new TemplateDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        templateDeployer.init(cfgCtx);
        templateDeployer.deploySynapseArtifact(inputElement, "sampleFile", null);

        String inputUpdateXML = "<template name = \"TestTemplateUpdated\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                +"              <sequence name=\"TestSequenceUpdated\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                + "                  <log/>"
                + "              </sequence>"
                +"          </template>";

        OMElement updatedElement = AXIOMUtil.stringToOM(inputUpdateXML);

        String response = templateDeployer.updateSynapseArtifact(updatedElement, "sampleUpdateFile", "TestTemplate", null);

        Assert.assertEquals("Sequence template not updated!", "TestTemplateUpdated", response);
    }

    /**
     * Test undeploying an endpoint
     *
     * @throws Exception
     */
    @Test
    public void testUndeploy() throws Exception {
        String inputXML = "<template name = \"TestTemplate\" xmlns=\"http://ws.apache.org/ns/synapse\">"
                +"              <endpoint name = \"sampleEP\" >"
                +"                  <address uri=\"http://localhost:9000/services/SimpleStockQuoteService\" >" + "</address>"
                +"              </endpoint>"
                +"          </template>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        TemplateDeployer templateDeployer = new TemplateDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        templateDeployer.init(cfgCtx);
        templateDeployer.deploySynapseArtifact(inputElement, "sampleFile", null);
        Assert.assertNotNull("Endpoint template not deployed!", synapseConfiguration.getEndpointTemplate("TestTemplate"));

        templateDeployer.undeploySynapseArtifact("TestTemplate");
        Assert.assertNull("Endpoint template cannot be undeployed", synapseConfiguration.getEndpointTemplate("TestTemplate"));
    }
}