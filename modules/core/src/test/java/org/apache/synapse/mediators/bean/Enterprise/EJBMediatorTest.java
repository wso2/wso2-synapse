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
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.mediators.bean.Enterprise;

import org.apache.synapse.ServerConfigurationInformation;
import org.apache.synapse.ServerContextInformation;
import org.apache.synapse.SynapseException;
import org.apache.synapse.commons.beanstalk.enterprise.EnterpriseBeanstalk;
import org.apache.synapse.commons.beanstalk.enterprise.EnterpriseBeanstalkConstants;
import org.apache.synapse.commons.beanstalk.enterprise.EnterpriseBeanstalkManager;
import org.apache.synapse.config.Entry;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.bean.BeanUtils;
import org.apache.synapse.mediators.bean.enterprise.EJBMediator;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;

/**
 * Unit tests for EJBMediator class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({BeanUtils.class, EnterpriseBeanstalk.class})
public class EJBMediatorTest {

    private static final String BEANSTALK_NAME = "testBeansTalk";
    private static final String CLASS_NAME = "testClassName";
    private static final String JNDI_NAME = "testJndiName";
    private static final String VALUE = "testValue";
    private static EJBMediator ejbMediator = new EJBMediator();
    private static EnterpriseBeanstalk beanstalk;

    @Rule
    public static ExpectedException thrown = ExpectedException.none();

    /**
     * Initializing EJBMediator and Mediating a messageContext
     */
    @Test
    public void testInitAndMediate() throws Exception {
        SynapseEnvironment synapseEnvironment = Mockito.mock(SynapseEnvironment.class);
        ServerConfigurationInformation configurationInformation = new ServerConfigurationInformation();
        ServerContextInformation contextInformation = new ServerContextInformation(configurationInformation);
        Mockito.when(synapseEnvironment.getServerContextInformation()).thenReturn(contextInformation);
        try {
            ejbMediator.init(synapseEnvironment);
        } catch (Exception ex) {
            Assert.assertEquals("assert exception class", SynapseException.class, ex.getClass());
            Assert.assertEquals("assert exception message",
                    "Initialization failed. BeanstalkManager not found.", ex.getMessage());
        }
        ejbMediator.setBeanstalkName(BEANSTALK_NAME);
        EnterpriseBeanstalkManager beanstalkManager = Mockito.mock(EnterpriseBeanstalkManager.class);
        contextInformation.addProperty(EnterpriseBeanstalkConstants.BEANSTALK_MANAGER_PROP_NAME, beanstalkManager);
        try {
            ejbMediator.init(synapseEnvironment);
        } catch (Exception ex) {
            Assert.assertEquals("assert exception class", SynapseException.class, ex.getClass());
            Assert.assertEquals("assert exception message", "Initialization failed. '"
                    + BEANSTALK_NAME + "' " + "beanstalk not found.", ex.getMessage());
        }
        beanstalk = PowerMockito.mock(EnterpriseBeanstalk.class);
        Value beanId = new Value(VALUE);
        ejbMediator.setBeanId(beanId);
        Object obj = new Object();
        PowerMockito.when(beanstalk.getEnterpriseBean(any(String.class), any(String.class), any(String.class)))
                .thenReturn(obj);
        PowerMockito.when(beanstalkManager.getBeanstalk(BEANSTALK_NAME)).thenReturn(beanstalk);
        PowerMockito.mockStatic(BeanUtils.class);
        PowerMockito.when(BeanUtils.invokeInstanceMethod(any(Object.class), any(Method.class), any(Object[].class)))
                .thenReturn(new Object());
        ejbMediator.init(synapseEnvironment);
        String samplePayload = "<test>value</test>";
        Map<String, Entry> properties = new HashMap<>();
        Axis2MessageContext messageContext = TestUtils.getAxis2MessageContext(samplePayload, properties);
        ejbMediator.setClassName(CLASS_NAME);
        ejbMediator.setJndiName(JNDI_NAME);
        Assert.assertTrue("mediation must be successful", ejbMediator.mediate(messageContext));
    }
}
