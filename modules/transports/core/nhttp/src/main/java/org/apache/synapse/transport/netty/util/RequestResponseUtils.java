/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.synapse.transport.netty.util;

import io.netty.util.AttributeKey;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAP11Constants;
import org.apache.axiom.soap.SOAP12Constants;
import org.apache.axiom.util.UIDGenerator;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.java2wsdl.Java2WSDLConstants;
import org.apache.axis2.transport.RequestResponseTransport;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.transport.base.BaseConstants;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.netty.BridgeConstants;
import org.apache.synapse.transport.netty.config.BaseConfiguration;
import org.apache.synapse.transport.netty.config.NettyConfiguration;
import org.apache.synapse.transport.netty.config.SourceConfiguration;
import org.apache.synapse.transport.nhttp.HttpCoreRequestResponseTransport;
import org.apache.synapse.transport.nhttp.util.SecureVaultValueReader;
import org.apache.synapse.transport.util.HttpMessageHandler;
import org.wso2.securevault.SecretResolver;
import org.wso2.transport.http.netty.contract.config.InboundMsgSizeValidationConfig;
import org.wso2.transport.http.netty.contract.config.ListenerConfiguration;
import org.wso2.transport.http.netty.contract.config.SslConfiguration;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.xml.namespace.QName;

/**
 * {@code RequestResponseUtils} contains utilities used in request and response message flow.
 */
public class RequestResponseUtils {

    private static final Log LOG = LogFactory.getLog(RequestResponseUtils.class);

    /**
     * Create an Axis2 message context for the given HttpCarbonMessage. The carbon message may be in the
     * process of being streamed.
     *
     * @param incomingCarbonMsg   the http carbon message to be used to create the corresponding Axis2 message context
     * @param sourceConfiguration source configuration
     * @return the Axis2 message context created
     */
    public static MessageContext convertCarbonMsgToAxis2MsgCtx(HttpCarbonMessage incomingCarbonMsg,
                                                               SourceConfiguration sourceConfiguration) {

        MessageContext msgCtx = new MessageContext();

        String transportName;
        if (sourceConfiguration.getScheme().isSSL()) {
            transportName = BridgeConstants.TRANSPORT_HTTPSWSS;
        } else {
            transportName = BridgeConstants.TRANSPORT_HTTPWS;
        }
        ConfigurationContext configurationContext = sourceConfiguration.getConfigurationContext();
        msgCtx.setConfigurationContext(configurationContext);
        msgCtx.setTransportOut(configurationContext.getAxisConfiguration().getTransportOut(transportName));
        msgCtx.setTransportIn(sourceConfiguration.getInDescription());
        msgCtx.setIncomingTransportName(transportName);
        msgCtx.setServerSide(true);

        //TODO: once the correlation id support is brought, the correlationID should be set as the messageID.
        // Refer to https://github.com/wso2-support/wso2-synapse/commit/2c86e14151d48ae3bb814be19b874800bd7468e5
        msgCtx.setMessageID(UIDGenerator.generateURNString());

        //TODO: set correlation id here

        msgCtx.setProperty(BaseConstants.INTERNAL_TRANSACTION_COUNTED, incomingCarbonMsg.getSourceContext().channel()
                .attr(AttributeKey.valueOf(BaseConstants.INTERNAL_TRANSACTION_COUNTED)).get());

        msgCtx.setProperty(Constants.Configuration.TRANSPORT_IN_URL, incomingCarbonMsg.getProperty(BridgeConstants.TO));
        msgCtx.setProperty(MessageContext.CLIENT_API_NON_BLOCKING, Boolean.FALSE);
        msgCtx.setProperty(RequestResponseTransport.TRANSPORT_CONTROL, new HttpCoreRequestResponseTransport(msgCtx));
        msgCtx.setProperty(BridgeConstants.HTTP_SOURCE_CONFIGURATION, sourceConfiguration);

        // Following section is required for throttling to work
        msgCtx.setProperty(MessageContext.REMOTE_ADDR, incomingCarbonMsg.getProperty(
                org.wso2.transport.http.netty.contract.Constants.REMOTE_ADDRESS).toString());
        msgCtx.setProperty(BridgeConstants.REMOTE_HOST,
                incomingCarbonMsg.getProperty(org.wso2.transport.http.netty.contract.Constants.ORIGIN_HOST));

        // http transport header names are case insensitive
        Map<String, String> headers = new HashMap<>();
        incomingCarbonMsg.getHeaders().forEach(entry -> headers.put(entry.getKey(), entry.getValue()));
        msgCtx.setProperty(MessageContext.TRANSPORT_HEADERS, headers);

        // Set the original incoming carbon message as a property
        msgCtx.setProperty(BridgeConstants.HTTP_CARBON_MESSAGE, incomingCarbonMsg);
        // This property is used when responding back to the client
        msgCtx.setProperty(BridgeConstants.HTTP_CLIENT_REQUEST_CARBON_MESSAGE, incomingCarbonMsg);
        msgCtx.setProperty(BridgeConstants.TRANSPORT_MESSAGE_HANDLER, new HttpMessageHandler());
        return msgCtx;
    }

