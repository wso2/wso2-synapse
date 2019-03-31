/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.core.axis2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;

import junit.framework.TestCase;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.MessageContext;
import org.apache.synapse.ServerConfigurationInformation;
import org.apache.synapse.ServerContextInformation;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.TestMessageContext;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.endpoints.AddressEndpoint;
import org.apache.synapse.endpoints.EndpointDefinition;
import org.apache.synapse.mediators.MediatorFaultHandler;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.util.resolver.ResourceMap;
import org.junit.Assert;
import org.mockito.Mockito;
import org.xml.sax.InputSource;

public class ProxyServiceTest extends TestCase {
    /**
     * Test that a proxy service without publishWSDL will produce a meaningful WSDL.
     * This is a regression test for SYNAPSE-366.
     */
    public void testWSDLWithoutPublishWSDL() throws Exception {
        // Build the proxy service
        SynapseConfiguration synCfg = new SynapseConfiguration();
        AxisConfiguration axisCfg = new AxisConfiguration();
        ProxyService proxyService = new ProxyService("Test");
        AxisService axisService = proxyService.buildAxisService(synCfg, axisCfg);
        // Serialize the WSDL
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        axisService.printWSDL(baos);
        // Check that the produced WSDL can be read by WSDL4J
        WSDLReader wsdlReader = WSDLFactory.newInstance().newWSDLReader();
        wsdlReader.readWSDL(null, new InputSource(new ByteArrayInputStream(baos.toByteArray())));
    }
    
    /**
     * Test a proxy with a WSDL importing another WSDL importing an XSD. The imported WSDL and
     * XSD documents are resolved using a {@link ResourceMap} (i.e. &lt;resource> elements). The
     * test checks that the proxy service can be built and produce a WSDL.
     * This is a test for the feature introduced by SYNAPSE-200 and a regression test
     * for SYNAPSE-362.
     */
    public void testWSDLWithPublishWSDLAndRecursiveImports() throws Exception {
        SynapseConfiguration synCfg = new SynapseConfiguration();
        AxisConfiguration axisCfg = new AxisConfiguration();
        // Add local entries
        Entry entry = new Entry();
        entry.setType(Entry.URL_SRC);
        entry.setSrc(getClass().getResource("root.wsdl"));
        synCfg.addEntry("root_wsdl", entry);
        entry = new Entry();
        entry.setType(Entry.URL_SRC);
        entry.setSrc(getClass().getResource("imported.xsd"));
        synCfg.addEntry("imported_xsd", entry);
        entry = new Entry();
        entry.setType(Entry.URL_SRC);
        entry.setSrc(getClass().getResource("imported.wsdl"));
        synCfg.addEntry("imported_wsdl", entry);
        // Build the proxy service
        ProxyService proxyService = new ProxyService("Test");
        proxyService.setWSDLKey("root_wsdl");
        ResourceMap resourceMap = new ResourceMap();
        resourceMap.addResource("imported.wsdl", "imported_wsdl");
        resourceMap.addResource("imported.xsd", "imported_xsd");
        proxyService.setResourceMap(resourceMap);
        AxisService axisService = proxyService.buildAxisService(synCfg, axisCfg);
        // Serialize the WSDL. Note that we can't parse the WSDL because it will have imports
        // referring to locations such as "my-matches?xsd=xsd0.xsd".
        axisService.printWSDL(new ByteArrayOutputStream());
    }
    
    /**
     * Test a proxy service with recursive imports and without a {@link ResourceMap}.
     * Regression test for SYNAPSE-442.
     */
    public void testRecursiveImports2() throws Exception {
        ProxyService testService = new ProxyService("mytest");
        SynapseConfiguration synCfg = new SynapseConfiguration();
        AxisConfiguration axisCfg = new AxisConfiguration();
        testService.setWsdlURI(getClass().getResource("SimpleStockService.wsdl").toURI());
        testService.buildAxisService(synCfg, axisCfg);
    }

