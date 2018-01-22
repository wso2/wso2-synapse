/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.transport.nhttp.util;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.impl.llom.soap11.SOAP11Factory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisBindingOperation;
import org.apache.axis2.description.AxisEndpoint;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.WSDL20DefaultValueHolder;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.dispatchers.HTTPLocationBasedDispatcher;
import org.apache.axis2.dispatchers.RequestURIBasedDispatcher;
import org.apache.axis2.dispatchers.RequestURIOperationDispatcher;
import org.apache.axis2.engine.AxisEngine;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.util.URIEncoderDecoder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.synapse.transport.nhttp.NHttpConfiguration;
import org.apache.synapse.transport.nhttp.NhttpConstants;

import javax.xml.namespace.QName;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

/**
 * This class provides a set of utility methods to manage the REST invocation calls
 * going out from the nhttp transport in the HTTP GET method
 */
public class RESTUtil {

    private static final Log log = LogFactory.getLog(RESTUtil.class);
    private static RequestURIBasedDispatcher requestDispatcher = new RequestURIBasedDispatcher();
    private static HTTPLocationBasedDispatcher httpLocationBasedDispatcher =
            new HTTPLocationBasedDispatcher();
    private static RequestURIOperationDispatcher requestURIOperationDispatcher =
            new RequestURIOperationDispatcher();
    private static Object dispatcherInstance = null;
    private static Method invokeMethod = null;


    static {
        String extendedURIBasedDispatcher = System.getProperty("ei.extendedURIBasedDispatcher");
        try {
            if (extendedURIBasedDispatcher != null) {
                Class<?> extendedURIBasedDispatcherClass = RESTUtil.class.getClassLoader().loadClass(extendedURIBasedDispatcher);
                dispatcherInstance = extendedURIBasedDispatcherClass.newInstance();
                invokeMethod = extendedURIBasedDispatcherClass.getMethod("invoke", MessageContext.class);
            }
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | ClassNotFoundException e) {
            log.fatal(e);
        }
    }

    /**
     * This method will return the URI part for the GET HTTPRequest by converting
     * the SOAP infoset to the URL-encoded GET format
     *
     * @param messageContext - from which the SOAP infoset will be extracted to encode
     * @param address        - address of the actual service
     * @return uri       - ERI of the GET request
     * @throws AxisFault - if the SOAP infoset cannot be converted in to the GET URL-encoded format
     */
    public static String getURI(MessageContext messageContext, String address) throws AxisFault {

        OMElement firstElement;
        address = address.substring(address.indexOf("//") + 2);
        address = address.substring(address.indexOf("/"));
        String queryParameterSeparator = (String) messageContext
                .getProperty(WSDL2Constants.ATTR_WHTTP_QUERY_PARAMETER_SEPARATOR);
        // In case queryParameterSeparator is null we better use the default value

        if (queryParameterSeparator == null) {
            queryParameterSeparator = WSDL20DefaultValueHolder
                    .getDefaultValue(WSDL2Constants.ATTR_WHTTP_QUERY_PARAMETER_SEPARATOR);
        }

        firstElement = messageContext.getEnvelope().getBody().getFirstElement();
        String params = "";
        if (firstElement != null) {
            // first element corresponds to the operation name
            address = address + "/" + firstElement.getLocalName();
        } else {
            firstElement = messageContext.getEnvelope().getBody();
        }

        Iterator iter = firstElement.getChildElements();

        String legalCharacters = WSDL2Constants
                .LEGAL_CHARACTERS_IN_QUERY.replaceAll(queryParameterSeparator, "");
        StringBuffer buff = new StringBuffer(params);

        // iterate through the child elements and find the request parameters
        while (iter.hasNext()) {
            OMElement element = (OMElement) iter.next();
            try {
                buff.append(URIEncoderDecoder.quoteIllegal(element.getLocalName(),
                        legalCharacters)).append("=").append(URIEncoderDecoder.quoteIllegal(element.getText(),
                        legalCharacters)).append(queryParameterSeparator);
            } catch (UnsupportedEncodingException e) {
                throw new AxisFault("URI Encoding error : " + element.getLocalName()
                        + "=" + element.getText(), e);
            }
        }

        params = buff.toString();
        if (params.trim().length() != 0) {
            int index = address.indexOf("?");
            if (index == -1) {
                address = address + "?" + params.substring(0, params.length() - 1);
            } else if (index == address.length() - 1) {
                address = address + params.substring(0, params.length() - 1);

            } else {
                address = address
                        + queryParameterSeparator + params.substring(0, params.length() - 1);
            }
        }

        return address;
    }

