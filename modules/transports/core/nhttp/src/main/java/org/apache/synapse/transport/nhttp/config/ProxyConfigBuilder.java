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
package org.apache.synapse.transport.nhttp.config;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.synapse.transport.http.conn.ProxyConfig;
import org.apache.synapse.transport.http.conn.ProxyProfileConfig;
import org.apache.synapse.transport.passthru.PassThroughConstants;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ProxyConfigBuilder {

    private HttpHost proxy;
    private UsernamePasswordCredentials proxyCredentials;
    private String[] proxyBypass;
    private String name;

    private static final QName Q_PROFILE = new QName("profile");
    private static final QName Q_TARGET_HOSTS = new QName("targetHosts");
    private static final QName Q_PROXY_HOST = new QName("proxyHost");
    private static final QName Q_PROXY_PORT = new QName("proxyPort");
    private static final QName Q_PROXY_USER = new QName("proxyUserName");
    private static final QName Q_PROXY_PASSWORD = new QName("proxyPassword");
    private static final QName Q_BYPASS = new QName("bypass");

    private static final Log log = LogFactory.getLog(ProxyConfigBuilder.class);

    /**
     * Tries to read the axis2.xml transport sender's proxy configuration
     * @param transportOut axis2 transport out description
     * @return ProxyConfig
     * @throws AxisFault
     */
    public ProxyConfig build(TransportOutDescription transportOut) throws AxisFault {
        name = transportOut.getName();
        Map<String, ProxyProfileConfig> proxyProfileConfigMap = getProxyProfiles(transportOut);

        // if proxy profile is not configured, we read the proxy configured using http.proxyHost
        // if proxy profile is configured we only read profile related configuration
        if (proxyProfileConfigMap == null) {

            String proxyHost = null;
            int proxyPort = -1;
            Parameter proxyHostParam = transportOut.getParameter(PassThroughConstants.HTTP_PROXY_HOST);
            if (proxyHostParam != null) {
                proxyHost = (String) proxyHostParam.getValue();
                Parameter proxyPortParam = transportOut.getParameter(PassThroughConstants.HTTP_PROXY_PORT);
                if (proxyPortParam != null) {
                    proxyPort = Integer.parseInt((String) proxyPortParam.getValue());
                }
            }
            if (proxyHost == null) {
                proxyHost = System.getProperty(PassThroughConstants.HTTP_PROXY_HOST);
                if (proxyHost != null) {
                    String s = System.getProperty(PassThroughConstants.HTTP_PROXY_PORT);
                    if (s != null) {
                        proxyPort = Integer.parseInt(s);
                    }
                }
            }
            if (proxyHost != null) {
                proxy = new HttpHost(proxyHost, proxyPort >= 0 ? proxyPort : 80);

                String bypassListStr = null;
                Parameter bypassListParam = transportOut.getParameter(PassThroughConstants.HTTP_NON_PROXY_HOST);

                if (bypassListParam == null) {
                    bypassListStr = System.getProperty(PassThroughConstants.HTTP_NON_PROXY_HOST);
                } else {
                    bypassListStr = (String) bypassListParam.getValue();
                }
                if (bypassListStr != null) {
                    proxyBypass = bypassListStr.split("\\|");
                }

                Parameter proxyUsernameParam = transportOut.getParameter(PassThroughConstants.HTTP_PROXY_USERNAME);
                Parameter proxyPasswordParam = transportOut.getParameter(PassThroughConstants.HTTP_PROXY_PASSWORD);
                if (proxyUsernameParam != null) {
                    proxyCredentials = new UsernamePasswordCredentials((String) proxyUsernameParam.getValue(),
                            proxyPasswordParam != null ? (String) proxyPasswordParam.getValue() : "");
                }
            }
        }
        return new ProxyConfig(proxy, proxyCredentials, proxyBypass, proxyProfileConfigMap);
    }

    /**
     * Looks for a transport parameter named proxyProfiles and initializes a map of ProxyProfileConfig.
     * The syntax for defining a proxy profiles is as follows.
     * {@code
     * <parameter name="proxyProfiles">
     *      <profile>
     *          <targetHosts>example.com, *.sample.com</targetHosts>
     *          <proxyHost>localhost</proxyHost>
     *          <proxyPort>3128</proxyPort>
     *          <proxyUserName>squidUser</proxyUserName>
     *          <proxyPassword>password</proxyPassword>
     *          <bypass>xxx.sample.com</bypass>
     *      </profile>
     *      <profile>
     *          <targetHosts>localhost</targetHosts>
     *          <proxyHost>localhost</proxyHost>
     *          <proxyPort>7443</proxyPort>
     *      </profile>
     *      <profile>
     *          <targetHosts>*</targetHosts>
     *          <proxyHost>localhost</proxyHost>
     *          <proxyPort>7443</proxyPort>
     *          <bypass>test.com, direct.com</bypass>
     *      </profile>
     * </parameter>
     * }
     *
     * @param transportOut transport out description
     * @return map of <code>ProxyProfileConfig<code/> if configured in axis2.xml; otherwise null
     * @throws AxisFault if proxy profile is not properly configured
     */
    private Map<String, ProxyProfileConfig> getProxyProfiles(TransportOutDescription transportOut) throws AxisFault {
        Parameter proxyProfilesParam = transportOut.getParameter("proxyProfiles");
        if (proxyProfilesParam == null) {
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug(name + " Loading proxy profiles for the HTTP/S sender");
        }

        OMElement proxyProfilesParamEle = proxyProfilesParam.getParameterElement();
        Iterator<?> profiles = proxyProfilesParamEle.getChildrenWithName(Q_PROFILE);
        Map<String, ProxyProfileConfig> proxyProfileMap = new HashMap<String, ProxyProfileConfig>();

        while (profiles.hasNext()) {
            OMElement profile = (OMElement) profiles.next();
            OMElement targetHostsEle = profile.getFirstChildWithName(Q_TARGET_HOSTS);
            if (targetHostsEle == null || targetHostsEle.getText().isEmpty()) {
                String msg = "Each proxy profile must define at least one host " +
                        "or a wildcard matcher under the targetHosts element";
                log.error(name + " " + msg);
                throw new AxisFault(msg);
            }

            HttpHost proxy = getHttpProxy(profile, targetHostsEle.getText());

            UsernamePasswordCredentials proxyCredentials = getUsernamePasswordCredentials(profile);

            Set<String> proxyBypass = getProxyBypass(profile);

            ProxyProfileConfig proxyProfileConfig = new ProxyProfileConfig(proxy, proxyCredentials, proxyBypass);

            String[] targetHosts = targetHostsEle.getText().split(",");

            for (String endPoint : targetHosts) {
                endPoint = endPoint.trim();
                if (!proxyProfileMap.containsKey(endPoint)) {
                    proxyProfileMap.put(endPoint, proxyProfileConfig);
                } else {
                    log.warn(name + " Multiple proxy profiles were found for the endPoint : " +
                            endPoint + ". Ignoring the excessive profiles.");
                }
            }
        }

        if (proxyProfileMap.size() > 0) {
            log.info(name + " Proxy profiles initialized for " + proxyProfileMap.size() + " targetHosts");
            return proxyProfileMap;
        }
        return null;
    }

    /**
     * Extracts the proxy server detail from given profile
     * @param profile profile element
     * @param targetHosts targetHosts given in the profile
     * @return proxyServer(HttpHost) configured in the given profile element
     * @throws AxisFault, if host or port element is not configured properly
     */
    private HttpHost getHttpProxy(OMElement profile, String targetHosts) throws AxisFault {
        String proxyHost;
        String proxyPortStr;
        OMElement proxyHostEle = profile.getFirstChildWithName(Q_PROXY_HOST);
        if (proxyHostEle != null) {
            proxyHost = proxyHostEle.getText();
            OMElement proxyPortEle = profile.getFirstChildWithName(Q_PROXY_PORT);
            if (proxyPortEle != null) {
                proxyPortStr = proxyPortEle.getText();
            } else {
                throw new AxisFault("Proxy Port didn't configure correctly in proxy profile [" + targetHosts + "]");
            }
        } else {
            throw new AxisFault("Proxy Host didn't configure correctly in proxy profile [" + targetHosts + "]");
        }

        int proxyPort = Integer.parseInt(proxyPortStr);
        return new HttpHost(proxyHost, proxyPort >= 0 ? proxyPort : 80);
    }

    /**
     * Extracts the credential from given profile
     * @param profile profile element
     * @return usernamePasswordCredentials if username, password is configured, null otherwise
     */
    private UsernamePasswordCredentials getUsernamePasswordCredentials(OMElement profile) {
        UsernamePasswordCredentials proxyCredentials = null;
        OMElement proxyUserNameEle = profile.getFirstChildWithName(Q_PROXY_USER);
        if (proxyUserNameEle != null) {
            String proxyUserName = proxyUserNameEle.getText();
            OMElement proxyPasswordEle = profile.getFirstChildWithName(Q_PROXY_PASSWORD);
            String proxyPassword = proxyPasswordEle != null ? proxyPasswordEle.getText() : "";
            proxyCredentials = new UsernamePasswordCredentials(proxyUserName,
                    proxyPassword != null ? proxyPassword : "");
        }
        return proxyCredentials;
    }

    /**
     * Extracts the proxyBypass hosts from given profile
     * @param profile profile element
     * @return Set of String containing bypass hosts; empty set if bypass hosts is not configured
     */
    private Set<String> getProxyBypass(OMElement profile) {
        Set<String> bypassSet = new HashSet<String>();
        OMElement bypassEle = profile.getFirstChildWithName(Q_BYPASS);
        if (bypassEle != null && !bypassEle.getText().isEmpty()) {
            String[] bypassHosts = bypassEle.getText().split(",");

            for (String bypassHost : bypassHosts) {
                bypassSet.add(bypassHost.trim());
            }
        }
        return bypassSet;
    }

}
