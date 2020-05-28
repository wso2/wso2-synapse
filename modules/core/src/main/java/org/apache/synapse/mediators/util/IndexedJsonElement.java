/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.apache.synapse.mediators.util;

import com.google.gson.JsonElement;

/**
 * Represents a JsonElement along with it's position in the Json Array.
 */
public class IndexedJsonElement {

    private int index;
    private JsonElement element;

    public IndexedJsonElement(int index, JsonElement element) {

        this.index = index;
        this.element = element;
    }

    public int getIndex() {

        return index;
    }

    public void setIndex(int index) {

        this.index = index;
    }

    public JsonElement getElement() {

        return element;
    }

    public void setElement(JsonElement element) {

        this.element = element;
    }
}
