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

package org.apache.synapse.samples.framework;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.SynapseException;
import org.apache.synapse.samples.framework.config.SampleConfigConstants;

import javax.xml.namespace.QName;
import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class SynapseTestUtils {

    static String replace(String str, String pattern, String replace) {
        int s = 0;
        int e;
        StringBuilder result = new StringBuilder();

        while ((e = str.indexOf(pattern, s)) >= 0) {
            result.append(str.substring(s, e));
            result.append(replace);
            s = e + pattern.length();
        }
        result.append(str.substring(s));
        return result.toString();
    }

    static String getIPAddress() throws Exception {
        List<InetAddress> ipAddresses = new ArrayList<InetAddress>();
        String ipAddress = null;

        Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
        while (e.hasMoreElements()) {
            NetworkInterface ni = e.nextElement();
            // Clustering doesn't work for loop-back addresses, so we are not interested
            // we are not interested in inactive interfaces either
            if (ni.isLoopback() || !ni.isUp()) continue;

            Enumeration<InetAddress> e2 = ni.getInetAddresses();
            while (e2.hasMoreElements()) {
                InetAddress ip = e2.nextElement();
                ipAddresses.add(ip);
            }
        }

        if (ipAddresses.isEmpty()) {
            return null;
        } else {
            for (InetAddress ip : ipAddresses) {
                if (ip instanceof Inet4Address) {
                    ipAddress = ip.getHostAddress();
                    break;
                }
            }
        }

        if (ipAddress == null) {
            ipAddress = ipAddresses.get(0).getHostAddress();
        }

        return ipAddress;
    }

    public static String getParameter(OMElement root, String name, String def) {
        OMElement child = root.getFirstChildWithName(new QName(name));
        if (child != null && !"".equals(child.getText())) {
            return child.getText();
        } else {
            return def;
        }
    }

    public static String getRequiredParameter(OMElement root, String name) {
        OMElement child = root.getFirstChildWithName(new QName(name));
        if (child != null && !"".equals(child.getText())) {
            return child.getText();
        } else {
            throw new SynapseException("Required parameter: " + name + " unspecified");
        }
    }

    public static String getAttribute(OMElement root, String name, String def) {
        String value = root.getAttributeValue(new QName(name));
        if (value != null && !"".equals(value)) {
            return value;
        } else {
            return def;
        }
    }

    public static ProcessController createController(OMElement root) {
        if (SampleConfigConstants.TAG_BE_SERVER_CONF_AXIS2_SERVER.equals(root.getLocalName())) {
            return new Axis2BackEndServerController(root);
        } else if (SampleConfigConstants.TAG_BE_SERVER_CONF_DERBY_SERVER.equals(root.getLocalName())) {
            return new DerbyServerController(root);
        } else if (SampleConfigConstants.TAG_BE_SERVER_CONF_JMS_BROKER.equals(root.getLocalName())) {
            return new ActiveMQController(root);
        } else if (SampleConfigConstants.TAG_BE_SERVER_CONF_ECHO_SERVER.equals(root.getLocalName())) {
            return new EchoHttpServerController(root);
        }
        return null;
    }

    public static String getCurrentDir() {
        return System.getProperty("user.dir") + File.separator;
    }

}
