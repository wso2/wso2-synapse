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
package org.apache.synapse.commons.staxon.core.event;

import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

/**
 * Simple implementation of a filtered {@link XMLEventReader}.
 */
public class SimpleXMLFilteredEventReader extends EventReaderDelegate {
    private final EventFilter filter;

    private int currentEventType = -1;

    public SimpleXMLFilteredEventReader(XMLEventReader reader, EventFilter filter) {
        super(reader);
        this.filter = filter;
    }

    @Override
    public void setParent(XMLEventReader reader) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasNext() {
        try {
            while (getParent().hasNext()) {
                if (filter.accept(getParent().peek())) {
                    return true;
                }
                getParent().nextEvent();
            }
            return false;
        } catch (XMLStreamException e) {
            return false;
        }
    }

    @Override
    public XMLEvent peek() throws XMLStreamException {
        return hasNext() ? getParent().peek() : null;
    }

    @Override
    public Object next() {
        try {
            return nextEvent();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public XMLEvent nextEvent() throws XMLStreamException {
        if (hasNext()) {
            XMLEvent event = getParent().nextEvent();
            currentEventType = event.getEventType();
            return event;
        }
        throw new XMLStreamException("no more events");
    }

    @Override
    public String getElementText() throws XMLStreamException {
        if (currentEventType != XMLStreamConstants.START_ELEMENT) {
            throw new XMLStreamException("Expected start element event");
        }
        StringBuilder builder = null;
        String leadText = null;
        while (true) {
            XMLEvent event = nextEvent();
            String data = null;
            switch (event.getEventType()) {
                case XMLStreamConstants.ENTITY_REFERENCE:
                    data = ((EntityReference) event).getName();
                    break;
                case XMLStreamConstants.CDATA:
                case XMLStreamConstants.CHARACTERS:
                    data = event.asCharacters().getData();
                    break;
                case XMLStreamConstants.COMMENT:
                case XMLStreamConstants.PROCESSING_INSTRUCTION:
                case XMLStreamConstants.SPACE:
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    return builder == null ? (leadText == null ? "" : leadText) : builder.toString();
                default:
                    throw new XMLStreamException("Unexpected event type " + currentEventType, event.getLocation());
            }
            if (data != null) {
                if (leadText == null) { // first event?
                    leadText = data;
                } else {
                    if (builder == null) { // second event?
                        builder = new StringBuilder(leadText);
                    }
                    builder.append(data);
                }
            }
        }
    }

    @Override
    public XMLEvent nextTag() throws XMLStreamException {
        while (true) {
            XMLEvent event = nextEvent();
            switch (event.getEventType()) {
                case XMLStreamConstants.START_DOCUMENT:
                    break;
                case XMLStreamConstants.COMMENT:
                case XMLStreamConstants.PROCESSING_INSTRUCTION:
                case XMLStreamConstants.SPACE:
                    break;
                case XMLStreamConstants.CDATA:
                case XMLStreamConstants.CHARACTERS:
                    if (!event.asCharacters().isWhiteSpace()) {
                        throw new XMLStreamException("Encountered non-whitespace text");
                    }
                    break;
                case XMLStreamConstants.START_ELEMENT:
                case XMLStreamConstants.END_ELEMENT:
                    return event;
                default:
                    throw new XMLStreamException("Encountered unexpected event: " + event.getEventType());
            }
        }
    }
}
