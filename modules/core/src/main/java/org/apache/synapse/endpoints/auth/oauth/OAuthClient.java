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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.NoConnectionReuseStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.auth.AuthConstants;
import org.apache.synapse.endpoints.auth.AuthException;
import org.apache.synapse.transport.http.conn.SSLContextDetails;
import org.apache.synapse.transport.nhttp.config.ClientConnFactoryBuilder;
import org.apache.synapse.transport.nhttp.util.SecureVaultValueReader;
import org.wso2.securevault.SecretResolver;
import org.wso2.securevault.SecretResolverFactory;

import javax.net.ssl.SSLContext;
import javax.xml.namespace.QName;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
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

    private CloseableHttpClient httpClient;

    public OAuthClient() {
        // preserving the previous behaviour in case there is a scenario to invoke
        // the client without the message context
        httpClient = getDefaultHttpClient();
    }

    /**
     * Method to generate the access token from an OAuth server
     *
     * @param tokenApiUrl The token url of the server
     * @param payload     The payload of the request
     * @param credentials The encoded credentials
     * @return accessToken String
     * @throws AuthException In the event of an unexpected HTTP status code return from the server or access_token
     *                        key missing in the response payload
     * @throws IOException    In the event of a problem parsing the response from the server
     */
    public String generateToken(String tokenApiUrl, String payload, String credentials, MessageContext messageContext)
            throws AuthException, IOException {
        httpClient = getSecureClient(tokenApiUrl, messageContext);
        if (log.isDebugEnabled()) {
            log.debug("Initializing token generation request: [token-endpoint] " + tokenApiUrl);
        }

        HttpPost httpPost = new HttpPost(tokenApiUrl);
        httpPost.setHeader(AuthConstants.CONTENT_TYPE_HEADER, AuthConstants.APPLICATION_X_WWW_FORM_URLENCODED);
        if (credentials != null) {
            httpPost.setHeader(AuthConstants.AUTHORIZATION_HEADER, AuthConstants.BASIC + credentials);
        }
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
    private CloseableHttpClient getSecureClient(String tokenUrl, MessageContext messageContext) throws AuthException {
        SSLContext sslContext;
        ConfigurationContext configurationContext = ((Axis2MessageContext) messageContext).getAxis2MessageContext().
                getConfigurationContext();
        TransportOutDescription transportOut = configurationContext.getAxisConfiguration().getTransportOut("https");
        try {
            ClientConnFactoryBuilder clientConnFactoryBuilder =
                    new ClientConnFactoryBuilder(transportOut, configurationContext).parseSSL();

            sslContext = getSSLContextWithUrl(tokenUrl, clientConnFactoryBuilder.getSslByHostMap(),
                                              clientConnFactoryBuilder.getSsl());
        } catch (AxisFault e) {
            throw new AuthException("Error while reading SSL configs. Using default Keystore and Truststore", e);
        }
        SSLConnectionSocketFactory sslConnectionFactory =
                new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create().
                register(HTTPS_CONNECTION, sslConnectionFactory).register(HTTP_CONNECTION, new PlainConnectionSocketFactory())
                .build();

        BasicHttpClientConnectionManager connManager = new BasicHttpClientConnectionManager(registry);

        CloseableHttpClient client = HttpClients.custom().setConnectionManager(connManager)
                .setSSLSocketFactory(sslConnectionFactory).build();

        return client;
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

    private SSLContext getSSLContextWithUrl(String urlPath, Map<String, SSLContext> sslByHostMap,
                                            SSLContextDetails ssl) {
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
            return null;
        }
    }
}
