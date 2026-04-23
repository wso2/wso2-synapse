/*
 *  Copyright (c) 2025, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management;

import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ServiceAttributes;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link TelemetryUtil} — specifically verifying that
 * {@link TelemetryUtil#getTracerProviderResource(String)} correctly embeds the
 * provided service name into the returned OpenTelemetry {@link Resource}.
 *
 * <p>Relates to issue #4200: when a user configures {@code service_name} in
 * {@code deployment.toml}, the value must eventually reach the OpenTelemetry SDK
 * as the {@code service.name} resource attribute.  The method under test is the
 * last step in that chain (called by {@code OTLPTelemetryManager.init()}).
 */
public class TelemetryUtilTest {

    /**
     * A custom service name propagated through the configuration chain must appear
     * verbatim as the {@code service.name} attribute in the built resource.
     * This is the primary regression test for issue #4200.
     */
    @Test
    public void testGetTracerProviderResource_setsCustomServiceName() {
        String customServiceName = "custom-test-service";
        Resource resource = TelemetryUtil.getTracerProviderResource(customServiceName);

        Assert.assertNotNull("Resource must not be null", resource);
        String actualServiceName = resource.getAttribute(ServiceAttributes.SERVICE_NAME);
        Assert.assertEquals(
                "SERVICE_NAME attribute in the resource must match the value passed to getTracerProviderResource()",
                customServiceName, actualServiceName);
    }

    /**
     * When the default service name constant is passed (simulating the fallback
     * path when no {@code opentelemetry.service.name} synapse property is set),
     * the resource must carry {@code WSO2-SYNAPSE} as the service name.
     */
    @Test
    public void testGetTracerProviderResource_setsDefaultServiceName() {
        String defaultServiceName = TelemetryConstants.DEFAULT_SERVICE_NAME;
        Resource resource = TelemetryUtil.getTracerProviderResource(defaultServiceName);

        Assert.assertNotNull("Resource must not be null", resource);
        String actualServiceName = resource.getAttribute(ServiceAttributes.SERVICE_NAME);
        Assert.assertEquals(
                "SERVICE_NAME attribute must equal the default service name constant",
                defaultServiceName, actualServiceName);
    }

    /**
     * The resource returned by {@code getTracerProviderResource()} must not be null
     * regardless of the service name value supplied, including an empty string.
     */
    @Test
    public void testGetTracerProviderResource_returnsNonNullForEmptyServiceName() {
        Resource resource = TelemetryUtil.getTracerProviderResource("");

        Assert.assertNotNull("Resource must not be null even when service name is empty", resource);
        String actualServiceName = resource.getAttribute(ServiceAttributes.SERVICE_NAME);
        Assert.assertEquals("SERVICE_NAME attribute must equal the empty string passed in",
                "", actualServiceName);
    }

    /**
     * Service names that contain special characters, mixed case, or hyphens must be
     * preserved exactly as provided — no normalisation or trimming should occur.
     */
    @Test
    public void testGetTracerProviderResource_preservesServiceNameExactly() {
        String[] serviceNames = {
                "wso2-synapse",        // lowercase variant (Grafana dashboard default)
                "WSO2-SYNAPSE",        // uppercase default
                "My Service 123",      // spaces and mixed case
                "service_with_under",  // underscores
        };

        for (String name : serviceNames) {
            Resource resource = TelemetryUtil.getTracerProviderResource(name);
            Assert.assertNotNull("Resource must not be null for name: " + name, resource);
            Assert.assertEquals(
                    "SERVICE_NAME attribute must be preserved exactly for input: " + name,
                    name, resource.getAttribute(ServiceAttributes.SERVICE_NAME));
        }
    }

    /**
     * Two successive calls with different service names must each return their own
     * resource with the correct service name — {@code getTracerProviderResource()}
     * must not cache or share state between invocations.
     */
    @Test
    public void testGetTracerProviderResource_isStateless() {
        Resource r1 = TelemetryUtil.getTracerProviderResource("service-alpha");
        Resource r2 = TelemetryUtil.getTracerProviderResource("service-beta");

        Assert.assertEquals("First resource must carry service-alpha",
                "service-alpha", r1.getAttribute(ServiceAttributes.SERVICE_NAME));
        Assert.assertEquals("Second resource must carry service-beta",
                "service-beta", r2.getAttribute(ServiceAttributes.SERVICE_NAME));
    }
}
