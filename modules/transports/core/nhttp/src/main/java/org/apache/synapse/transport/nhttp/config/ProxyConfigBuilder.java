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
import java.util.Iterator;
import java.util.Map;

public class ProxyConfigBuilder {

    private HttpHost proxy;
    private UsernamePasswordCredentials proxycreds;
    private String[] proxyBypass;
    private Map<String, ProxyProfileConfig> proxyProfileConfigMap;
    private String name;

    private static final Log log = LogFactory.getLog(ProxyConfigBuilder.class);

    /**
     * Tries to read the axis2.xml transport sender's proxy configuration
     * @param transportOut axis2 transport out description
     * @return ProxyConfigBuilder
     * @throws AxisFault
     */
    public ProxyConfigBuilder parse(TransportOutDescription transportOut) throws AxisFault {
        name = transportOut.getName();
        proxyProfileConfigMap = getProxyProfiles(transportOut);

        // if proxy profile is configured, we can ignore the proxy configured with http.proxyHost
        if (proxyProfileConfigMap != null) {
            return this;
        }

        String proxyHost = null;
        int proxyPort = -1;
        Parameter proxyHostParam = transportOut.getParameter("http.proxyHost");
        if (proxyHostParam != null) {
            proxyHost = (String) proxyHostParam.getValue();
            Parameter proxyPortParam = transportOut.getParameter("http.proxyPort");
            if (proxyPortParam != null) {
                proxyPort = Integer.parseInt((String) proxyPortParam.getValue());
            }
        }
        if (proxyHost == null) {
            proxyHost = System.getProperty("http.proxyHost");
            if (proxyHost != null) {
                String s = System.getProperty("http.proxyPort");
                if (s != null) {
                    proxyPort = Integer.parseInt(s);
                }
            }
        }
        if (proxyHost != null) {
            proxy = new HttpHost(proxyHost, proxyPort >= 0 ? proxyPort : 80);

            String s = null;
            Parameter bypassListParam = transportOut.getParameter("http.nonProxyHosts");
            if (bypassListParam != null) {
                s = (String) bypassListParam.getValue();
            }
            if (s == null) {
                s = System.getProperty("http.nonProxyHosts");
            }
            if (s != null) {
                proxyBypass = s.split("\\|");
            }

            Parameter proxyUsernameParam = transportOut.getParameter("http.proxy.username");
            Parameter proxyPasswordParam = transportOut.getParameter("http.proxy.password");
            if (proxyUsernameParam != null) {
                proxycreds = new UsernamePasswordCredentials((String) proxyUsernameParam.getValue(),
                        proxyPasswordParam != null ? (String) proxyPasswordParam.getValue() : "");
            }
        }
        return this;
    }

    public ProxyConfig build() {
        return new ProxyConfig(proxy, proxycreds, proxyBypass, proxyProfileConfigMap);
    }

    /**
     * Looks for a transport parameter named proxyProfiles and initializes a map of ProxyProfileConfig.
     * The syntax for defining a proxy profiles is as follows.
     * {@code
     * <parameter name="proxyProfiles">
     *      <profile>
     *          <endPoints>localhost:8247, localhost:8251</endPoints>
     *          <proxyHost>localhost</proxyHost>
     *          <proxyPort>3128</proxyPort>
     *          <proxyUserName>squidUser</proxyUserName>
     *          <proxyPassword>password</proxyPassword>
     *      </profile>
     *      <profile>
     *          <endPoints>localhost:8249</endPoints>
     *          <proxyHost>localhost</proxyHost>
     *          <proxyPort>7443</proxyPort>
     *      </profile>
     * </parameter>
     * }
     *
     * @param transportOut transport out description
     * @return map of <code>ProxyProfileConfig<code/> if configured in axis2.xml; otherwise null
     * @throws AxisFault if at least one proxy profile is not properly configured
     */
    private Map<String, ProxyProfileConfig> getProxyProfiles(TransportOutDescription transportOut) throws AxisFault {
        Parameter proxyProfilesParam = transportOut.getParameter("proxyProfiles");
        if (proxyProfilesParam == null) {
            return null;
        }

        if (log.isDebugEnabled()) {
            log.debug(name + " Loading proxy profiles for the HTTP/S sender");
        }

        OMElement proxyProfilesEle = proxyProfilesParam.getParameterElement();
        Iterator<?> profiles = proxyProfilesEle.getChildrenWithName(new QName("profile"));
        Map<String, ProxyProfileConfig> proxyProfileMap = new HashMap<String, ProxyProfileConfig>();
        while (profiles.hasNext()) {
            OMElement profile = (OMElement) profiles.next();
            OMElement endPointsEle = profile.getFirstChildWithName(new QName("endPoints"));
            if (endPointsEle == null || endPointsEle.getText() == null) {
                String msg = "Each proxy profile must define at least one host:port " +
                        "pair under the endPoints element";
                log.error(name + " " + msg);
                throw new AxisFault(msg);
            }

            String proxyHost;
            String proxyPortStr;

            String[] endPoints = endPointsEle.getText().split(",");
            OMElement proxyHostEle = profile.getFirstChildWithName(new QName("proxyHost"));
            if (proxyHostEle != null) {
                proxyHost = proxyHostEle.getText();
                OMElement proxyPortEle = profile.getFirstChildWithName(new QName("proxyPort"));
                if (proxyPortEle != null) {
                    proxyPortStr = proxyPortEle.getText();
                } else {
                    throw new AxisFault("Proxy Port didn't configure correctly in proxy profile");
                }
            } else {
                throw new AxisFault("Proxy Host didn't configure correctly in proxy profile");
            }

            UsernamePasswordCredentials proxyCredentials = null;
            OMElement proxyUserNameEle = profile.getFirstChildWithName(new QName("proxyUserName"));
            if (proxyUserNameEle != null) {
                String proxyUserName = proxyUserNameEle.getText();
                OMElement proxyPasswordEle = profile.getFirstChildWithName(new QName("proxyPassword"));
                String proxyPassword = proxyPasswordEle != null ? proxyPasswordEle.getText() : "";
                proxyCredentials = new UsernamePasswordCredentials(proxyUserName,
                        proxyPassword != null ? proxyPassword : "");
            }

            int proxyPort = Integer.parseInt(proxyPortStr);
            HttpHost proxy = new HttpHost(proxyHost, proxyPort >= 0 ? proxyPort : 80);

            ProxyProfileConfig proxyProfileConfig = new ProxyProfileConfig(proxy, proxyCredentials);

            for (String endPoint : endPoints) {
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
            log.info(name + " Proxy profiles initialized for " + proxyProfileMap.size() + " endPoints");
            return proxyProfileMap;
        }
        return null;
    }

}
