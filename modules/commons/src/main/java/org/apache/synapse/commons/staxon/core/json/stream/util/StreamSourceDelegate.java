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

import org.apache.synapse.commons.staxon.core.json.stream.JsonStreamSource;
import org.apache.synapse.commons.staxon.core.json.stream.JsonStreamTarget;
import org.apache.synapse.commons.staxon.core.json.stream.JsonStreamToken;

/**
 * Delegating stream source.
 */
public class StreamSourceDelegate implements JsonStreamSource {
    /*
     * delegate source
     */
    private final JsonStreamSource delegate;

    public StreamSourceDelegate(JsonStreamSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public int getLineNumber() {
        return delegate.getLineNumber();
    }

    @Override
    public int getColumnNumber() {
        return delegate.getColumnNumber();
    }

    @Override
    public String name() throws IOException {
        return delegate.name();
    }

    @Override
    public int getCharacterOffset() {
        return delegate.getCharacterOffset();
    }

    @Override
    public Value value() throws IOException {
        return delegate.value();
    }

    @Override
    public void startObject() throws IOException {
        delegate.startObject();
    }

    @Override
    public String getPublicId() {
        return delegate.getPublicId();
    }

    @Override
    public void endObject() throws IOException {
        delegate.endObject();
    }

    @Override
    public String getSystemId() {
        return delegate.getSystemId();
    }

    @Override
    public void startArray() throws IOException {
        delegate.startArray();
    }

    @Override
    public void endArray() throws IOException {
        delegate.endArray();
    }

    @Override
    public JsonStreamToken peek() throws IOException {
        return delegate.peek();
    }

    /**
     * Copy events to given target until <code>peek() == JsonStreamToken.NONE</code>.
     * This method does <em>not</em> close streams.
     *
     * @param target
     * @throws IOException
     */
    public void copy(JsonStreamTarget target) throws IOException {
        while (true) {
            switch (delegate.peek()) {
                case START_OBJECT:
                    delegate.startObject();
                    target.startObject();
                    break;
                case END_OBJECT:
                    delegate.endObject();
                    target.endObject();
                    break;
                case START_ARRAY:
                    delegate.startArray();
                    target.startArray();
                    break;
                case END_ARRAY:
                    delegate.endArray();
                    target.endArray();
                    break;
                case NAME:
                    target.name(delegate.name());
                    break;
                case VALUE:
                    target.value(delegate.value().data);
                    break;
                case NONE:
                    return;
            }
        }
    }
}
