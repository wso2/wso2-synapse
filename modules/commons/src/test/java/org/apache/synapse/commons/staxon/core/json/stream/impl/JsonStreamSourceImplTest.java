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
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.apache.synapse.commons.staxon.core.json.stream.JsonStreamSource;
import org.apache.synapse.commons.staxon.core.json.stream.JsonStreamToken;
import org.apache.synapse.commons.staxon.core.json.stream.util.StreamSourceDelegate;

public class JsonStreamSourceImplTest {
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    void readInvalid(String input, Class<? extends Exception> exceptionClass, String exceptiondMessage) throws IOException {
        expectedException.expect(exceptionClass);
        expectedException.expectMessage(exceptiondMessage);

        StreamSourceDelegate source =
                new StreamSourceDelegate(new JsonStreamSourceImpl(new Yylex(new StringReader(input)), true));
        try {
            source.copy(new JsonStreamTargetImpl(new StringWriter(), true));
        } catch (Exception ex) {
            //empty
        } finally {
            source.close();
        }
    }

    @Test
    public void testStringValue() throws IOException {
        StringReader reader = new StringReader("\"bob\"");
        JsonStreamSourceImpl source = new JsonStreamSourceImpl(new Yylex(reader), true);

        Assert.assertEquals(JsonStreamToken.VALUE, source.peek());
        Assert.assertEquals("bob", source.value().text);

        Assert.assertEquals(JsonStreamToken.NONE, source.peek());
        source.close();
    }

    @Test
    public void testObjectValue() throws IOException {
        StringReader reader = new StringReader("{\"alice\":\"bob\"}");
        JsonStreamSourceImpl source = new JsonStreamSourceImpl(new Yylex(reader), true);

        Assert.assertEquals(JsonStreamToken.START_OBJECT, source.peek());
        source.startObject();

        Assert.assertEquals(JsonStreamToken.NAME, source.peek());
        Assert.assertEquals("alice", source.name());

        Assert.assertEquals(JsonStreamToken.VALUE, source.peek());
        Assert.assertEquals("bob", source.value().text);

        Assert.assertEquals(JsonStreamToken.END_OBJECT, source.peek());
        source.endObject();

        Assert.assertEquals(JsonStreamToken.NONE, source.peek());
        source.close();
    }

    @Test
    public void testArrayValue() throws IOException {
        StringReader reader = new StringReader("[\"bob\"]");
        JsonStreamSourceImpl source = new JsonStreamSourceImpl(new Yylex(reader), true);

        Assert.assertEquals(JsonStreamToken.START_ARRAY, source.peek());
        source.startArray();

        Assert.assertEquals(JsonStreamToken.VALUE, source.peek());
        Assert.assertEquals("bob", source.value().text);

        Assert.assertEquals(JsonStreamToken.END_ARRAY, source.peek());
        source.endArray();

        Assert.assertEquals(JsonStreamToken.NONE, source.peek());
        source.close();
    }

    @Test
    public void testArray1() throws IOException {
        StringReader reader = new StringReader("{\"alice\":[\"bob\"]}");
        JsonStreamSourceImpl source = new JsonStreamSourceImpl(new Yylex(reader), true);

        Assert.assertEquals(JsonStreamToken.START_OBJECT, source.peek());
        source.startObject();

        Assert.assertEquals(JsonStreamToken.NAME, source.peek());
        Assert.assertEquals("alice", source.name());

        Assert.assertEquals(JsonStreamToken.START_ARRAY, source.peek());
        source.startArray();

        Assert.assertEquals(JsonStreamToken.VALUE, source.peek());
        Assert.assertEquals("bob", source.value().text);

        Assert.assertEquals(JsonStreamToken.END_ARRAY, source.peek());
        source.endArray();

        Assert.assertEquals(JsonStreamToken.END_OBJECT, source.peek());
        source.endObject();

        Assert.assertEquals(JsonStreamToken.NONE, source.peek());
        source.close();
    }

