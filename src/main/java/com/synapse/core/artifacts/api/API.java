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

import com.synapse.core.artifacts.utils.Position;

import java.util.List;

public class API {
    private String context;
    private String name;
    private List<Resource> resources;
    private Position position;
    private CORSConfig corsConfig;
    
    public API(String context, String name, List<Resource> resources, Position position) {
        this.context = context;
        this.name = name;
        this.resources = resources;
        this.position = position;
    }

    public API() {

    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public CORSConfig getCorsConfig() {
        return corsConfig;
    }

    public void setCorsConfig(CORSConfig corsConfig) {
        this.corsConfig = corsConfig;
    }
}