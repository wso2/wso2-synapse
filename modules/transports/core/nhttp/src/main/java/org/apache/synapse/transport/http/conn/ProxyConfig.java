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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.auth.UsernamePasswordCredentials;

public class ProxyConfig {

    private static final Log log = LogFactory.getLog(ProxyConfig.class);
    private final HttpHost proxy;
    private final UsernamePasswordCredentials creds;
    private final Set<String> proxyBypass;

    // The set of known hosts to bypass proxy
    private Set<String> knownDirectHosts = new HashSet<String>();

     // The set of known hosts to go via proxy
    private Set<String> knownProxyHosts = new HashSet<String>();

    // Map to hold the known proxy profile configuration
    private Map<String, ProxyProfileConfig> knownProxyConfigMap = new HashMap<String,ProxyProfileConfig>();

    // Map to hold the custom proxy profile details
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

        if (proxyProfileMap != null) {
            this.proxyProfileMap = proxyProfileMap;
        } else {
            this.proxyProfileMap = Collections.emptyMap();
        }
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

    /**
     * Selects the configured proxy server
     * @param target request endpoint
     * @return proxy host based on the proxy profile or http.proxyHost,
     *         null when no proxy is configured or if the target is matched with proxy bypass
     */
    public HttpHost selectProxy(final HttpHost target) {
        if (isProxyProfileConfigured()) {
            return getProxyForTargetHost(target.getHostName());
        }

        if (this.proxy != null) {
            if (knownProxyHosts.contains(target.getHostName().toLowerCase(Locale.US))) {
                return this.proxy;
            } else if (knownDirectHosts.contains(target.getHostName().toLowerCase(Locale.US))) {
                return null;
            } else {
                // we are encountering this host for the first time
                if (isBypass(target.getHostName().toLowerCase(Locale.US))) {
                    return null;
                } else {
                    return this.proxy;
                }
            }
        }
        return this.proxy;
    }

    /**
     * checks weather the proxy profile map is empty
     *
     * @return true if proxy profile map is not empty, false otherwise
     */
    public boolean isProxyProfileConfigured() {
        return !this.proxyProfileMap.isEmpty();
    }

    /**
     * select the appropriate proxy for the given targetHost
     *
     * @param targetHost targeted end point
     * @return proxy mapped for the end point, if not returns null
     */
    private HttpHost getProxyForTargetHost(String targetHost) {
        HttpHost proxy = null;

        ProxyProfileConfig proxyProfileForTargetHost = getProxyProfileForTargetHost(targetHost);
        if (proxyProfileForTargetHost != null) {
            proxy = proxyProfileForTargetHost.getProxy();
        }

        return proxy;
    }

    /**
     * Selects the appropriate proxyProfileConfiguration for the given targetHost
     *
     * First, checks in the knownProxyConfigMap and returns the proxyProfile. If the profile is not in the
     * knowProxyConfigMap checks the knowDirectHosts and returns null (since the targetHost is not associated with
     * any proxy).
     *
     * If the request hits the ESB for the first time, checks whether the default profile is configured. If so, a flag
     * is set true. Then the targetHost is matched against the proxyProfileMaps key set
     * i.e check any of the key patten is matching with the targetHost. If the targetHost is matched against a key then
     * calls getProxyProfileConfig(String, String) and returns the proxyProfileConfig.
     *
     * If the targetHost is not matched against the proxyProfileMap key set and default profile flag is set true
     * then calls getProxyProfileConfig(String, String) and returns the defaultProfile.
     *
     * @param targetHost request's targeted host
     * @return ProxyProfileConfig for the given targetHost
     */
    private ProxyProfileConfig getProxyProfileForTargetHost(String targetHost) {
        if (knownProxyConfigMap.containsKey(targetHost)) {
            return knownProxyConfigMap.get(targetHost);
        }

        if (knownDirectHosts.contains(targetHost)) {
            return null;
        }

        boolean defaultProfile = false;
        for (String key : proxyProfileMap.keySet()) {
            if ("*".equals(key)) {
                log.debug("Default proxy profile found");
                defaultProfile = true;
                continue;
            }
            if (targetHost.matches(key)) {
                return getProxyProfileConfig(targetHost, key);
            }
        }

        if (defaultProfile) {
            return getProxyProfileConfig(targetHost, "*");
        }

        return null;
    }

