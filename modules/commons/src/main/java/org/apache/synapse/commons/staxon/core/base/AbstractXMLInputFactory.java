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
package org.apache.synapse.commons.staxon.core.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;
import javax.xml.stream.util.XMLEventAllocator;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

/**
 * Abstract XML input factory.
 */
public abstract class AbstractXMLInputFactory extends XMLInputFactory {
    private XMLEventAllocator allocator;
    private XMLResolver resolver;
    private XMLReporter reporter;

    private boolean coalescing;
    private boolean namespaceAware;
    private boolean replacingEntityReferences;
    private boolean supportingExternalEntities;
    private boolean validating;
    private boolean supportDTD;

    @Override
    public XMLStreamReader createXMLStreamReader(Source source) throws XMLStreamException {
        if (source instanceof StreamSource) {
            StreamSource streamSource = (StreamSource) source;
            InputStream input = streamSource.getInputStream();
            if (input != null) {
                if (streamSource.getSystemId() != null) {
                    return createXMLStreamReader(streamSource.getSystemId(), input);
                } else {
                    return createXMLStreamReader(input);
                }
            }
            Reader reader = streamSource.getReader();
            if (reader != null) {
                if (streamSource.getSystemId() != null) {
                    return createXMLStreamReader(streamSource.getSystemId(), reader);
                } else {
                    return createXMLStreamReader(reader);
                }
            }
            if (streamSource.getSystemId() != null) {
                try {
                    final InputStream stream = new URI(source.getSystemId()).toURL().openStream();
                    return new StreamReaderDelegate(createXMLStreamReader(streamSource.getSystemId(), stream)) {
                        /*
                         * Close underlying stream, otherwise it could never be done
                         * @see javax.xml.stream.util.StreamReaderDelegate#close()
                         */
                        @Override
                        public void close() throws XMLStreamException {
                            super.close();
                            try {
                                stream.close();
                            } catch (IOException e) {
                                // ignore
                            }
                        }
                    };
                } catch (URISyntaxException e) {
                    throw new XMLStreamException("Cannot parse system id for reading: " + source.getSystemId(), e);
                } catch (IOException e) {
                    throw new XMLStreamException("Cannot open system id as URL for reading: " + source.getSystemId(), e);
                }
            } else {
                throw new XMLStreamException("Invalid stream source: none of input, reader, systemId set");
            }
        }
        throw new XMLStreamException("Unsupported source type: " + source.getClass());
    }

    @Override
    public XMLEventReader createXMLEventReader(Reader reader) throws XMLStreamException {
        return createXMLEventReader(createXMLStreamReader(reader));
    }

    @Override
    public XMLEventReader createXMLEventReader(String systemId, Reader reader) throws XMLStreamException {
        return createXMLEventReader(createXMLStreamReader(systemId, reader));
    }

    @Override
    public XMLEventReader createXMLEventReader(Source source) throws XMLStreamException {
        return createXMLEventReader(createXMLStreamReader(source));
    }

    @Override
    public XMLEventReader createXMLEventReader(InputStream stream) throws XMLStreamException {
        return createXMLEventReader(createXMLStreamReader(stream));
    }

    @Override
    public XMLEventReader createXMLEventReader(InputStream stream, String encoding) throws XMLStreamException {
        return createXMLEventReader(createXMLStreamReader(stream, encoding));
    }

    @Override
    public XMLEventReader createXMLEventReader(String systemId, InputStream stream) throws XMLStreamException {
        return createXMLEventReader(createXMLStreamReader(systemId, stream));
    }

    @Override
    public XMLStreamReader createFilteredReader(XMLStreamReader reader, final StreamFilter filter) throws XMLStreamException {
        return new StreamReaderDelegate(reader) {
            @Override
            public boolean hasNext() throws XMLStreamException {
                while (super.hasNext()) {
                    if (filter.accept(getParent())) {
                        return true;
                    }
                    super.next();
                }
                return false;
            }

            @Override
            public int next() throws XMLStreamException {
                if (hasNext()) {
                    return getParent().getEventType();
                }
                throw new IllegalStateException("No more events");
            }
        };
    }

    @Override
    public XMLEventAllocator getEventAllocator() {
        return allocator;
    }

    @Override
    public void setEventAllocator(XMLEventAllocator allocator) {
        this.allocator = allocator;
    }

    @Override
    public XMLResolver getXMLResolver() {
        return resolver;
    }

    @Override
    public void setXMLResolver(XMLResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public XMLReporter getXMLReporter() {
        return reporter;
    }

    @Override
    public void setXMLReporter(XMLReporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public boolean isPropertySupported(String name) {
        if (ALLOCATOR.equals(name)) {
            return true;
        } else if (IS_COALESCING.equals(name)) {
            return true;
        } else if (IS_NAMESPACE_AWARE.equals(name)) {
            return true;
        } else if (IS_REPLACING_ENTITY_REFERENCES.equals(name)) {
            return true;
        } else if (IS_SUPPORTING_EXTERNAL_ENTITIES.equals(name)) {
            return true;
        } else if (IS_VALIDATING.equals(name)) {
            return true;
        } else if (REPORTER.equals(name)) {
            return true;
        } else if (RESOLVER.equals(name)) {
            return true;
        } else if (SUPPORT_DTD.equals(name)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        if (ALLOCATOR.equals(name)) {
            return allocator;
        } else if (IS_COALESCING.equals(name)) {
            return Boolean.valueOf(coalescing);
        } else if (IS_NAMESPACE_AWARE.equals(name)) {
            return Boolean.valueOf(namespaceAware);
        } else if (IS_REPLACING_ENTITY_REFERENCES.equals(name)) {
            return Boolean.valueOf(replacingEntityReferences);
        } else if (IS_SUPPORTING_EXTERNAL_ENTITIES.equals(name)) {
            return Boolean.valueOf(supportingExternalEntities);
        } else if (IS_VALIDATING.equals(name)) {
            return Boolean.valueOf(validating);
        } else if (REPORTER.equals(name)) {
            return reporter;
        } else if (RESOLVER.equals(name)) {
            return resolver;
        } else if (SUPPORT_DTD.equals(name)) {
            return Boolean.valueOf(supportDTD);
        } else {
            throw new IllegalArgumentException("Unsupported property: " + name);
        }
    }

    @Override
    public void setProperty(String name, Object value) throws IllegalArgumentException {
        if (ALLOCATOR.equals(name)) {
            allocator = (XMLEventAllocator) value;
        } else if (IS_COALESCING.equals(name)) {
            coalescing = ((Boolean) value).booleanValue();
        } else if (IS_NAMESPACE_AWARE.equals(name)) {
            namespaceAware = ((Boolean) value).booleanValue();
        } else if (IS_REPLACING_ENTITY_REFERENCES.equals(name)) {
            replacingEntityReferences = ((Boolean) value).booleanValue();
        } else if (IS_SUPPORTING_EXTERNAL_ENTITIES.equals(name)) {
            supportingExternalEntities = ((Boolean) value).booleanValue();
        } else if (IS_VALIDATING.equals(name)) {
            validating = ((Boolean) value).booleanValue();
        } else if (REPORTER.equals(name)) {
            reporter = (XMLReporter) value;
        } else if (RESOLVER.equals(name)) {
            resolver = (XMLResolver) value;
        } else if (SUPPORT_DTD.equals(name)) {
            supportDTD = ((Boolean) value).booleanValue();
        } else {
            throw new IllegalArgumentException("Unsupported property: " + name);
        }
    }
}

