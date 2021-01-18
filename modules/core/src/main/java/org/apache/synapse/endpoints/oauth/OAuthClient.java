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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * This class represents the client used to request and retrieve OAuth tokens
 * from an OAuth server
 */
public class OAuthClient {

    private static final Log log = LogFactory.getLog(OAuthClient.class);

    private static final CloseableHttpClient httpClient = HttpClients.createDefault();

    /**
     * Method to generate the access token from an OAuth server
     *
     * @param tokenApiUrl The token url of the server
     * @param payload     The payload of the request
     * @param credentials The encoded credentials
     * @return accessToken String
     * @throws OAuthException In the event of an unexpected HTTP status code return from the server or access_token
     *                        key missing in the response payload
     * @throws IOException    In the event of a problem parsing the response from the server
     */
    public static String generateToken(String tokenApiUrl, String payload, String credentials)
            throws OAuthException, IOException {

        if (log.isDebugEnabled()) {
            log.debug("Initializing token generation request: [token-endpoint] " + tokenApiUrl);
        }

        HttpPost httpPost = new HttpPost(tokenApiUrl);
        httpPost.setHeader(OAuthConstants.CONTENT_TYPE_HEADER, OAuthConstants.APPLICATION_X_WWW_FORM_URLENCODED);
        httpPost.setHeader(OAuthConstants.AUTHORIZATION_HEADER, OAuthConstants.BASIC + credentials);
        httpPost.setEntity(new StringEntity(payload));

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            return extractToken(response);
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
    private static String extractToken(CloseableHttpResponse response) throws OAuthException, IOException {

        int responseCode = response.getStatusLine().getStatusCode();

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

        if (log.isDebugEnabled()) {
            log.debug("Response: [status-code] " + responseCode + " [message] "
                    + stringBuilder.toString());
        }

        if (responseCode != HttpStatus.SC_OK) {
            throw new OAuthException("Error while accessing the Token URL. "
                    + response.getStatusLine());
        }

        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = (JsonObject) parser.parse(stringBuilder.toString());
        if (jsonResponse.has(OAuthConstants.ACCESS_TOKEN)) {
            return jsonResponse.get(OAuthConstants.ACCESS_TOKEN).getAsString();
        }
        throw new OAuthException("Missing key [access_token] in the response from the OAuth server");
    }
}
