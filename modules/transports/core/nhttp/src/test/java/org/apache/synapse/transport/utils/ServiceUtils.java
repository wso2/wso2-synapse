/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
*/
package org.apache.synapse.transport.utils;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.context.OperationContext;
import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.http.HttpException;
import org.apache.synapse.transport.passthru.PassThroughHttpSender;

import java.io.IOException;

public class ServiceUtils {
    /**
     * This will return the service endpoint url
     *
     * @param service service name
     * @param host    hots name
     * @param port    port number
     * @return service url
     */
    public static String getServiceEndpoint(String service, String host, int port) {
        return "http://" + host + ":" + port + "/services/" + service;
    }

    /**
     * This will provide the mocking functionality of AxisEngine.receive() method.
     * when AxisEngine is mocked, this method can be used with doAnswer
     *
     * @param axis2MessageContext
     * @throws IOException
     * @throws HttpException
     */
    public static void receive(MessageContext axis2MessageContext) throws IOException, HttpException {
        ServiceContext svcCtx = new ServiceContext();
        OperationContext opCtx = new OperationContext(new InOutAxisOperation(), svcCtx);
        axis2MessageContext.setServiceContext(svcCtx);
        axis2MessageContext.setOperationContext(opCtx);
        axis2MessageContext.getOperationContext().setProperty(org.apache.axis2.Constants.RESPONSE_WRITTEN, "SKIP");
        PassThroughHttpSender sender = new PassThroughHttpSender();
        ConfigurationContext cfgCtx = new ConfigurationContext(new AxisConfiguration());
        sender.init(cfgCtx, new TransportOutDescription("http"));
        sender.submitResponse(axis2MessageContext);
    }
}
