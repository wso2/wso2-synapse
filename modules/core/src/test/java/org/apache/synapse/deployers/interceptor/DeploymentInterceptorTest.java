/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.deployers.interceptor;

import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.axis2.ProxyService;
import org.apache.synapse.data.ConfigDataHolder;
import org.apache.synapse.registry.Registry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Tests persisting the stopped state of proxy services through the DeploymentInterceptor.
 */
public class DeploymentInterceptorTest {

    private static final String PROXY_NAME = "StatePersistenceProxy";
    private static final String SERVICE_RESOURCE_PATH =
            "/repository/axis2/service-groups/" + PROXY_NAME + "/services/" + PROXY_NAME;

    private Registry previousRegistry;
    private Registry registry;
    private SynapseConfiguration synapseConfiguration;
    private AxisService axisService;

    @Before
    public void setUp() throws Exception {
        previousRegistry = ConfigDataHolder.getInstance().getRegistry();
        registry = Mockito.mock(Registry.class);
        ConfigDataHolder.getInstance().setRegistry(registry);

        AxisConfiguration axisConfiguration = new AxisConfiguration();
        axisConfiguration.addObservers(new DeploymentInterceptor());
        axisService = new AxisService(PROXY_NAME);
        axisConfiguration.addService(axisService);

        synapseConfiguration = new SynapseConfiguration();
        synapseConfiguration.setAxisConfiguration(axisConfiguration);
    }

    @After
    public void tearDown() {
        ConfigDataHolder.getInstance().setRegistry(previousRegistry);
    }

    @Test
    public void testStopPersistsState() {
        new ProxyService(PROXY_NAME).stop(synapseConfiguration);

        assertFalse(axisService.isActive());
        Mockito.verify(registry).newNonEmptyResource(eq(SERVICE_RESOURCE_PATH), eq(false), eq("text/plain"),
                eq("false"), eq(DeploymentInterceptor.ACTIVE));
    }

    @Test
    public void testStopWithoutPersistingState() {
        new ProxyService(PROXY_NAME).stop(synapseConfiguration, false);

        assertFalse(axisService.isActive());
        Mockito.verify(registry, Mockito.never()).newNonEmptyResource(anyString(), anyBoolean(), anyString(),
                anyString(), anyString());
        assertNull("The skip parameter should be removed after the service is stopped",
                axisService.getParameter(DeploymentInterceptor.SKIP_SERVICE_STATE_PERSISTENCE_PARAM));
    }

    @Test
    public void testParameterDefinedByParentIsNotRemoved() throws Exception {
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        Parameter lockedParam = new Parameter(DeploymentInterceptor.SKIP_SERVICE_STATE_PERSISTENCE_PARAM, "false");
        lockedParam.setLocked(true);
        axisConfiguration.addParameter(lockedParam);

        new ProxyService(PROXY_NAME).stop(synapseConfiguration, false);

        assertFalse(axisService.isActive());
        assertSame("The parameter defined by the parent should not be removed", lockedParam,
                axisConfiguration.getParameter(DeploymentInterceptor.SKIP_SERVICE_STATE_PERSISTENCE_PARAM));
        // the parameter could not be added, so the stop falls back to being persisted
        Mockito.verify(registry).newNonEmptyResource(eq(SERVICE_RESOURCE_PATH), eq(false), eq("text/plain"),
                eq("false"), eq(DeploymentInterceptor.ACTIVE));
    }

    @Test
    public void testStopPersistsStateAfterNonPersistentStop() {
        ProxyService proxy = new ProxyService(PROXY_NAME);
        proxy.stop(synapseConfiguration, false);
        axisService.setActive(true);
        proxy.stop(synapseConfiguration);

        Mockito.verify(registry, Mockito.times(1)).newNonEmptyResource(eq(SERVICE_RESOURCE_PATH), eq(false),
                eq("text/plain"), eq("false"), eq(DeploymentInterceptor.ACTIVE));
    }
}
