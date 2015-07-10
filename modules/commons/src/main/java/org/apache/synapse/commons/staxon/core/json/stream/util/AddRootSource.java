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
package org.apache.synapse.commons.staxon.core.json.stream.util;

import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.apache.synapse.commons.staxon.core.json.stream.JsonStreamSource;
import org.apache.synapse.commons.staxon.core.json.stream.JsonStreamToken;

public class AddRootSource extends StreamSourceDelegate {
    private enum State {
        START_DOC,
        ROOT_NAME,
        ROOT_XMLNS_NAME,
        ROOT_XMLNS_VALUE,
        DELEGATE,
        END_DOC
    }

    private final QName root;
    private final char namespaceSeparator;

    private State state = State.START_DOC;
    private int depth = 0;

    public AddRootSource(JsonStreamSource delegate, QName root, char namespaceSeparator) {
        super(delegate);
        this.root = root;
        this.namespaceSeparator = namespaceSeparator;
    }

    @Override
    public String name() throws IOException {
        if (state == State.ROOT_NAME) {
            state = State.DELEGATE;
            if (XMLConstants.DEFAULT_NS_PREFIX.equals(root.getPrefix())) {
                return root.getLocalPart();
            } else {
                return root.getPrefix() + namespaceSeparator + root.getLocalPart();
            }
        } else if (state == State.ROOT_XMLNS_NAME) {
            state = State.ROOT_XMLNS_VALUE;
            if (XMLConstants.DEFAULT_NS_PREFIX.equals(root.getPrefix())) {
                return '@' + XMLConstants.XMLNS_ATTRIBUTE;
            } else {
                return '@' + XMLConstants.XMLNS_ATTRIBUTE + namespaceSeparator + root.getLocalPart();
            }
        }
        return super.name();
    }

    @Override
    public Value value() throws IOException {
        if (state == State.ROOT_XMLNS_VALUE) {
            state = State.DELEGATE;
            return new Value(root.getNamespaceURI());
        }
        return super.value();
    }

    @Override
    public void startObject() throws IOException {
        if (state == State.START_DOC) {
            state = State.ROOT_NAME;
        } else {
            if (depth == 1 && !XMLConstants.NULL_NS_URI.equals(root.getNamespaceURI())) {
                state = State.ROOT_XMLNS_NAME;
            }
            super.startObject();
        }
        depth++;
    }

    @Override
    public void endObject() throws IOException {
        if (state == State.END_DOC) {
            state = null;
            return;
        }
        if (depth == 1 && state == State.DELEGATE && super.peek() == JsonStreamToken.NONE) {
            state = State.END_DOC;
        }
        if (state != State.END_DOC) {
            super.endObject();
        }
        depth--;
    }

    @Override
    public JsonStreamToken peek() throws IOException {
        if (state == null) {
            return JsonStreamToken.NONE;
        }
        switch (state) {
            case START_DOC:
                return JsonStreamToken.START_OBJECT;
            case ROOT_NAME:
                return JsonStreamToken.NAME;
            case ROOT_XMLNS_NAME:
                return JsonStreamToken.NAME;
            case ROOT_XMLNS_VALUE:
                return JsonStreamToken.VALUE;
            case END_DOC:
                return JsonStreamToken.END_OBJECT;
            case DELEGATE:
                JsonStreamToken result = super.peek();
                if (depth == 1 && result == JsonStreamToken.NONE) {
                    result = JsonStreamToken.END_OBJECT;
                }
                return result;
            default:
                throw new IllegalStateException("Unexpected state: " + state);
        }
    }
}