    /**
     * Selects the proxyProfile for the given key and gets the bypass set. Matches the targetHost against the
     * bypass set. If it is matched then adds the targetHost to the knownDirectHosts List and returns null.
     * Otherwise (i.e the targetHost is not matched in the bypass proxy) puts the proxyProfile against the targetHost
     * into the knownProxyConfigMap and returns the proxyProfileConfig
     *
     * @param targetHost request's targeted host
     * @param key proxyProfileMap's key, if default profile then the key is "*"
     * @return proxyProfileConfig
     */
    private ProxyProfileConfig getProxyProfileConfig(String targetHost, String key) {
        ProxyProfileConfig proxyProfileConfig = proxyProfileMap.get(key);
        Set<String> proxyByPass = proxyProfileConfig.getProxyByPass();
        for (String bypass : proxyByPass) {
            if (targetHost.matches(bypass)) {
                knownDirectHosts.add(targetHost);
                return null;
            }
        }
        knownProxyConfigMap.put(targetHost, proxyProfileConfig);
        return proxyProfileConfig;
    }


    /**
     * select the proxy credential for the targetHost
     *
     * @param targetHost targeted host
     * @return proxy credential for the given end point, if not returns null
     */
    public UsernamePasswordCredentials getCredentialsForTargetHost(String targetHost) {
        UsernamePasswordCredentials credentials = null;
        ProxyProfileConfig proxyProfileForTargetHost = getProxyProfileForTargetHost(targetHost);

        if (proxyProfileForTargetHost != null) {
            credentials = proxyProfileForTargetHost.getCredentials();
        }

        return credentials;
    }

    /**
     * returns appropriate log message based on the proxy configuration
     * whether loading proxy profile or single proxy server or no proxy configured
     *
     * @return log message
     */
    public String logProxyConfig() {
        if (isProxyProfileConfigured()) {
            return "HTTP Sender using proxy profile";
        }

        if (this.proxy != null) {
            return "HTTP Sender using Proxy " + getProxy() + " and  bypassing " + getProxyBypass();
        } else {
            return "No proxy configuration found";
        }
    }

    /**
     * checks whether proxy configured (either proxy profile or single server)
     *
     * @return true when either proxy profile is configure or default proxy server is configure, false otherwise
     */
    private boolean isProxyConfigured() {
        return proxy != null || isProxyProfileConfigured();
    }

    /**
     * checks the proxy configuration and profile configuration whether proxy is configured with credential
     *
     * @return true when at least one proxy has configured with credential, false otherwise
     */
    private boolean isProxyHasCredential() {
        if (!isProxyConfigured()) {
            return false;
        }

        if (!isProxyProfileConfigured()) {
            return getCreds() != null;
        }

        for (Map.Entry<String, ProxyProfileConfig> proxyProfile : this.proxyProfileMap.entrySet()) {
            if (proxyProfile.getValue().getCredentials() != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * returns DefaultProxyAuthenticator if single proxy server is configured
     * ProfileProxyAuthenticator if proxy profile is configured
     *
     * @return ProxyAuthenticator, if proxy is not configured null
     * @throws AxisFault
     */
    public ProxyAuthenticator createProxyAuthenticator() throws AxisFault {
        if (!isProxyHasCredential()) return null;

        ProxyAuthenticator proxyAuthenticator;

        try {
            if (isProxyProfileConfigured()) {
                proxyAuthenticator = new ProfileProxyAuthenticator(this);
            } else {
                proxyAuthenticator = new DefaultProxyAuthenticator(getCreds());
            }

        } catch (MalformedChallengeException e) {
            throw new AxisFault("Error while creating proxy authenticator", e);
        }

        return proxyAuthenticator;
    }

    @Override
    public String toString() {
        return "[proxy=" + proxy + ", proxyCredential=" + creds + ", proxyBypass=" + proxyBypass +
                ", proxyProfileMap=" + proxyProfileMap + "]";
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