    /**
     * Check if the HTTP_CARBON_MESSAGE is present in the message context.
     *
     * @param msgContext axis2 message context
     * @return true if HTTP_CARBON_MESSAGE is present in the message context, false otherwise
     */
    public static boolean isHttpCarbonMessagePresent(MessageContext msgContext) {

        return Objects.nonNull(msgContext.getProperty(BridgeConstants.HTTP_CARBON_MESSAGE));
    }

    /**
     * Checks if the request/response body should be written using the appropriate message
     * formatter.
     *
     * @param msgContext axis2 message context
     * @return true if the request was initiated from a non-http client or if the message has been built.
     * Otherwise, return false if this is pass-through.
     */
    public static boolean shouldInvokeFormatterToWriteBody(MessageContext msgContext) {

        // If the property HTTP_CARBON_MESSAGE is null, it means the message was initiated from
        // a non-http client. In such cases, the message body will always be available in the
        // envelope itself. Hence, returning true.
        if (!RequestResponseUtils.isHttpCarbonMessagePresent(msgContext)) {
            return true;
        }
        if (msgContext.getEnvelope().getBody().getFirstElement() != null
                || msgContext.isPropertyTrue(BridgeConstants.NO_ENTITY_BODY)) {
            return true;
        }
        return msgContext.isPropertyTrue(BridgeConstants.MESSAGE_BUILDER_INVOKED);
    }

    /**
     * Check whether the content type is multipart or not.
     *
     * @param contentType value of the content type
     * @return true for multipart content types
     */
    public static boolean isMultipartContent(String contentType) {

        return contentType.contains(HTTPConstants.MEDIA_TYPE_MULTIPART_FORM_DATA)
                || contentType.contains(HTTPConstants.HEADER_ACCEPT_MULTIPART_RELATED);
    }

