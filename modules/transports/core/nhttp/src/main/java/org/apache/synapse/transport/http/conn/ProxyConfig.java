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

import java.util.*;

import org.apache.http.HttpHost;
import org.apache.http.auth.UsernamePasswordCredentials;

public class ProxyConfig {

    private final HttpHost proxy;
    private final UsernamePasswordCredentials creds;
    private final Set<String> proxyBypass;
    /**
     * The list of known hosts to bypass proxy
     */
    private List<String> knownDirectHosts = new ArrayList<String>();
    /**
     * The list of known hosts to go via proxy
     */
    private List<String> knownProxyHosts = new ArrayList<String>();


    /**
     * Map to hold the custom proxy profile details
     */
    private Map<String, ProxyProfileConfig> proxyProfileMap = new HashMap<String, ProxyProfileConfig>();

    public ProxyConfig(
            final HttpHost proxy,
            final UsernamePasswordCredentials creds,
            final String[] proxyBypass,
            final Map<String, ProxyProfileConfig> proxyProfileMap) {
        super();
        this.proxy = proxy;
        this.creds = creds;
        if (proxyBypass != null) {
            this.proxyBypass = new LinkedHashSet<String>(proxyBypass.length);
            for (String s: proxyBypass) {
                this.proxyBypass.add(s.trim().toLowerCase(Locale.US));
            }
        } else {
            this.proxyBypass = Collections.<String>emptySet();
        }

        if(proxyProfileMap != null) {
            this.proxyProfileMap = proxyProfileMap;
        } else{
            this.proxyProfileMap = Collections.emptyMap();
        }
    }

    public ProxyConfig(final HttpHost proxy, final UsernamePasswordCredentials creds) {
        super();
        this.proxy = proxy;
        this.creds = creds;
        this.proxyBypass = null;
    }


    public HttpHost getProxy() {
        return proxy;
    }

    public UsernamePasswordCredentials getCreds() {
        return creds;
    }

    public Set<String> getProxyBypass() {
        return proxyBypass;
    }

    public HttpHost selectProxy(final HttpHost target) {
        if (this.proxy != null) {
            if (knownProxyHosts.contains(target.getHostName().toLowerCase(Locale.US))) {
                return this.proxy;
            } else if (knownDirectHosts.contains(target.getHostName().toLowerCase(Locale.US))) {
                return null;
            } else {
                // we are encountering this host for the first time
                if (isBypass(target.getHostName().toLowerCase(Locale.US))) {
                    return null;
                }else{
                    return this.proxy;
                }
            }
        }
        return this.proxy;
    }

    public boolean isProxyProfileEmpty() {
        return this.proxyProfileMap.isEmpty();
    }

    public HttpHost getProxyForEndPoint(String endPoint){
        ProxyProfileConfig proxyProfileConfig = this.proxyProfileMap.get(endPoint);
        if (proxyProfileConfig == null) {
            return null;
        }
        return proxyProfileConfig.getProxy();
    };

    public UsernamePasswordCredentials getCredentialsForEndPoint(String endPoint) {
        ProxyProfileConfig proxyProfileConfig = this.proxyProfileMap.get(endPoint);
        if (proxyProfileConfig == null) {
            return null;
        }
        return proxyProfileConfig.getCreds();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[proxy=").append(proxy).append(", proxycreds=")
            .append(creds).append(", proxyBypass=").append(proxyBypass).append("]");
        return builder.toString();
    }

    private boolean isBypass(String hostName) {
        for (String entry : this.proxyBypass) {
            if (hostName.matches(entry)) {
                knownDirectHosts.add(hostName);
                return true;
            }
        }
        knownProxyHosts.add(hostName);
        return false;
    }
}