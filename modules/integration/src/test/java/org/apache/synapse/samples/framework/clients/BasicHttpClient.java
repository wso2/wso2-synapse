/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.samples.framework.clients;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.Map;

/**
 * A simple HTTP client that enables making HTTP requests. Useful for testing
 * RESTful invocations on Synapse.
 */
public class BasicHttpClient {

    /**
     * Make a HTTP GET request on the specified URL.
     *
     * @param url A valid HTTP URL
     * @return A HttpResponse object
     * @throws Exception If an error occurs while making the HTTP call
     */
    public HttpResponse doGet(String url) throws Exception {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        try {
            HttpGet get = new HttpGet(url);
            return new HttpResponse(client.execute(get));
        } finally {
            client.close();
        }
    }

    /**
     * Make a HTTP OPTIONS request on the specified URL.
     *
     * @param url A valid HTTP URL
     * @return A HttpResponse object
     * @throws Exception If an error occurs while making the HTTP call
     */
    public HttpResponse doOptions(String url) throws Exception {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        try {
            HttpOptions options = new HttpOptions(url);
            return new HttpResponse(client.execute(options));
        } finally {
            client.close();
        }
    }

    /**
     * Make a HTTP POST request on the specified URL.
     *
     * @param url A valid HTTP URL
     * @param payload An array of bytes to be posted to the URL (message body)
     * @param contentType Content type of the message body
     * @return A HttpResponse object
     * @throws Exception If an error occurs while making the HTTP call
     */
    public HttpResponse doPost(String url, byte[] payload, String contentType) throws Exception {
        return doPost(url, payload, contentType, null);
    }

    /**
     * Make a HTTP POST request on the specified URL.
     *
     * @param url A valid HTTP URL
     * @param payload An array of bytes to be posted to the URL (message body)
     * @param contentType Content type of the message body
     * @param headers A map of HTTP headers to be set on the outgoing request
     * @return A HttpResponse object
     * @throws Exception If an error occurs while making the HTTP call
     */
    public HttpResponse doPost(String url, byte[] payload,
                               String contentType, Map<String,String> headers) throws Exception {
        CloseableHttpClient client = HttpClientBuilder.create().build();
        try {
            HttpPost post = new HttpPost(url);
            if (headers != null) {
                for (Map.Entry<String,String> entry : headers.entrySet()) {
                    post.setHeader(entry.getKey(), entry.getValue());
                }
            }
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContentType(contentType);
            entity.setContent(new ByteArrayInputStream(payload));
            post.setEntity(entity);
            return new HttpResponse(client.execute(post));
        } finally {
            client.close();
        }
    }

    public HttpResponse doPostWithCert(String url, byte[] payload,
                               String contentType, String keyStorePath,
                                       String trustStorePath, String keyStorePass, String trustStorePass)
            throws Exception {

        KeyStore testKeyStore = KeyStore.getInstance("JKS");
        testKeyStore.load(Files.newInputStream(Paths.get(keyStorePath)), keyStorePass.toCharArray());

        KeyStore testTrustStore = KeyStore.getInstance("JKS");
        testTrustStore.load(Files.newInputStream(Paths.get(trustStorePath)), trustStorePass.toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(testKeyStore, keyStorePass.toCharArray());

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(testTrustStore);

        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

        try (CloseableHttpClient client = HttpClientBuilder.create().setSSLContext(sslContext).build()) {
            HttpPost post = new HttpPost(url);
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContentType(contentType);
            entity.setContent(new ByteArrayInputStream(payload));
            post.setEntity(entity);
            return new HttpResponse(client.execute(post));
        }
    }

}