    /**
     * Generate the REST_URL_POSTFIX from the request URI.
     *
     * @param uri         the Request URI as a string
     * @param servicePath service path
     * @return REST_URL_POSTFIX as a string
     */
    public static String getRestUrlPostfix(String uri, String servicePath) {

        String contextServicePath = "/" + servicePath;
        if (uri.startsWith(contextServicePath)) {
            // discard upto servicePath
            uri = uri.substring(uri.indexOf(contextServicePath) +
                    contextServicePath.length());
            // discard [proxy] service name if any
            int pos = uri.indexOf("/", 1);
            if (pos > 0) {
                uri = uri.substring(pos);
            } else {
                pos = uri.indexOf("?");
                if (pos != -1) {
                    uri = uri.substring(pos);
                } else {
                    uri = "";
                }
            }
        } else {
            // remove any absolute prefix if any
            int pos = uri.indexOf("://");
            //compute index of beginning of Query Parameter
            int indexOfQueryStart = uri.indexOf("?");

            //Check if there exist a absolute prefix '://' and it is before query parameters
            //To allow query parameters with URLs. ex: /test?a=http://asddd
            if (pos != -1 && ((indexOfQueryStart == -1 || pos < indexOfQueryStart))) {
                uri = uri.substring(pos + 3);
            }
            pos = uri.indexOf("/");
            if (pos != -1) {
                uri = uri.substring(pos + 1);
            }
            // Remove the service prefix
            if (uri.startsWith(servicePath)) {
                // discard upto servicePath
                uri = uri.substring(uri.indexOf(contextServicePath)
                        + contextServicePath.length());
                // discard [proxy] service name if any
                pos = uri.indexOf("/", 1);
                if (pos > 0) {
                    uri = uri.substring(pos);
                } else {
                    pos = uri.indexOf("?");
                    if (pos != -1) {
                        uri = uri.substring(pos);
                    } else {
                        uri = "";
                    }
                }
            }
        }

        return uri;
    }

    /**
     * Checks if the HTTP request is REST based on the given Content-Type header.
     *
     * @param contentType Content-Type header
     * @return whether the HTTP request is REST or not
     */
    public static boolean isRESTSupportedMediaType(String contentType) {

        // TODO: verify for text/xml
        return contentType != null && !contentType.contains(SOAP11Constants.SOAP_11_CONTENT_TYPE)
                && !contentType.contains(SOAP12Constants.SOAP_12_CONTENT_TYPE);
    }

    /**
     * Populates the SOAP version based on the given Content-Type.
     *
     * @param msgContext  axis2 message context
     * @param contentType Content type of the request.
     * @return 0 if the content type is null. return 2 if the content type is application/soap+xml. Otherwise 1
     */
    public static int populateSOAPVersion(MessageContext msgContext, String contentType) {

        int soapVersion = 0;
        if (contentType != null) {
            if (contentType.contains(SOAP12Constants.SOAP_12_CONTENT_TYPE)) {
                soapVersion = 2;
                TransportUtils.processContentTypeForAction(contentType, msgContext);
            } else if (contentType.contains(SOAP11Constants.SOAP_11_CONTENT_TYPE)) {
                soapVersion = 1;
            } else if (isRESTSupportedMediaType(contentType)) {
                soapVersion = 1;
            }
        }
        return soapVersion;
    }

    /**
     * Checks if the request should be considered as REST.
     *
     * @param msgContext       axis2 message context
     * @param contentType      content-type of the request
     * @param soapVersion      SOAP version
     * @param soapActionHeader SOAPAction header
     * @return whether the request should be considered as REST
     */
    public static boolean isRESTRequest(MessageContext msgContext, String contentType, int soapVersion,
                                        String soapActionHeader) {

        if (isRESTSupportedMediaType(contentType)) {
            return true;
        }
        if (soapVersion == 1) {
            Parameter disableREST = msgContext.getParameter(Java2WSDLConstants.DISABLE_BINDING_REST);
            return soapActionHeader == null && disableREST != null && "false".equals(disableREST.getValue());
        }
        return false;
    }

    /**
     * Get the EPR for the message passed in.
     *
     * @param msgContext the message context
     * @return the destination EPR
     */
    public static EndpointReference getDestinationEPR(MessageContext msgContext) {

        // Transport URL can be different from the WSA-To
        String transportURL = (String) msgContext.getProperty(
                Constants.Configuration.TRANSPORT_URL);

        if (transportURL != null) {
            return new EndpointReference(transportURL);
        } else if (
                (msgContext.getTo() != null) && !msgContext.getTo().hasAnonymousAddress()) {
            return msgContext.getTo();
        }
        return null;
    }

