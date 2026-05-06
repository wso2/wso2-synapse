/*
 *  Copyright (c) 2024, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.transport.passthru.vt;

import org.apache.axis2.context.MessageContext;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.util.RelayUtils;
import org.apache.synapse.transport.util.MessageHandler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.stream.XMLStreamException;

/**
 * Message handler for VT passthrough streams.
 */
public class VTMessageHandler implements MessageHandler {

    @Override
    public InputStream getMessageDataStream(MessageContext context) throws IOException {

        if (context.getProperty(PassThroughConstants.BUFFERED_INPUT_STREAM) != null) {
            BufferedInputStream bufferedInputStream =
                    (BufferedInputStream) context.getProperty(PassThroughConstants.BUFFERED_INPUT_STREAM);
            try {
                bufferedInputStream.reset();
                bufferedInputStream.mark(0);
            } catch (Exception ignored) {
                // Ignore reset errors and let the caller handle stream consumption.
            }
            return bufferedInputStream;
        }

        VTInputStreamPipe pipe = getVTPipe(context);
        if (pipe == null || pipe.getInputStream() == null) {
            return null;
        }

        BufferedInputStream bufferedInputStream = new BufferedInputStream(pipe.getInputStream());
        bufferedInputStream.mark(Integer.MAX_VALUE);
        context.setProperty(PassThroughConstants.BUFFERED_INPUT_STREAM, bufferedInputStream);
        return bufferedInputStream;
    }

    @Override
    public void buildMessage(MessageContext messageContext) throws XMLStreamException, IOException {

        buildMessage(messageContext, false);
    }

    @Override
    public void buildMessage(MessageContext messageContext, boolean earlyBuild) throws XMLStreamException, IOException {

        InputStream inputStream = getMessageDataStream(messageContext);
        if (inputStream == null) {
            return;
        }

        messageContext.removeProperty(VTConstants.VT_STREAM_PIPE);
        messageContext.removeProperty(PassThroughConstants.NO_ENTITY_BODY);
        RelayUtils.buildMessage(messageContext, earlyBuild, inputStream);
    }

    private VTInputStreamPipe getVTPipe(MessageContext context) {

        Object pipe = context.getProperty(VTConstants.VT_STREAM_PIPE);
        if (pipe instanceof VTInputStreamPipe) {
            return (VTInputStreamPipe) pipe;
        }

        return null;
    }
}
