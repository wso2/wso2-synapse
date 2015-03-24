/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.synapse.transport.passthru.core.ssl;


import org.apache.axis2.AxisFault;
import org.apache.axis2.description.TransportInDescription;
import org.apache.http.HttpHost;
import org.apache.synapse.transport.http.conn.ServerConnFactory;
import org.apache.synapse.transport.passthru.core.PassThroughSharedListenerConfiguration;

public class SSLConnectionUtils {

    /**
     * Method for return ServerConnectionFactory by building ServerConnectionFactory with given SSL Configurations
     *
     * @param endpointName  Endpoint Name
     * @param configuration PassThroughSharedListenerConfiguration
     * @return ServerConnectionFactory
     * @throws AxisFault
     */
    public static ServerConnFactory getServerConnectionFactory(String endpointName,
                                                               PassThroughSharedListenerConfiguration configuration,
                                                               SSLConfiguration sslConfiguration)
               throws AxisFault {
        TransportInDescription transportInDescription = new TransportInDescription(endpointName);
        HttpHost host = new HttpHost(
                   configuration.getSourceConfiguration().getHostname(),
                   configuration.getSourceConfiguration().getPort(),
                   configuration.getSourceConfiguration().getScheme().getName());
        SSLServerConnFactoryBuilder sslServerConnFactoryBuilder = new SSLServerConnFactoryBuilder(transportInDescription, host);

        return sslServerConnFactoryBuilder.
                   parseSSL(sslConfiguration.getKeyStoreElement(), sslConfiguration.getTrustStoreElement(),
                            sslConfiguration.getClientAuthElement(), sslConfiguration.getHttpsProtocolElement(),
                            sslConfiguration.getSslProtocol(), sslConfiguration.getRevocationVerifierElement()).
                   build(configuration.getSourceConfiguration().getHttpParams());

    }

}
