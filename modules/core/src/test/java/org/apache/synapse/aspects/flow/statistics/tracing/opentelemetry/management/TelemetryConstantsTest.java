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

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for constants defined in {@link TelemetryConstants} that are relevant
 * to issue #4200 (configurable Jaeger / OpenTelemetry service name).
 *
 * <p>These tests guard:
 * <ul>
 *   <li>The synapse-property key used to look up the configured service name
 *       ({@code OPENTELEMETRY_SERVICE_NAME}).</li>
 *   <li>The fallback value ({@code DEFAULT_SERVICE_NAME}) when no property is set.</li>
 *   <li>Backward compatibility of the deprecated {@code SERVICE_NAME} constant.</li>
 * </ul>
 */
public class TelemetryConstantsTest {

    /**
     * The synapse property key used in both {@code OTLPTelemetryManager} and the
     * key-mappings config-tool must be exactly {@code "opentelemetry.service.name"}.
     * A typo here would silently prevent the configured value from being picked up.
     */
    @Test
    public void testOpentelemetryServiceNamePropertyKey() {
        Assert.assertEquals(
                "OPENTELEMETRY_SERVICE_NAME must equal 'opentelemetry.service.name'",
                "opentelemetry.service.name",
                TelemetryConstants.OPENTELEMETRY_SERVICE_NAME);
    }

    /**
     * {@code DEFAULT_SERVICE_NAME} must never be null — it is used as the fallback
     * value passed to {@code SynapsePropertiesLoader.getPropertyValue()} and later
     * to {@code TelemetryUtil.getTracerProviderResource()}.
     */
    @Test
    public void testDefaultServiceName_isNotNull() {
        Assert.assertNotNull(
                "DEFAULT_SERVICE_NAME must not be null",
                TelemetryConstants.DEFAULT_SERVICE_NAME);
    }

    /**
     * When the {@code SERVICE_NAME} environment variable is absent (normal CI / test
     * environment), {@code DEFAULT_SERVICE_NAME} must fall back to {@code "WSO2-SYNAPSE"}.
     * This matches the value shipped in {@code default.json} and documented in the
     * server configuration templates.
     */
    @Test
    public void testDefaultServiceName_fallsBackToHardcodedValue_whenEnvVarAbsent() {
        String envServiceName = System.getenv("SERVICE_NAME");
        if (envServiceName == null || envServiceName.isEmpty()) {
            Assert.assertEquals(
                    "DEFAULT_SERVICE_NAME must be 'WSO2-SYNAPSE' when the SERVICE_NAME env var is not set",
                    "WSO2-SYNAPSE",
                    TelemetryConstants.DEFAULT_SERVICE_NAME);
        } else {
            // If the env var IS set, the constant should reflect it.
            Assert.assertEquals(
                    "DEFAULT_SERVICE_NAME must equal the SERVICE_NAME env var when it is set",
                    envServiceName,
                    TelemetryConstants.DEFAULT_SERVICE_NAME);
        }
    }

    /**
     * The deprecated {@code SERVICE_NAME} constant must equal {@code DEFAULT_SERVICE_NAME}
     * so that any existing callers that have not yet been migrated continue to receive
     * the same value.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testDeprecatedServiceName_equalsDefaultServiceName() {
        Assert.assertEquals(
                "Deprecated SERVICE_NAME must equal DEFAULT_SERVICE_NAME for backwards compatibility",
                TelemetryConstants.DEFAULT_SERVICE_NAME,
                TelemetryConstants.SERVICE_NAME);
    }

    /**
     * Verify that the set of other foundational telemetry constants have not been
     * accidentally removed or renamed.  These are referenced throughout the codebase
     * and changing them would break the OpenTelemetry configuration flow.
     */
    @Test
    public void testEssentialConstantsArePresent() {
        Assert.assertEquals("opentelemetry.enable", TelemetryConstants.OPENTELEMETRY_ENABLE);
        Assert.assertEquals("opentelemetry.host", TelemetryConstants.OPENTELEMETRY_HOST);
        Assert.assertEquals("opentelemetry.port", TelemetryConstants.OPENTELEMETRY_PORT);
        Assert.assertEquals("opentelemetry.protocol", TelemetryConstants.OPENTELEMETRY_PROTOCOL);
        Assert.assertEquals("opentelemetry.url", TelemetryConstants.OPENTELEMETRY_URL);
        Assert.assertEquals("http", TelemetryConstants.HTTP_PROTOCOL);
        Assert.assertEquals("grpc", TelemetryConstants.GRPC_PROTOCOL);
    }
}
