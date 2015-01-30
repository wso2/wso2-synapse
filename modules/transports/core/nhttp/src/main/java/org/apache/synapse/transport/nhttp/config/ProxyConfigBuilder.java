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
            String proxyHostStr = "http.proxyHost";
            String proxyPortStr = "http.proxyPort";
            Parameter proxyHostParam = transportOut.getParameter(proxyHostStr);
            if (proxyHostParam != null) {
                proxyHost = (String) proxyHostParam.getValue();
                Parameter proxyPortParam = transportOut.getParameter(proxyPortStr);
                if (proxyPortParam != null) {
                    proxyPort = Integer.parseInt((String) proxyPortParam.getValue());
                }
            }
            if (proxyHost == null) {
                proxyHost = System.getProperty(proxyHostStr);
                if (proxyHost != null) {
                    String s = System.getProperty(proxyPortStr);
                    if (s != null) {
                        proxyPort = Integer.parseInt(s);
                    }
                }
            }
            if (proxyHost != null) {
                proxy = new HttpHost(proxyHost, proxyPort >= 0 ? proxyPort : 80);

                String bypassListStr = null;
                String nonProxyHostStr = "http.nonProxyHosts";
                Parameter bypassListParam = transportOut.getParameter(nonProxyHostStr);
                if (bypassListParam != null) {
                    bypassListStr = (String) bypassListParam.getValue();
                }
                if (bypassListStr == null) {
                    bypassListStr = System.getProperty(nonProxyHostStr);
                }
                if (bypassListStr != null) {
                    proxyBypass = bypassListStr.split("\\|");
                }

                Parameter proxyUsernameParam = transportOut.getParameter("http.proxy.username");
                Parameter proxyPasswordParam = transportOut.getParameter("http.proxy.password");
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
     *          <bypass>xxx.sample.com, </bypass>
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

        QName profileQName = new QName("profile");
        QName targetHostsQName = new QName("targetHosts");

        OMElement proxyProfilesParamEle = proxyProfilesParam.getParameterElement();
        Iterator<?> profiles = proxyProfilesParamEle.getChildrenWithName(profileQName);
        Map<String, ProxyProfileConfig> proxyProfileMap = new HashMap<String, ProxyProfileConfig>();

        while (profiles.hasNext()) {
            OMElement profile = (OMElement) profiles.next();
            OMElement targetHostsEle = profile.getFirstChildWithName(targetHostsQName);
            if (targetHostsEle == null || targetHostsEle.getText() == null) {
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

    private HttpHost getHttpProxy(OMElement profile, String targetHosts) throws AxisFault {
        String proxyHost;
        String proxyPortStr;
        QName proxyHostQName = new QName("proxyHost");
        QName proxyPortQName = new QName("proxyPort");
        OMElement proxyHostEle = profile.getFirstChildWithName(proxyHostQName);
        if (proxyHostEle != null) {
            proxyHost = proxyHostEle.getText();
            OMElement proxyPortEle = profile.getFirstChildWithName(proxyPortQName);
            if (proxyPortEle != null) {
                proxyPortStr = proxyPortEle.getText();
            } else {
                throw new AxisFault("Proxy Port didn't configure correctly in proxy profile ["+targetHosts+"]");
            }
        } else {
            throw new AxisFault("Proxy Host didn't configure correctly in proxy profile ["+targetHosts+"]");
        }

        int proxyPort = Integer.parseInt(proxyPortStr);
        return new HttpHost(proxyHost, proxyPort >= 0 ? proxyPort : 80);
    }

    private UsernamePasswordCredentials getUsernamePasswordCredentials(OMElement profile) {
        UsernamePasswordCredentials proxyCredentials = null;
        QName proxyUserQName = new QName("proxyUserName");
        QName proxyPasswordQName = new QName("proxyPassword");

        OMElement proxyUserNameEle = profile.getFirstChildWithName(proxyUserQName);
        if (proxyUserNameEle != null) {
            String proxyUserName = proxyUserNameEle.getText();
            OMElement proxyPasswordEle = profile.getFirstChildWithName(proxyPasswordQName);
            String proxyPassword = proxyPasswordEle != null ? proxyPasswordEle.getText() : "";
            proxyCredentials = new UsernamePasswordCredentials(proxyUserName,
                    proxyPassword != null ? proxyPassword : "");
        }
        return proxyCredentials;
    }

    private Set<String> getProxyBypass(OMElement profile) {
        Set<String> bypassSet = new HashSet<String>();
        QName bypassQName = new QName("bypass");
        OMElement bypassEle = profile.getFirstChildWithName(bypassQName);
        if (bypassEle != null && !bypassEle.getText().equals("")) {
            String[] bypass = bypassEle.getText().split(",");

            for (String s : bypass) {
                bypassSet.add(s.trim());
            }
        }

        return bypassSet;
    }



}
