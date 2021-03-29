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
 * Test class for ProxyServiceDeployer
 */
public class ProxyServiceDeployerTest {
    /**
     * Testing the deployment of a proxy service
     *
     * @throws Exception
     */
    @Test
    public void testDeploy() throws Exception {
        String inputXML = "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"TestProxy\">"
                + "        <target>"
                + "            <endpoint>"
                + "                <address uri=\"http://localhost:9000/services/SimpleStockQuoteService\"/>"
                + "            </endpoint>"
                + "            <outSequence>"
                + "                <send/>"
                + "            </outSequence>"
                + "        </target>"
                + "    </proxy>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        ProxyServiceDeployer proxyServiceDeployer = new ProxyServiceDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        proxyServiceDeployer.init(cfgCtx);
        String response = proxyServiceDeployer.deploySynapseArtifact(inputElement, "sampleFile", new Properties());
        Assert.assertEquals("Proxy service not deployed!", "TestProxy", response);
    }

    /**
     * Test updating a proxy service
     *
     * @throws Exception
     */
    @Test
    public void testUpdate() throws Exception {
        String inputXML = "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"TestProxy\">"
                + "        <target>"
                + "            <endpoint>"
                + "                <address uri=\"http://localhost:9000/services/SimpleStockQuoteService\"/>"
                + "            </endpoint>"
                + "            <outSequence>"
                + "                <send/>"
                + "            </outSequence>"
                + "        </target>"
                + "    </proxy>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        ProxyServiceDeployer proxyServiceDeployer = new ProxyServiceDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        proxyServiceDeployer.init(cfgCtx);
        proxyServiceDeployer.deploySynapseArtifact(inputElement, "sampleFile", new Properties());

        String inputUpdateXML = "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"TestProxyUpdated\">"
                + "        <target>"
                + "            <endpoint>"
                + "                <address uri=\"http://localhost:9000/services/SimpleStockQuoteService\"/>"
                + "            </endpoint>"
                + "            <outSequence>"
                + "                <send/>"
                + "            </outSequence>"
                + "        </target>"
                + "    </proxy>";

        OMElement updatedElement = AXIOMUtil.stringToOM(inputUpdateXML);

        String response = proxyServiceDeployer.updateSynapseArtifact(updatedElement, "sampleUpdateFile", "TestProxy", new Properties());

        Assert.assertEquals("Proxy not updated!", "TestProxyUpdated", response);
    }

    /**
     * Test undeploying a proxy service
     *
     * @throws Exception
     */
    @Test
    public void testUndeploy() throws Exception {
        String inputXML = "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"TestProxy\">"
                + "        <target>"
                + "            <endpoint>"
                + "                <address uri=\"http://localhost:9000/services/SimpleStockQuoteService\"/>"
                + "            </endpoint>"
                + "            <outSequence>"
                + "                <send/>"
                + "            </outSequence>"
                + "        </target>"
                + "    </proxy>";

        OMElement inputElement = AXIOMUtil.stringToOM(inputXML);
        ProxyServiceDeployer proxyServiceDeployer = new ProxyServiceDeployer();

        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        proxyServiceDeployer.init(cfgCtx);
        proxyServiceDeployer.deploySynapseArtifact(inputElement, "sampleFile", new Properties());
        Assert.assertNotNull("Proxy not deployed!", synapseConfiguration.getProxyService("TestProxy"));

        proxyServiceDeployer.undeploySynapseArtifact("TestProxy");
        Assert.assertNull("Proxy service cannot be undeployed", synapseConfiguration.getProxyService("TestProxy"));
    }
}