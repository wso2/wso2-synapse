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

package org.apache.synapse.endpoints.oauth;

import junit.framework.TestCase;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Test for OAuthClient that is used to generate tokens
 */

@RunWith(PowerMockRunner.class)
public class OAuthClientTest extends TestCase {

    @PrepareForTest(HttpClientBuilder.class)

    /**
     * Tests if the oauth client correctly parse the response and generate a Token object
     *
     * @throws Exception
     */
    @Test
    public void testGenerateToken() throws Exception {

        HttpClientBuilder mockClientBuilder = mock(HttpClientBuilder.class);
        CloseableHttpClient mockHttpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
        HttpEntity entity = mock(HttpEntity.class);
        StatusLine statusLine = mock(StatusLine.class);
        PowerMockito.mockStatic(HttpClientBuilder.class);

        PowerMockito.when(HttpClientBuilder.class, "create").thenReturn(mockClientBuilder);
        PowerMockito.when(mockClientBuilder.build()).thenReturn(mockHttpClient);
        PowerMockito.when(mockHttpClient.execute(any(HttpPost.class))).thenReturn(mockResponse);

        when(statusLine.getStatusCode()).thenReturn(200);
        when(mockResponse.getStatusLine()).thenReturn(statusLine);

        when(mockResponse.getEntity()).thenReturn(entity);

        InputStream stream =
                new ByteArrayInputStream(("{ \"access_token\" : \"abc123\", \"token_type\" : \"Bearer\", " +
                        "\"expires_in\" : 3600 }").getBytes());
        when(entity.getContent()).thenReturn(stream);

        String token = OAuthClient.generateToken("uri", "body", "credentials");

        assertEquals("abc123", token);
    }
}
