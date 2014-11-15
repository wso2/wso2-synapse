/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.transport.passthru.core;

import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.nio.NHttpServerEventHandler;
import org.apache.synapse.transport.http.conn.ServerConnFactory;
import org.apache.synapse.transport.passthru.ServerIODispatch;

import java.io.IOException;
import java.util.Map;

public class MultiListenerServerIODispatch extends ServerIODispatch {

    private volatile Map<Integer, NHttpServerEventHandler> handlers;


    public MultiListenerServerIODispatch
            (final Map<Integer, NHttpServerEventHandler> handlers, final ServerConnFactory connFactory){
        super(null,connFactory);
        this.handlers = handlers;

    }

    @Override
    protected void onConnected(final DefaultNHttpServerConnection defaultNHttpServerConnection) {
        int localPort = defaultNHttpServerConnection.getLocalPort();
        NHttpServerEventHandler handler = handlers.get(localPort);
        try {
            handler.connected(defaultNHttpServerConnection);
        } catch (final Exception ex) {
            handler.exception(defaultNHttpServerConnection, ex);
        }
    }

    @Override
    protected void onClosed(DefaultNHttpServerConnection defaultNHttpServerConnection) {
        int localPort = defaultNHttpServerConnection.getLocalPort();
        NHttpServerEventHandler handler = handlers.get(localPort);
        try {
            handler.closed(defaultNHttpServerConnection);
        } catch (final Exception ex) {
            handler.exception(defaultNHttpServerConnection, ex);
        }
    }

    @Override
    protected void onException(DefaultNHttpServerConnection defaultNHttpServerConnection, IOException e) {
        int localPort = defaultNHttpServerConnection.getLocalPort();
        NHttpServerEventHandler handler = handlers.get(localPort);
        try {
            handler.exception(defaultNHttpServerConnection, e);
        } catch (final Exception ex) {
            handler.exception(defaultNHttpServerConnection, ex);
        }
    }

    @Override
    protected void onInputReady(DefaultNHttpServerConnection defaultNHttpServerConnection) {
        int localPort = defaultNHttpServerConnection.getLocalPort();
        NHttpServerEventHandler handler = handlers.get(localPort);
        try {
            defaultNHttpServerConnection.consumeInput(handler);
        } catch (final Exception ex) {
            handler.exception(defaultNHttpServerConnection, ex);
        }
    }

    @Override
    protected void onOutputReady(DefaultNHttpServerConnection defaultNHttpServerConnection) {
        int localPort = defaultNHttpServerConnection.getLocalPort();
        NHttpServerEventHandler handler = handlers.get(localPort);
        try {
            defaultNHttpServerConnection.produceOutput(handler);
        } catch (final Exception ex) {
            handler.exception(defaultNHttpServerConnection, ex);
        }
    }

    @Override
    protected void onTimeout(DefaultNHttpServerConnection defaultNHttpServerConnection) {
        int localPort = defaultNHttpServerConnection.getLocalPort();
        NHttpServerEventHandler handler = handlers.get(localPort);
        try {
            handler.timeout(defaultNHttpServerConnection);
        } catch (final Exception ex) {
            handler.exception(defaultNHttpServerConnection, ex);
        }
    }

}