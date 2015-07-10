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
package org.apache.synapse.commons.staxon.core.xml;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.synapse.commons.staxon.core.base.AbstractXMLOutputFactory;
import org.apache.synapse.commons.staxon.core.event.SimpleXMLEventWriter;

public class SimpleXMLOutputFactory extends AbstractXMLOutputFactory {
    public SimpleXMLOutputFactory() {
        /*
         * initialize properties
		 */
        super.setProperty(IS_REPAIRING_NAMESPACES, Boolean.FALSE);
    }

    @Override
    public SimpleXMLStreamWriter createXMLStreamWriter(OutputStream stream, String encoding) throws XMLStreamException {
        try {
            return createXMLStreamWriter(new OutputStreamWriter(stream, encoding));
        } catch (UnsupportedEncodingException e) {
            throw new XMLStreamException(e);
        }
    }

    @Override
    public SimpleXMLStreamWriter createXMLStreamWriter(Writer stream) throws XMLStreamException {
        return new SimpleXMLStreamWriter(stream, Boolean.TRUE.equals(getProperty(IS_REPAIRING_NAMESPACES)));
    }

    @Override
    public SimpleXMLStreamWriter createXMLStreamWriter(OutputStream stream) throws XMLStreamException {
        return createXMLStreamWriter(stream, "UTF-8");
    }

    @Override
    public XMLEventWriter createXMLEventWriter(XMLStreamWriter writer) throws XMLStreamException {
        return new SimpleXMLEventWriter(writer);
    }

    @Override
    public void setProperty(String name, Object value) throws IllegalArgumentException {
        if (XMLOutputFactory.IS_REPAIRING_NAMESPACES.equals(name)) {
            if (!getProperty(name).equals(value)) {
                throw new IllegalArgumentException("Cannot change property: " + name);
            }
        } else if (super.isPropertySupported(name)) {
            super.setProperty(name, value);
        } else {
            throw new IllegalArgumentException("Unsupported property: " + name);
        }
    }
}
