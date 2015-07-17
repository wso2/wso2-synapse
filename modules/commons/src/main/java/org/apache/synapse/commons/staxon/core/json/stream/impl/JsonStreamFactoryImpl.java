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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import org.apache.synapse.commons.staxon.core.json.stream.JsonStreamFactory;
import org.apache.synapse.commons.staxon.core.json.stream.JsonStreamSource;
import org.apache.synapse.commons.staxon.core.json.stream.JsonStreamTarget;

/**
 * Default <code>JsonStreamFactory</code> implementation.
 */
public class JsonStreamFactoryImpl extends JsonStreamFactory {
    private final String prettyIndent;
    private final String prettyNewline;
    private final String prettySpace;

    /**
     * Create instance.
     * Petty printing will use <code>"\t"</code> for indentation (per level),
     * <code>"\n"</code> as line separator and <code>" "</code> to decorate
     * colons, commas, etc.
     */
    public JsonStreamFactoryImpl() {
        this(" ", "\t", "\n");
    }

    /**
     * Create instance.
     *
     * @param prettySpace   inserted around colons, commas, etc
     * @param prettyIndent  indentation per depth level
     * @param prettyNewline newline character sequence
     */
    public JsonStreamFactoryImpl(String prettySpace, String prettyIndent, String prettyNewline) {
        this.prettySpace = prettySpace;
        this.prettyIndent = prettyIndent;
        this.prettyNewline = prettyNewline;
    }

    @Override
    public JsonStreamSource createJsonStreamSource(InputStream input) throws IOException {
        return createJsonStreamSource(new InputStreamReader(input, "UTF-8"));
    }

    @Override
    public JsonStreamSource createJsonStreamSource(InputStream input, Constants.SCANNER scanner) throws IOException {
        return createJsonStreamSource(new InputStreamReader(input, "UTF-8"), scanner);
    }

    @Override
    public JsonStreamSource createJsonStreamSource(Reader reader) {
        return new JsonStreamSourceImpl(new Yylex(reader), false);
    }

    @Override
    public JsonStreamSource createJsonStreamSource(Reader reader, Constants.SCANNER scanner) {
        if (scanner == Constants.SCANNER.SCANNER_1) {
            return new JsonStreamSourceImpl(new JsonScanner(reader), false);
        } else if (scanner == Constants.SCANNER.SCANNER_2) {
            return new JsonStreamSourceImpl(new JsonScanner2(reader), false);
        }
        return new JsonStreamSourceImpl(new Yylex(reader), false);
    }

    @Override
    public JsonStreamTarget createJsonStreamTarget(OutputStream output, boolean pretty) throws IOException {
        return createJsonStreamTarget(new OutputStreamWriter(output, "UTF-8"), pretty);
    }

    @Override
    public JsonStreamTarget createJsonStreamTarget(Writer writer, boolean pretty) {
        if (pretty) {
            return new JsonStreamTargetImpl(writer, false, prettySpace, prettyIndent, prettyNewline);
        } else {
            return new JsonStreamTargetImpl(writer, false);
        }
    }
}
