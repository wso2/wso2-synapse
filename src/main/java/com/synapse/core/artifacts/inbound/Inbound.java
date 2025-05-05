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

package com.synapse.core.artifacts.inbound;

import com.synapse.core.artifacts.utils.Position;
import com.synapse.core.ports.InboundEndpoint;

import java.util.List;

public class Inbound {
    private String name;
    private String sequence;
    private String protocol;
    private String suspend;
    private String onError;
    private List<Parameter> parameters;
    private Position position;
    private InboundEndpoint inboundEndpoint;

    public Inbound() {
    }

    public Inbound(String name, String sequence, String protocol, String suspend, String onError, List<Parameter> parameters, Position position) {
        this.name = name;
        this.sequence = sequence;
        this.protocol = protocol;
        this.suspend = suspend;
        this.onError = onError;
        this.parameters = parameters;
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getSuspend() {
        return suspend;
    }

    public void setSuspend(String suspend) {
        this.suspend = suspend;
    }

    public String getOnError() {
        return onError;
    }

    public void setOnError(String onError) {
        this.onError = onError;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public InboundEndpoint getInboundEndpoint() {
        return inboundEndpoint;
    }

    public void setInboundEndpoint(InboundEndpoint inboundEndpoint) {}

    @Override
    public String toString() {
        return "Inbound{" +
                "name='" + name + '\'' +
                ", sequence='" + sequence + '\'' +
                ", protocol='" + protocol + '\'' +
                ", suspend='" + suspend + '\'' +
                ", onError='" + onError + '\'' +
                ", parameters=" + parameters +
                ", position=" + position +
                '}';
    }
}