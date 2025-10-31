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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Map;

import javax.net.ssl.SSLContext;

/**
 * This class represents the client used to request and retrieve OPA response for a given policy
 * from the OPA server
 */
public class OPAClient {

    private static final Log log = LogFactory.getLog(OPAClient.class);

    private int maxOpenConnections = 500;
    private int maxPerRoute = 200;
    private int connectionTimeout = 30;

    private CloseableHttpClient httpClient = null;
    private static final String PRIMARY_KEY_STORE_TYPE_PROPERTY = "primary.key.type";
    private static final String DEFAULT_KEYSTORE_TYPE ="JKS";
    private static final String SECURITY_JCE_PROVIDER = "security.jce.provider";
    public static final String BCFKS = "BCFKS";

    public OPAClient(String url, Map<String, String> additionalParameters) throws OPASecurityException {

        if (additionalParameters.get(OPAConstants.MAX_OPEN_CONNECTIONS_PARAMETER) != null) {
            this.maxOpenConnections =
                    Integer.parseInt(additionalParameters.get(OPAConstants.MAX_OPEN_CONNECTIONS_PARAMETER));
        }
        if (additionalParameters.get(OPAConstants.MAX_PER_ROUTE_PARAMETER) != null) {
            this.maxPerRoute = Integer.parseInt(additionalParameters.get(OPAConstants.MAX_PER_ROUTE_PARAMETER));
        }
        if (additionalParameters.get(OPAConstants.CONNECTION_TIMEOUT_PARAMETER) != null) {
            this.connectionTimeout =
                    Integer.parseInt(additionalParameters.get(OPAConstants.CONNECTION_TIMEOUT_PARAMETER));
        }
        httpClient = createHttpClient(url);
    }

