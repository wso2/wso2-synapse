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

/**
 * Interface for managing token caching operations.
 */
public interface TokenCacheProvider {

    /**
     * Stores a token in the cache with the specified ID.
     *
     * @param id    the unique identifier for the token
     * @param token the token to be cached
     */
    void putToken(String id, String token);

    /**
     * Retrieves a token from the cache using the specified ID.
     *
     * @param id the unique identifier for the token
     * @return the cached token, or {@code null} if not found
     */
    String getToken(String id);

    /**
     * Removes a token from the cache using the specified ID.
     *
     * @param id the unique identifier for the token to be removed
     */
    void removeToken(String id);

    /**
     * Removes all tokens associated with the specified OAuth handler from the cache.
     *
     * @param oauthHandlerId the identifier of the OAuth handler whose tokens are to be removed
     */
    void removeTokens(String oauthHandlerId);
}
