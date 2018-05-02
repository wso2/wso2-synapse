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

package org.apache.synapse.endpoints.dispatch;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.log4j.BasicConfigurator;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.endpoints.AddressEndpoint;
import org.apache.synapse.endpoints.Endpoint;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Test class for SALSession
 */
public class SALSessionsTest {
    /**
     * Test updating session with session id
     *
     * @throws Exception
     */
    @Test
    public void testUpdateWithId() throws Exception {
        BasicConfigurator.configure();
        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        Axis2MessageContext axis2MessageContext = new Axis2MessageContext(new org.apache.axis2.context.MessageContext(),
                synapseConfiguration, synapseEnvironment);
        MessageContext messageContext = axis2MessageContext;
        Endpoint endpoint = new AddressEndpoint();
        List<Endpoint> endpoints = new ArrayList<>();
        endpoints.add(endpoint);
        messageContext.setProperty(SynapseConstants.PROP_SAL_ENDPOINT_ENDPOINT_LIST, endpoints);

        SALSessions salSessions = SALSessions.getInstance();
        salSessions.initialize(false, cfgCtx);

        salSessions.updateSession(messageContext, "testSession");

        SessionInformation sessionInformation = salSessions.getSession("testSession");
        Assert.assertEquals("Session not updated!", "testSession", sessionInformation.getId());
    }

    /**
     * Test updating session with cookie
     *
     * @throws Exception
     */
    @Test
    public void testUpdateWithCookie() throws Exception {
        BasicConfigurator.configure();
        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        Axis2MessageContext axis2MessageContext = new Axis2MessageContext(new org.apache.axis2.context.MessageContext(),
                synapseConfiguration, synapseEnvironment);
        MessageContext messageContext = axis2MessageContext;
        Endpoint endpoint = new AddressEndpoint();
        List<Endpoint> endpoints = new ArrayList<>();
        endpoints.add(endpoint);
        messageContext.setProperty(SynapseConstants.PROP_SAL_ENDPOINT_ENDPOINT_LIST, endpoints);

        SALSessions salSessions = SALSessions.getInstance();
        salSessions.initialize(false, cfgCtx);

        SessionCookie sessionCookie = new SessionCookie();
        sessionCookie.setSessionId("testCookie");

        salSessions.updateSession(messageContext, sessionCookie);

        SessionInformation sessionInformation = salSessions.getSession("testCookie");
        Assert.assertEquals("Session not updated!", "testCookie", sessionInformation.getId());
    }

    /**
     * Test updating session with session id and old session
     *
     * @throws Exception
     */
    @Test
    public void testUpdateWithOldSession() throws Exception {
        BasicConfigurator.configure();
        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        Axis2MessageContext axis2MessageContext = new Axis2MessageContext(new org.apache.axis2.context.MessageContext(),
                synapseConfiguration, synapseEnvironment);
        MessageContext messageContext = axis2MessageContext;
        Endpoint endpoint = new AddressEndpoint();
        List<Endpoint> endpoints = new ArrayList<>();
        endpoints.add(endpoint);

        SALSessions salSessions = SALSessions.getInstance();
        salSessions.initialize(false, cfgCtx);

        SessionInformation oldSessionInfo = new SessionInformation("oldTestSession", endpoints, 30000);
        messageContext.setProperty(SynapseConstants.PROP_SAL_CURRENT_SESSION_INFORMATION, oldSessionInfo);
        salSessions.updateSession(messageContext, "testSession2");

        SessionInformation sessionInformation = salSessions.getSession("testSession2");
        Assert.assertEquals("Session not updated!", "testSession2", sessionInformation.getId());
    }

