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

package com.synapse.core.artifacts.endpoint;

import com.synapse.core.artifacts.utils.Position;

public class Endpoint {
    private String name;
    private EndpointUrl endpointUrl;
    private String fileName;
    private Position position;

    public Endpoint() {
    }

    public Endpoint(String name, EndpointUrl endpointUrl, String fileName, Position position) {
        this.name = name;
        this.endpointUrl = endpointUrl;
        this.fileName = fileName;
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EndpointUrl getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(EndpointUrl endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "Endpoint{" +
                "name='" + name + '\'' +
                ", endpointUrl=" + endpointUrl +
                ", fileName='" + fileName + '\'' +
                ", position=" + position +
                '}';
    }
}
