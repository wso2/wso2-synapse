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

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.apache.synapse.commons.staxon.core.util.EventWriterDelegate;

/**
 * Pretty printing XML event writer.
 */
public class PrettyXMLEventWriter extends EventWriterDelegate {
    private final PrettyXMLWhitespaceHandler handler;

    /**
     * Create instance using default indentation (\t) and line separator (\n).
     *
     * @param writer parent writer
     */
    public PrettyXMLEventWriter(XMLEventWriter writer) {
        this(writer, "\t", "\n");
    }

    /**
     * Create instance.
     *
     * @param writer      parent writer
     * @param indentation line indentation
     * @param newline     line separator
     */
    public PrettyXMLEventWriter(XMLEventWriter writer, String indentation, String newline) {
        super(writer);
        this.handler = new PrettyXMLWhitespaceHandler(writer, indentation, newline);
    }

    @Override
    public void setParent(XMLEventWriter parent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(XMLEventReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            add(reader.nextEvent());
        }
    }

    @Override
    public void add(XMLEvent event) throws XMLStreamException {
        switch (event.getEventType()) {
            case XMLStreamConstants.START_DOCUMENT:
                handler.preStartDocument();
                super.add(event);
                handler.postStartDocument();
                break;
            case XMLStreamConstants.START_ELEMENT:
                handler.preStartElement();
                super.add(event);
                handler.postStartElement();
                break;
            case XMLStreamConstants.END_ELEMENT:
                handler.preEndElement();
                super.add(event);
                handler.postEndElement();
                break;
            case XMLStreamConstants.CHARACTERS:
            case XMLStreamConstants.CDATA:
                handler.preCharacters();
                super.add(event);
                handler.postCharacters();
                break;
            case XMLStreamConstants.COMMENT:
                handler.preComment();
                super.add(event);
                handler.postComment();
                break;
            case XMLStreamConstants.PROCESSING_INSTRUCTION:
                handler.preProcessingInstruction();
                super.add(event);
                handler.postProcessingInstruction();
                break;
            default:
                super.add(event);
        }
    }
}
