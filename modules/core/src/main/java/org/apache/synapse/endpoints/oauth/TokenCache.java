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

package org.apache.synapse.endpoints.oauth;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Token Cache Implementation
 * Tokens will be invalidate after a interval of TOKEN_CACHE_TIMEOUT minutes
 */
public class TokenCache {

    private static final TokenCache instance = new TokenCache();

    private final Cache<String, String> tokenMap =
            CacheBuilder.newBuilder().expireAfterWrite(OAuthConstants.TOKEN_CACHE_TIMEOUT, TimeUnit.MINUTES).build();

    private TokenCache() {

    }

    /**
     * Get TokenCache Instance
     *
     * @return TokenCache
     */
    public static TokenCache getInstance() {

        return instance;
    }

    /**
     * This method returns the value in the cache, or computes it from the specified Callable
     *
     * @param id       id of the oauth handler
     * @param callable to generate a new token by calling oauth server
     * @return Token object
     */
    public String getToken(String id, Callable<String> callable) throws ExecutionException {

        return tokenMap.get(id, callable);
    }

    /**
     * This method is called to remove the token from the cache when the endpoint is destroyed
     *
     * @param id id of the endpoint
     */
    public void removeToken(String id) {

        tokenMap.invalidate(id);
    }

}
