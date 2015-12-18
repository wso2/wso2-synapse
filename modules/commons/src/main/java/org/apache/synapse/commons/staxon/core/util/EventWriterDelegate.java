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
package org.apache.synapse.commons.staxon.core.util;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

/**
 * Filter an {@link XMLEventWriter}.
 * Counterpart to {@link EventReaderDelegate}.
 */
public class EventWriterDelegate implements XMLEventWriter {
    private XMLEventWriter parent;

    public EventWriterDelegate() {
        super();
    }

    public EventWriterDelegate(XMLEventWriter parent) {
        this.parent = parent;
    }

    public XMLEventWriter getParent() {
        return parent;
    }

    public void setParent(XMLEventWriter parent) {
        this.parent = parent;
    }

    @Override
    public void flush() throws XMLStreamException {
        parent.flush();
    }

    @Override
    public void close() throws XMLStreamException {
        parent.close();
    }

    @Override
    public void add(XMLEvent event) throws XMLStreamException {
        parent.add(event);
    }

    @Override
    public void add(XMLEventReader reader) throws XMLStreamException {
        parent.add(reader);
    }

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
        return parent.getPrefix(uri);
    }

    @Override
    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        parent.setPrefix(prefix, uri);
    }

    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {
        parent.setDefaultNamespace(uri);
    }

    @Override
    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
        parent.setNamespaceContext(context);
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return parent.getNamespaceContext();
    }
}
