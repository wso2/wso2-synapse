/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.endpoints.auth.oauth;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.config.SynapsePropertiesLoader;
import org.apache.synapse.endpoints.auth.AuthConstants;

import java.util.concurrent.TimeUnit;

import static org.apache.synapse.endpoints.auth.AuthConstants.TOKEN_CACHE_TIMEOUT_PROPERTY;

/**
 * Token Cache Implementation
 * Tokens will be invalidate after a interval of TOKEN_CACHE_TIMEOUT minutes
 */
public class TokenCache implements TokenCacheProvider {

    private static final Log log = LogFactory.getLog(TokenCache.class);

    private static TokenCache instance = null;

    private static Cache<String, String> tokenMap;

    private TokenCache() {

        long cacheTimeout = AuthConstants.TOKEN_CACHE_TIMEOUT;
        try {
            cacheTimeout =
                    Long.parseLong(
                            SynapsePropertiesLoader.loadSynapseProperties().getProperty(TOKEN_CACHE_TIMEOUT_PROPERTY));
        } catch (NumberFormatException e) {
            log.debug("OAuth token cache will be using default timeout");
        }
        tokenMap = CacheBuilder.newBuilder().expireAfterWrite(cacheTimeout, TimeUnit.SECONDS).build();
    }

    /**
     * Get TokenCache Instance
     *
     * @return TokenCache
     */
    public static TokenCache getInstance() {

        if (instance == null) {
            instance = new TokenCache();
        }
        return instance;
    }

    /**
     * Stores a token in the cache with the specified ID.
     *
     * @param id    the unique identifier for the token
     * @param token the token to be cached
     */
    @Override
    public void putToken(String id, String token) {

        tokenMap.put(id, token);
    }

    /**
     * Retrieves a token from the cache using the specified ID.
     *
     * @param id the unique identifier for the token
     * @return the cached token, or {@code null} if not found
     */
    @Override
    public String getToken(String id) {

        return tokenMap.getIfPresent(id);
    }

    /**
     * This method is called to remove the token from the cache when the token is invalid
     *
     * @param id id of the endpoint
     */
    @Override
    public void removeToken(String id) {

        tokenMap.invalidate(id);
    }

    /**
     * This method is called to remove the tokens from the cache when the endpoint is destroyed
     *
     * @param oauthHandlerId id of the OAuth handler bounded to the endpoint
     */
    @Override
    public void removeTokens(String oauthHandlerId) {
        tokenMap.asMap().entrySet().removeIf(entry -> entry.getKey().startsWith(oauthHandlerId));
    }
}
