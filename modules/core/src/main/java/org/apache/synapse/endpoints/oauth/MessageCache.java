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

import org.apache.synapse.MessageContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Message Cache Implementation
 */
public class MessageCache {

    private static final MessageCache instance = new MessageCache();

    private final Map<String, MessageContext> messageContextMap = new ConcurrentHashMap<>();

    private MessageCache() {

    }

    /**
     * Get MessageCache Instance
     *
     * @return MessageCache
     */
    public static MessageCache getInstance() {

        return instance;
    }

    /**
     * Add a message context to the cache
     *
     * @param id     id of the message context
     * @param synCtx MessageContext object
     */
    public void addMessageContext(String id, MessageContext synCtx) {

        messageContextMap.put(id, synCtx);
    }

    /**
     * Remove a MessageContext from the cache and return it
     *
     * @param id id of the message context
     * @return MessageContext object
     */
    public MessageContext removeMessageContext(String id) {

        return messageContextMap.remove(id);
    }
}