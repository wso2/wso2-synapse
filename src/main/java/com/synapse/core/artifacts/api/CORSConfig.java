/*
 * Copyright (c) 2025, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package com.synapse.core.artifacts.api;

public class CORSConfig {

    private boolean enabled = false;
    private String[] allowOrigins = {"*"};
    private String[] allowMethods = {"GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"};
    private String[] allowHeaders = {"Origin", "Content-Type", "Accept", "Authorization"};
    private String[] exposeHeaders = {};
    private boolean allowCredentials = false;
    private int maxAge = 86400;

    public CORSConfig() {
        this.enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String[] getAllowOrigins() {
        return allowOrigins;
    }

    public void setAllowOrigins(String[] allowOrigins) {
        this.allowOrigins = allowOrigins;
    }

    public String[] getAllowMethods() {
        return allowMethods;
    }

    public void setAllowMethods(String[] allowMethods) {
        this.allowMethods = allowMethods;
    }

    public String[] getAllowHeaders() {
        return allowHeaders;
    }

    public void setAllowHeaders(String[] allowHeaders) {
        this.allowHeaders = allowHeaders;
    }

    public String[] getExposeHeaders() {
        return exposeHeaders;
    }

    public void setExposeHeaders(String[] exposeHeaders) {
        this.exposeHeaders = exposeHeaders;
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }
}