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

import java.util.LinkedList;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

public class XMLStreamReaderScope<T> extends AbstractXMLStreamScope {
    private List<Pair<String, String>> declarations;
    private T info;

    public XMLStreamReaderScope(String defaultNamespace, T info) {
        super(defaultNamespace);
        this.info = info;
    }

    public XMLStreamReaderScope(NamespaceContext parent, T info) {
        super(parent);
        this.info = info;
    }

    public XMLStreamReaderScope(XMLStreamReaderScope<T> parent, String prefix, String localName, String namespaceURI) {
        super(parent, prefix, localName, namespaceURI);
    }

    public T getInfo() {
        return info;
    }

    void setInfo(T info) {
        this.info = info;
    }

    @Override
    @SuppressWarnings("unchecked")
    public XMLStreamReaderScope<T> getParent() {
        return (XMLStreamReaderScope<T>) super.getParent();
    }

    public int getNamespaceCount() {
        return declarations == null ? 0 : declarations.size();
    }

    public String getNamespacePrefix(int index) {
        return declarations == null ? null : declarations.get(index).getFirst();
    }

    public String getNamespaceURI(int index) {
        return declarations == null ? null : declarations.get(index).getSecond();
    }

    void addNamespaceURI(String prefix, String namespaceURI) {
        if (declarations == null) {
            declarations = new LinkedList<Pair<String, String>>();
        }
        declarations.add(new Pair<String, String>(prefix, namespaceURI));
    }

    public int getAttributeCount() {
        return getAttributes() == null ? 0 : getAttributes().size();
    }

    public QName getAttributeName(int index) {
        Attr attribute = getAttributes().get(index);
        return new QName(attribute.getNamespaceURI(), attribute.getLocalName(), attribute.getPrefix());
    }

    public String getAttributeValue(int index) {
        return getAttributes().get(index).getValue();
    }

    public String getAttributeValue(String namespaceURI, String localName) {
        if (getAttributes() != null) {
            for (Attr attribute : getAttributes()) {
                if (localName.equals(attribute.getLocalName())) {
                    if (namespaceURI == null || namespaceURI.equals(attribute.getNamespaceURI())) {
                        return attribute.getValue();
                    }
                }
            }
        }
        return null;
    }
}
