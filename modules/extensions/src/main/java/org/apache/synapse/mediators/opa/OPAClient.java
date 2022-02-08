/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.mediators.opa;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.synapse.endpoints.oauth.OAuthException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * This class represents the client used to request and retrieve OPA response for a given policy
 * from the OPA server
 */
public class OPAClient {

    private static final Log log = LogFactory.getLog(OPAClient.class);

    private static final CloseableHttpClient httpClient = createHTTPClient();
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BASIC = "Basic ";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";

    /**
     * Method to publish the OPA payload to the OPA server
     *
     * @param opaServerUrl The token url of the server
     * @param payload     The payload of the request
     * @param credentials The encoded credentials
     * @return accessToken String
     * @throws OAuthException In the event of an unexpected HTTP status code return from the server or access_token
     *                        key missing in the response payload
     * @throws IOException    In the event of a problem parsing the response from the server
     */
    public static String publish(String opaServerUrl, String payload, String credentials)
            throws OPASecurityException {

        if (log.isDebugEnabled()) {
            log.debug("Initializing opa policy validation request: [validation-endpoint] " + opaServerUrl);
        }

        HttpPost httpPost = new HttpPost(opaServerUrl);
        httpPost.setHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON);
        if (credentials != null) {
            httpPost.setHeader(AUTHORIZATION_HEADER, credentials);
        }
        try {
            httpPost.setEntity(new StringEntity(payload));

            CloseableHttpResponse response = httpClient.execute(httpPost);
            return extractResponse(response);

        } catch (IOException e){
            throw new OPASecurityException(OPASecurityException.OPA_REQUEST_ERROR,
                    "Error occurred while publishing to OPA server", e);
        } finally {
            httpPost.releaseConnection();
        }
    }

    /**
     * Method to retrieve the token response sent from the server
     *
     * @param response CloseableHttpResponse object
     * @return accessToken String
     * @throws OAuthException In the event of an unexpected HTTP status code return from the server or access_token
     *                        key missing in the response payload
     * @throws IOException    In the event of a problem parsing the response from the server
     */
    private static String extractResponse(CloseableHttpResponse response)
            throws OPASecurityException {

        String opaResponse = null;
        try {
            int responseCode = response.getStatusLine().getStatusCode();

            if (responseCode != HttpStatus.SC_OK) {
                throw new OPASecurityException(OPASecurityException.OPA_RESPONSE_ERROR,
                        "Error while accessing the OPA server URL. " + response.getStatusLine());
            } else {
                HttpEntity entity = response.getEntity();
                Charset charset = ContentType.getOrDefault(entity).getCharset();
                if (charset == null) {
                    charset = StandardCharsets.UTF_8;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent(), charset));
                String inputLine;
                StringBuilder stringBuilder = new StringBuilder();

                while ((inputLine = reader.readLine()) != null) {
                    stringBuilder.append(inputLine);
                }
                opaResponse = stringBuilder.toString();
                if (log.isDebugEnabled()) {
                    log.debug("Response: [status-code] " + responseCode + " [message] " + opaResponse);
                }
            }
        } catch (IOException e) {
            throw new OPASecurityException(OPASecurityException.OPA_RESPONSE_ERROR,
                    "Error while reading the OPA policy validation response", e);
        }

        return opaResponse;
    }

    /**
     * Creates a CloseableHttpClient with NoConnectionReuseStrategy
     *
     * @return httpClient CloseableHttpClient
     */
    private static CloseableHttpClient createHTTPClient() {

        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setConnectionReuseStrategy(new NoConnectionReuseStrategy());
        return builder.build();
    }
}