    /**
     * Configures pinned servers for the proxy server and asserts if the proxy gets built correctly with and without
     * correct pinned servers.
     *
     * @throws Exception is an error occurs while adding the synapse environment to the axis configuration
     */
    public void testBuildAxisServiceWithPinnedServers() throws Exception {
        String pinnedServer = "localhost";
        SynapseConfiguration synCfg = new SynapseConfiguration();
        AxisConfiguration axisCfg = new AxisConfiguration();
        ServerConfigurationInformation serverConfigurationInformation
                = Mockito.mock(ServerConfigurationInformation.class);
        ServerContextInformation serverContextInformation
                = new ServerContextInformation(serverConfigurationInformation);
        SynapseEnvironment synEnv = new Axis2SynapseEnvironment(
                new ConfigurationContext(axisCfg), synCfg, serverContextInformation);
        axisCfg.addParameter(SynapseConstants.SYNAPSE_ENV, synEnv);
        ProxyService proxyService = new ProxyService("testBuildAxisServiceWithPinnedServersProxy");
        List<String> pinnedServersList = new ArrayList<>();
        pinnedServersList.add(pinnedServer);
        proxyService.setPinnedServers(pinnedServersList);

        //Tests with incorrect pinned servers
        Mockito.when(synEnv.getServerContextInformation()
                .getServerConfigurationInformation().getServerName()).thenReturn("10.10.10.1");
        AxisService axisService = proxyService.buildAxisService(synCfg, axisCfg);
        Assert.assertNull("Axis service built with incorrect pinned servers should be null", axisService);

        //Asserts with correct pinned servers
        Mockito.when(synEnv.getServerContextInformation()
                .getServerConfigurationInformation().getServerName()).thenReturn(pinnedServer);
        Assert.assertNotNull("Axis service should be built with correct pinned servers",
                             proxyService.buildAxisService(synCfg, axisCfg));
    }

    /**
     * Tests building a proxy service with a malformed URI specified as the published wsdl url.
     *
     * @throws Exception if an error occurs due to the malformed url
     */
    public void testBuildAxisServiceWithMalformedWsdlUri() throws Exception {
        SynapseConfiguration synCfg = new SynapseConfiguration();
        AxisConfiguration axisCfg = new AxisConfiguration();
        ProxyService proxyService = new ProxyService("testBuildAxisServiceWithInvalidWsdlUriProxy");
        //note incorrect protocol, 'files'
        URI wsdlUri = new URI("files:/home/sasikala/Documents/ei/git/wso2-synapse/modules/core/target/test-classes"
                              + "/org/apache/synapse/core/axis2/SimpleStockService.wsdl");
        proxyService.setWsdlURI(wsdlUri);
        try {
            proxyService.buildAxisService(synCfg, axisCfg);
            Assert.fail("Axis service should not be built with malformed publish wsdl URI: " + wsdlUri.toString());
        } catch (SynapseException e) {
            Assert.assertEquals("Unexpected exception thrown: " + e, "Malformed URI for wsdl", e.getMessage());
        }
    }

    /**
     * Tests building a proxy service with an unreachable URI specified as the published wsdl url with the
     * 'enablePublishWSDLSafeMode' set to true.
     *
     * @throws URISyntaxException if an error occurs when converting the URI string to a URI
     */
    public void testBuildAxisServiceWithUnreachableWsdlUriWithPublishWSDLSafeModeEnabled() throws Exception {
        SynapseConfiguration synCfg = new SynapseConfiguration();
        AxisConfiguration axisCfg = new AxisConfiguration();
        ProxyService proxyService = new ProxyService
                ("unreachableWsdlUriWithPublishWSDLSafeModeEnabledProxy");
        proxyService.addParameter("enablePublishWSDLSafeMode", true);
        URI wsdlUri = new URI("http://invalid-host/SimpleStockService.wsdl");
        proxyService.setWsdlURI(wsdlUri);
        Assert.assertNull("Axis service returned should be null", proxyService.buildAxisService(synCfg, axisCfg));
    }

    /**
     * Tests building a proxy service with an unreachable URI specified as the published wsdl url with the
     * 'enablePublishWSDLSafeMode' set to false.
     *
     * @throws Exception if an error occurs when converting the URI string to a URI
     */
    public void testBuildAxisServiceWithUnreachableWsdlUriWithPublishWSDLSafeModeDisabled() throws Exception {
        SynapseConfiguration synCfg = new SynapseConfiguration();
        AxisConfiguration axisCfg = new AxisConfiguration();
        ProxyService proxyService = new ProxyService("unreachableWsdlUriWithPublishWSDLSafeModeDisabledProxy");
        proxyService.addParameter("enablePublishWSDLSafeMode", false);
        URI wsdlUri = new URI("http://invalid-host/SimpleStockService.wsdl");
        proxyService.setWsdlURI(wsdlUri);
        try {
            proxyService.buildAxisService(synCfg, axisCfg);
            Assert.fail("Axis service should not be built with malformed publish wsdl URI: " + wsdlUri);
        } catch (SynapseException e) {
            Assert.assertEquals("Unexpected exception thrown: " + e,
                                "Error reading from wsdl URI", e.getMessage());
        }
    }