    /**
     * Test updating session with session id and old session where the old session id is same as current session id
     *
     * @throws Exception
     */
    @Test
    public void testUpdateWithOldSessionSameName() throws Exception {
        BasicConfigurator.configure();
        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        Axis2MessageContext axis2MessageContext = new Axis2MessageContext(new org.apache.axis2.context.MessageContext(),
                synapseConfiguration, synapseEnvironment);
        MessageContext messageContext = axis2MessageContext;
        Endpoint endpoint = new AddressEndpoint();
        List<Endpoint> endpoints = new ArrayList<>();
        endpoints.add(endpoint);
        messageContext.setProperty(SynapseConstants.PROP_SAL_ENDPOINT_ENDPOINT_LIST, endpoints);

        SALSessions salSessions = SALSessions.getInstance();
        salSessions.initialize(false, cfgCtx);

        SessionInformation oldSessionInfo = new SessionInformation("testSession3", endpoints, 30000);
        messageContext.setProperty(SynapseConstants.PROP_SAL_CURRENT_SESSION_INFORMATION, oldSessionInfo);
        salSessions.updateSession(messageContext, "testSession3");

        SessionInformation sessionInformation = salSessions.getSession("testSession3");
        Assert.assertEquals("Session not updated!", "testSession3", sessionInformation.getId());
    }

    /**
     * Test updating session with cookie and old session
     *
     * @throws Exception
     */
    @Test
    public void testUpdateCookieWithOldSession() throws Exception {
        BasicConfigurator.configure();
        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        Axis2MessageContext axis2MessageContext = new Axis2MessageContext(new org.apache.axis2.context.MessageContext(),
                synapseConfiguration, synapseEnvironment);
        MessageContext messageContext = axis2MessageContext;
        Endpoint endpoint = new AddressEndpoint();
        List<Endpoint> endpoints = new ArrayList<>();
        endpoints.add(endpoint);

        SALSessions salSessions = SALSessions.getInstance();
        salSessions.initialize(false, cfgCtx);

        SessionInformation oldSessionInfo = new SessionInformation("oldTestSession", endpoints, 30000);
        List<String> path = new ArrayList<>();
        path.add("samplePath");
        oldSessionInfo.setPath(path);
        messageContext.setProperty(SynapseConstants.PROP_SAL_CURRENT_SESSION_INFORMATION, oldSessionInfo);
        SessionCookie sessionCookie = new SessionCookie();
        sessionCookie.setSessionId("testCookie2");
        sessionCookie.setPath("samplePath");
        salSessions.updateSession(messageContext, sessionCookie);

        SessionInformation sessionInformation = salSessions.getSession("testCookie2");
        Assert.assertEquals("Session not updated!", "testCookie2", sessionInformation.getId());
    }

    /**
     * Test updating session with cookie and old session where the old session id is same as current session id
     *
     * @throws Exception
     */
    @Test
    public void testUpdateCookieWithOldSessionSameName() throws Exception {
        BasicConfigurator.configure();
        SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
        AxisConfiguration axisConfiguration = synapseConfiguration.getAxisConfiguration();
        ConfigurationContext cfgCtx = new ConfigurationContext(axisConfiguration);
        SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(cfgCtx, synapseConfiguration);
        Axis2MessageContext axis2MessageContext = new Axis2MessageContext(new org.apache.axis2.context.MessageContext(),
                synapseConfiguration, synapseEnvironment);
        MessageContext messageContext = axis2MessageContext;
        Endpoint endpoint = new AddressEndpoint();
        List<Endpoint> endpoints = new ArrayList<>();
        endpoints.add(endpoint);
        messageContext.setProperty(SynapseConstants.PROP_SAL_ENDPOINT_ENDPOINT_LIST, endpoints);

        SALSessions salSessions = SALSessions.getInstance();
        salSessions.initialize(false, cfgCtx);

        SessionInformation oldSessionInfo = new SessionInformation("testCookie3", endpoints, 30000);
        List<String> path = new ArrayList<>();
        path.add("samplePath");
        oldSessionInfo.setPath(path);
        messageContext.setProperty(SynapseConstants.PROP_SAL_CURRENT_SESSION_INFORMATION, oldSessionInfo);
        SessionCookie sessionCookie = new SessionCookie();
        sessionCookie.setSessionId("testCookie3");
        sessionCookie.setPath("samplePath");
        salSessions.updateSession(messageContext, sessionCookie);

        SessionInformation sessionInformation = salSessions.getSession("testCookie3");
        Assert.assertEquals("Session not updated!", "testCookie3", sessionInformation.getId());
    }
}