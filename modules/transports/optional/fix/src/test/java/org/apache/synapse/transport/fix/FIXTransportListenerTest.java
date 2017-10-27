/*
 *     Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *     WSO2 Inc. licenses this file to you under the Apache License,
 *     Version 2.0 (the "License"); you may not use this file except
 *     in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */

package org.apache.synapse.transport.fix;

import junit.framework.TestCase;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.testkit.axis2.TransportDescriptionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.ArgumentMatchers.anyString;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
@PrepareForTest(FIXTransportListener.class)
public class FIXTransportListenerTest extends TestCase {
    @Mock(name = "sessionFactory")
    FIXSessionFactory fixSessionFactory;

    @InjectMocks
    FIXTransportListener spy;

    private static Log logger = LogFactory.getLog(FIXTransportListenerTest.class.getName());

    @Test
    public void testFIXTransportListenerInit() throws Exception {
        AxisService axisService = new AxisService("testFIXService");
        axisService.addParameter(new Parameter(FIXConstants.FIX_ACCEPTOR_CONFIG_URL_PARAM, "/sample/path/Mock.cfg"));
        axisService.addParameter(new Parameter(FIXConstants.FIX_INITIATOR_CONFIG_URL_PARAM, "/sample/path/Mock2.cfg"));

        ConfigurationContext cfgCtx = new ConfigurationContext(new AxisConfiguration());
        TransportDescriptionFactory transportDescriptionFactory = new TransportDescriptionFactory() {
            @Override
            public TransportOutDescription createTransportOutDescription() throws Exception {
                TransportOutDescription trpOutDesc = new TransportOutDescription("fix");
                trpOutDesc.setSender(new FIXTransportSender());
                return trpOutDesc;
            }

            @Override
            public TransportInDescription createTransportInDescription() throws Exception {
                TransportInDescription trpInDesc = new TransportInDescription("fix");
                trpInDesc.setReceiver(new FIXTransportListener());
                return trpInDesc;
            }
        };
        TransportInDescription transportInDescription = null;
        try {
            transportInDescription = transportDescriptionFactory.createTransportInDescription();
        } catch (Exception e) {
            logger.error(e);
            Assert.fail("Error occurred while creating transport in description");
        }

        FIXTransportListener listner = new FIXTransportListener();
        listner.init(cfgCtx, transportInDescription);
    }

    @Test
    public void testGetEPRs() throws Exception {
        MockitoAnnotations.initMocks(this);
        String[] serviceEPRStrings = { "epr1", "epr2" };

        PowerMockito.when(fixSessionFactory.getServiceEPRs(anyString(), anyString())).thenReturn(serviceEPRStrings);

        EndpointReference[] eprs = spy.getEPRsForService("test.service", "random.ip");
        Assert.assertNotNull("Cannot get endpoint refernces from the service!", eprs);

    }
}