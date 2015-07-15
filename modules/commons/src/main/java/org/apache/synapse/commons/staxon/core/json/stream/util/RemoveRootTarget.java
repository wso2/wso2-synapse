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

import javax.xml.namespace.QName;

import org.apache.synapse.commons.staxon.core.json.stream.JsonStreamTarget;

public class RemoveRootTarget extends StreamTargetDelegate {
    private final QName root;
    private final char namespaceSeparator;

    private int depth;

    public RemoveRootTarget(JsonStreamTarget delegate, QName root, char namespaceSeparator) {
        super(delegate);
        this.root = root;
        this.namespaceSeparator = namespaceSeparator;
    }

    @Override
    public void name(String name) throws IOException {
        if (depth > 1) {
            super.name(name);
        } else {
            String localPart = name.substring(name.indexOf(namespaceSeparator) + 1);
            if (!localPart.equals(root.getLocalPart())) {
                throw new IOException("Unexpected root: " + name);
            }
        }
    }

    @Override
    public void startObject() throws IOException {
        if (depth++ > 0) {
            super.startObject();
        }
    }

    @Override
    public void endObject() throws IOException {
        if (--depth > 0) {
            super.endObject();
        }
    }
}