    /**
     * Tests building a proxy service with a faulty wsdl endpoint specified as the wsdl endpoint.
     *
     * @throws Exception if an error occurs when converting the URI string to a URI
     */
    public void testBuildAxisServiceWithFaultyPublishWsdlEndpoint() throws Exception {
        SynapseConfiguration synCfg = new SynapseConfiguration();
        AxisConfiguration axisCfg = new AxisConfiguration();
        ProxyService proxyService = new ProxyService("faultyPublishWsdlEndpointProxy");
        proxyService.setPublishWSDLEndpoint("wsdlEndPoint");
        try {
            proxyService.buildAxisService(synCfg, axisCfg);
            Assert.fail("Axis service built with null wsdl endpoint should throw fault");
        } catch (SynapseException e) {
            Assert.assertEquals("Unexpected exception thrown: " + e,
                                "Unable to resolve WSDL url. wsdlEndPoint is null", e.getMessage());
        }

        AddressEndpoint wsdlEndpoint = new AddressEndpoint();
        EndpointDefinition endpointDefinition = new EndpointDefinition();
        endpointDefinition.setAddress(getClass().getResource("SimpleStockService.wsdl").toURI().toString());
        wsdlEndpoint.setDefinition(endpointDefinition);
        synCfg.addEndpoint("wsdlEndPoint", wsdlEndpoint);
        try {
            proxyService.buildAxisService(synCfg, axisCfg);
            Assert.fail("Axis service built with faulty wsdl endpoint should be null");
        } catch (SynapseException e) {
            Assert.assertEquals("Unexpected exception thrown: " + e,
                                "Error building service from WSDL", e.getMessage());
        }

    }

    /**
     * Tests building a proxy service with an unreachable endpoint specified as the published wsdl url with the
     * 'enablePublishWSDLSafeMode' set to true.
     *
     * @throws Exception if an error occurs when converting the URI string to a URI
     */
    public void testBuildAxisServiceWithUnreachableWsdlEndpointWithPublishWSDLSafeModeDisabled() throws Exception {
        SynapseConfiguration synCfg = new SynapseConfiguration();
        AxisConfiguration axisCfg = new AxisConfiguration();
        ProxyService proxyService = new ProxyService("faultyPublishWsdlEndpointProxyWithPublishWSDLSafeModeDisabled");
        proxyService.setPublishWSDLEndpoint("wsdlEndPoint");

        AddressEndpoint wsdlEndpoint = new AddressEndpoint();
        EndpointDefinition endpointDefinition = new EndpointDefinition();
        endpointDefinition.setAddress((new URI("http://invalid-host/SimpleStockService.wsdl")).toString());
        wsdlEndpoint.setDefinition(endpointDefinition);
        proxyService.addParameter("enablePublishWSDLSafeMode", false);
        synCfg.addEndpoint("wsdlEndPoint", wsdlEndpoint);
        try {
            proxyService.buildAxisService(synCfg, axisCfg);
            Assert.fail("Axis service built with an unreachable wsdl endpoint should throw fault");
        } catch (SynapseException e) {
            Assert.assertTrue("Unexpected exception thrown: " + e,
                              e.getMessage().contains("Error reading from wsdl URI"));
        }
    }

    /**
     * Tests building a proxy service with an unreachable endpoint specified as the published wsdl url with the
     * 'enablePublishWSDLSafeMode' set to false.
     *
     * @throws Exception if an error occurs when converting the URI string to a URI
     */
    public void testBuildAxisServiceWithUnreachableWsdlEndpointWithPublishWSDLSafeModeEnabled() throws Exception {
        MessageContext synCtx = new TestMessageContext();
        SynapseConfiguration synCfg = new SynapseConfiguration();
        synCtx.setConfiguration(synCfg);
        AxisConfiguration axisCfg = new AxisConfiguration();
        ProxyService proxyService = new ProxyService("faultyPublishWsdlEndpointProxyWithPublishWSDLSafeModeEnabled");
        proxyService.setPublishWSDLEndpoint("wsdlEndPoint");

        AddressEndpoint wsdlEndpoint = new AddressEndpoint();
        EndpointDefinition endpointDefinition = new EndpointDefinition();
        endpointDefinition.setAddress((new URI("http://invalid-host/SimpleStockService.wsdl")).toString());
        wsdlEndpoint.setDefinition(endpointDefinition);
        proxyService.addParameter("enablePublishWSDLSafeMode", true);
        synCfg.addEndpoint("wsdlEndPoint", wsdlEndpoint);
        Assert.assertNull("Axis service built with an unreachable wsdl endpoint be null",
                          proxyService.buildAxisService(synCfg, axisCfg));
    }

