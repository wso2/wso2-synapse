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
package org.apache.synapse.commons.staxon.core.json.stream;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

/**
 * JSON stream target.
 */
public interface JsonStreamTarget extends Closeable, Flushable {
    /**
     * Write name.
     *
     * @param name
     * @throws IOException
     */
    public void name(String name) throws IOException;

    /**
     * Write value.
     *
     * @param value
     * @throws IOException
     */
    public void value(Object value) throws IOException;

    /**
     * Start object
     *
     * @throws IOException
     */
    public void startObject() throws IOException;

    /**
     * End object.
     *
     * @throws IOException
     */
    public void endObject() throws IOException;

    /**
     * Start array.
     *
     * @throws IOException
     */
    public void startArray() throws IOException;

    /**
     * End array.
     *
     * @throws IOException
     */
    public void endArray() throws IOException;
}
