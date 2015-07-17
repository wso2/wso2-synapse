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

import org.apache.synapse.commons.staxon.core.json.stream.JsonStreamTarget;

/**
 * Delegating stream target.
 */
public class StreamTargetDelegate implements JsonStreamTarget {
    /*
     * delegate target
     */
    private final JsonStreamTarget delegate;

    public StreamTargetDelegate(JsonStreamTarget delegate) {
        this.delegate = delegate;
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public void name(String name) throws IOException {
        delegate.name(name);
    }

    @Override
    public void value(Object value) throws IOException {
        delegate.value(value);
    }

    @Override
    public void startObject() throws IOException {
        delegate.startObject();
    }

    @Override
    public void endObject() throws IOException {
        delegate.endObject();
    }

    @Override
    public void startArray() throws IOException {
        delegate.startArray();
    }

    @Override
    public void endArray() throws IOException {
        delegate.endArray();
    }
}