    /**
     * Tests starting a proxy service with correct axis configuration provided.
     *
     * @throws Exception if an exception occurs while configuring the axis configuration
     */
    public void testStart() throws Exception {
        String proxyServiceName = "TestStartProxy";
        SynapseConfiguration synCfg = new SynapseConfiguration();
        AxisConfiguration axisCfg = new AxisConfiguration();
        SynapseEnvironment synEnv = new Axis2SynapseEnvironment(new ConfigurationContext(axisCfg), synCfg);
        axisCfg.addParameter(SynapseConstants.SYNAPSE_ENV, synEnv);
        synCfg.setAxisConfiguration(axisCfg);
        ProxyService proxyService = new ProxyService(proxyServiceName);
        AxisService axisServiceForActivation = new AxisService();
        axisServiceForActivation.setName(proxyServiceName);
        axisCfg.addToAllServicesMap(axisServiceForActivation);
        proxyService.setTargetInLineInSequence(new SequenceMediator());
        proxyService.setTargetInLineOutSequence(new SequenceMediator());
        proxyService.setTargetInLineFaultSequence(new SequenceMediator());
        proxyService.start(synCfg);
        Assert.assertTrue("Underlying Axis service is not activated", axisServiceForActivation.isActive());
        Assert.assertTrue("Proxy service is not running", proxyService.isRunning());
        proxyService.stop(synCfg);
    }

    /**
     * Tests starting a proxy service without proving an axis configuration. The proxy service should not start by this.
     */
    public void testStartWithoutAxisConfiguration() {
        SynapseConfiguration synCfg = new SynapseConfiguration();
        synCfg.setAxisConfiguration(null);
        ProxyService proxyService = new ProxyService("TestStartWithoutAxisConfigurationProxy");
        proxyService.start(synCfg);
        Assert.assertFalse("Proxy service is running without axis configuration", proxyService.isRunning());
        proxyService.stop(synCfg);
    }

    /**
     * Tests if the correct fault handler is set when a fault handler is not explicitly set, and when set by using
     * targetFaultSequence and targetInlineFaultSequence properties of the proxy.
     */
    public void testRegisterFaultHandler() {

        ProxyService proxyService = new ProxyService("TestRegisterFaultHandlerProxy");
        MessageContext messageContext = new TestMessageContext();
        SynapseConfiguration synCfg = new SynapseConfiguration();
        messageContext.setConfiguration(synCfg);
        SequenceMediator faultMediator = new SequenceMediator();
        synCfg.addSequence("fault", faultMediator);

        //test if the default fault proxy set in the message context is returned when a fault sequence is not
        // explicitly set
        faultMediator.setName("defaultFault");
        proxyService.registerFaultHandler(messageContext);
        String faultMediatorName = ((SequenceMediator) ((MediatorFaultHandler) messageContext.getFaultStack().pop())
                .getFaultMediator()).getName();
        Assert.assertEquals("Incorrect fault handler set: " + faultMediatorName, "defaultFault", faultMediatorName);

        //tests the functionality when the fault sequence is set in line in the target
        faultMediator.setName("targetInLineFaultSequenceMediator");
        proxyService.setTargetInLineFaultSequence(faultMediator);
        proxyService.registerFaultHandler(messageContext);
        faultMediatorName = ((SequenceMediator) ((MediatorFaultHandler) messageContext.getFaultStack().pop())
                .getFaultMediator()).getName();
        Assert.assertEquals("Incorrect fault handler set: "
                            + faultMediatorName, "targetInLineFaultSequenceMediator", faultMediatorName);

        //tests the functionality when the fault sequence is set separately in the target
        AxisConfiguration axisCfg = new AxisConfiguration();
        SynapseEnvironment synEnv = new Axis2SynapseEnvironment(new ConfigurationContext(axisCfg), synCfg);
        messageContext.setEnvironment(synEnv);
        proxyService.setTargetFaultSequence("targetFaultSequence");

        //when the message context does not have the correct fault sequence specified as the target fault sequence
        faultMediator.setName("defaultFaultMediator");
        proxyService.registerFaultHandler(messageContext);
        faultMediatorName = ((SequenceMediator) ((MediatorFaultHandler) messageContext.getFaultStack().pop())
                .getFaultMediator()).getName();
        Assert.assertEquals(
                "Incorrect fault handler set: " + faultMediatorName, "defaultFaultMediator", faultMediatorName);

        //when the message context has the correct fault sequence specified as the target fault sequence
        faultMediator.setName("targetFaultSequenceMediator");
        synCfg.addSequence("targetFaultSequence", faultMediator);
        proxyService.registerFaultHandler(messageContext);
        faultMediatorName = ((SequenceMediator) ((MediatorFaultHandler) messageContext.getFaultStack().pop())
                .getFaultMediator()).getName();
        Assert.assertEquals(
                "Incorrect fault handler set: " + faultMediatorName, "targetFaultSequenceMediator", faultMediatorName);
    }
}
