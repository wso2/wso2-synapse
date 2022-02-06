package org.apache.synapse.mediators.opa;

import org.apache.axis2.util.JavaUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.mediators.AbstractMediator;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

public class OPAMediator extends AbstractMediator {

    private static final Log log = LogFactory.getLog(OPAMediator.class);
    public static final String HTTP_METHOD_STRING = "HTTP_METHOD";
    public static final String API_BASEPATH_STRING = "TransportInURL";
    public static final int SERVER_RESPONSE_CODE_SUCCESS = 200;
    public static final int SERVER_RESPONSE_BAD_REQUEST = 400;
    public static final int SERVER_RESPONSE_SERVER_ERROR = 500;

    private String opaServerUrl = null;
    private String opaToken = null;
    private String requestGeneratorClass = "org.apache.synapse.mediators.opa.OPASynapseRequestGenerator";
    private Map<String, Object> advancedProperties = new HashMap<String, Object>();


    public void init(){
    }

    @Override
    public boolean mediate(MessageContext messageContext) {

        String requestGeneratorClass = this.getRequestGeneratorClass();
        OPARequestGenerator requestGenerator = null;
        boolean opaResponse = false;
        String opaResponseString = null;
        try {
            Class<?> requestGeneratorClassObject = Class.forName(requestGeneratorClass);
            Constructor<?> constructor = requestGeneratorClassObject.getConstructor();
            requestGenerator = (OPARequestGenerator) constructor.newInstance();

            String opaPayload = requestGenerator.createRequest(messageContext, this.getAdvancedProperties());
            opaResponseString = publish(opaPayload,  this.getOpaServerUrl(), this.getOpaToken());
            opaResponse = requestGenerator.handleResponse(messageContext, opaResponseString);
        } catch (NoSuchMethodException | ClassNotFoundException | InstantiationException | IllegalAccessException
                | InvocationTargetException e) {
            e.printStackTrace();
        }
        return opaResponse;
    }

    public String publish(String opaPayload, String url, String token) throws SynapseException {

        HttpPost postRequest = new HttpPost(url);
        postRequest.addHeader("Authorization", token);
        postRequest.addHeader("Content-type", "application/json");
        CloseableHttpResponse response = null;
        String responseString = null;
        try {
            postRequest.setEntity(new StringEntity(opaPayload));

            if (log.isDebugEnabled()) {
                log.debug("Sending POST to " + url);
            }

            CloseableHttpClient httpClient = OPAUtils.getClient(url);
            if (httpClient != null) {
                long publishingStartTime = System.nanoTime();
                response = httpClient.execute(postRequest);
                long publishingEndTime = System.nanoTime();

                if (log.isDebugEnabled()) {
                    log.debug("Time taken to communicate with OPA server:" + (publishingEndTime - publishingStartTime));
                }

                if (response != null) {
                    int serverResponseCode = response.getStatusLine().getStatusCode();
                    switch (serverResponseCode) {
                        case SERVER_RESPONSE_BAD_REQUEST:
                            log.error("Incorrect JSON format sent for the server from the request ");
                            break;
                        case SERVER_RESPONSE_SERVER_ERROR:
                            if (log.isDebugEnabled()) {
                                log.debug("OPA Server error code sent for the request ");
                            }
                            break;
                        case SERVER_RESPONSE_CODE_SUCCESS:
                            HttpEntity entity = response.getEntity();
                            responseString = EntityUtils.toString(entity, "UTF-8");
                            if (log.isDebugEnabled()) {
                                log.debug("OPA Server Response for for the request " + " was " + responseString);
                            }
                            break;
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("OPA Server connection time for the request in nano seconds is "
                                + (publishingEndTime - publishingStartTime));
                    }
                } else {
                    throw new SynapseException("Null response returned from OPA server for the request");
                }
            } else {
                throw new SynapseException("Internal server error. Cannot find a http client");
            }
        } catch (SocketTimeoutException e) {
            log.error("Connection timed out. Socket Timeout");
            throw new SynapseException("Internal server error. OPA server takes more time than expected", e);
        } catch (IOException e) {
            log.error("Error occurred while sending POST request to OPA endpoint.", e);
            throw new SynapseException("Internal server error. Unable to send request to the OPA server", e);
        } finally {
            if (response != null) {
                try {
                    EntityUtils.consumeQuietly(response.getEntity());
                    response.close();
                } catch (IOException e) {
                    log.error("Error occurred when closing the response of the post request", e);
                }
            }
        }
        return responseString;

    }

    public String getOpaServerUrl() {

        return opaServerUrl;
    }

    public void setOpaServerUrl(String opaServerUrl) {

        this.opaServerUrl = opaServerUrl;
    }

    public String getOpaToken() {

        return opaToken;
    }

    public void setOpaToken(String opaToken) {

        this.opaToken = opaToken;
    }

    public String getRequestGeneratorClass() {

        return requestGeneratorClass;
    }

    public void setRequestGeneratorClass(String requestGeneratorClass) {

        this.requestGeneratorClass = requestGeneratorClass;
    }

    public Map<String, Object> getAdvancedProperties() {

        return advancedProperties;
    }

    public void setAdvancedProperties(Map<String, Object> advancedProperties) {

        this.advancedProperties = advancedProperties;
    }

    public void addAdvancedProperty(String parameter, Object value) {

        this.advancedProperties.put(parameter, value);
    }
}
