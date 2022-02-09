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
package org.apache.synapse.transport.netty.sender;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.TransportOutDescription;

/**
 * {@code Axis2HttpSSLTransportSender} is the sender class of the HTTP Transport, and it sends out HTTPS
 * outbound requests.
 */
public class Axis2HttpSSLTransportSender extends Axis2HttpTransportSender {

    public void init(ConfigurationContext configurationContext, TransportOutDescription transportOutDescription)
            throws AxisFault {

        super.init(configurationContext, transportOutDescription);
        targetConfiguration.setClientSSLConfigurationBuilder(new ClientSSLConfigurationBuilder()
                .parseSSL(targetConfiguration, transportOutDescription));
    }
}