    /**
     * Returns Listener configuration instance populated with source configuration.
     *
     * @param sourceConfiguration source configuration
     * @return transport listener configuration instance
     */
    public static ListenerConfiguration getListenerConfig(SourceConfiguration sourceConfiguration, boolean sslEnabled)
            throws AxisFault {

        ListenerConfiguration listenerConfiguration = new ListenerConfiguration();

        listenerConfiguration.setPort(sourceConfiguration.getPort());
        listenerConfiguration.setHost(sourceConfiguration.getHost());
        listenerConfiguration.setVersion(sourceConfiguration.getProtocol());

        NettyConfiguration globalConfig = NettyConfiguration.getInstance();

        // Set Request validation limits.
        boolean isRequestLimitsValidationEnabled = globalConfig.isRequestLimitsValidationEnabled();
        if (isRequestLimitsValidationEnabled) {
            setInboundMgsSizeValidationConfig(globalConfig.getMaxStatusLineLength(), globalConfig.getMaxHeaderSize(),
                    globalConfig.getMaxEntityBodySize(), listenerConfiguration.getMsgSizeValidationConfig());
        }

        int idleTimeout = globalConfig.getSocketTimeout();
        if (idleTimeout < 0) {
            throw new AxisFault("Idle timeout cannot be negative. If you want to disable the " +
                    "timeout please use value 0");
        }
        listenerConfiguration.setSocketIdleTimeout(idleTimeout);

        listenerConfiguration.setPipeliningEnabled(false); //Pipelining is disabled all the time

        if (isHTTPTraceLoggerEnabled()) {
            listenerConfiguration.setHttpTraceLogEnabled(true);
        }

        if (isHTTPAccessLoggerEnabled()) {
            listenerConfiguration.setHttpAccessLogEnabled(true);
        }

        if (sslEnabled) {
            return setSslConfig(sourceConfiguration.getInDescription(), listenerConfiguration, sourceConfiguration);
        }

        return listenerConfiguration;
    }

    public static boolean isHTTPTraceLoggerEnabled() {

        return Boolean.parseBoolean(System.getProperty(BridgeConstants.HTTP_TRACE_LOG_ENABLED));
    }

    public static boolean isHTTPAccessLoggerEnabled() {

        return Boolean.parseBoolean(System.getProperty(BridgeConstants.HTTP_ACCESS_LOG_ENABLED));
    }

