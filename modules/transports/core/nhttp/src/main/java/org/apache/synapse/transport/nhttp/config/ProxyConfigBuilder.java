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
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
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

    public ProxyConfigBuilder parse(TransportOutDescription transportOut) {
        proxyProfileConfigMap = getProxyProfiles(transportOut);
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

    public Map<String, ProxyConfig> build(TransportOutDescription transportOut) {
        Parameter proxyProfilesParam = transportOut.getParameter("proxyProfiles");
        if (proxyProfilesParam == null) {
            return null;
        }

        //todo add proper log

        OMElement proxyProfilesElt = proxyProfilesParam.getParameterElement();
        Iterator<?> profiles = proxyProfilesElt.getChildrenWithName(new QName("profile"));
        Map<String, ProxyConfig> proxyMap = new HashMap<String, ProxyConfig>();
        while (profiles.hasNext()) {
            OMElement profile = (OMElement) profiles.next();
            OMElement endPointsElt = profile.getFirstChildWithName(new QName("endPoints"));
            if (endPointsElt == null || endPointsElt.getText() == null) {
                //todo throw proper exception (axis2 fault ?) amd log a nice message
                return null;
            }

            String[] endPoints = endPointsElt.getText().split(",");
            String proxyHost = profile.getFirstChildWithName(new QName("proxyHost")).getText();
            String proxyPortStr = profile.getFirstChildWithName(new QName("proxyPort")).getText();
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

            ProxyConfig proxyConfig = new ProxyConfig(proxy, proxyCredentials);

            for (String endPoint : endPoints) {
                endPoint = endPoint.trim();
                if (!proxyMap.containsKey(endPoint)) {
                    proxyMap.put(endPoint, proxyConfig);
                } else {
                    System.out.println("same EP in different proxy");
                    //todo log
                }
            }
        }

        if (proxyMap.size() > 0) {
            //todo log
            return proxyMap;
        }
        return null;
    }



    private Map<String, ProxyProfileConfig> getProxyProfiles(TransportOutDescription transportOut) {
        Parameter proxyProfilesParam = transportOut.getParameter("proxyProfiles");
        if (proxyProfilesParam == null) {
            return null;
        }

        //todo add proper log

        OMElement proxyProfilesElt = proxyProfilesParam.getParameterElement();
        Iterator<?> profiles = proxyProfilesElt.getChildrenWithName(new QName("profile"));
        Map<String, ProxyProfileConfig> proxyProfileMap = new HashMap<String, ProxyProfileConfig>();
        while (profiles.hasNext()) {
            OMElement profile = (OMElement) profiles.next();
            OMElement endPointsEle = profile.getFirstChildWithName(new QName("endPoints"));
            if (endPointsEle == null || endPointsEle.getText() == null) {
                //todo throw proper exception (axis2 fault ?) and log a nice message
                return null;
            }

            String[] endPoints = endPointsEle.getText().split(",");
            String proxyHost = profile.getFirstChildWithName(new QName("proxyHost")).getText();
            String proxyPortStr = profile.getFirstChildWithName(new QName("proxyPort")).getText();
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
                    System.out.println("same EP in different proxy");
                    //todo log
                }
            }
        }

        if (proxyProfileMap.size() > 0) {
            //todo log
            return proxyProfileMap;
        }
        return null;
    }

}
