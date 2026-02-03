/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.apache.synapse.transport.passthru;

import junit.framework.TestCase;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.synapse.transport.http.conn.ProxyConfig;
import org.apache.synapse.transport.passthru.config.TargetConfiguration;
import org.apache.synapse.transport.passthru.connections.TargetConnections;
import org.junit.Assert;
import org.junit.Test;
import static org.mockito.Mockito.mock;

/**
 * Test case for DeliveryAgent class
 */
public class DeliveryAgentTest extends TestCase {
    /**
     * This method tests whether the message is queued by the delivery agent when the connection is null
     *
     * @throws Exception
     */
    @Test
    public void test() throws Exception {
        MessageContext messageContext = new MessageContext();
        ServiceContext svcCtx = new ServiceContext();
        OperationContext opCtx = new OperationContext(new InOutAxisOperation(), svcCtx);
        messageContext.setServiceContext(svcCtx);
        messageContext.setOperationContext(opCtx);
        TargetConfiguration targetConfiguration = mock(TargetConfiguration.class);
        TargetConnections conns = mock(TargetConnections.class);
        ProxyConfig config = mock(ProxyConfig.class);
        EndpointReference epr = new EndpointReference("http://127.0.0.1:3001/services");
        DeliveryAgent agent = new DeliveryAgent(targetConfiguration, conns, config);
        agent.submit(messageContext, epr);

        Assert.assertEquals("127.0.0.1", messageContext.getProperty("PROXY_PROFILE_TARGET_HOST"));

    }
}