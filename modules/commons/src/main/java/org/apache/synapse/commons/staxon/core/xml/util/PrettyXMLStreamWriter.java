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
package org.apache.synapse.commons.staxon.core.xml.util;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.synapse.commons.staxon.core.util.StreamWriterDelegate;

/**
 * Pretty printing XML stream writer.
 */
public class PrettyXMLStreamWriter extends StreamWriterDelegate {
    private final PrettyXMLWhitespaceHandler handler;

    /**
     * Create instance using default indentation (\t) and line separator (\n).
     *
     * @param writer parent writer
     */
    public PrettyXMLStreamWriter(XMLStreamWriter writer) {
        this(writer, "\t", "\n");
    }

    /**
     * Create instance.
     *
     * @param writer      parent writer
     * @param indentation line indentation
     * @param newline     line separator
     */
    public PrettyXMLStreamWriter(XMLStreamWriter writer, String indentation, String newline) {
        super(writer);
        this.handler = new PrettyXMLWhitespaceHandler(writer, indentation, newline);
    }

    @Override
    public void setParent(XMLStreamWriter parent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeStartDocument() throws XMLStreamException {
        handler.preStartDocument();
        super.writeStartDocument();
        handler.postStartDocument();
    }

    @Override
    public void writeStartDocument(String version) throws XMLStreamException {
        handler.preStartDocument();
        super.writeStartDocument(version);
        handler.postStartDocument();
    }

    @Override
    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        handler.preStartDocument();
        super.writeStartDocument(encoding, version);
        handler.postStartDocument();
    }

    @Override
    public void writeStartElement(String localName) throws XMLStreamException {
        handler.preStartElement();
        super.writeStartElement(localName);
        handler.postStartElement();
    }

    @Override
    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        handler.preStartElement();
        super.writeStartElement(namespaceURI, localName);
        handler.postStartElement();
    }

    @Override
    public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        handler.preStartElement();
        super.writeStartElement(prefix, localName, namespaceURI);
        handler.postStartElement();
    }

    @Override
    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        handler.preEmptyELement();
        super.writeEmptyElement(namespaceURI, localName);
        handler.postEmptyELement();
    }

    @Override
    public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        handler.preEmptyELement();
        super.writeEmptyElement(prefix, localName, namespaceURI);
        handler.postEmptyELement();
    }

    @Override
    public void writeEmptyElement(String localName) throws XMLStreamException {
        handler.preEmptyELement();
        super.writeEmptyElement(localName);
        handler.postEmptyELement();
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        handler.preEndElement();
        super.writeEndElement();
        handler.postEndElement();
    }

    @Override
    public void writeCData(String data) throws XMLStreamException {
        handler.preCharacters();
        super.writeCData(data);
        handler.postCharacters();
    }

    @Override
    public void writeCharacters(String text) throws XMLStreamException {
        handler.preCharacters();
        super.writeCharacters(text);
        handler.postCharacters();
    }

    @Override
    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException {
        handler.preCharacters();
        super.writeCharacters(text, start, len);
        handler.postCharacters();
    }

    @Override
    public void writeComment(String data) throws XMLStreamException {
        handler.preComment();
        super.writeComment(data);
        handler.postComment();
    }

    @Override
    public void writeProcessingInstruction(String target) throws XMLStreamException {
        handler.preProcessingInstruction();
        super.writeProcessingInstruction(target);
        handler.postProcessingInstruction();
    }

    @Override
    public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
        handler.preProcessingInstruction();
        super.writeProcessingInstruction(target, data);
        handler.postProcessingInstruction();
    }
}
