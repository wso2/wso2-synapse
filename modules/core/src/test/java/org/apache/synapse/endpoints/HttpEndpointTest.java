/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.endpoints;

import com.damnhandy.uri.template.UriTemplate;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axiom.util.UIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.endpoints.HTTPEndpointFactory;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import javax.xml.stream.XMLStreamException;

/**
 * Test for HttpEndpoint's uri-template combinations
 */
public class HttpEndpointTest {

    @Test
    public void testInit() throws AxisFault {
        HTTPEndpoint httpEndpoint = new HTTPEndpoint();
        httpEndpoint.init(getMockedSynapseEnvironment());
    }

    /**
     * Tests sending sample reserved characters(@,:) as query parameter content
     * @throws AxisFault
     * @throws XMLStreamException
     */
    @Test
    public void testQueryParamsAsReservedChars() throws AxisFault, XMLStreamException {

        HTTPEndpointFactory factory = new HTTPEndpointFactory();
        OMElement em = AXIOMUtil.stringToOM("<endpoint><http method=\"GET\" uri-template=\"http://abc.com?symbol={query.param.symbol}&amp;user={query.param.user}\"/></endpoint>");
        EndpointDefinition ep1 = factory.createEndpointDefinition(em);

        HTTPEndpoint httpEndpoint = new HTTPEndpoint();
        httpEndpoint.setHttpMethod("GET");
        httpEndpoint.setDefinition(ep1);
        httpEndpoint.setUriTemplate(UriTemplate.fromTemplate("http://abc.com?symbol={query.param.symbol}&amp;user={query.param.user}"));
        SynapseEnvironment synapseEnvironment = getMockedSynapseEnvironment();
        httpEndpoint.init(getMockedSynapseEnvironment());
        MessageContext messageContext = createMessageContext();
        messageContext.setProperty("query.param.symbol", "US:123");
        messageContext.setProperty("query.param.user", "john@gmail");
        //set mocked SynapseEnvironment to message context
        ((Axis2MessageContext) messageContext).getAxis2MessageContext().
                getConfigurationContext().getAxisConfiguration().
                addParameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment);
        messageContext.setEnvironment(synapseEnvironment);
        httpEndpoint.executeEpTypeSpecificFunctions(messageContext);
        Assert.assertEquals("Reserved characters need to be encoded",
                "http://abc.com?symbol=US%3A123&amp;user=john%40gmail", messageContext.getTo().getAddress().toString());


    }

    /**
     * Tests sending sample characters that may be unreserved (@,: etc) as query parameter content
     */
    @Test
    public void testQueryParamsAsUnreservedChars() throws AxisFault, XMLStreamException {

        HTTPEndpointFactory factory = new HTTPEndpointFactory();
        OMElement em = AXIOMUtil.stringToOM("<endpoint><http method=\"GET\" uri-template=\"http://abc.com?symbol={+query.param.symbol}&amp;user={+query.param.user}\"/></endpoint>");

        EndpointDefinition ep1 = factory.createEndpointDefinition(em);

        HTTPEndpoint httpEndpoint = new HTTPEndpoint();
        httpEndpoint.setHttpMethod("GET");
        httpEndpoint.setDefinition(ep1);
        httpEndpoint.setUriTemplate(UriTemplate.fromTemplate("http://abc.com?symbol={+query.param.symbol}&amp;user={+query.param.user}"));
        SynapseEnvironment synapseEnvironment = getMockedSynapseEnvironment();
        httpEndpoint.init(getMockedSynapseEnvironment());
        MessageContext messageContext = createMessageContext();
        messageContext.setProperty("query.param.symbol", "US:123");
        messageContext.setProperty("query.param.user", "john@gmail");
        //set mocked SynapseEnvironment to message context
        ((Axis2MessageContext) messageContext).getAxis2MessageContext().
                getConfigurationContext().getAxisConfiguration().
                addParameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment);
        messageContext.setEnvironment(synapseEnvironment);
        httpEndpoint.executeEpTypeSpecificFunctions(messageContext);
        Assert.assertEquals("Reserved characters need to be sent in default format without encoding",
                "http://abc.com?symbol=US:123&amp;user=john@gmail", messageContext.getTo().getAddress().toString());

    }

    /**
     * Test usage of legacy-encoding property where encoded values will be decoded
     * @throws AxisFault
     * @throws XMLStreamException
     */
    @Test
    public void testQueryParamsWithLegacyEncoding() throws AxisFault, XMLStreamException {

        HTTPEndpointFactory factory = new HTTPEndpointFactory();
        OMElement em = AXIOMUtil.stringToOM("<endpoint><http method=\"GET\" uri-template=\"http://abc.com?symbol={query.param.symbol}&amp;user={query.param.user}\"/></endpoint>");

        EndpointDefinition ep1 = factory.createEndpointDefinition(em);

        HTTPEndpoint httpEndpoint = new HTTPEndpoint();
        httpEndpoint.setHttpMethod("GET");
        httpEndpoint.setLegacySupport(true);
        httpEndpoint.setDefinition(ep1);
        httpEndpoint.setUriTemplate(UriTemplate.fromTemplate("http://abc.com?symbol={query.param.symbol}&amp;user={query.param.user}"));
        SynapseEnvironment synapseEnvironment = getMockedSynapseEnvironment();
        httpEndpoint.init(getMockedSynapseEnvironment());
        MessageContext messageContext = createMessageContext();
        messageContext.setProperty("query.param.symbol", "US%3A123");
        messageContext.setProperty("query.param.user", "john%40G%C3%BCnter");
        //set mocked SynapseEnvironment to message context
        ((Axis2MessageContext) messageContext).getAxis2MessageContext().
                getConfigurationContext().getAxisConfiguration().
                addParameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment);
        messageContext.setEnvironment(synapseEnvironment);
        httpEndpoint.executeEpTypeSpecificFunctions(messageContext);
        Assert.assertEquals("With legacy encoding encoded characters need to be decoded",
                "http://abc.com?symbol=US:123&amp;user=john@Günter", messageContext.getTo().getAddress().toString());

    }

    /**
     * Create a mock SynapseEnvironment object
     *
     * @return Axis2SynapseEnvironment instance
     * @throws AxisFault on creating/mocking object
     */
    private Axis2SynapseEnvironment getMockedSynapseEnvironment() throws AxisFault {
        Axis2SynapseEnvironment synapseEnvironment = PowerMockito.mock(Axis2SynapseEnvironment.class);
        ConfigurationContext axis2ConfigurationContext = new ConfigurationContext(new AxisConfiguration());
        axis2ConfigurationContext.getAxisConfiguration().addParameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment);
        Mockito.when(synapseEnvironment.getAxis2ConfigurationContext()).thenReturn(axis2ConfigurationContext);
        return synapseEnvironment;
    }

    /**
     * Create a empty message context
     *
     * @return A context with empty message
     * @throws AxisFault on an error creating a context
     */
    private MessageContext createMessageContext() throws AxisFault {

        Axis2SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(new SynapseConfiguration());
        org.apache.axis2.context.MessageContext axis2MC
                = new org.apache.axis2.context.MessageContext();
        axis2MC.setConfigurationContext(new ConfigurationContext(new AxisConfiguration()));

        ServiceContext svcCtx = new ServiceContext();
        OperationContext opCtx = new OperationContext(new InOutAxisOperation(), svcCtx);
        axis2MC.setServiceContext(svcCtx);
        axis2MC.setOperationContext(opCtx);
        axis2MC.setTransportIn(new TransportInDescription("http"));
      //  axis2MC.setTo(new EndpointReference("http://localhost:9000/services/SimpleStockQuoteService"));
        MessageContext mc = new Axis2MessageContext(axis2MC, new SynapseConfiguration(), synapseEnvironment);
        mc.setMessageID(UIDGenerator.generateURNString());
        mc.setEnvelope(OMAbstractFactory.getSOAP12Factory().createSOAPEnvelope());
        mc.getEnvelope().addChild(OMAbstractFactory.getSOAP12Factory().createSOAPBody());
        return mc;
    }
}