    /**
     * Method to publish the OPA payload to the OPA server
     *
     * @param opaServerUrl The url of the opa server
     * @param payload      The payload of the request
     * @param credentials  Access key of the opa validation request
     * @return opa response String
     * @throws OPASecurityException
     */
    public String publish(String opaServerUrl, String payload, String credentials)
            throws OPASecurityException {

        if (log.isDebugEnabled()) {
            log.debug("Initializing opa policy validation request: [validation-endpoint] " + opaServerUrl);
        }

        HttpPost httpPost = new HttpPost(opaServerUrl);
        httpPost.setHeader(OPAConstants.CONTENT_TYPE_HEADER, OPAConstants.APPLICATION_JSON);
        if (credentials != null) {
            httpPost.setHeader(OPAConstants.AUTHORIZATION_HEADER, credentials);
        }
        CloseableHttpResponse response = null;
        try {
            httpPost.setEntity(new StringEntity(payload));
            response = httpClient.execute(httpPost);
            return extractResponse(response);
        } catch (IOException e) {
            log.error("Error occurred while publishing to OPA server", e);
            throw new OPASecurityException(OPASecurityException.INTERNAL_ERROR,
                    OPASecurityException.INTERNAL_ERROR_MESSAGE, e);
        } finally {
            httpPost.releaseConnection();
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    log.error("Error when closing the response of the opa request", e);
                }
            }
        }
    }

    /**
     * Extract the opa response string from HTTP response
     *
     * @param response CloseableHttpResponse object
     * @return opaResponse String
     * @throws OPASecurityException
     */
    private String extractResponse(CloseableHttpResponse response)
            throws OPASecurityException {

        String opaResponse = null;
        try {
            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode != HttpStatus.SC_OK) {
                log.error("Error occurred while connecting to the OPA server. " + responseCode + " response returned");
                throw new OPASecurityException(OPASecurityException.INTERNAL_ERROR,
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
            log.error("Error while reading the OPA policy validation response", e);
            throw new OPASecurityException(OPASecurityException.INTERNAL_ERROR,
                    "Error while reading the OPA policy validation response", e);
        }

        return opaResponse;
    }

    /**
     * Return a PoolingHttpClientConnectionManager instance
     *
     * @param protocol- service endpoint protocol. It can be http/https
     * @return PoolManager
     * @throws OPASecurityException
     */
    private PoolingHttpClientConnectionManager getPoolingHttpClientConnectionManager(String protocol)
            throws OPASecurityException {

        PoolingHttpClientConnectionManager poolManager;
        if (OPAConstants.HTTPS.equals(protocol)) {

            char[] trustStorePassword =
                    System.getProperty(OPAConstants.TRUST_STORE_PASSWORD_SYSTEM_PROPERTY).toCharArray();
            String trustStoreLocation = System.getProperty(OPAConstants.TRUST_STORE_LOCATION_SYSTEM_PROPERTY);
            File trustStoreFile = new File(trustStoreLocation);
            try (InputStream localTrustStoreStream = new FileInputStream(trustStoreFile)) {
                KeyStore trustStore = KeyStore.getInstance(getKeyType());
                trustStore.load(localTrustStoreStream, trustStorePassword);
                SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(trustStore).build();

                X509HostnameVerifier hostnameVerifier;
                String hostnameVerifierOption = System.getProperty(OPAConstants.HOST_NAME_VERIFIER);

                if (OPAConstants.ALLOW_ALL.equalsIgnoreCase(hostnameVerifierOption)) {
                    hostnameVerifier = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
                } else if (OPAConstants.STRICT.equalsIgnoreCase(hostnameVerifierOption)) {
                    hostnameVerifier = SSLConnectionSocketFactory.STRICT_HOSTNAME_VERIFIER;
                } else {
                    hostnameVerifier = SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;
                }

                SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
                Registry<ConnectionSocketFactory> socketFactoryRegistry =
                        RegistryBuilder.<ConnectionSocketFactory>create()
                                .register(OPAConstants.HTTPS, sslsf).build();
                poolManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            } catch (IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException | KeyManagementException e) {
                log.error("Error while reading and setting truststore", e);
                throw new OPASecurityException(OPASecurityException.INTERNAL_ERROR,
                        "Error while reading and setting truststore", e);
            }
        } else {
            poolManager = new PoolingHttpClientConnectionManager();
        }
        return poolManager;
    }

    /**
     * Return a CloseableHttpClient instance
     *
     * @param url Service endpoint.It can be http/https
     * @return CloseableHttpClient
     * @throws OPASecurityException
     **/
    public CloseableHttpClient createHttpClient(String url) throws OPASecurityException {

        PoolingHttpClientConnectionManager pool;
        try {
            String protocol = new URL(url).getProtocol();
            pool = getPoolingHttpClientConnectionManager(protocol);
        } catch (OPASecurityException | MalformedURLException e) {
            log.error("Error while creating the http client", e);
            throw new OPASecurityException(OPASecurityException.INTERNAL_ERROR,
                    OPASecurityException.INTERNAL_ERROR_MESSAGE, e);
        }

        pool.setMaxTotal(maxOpenConnections);
        pool.setDefaultMaxPerRoute(maxPerRoute);

        //Socket timeout is set to 10 seconds addition to connection timeout.
        RequestConfig params = RequestConfig.custom()
                .setConnectTimeout(connectionTimeout * 1000)
                .setSocketTimeout((connectionTimeout + 10) * 10000).build();

        return HttpClients.custom().setConnectionManager(pool).setDefaultRequestConfig(params).build();
    }

    private static String getKeyType() {
        String keyType = System.getProperty(PRIMARY_KEY_STORE_TYPE_PROPERTY);
        if (System.getProperty(SECURITY_JCE_PROVIDER) != null) {
            return StringUtils.isNotEmpty(keyType) ? keyType : BCFKS;
        } else {
            return StringUtils.isNotEmpty(keyType) ? keyType : DEFAULT_KEYSTORE_TYPE;
        }
    }
}
