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
 * Interface For Stream Interceptors
 * <p/>
 * Interceptors are invoked when the data is received to the synapse engine or when data is sent out
 * of the engine.
 * <p/>
 * After a connection is established, the data is read chunk wise/ whenever available. The same applies when the data
 * leaves the engine as well. There are two requests coming into the engine and two responses going out from the
 * engine. Interceptors will be invoked in all of these four phases
 */
public interface StreamInterceptor {

    /**
     * Logic to determine whether to intercept the source request
     *
     * @param axisCtx associated axis2MsgCtx of the request
     * @return intercept source request or not
     */
    boolean interceptSourceRequest(MessageContext axisCtx);

    /**
     * Handles the request data coming in to the engine from the client
     *
     * @param buffer  copy of data entering in
     * @param axisCtx associated axis2MsgCtx
     * @return whether to continue reading the data or to close the connection
     */
    boolean sourceRequest(ByteBuffer buffer, MessageContext axisCtx);

    /**
     * Logic to determine whether to intercept the target request
     *
     * @param axisCtx associated axis2MsgCtx of the request
     * @return intercept target request or not
     */
    boolean interceptTargetRequest(MessageContext axisCtx);

    /**
     * Handles the request data leaving out of the engine
     *
     * @param buffer  copy of data being send out
     * @param axisCtx associated axis2MsgCtx
     */
    void targetRequest(ByteBuffer buffer, MessageContext axisCtx);

    /**
     * Logic to determine whether to intercept the target response
     *
     * @param axisCtx associated axis2MsgCtx of the response
     * @return intercept target response or not
     */
    boolean interceptTargetResponse(MessageContext axisCtx);

    /**
     * Handles the response data coming in to the engine from the back end
     *
     * @param buffer  copy of data entering in
     * @param axisCtx associated axis2MsgCtx
     * @return whether to continue reading the data or to close the connection
     */
    boolean targetResponse(ByteBuffer buffer, MessageContext axisCtx);

    /**
     * Logic to determine whether to intercept the source response
     *
     * @param axisCtx associated axis2MsgCtx of the response
     * @return intercept source response or not
     */
    boolean interceptSourceResponse(MessageContext axisCtx);

    /**
     * Handles the response data leaving out of the engine
     *
     * @param buffer  copy of data leaving
     * @param axisCtx associated axis2MsgCtx
     */
    void sourceResponse(ByteBuffer buffer, MessageContext axisCtx);

    /**
     * Add an interceptor property
     *
     * @param name  property name
     * @param value property value
     */
    void addProperty(String name, Object value);

    /**
     * Get all interceptor properties
     *
     * @return Map of handler properties
     */
    Map getProperties();

    /**
     * Get the name of the interceptor
     *
     * @return handler name
     */
    String getName();

    /**
     * Set the interceptor name
     *
     * @param name handler name
     */
    void setName(String name);

}
