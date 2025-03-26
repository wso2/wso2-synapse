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
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
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
import org.apache.synapse.endpoints.ProxyConfigs;
import org.apache.synapse.endpoints.auth.AuthConstants;
import org.apache.synapse.endpoints.auth.AuthException;
import org.apache.synapse.transport.http.conn.SSLContextDetails;
import org.apache.synapse.transport.nhttp.config.ClientConnFactoryBuilder;
import org.apache.synapse.transport.nhttp.util.SecureVaultValueReader;
import org.apache.synapse.util.xpath.KeyStoreManager;
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecretResolverFactory;
import org.wso2.securevault.commons.MiscellaneousUtil;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.xml.namespace.QName;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.KeyStoreException;
import java.util.Arrays;
import java.util.Map;

/**
 * This class represents the client used to request and retrieve OAuth tokens
 * from an OAuth server
 */
public class OAuthClient {

    private static final Log log = LogFactory.getLog(OAuthClient.class);

    // Elements defined in key/trust store configuration
    private static final String STORE_TYPE = "Type";
    private static final String STORE_LOCATION = "Location";
    private static final String STORE_PASSWORD = "Password";
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
     * @param tokenApiUrl   The token url of the server
     * @param payload       The payload of the request
     * @param credentials   The encoded credentials
     * @param proxyConfigs  The proxy configurations
     * @return accessToken String
     * @throws AuthException In the event of an unexpected HTTP status code return from the server or access_token key
     *                       missing in the response payload
     * @throws IOException   In the event of a problem parsing the response from the server
     */
    public static String generateToken(String tokenApiUrl, String payload, String credentials,
                                       MessageContext messageContext, Map<String, String> customHeaders,
                                       int connectionTimeout, int connectionRequestTimeout, int socketTimeout,  ProxyConfigs proxyConfigs) throws AuthException, IOException {

        if (log.isDebugEnabled()) {
            log.debug("Initializing token generation request: [token-endpoint] " + tokenApiUrl);
        }

        try (CloseableHttpClient httpClient = getSecureClient(tokenApiUrl, messageContext, connectionTimeout,
                connectionRequestTimeout, socketTimeout, proxyConfigs)) {
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

    /**
     * Initializes a Secure HTTP client fot token endpoint.
     *
     * @return Secure CloseableHttpClient
     * @throws AuthException
     */
    private static CloseableHttpClient getSecureClient(String tokenUrl, MessageContext messageContext,
            int connectionTimeout, int connectionRequestTimeout, int socketTimeout, ProxyConfigs proxyConfigs)
            throws AuthException {

        if (proxyConfigs.isProxyEnabled()) {
            return getSecureClientWithProxy(messageContext, connectionTimeout, connectionRequestTimeout, socketTimeout,
                    proxyConfigs);
        } else {
            return getSecureClientWithoutProxy(tokenUrl, messageContext, connectionTimeout, connectionRequestTimeout,
                    socketTimeout);
        }
    }

    private static CloseableHttpClient getSecureClientWithoutProxy(String tokenUrl, MessageContext messageContext,
            int connectionTimeout, int connectionRequestTimeout, int socketTimeout) throws AuthException {
        SSLContext sslContext;
        ConfigurationContext configurationContext = ((Axis2MessageContext) messageContext).getAxis2MessageContext()
                .getConfigurationContext();
        TransportOutDescription transportOut = configurationContext.getAxisConfiguration().getTransportOut("https");
        try {
            ClientConnFactoryBuilder clientConnFactoryBuilder = new ClientConnFactoryBuilder(transportOut,
                    configurationContext).parseSSL();
            sslContext = getSSLContextWithUrl(tokenUrl, clientConnFactoryBuilder.getSslByHostMap(),
                    clientConnFactoryBuilder.getSSLContextDetails());
        } catch (AxisFault e) {
            throw new AuthException("Error while reading SSL configs. Using default Keystore and Truststore", e);
        }
        SSLConnectionSocketFactory sslConnectionFactory = new SSLConnectionSocketFactory(sslContext,
                NoopHostnameVerifier.INSTANCE);
        RequestConfig config = RequestConfig.custom().setConnectTimeout(connectionTimeout)
                .setConnectionRequestTimeout(connectionRequestTimeout).setSocketTimeout(socketTimeout).build();
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory> create()
                .register(HTTPS_CONNECTION, sslConnectionFactory)
                .register(HTTP_CONNECTION, new PlainConnectionSocketFactory()).build();

        BasicHttpClientConnectionManager connManager = new BasicHttpClientConnectionManager(registry);
        CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config)
                .setConnectionManager(connManager).setSSLSocketFactory(sslConnectionFactory).build();

        return client;
    }

    private static CloseableHttpClient getSecureClientWithProxy(MessageContext messageContext, int connectionTimeout,
                                                                int connectionRequestTimeout, int socketTimeout,
                                                                ProxyConfigs proxyConfigs) throws AuthException {

        PoolingHttpClientConnectionManager pool = getPoolingHttpClientConnectionManager(
                proxyConfigs.getProxyProtocol());
        pool.setMaxTotal(MAX_TOTAL_POOL_SIZE);
        pool.setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE);

        RequestConfig params = RequestConfig.custom().build();
        HttpClientBuilder clientBuilder = HttpClients.custom().setConnectionManager(pool)
                .setDefaultRequestConfig(params);

        HttpHost host = new HttpHost(proxyConfigs.getProxyHost(), Integer.parseInt(proxyConfigs.getProxyPort()),
                proxyConfigs.getProxyProtocol());
        DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(host);
        RequestConfig config = RequestConfig.custom().setConnectTimeout(connectionTimeout)
                .setConnectionRequestTimeout(connectionRequestTimeout).setSocketTimeout(socketTimeout).build();
        clientBuilder = clientBuilder.setRoutePlanner(routePlanner).setDefaultRequestConfig(config);

        if (StringUtils.isNotBlank(proxyConfigs.getProxyUsername()) && StringUtils.isNotBlank(
                proxyConfigs.getProxyPassword())) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    new AuthScope(proxyConfigs.getProxyHost(), Integer.parseInt(proxyConfigs.getProxyPort())),
                    new UsernamePasswordCredentials(proxyConfigs.getProxyUsername(), resolveProxyPassword(proxyConfigs,
                            messageContext)));
            clientBuilder = clientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }

        return clientBuilder.build();
    }

    private static String resolveProxyPassword(ProxyConfigs proxyConfigs, MessageContext messageContext)
            throws AuthException {
        if (proxyConfigs.getProxyPasswordSecretResolver() != null) {
            // Resolves the password when global proxy configurations are used
            return MiscellaneousUtil.resolve(proxyConfigs.getProxyPassword(),
                    proxyConfigs.getProxyPasswordSecretResolver());
        } else {
            // Resolves the password when endpoint specific proxy configurations are used
            return OAuthUtils.resolveExpression(proxyConfigs.getProxyPassword(), messageContext);
        }
    }

    private static PoolingHttpClientConnectionManager getPoolingHttpClientConnectionManager(String protocol)
            throws AuthException {

        PoolingHttpClientConnectionManager poolManager;
        if (AuthConstants.HTTPS_PROTOCOL.equals(protocol)) {
            SSLConnectionSocketFactory socketFactory = createSocketFactory();
            org.apache.http.config.Registry<ConnectionSocketFactory> socketFactoryRegistry =
                    RegistryBuilder.<ConnectionSocketFactory> create()
                    .register(AuthConstants.HTTPS_PROTOCOL, socketFactory).build();
            poolManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        } else {
            poolManager = new PoolingHttpClientConnectionManager();
        }
        return poolManager;
    }

    private static SSLConnectionSocketFactory createSocketFactory() throws AuthException {
        SSLContext sslContext;

        char[] trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword").toCharArray();
        String trustStoreLocation = System.getProperty("javax.net.ssl.trustStore");
        try {
            KeyStore trustStore = KeyStoreManager.getKeyStore(trustStoreLocation, Arrays.toString(trustStorePassword),
                    "JKS");
            sslContext = SSLContexts.custom().loadTrustMaterial(trustStore).build();

            HostnameVerifier hostnameVerifier;
            String hostnameVerifierOption = System.getProperty(HOST_NAME_VERIFIER);

            if (ALLOW_ALL.equalsIgnoreCase(hostnameVerifierOption)) {
                hostnameVerifier = SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
            } else if (STRICT.equalsIgnoreCase(hostnameVerifierOption)) {
                hostnameVerifier = SSLSocketFactory.STRICT_HOSTNAME_VERIFIER;
            } else if (DEFAULT_AND_LOCALHOST.equalsIgnoreCase(hostnameVerifierOption)) {
                hostnameVerifier = new HostnameVerifier() {
                    final String[] localhosts = { "::1", "127.0.0.1", "localhost", "localhost.localdomain" };

                    @Override
                    public boolean verify(String urlHostName, SSLSession session) {
                        return SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER.verify(urlHostName,
                                session) || Arrays.asList(localhosts).contains(urlHostName);
                    }
                };
            } else {
                hostnameVerifier = SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;
            }

            return new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
        } catch (NoSuchAlgorithmException e) {
            throw new AuthException(e);
        } catch (KeyStoreException e) {
            throw new AuthException(e);
        } catch (KeyManagementException e) {
            throw new AuthException(e);
        }
    }

    /**
     * Returns SSL Context for the token endpoint.
     *
     * @return SSLContext
     * @throws AuthException
     */
    private SSLContext getSSLContext(MessageContext messageContext) throws AuthException {
        ConfigurationContext configurationContext = ((Axis2MessageContext) messageContext).getAxis2MessageContext().
                getConfigurationContext();

        Parameter keystoreParameter = configurationContext.getAxisConfiguration().getTransportOut("https").
                getParameter("keystore");

        Parameter truststoreParameter = configurationContext.getAxisConfiguration().getTransportOut("https").
                getParameter("truststore");

        OMElement keystoreElem, truststoreElem;
        if (keystoreParameter != null && truststoreParameter != null) {
            keystoreElem = keystoreParameter.getParameterElement().getFirstElement();
            truststoreElem = truststoreParameter.getParameterElement().getFirstElement();
        } else {
            throw new AuthException("Key Store and/or Trust Store parameters missing in Axis2 configuration");
        }

        SecretResolver resolver;
        if (configurationContext != null && configurationContext.getAxisConfiguration() != null) {
            resolver = configurationContext.getAxisConfiguration().getSecretResolver();
        } else {
            resolver = SecretResolverFactory.create(keystoreElem, false);
        }

        String keystorePassword = SecureVaultValueReader.getSecureVaultValue(resolver,
                keystoreElem.getFirstChildWithName(new QName(STORE_PASSWORD)));

        KeyStore keyStore = getStore(keystoreElem, resolver);
        KeyStore truststore = getStore(truststoreElem, resolver);
        try {
            return SSLContexts.custom().loadKeyMaterial(keyStore, keystorePassword.toCharArray()).
                    loadTrustMaterial(truststore).build();
        } catch (GeneralSecurityException e) {
            throw new AuthException(e);
        }
    }

    /**
     * Returns Trust Store for the provided Axis2 Configuration.
     *
     * @param storeElem OMElement
     * @param resolver Secret Resolver
     * @return Trust tStore
     * @throws AuthException
     */
    private KeyStore getStore(OMElement storeElem, SecretResolver resolver) throws AuthException {
        OMElement storeLocationElement = storeElem.getFirstChildWithName(new QName(STORE_LOCATION));
        OMElement typeElement = storeElem.getFirstChildWithName(new QName(STORE_TYPE));
        String storePassword = SecureVaultValueReader.getSecureVaultValue(resolver,
                storeElem.getFirstChildWithName(new QName(STORE_PASSWORD)));

        if (storeLocationElement == null || typeElement == null || storePassword == null) {
            throw new AuthException("Missing parameters in the store");
        }

        String storeLocation = storeLocationElement.getText();
        String type = typeElement.getText();

        try (FileInputStream fis = new FileInputStream(storeLocation)) {
            KeyStore trustStore = KeyStore.getInstance(type);
            trustStore.load(fis, storePassword.toCharArray());
            return trustStore;
        } catch (GeneralSecurityException gse) {
            throw new AuthException("Error loading Trust/Key store : " + storeLocation);
        } catch (IOException ioe) {
            throw new AuthException("Error opening Trust/Key store : " + storeLocation);
        }
    }

    public CloseableHttpClient getDefaultHttpClient() {
        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setConnectionReuseStrategy(new NoConnectionReuseStrategy());
        return builder.build();
    }

    private static SSLContext getSSLContextWithUrl(String urlPath, Map<String, SSLContext> sslByHostMap,
                                                   SSLContextDetails ssl) throws AuthException {
        try {
            URL url = new URL(urlPath);
            SSLContext customContext = null;
            if (sslByHostMap != null) {
                String host = url.getHost() + ":" + url.getPort();
                // See if there's a custom SSL profile configured for this server
                customContext = sslByHostMap.get(host);
                if (customContext == null) {
                    customContext = sslByHostMap.get(ALL_HOSTS);
                }
            }
            if (customContext != null) {
                return customContext;
            } else {
                return ssl != null ? ssl.getContext() : null;
            }
        } catch (MalformedURLException e) {
            throw new AuthException("OAuth token URL is invalid", e);
        }
    }
}