    @Test
    public void testArray2() throws IOException {
        StringReader reader = new StringReader("{\"alice\":{\"bob\":[\"edgar\",\"charlie\"]}}");
        JsonStreamSourceImpl source = new JsonStreamSourceImpl(new Yylex(reader), true);

        Assert.assertEquals(JsonStreamToken.START_OBJECT, source.peek());
        source.startObject();

        Assert.assertEquals(JsonStreamToken.NAME, source.peek());
        Assert.assertEquals("alice", source.name());

        Assert.assertEquals(JsonStreamToken.START_OBJECT, source.peek());
        source.startObject();

        Assert.assertEquals(JsonStreamToken.NAME, source.peek());
        Assert.assertEquals("bob", source.name());

        Assert.assertEquals(JsonStreamToken.START_ARRAY, source.peek());
        source.startArray();

        Assert.assertEquals(JsonStreamToken.VALUE, source.peek());
        Assert.assertEquals("edgar", source.value().text);

        Assert.assertEquals(JsonStreamToken.VALUE, source.peek());
        Assert.assertEquals("charlie", source.value().text);

        Assert.assertEquals(JsonStreamToken.END_ARRAY, source.peek());
        source.endArray();

        Assert.assertEquals(JsonStreamToken.END_OBJECT, source.peek());
        source.endObject();

        Assert.assertEquals(JsonStreamToken.END_OBJECT, source.peek());
        source.endObject();

        Assert.assertEquals(JsonStreamToken.NONE, source.peek());
        source.close();
    }

    @Test
    public void testArray3() throws IOException {
        StringReader reader = new StringReader("{\"alice\":{\"edgar\":[\"bob\"],\"charlie\":[\"bob\"]}}");
        JsonStreamSourceImpl source = new JsonStreamSourceImpl(new Yylex(reader), true);

        Assert.assertEquals(JsonStreamToken.START_OBJECT, source.peek());
        source.startObject();

        Assert.assertEquals(JsonStreamToken.NAME, source.peek());
        Assert.assertEquals("alice", source.name());

        Assert.assertEquals(JsonStreamToken.START_OBJECT, source.peek());
        source.startObject();

        Assert.assertEquals(JsonStreamToken.NAME, source.peek());
        Assert.assertEquals("edgar", source.name());

        Assert.assertEquals(JsonStreamToken.START_ARRAY, source.peek());
        source.startArray();

        Assert.assertEquals(JsonStreamToken.VALUE, source.peek());
        Assert.assertEquals("bob", source.value().text);

        Assert.assertEquals(JsonStreamToken.END_ARRAY, source.peek());
        source.endArray();

        Assert.assertEquals(JsonStreamToken.NAME, source.peek());
        Assert.assertEquals("charlie", source.name());

        Assert.assertEquals(JsonStreamToken.START_ARRAY, source.peek());
        source.startArray();

        Assert.assertEquals(JsonStreamToken.VALUE, source.peek());
        Assert.assertEquals("bob", source.value().text);

        Assert.assertEquals(JsonStreamToken.END_ARRAY, source.peek());
        source.endArray();

        Assert.assertEquals(JsonStreamToken.END_OBJECT, source.peek());
        source.endObject();

        Assert.assertEquals(JsonStreamToken.END_OBJECT, source.peek());
        source.endObject();

        Assert.assertEquals(JsonStreamToken.NONE, source.peek());
        source.close();
    }

    @Test
    public void testString() throws IOException {
        StringReader reader = new StringReader("[\"\",\"abc\",\"\\b\\f\\n\\r\\t\",\"\\\"\",\"\\\\\",\"\\u001F\"]");
        JsonStreamSourceImpl source = new JsonStreamSourceImpl(new Yylex(reader), true);

        Assert.assertEquals(JsonStreamToken.START_ARRAY, source.peek());
        source.startArray();

        Assert.assertEquals(JsonStreamToken.VALUE, source.peek());
        Assert.assertEquals("", source.value().text);

        Assert.assertEquals(JsonStreamToken.VALUE, source.peek());
        Assert.assertEquals("abc", source.value().text);

        Assert.assertEquals(JsonStreamToken.VALUE, source.peek());
        Assert.assertEquals("\b\f\n\r\t", source.value().text);

        Assert.assertEquals(JsonStreamToken.VALUE, source.peek());
        Assert.assertEquals("\"", source.value().text);

        Assert.assertEquals(JsonStreamToken.VALUE, source.peek());
        Assert.assertEquals("\\", source.value().text);

        Assert.assertEquals(JsonStreamToken.VALUE, source.peek());
        Assert.assertEquals("\u001F", source.value().text);

        Assert.assertEquals(JsonStreamToken.END_ARRAY, source.peek());
        source.endArray();

        Assert.assertEquals(JsonStreamToken.NONE, source.peek());
        source.close();
    }

