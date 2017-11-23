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


import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.util.UIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.clustering.Member;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.LoadBalanceMembershipHandler;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.endpoints.algorithms.AlgorithmContext;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import static org.mockito.ArgumentMatchers.any;

/**
 * Test for DynamicLoadBalanceEndpoint
 */
public class DynamicLoadBalanceEndpointTest {

    /**
     * Test initialization of DynamicLoadBalanceEndpoint
     *
     * @throws AxisFault on an issue initializing DynamicLoadBalanceEndpoint
     */
    @Test
    public void testInit() throws AxisFault {
        DynamicLoadbalanceEndpoint dynamicLoadbalanceEndpoint = new DynamicLoadbalanceEndpoint();
        dynamicLoadbalanceEndpoint.init(getMockedSynapseEnvironment());
    }

    /**
     * Test sending a message to a load balance EP
     *
     * @throws AxisFault on an issue sending message to DynamicLoadBalanceEndpoint
     */
    @Test
    public void testSend() throws AxisFault {
        DynamicLoadbalanceEndpoint dynamicLoadbalanceEndpoint = new DynamicLoadbalanceEndpoint();
        SynapseEnvironment synapseEnvironment = getMockedSynapseEnvironment();
        dynamicLoadbalanceEndpoint.init(synapseEnvironment);
        MessageContext messageContext = createMessageContext();

        //Mock a LoadBalanceMembershipHandler and set
        LoadBalanceMembershipHandler loadBalanceMembershipHandler = PowerMockito.
                mock(LoadBalanceMembershipHandler.class);
        Member member1 = new Member("localhost", 9000);
        Mockito.when(loadBalanceMembershipHandler.
                getNextApplicationMember(any(AlgorithmContext.class))).thenReturn(member1);
        dynamicLoadbalanceEndpoint.setLoadBalanceMembershipHandler(loadBalanceMembershipHandler);

        //set mocked SynapseEnvironment to message context
        ((Axis2MessageContext)messageContext).getAxis2MessageContext().
                getConfigurationContext().getAxisConfiguration().
                addParameter(SynapseConstants.SYNAPSE_ENV, synapseEnvironment);
        messageContext.setEnvironment(synapseEnvironment);

        //send message
        dynamicLoadbalanceEndpoint.send(messageContext);

        Assert.assertNotNull("SynapseConstants.LAST_ENDPOINT should return a not null value",
                messageContext.getProperty(SynapseConstants.LAST_ENDPOINT));
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
        axis2MC.setTo(new EndpointReference("http://localhost:9000/services/SimpleStockQuoteService"));
        MessageContext mc = new Axis2MessageContext(axis2MC, new SynapseConfiguration(), synapseEnvironment);
        mc.setMessageID(UIDGenerator.generateURNString());
        mc.setEnvelope(OMAbstractFactory.getSOAP12Factory().createSOAPEnvelope());
        mc.getEnvelope().addChild(OMAbstractFactory.getSOAP12Factory().createSOAPBody());
        return mc;
    }
}
