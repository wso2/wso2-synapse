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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Sample stream interceptor, which intercepts source and target request to print the stream passing via the engine.
 * <pre>
 * {@code
 * <interceptors xmlns:svns="http://org.wso2.securevault/configuration">
 *     <interceptor class="org.apache.synapse.transport.passthru.SampleStreamInterceptor">
 *         <parameter name="charset" value="ISO-8859-1"/>
 *         <parameter name="enableInterception" value="true"/>
 *     </interceptor>
 * </interceptors>
 * }
 * </pre>
 */
public class SampleStreamInterceptor extends DefaultStreamInterceptor {

    private static final Log log = LogFactory.getLog(SampleStreamInterceptor.class);

    private static final String INPUT = ">>>";
    private static final String OUTPUT = "<<<";

    private String charset = Charset.defaultCharset().toString();
    private boolean enableInterception = false;

    @Override
    public boolean interceptSourceRequest(MessageContext axisCtx) {
        return enableInterception;
    }

    @Override
    public boolean sourceRequest(ByteBuffer buffer, MessageContext ctx) {
        printStream(buffer, INPUT);
        return true;
    }

    @Override
    public boolean interceptTargetRequest(MessageContext axisCtx) {
        return enableInterception;
    }

    @Override
    public void targetRequest(ByteBuffer buffer, MessageContext ctx) {
        printStream(buffer, OUTPUT);
    }

    @Override
    public boolean interceptTargetResponse(MessageContext axisCtx) {
        return enableInterception;
    }

    @Override
    public boolean targetResponse(ByteBuffer buffer, MessageContext ctx) {
        printStream(buffer, INPUT);
        return true;
    }

    @Override
    public boolean interceptSourceResponse(MessageContext axisCtx) {
        return enableInterception;
    }

    @Override
    public void sourceResponse(ByteBuffer buffer, MessageContext ctx) {
        printStream(buffer, OUTPUT);
    }

    private void printStream(ByteBuffer stream, String direction) {

        Charset charsetValue = Charset.forName(this.charset);
        String text = charsetValue.decode(stream).toString();
        log.info(direction + " " + text);
    }

    public void setCharset(String charset) {

        this.charset = charset;
        if (log.isDebugEnabled()) {
            log.debug("Charset : " + charset);
        }
    }

    public void setEnableInterception(boolean enableInterception) {
        this.enableInterception = enableInterception;
    }
}
