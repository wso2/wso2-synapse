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
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.endpoints;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.json.JSONObject;

/**
 * WSDLEndpoint represents the endpoints built using a WSDL document. It stores the details about
 * the endpoint in an EndpointDefinition object. Once the WSDLEndpoint object is constructed, it
 * should not access the WSDL document at runtime to obtain endpoint information. If it is necessary
 * to create an endpoint using a dynamic WSDL, store the endpoint configuration in the registry and
 * create a dynamic WSDL endpoint using that registry key.
 * <p/>
 * TODO: This should allow various policies to be applied on fine grained level (e.g. operations).
 */
public class WSDLEndpoint extends AbstractEndpoint {

    private String wsdlURI;
    private OMElement wsdlDoc;
    private String serviceName;
    private String portName;

    public void onFault(MessageContext synCtx) {

        // For setting Car name (still for Proxy)
        logSetter();

        if (synCtx.getProperty(EPConstants.TENANT_INFO_ID) != null &&
                ((int) synCtx.getProperty(EPConstants.TENANT_INFO_ID)) != EPConstants.SUPER_TENANT_ID) {
            org.apache.axis2.context.MessageContext axis2MessageContext =
                    ((Axis2MessageContext) synCtx).getAxis2MessageContext();
            Options options = axis2MessageContext.getOptions();
            EndpointReference to = options.getTo();
            if (to.getAddress() != null && to.getAddress().contains(EPConstants.LOCAL_TRANSPORT_IDENTIFIER)) {
                // removing the local transport identifier from the uri scheme
                options.setTo(new EndpointReference(
                        to.getAddress().substring(EPConstants.LOCAL_TRANSPORT_IDENTIFIER.length())));
            }
        }

        // is this an actual leaf endpoint
        if (getParentEndpoint() != null) {
            // is this really a fault or a timeout/connection close etc?
            if (isTimeout(synCtx)) {
                getContext().onTimeout();
            } else if (isSuspendFault(synCtx)) {
                getContext().onFault();
            }
        }
        
        setErrorOnMessage(synCtx, null, null);
        super.onFault(synCtx);
    }

    public void onSuccess() {
        getContext().onSuccess();
    }

    @Override
    protected void createJsonRepresentation() {

        endpointJson = new JSONObject();
        endpointJson.put(NAME_JSON_ATT, getName());
        endpointJson.put(TYPE_JSON_ATT, "WSDL Endpoint");
        endpointJson.put("wsdlUri", getWsdlURI());
        endpointJson.put("serviceName", getServiceName());
        endpointJson.put("portName", getPortName());
        setAdvancedProperties();
    }

    public void send(MessageContext synCtx) {

        // For setting Car name (still for Proxy)
        logSetter();

        if (getParentEndpoint() == null && !readyToSend()) {
            // if the this leaf endpoint is too a root endpoint and is in inactive 
            informFailure(synCtx, SynapseConstants.ENDPOINT_ADDRESS_NONE_READY,
                    "Currently , WSDL endpoint : " + getContext());
        } else {
            super.send(synCtx);
        }
    }
    
    public String getWsdlURI() {
        return wsdlURI;
    }

    public void setWsdlURI(String wsdlURI) {
        this.wsdlURI = wsdlURI;
    }

    public OMElement getWsdlDoc() {
        return wsdlDoc;
    }

    public void setWsdlDoc(OMElement wsdlDoc) {
        this.wsdlDoc = wsdlDoc;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getPortName() {
        return portName;
    }

    public void setPortName(String portName) {
        this.portName = portName;
    }
}
