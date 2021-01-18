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

package org.apache.synapse.samples.framework.tests.endpoint;

import org.apache.http.HttpStatus;
import org.apache.synapse.samples.framework.SynapseTestCase;
import org.apache.synapse.samples.framework.clients.BasicHttpClient;
import org.apache.synapse.samples.framework.clients.HttpResponse;

public class Sample63 extends SynapseTestCase {

    public Sample63() {

        super(63);
    }

    public void testOAuthConfiguredEPSuccess() throws Exception {

        BasicHttpClient client = new BasicHttpClient();
        HttpResponse response = client.doGet("http://127.0.0.1:8280/foodapi/list/food");
        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals("1", response.getBodyAsString());

        // make a second call to verify whether the cached token is used
        response = client.doGet("http://127.0.0.1:8280/foodapi/list/food");
        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals("1", response.getBodyAsString());

    }

    public void testOAuthConfiguredEPUnAuthorized() throws Exception {

        BasicHttpClient client = new BasicHttpClient();
        HttpResponse response = client.doGet("http://127.0.0.1:8280/foodapi/list/unauthorized");
        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatus());
        assertEquals("2", response.getBodyAsString());
    }

    public void testOAuthConfiguredEPInvalidCredentials() throws Exception {

        BasicHttpClient client = new BasicHttpClient();
        HttpResponse response = client.doGet("http://127.0.0.1:8280/foodapi/list/invalidCredentials");
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
    }

    public void testOAuthConfiguredEPOAuthServerFailed() throws Exception {

        BasicHttpClient client = new BasicHttpClient();
        HttpResponse response = client.doGet("http://127.0.0.1:8280/foodapi/list/oauthServerFailed");
        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
    }

    public void testOAuthConfiguredEPRefreshGrantType() throws Exception {

        BasicHttpClient client = new BasicHttpClient();
        HttpResponse response = client.doGet("http://127.0.0.1:8280/foodapi/list/refreshTokenGrant");
        assertEquals(HttpStatus.SC_OK, response.getStatus());
    }
}
