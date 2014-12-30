/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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


import org.apache.http.HttpHost;
import org.apache.http.auth.UsernamePasswordCredentials;

public class ProxyProfileConfig {

    private final HttpHost proxy;
    private final UsernamePasswordCredentials creds;


    public ProxyProfileConfig(HttpHost proxy, UsernamePasswordCredentials creds) {
        this.proxy = proxy;
        this.creds = creds;
    }

    public HttpHost getProxy() {
        return proxy;
    }

    public UsernamePasswordCredentials getCreds() {
        return creds;
    }

}
