/*
 *  Copyright (c) 2026, WSO2 LLC. (https://www.wso2.com/).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.endpoints;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ProxyConfigsTest {

    @Test
    public void testNonProxyHostsGetterSetter() {
        ProxyConfigs proxyConfigs = new ProxyConfigs();
        assertNull(proxyConfigs.getNonProxyHosts());

        proxyConfigs.setNonProxyHosts("localhost|*.internal.com");
        assertEquals("localhost|*.internal.com", proxyConfigs.getNonProxyHosts());
    }

    @Test
    public void testTargetProxyHostsGetterSetter() {
        ProxyConfigs proxyConfigs = new ProxyConfigs();
        assertNull(proxyConfigs.getTargetProxyHosts());

        proxyConfigs.setTargetProxyHosts("*.external.com|api.example.com");
        assertEquals("*.external.com|api.example.com", proxyConfigs.getTargetProxyHosts());
    }

    @Test
    public void testNonProxyHostsWithEmptyString() {
        ProxyConfigs proxyConfigs = new ProxyConfigs();
        proxyConfigs.setNonProxyHosts("");
        assertEquals("", proxyConfigs.getNonProxyHosts());
    }

    @Test
    public void testTargetProxyHostsWithNull() {
        ProxyConfigs proxyConfigs = new ProxyConfigs();
        proxyConfigs.setTargetProxyHosts(null);
        assertNull(proxyConfigs.getTargetProxyHosts());
    }

    @Test
    public void testExistingFieldsUnaffected() {
        ProxyConfigs proxyConfigs = new ProxyConfigs();
        proxyConfigs.setProxyHost("proxy.example.com");
        proxyConfigs.setProxyPort("8080");
        proxyConfigs.setProxyProtocol("HTTP");
        proxyConfigs.setProxyEnabled(true);
        proxyConfigs.setNonProxyHosts("localhost");
        proxyConfigs.setTargetProxyHosts("*.external.com");

        assertEquals("proxy.example.com", proxyConfigs.getProxyHost());
        assertEquals("8080", proxyConfigs.getProxyPort());
        assertEquals("HTTP", proxyConfigs.getProxyProtocol());
        assertEquals(true, proxyConfigs.isProxyEnabled());
        assertEquals("localhost", proxyConfigs.getNonProxyHosts());
        assertEquals("*.external.com", proxyConfigs.getTargetProxyHosts());
    }
}
