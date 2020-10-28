/**
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.commons.json;

import org.apache.axiom.om.*;
import org.apache.axiom.om.impl.llom.OMSourcedElementImpl;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.stream.XMLStreamException;
import java.io.OutputStream;
import java.net.URL;

public final class JsonFormatter implements MessageFormatter {
    private static final Log logger = LogFactory.getLog(JsonFormatter.class.getName());

    public byte[] getBytes(MessageContext messageContext, OMOutputFormat omOutputFormat)
            throws AxisFault {
        OMElement element = messageContext.getEnvelope().getBody().getFirstElement();
        byte[] json;
        if (element == null) {
            json = new JsonStreamFormatter().getBytes(messageContext, omOutputFormat);
        } else {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            JsonUtil.writeAsJson(element, outputStream);
            json = outputStream.toByteArray();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("#getBytes. Converted XML payload to JSON byte array. MessageID: " + messageContext.getMessageID());
        }
        return json;
    }

    public void writeTo(MessageContext messageContext, OMOutputFormat omOutputFormat,
                        OutputStream outputStream, boolean preserve) throws AxisFault {
        OMElement element = messageContext.getEnvelope().getBody().getFirstElement();
        if (element == null) {
            if (preserve) {
                messageContext.setProperty(JsonUtil.PRESERVE_JSON_STREAM, true);
            }
            JsonUtil.writeAsJson(messageContext, outputStream);
            if (logger.isDebugEnabled()) {
                logger.debug("#writeTo. Wrote JSON stream to output stream. MessageID: " + messageContext.getMessageID());
            }
            return;
        }
        if (element instanceof OMSourcedElementImpl) {
            try {
                OMDataSource dataSource = ((OMSourcedElementImpl) element).getDataSource();
                if (dataSource instanceof JsonDataSource) {
                    dataSource.serialize(outputStream, null);
                    if (logger.isDebugEnabled()) {
                        logger.debug("#writeTo. Wrote JSON DataSource to output stream. MessageID: " + messageContext.getMessageID());
                    }
                    return;
                }
            } catch (XMLStreamException e) {
                logger.error("#writeTo. Could not write JSON message. MessageID: "
                        + messageContext.getMessageID() + ". Error>>> " + e.getLocalizedMessage());
                throw new AxisFault("Could not Write JSON message.", e);
            }
        }
        JsonUtil.writeAsJson(element, outputStream);
        if (logger.isDebugEnabled()) {
            logger.debug("#writeTo. Converted XML payload to JSON output stream. MessageID: " + messageContext.getMessageID());
        }
    }

    /**
     * @param element      XML element
     * @param outputStream Output Stream to write the converted JSON representation.
     * @throws AxisFault
     * @deprecated Use {@link org.apache.synapse.commons.json.JsonUtil#writeAsJson(org.apache.axiom.om.OMElement, java.io.OutputStream)}
     */
    public static void toJson(OMElement element, OutputStream outputStream) throws AxisFault {
        JsonUtil.writeAsJson(element.cloneOMElement(), outputStream);
    }

    public String getContentType(MessageContext messageContext, OMOutputFormat format,
                                 String soapActionString) {
        String contentType = (String) messageContext.getProperty(Constants.Configuration.CONTENT_TYPE);
        String encoding = format.getCharSetEncoding();
        if (contentType == null) {
            contentType = (String) messageContext.getProperty(Constants.Configuration.MESSAGE_TYPE);
        }
        String setEncoding = (String) messageContext
                .getProperty(org.apache.synapse.commons.json.Constants.SET_CONTENT_TYPE_CHARACTER_ENCODING);
        // If the encoding taken from the OMOutputFormat is not null, "setCharacterEncoding" property is not false,
        // and ContentType doesn't already contain a character encoding
        // we append the encoding taken from OMOutputFormat.
        // The default value for encoding coming from OMOutputFormat is UTF-8.
        // The last condition is to avoid two character encodings being appended in case ContentType
        // already contains a character encoding.
        // This fixes ESBJAVA-4940 and "setCharacterEncoding" property was introduced for this.
        if (encoding != null && !"false".equals(setEncoding)
                && contentType != null && !contentType.contains("charset")) {
            contentType += "; charset=" + encoding;
        }
        return contentType;
    }

    public URL getTargetAddress(MessageContext messageContext, OMOutputFormat omOutputFormat,
                                URL url) throws AxisFault {
        if (logger.isDebugEnabled()) {
            logger.debug("#getTargetAddress. Not implemented. #getTargetAddress()");
        }
        return url;
    }

    public String formatSOAPAction(MessageContext messageContext, OMOutputFormat omOutputFormat,
                                   String s) {
        if (logger.isDebugEnabled()) {
            logger.debug("#formatSOAPAction. Not implemented. #formatSOAPAction()");
        }
        return null;
    }


}
