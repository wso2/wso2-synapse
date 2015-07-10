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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;

/**
 * Represent document/element scope. Used to store namespace bindings and
 * attributes, implements {@link NamespaceContext}.
 */
public abstract class AbstractXMLStreamScope implements NamespaceContext {
    class Attr {
        private final String prefix;
        private final String localName;
        private final String namespaceURI;
        private final String value;

        Attr(String prefix, String localName, String namespaceURI, String value) {
            this.prefix = prefix;
            this.localName = localName;
            this.namespaceURI = namespaceURI;
            this.value = value;
        }

        String getPrefix() {
            if (prefix != null) {
                return prefix;
            } else if (XMLConstants.NULL_NS_URI.equals(namespaceURI)) {
                return XMLConstants.DEFAULT_NS_PREFIX;
            } else {
                return AbstractXMLStreamScope.this.getNonEmptyPrefix(namespaceURI);
            }
        }

        String getLocalName() {
            return localName;
        }

        String getNamespaceURI() {
            if (namespaceURI != null) {
                return namespaceURI;
            } else if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
                return XMLConstants.NULL_NS_URI;
            } else {
                return AbstractXMLStreamScope.this.getNamespaceURI(prefix);
            }
        }

        String getValue() {
            return value;
        }

        void verify() throws XMLStreamException {
            if (prefix == null) {
                if (!XMLConstants.NULL_NS_URI.equals(namespaceURI)) {
                    String prefix = AbstractXMLStreamScope.this.getNonEmptyPrefix(namespaceURI);
                    if (prefix == null) {
                        throw new XMLStreamException("No prefix found for attribute namespace: " + namespaceURI);
                    }
                }
            } else if (namespaceURI == null) {
                if (!XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
                    String namespaceURI = AbstractXMLStreamScope.this.getNamespaceURI(prefix);
                    if (namespaceURI == null || XMLConstants.NULL_NS_URI.equals(namespaceURI)) {
                        throw new XMLStreamException("Unbound attribute prefix: " + prefix);
                    }
                }
            } else {
                if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
                    if (!XMLConstants.NULL_NS_URI.equals(namespaceURI)) {
                        throw new XMLStreamException("Illegal namespace for unprefixed attribute: " + namespaceURI);
                    }
                } else if (XMLConstants.NULL_NS_URI.equals(namespaceURI)) {
                    if (!XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
                        throw new XMLStreamException("Illegal prefix for null namespace: " + prefix);
                    }
                } else {
                    if (!AbstractXMLStreamScope.this.getNamespaceURI(prefix).equals(namespaceURI)) {
                        throw new XMLStreamException("Prefix '" + prefix + "' is not bound to: " + namespaceURI);
                    }
                }
            }
        }
    }

    private final NamespaceContext parent;
    private final String prefix;
    private final String localName;
    private final String namespaceURI;

    private String defaultNamespace;
    private List<Attr> attributes;
    private List<Pair<String, String>> prefixes;
    private AbstractXMLStreamScope lastChild;
    private boolean startTagClosed;

    /**
     * Create root scope.
     *
     * @param defaultNamespace
     */
    public AbstractXMLStreamScope(String defaultNamespace) {
        this.parent = null;
        this.prefix = null;
        this.localName = null;
        this.namespaceURI = XMLConstants.NULL_NS_URI;
        this.defaultNamespace = defaultNamespace;
        this.startTagClosed = true;
    }

    /**
     * Create root scope.
     *
     * @param parent root namespace context
     */
    public AbstractXMLStreamScope(NamespaceContext parent) {
        this.parent = parent;
        this.prefix = null;
        this.localName = null;
        this.namespaceURI = XMLConstants.NULL_NS_URI;
        this.defaultNamespace = parent.getNamespaceURI(XMLConstants.NULL_NS_URI);
        this.startTagClosed = true;
    }

    /**
     * Create element scope.
     *
     * @param parent
     * @param prefix
     * @param localName
     */
    public AbstractXMLStreamScope(AbstractXMLStreamScope parent, String prefix, String localName, String namespaceURI) {
        this.parent = parent;
        this.prefix = prefix;
        this.localName = localName;
        this.namespaceURI = namespaceURI;
        this.startTagClosed = false;
        this.defaultNamespace = parent.getNamespaceURI(XMLConstants.NULL_NS_URI);

        parent.lastChild = this;
        parent.startTagClosed = true;
    }

    void addAttribute(String prefix, String localName, String namespaceURI, String value) {
        if (attributes == null) {
            attributes = new LinkedList<Attr>();
        }
        attributes.add(new Attr(prefix, localName, namespaceURI, value));
    }

    List<Attr> getAttributes() {
        return attributes;
    }

    public String getPrefix() {
        return prefix == null ? getPrefix(namespaceURI) : prefix;
    }

    public String getLocalName() {
        return localName;
    }

    public String getNamespaceURI() {
        return namespaceURI == null ? getNamespaceURI(prefix) : namespaceURI;
    }

    public boolean isRoot() {
        return localName == null;
    }

    public AbstractXMLStreamScope getParent() {
        return isRoot() ? null : (AbstractXMLStreamScope) parent;
    }

    public AbstractXMLStreamScope getLastChild() {
        return lastChild;
    }

    public boolean isStartTagClosed() {
        return startTagClosed;
    }

    private void verify() throws XMLStreamException {
        if (prefix == null) {
            if (!XMLConstants.NULL_NS_URI.equals(namespaceURI) && getPrefix(namespaceURI) == null) {
                throw new XMLStreamException("No prefix for namespace URI: " + namespaceURI);
            }
        } else if (namespaceURI == null) {
            if (!XMLConstants.DEFAULT_NS_PREFIX.equals(prefix) && XMLConstants.NULL_NS_URI.equals(getNamespaceURI(prefix))) {
                throw new XMLStreamException("Unbound prefix: " + prefix);
            }
        } else {
            if (!namespaceURI.equals(getNamespaceURI(prefix))) {
                if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
                    throw new XMLStreamException("Prefix required for namespace URI: '" + namespaceURI);
                } else if (XMLConstants.NULL_NS_URI.equals(namespaceURI)) {
                    throw new XMLStreamException("Prefix '" + prefix + "' is bound to: " + getNamespaceURI(prefix));
                } else {
                    throw new XMLStreamException("Prefix '" + prefix + "' is not bound to: " + namespaceURI);
                }
            }
        }
        if (attributes != null) {
            for (Attr attribute : attributes) {
                attribute.verify();
            }
        }
    }

    void setStartTagClosed(boolean startTagClosed) throws XMLStreamException {
        if (startTagClosed) {
            verify();
        }
        this.startTagClosed = startTagClosed;
    }

    private String findNonEmptyPrefix(String namespaceURI, AbstractXMLStreamScope descendent) {
        if (prefixes != null) {
            for (Pair<String, String> pair : prefixes) {
                if (pair.getSecond().equals(namespaceURI)) {
                    if (descendent == this || descendent.getNamespaceURI(pair.getFirst()).equals(namespaceURI)) {
                        return pair.getFirst();
                    }
                }
            }
        }
        if (isRoot()) {
            if (parent == null) {
                return null;
            } else {
                Iterator<?> prefixes = parent.getPrefixes(namespaceURI);
                while (prefixes.hasNext()) {
                    String prefix = prefixes.next().toString();
                    if (!XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
                        if (descendent == this || descendent.getNamespaceURI(prefix).equals(namespaceURI)) {
                            return prefix;
                        }
                    }
                }
                return null;
            }
        } else {
            return getParent().findNonEmptyPrefix(namespaceURI, descendent);
        }
    }

    String getNonEmptyPrefix(String namespaceURI) {
        if (namespaceURI == null) {
            throw new IllegalArgumentException("Namespace URI must not be null");
        } else if (XMLConstants.XML_NS_URI.equals(namespaceURI)) {
            return XMLConstants.XML_NS_PREFIX;
        } else if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI)) {
            return XMLConstants.XMLNS_ATTRIBUTE;
        } else {
            return findNonEmptyPrefix(namespaceURI, this);
        }
    }

    @Override
    public String getPrefix(String namespaceURI) {
        if (XMLConstants.NULL_NS_URI.equals(namespaceURI)) {
            return null;
        } else if (defaultNamespace.equals(namespaceURI)) {
            return XMLConstants.DEFAULT_NS_PREFIX;
        } else {
            return getNonEmptyPrefix(namespaceURI);
        }
    }

    public void setPrefix(String prefix, String namespaceURI) {
        if (prefix == null || namespaceURI == null) {
            throw new IllegalArgumentException("Prefix and namespace URI must not be null");
        }
        if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
            defaultNamespace = namespaceURI;
        } else if (XMLConstants.XML_NS_PREFIX.equals(namespaceURI)) {
            throw new IllegalArgumentException("Cannot bind to prefix: " + prefix);
        } else if (XMLConstants.XMLNS_ATTRIBUTE.equals(namespaceURI)) {
            throw new IllegalArgumentException("Cannot bind to prefix: " + prefix);
        } else {
            if (prefixes == null) {
                prefixes = new LinkedList<Pair<String, String>>();
            } else {
                Iterator<Pair<String, String>> iterator = prefixes.iterator();
                while (iterator.hasNext()) {
                    if (iterator.next().getFirst().equals(prefix)) {
                        iterator.remove();
                    }
                }
            }
            prefixes.add(new Pair<String, String>(prefix, namespaceURI));
        }
    }

    @Override
    public Iterator<String> getPrefixes(final String namespaceURI) {
        if (namespaceURI == null) {
            throw new IllegalArgumentException("Namespace URI must not be null");
        } else if (XMLConstants.XML_NS_URI.equals(namespaceURI)) {
            return Arrays.asList(XMLConstants.XML_NS_PREFIX).iterator();
        } else if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI)) {
            return Arrays.asList(XMLConstants.XMLNS_ATTRIBUTE).iterator();
        } else {
            return new Iterator<String>() {
                int state = 0;
                String next = null;
                Iterator<Pair<String, String>> pairs;
                Iterator<?> above;

                private String next0() {
                    if (state == 0) { // check default
                        state = 1;
                        if (namespaceURI.equals(defaultNamespace)) {
                            return XMLConstants.DEFAULT_NS_PREFIX;
                        }
                    }
                    if (state == 1) { // check pairs
                        if (prefixes != null) {
                            if (pairs == null) {
                                pairs = prefixes.iterator();
                            }
                            while (pairs.hasNext()) {
                                Pair<String, String> pair = pairs.next();
                                if (namespaceURI.equals(pair.getSecond())) {
                                    return pair.getFirst();
                                }
                            }
                        }
                        state = 2;
                    }
                    if (state == 2) { // check above
                        if (parent != null) {
                            if (above == null) {
                                above = parent.getPrefixes(namespaceURI);
                            }
                            while (above.hasNext()) {
                                String prefix = above.next().toString();
                                if (getNamespaceURI(prefix).equals(namespaceURI)) {
                                    return prefix;
                                }
                            }
                        }
                        state = 3;
                    }
                    if (state == 3) { // check out...
                        return null;
                    }
                    throw new IllegalStateException(); // should not happen
                }

                @Override
                public boolean hasNext() {
                    if (next == null) {
                        next = next0();
                    }
                    return next != null;
                }

                @Override
                public String next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    String result = next;
                    next = null;
                    return result;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("Cannot remove prefix");
                }
            };
        }
    }

    @Override
    public String getNamespaceURI(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("Prefix must not be null");
        } else if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
            return defaultNamespace;
        } else if (XMLConstants.XML_NS_PREFIX.equals(prefix)) {
            return XMLConstants.XML_NS_URI;
        } else if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix)) {
            return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
        } else {
            if (prefixes != null) {
                for (Pair<String, String> pair : prefixes) {
                    if (pair.getFirst().equals(prefix)) {
                        return pair.getSecond();
                    }
                }
            }
            return parent == null ? XMLConstants.NULL_NS_URI : parent.getNamespaceURI(prefix);
        }
    }
}