    public static ListenerConfiguration setSslConfig(TransportInDescription transportIn,
                                                     ListenerConfiguration listenerConfiguration,
                                                     BaseConfiguration sourceConfiguration) throws AxisFault {

        List<org.wso2.transport.http.netty.contract.config.Parameter> serverParamList = new ArrayList<>();
        listenerConfiguration.setScheme(BridgeConstants.PROTOCOL_HTTPS);

        // evaluate keystore field
        Parameter keyParam = transportIn.getParameter(BridgeConstants.KEY_STORE);
        OMElement keyStoreEl = keyParam != null ? keyParam.getParameterElement() : null;

        SecretResolver secretResolver = sourceConfiguration.getConfigurationContext()
                .getAxisConfiguration().getSecretResolver();

        populateKeyStoreConfigs(keyStoreEl, listenerConfiguration, secretResolver);

        // evaluate truststore field
        Parameter trustParam = transportIn.getParameter(BridgeConstants.TRUST_STORE);
        OMElement trustStoreEl = trustParam != null ? trustParam.getParameterElement() : null;
        populateTrustStoreConfigs(trustStoreEl, listenerConfiguration, secretResolver);

        // evaluate SSLVerifyClient field
        Parameter clientAuthParam = transportIn.getParameter(BridgeConstants.SSL_VERIFY_CLIENT);
        OMElement clientAuthEl = clientAuthParam != null ? clientAuthParam.getParameterElement() : null;
        final String s = clientAuthEl != null ? clientAuthEl.getText() : "";
        listenerConfiguration.setVerifyClient(s);

        // evaluate HttpsProtocols and SSLProtocol fields
        Parameter httpsProtocolsParam = transportIn.getParameter(BridgeConstants.HTTPS_PROTOCOL);
        OMElement httpsProtocolsEl = httpsProtocolsParam != null ? httpsProtocolsParam.getParameterElement() : null;
        Parameter sslParameter = transportIn.getParameter(BridgeConstants.SSL_PROTOCOL);
        String sslProtocol = sslParameter != null && sslParameter.getValue() != null
                ? sslParameter.getValue().toString() : BridgeConstants.TLS_PROTOCOL;
        populateProtocolConfigs(httpsProtocolsEl, sslProtocol, listenerConfiguration, serverParamList);

        // evaluate PreferredCiphers field
        Parameter preferredCiphersParam = transportIn.getParameter(BridgeConstants.PREFERRED_CIPHERS);
        OMElement preferredCiphersEl = preferredCiphersParam != null
                ? preferredCiphersParam.getParameterElement() : null;
        populateCiphersConfigs(preferredCiphersEl, serverParamList);

        // evaluate CertificateRevocationVerifier field
        Parameter cvpParam = transportIn.getParameter(BridgeConstants.CLIENT_REVOCATION);
        OMElement cvpEl = cvpParam != null ? cvpParam.getParameterElement() : null;
        populateCertValidationConfigs(cvpEl, listenerConfiguration);

        // evaluate common fields
        Parameter sessionTimeoutParam = transportIn.getParameter(BridgeConstants.SSL_SESSION_TIMEOUT);
        Parameter handshakeTimeoutParam = transportIn.getParameter(BridgeConstants.SSL_HANDSHAKE_TIMEOUT);
        String sessionTimeoutEl = sessionTimeoutParam != null && sessionTimeoutParam.getValue() != null
                ? sessionTimeoutParam.getValue().toString() : null;
        String handshakeTimeoutEl = handshakeTimeoutParam != null && handshakeTimeoutParam.getValue() != null
                ? handshakeTimeoutParam.getValue().toString() : null;
        populateTimeoutConfigs(sessionTimeoutEl, handshakeTimeoutEl, listenerConfiguration);

        if (!serverParamList.isEmpty()) {
            listenerConfiguration.setParameters(serverParamList);
        }

        listenerConfiguration.setId(getListenerInterface(listenerConfiguration.getHost(),
                listenerConfiguration.getPort()));
        return listenerConfiguration;
    }

    public static void populateKeyStoreConfigs(OMElement keyStoreEl, SslConfiguration sslConfiguration,
                                               SecretResolver secretResolver) throws AxisFault {

        if (keyStoreEl != null) {
            String location = getValueOfElementWithLocalName(keyStoreEl, BridgeConstants.STORE_LOCATION);
            String type = getValueOfElementWithLocalName(keyStoreEl, BridgeConstants.TYPE);
            OMElement storePasswordEl = keyStoreEl.getFirstChildWithName(
                    new QName(keyStoreEl.getNamespace().getNamespaceURI(), BridgeConstants.PASSWORD));
            OMElement keyPasswordEl = keyStoreEl.getFirstChildWithName(
                    new QName(keyStoreEl.getNamespace().getNamespaceURI(), BridgeConstants.KEY_PASSWORD));

            if (Objects.isNull(location) || location.isEmpty()) {
                throw new AxisFault("KeyStore file location must be provided for secure connection");
            }
            if (storePasswordEl == null) {
                throw new AxisFault("KeyStore password must be provided for secure connection");
            }
            if (keyPasswordEl == null) {
                throw new AxisFault("Cannot proceed because KeyPassword element is missing in KeyStore");
            }
            String storePassword = SecureVaultValueReader.getSecureVaultValue(secretResolver, storePasswordEl);
            String keyPassword = SecureVaultValueReader.getSecureVaultValue(secretResolver, keyPasswordEl);

            sslConfiguration.setKeyStoreFile(location);
            sslConfiguration.setKeyStorePass(storePassword);
            sslConfiguration.setCertPass(keyPassword);
            sslConfiguration.setTLSStoreType(type);
        }
    }

