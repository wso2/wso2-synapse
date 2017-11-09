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

package org.apache.synapse.transport.http.conn;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpRequestFactory;
import org.apache.http.HttpResponseFactory;
import org.apache.http.impl.nio.DefaultNHttpClientConnection;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.nio.NHttpClientEventHandler;
import org.apache.http.nio.NHttpServerEventHandler;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.params.HttpParams;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoggingUtils {

    public final static String HEADER_LOG_ID = "org.apache.synapse.transport.http.headers";
    public final static String WIRE_LOG_ID = "org.apache.synapse.transport.http.wire";
    public final static String ACCESS_LOG_ID = "org.apache.synapse.transport.http.access";
    private final static Pattern SKIP_LOGGING_PATTERN;

    static {
        if (System.getProperty("skip.logging.pattern") != null) {
            SKIP_LOGGING_PATTERN = Pattern.compile(System.getProperty("skip.logging.pattern"));
        } else {
            SKIP_LOGGING_PATTERN = null;
        }
    }

    public static NHttpClientEventHandler decorate(NHttpClientEventHandler handler) {
        Log log = LogFactory.getLog(handler.getClass());
        if (log.isDebugEnabled()) {
            handler = new LoggingNHttpClientHandler(log, handler);
        }
        return handler;
    }

    public static NHttpServerEventHandler decorate(NHttpServerEventHandler handler) {
        Log log = LogFactory.getLog(handler.getClass());
        if (log.isDebugEnabled()) {
            handler = new LoggingNHttpServiceHandler(log, handler);
        }
        return handler;
    }

    public static DefaultNHttpClientConnection createClientConnection(
            final IOSession iosession,
            final HttpResponseFactory responseFactory,
            final ByteBufferAllocator allocator,
            final HttpParams params) {
        return new LoggingNHttpClientConnection(
                iosession,
                responseFactory,
                allocator,
                params);
    }

    public static Pattern getSkipLoggingMatcher() {
        return SKIP_LOGGING_PATTERN;
    }

    public static DefaultNHttpServerConnection createServerConnection(
            final IOSession iosession,
            final HttpRequestFactory requestFactory,
            final ByteBufferAllocator allocator,
            final HttpParams params) {
        return new LoggingNHttpServerConnection(
                iosession,
                requestFactory,
                allocator,
                params);
    }

}