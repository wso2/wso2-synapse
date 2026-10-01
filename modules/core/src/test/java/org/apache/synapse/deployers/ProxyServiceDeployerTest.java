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
import org.apache.synapse.AbstractExtendedSynapseHandler;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseHandler;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    /**
     * Test that updating a proxy service reports the previous version as undeployed and the new version as deployed
     * to the extended synapse handlers
     *
     * @throws Exception
     */
    @Test
    public void testUpdateExecutesExtendedSynapseHandler() throws Exception {
        RecordingHandler handler = new RecordingHandler(false, false);
        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        ProxyServiceDeployer proxyServiceDeployer = createDeployer(synapseConfiguration, handler);

        proxyServiceDeployer.deploySynapseArtifact(AXIOMUtil.stringToOM(getProxyXML("TestProxy")), "sampleFile",
                new Properties());
        proxyServiceDeployer.updateSynapseArtifact(AXIOMUtil.stringToOM(getProxyXML("TestProxyUpdated")),
                "sampleUpdateFile", "TestProxy", new Properties());

        Assert.assertEquals("Unexpected handler events!", Arrays.asList(
                "deploy:TestProxy:" + SynapseConstants.PROXY_SERVICE_TYPE,
                "undeploy:TestProxy:" + SynapseConstants.PROXY_SERVICE_TYPE,
                "deploy:TestProxyUpdated:" + SynapseConstants.PROXY_SERVICE_TYPE), handler.events);
    }

    /**
     * Test that a failing extended synapse handler does not fail the update of a proxy service
     *
     * @throws Exception
     */
    @Test
    public void testUpdateWithFailingExtendedSynapseHandler() throws Exception {
        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        ProxyServiceDeployer proxyServiceDeployer = createDeployer(synapseConfiguration, null);
        proxyServiceDeployer.deploySynapseArtifact(AXIOMUtil.stringToOM(getProxyXML("TestProxy")), "sampleFile",
                new Properties());

        RecordingHandler handler = new RecordingHandler(true, true);
        ((SynapseEnvironment) synapseConfiguration.getAxisConfiguration()
                .getParameter(SynapseConstants.SYNAPSE_ENV).getValue()).registerSynapseHandler(handler);
        String response = proxyServiceDeployer.updateSynapseArtifact(AXIOMUtil.stringToOM(getProxyXML("TestProxy")),
                "sampleFile", "TestProxy", new Properties());

        Assert.assertEquals("Proxy not updated!", "TestProxy", response);
        Assert.assertNotNull("Proxy not updated!", synapseConfiguration.getProxyService("TestProxy"));
    }

    /**
     * Test that a handler failing on the undeployment of the previous version still receives the deployment of the
     * updated proxy service
     *
     * @throws Exception
     */
    @Test
    public void testUpdateDeliversDeployWhenUndeployFails() throws Exception {
        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        ProxyServiceDeployer proxyServiceDeployer = createDeployer(synapseConfiguration, null);
        proxyServiceDeployer.deploySynapseArtifact(AXIOMUtil.stringToOM(getProxyXML("TestProxy")), "sampleFile",
                new Properties());

        RecordingHandler handler = new RecordingHandler(false, true);
        ((SynapseEnvironment) synapseConfiguration.getAxisConfiguration()
                .getParameter(SynapseConstants.SYNAPSE_ENV).getValue()).registerSynapseHandler(handler);
        proxyServiceDeployer.updateSynapseArtifact(AXIOMUtil.stringToOM(getProxyXML("TestProxyUpdated")),
                "sampleUpdateFile", "TestProxy", new Properties());

        Assert.assertEquals("Deployment event not delivered after a failed undeployment!", Arrays.asList(
                "deploy:TestProxyUpdated:" + SynapseConstants.PROXY_SERVICE_TYPE), handler.events);
    }

    private ProxyServiceDeployer createDeployer(SynapseConfiguration synapseConfiguration, SynapseHandler handler)
            throws Exception {
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        if (handler != null) {
            synapseEnvironment.registerSynapseHandler(handler);
        }
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment));
        axisConfiguration.addParameter(new Parameter(SynapseConstants.SYNAPSE_CONFIG, synapseConfiguration));
        cfgCtx.setAxisConfiguration(axisConfiguration);

        ProxyServiceDeployer proxyServiceDeployer = new ProxyServiceDeployer();
        proxyServiceDeployer.init(cfgCtx);
        return proxyServiceDeployer;
    }

    private String getProxyXML(String name) {
        return "<proxy xmlns=\"http://ws.apache.org/ns/synapse\" name=\"" + name + "\">"
                + "        <target>"
                + "            <endpoint>"
                + "                <address uri=\"http://localhost:9000/services/SimpleStockQuoteService\"/>"
                + "            </endpoint>"
                + "            <outSequence>"
                + "                <send/>"
                + "            </outSequence>"
                + "        </target>"
                + "    </proxy>";
    }

    /**
     * Extended synapse handler that records artifact events, or fails on the configured ones
     */
    private static class RecordingHandler extends AbstractExtendedSynapseHandler {

        private final List<String> events = new ArrayList<>();
        private final boolean failOnDeploy;
        private final boolean failOnUndeploy;

        RecordingHandler(boolean failOnDeploy, boolean failOnUndeploy) {
            this.failOnDeploy = failOnDeploy;
            this.failOnUndeploy = failOnUndeploy;
        }

        @Override
        public boolean handleArtifactDeployment(String artifactName, String artifactType, String startTime) {
            if (failOnDeploy) {
                throw new IllegalStateException("Handler failure");
            }
            events.add("deploy:" + artifactName + ":" + artifactType);
            return true;
        }

        @Override
        public boolean handleArtifactUnDeployment(String artifactName, String artifactType, String unDeployTime) {
            if (failOnUndeploy) {
                throw new IllegalStateException("Handler failure");
            }
            events.add("undeploy:" + artifactName + ":" + artifactType);
            return true;
        }

        @Override
        public boolean handleServerInit() {
            return true;
        }

        @Override
        public boolean handleServerShutDown() {
            return true;
        }

        @Override
        public boolean handleError(MessageContext synCtx) {
            return true;
        }

        @Override
        public boolean handleRequestInFlow(MessageContext synCtx) {
            return true;
        }

        @Override
        public boolean handleRequestOutFlow(MessageContext synCtx) {
            return true;
        }

        @Override
        public boolean handleResponseInFlow(MessageContext synCtx) {
            return true;
        }

        @Override
        public boolean handleResponseOutFlow(MessageContext synCtx) {
            return true;
        }
    }
}