    @Test
    public void testLiteralValues() throws IOException {
        StringReader reader = new StringReader("[true,false,null]");
        JsonStreamSourceImpl source = new JsonStreamSourceImpl(new Yylex(reader), true);
        JsonStreamSource.Value value = null;

        Assert.assertEquals(JsonStreamToken.START_ARRAY, source.peek());
        source.startArray();

        Assert.assertEquals(JsonStreamToken.VALUE, source.peek());
        value = source.value();
        Assert.assertEquals("true", value.text);
        Assert.assertEquals(Boolean.TRUE, value.data);

        Assert.assertEquals(JsonStreamToken.VALUE, source.peek());
        value = source.value();
        Assert.assertEquals("false", value.text);
        Assert.assertEquals(Boolean.FALSE, value.data);

        Assert.assertEquals(JsonStreamToken.VALUE, source.peek());
        value = source.value();
        Assert.assertNull(value.text);
        Assert.assertNull(value.data);

        Assert.assertEquals(JsonStreamToken.END_ARRAY, source.peek());
        source.endArray();

        Assert.assertEquals(JsonStreamToken.NONE, source.peek());
        source.close();
    }

    @Test
    public void testNumberValues() throws IOException {
        StringReader reader = new StringReader("[123,12e3,12E3,12.3,1.2e3,1.2E3]");
        JsonStreamSourceImpl source = new JsonStreamSourceImpl(new Yylex(reader), true);
        JsonStreamSource.Value value = null;

        Assert.assertEquals(JsonStreamToken.START_ARRAY, source.peek());
        source.startArray();

        Assert.assertEquals(JsonStreamToken.VALUE, source.peek());
        value = source.value();
        Assert.assertEquals("123", value.text);
        Assert.assertEquals(new BigInteger("123"), value.data);

        Assert.assertEquals(JsonStreamToken.VALUE, source.peek());
        value = source.value();
        Assert.assertEquals("12e3", value.text);
        Assert.assertEquals(new BigDecimal("12e3"), value.data);

        Assert.assertEquals(JsonStreamToken.VALUE, source.peek());
        value = source.value();
        Assert.assertEquals("12E3", value.text);
        Assert.assertEquals(new BigDecimal("12E3"), value.data);

        Assert.assertEquals(JsonStreamToken.VALUE, source.peek());
        value = source.value();
        Assert.assertEquals("12.3", value.text);
        Assert.assertEquals(new BigDecimal("12.3"), value.data);

        Assert.assertEquals(JsonStreamToken.VALUE, source.peek());
        value = source.value();
        Assert.assertEquals("1.2e3", value.text);
        Assert.assertEquals(new BigDecimal("1.2e3"), value.data);

        Assert.assertEquals(JsonStreamToken.VALUE, source.peek());
        value = source.value();
        Assert.assertEquals("1.2E3", value.text);
        Assert.assertEquals(new BigDecimal("1.2E3"), value.data);

        Assert.assertEquals(JsonStreamToken.END_ARRAY, source.peek());
        source.endArray();

        Assert.assertEquals(JsonStreamToken.NONE, source.peek());
        source.close();
    }

    @Test
    public void testWhitespace() throws IOException {
        StringReader reader = new StringReader("{\r  \"alice\" : \"bob\"\r\n}");
        JsonStreamSourceImpl source = new JsonStreamSourceImpl(new Yylex(reader), true);

        Assert.assertEquals(JsonStreamToken.START_OBJECT, source.peek());
        source.startObject();

        Assert.assertEquals(JsonStreamToken.NAME, source.peek());
        Assert.assertEquals("alice", source.name());

        Assert.assertEquals(JsonStreamToken.VALUE, source.peek());
        Assert.assertEquals("bob", source.value().text);

        Assert.assertEquals(JsonStreamToken.END_OBJECT, source.peek());
        source.endObject();

        Assert.assertEquals(JsonStreamToken.NONE, source.peek());
        source.close();
    }

