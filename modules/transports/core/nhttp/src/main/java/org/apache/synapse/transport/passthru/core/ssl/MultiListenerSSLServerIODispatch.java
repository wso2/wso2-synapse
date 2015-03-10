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


import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.nio.NHttpServerEventHandler;
import org.apache.http.nio.reactor.IOSession;
import org.apache.log4j.Logger;
import org.apache.synapse.transport.http.conn.ServerConnFactory;
import org.apache.synapse.transport.passthru.core.MultiListenerServerIODispatch;
import org.apache.synapse.transport.passthru.core.carbonext.TenantInfoConstants;
import org.apache.synapse.transport.passthru.core.carbonext.TenantInfoProvider;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;

public class MultiListenerSSLServerIODispatch extends MultiListenerServerIODispatch {

    private static final Logger log = Logger.getLogger(MultiListenerSSLServerIODispatch.class);

    private volatile Map<Integer, Map<String, ServerConnFactory>> endpointSSLConfigHolder;


    public MultiListenerSSLServerIODispatch(
               Map<Integer, NHttpServerEventHandler> handlers,
               NHttpServerEventHandler nHttpServerEventHandler,
               Map<Integer, Map<String, ServerConnFactory>> endpointSSLConfigHolder) {
        super(handlers, nHttpServerEventHandler, null);
        this.endpointSSLConfigHolder = endpointSSLConfigHolder;
    }

    @Override
    public void update(final ServerConnFactory connFactory) {
        super.update(connFactory);
    }

    @Override
    protected DefaultNHttpServerConnection createConnection(IOSession session) {
        ServerConnFactory serverConnFactory;
        SocketAddress socketAddress = session.getLocalAddress();
        int port = ((InetSocketAddress) socketAddress).getPort();
        Map<String, ServerConnFactory> tenantMap = endpointSSLConfigHolder.get(port);
        if (tenantMap != null) {
            Map<String, String> tenantInformation = TenantInfoProvider.getTenantInformation();
            String callingTenantDomain = tenantInformation.get(TenantInfoConstants.TENANT_DOMAIN);
            if (tenantMap.get(callingTenantDomain) != null) {
                serverConnFactory = tenantMap.get(callingTenantDomain);
                return  serverConnFactory.createConnection(session);
            } else {
                log.error("Cannot find Configured Server Connection Factory for tenant domain " + callingTenantDomain);
            }

        } else {
            log.error("Cannot find Server Connection Factory for port  " + port);
        }
        return null;
    }
}