    /**
     * Processes the HTTP GET / DELETE request and builds the SOAP info-set of the REST message
     *
     * @param msgContext        The MessageContext of the Request Message
     * @param out               The output stream of the response
     * @param requestURI        The URL that the request came to
     * @param contentTypeHeader The contentType header of the request
     * @param httpMethod        The http method of the request
     * @param dispatching   Weather we should do service dispatching
     * @throws AxisFault - Thrown in case a fault occurs
     */
    public static void processGetAndDeleteRequest(MessageContext msgContext, OutputStream out,
                                                  String requestURI, Header contentTypeHeader,
                                                  String httpMethod, boolean dispatching)
            throws AxisFault {

        String contentType = contentTypeHeader != null ? contentTypeHeader.getValue() : null;

        prepareMessageContext(msgContext, requestURI, httpMethod, out, contentType, dispatching);

        msgContext.setProperty(NhttpConstants.NO_ENTITY_BODY, Boolean.TRUE);

        org.apache.axis2.transport.http.util.RESTUtil.processURLRequest(msgContext, out,
                contentType);
    }

    /**
     * Processes the HTTP GET request and builds the SOAP info-set of the REST message
     *
     * @param msgContext The MessageContext of the Request Message
     * @param out        The output stream of the response
     * @param soapAction SoapAction of the request
     * @param requestURI The URL that the request came to
     * @throws AxisFault - Thrown in case a fault occurs
     */
    public static void processURLRequest(MessageContext msgContext, OutputStream out,
                                            String soapAction, String requestURI) throws AxisFault {

        if ((soapAction != null) && soapAction.startsWith("\"") && soapAction.endsWith("\"")) {
            soapAction = soapAction.substring(1, soapAction.length() - 1);
        }

        msgContext.setSoapAction(soapAction);
        msgContext.setTo(new EndpointReference(requestURI));
        msgContext.setProperty(MessageContext.TRANSPORT_OUT, out);
        msgContext.setServerSide(true);
        msgContext.setDoingREST(true);
        msgContext.setEnvelope(new SOAP11Factory().getDefaultEnvelope());
        msgContext.setProperty(NhttpConstants.NO_ENTITY_BODY, Boolean.TRUE);
        AxisEngine.receive(msgContext);
    }

    /**
     * Processes the HTTP POST request and builds the SOAP info-set of the REST message
     *
     * @param msgContext        The MessageContext of the Request Message
     * @param is                The  input stream of the request
     * @param os                The output stream of the response
     * @param requestURI        The URL that the request came to
     * @param contentTypeHeader The contentType header of the request
     * @param dispatching  Weather we should do dispatching
     * @throws AxisFault - Thrown in case a fault occurs
     */
    public static void processPOSTRequest(MessageContext msgContext, InputStream is,
                                          OutputStream os, String requestURI,
                                          Header contentTypeHeader,
                                          boolean dispatching) throws AxisFault {

        String contentType = contentTypeHeader != null ? contentTypeHeader.getValue() : null;
        processPOSTRequest(msgContext, is, os, requestURI, contentType, dispatching);
    }

    /**
     * Processes the HTTP POST request and builds the SOAP info-set of the REST message
     *
     * @param msgContext  MessageContext of the Request Message
     * @param is          Input stream of the request
     * @param os          Output stream of the response
     * @param requestURI  URL that the request came to
     * @param contentType ContentType header of the request
     * @param dispatching Whether we should do dispatching
     * @throws AxisFault - Thrown in case a fault occurs
     */
    public static void processPOSTRequest(MessageContext msgContext, InputStream is,
                                          OutputStream os, String requestURI,
                                          String contentType,
                                          boolean dispatching) throws AxisFault {

        prepareMessageContext(msgContext, requestURI, HTTPConstants.HTTP_METHOD_POST,
                              os, contentType, dispatching);
        org.apache.axis2.transport.http.util.RESTUtil.processXMLRequest(msgContext, is, os,
                                                                        contentType);
    }

