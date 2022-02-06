package org.apache.synapse.mediators.opa;

import org.apache.axis2.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.synapse.SynapseException;
import org.apache.synapse.transport.passthru.ServerWorker;
import org.apache.synapse.transport.passthru.SourceRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.TreeMap;

public class OPAUtils {

    private static final Log log = LogFactory.getLog(OPAUtils.class);

    static final String HTTP_VERSION_CONNECTOR = ".";
    private static final String STRICT = "Strict";
    private static final String ALLOW_ALL = "AllowAll";
    private static final String HOST_NAME_VERIFIER = "httpclient.hostnameVerifier";
    private static int maxOpenConnections = 150;
    private static int maxPerRoute = 50;
    private static int socketTimeout = 30;
    private static CloseableHttpClient httpClient = null;
    private static CloseableHttpClient httpsClient = null;



    /**
     * Return a CloseableHttpClient instance
     *
     * @return CloseableHttpClient
     */
    public static CloseableHttpClient createHttpClient() {

        PoolingHttpClientConnectionManager pool = new PoolingHttpClientConnectionManager();
        pool.setMaxTotal(maxOpenConnections);
        pool.setDefaultMaxPerRoute(maxPerRoute);
        RequestConfig params = RequestConfig.custom()
                .setSocketTimeout(socketTimeout * 1000)
                .build();

        return HttpClients.custom().setConnectionManager(pool).setDefaultRequestConfig(params).build();

    }

    public static CloseableHttpClient getClient(String url) throws SynapseException {

        String protocol;
        try {
            protocol = new URL(url).getProtocol();
            if ("https".equals(protocol)) {
                if (httpsClient == null) {
                    //httpsClient = createHttpsClient();
                }
                return httpsClient;
            } else {
                if (httpClient == null) {
                    httpClient = createHttpClient();
                }
                return httpClient;
            }
        } catch (MalformedURLException e) {
            log.error("Error when getting the endpoint protocol", e);
            throw new SynapseException("Internal server error.", e);
        }
    }
}