    @Test
    public void testLocation() throws IOException {
        String input = "{\n\t\"alice\" : {\n\t\t\"bob\" : [ \"charlie\" ],\n\t\t\"edgar\" : \"david\"\n\t}\n}";
//		System.out.println(input);
        JsonStreamSourceImpl source = new JsonStreamSourceImpl(new Yylex(new StringReader(input)), true);

        Assert.assertEquals(1, source.getLineNumber());
        Assert.assertEquals(1, source.getColumnNumber());
        Assert.assertEquals(0, source.getCharacterOffset());
        source.startObject();
        Assert.assertEquals(1, source.getLineNumber());
        Assert.assertEquals(1, source.getColumnNumber());
        Assert.assertEquals(0, source.getCharacterOffset());
        source.name();
        Assert.assertEquals(2, source.getLineNumber());
        Assert.assertEquals(8, source.getColumnNumber());
        Assert.assertEquals(9, source.getCharacterOffset());
        source.startObject();
        Assert.assertEquals(2, source.getLineNumber());
        Assert.assertEquals(12, source.getColumnNumber());
        Assert.assertEquals(13, source.getCharacterOffset());
        source.name();
        Assert.assertEquals(3, source.getLineNumber());
        Assert.assertEquals(7, source.getColumnNumber());
        Assert.assertEquals(21, source.getCharacterOffset());
        source.startArray();
        Assert.assertEquals(3, source.getLineNumber());
        Assert.assertEquals(11, source.getColumnNumber());
        Assert.assertEquals(25, source.getCharacterOffset());
        source.value();
        Assert.assertEquals(3, source.getLineNumber());
        Assert.assertEquals(21, source.getColumnNumber());
        Assert.assertEquals(35, source.getCharacterOffset());
        source.endArray();
        Assert.assertEquals(3, source.getLineNumber());
        Assert.assertEquals(23, source.getColumnNumber());
        Assert.assertEquals(37, source.getCharacterOffset());
        source.name();
        Assert.assertEquals(4, source.getLineNumber());
        Assert.assertEquals(9, source.getColumnNumber());
        Assert.assertEquals(48, source.getCharacterOffset());
        source.value();
        Assert.assertEquals(4, source.getLineNumber());
        Assert.assertEquals(19, source.getColumnNumber());
        Assert.assertEquals(58, source.getCharacterOffset());
        source.endObject();
        Assert.assertEquals(5, source.getLineNumber());
        Assert.assertEquals(2, source.getColumnNumber());
        Assert.assertEquals(61, source.getCharacterOffset());
        source.endObject();
        Assert.assertEquals(6, source.getLineNumber());
        Assert.assertEquals(1, source.getColumnNumber());
        Assert.assertEquals(63, source.getCharacterOffset());
        source.close();
    }

    @Test
    public void testInvalid_UnclosedArray() throws IOException {
        readInvalid("{\"alice\":[\"bob\"}}", IOException.class, "Unclosed array");
    }

    @Test
    public void testInvalid_UnclosedArray2() throws IOException {
        readInvalid("[\"edgar\",\"david\"}", IOException.class, "Unclosed array");
    }

    @Test
    public void testInvalid_NotInAnArray() throws IOException {
        readInvalid("{\"alice\":\"bob\"]", IOException.class, "Not in an array");
    }

    @Test
    public void testInvalid_NotInAnArray2() throws IOException {
        readInvalid("{\"alice\":[\"bob\"]]", IOException.class, "Not in an array");
    }

    @Test
    public void testInvalid_NotInAnObject() throws IOException {
        readInvalid("{\"alice\":\"bob\"}}", IOException.class, "Not in an object");
    }

    @Test
    public void testInvalid_UnexpectedSymbol() throws IOException {
        readInvalid("{\"alice\":{\"bob\":\"charlie\"}:}", IOException.class, "Unexpected symbol: COLON");
    }

    @Test
    public void testInvalid_UnexpectedSymbol2() throws IOException {
        readInvalid("\"alice\":\"bob\"", IOException.class, "Unexpected symbol: COLON");
    }

    @Test
    public void testInvalid_PrematureEOF() throws IOException {
        readInvalid("[\"edgar\",\"david\"", IOException.class, "Premature EOF");
    }
}