    public static void populateTrustStoreConfigs(OMElement trustStoreEl, SslConfiguration sslConfiguration,
                                                 SecretResolver secretResolver) throws AxisFault {

        String location = getValueOfElementWithLocalName(trustStoreEl, BridgeConstants.STORE_LOCATION);
        String type = getValueOfElementWithLocalName(trustStoreEl, BridgeConstants.TYPE);
        OMElement storePasswordEl = trustStoreEl.getFirstChildWithName(
                new QName(trustStoreEl.getNamespace().getNamespaceURI(), BridgeConstants.PASSWORD));
        if (storePasswordEl == null) {
            throw new AxisFault("Cannot proceed because Password element is missing in TrustStore");
        }
        String storePassword = SecureVaultValueReader.getSecureVaultValue(secretResolver, storePasswordEl);

        sslConfiguration.setTrustStoreFile(location);
        sslConfiguration.setTrustStoreType(type);
        sslConfiguration.setTrustStorePass(storePassword);
    }

    /**
     * Sets the configuration for the inbound request and response size validation.
     *
     * @param maxInitialLineLength The maximum length of the initial line (e.g. {@code "GET / HTTP/1.0"}
     *                             or {@code "HTTP/1.0 200 OK"})
     * @param maxHeaderSize        The maximum length of all headers.
     * @param maxEntityBodySize    The maximum length of the content or each chunk.
     * @param sizeValidationConfig instance that represents the configuration for the inbound request and
     *                             response size validation.
     * @throws AxisFault when the given values are invalid.
     */
    public static void setInboundMgsSizeValidationConfig(int maxInitialLineLength, int maxHeaderSize,
                                                         int maxEntityBodySize,
                                                         InboundMsgSizeValidationConfig sizeValidationConfig)
            throws AxisFault {

        if (maxInitialLineLength >= 0) {
            sizeValidationConfig.setMaxInitialLineLength(Math.toIntExact(maxInitialLineLength));
        }

        if (maxHeaderSize >= 0) {
            sizeValidationConfig.setMaxHeaderSize(Math.toIntExact(maxHeaderSize));
        }

        if (maxEntityBodySize != -1) {
            if (maxEntityBodySize >= 0) {
                sizeValidationConfig.setMaxEntityBodySize(maxEntityBodySize);
            } else {
                throw new AxisFault("Invalid configuration found for maxEntityBodySize : " + maxEntityBodySize);
            }
        }
    }

    public static String getValueOfElementWithLocalName(OMElement element, String localName) {

        Iterator iterator = element.getChildrenWithLocalName(localName);
        String value = null;
        Object obj = iterator.next();
        if (obj instanceof OMElement) {
            value = ((OMElement) obj).getText();
        }
        return value;
    }

    private static void populateCertValidationConfigs(OMElement cvpEl, SslConfiguration sslConfiguration) {

        final String cvEnable = cvpEl != null ?
                cvpEl.getAttribute(new QName("enable")).getAttributeValue() : null;

        if ("true".equalsIgnoreCase(cvEnable)) {
            sslConfiguration.setValidateCertEnabled(true);
            String cacheSizeString = cvpEl.getFirstChildWithName(
                    new QName(cvpEl.getNamespace().getNamespaceURI(), BridgeConstants.CACHE_SIZE)).getText();
            String cacheDelayString = cvpEl.getFirstChildWithName(
                    new QName(cvpEl.getNamespace().getNamespaceURI(), BridgeConstants.CACHE_DELAY)).getText();
            Integer cacheSize = null;
            Integer cacheDelay = null;
            try {
                cacheSize = new Integer(cacheSizeString);
                cacheDelay = new Integer(cacheDelayString);
            } catch (NumberFormatException e) {
                //
            }

            if (Objects.nonNull(cacheDelay) && cacheDelay != 0) {
                sslConfiguration.setCacheValidityPeriod(Math.toIntExact(cacheDelay));
            }
            if (Objects.nonNull(cacheSize) && cacheSize != 0) {
                sslConfiguration.setCacheSize(Math.toIntExact(cacheSize));
            }
        }
    }

