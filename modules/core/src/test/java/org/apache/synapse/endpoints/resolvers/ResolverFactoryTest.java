/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.synapse.endpoints.resolvers;

import junit.framework.TestCase;
import org.apache.synapse.commons.resolvers.DefaultResolver;
import org.apache.synapse.commons.resolvers.Resolver;
import org.apache.synapse.commons.resolvers.ResolverException;
import org.apache.synapse.commons.resolvers.ResolverFactory;
import org.apache.synapse.commons.resolvers.SystemResolver;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;


/*
This test class tests the resolve factory implementation
 */
@RunWith(PowerMockRunner.class)
public class ResolverFactoryTest extends TestCase {

    public void testGetSystemResolver() {
        String synapseVariable = "$SYSTEM:VAR";
        Resolver resolver = ResolverFactory.getInstance().getResolver(synapseVariable);
        assertNotNull(resolver);
        assertTrue(resolver instanceof SystemResolver);
    }

    public void testGetDefaultResolver() {
        String synapseVariable = "https://localhost:9443/services";
        Resolver resolver = ResolverFactory.getInstance().getResolver(synapseVariable);
        assertNotNull(resolver);
        assertTrue(resolver instanceof DefaultResolver);
        assertEquals(synapseVariable, resolver.resolve());
    }

    public void testGetDefaultResolverUrl() {
        String synapseVariable = "$url";
        Resolver resolver = ResolverFactory.getInstance().getResolver(synapseVariable);
        assertNotNull(resolver);
        assertTrue(resolver instanceof DefaultResolver);
        assertEquals(synapseVariable, resolver.resolve());
    }

    public void testGetDefaultResolverUri() {
        String synapseVariable = "{uri.var.temp}";
        Resolver resolver = ResolverFactory.getInstance().getResolver(synapseVariable);
        assertNotNull(resolver);
        assertTrue(resolver instanceof DefaultResolver);
        assertEquals(synapseVariable, resolver.resolve());
    }

    @Test(expected = ResolverException.class)
    public void testGetUnknownResolver() {
        String synapseVariable = "$SYSTEM1:VAR";
        ResolverFactory.getInstance().getResolver(synapseVariable);
    }

    public void testSystemResolver() {
        String synapseVariable = "$SYSTEM:VAR";
        String envValue = "https://localhost:8080/service";
        Resolver resolver = ResolverFactory.getInstance().getResolver(synapseVariable);
        assertNotNull(resolver);
        assertTrue(resolver instanceof SystemResolver);
        EnvironmentVariables environmentVariables = new EnvironmentVariables();
        environmentVariables.set("VAR", envValue);
        assertEquals(envValue, resolver.resolve());
    }
}
