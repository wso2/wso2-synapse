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
package org.apache.synapse.transport.netty.config;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;

/**
 * This HTTP transport level interface is used for plugging in different implementations for special processing of some
 * HTTP GET requests.
 * <p/>
 * e.g. ?wsdl, ?wsdl2 etc.
 * <p/>
 * If you need to handle a special HTTP GET request, you have to write an implementation of this
 * interface.
 */
public interface HttpGetRequestProcessor {

    /**
     * Initialize the HttpGetRequestProcessor.
     *
     * @param cfgCtx servers configuration context
     * @throws AxisFault if an error occurs
     */
    void init(ConfigurationContext cfgCtx) throws AxisFault;

    /**
     * Process the HTTP GET request.
     *
     * @param inboundCarbonMsg  inbound HttpCarbonMessage
     * @param messageContext    The MessageContext
     * @param isRestDispatching Rest dispatching
     */
    void process(HttpCarbonMessage inboundCarbonMsg, MessageContext messageContext, boolean isRestDispatching);

}
