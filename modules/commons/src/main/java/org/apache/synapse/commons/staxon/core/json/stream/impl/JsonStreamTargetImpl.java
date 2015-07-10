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
import java.io.Writer;
import java.util.Stack;

import org.apache.synapse.commons.staxon.core.json.stream.JsonStreamTarget;

/**
 * Default <code>JsonStreamTarget</code> implementation.
 */
class JsonStreamTargetImpl implements JsonStreamTarget {
    private final Writer writer;
    private final int[] namePos = new int[64];
    private final int[] arrayPos = new int[64];
    private final StringBuilder buffer = new StringBuilder();
    private final boolean closeWriter;

    private final String[] indent;
    private final String space;

    private int depth = 0;

    private JsonStreamSourceImpl.Scanner.Symbol symbol = null;
    private Stack<Character> symbols = new Stack<Character>();
    private Stack<Boolean> objects = new Stack<Boolean>();

    JsonStreamTargetImpl(Writer writer, boolean closeWriter) {
        this(writer, closeWriter, null, null, null);
    }

    JsonStreamTargetImpl(Writer writer, boolean closeWriter, String prettySpace, String prettyIndent, String prettyNewline) {
        this.writer = writer;
        this.closeWriter = closeWriter;
        this.space = prettySpace;

        if (prettyIndent != null || prettyNewline != null) {
            this.indent = new String[64];
            StringBuilder builder = new StringBuilder();
            if (prettyNewline != null) {
                builder.append(prettyNewline);
            }
            for (int i = 0; i < 64; i++) {
                indent[i] = builder.toString();
                if (prettyIndent != null) {
                    builder.append(prettyIndent);
                }
            }
        } else {
            this.indent = null;
        }
    }

    private String encode(String value) {
        buffer.setLength(0);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    buffer.append("\\\"");
                    break;
                case '\\':
                    buffer.append("\\\\");
                    break;
                case '\b':
                    buffer.append("\\b");
                    break;
                case '\f':
                    buffer.append("\\f");
                    break;
                case '\n':
                    buffer.append("\\n");
                    break;
                case '\r':
                    buffer.append("\\r");
                    break;
                case '\t':
                    buffer.append("\\t");
                    break;
                default:
                    if (c < ' ') {
                        buffer.append(String.format("\\u%04X", (int) c));
                    } else {
                        buffer.append(c);
                    }
            }
        }
        return buffer.toString();
    }

    public void close() throws IOException {
        if (closeWriter) {
            writer.close();
        } else {
            writer.flush();
        }
    }

    public void flush() throws IOException {
        writer.flush();
    }

    public void name(String name) throws IOException {
        if (Constants.EMPTY.equals(name)) {
            writer.write(symbols.pop());
            objects.push(false);
            symbol = null;
            return;
        }
        if (symbol == JsonStreamSourceImpl.Scanner.Symbol.START_OBJECT) {
            symbol = null;
            if (Constants.ARRAY.equals(name)
                    || Constants.ARRAY_ELEM.equals(name)
                    || Constants.OBJECT.equals(name)) {
                symbols.pop();
                objects.push(true);
                return;
            }
            objects.push(false);
            writer.write(symbols.pop());
        }
        symbol = null;
        if (namePos[depth] > 1) {
            writer.write(',');
        }
        namePos[depth]++;
        if (indent != null) {
            writer.write(indent[depth]);
        } else if (space != null) {
            writer.write(space);
        }
        writer.write('"');
        writer.write(name);
        writer.write('"');
        if (space != null) {
            writer.write(space);
        }
        writer.write(':');
    }

    public void value(Object value) throws IOException {
        symbol = null;
        if (arrayPos[depth] > 0) {
            if (arrayPos[depth] > 1) {
                writer.write(',');
            }
            arrayPos[depth]++;
        }
        if (space != null) {
            writer.write(space);
        }
        if (value == null) {
            writer.write("null");
        } else if (value instanceof String) {
            if (Constants.EMPTY_VALUE.equals(value)) {
                return;
            }
            writer.write('"');
            writer.write(encode((String) value));
            writer.write('"');
        } else {
            writer.write(value.toString());
        }
    }

    public void startObject() throws IOException {
        if (arrayPos[depth] > 0) {
            if (arrayPos[depth] > 1) {
                writer.write(',');
            }
            arrayPos[depth]++;
        }
        if (space != null && (depth > 0 || arrayPos[depth] > 0)) {
            writer.write(space);
        }
        //writer.write('{');
        symbols.push('{');
        symbol = JsonStreamSourceImpl.Scanner.Symbol.START_OBJECT;
        depth++;
        namePos[depth] = 1;
    }

    public void endObject() throws IOException {
        symbol = null;
        namePos[depth] = 0;
        depth--;
        boolean skipEndObject = objects.pop();
        if (skipEndObject) {
            return;
        }
        if (indent != null) {
            writer.write(indent[depth]);
        } else if (space != null) {
            writer.write(space);
        }
        writer.write('}');
        if (depth == 0) {
            writer.flush();
        }
    }

    public void startArray() throws IOException {
        symbol = null;
        if (arrayPos[depth] > 0) {
            throw new IOException("Nested arrays are not supported!");
        }
        if (space != null && depth > 0) {
            writer.write(space);
        }
        writer.write('[');
        arrayPos[depth] = 1;
    }

    public void endArray() throws IOException {
        symbol = null;
        arrayPos[depth] = 0;
        if (space != null) {
            writer.write(space);
        }
        writer.write(']');
    }
}