    private static void populateTimeoutConfigs(String sessionTimeoutStr, String handshakeTimeoutStr,
                                               SslConfiguration sslConfiguration) {

        if (Objects.nonNull(sessionTimeoutStr) && !sessionTimeoutStr.isEmpty()) {
            try {
                int sessionTimeout = new Integer(sessionTimeoutStr);
                if (sessionTimeout > 0) {
                    sslConfiguration.setSslSessionTimeOut(sessionTimeout);
                } else {
                    LOG.warn("SessionTimeout should be a valid positive number. But found : " + sessionTimeoutStr
                            + ". Hence, using the default value of 86400s/24h");
                }
            } catch (NumberFormatException e) {
                LOG.warn("Invalid number found for SSL SessionTimeout : " + sessionTimeoutStr
                        + ". Hence, using the default value of 86400s/24h");
            }
        }

        if (Objects.nonNull(handshakeTimeoutStr) && !handshakeTimeoutStr.isEmpty()) {
            try {
                int handshakeTimeout = new Integer(handshakeTimeoutStr);
                if (handshakeTimeout > 0) {
                    sslConfiguration.setSslHandshakeTimeOut(handshakeTimeout);
                } else {
                    LOG.warn("HandshakeTimeout should be a valid positive number. But found : " + handshakeTimeoutStr
                            + ". Hence, using the default value of 10s");
                }
            } catch (NumberFormatException e) {
                LOG.warn("Invalid number found for ssl handshakeTimeoutStr : " + handshakeTimeoutStr +
                        ". Hence, using the default value of 10s");
            }
        }
    }

    private static void populateProtocolConfigs(
            OMElement httpsProtocolsEl, String sslProtocol, SslConfiguration sslConfiguration,
            List<org.wso2.transport.http.netty.contract.config.Parameter> paramList) {

        if (httpsProtocolsEl != null) {
            String configuredHttpsProtocols = httpsProtocolsEl.getText().replaceAll("\\s", "");

            if (!configuredHttpsProtocols.isEmpty()) {
                org.wso2.transport.http.netty.contract.config.Parameter serverProtocols
                        = new org.wso2.transport.http.netty.contract.config.Parameter(
                        BridgeConstants.SSL_ENABLED_PROTOCOLS, configuredHttpsProtocols);
                paramList.add(serverProtocols);
            }
        }

        if (Objects.isNull(sslProtocol) || sslProtocol.isEmpty()) {
            sslProtocol = BridgeConstants.TLS_PROTOCOL;
        }
        sslConfiguration.setSSLProtocol(sslProtocol);
    }

    private static void populateCiphersConfigs(
            OMElement preferredCiphersEl, List<org.wso2.transport.http.netty.contract.config.Parameter> paramList) {

        if (preferredCiphersEl != null) {
            String preferredCiphers = preferredCiphersEl.getText().replaceAll("\\s", "");

            if (!preferredCiphers.isEmpty()) {
                org.wso2.transport.http.netty.contract.config.Parameter serverParameters
                        = new org.wso2.transport.http.netty.contract.config.Parameter(
                        BridgeConstants.CIPHERS, preferredCiphers);
                paramList.add(serverParameters);
            }
        }
    }

    public static String getListenerInterface(String host, int port) {

        host = host != null ? host : BridgeConstants.HTTP_DEFAULT_HOST;
        return host + ":" + port;
    }

    public static void handleException(String s, Exception e) throws AxisFault {

        LOG.error(s, e);
        throw new AxisFault(s, e);
    }

    public static void handleException(String msg) throws AxisFault {

        LOG.error(msg);
        throw new AxisFault(msg);
    }
}
