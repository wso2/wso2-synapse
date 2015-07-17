/*
 * Copyright 2011, 2012 Odysseus Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.synapse.commons.staxon.core.json.util;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.synapse.commons.staxon.core.util.StreamWriterDelegate;

/**
 * Simple delegate writer which generates <code>&lt;?xml-multiple?></code>
 * processing instructions when writing a sequence of elements matching some
 * element path. Use this class to trigger JSON array start events like this:
 * <pre>
 * XMLOutputFactory factory = new JsonXMLOutputFactory();
 * XMLEventWriter writer = factory.createXMLStreamWriter(...);
 * writer = new XMLMultipleStreamWriter(writer, false, "/alice/bob", ...);
 * </pre>
 */
public class XMLMultipleStreamWriter extends StreamWriterDelegate {
    private final XMLMultipleProcessingInstructionHandler handler;

    /**
     * Create instance.
     *
     * @param parent        delegate
     * @param matchRoot     whether the root element is included in paths
     * @param multiplePaths added via {@link #addMultiplePath(String)}
     */
    public XMLMultipleStreamWriter(XMLStreamWriter parent, boolean matchRoot, String... multiplePaths) throws XMLStreamException {
        super(parent);

        this.handler = new XMLMultipleProcessingInstructionHandler(this, matchRoot, false);
        for (String path : multiplePaths) {
            addMultiplePath(path);
        }
    }

    @Override
    public void setParent(XMLStreamWriter parent) {
        throw new UnsupportedOperationException();
    }

    /**
     * Add path to trigger <code>&lt;?xml-multiple?></code> PI.
     * The path may start with <code>'/'</code> and contain local element
     * names, separated by <code>'/'</code>, e.g
     * <code>"/foo/bar"</code>, <code>"foo/bar"</code> or <code>"bar"</code>.
     *
     * @param path multiple path
     * @throws XMLStreamException if the path is invalid
     */
    public void addMultiplePath(String path) throws XMLStreamException {
        handler.addMultiplePath(path);
    }

    @Override
    public void writeEmptyElement(String localName) throws XMLStreamException {
        handler.preEmptyElement(XMLConstants.DEFAULT_NS_PREFIX, localName);
        super.writeEmptyElement(localName);
        handler.postEmptyElement();
    }

    @Override
    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        handler.preEmptyElement(getPrefix(namespaceURI), localName);
        super.writeEmptyElement(namespaceURI, localName);
        handler.postEmptyElement();
    }

    @Override
    public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        handler.preEmptyElement(prefix, localName);
        super.writeEmptyElement(prefix, localName, namespaceURI);
        handler.postEmptyElement();
    }

    @Override
    public void writeStartElement(String localName) throws XMLStreamException {
        handler.preStartElement(XMLConstants.DEFAULT_NS_PREFIX, localName);
        super.writeStartElement(localName);
        handler.postStartElement();
    }

    @Override
    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        handler.preStartElement(getPrefix(namespaceURI), localName);
        super.writeStartElement(namespaceURI, localName);
        handler.postStartElement();
    }

    @Override
    public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        handler.preStartElement(prefix, localName);
        super.writeStartElement(prefix, localName, namespaceURI);
        handler.postStartElement();
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        handler.preEndElement();
        super.writeEndElement();
        handler.postEndElement();
    }
}
