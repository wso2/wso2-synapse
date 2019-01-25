/**
 *  Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.transport.passthru;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.ParameterInclude;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.synapse.transport.http.conn.Scheme;
import org.apache.synapse.transport.nhttp.config.ClientConnFactoryBuilder;
import org.apache.synapse.transport.dynamicconfigurations.SSLProfileLoader;
import org.apache.synapse.transport.dynamicconfigurations.SenderProfileReloader;

public class PassThroughHttpSSLSender extends PassThroughHttpSender implements SSLProfileLoader {

    @Override
    public void init(ConfigurationContext configurationContext,
                     TransportOutDescription transportOutDescription) throws AxisFault {
        super.init(configurationContext, transportOutDescription);
        new SenderProfileReloader(this, transportOutDescription);
    }

    @Override
    protected Scheme getScheme() {
        return new Scheme("https", 443, true);
    }

    @Override
    protected ClientConnFactoryBuilder initConnFactoryBuilder(
            final TransportOutDescription transportOut, ConfigurationContext configurationContext) throws AxisFault {
        return new ClientConnFactoryBuilder(transportOut, configurationContext).parseSSL();
    }


    /**
     * Reload SSL profiles and reset connections in PassThroughHttpSSLSender
     *
     * @param transport TransportInDescription of the configuration
     * @throws AxisFault
     */
    public void reloadConfig(ParameterInclude transport) throws AxisFault {
        reloadDynamicSSLConfig((TransportOutDescription) transport);
    }

}
