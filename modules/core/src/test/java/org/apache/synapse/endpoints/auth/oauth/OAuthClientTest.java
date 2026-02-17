/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.endpoints.auth.oauth;

import junit.framework.TestCase;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.core.axis2.Axis2SynapseEnvironment;
import org.apache.synapse.endpoints.TrustStoreConfigs;
import org.apache.synapse.endpoints.ProxyConfigs;
import org.apache.synapse.transport.passthru.PassThroughHttpSender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;

/**
 * Test for OAuthClient that is used to generate tokens
 */

@RunWith(MockitoJUnitRunner.class)
public class OAuthClientTest extends TestCase {

    /**
     * Tests if the oauth client correctly parse the response and generate a Token object
     *
     * @throws Exception
     */
    @Test
    public void testGenerateToken() throws Exception {

        HttpClientBuilder mockClientBuilder = Mockito.mock(HttpClientBuilder.class);
        CloseableHttpClient mockHttpClient = Mockito.mock(CloseableHttpClient.class);
        CloseableHttpResponse mockResponse = Mockito.mock(CloseableHttpResponse.class);
        HttpEntity entity = Mockito.mock(HttpEntity.class);
        StatusLine statusLine = Mockito.mock(StatusLine.class);

        try (MockedStatic<HttpClientBuilder> mockedBuilder = Mockito.mockStatic(HttpClientBuilder.class)) {
            mockedBuilder.when(HttpClientBuilder::create).thenReturn(mockClientBuilder);
            Mockito.when(mockClientBuilder.setDefaultRequestConfig(Mockito.any(RequestConfig.class))).thenReturn(mockClientBuilder);
            Mockito.when(mockClientBuilder.setConnectionManager(any(HttpClientConnectionManager.class))).thenReturn(mockClientBuilder);
            Mockito.when(mockClientBuilder.setSSLSocketFactory(any())).thenReturn(mockClientBuilder);
            Mockito.when(mockClientBuilder.build()).thenReturn(mockHttpClient);
            Mockito.when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);

            Mockito.when(statusLine.getStatusCode()).thenReturn(200);
            Mockito.when(mockResponse.getStatusLine()).thenReturn(statusLine);

            Mockito.when(mockResponse.getEntity()).thenReturn(entity);

            InputStream stream =
                    new ByteArrayInputStream(("{ \"access_token\" : \"abc123\", \"token_type\" : \"Bearer\", " +
                            "\"expires_in\" : 3600 }").getBytes());
            Mockito.when(entity.getContent()).thenReturn(stream);

            org.apache.axis2.context.MessageContext messageContext = new org.apache.axis2.context.MessageContext();
            AxisConfiguration axisConfiguration = new AxisConfiguration();
            TransportOutDescription transportOutDescription = new TransportOutDescription("https");
            transportOutDescription.setSender(new PassThroughHttpSender());
            axisConfiguration.addTransportOut(transportOutDescription);
            ConfigurationContext configurationContext = new ConfigurationContext(axisConfiguration);
            messageContext.setConfigurationContext(configurationContext);
            SynapseConfiguration synapseConfiguration = new SynapseConfiguration();
            SynapseEnvironment synapseEnvironment = new Axis2SynapseEnvironment(synapseConfiguration);
            String token = OAuthClient.generateToken("https://localhost:8280/token1/1.0.0", "body", "credentials",
                    new Axis2MessageContext(messageContext, new SynapseConfiguration(), synapseEnvironment), null, -1, -1,
                    -1, new ProxyConfigs(), new TrustStoreConfigs());

            assertEquals("abc123", token);
        }
    }
}
