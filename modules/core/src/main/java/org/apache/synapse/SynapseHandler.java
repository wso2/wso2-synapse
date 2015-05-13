/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse;

import java.util.Map;


/**
 * Interface For Synapse Handlers
 * <p/>
 * Synapse Handlers are invoked when a message received to the mediation engine or a message sent out
 * from the engine.
 * <p/>
 * When a message received to the engine, handles are invoked just before the mediation flow
 * When a message is sent out from the engine, handlers are invoked just after the mediation flow
 * There are two requests coming into the engine and two responses going out from the engine.
 * For all four messages relevant method is invoked
 */
public interface SynapseHandler {

    /**
     * Handle request message coming into the engine
     *
     * @param synCtx incoming request message context
     * @return whether mediation flow should continue
     */
    public boolean handleRequestInFlow(MessageContext synCtx);

    /**
     * Handle request message going out from the engine
     *
     * @param synCtx outgoing request message context
     * @return whether mediation flow should continue
     */
    public boolean handleRequestOutFlow(MessageContext synCtx);

    /**
     * Handle response message coming into the engine
     *
     * @param synCtx incoming response message context
     * @return whether mediation flow should continue
     */
    public boolean handleResponseInFlow(MessageContext synCtx);

    /**
     * Handle response message going out from the engine
     *
     * @param synCtx outgoing response message context
     * @return whether mediation flow should continue
     */
    public boolean handleResponseOutFlow(MessageContext synCtx);

    /**
     * Add a handler property
     *
     * @param name  property name
     * @param value property value
     */
    public void addProperty(String name, Object value);

    /**
     * Get all handler properties
     *
     * @return Map of handler properties
     */
    public Map getProperties();

    /**
     * Get the name of the handler
     *
     * @return handler name
     */
    public String getName();

    /**
     * Set the handler name
     *
     * @param name handler name
     */
    public void setName(String name);

}
