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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.UnsupportedSchemeException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.TrustStoreConfigs;
import org.apache.synapse.endpoints.ProxyConfigs;
import org.apache.synapse.endpoints.auth.AuthConstants;
import org.apache.synapse.endpoints.auth.AuthException;
import org.apache.synapse.transport.http.conn.RequestDescriptor;
import org.apache.synapse.transport.http.conn.SSLContextDetails;
import org.apache.synapse.transport.nhttp.config.ClientConnFactoryBuilder;
import org.wso2.securevault.commons.MiscellaneousUtil;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Map;

/**
 * This class represents the client used to request and retrieve OAuth tokens
 * from an OAuth server
 */
public class OAuthClient {

    private static final Log log = LogFactory.getLog(OAuthClient.class);

    // Elements defined in key/trust store configuration
    private static final String HTTP_CONNECTION = "http";
    private static final String HTTPS_CONNECTION = "https";
    private static final String ALL_HOSTS = "*";
    public static final String HOST_NAME_VERIFIER = "httpclient.hostnameVerifier";
    public static final String ALLOW_ALL = "AllowAll";
    public static final String STRICT = "Strict";
    public static final String DEFAULT_AND_LOCALHOST = "DefaultAndLocalhost";
    public static final int MAX_TOTAL_POOL_SIZE = 100;
    public static final int DEFAULT_MAX_PER_ROUTE = 50;

    /**
     * Method to generate the access token from an OAuth server
     *
     * @param tokenApiUrl  The token url of the server
     * @param payload      The payload of the request
     * @param credentials  The encoded credentials
     * @param proxyConfigs The proxy configurations
     * @return accessToken String
     * @throws AuthException In the event of an unexpected HTTP status code return from the server or access_token key
     *                       missing in the response payload
     * @throws IOException   In the event of a problem parsing the response from the server
     * @deprecated Use method with TrustStoreConfigs parameter for enhanced SSL configuration support
     */
    @Deprecated
    public static String generateToken(String tokenApiUrl, String payload, String credentials,
                                       MessageContext messageContext, Map<String, String> customHeaders,
                                       int connectionTimeout, int connectionRequestTimeout, int socketTimeout,
                                       ProxyConfigs proxyConfigs) throws AuthException, IOException {
        // Use null TrustStoreConfigs to maintain backward compatibility
        return generateToken(tokenApiUrl, payload, credentials, messageContext, customHeaders,
                connectionTimeout, connectionRequestTimeout, socketTimeout, proxyConfigs, null);
    }

    /**
     * Method to generate the access token from an OAuth server
     *
     * @param tokenApiUrl       The token url of the server
     * @param payload           The payload of the request
     * @param credentials       The encoded credentials
     * @param proxyConfigs      The proxy configurations
     * @param trustStoreConfigs The trust store configurations
     * @return accessToken String
     * @throws AuthException In the event of an unexpected HTTP status code return from the server or access_token key
     *                       missing in the response payload
     * @throws IOException   In the event of a problem parsing the response from the server
     */
    public static String generateToken(String tokenApiUrl, String payload, String credentials,
                                       MessageContext messageContext, Map<String, String> customHeaders,
                                       int connectionTimeout, int connectionRequestTimeout, int socketTimeout,
                                       ProxyConfigs proxyConfigs, TrustStoreConfigs trustStoreConfigs)
            throws AuthException, IOException {

        if (log.isDebugEnabled()) {
            log.debug("Initializing token generation request: [token-endpoint] " + tokenApiUrl);
        }

        try (CloseableHttpClient httpClient = OAuthUtils.getSecureClient(tokenApiUrl, messageContext, connectionTimeout,
                connectionRequestTimeout, socketTimeout, proxyConfigs, trustStoreConfigs)) {
            HttpPost httpPost = new HttpPost(tokenApiUrl);
            httpPost.setHeader(AuthConstants.CONTENT_TYPE_HEADER, AuthConstants.APPLICATION_X_WWW_FORM_URLENCODED);
            if (!(customHeaders == null || customHeaders.isEmpty())) {
                for (Map.Entry<String, String> entry : customHeaders.entrySet()) {
                    httpPost.setHeader(entry.getKey(), entry.getValue());
                }
            }
            if (credentials != null) {
                httpPost.setHeader(AuthConstants.AUTHORIZATION_HEADER, AuthConstants.BASIC + credentials);
            }
            httpPost.setEntity(new StringEntity(payload));

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                return extractToken(response);
            } catch (SocketTimeoutException e) {
                throw new AuthException("Socket timeout: OAuth token endpoint did not respond within the expected " +
                        "time frame.");
            } catch (ConnectionPoolTimeoutException e) {
                throw new AuthException("Connection request timeout: Unable to obtain a connection from the pool in " +
                        "time while communicating with the OAuth token endpoint.");
            } catch (ConnectTimeoutException e) {
                throw new AuthException("Connection timeout: Failed to establish a connection to the OAuth token " +
                        "endpoint.");
            } catch (UnknownHostException e) {
                throw new AuthException("Unable to resolve the hostname for the OAuth token endpoint connection.");
            } catch (HttpHostConnectException e) {
                throw new AuthException("Unable to connect to the OAuth token endpoint.");
            } catch (UnsupportedSchemeException e) {
                throw new AuthException("Unsupported protocol used for the OAuth token endpoint.");
            } catch (SSLHandshakeException e) {
                throw new AuthException("SSL handshake failed while connecting to the OAuth token endpoint.");
            } finally {
                httpPost.releaseConnection();
            }
        }
    }

    /**
     * Method to retrieve the token response sent from the server
     *
     * @param response CloseableHttpResponse object
     * @return accessToken String
     * @throws AuthException In the event of an unexpected HTTP status code return from the server or access_token
     *                        key missing in the response payload
     * @throws IOException    In the event of a problem parsing the response from the server
     */
    private static String extractToken(CloseableHttpResponse response) throws AuthException, IOException {

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
            throw new AuthException("Error while accessing the Token URL. "
                    + response.getStatusLine());
        }

        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = (JsonObject) parser.parse(stringBuilder.toString());
        if (jsonResponse.has(AuthConstants.ACCESS_TOKEN)) {
            return jsonResponse.get(AuthConstants.ACCESS_TOKEN).getAsString();
        }
        throw new AuthException("Missing key [access_token] in the response from the OAuth server");
    }

}
