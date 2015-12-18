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
package org.apache.synapse.commons.staxon.core.json;

class JsonXMLStreamScopeInfo {
    private String arrayName = null;
    private int arraySize = -1;

    void startArray(String arrayName) {
        if (isArray()) {
            throw new IllegalStateException("Cannot start array: " + arrayName);
        }
        this.arrayName = arrayName;
        this.arraySize = 0;
    }

    void incArraySize() {
        if (!isArray()) {
            throw new IllegalStateException("Not in an array");
        }
        arraySize++;
    }

    String getArrayName() {
        return arrayName;
    }

    boolean isArray() {
        return arraySize >= 0;
    }

    void endArray() {
        if (!isArray()) {
            throw new IllegalStateException("Cannot end array: " + arrayName);
        }
        this.arrayName = null;
        this.arraySize = -1;
    }
}
