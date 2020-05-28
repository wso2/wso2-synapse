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

import java.util.Map;

/**
 * Represents an element of a Json Object with the index of the element in the parent Json Object
 */
public class IndexedEntry {

    private int index;
    private Map.Entry<String, JsonElement> entry;

    public IndexedEntry(int index, Map.Entry<String, JsonElement> entry) {

        this.index = index;
        this.entry = entry;
    }

    public int getIndex() {

        return index;
    }

    public void setIndex(int index) {

        this.index = index;
    }

    public Map.Entry<String, JsonElement> getEntry() {

        return entry;
    }

    public void setEntry(Map.Entry<String, JsonElement> entry) {

        this.entry = entry;
    }
}