    /**
     * prepare message context prior to call axis2 RestUtils
     *
     * @param msgContext  The MessageContext of the Request Message
     * @param requestURI  The URL that the request came to
     * @param httpMethod  The http method of the request
     * @param out         The output stream of the response
     * @param contentType The content type of the request
     * @param dispatching weather we should do dispatching
     * @throws AxisFault Thrown in case a fault occurs
     */
    private static void prepareMessageContext(MessageContext msgContext,
                                              String requestURI,
                                              String httpMethod,
                                              OutputStream out,
                                              String contentType,
                                              boolean dispatching) throws AxisFault {

        msgContext.setTo(new EndpointReference(requestURI));
        msgContext.setProperty(HTTPConstants.HTTP_METHOD, httpMethod);
        msgContext.setServerSide(true);
        msgContext.setDoingREST(true);
        msgContext.setProperty(MessageContext.TRANSPORT_OUT, out);
        msgContext.setProperty(NhttpConstants.REST_REQUEST_CONTENT_TYPE, contentType);
        // workaround to get REST working in the case of
        //  1) Based on the request URI , it is possible to find a service name and operation.
        // However, there is no actual service deployed in the synapse ( no  i.e proxy or other)
        //  e.g  http://localhost:8280/services/StudentService/students where there  is no proxy
        //  service with name StudentService.This is a senario where StudentService is in an external
        //  server and it is needed to call it from synapse using the main sequence
        //  2) request is to be injected into  the main sequence  .i.e. http://localhost:8280
        // This method does not cause any performance issue ...
        // Proper fix should be refractoring axis2 RestUtil in a proper way

        /**
         * This reverseProxyMode was introduce to avoid the LB exposing it's own web service when REST call was initiated
         */
        boolean reverseProxyMode = Boolean.parseBoolean(System.getProperty("reverseProxyMode"));
        AxisService axisService = null;
        if(!reverseProxyMode){
            axisService = requestDispatcher.findService(msgContext);
        }

        boolean isCustomRESTDispatcher = false;
        if (requestURI.matches(NHttpConfiguration.getInstance().getRestUriApiRegex())
                || requestURI.matches(NHttpConfiguration.getInstance().getRestUriProxyRegex())) {
            isCustomRESTDispatcher = true;
        }

        //the logic determines which service dispatcher to get invoke, this will be determine 
        //based on parameter defines at disableRestServiceDispatching, and if super tenant invoke, with isTenantRequest
        //identifies whether the request to be dispatch to custom REST Dispatcher Service.
        
		if (dispatching || !isCustomRESTDispatcher) {
			if (axisService == null) {
				String defaultSvcName = NHttpConfiguration.getInstance().getStringValue("nhttp.default.service", "__SynapseService");
				axisService = msgContext.getConfigurationContext().getAxisConfiguration().getService(defaultSvcName);
			}
			msgContext.setAxisService(axisService);
            //When receiving rest request axis2 checks whether axis operation is set in the message context to build
            // the request message. Therefore we have to set axis operation before we handed over request to axis
            // engine. If axis operation is already set in the message context take it from there. If not take the
            // axis operation from axis service.
            setAxisOperation(msgContext, axisService);
        } else {
            String multiTenantDispatchService = NHttpConfiguration.getInstance().getRESTDispatchService();
            axisService = msgContext.getConfigurationContext().getAxisConfiguration().getService(multiTenantDispatchService);
            msgContext.setAxisService(axisService);
            setAxisOperation(msgContext, axisService);
        }
    }
    
    public static void dispatchAndVerify(MessageContext msgContext) throws AxisFault {
        String extendedURIBasedDispatcher = System.getProperty("ei.extendedURIBasedDispatcher");
        if (extendedURIBasedDispatcher == null) {
            requestDispatcher.invoke(msgContext);
        } else {
            try {
                invokeMethod.invoke(dispatcherInstance, msgContext);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new AxisFault(e.getMessage());
            }
        }
        AxisService axisService = msgContext.getAxisService();
        if (axisService != null) {
            httpLocationBasedDispatcher.invoke(msgContext);
            if (msgContext.getAxisOperation() == null) {
                requestURIOperationDispatcher.invoke(msgContext);
            }

            AxisOperation axisOperation;
            if ((axisOperation = msgContext.getAxisOperation()) != null) {
                AxisEndpoint axisEndpoint =
                        (AxisEndpoint) msgContext.getProperty(WSDL2Constants.ENDPOINT_LOCAL_NAME);
                if (axisEndpoint != null) {
                    AxisBindingOperation axisBindingOperation = (AxisBindingOperation) axisEndpoint
                            .getBinding().getChild(axisOperation.getName());
                    msgContext.setProperty(Constants.AXIS_BINDING_OPERATION, axisBindingOperation);
                }
                msgContext.setAxisOperation(axisOperation);
            }
        }
    }

    /**
     * Sets the axis operation to the message context. If axis operation is already set in the message context take it
     * from there. If not take the axis operation from axis service.
     *
     * @param msgContext  messageContext message context
     * @param axisService axis service
     */
    private static void setAxisOperation(MessageContext msgContext, AxisService axisService) {
        if (msgContext.getAxisOperation() == null && axisService != null) {
            AxisOperation axisOperation = findOperation(axisService, msgContext);
            msgContext.setAxisOperation(axisOperation);
        }
    }

    /**
     * Get the axis operation from the operation name used by the Synapse service (for message mediation).
     *
     * @param svc axis service
     * @param mc  messageContext message context
     * @return axis operation for the message context
     */
    public static AxisOperation findOperation(AxisService svc, MessageContext mc) {
        AxisOperation operation = svc.getOperation(NhttpConstants.SYNAPSE_OPERATION_NAME);
        if (operation == null && mc.getAxisService() != null) {
            operation = processOperationValidation(svc);
        }
        return operation;
    }

    /**
     * Get the default mediation operation from axis service.
     *
     * @param svc axis service which should check for axis operation
     * @return axis operation if service contains default mediate operation otherwise null
     */
    private static AxisOperation processOperationValidation(AxisService svc) {
        Object operationObj = svc.getParameterValue(NhttpConstants.DEFAULT_MEDIATE_OPERATION);
        if (operationObj != null) {
            return (AxisOperation) operationObj;
        }
        return null;
    }
}
