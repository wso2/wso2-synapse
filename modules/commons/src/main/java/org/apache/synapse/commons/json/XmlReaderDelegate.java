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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

/**
 * Detaches the special prefix applied by the JsonReaderDelegate.<br/>
 * Hence this returns the original key value that was read in.
 */
final class XmlReaderDelegate extends StreamReaderDelegate {
    private static final Log logger = LogFactory.getLog(XmlReaderDelegate.class.getName());

    public XmlReaderDelegate(XMLStreamReader reader) {
        super(reader);
        /** Possible reader implementations include;
            com.ctc.wstx.sr.ValidatingStreamReader
            de.odysseus.staxon.json.JsonXMLStreamReader
            com.sun.org.apache.xerces.internal.impl.XMLStreamReaderImpl
         */
        if (logger.isDebugEnabled()) {
            logger.debug("#XmlReaderDelegate. Setting XMLStreamReader: " + reader.getClass().getName());
        }
    }

    public String getLocalName() {
        String localName = super.getLocalName();
        String newName = localName;
        if (localName == null || "".equals(localName)) {
            return localName;
        }
        if (localName.charAt(0) == Constants.C_USOCRE) {
            if (localName.startsWith(Constants.PRECEDING_DIGIT)) {
                newName = localName.substring(Constants.PRECEDING_DIGIT.length(),
                                           localName.length());
            } else if (localName.startsWith(Constants.PRECEDING_DOLLOR)) {
                newName = (char) Constants.C_DOLLOR + localName.substring(Constants.PRECEDING_DOLLOR.length(),
                                           localName.length());
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("#getLocalName. old=" + localName + ", new=" + newName);
        }
        return newName;
    }

    public QName getName() {
        QName qName = super.getName();
        String localName = qName.getLocalPart();
        QName newName = qName;
        if (localName == null || "".equals(localName)) {
            return qName;
        }
        if (localName.charAt(0) == Constants.C_USOCRE) {
            if (localName.startsWith(Constants.PRECEDING_DIGIT)) {
                localName =  localName.substring(Constants.PRECEDING_DIGIT.length(),
                                                 localName.length());
                newName = new QName(qName.getNamespaceURI(), localName, qName.getPrefix());
            } else if (localName.startsWith(Constants.PRECEDING_DOLLOR)) {
                localName =  (char) Constants.C_DOLLOR + localName.substring(Constants.PRECEDING_DOLLOR.length(),
                                                 localName.length());
                newName = new QName(qName.getNamespaceURI(), localName, qName.getPrefix());
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("#getName. old=" + localName + ", new=" + newName.getLocalPart());
        }
        return newName;
    }
}
