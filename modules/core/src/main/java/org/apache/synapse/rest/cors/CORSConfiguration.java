/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.rest.cors;

import java.util.Set;

/**
 * {@code CORSConfiguration} is the interface that need to be implemented in order hold the CORS configuration information
 */
public interface CORSConfiguration {

    /**
     * Returns allowed origins in the configuration.
     *
     * @return allowed origins
     */
    Set<String> getAllowedOrigins();

    /**
     * Returns allowed headers in the configuration.
     *
     * @return allowed headers
     */
    String getAllowedHeaders();

    /**
     * Returns if CORS is enabled.
     *
     * @return boolean enabled
     */
    boolean isEnabled();

}
