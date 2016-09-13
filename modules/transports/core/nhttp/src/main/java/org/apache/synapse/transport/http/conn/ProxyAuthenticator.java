/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.transport.http.conn;

import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.protocol.HttpContext;

/**
 * If proxy server is configured with username and password, appropriate implementation of ProxyAuthenticator
 * will be initialised based on the proxy configuration (profile / default)
 */
public interface ProxyAuthenticator {
    /**
     * Adds proxy authentication header to the request
     * @param request out going request
     * @param context http context
     * @throws AuthenticationException
     */
    public void authenticatePreemptively(HttpRequest request, HttpContext context) throws AuthenticationException;
    
}


