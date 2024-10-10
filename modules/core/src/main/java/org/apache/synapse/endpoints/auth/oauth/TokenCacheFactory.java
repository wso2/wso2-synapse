/*
 * Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com/).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.endpoints.auth.oauth;

import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapsePropertiesLoader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Factory class responsible for providing the appropriate implementation of the TokenCacheProvider interface.
 * This class manages the singleton instance of TokenCacheProvider, ensuring that it is only loaded once and reused
 * across the application.
 */
public class TokenCacheFactory {

    /**
     * Singleton instance of TokenCacheProvider. This will be initialized the first time, and the same instance will be
     * returned on subsequent calls.
     */
    private static TokenCacheProvider tokenCacheProvider;

    /**
     * Retrieves the singleton instance of TokenCacheProvider. If the instance is not already initialized,
     * it attempts to load the provider class specified in the `token.cache.class` property. If the property
     * is not set or the class cannot be loaded, it defaults to the TokenCache implementation.
     *
     * @return the singleton instance of TokenCacheProvider
     * @throws SynapseException if there is an error loading the specified class
     */
    public static TokenCacheProvider getTokenCache() {
        if (tokenCacheProvider != null) {
            return tokenCacheProvider;
        }

        String classPath = SynapsePropertiesLoader.loadSynapseProperties().getProperty("token.cache.class");
        if (classPath != null) {
            tokenCacheProvider = loadTokenCacheProvider(classPath);
        } else {
            tokenCacheProvider = TokenCache.getInstance();
        }
        return tokenCacheProvider;
    }

    /**
     * Loads the TokenCacheProvider implementation specified by the given class path.
     *
     * @param classPath the fully qualified class path of the TokenCacheProvider implementation
     * @return an instance of the specified TokenCacheProvider implementation
     * @throws SynapseException if there is an error loading the class or invoking the `getInstance` method
     */
    private static TokenCacheProvider loadTokenCacheProvider(String classPath) {
        try {
            Class<?> clazz = Class.forName(classPath);
            Method getInstanceMethod = clazz.getMethod("getInstance");
            return (TokenCacheProvider) getInstanceMethod.invoke(null);
        } catch (ClassNotFoundException e) {
            throw new SynapseException("Error loading class: " + classPath, e);
        } catch (NoSuchMethodException e) {
            throw new SynapseException("getInstance method not found for class: " + classPath, e);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new SynapseException("Error invoking getInstance method for class: " + classPath, e);
        }
    }
}
