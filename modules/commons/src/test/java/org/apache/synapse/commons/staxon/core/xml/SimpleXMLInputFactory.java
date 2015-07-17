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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.synapse.commons.staxon.core.base.AbstractXMLInputFactory;
import org.apache.synapse.commons.staxon.core.event.SimpleXMLEventReader;
import org.apache.synapse.commons.staxon.core.event.SimpleXMLFilteredEventReader;

public class SimpleXMLInputFactory extends AbstractXMLInputFactory {
    public SimpleXMLInputFactory() {
        /*
         * initialize properties
		 */
        super.setProperty(IS_COALESCING, Boolean.FALSE);
        super.setProperty(IS_NAMESPACE_AWARE, Boolean.TRUE);
        super.setProperty(IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
        super.setProperty(IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        super.setProperty(IS_VALIDATING, Boolean.FALSE);
        super.setProperty(SUPPORT_DTD, Boolean.FALSE);
    }

    @Override
    public SimpleXMLStreamReader createXMLStreamReader(InputStream stream, String encoding) throws XMLStreamException {
        try {
            return createXMLStreamReader(new InputStreamReader(stream, encoding));
        } catch (UnsupportedEncodingException e) {
            throw new XMLStreamException(e);
        }
    }

    @Override
    public SimpleXMLStreamReader createXMLStreamReader(String systemId, InputStream stream) throws XMLStreamException {
        return createXMLStreamReader(stream);
    }

    @Override
    public SimpleXMLStreamReader createXMLStreamReader(String systemId, Reader reader) throws XMLStreamException {
        return createXMLStreamReader(reader);
    }

    @Override
    public SimpleXMLStreamReader createXMLStreamReader(Reader reader) throws XMLStreamException {
        return new SimpleXMLStreamReader(reader);
    }

    @Override
    public SimpleXMLStreamReader createXMLStreamReader(InputStream stream) throws XMLStreamException {
        return createXMLStreamReader(stream, "UTF-8");
    }

    @Override
    public XMLEventReader createXMLEventReader(XMLStreamReader reader) throws XMLStreamException {
        if (getEventAllocator() == null) {
            return new SimpleXMLEventReader(reader);
        } else {
            return new SimpleXMLEventReader(reader, getEventAllocator().newInstance());
        }
    }

    @Override
    public XMLEventReader createFilteredReader(XMLEventReader reader, EventFilter filter) throws XMLStreamException {
        return new SimpleXMLFilteredEventReader(reader, filter);
    }

    @Override
    public void setXMLResolver(XMLResolver resolver) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setXMLReporter(XMLReporter reporter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProperty(String name, Object value) throws IllegalArgumentException {
        if (IS_NAMESPACE_AWARE.equals(name)) {
            if (!getProperty(name).equals(value)) {
                throw new IllegalArgumentException("Cannot change property: " + name);
            }
        } else if (IS_REPLACING_ENTITY_REFERENCES.equals(name)) {
            if (!getProperty(name).equals(value)) {
                throw new IllegalArgumentException("Cannot change property: " + name);
            }
        } else if (IS_SUPPORTING_EXTERNAL_ENTITIES.equals(name)) {
            if (!getProperty(name).equals(value)) {
                throw new IllegalArgumentException("Cannot change property: " + name);
            }
        } else if (IS_VALIDATING.equals(name)) {
            if (!getProperty(name).equals(value)) {
                throw new IllegalArgumentException("Cannot change property: " + name);
            }
        } else if (SUPPORT_DTD.equals(name)) {
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
