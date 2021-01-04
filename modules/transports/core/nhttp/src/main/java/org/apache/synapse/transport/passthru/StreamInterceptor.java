/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.transport.passthru;

import org.apache.axis2.context.MessageContext;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Intercepts the streams of source/target request and responses.
 */
public interface StreamInterceptor {

    boolean interceptSourceRequest(MessageContext axisCtx);

    boolean sourceRequest(ByteBuffer buffer, MessageContext axisCtx);

    boolean interceptTargetRequest(MessageContext axisCtx);

    void targetRequest(ByteBuffer buffer, MessageContext axisCtx);

    boolean interceptTargetResponse(MessageContext axisCtx);

    boolean targetResponse(ByteBuffer buffer, MessageContext axisCtx);

    boolean interceptSourceResponse(MessageContext axisCtx);

    void sourceResponse(ByteBuffer buffer, MessageContext axisCtx);

    /**
     * Add a handler property
     *
     * @param name  property name
     * @param value property value
     */
    void addProperty(String name, Object value);

    /**
     * Get all handler properties
     *
     * @return Map of handler properties
     */
    Map getProperties();

    /**
     * Get the name of the handler
     *
     * @return handler name
     */
    String getName();

    /**
     * Set the handler name
     *
     * @param name handler name
     */
    void setName(String name);

}
