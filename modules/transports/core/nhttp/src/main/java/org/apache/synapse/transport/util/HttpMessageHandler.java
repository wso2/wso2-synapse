/*
 *  Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.synapse.transport.util;

import org.apache.axis2.context.MessageContext;
import org.apache.synapse.transport.netty.BridgeConstants;
import org.apache.synapse.transport.netty.util.MessageUtils;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;
import org.wso2.transport.http.netty.message.HttpMessageDataStreamer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Message handler to handle the messages coming through Netty based HTTP transport.
 */
public class HttpMessageHandler implements MessageHandler {

    @Override
    public InputStream getMessageDataStream(MessageContext msgContext) {

        HttpCarbonMessage carbonMessage =
                (HttpCarbonMessage) msgContext.getProperty(BridgeConstants.HTTP_CARBON_MESSAGE);
        if (Objects.isNull(carbonMessage)) {
            return null;
        }

        BufferedInputStream bufferedInputStream;
        if (msgContext.getProperty(PassThroughConstants.BUFFERED_INPUT_STREAM) != null) {
            bufferedInputStream =
                    (BufferedInputStream) msgContext.getProperty(PassThroughConstants.BUFFERED_INPUT_STREAM);
            try {
                bufferedInputStream.reset();
                bufferedInputStream.mark(0);
            } catch (Exception e) {
                //just ignore the error
            }
        } else {
            HttpMessageDataStreamer httpMessageDataStreamer = new HttpMessageDataStreamer(carbonMessage);
            bufferedInputStream = new BufferedInputStream(httpMessageDataStreamer.getInputStream());
            // Multiplied it by two because we always need a bigger read-limit than the buffer size.
            bufferedInputStream.mark(Integer.MAX_VALUE);
            msgContext.setProperty(PassThroughConstants.BUFFERED_INPUT_STREAM, bufferedInputStream);
        }
        return bufferedInputStream;
    }

    @Override
    public void buildMessage(MessageContext messageContext) throws IOException {

        MessageUtils.buildMessage(messageContext);
    }

    @Override
    public void buildMessage(MessageContext messageContext, boolean earlyBuild) throws IOException {

        MessageUtils.buildMessage(messageContext, earlyBuild);
    }
}
