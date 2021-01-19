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
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

/**
 * Default class implementing the methods of {@link StreamInterceptor}. A stream interceptor can be written by
 * extending and overding the necessary methods of this class.
 * For an example see{@link LoggingStreamInterceptor}
 */
public abstract class DefaultStreamInterceptor implements StreamInterceptor {

    private Properties properties = new Properties();

    public boolean interceptSourceRequest(MessageContext axisCtx) {
        return false;
    }

    public boolean sourceRequest(ByteBuffer buffer, MessageContext axisCtx) {
        return true;
    }

    public boolean interceptTargetRequest(MessageContext axisCtx) {
        return false;
    }

    public void targetRequest(ByteBuffer buffer, MessageContext axisCtx) {

    }

    public boolean interceptTargetResponse(MessageContext axisCtx) {
        return false;
    }

    public boolean targetResponse(ByteBuffer buffer, MessageContext axisCtx) {
        return true;
    }

    public boolean interceptSourceResponse(MessageContext axisCtx) {
        return false;
    }

    public void sourceResponse(ByteBuffer buffer, MessageContext axisCtx) {

    }

    public void addProperty(String name, Object value) {
        properties.put(name, value);
    }

    public Map getProperties() {
        return Collections.unmodifiableCollection(properties);
    }

}
