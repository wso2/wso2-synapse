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

import javax.xml.namespace.NamespaceContext;

public class XMLStreamWriterScope<T> extends AbstractXMLStreamScope {
    private final boolean emptyElement;

    private T info;

    public XMLStreamWriterScope(String defaultNamespace, T info) {
        super(defaultNamespace);
        this.info = info;
        this.emptyElement = false;
    }

    public XMLStreamWriterScope(NamespaceContext parent, T info) {
        super(parent);
        this.info = info;
        this.emptyElement = false;
    }

    public XMLStreamWriterScope(XMLStreamWriterScope<T> parent, String prefix, String localName, String namespaceURI, boolean emptyElement) {
        super(parent, prefix, localName, namespaceURI);
        this.emptyElement = emptyElement;
    }

    public T getInfo() {
        return info;
    }

    void setInfo(T info) {
        this.info = info;
    }

    @Override
    @SuppressWarnings("unchecked")
    public XMLStreamWriterScope<T> getParent() {
        return (XMLStreamWriterScope<T>) super.getParent();
    }

    public boolean isEmptyElement() {
        return emptyElement;
    }
}
