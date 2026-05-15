/*
 *  Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com/).
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

package org.apache.synapse.endpoints.auth.oauth;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class NonProxyAwareProxyRoutePlannerTest {

    private static final HttpHost PROXY = new HttpHost("proxy.example.com", 3128, "http");
    private static final HttpRequest REQUEST = new BasicHttpRequest("GET", "/");
    private static final HttpContext CONTEXT = new BasicHttpContext();

    private Object createRoutePlanner(String nonProxyHosts, String targetProxyHosts) throws Exception {
        Class<?> outerClass = OAuthUtils.class;
        Class<?>[] innerClasses = outerClass.getDeclaredClasses();
        Class<?> plannerClass = null;
        for (Class<?> c : innerClasses) {
            if (c.getSimpleName().equals("NonProxyAwareProxyRoutePlanner")) {
                plannerClass = c;
                break;
            }
        }
        assertNotNull("NonProxyAwareProxyRoutePlanner class not found", plannerClass);

        Constructor<?> constructor = plannerClass.getDeclaredConstructor(HttpHost.class, String.class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(PROXY, nonProxyHosts, targetProxyHosts);
    }

    private HttpHost callDetermineProxy(Object planner, HttpHost target) throws Exception {
        Method method = DefaultProxyRoutePlanner.class.getDeclaredMethod(
                "determineProxy", HttpHost.class, HttpRequest.class, HttpContext.class);
        method.setAccessible(true);
        return (HttpHost) method.invoke(planner, target, REQUEST, CONTEXT);
    }

    @Test
    public void testNonProxyHostsExactMatch() throws Exception {
        Object planner = createRoutePlanner("localhost", null);
        HttpHost result = callDetermineProxy(planner, new HttpHost("localhost", 9876));
        assertNull("localhost should bypass proxy", result);
    }

    @Test
    public void testNonProxyHostsNoMatch() throws Exception {
        Object planner = createRoutePlanner("internal.example.com", null);
        HttpHost result = callDetermineProxy(planner, new HttpHost("external.example.com", 443));
        assertNotNull("external.example.com should use proxy", result);
    }

    @Test
    public void testNonProxyHostsWildcardMatch() throws Exception {
        Object planner = createRoutePlanner("*.internal.com", null);
        HttpHost result = callDetermineProxy(planner, new HttpHost("api.internal.com", 443));
        assertNull("api.internal.com should match *.internal.com and bypass proxy", result);
    }

    @Test
    public void testNonProxyHostsWildcardNoMatch() throws Exception {
        Object planner = createRoutePlanner("*.internal.com", null);
        HttpHost result = callDetermineProxy(planner, new HttpHost("api.external.com", 443));
        assertNotNull("api.external.com should not match *.internal.com", result);
    }

    @Test
    public void testNonProxyHostsWildcardAll() throws Exception {
        Object planner = createRoutePlanner("*", null);
        HttpHost result = callDetermineProxy(planner, new HttpHost("anything.example.com", 443));
        assertNull("* should bypass proxy for all hosts", result);
    }

    @Test
    public void testNonProxyHostsPipeSeparated() throws Exception {
        Object planner = createRoutePlanner("localhost|*.internal.com|10.0.*", null);

        assertNull("localhost should match",
                callDetermineProxy(planner, new HttpHost("localhost", 80)));
        assertNull("api.internal.com should match *.internal.com",
                callDetermineProxy(planner, new HttpHost("api.internal.com", 443)));
        assertNull("10.0.1.5 should match 10.0.*",
                callDetermineProxy(planner, new HttpHost("10.0.1.5", 80)));
        assertNotNull("external.com should not match any",
                callDetermineProxy(planner, new HttpHost("external.com", 443)));
    }

    @Test
    public void testNonProxyHostsEmpty() throws Exception {
        Object planner = createRoutePlanner("", null);
        HttpHost result = callDetermineProxy(planner, new HttpHost("localhost", 80));
        assertNotNull("Empty nonProxyHosts should use proxy (default)", result);
    }

    @Test
    public void testNonProxyHostsNull() throws Exception {
        Object planner = createRoutePlanner(null, null);
        HttpHost result = callDetermineProxy(planner, new HttpHost("localhost", 80));
        assertNotNull("Null nonProxyHosts should use proxy (default)", result);
    }

    @Test
    public void testTargetProxyHostsExactMatch() throws Exception {
        Object planner = createRoutePlanner(null, "api.external.com");
        HttpHost result = callDetermineProxy(planner, new HttpHost("api.external.com", 443));
        assertNotNull("api.external.com matches targetProxyHosts — should use proxy", result);
    }

    @Test
    public void testTargetProxyHostsNoMatch() throws Exception {
        Object planner = createRoutePlanner(null, "*.external.com");
        HttpHost result = callDetermineProxy(planner, new HttpHost("localhost", 80));
        assertNull("localhost doesn't match *.external.com — should bypass proxy", result);
    }

    @Test
    public void testTargetProxyHostsWildcardAll() throws Exception {
        Object planner = createRoutePlanner(null, "*");
        HttpHost result = callDetermineProxy(planner, new HttpHost("anything.example.com", 443));
        assertNotNull("* targetProxyHosts should proxy all hosts", result);
    }

    @Test
    public void testTargetProxyHostsPipeSeparated() throws Exception {
        Object planner = createRoutePlanner(null, "*.external.com|api.partner.org");

        assertNotNull("api.external.com should match *.external.com",
                callDetermineProxy(planner, new HttpHost("api.external.com", 443)));
        assertNotNull("api.partner.org should match exactly",
                callDetermineProxy(planner, new HttpHost("api.partner.org", 443)));
        assertNull("localhost should not match any targetProxyHosts",
                callDetermineProxy(planner, new HttpHost("localhost", 80)));
    }

    @Test
    public void testNonProxyHostsTakesPrecedenceOverTargetProxyHosts() throws Exception {
        Object planner = createRoutePlanner("localhost", "localhost");
        HttpHost result = callDetermineProxy(planner, new HttpHost("localhost", 80));
        assertNull("nonProxyHosts should take precedence — bypass proxy", result);
    }

    @Test
    public void testBothConfiguredDifferentHosts() throws Exception {
        Object planner = createRoutePlanner("localhost", "*.external.com");

        assertNull("localhost matches nonProxyHosts — bypass",
                callDetermineProxy(planner, new HttpHost("localhost", 80)));
        assertNotNull("api.external.com matches targetProxyHosts — use proxy",
                callDetermineProxy(planner, new HttpHost("api.external.com", 443)));
        assertNull("unknown.com doesn't match targetProxyHosts — bypass",
                callDetermineProxy(planner, new HttpHost("unknown.com", 443)));
    }

    @Test
    public void testNonProxyHostsWithSpaces() throws Exception {
        Object planner = createRoutePlanner(" localhost | *.internal.com ", null);
        assertNull("localhost should match despite spaces",
                callDetermineProxy(planner, new HttpHost("localhost", 80)));
        assertNull("api.internal.com should match despite spaces",
                callDetermineProxy(planner, new HttpHost("api.internal.com", 443)));
    }

    @Test
    public void testNoConfigDefaultBehavior() throws Exception {
        Object planner = createRoutePlanner(null, null);
        HttpHost result = callDetermineProxy(planner, new HttpHost("any.host.com", 443));
        assertNotNull("No config should default to using proxy", result);
    }
}
