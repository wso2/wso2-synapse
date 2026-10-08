/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.synapse.commons.property.PropertyHolder;
import org.apache.synapse.commons.resolvers.ResolverException;
import org.apache.synapse.core.axis2.ProxyService;

import java.util.Properties;

/**
 * Tests resolving the startOnLoad attribute of a proxy service through the configuration resolver.
 */
public class ProxyServiceFactoryStartOnLoadTest extends AbstractTestCase {

    private static final String CONFIG_KEY = "proxyStartOnLoadTestKey";

    @Override
    protected void tearDown() throws Exception {
        PropertyHolder.getInstance().getProperties().remove(CONFIG_KEY);
        super.tearDown();
    }

    public void testStartOnLoadResolvedToTrue() {
        PropertyHolder.getInstance().setProperty(CONFIG_KEY, "true");
        ProxyService proxy = createProxy("startOnLoad=\"${configs." + CONFIG_KEY + "}\"");
        assertTrue("startOnLoad should resolve to true", proxy.isStartOnLoad());
    }

    public void testStartOnLoadResolvedToFalse() {
        PropertyHolder.getInstance().setProperty(CONFIG_KEY, "false");
        ProxyService proxy = createProxy("startOnLoad=\"${configs." + CONFIG_KEY + "}\"");
        assertFalse("startOnLoad should resolve to false", proxy.isStartOnLoad());
    }

    public void testStartOnLoadUndefinedConfigKey() {
        try {
            createProxy("startOnLoad=\"${configs." + CONFIG_KEY + "}\"");
            fail("An undefined configurable key should fail instead of defaulting to false");
        } catch (ResolverException e) {
            assertTrue(e.getMessage().contains(CONFIG_KEY));
        }
    }

    public void testStartOnLoadLiteralValues() {
        assertTrue(createProxy("startOnLoad=\"true\"").isStartOnLoad());
        assertFalse(createProxy("startOnLoad=\"false\"").isStartOnLoad());
    }

    public void testStartOnLoadDefaultsToTrue() {
        assertTrue("startOnLoad should default to true", createProxy("").isStartOnLoad());
    }

    private ProxyService createProxy(String startOnLoadAttribute) {
        OMElement proxyElem = createOMElement("<proxy xmlns=\"http://ws.apache.org/ns/synapse\" " +
                "name=\"StartOnLoadProxy\" transports=\"http\" " + startOnLoadAttribute + ">" +
                "<target endpoint=\"epr\"/></proxy>");
        return ProxyServiceFactory.createProxy(proxyElem, new Properties());
    }
}
