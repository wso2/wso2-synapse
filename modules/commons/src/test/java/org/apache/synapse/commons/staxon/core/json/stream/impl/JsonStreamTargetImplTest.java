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
package org.apache.synapse.commons.staxon.core.json.stream.impl;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;

public class JsonStreamTargetImplTest {
    @Test
    public void testStringValue() throws IOException {
        StringWriter writer = new StringWriter();
        JsonStreamTargetImpl target = new JsonStreamTargetImpl(writer, true);

        target.value("bob");

        target.close();

        Assert.assertEquals("\"bob\"", writer.toString());
    }

    @Test
    public void testObjectValue() throws IOException {
        StringWriter writer = new StringWriter();
        JsonStreamTargetImpl target = new JsonStreamTargetImpl(writer, true);

        target.startObject();
        target.name("alice");
        target.value("bob");
        target.endObject();

        target.close();

        Assert.assertEquals("{\"alice\":\"bob\"}", writer.toString());
    }

    @Test
    public void testArrayValue() throws IOException {
        StringWriter writer = new StringWriter();
        JsonStreamTargetImpl target = new JsonStreamTargetImpl(writer, true);

        target.startArray();
        target.value("bob");
        target.endArray();

        target.close();

        Assert.assertEquals("[\"bob\"]", writer.toString());
    }

    @Test
    public void testArray1() throws IOException {
        StringWriter writer = new StringWriter();
        JsonStreamTargetImpl target = new JsonStreamTargetImpl(writer, true);

        target.startObject();
        target.name("alice");
        target.startArray();
        target.value("bob");
        target.endArray();
        target.endObject();

        target.close();

        Assert.assertEquals("{\"alice\":[\"bob\"]}", writer.toString());
    }

    @Test
    public void testArray2() throws IOException {
        StringWriter writer = new StringWriter();
        JsonStreamTargetImpl target = new JsonStreamTargetImpl(writer, true);

        target.startObject();
        target.name("alice");
        target.startObject();
        target.name("bob");
        target.startArray();
        target.value("edgar");
        target.value("charlie");
        target.endArray();
        target.endObject();
        target.endObject();

        target.close();

        Assert.assertEquals("{\"alice\":{\"bob\":[\"edgar\",\"charlie\"]}}", writer.toString());
    }

    @Test
    public void testArray3() throws IOException {
        StringWriter writer = new StringWriter();
        JsonStreamTargetImpl target = new JsonStreamTargetImpl(writer, true);

        target.startObject();
        target.name("alice");
        target.startObject();
        target.name("edgar");
        target.startArray();
        target.value("bob");
        target.endArray();
        target.name("charlie");
        target.startArray();
        target.value("bob");
        target.endArray();
        target.endObject();
        target.endObject();

        target.close();

        Assert.assertEquals("{\"alice\":{\"edgar\":[\"bob\"],\"charlie\":[\"bob\"]}}", writer.toString());
    }

    @Test
    public void testString() throws IOException {
        StringWriter writer = new StringWriter();
        JsonStreamTargetImpl target = new JsonStreamTargetImpl(writer, true);

        target.startArray();
        target.value("");
        target.value("abc");
        target.value("\b\f\n\r\t");
        target.value("\"");
        target.value("\\");
        target.value("\u001F");
        target.endArray();

        target.close();

        Assert.assertEquals("[\"\",\"abc\",\"\\b\\f\\n\\r\\t\",\"\\\"\",\"\\\\\",\"\\u001F\"]", writer.toString());
    }

    @Test
    public void testSimpleValue() throws IOException {
        StringWriter writer = new StringWriter();
        JsonStreamTargetImpl target = new JsonStreamTargetImpl(writer, true);

        target.startArray();
        target.value("abc");
        target.value(1234);
        target.value(true);
        target.endArray();

        target.close();

        Assert.assertEquals("[\"abc\",1234,true]", writer.toString());
    }
}
