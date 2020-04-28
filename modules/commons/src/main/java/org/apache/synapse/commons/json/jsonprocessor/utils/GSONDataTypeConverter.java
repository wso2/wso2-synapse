/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.commons.json.jsonprocessor.utils;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class provides functionality to convert between GSON data structures.
 */
public class GSONDataTypeConverter {

    // use without instantiating
    private GSONDataTypeConverter() {
    }

    /**
     * Given a string contains a json array, this method will return the Map.
     * This is where the single element array correction happens.
     *
     * @param input JsonArray as a string.
     * @return map entry of json array.
     */
    public static Map.Entry<String, JsonElement> getMapFromString(String input) {
        JsonParser parser = new JsonParser();
        JsonObject temp = new JsonObject();
        JsonElement inputElement = parser.parse(input);
        JsonArray arrayObject = null;
        if (inputElement.isJsonArray()) {
            arrayObject = (JsonArray) parser.parse(input);
        } else if (inputElement.isJsonPrimitive() || inputElement.isJsonObject()) {
            arrayObject = new JsonArray();
            arrayObject.add(inputElement);
        }
        temp.add("test", arrayObject);
        //converting Set<Map.Entry<String, JsonElement>> to Map.Entry<String, JsonElement>
        //the key 'test' will be removed eventually
        Set<Map.Entry<String, JsonElement>> entries = temp.entrySet();
        Iterator itr = entries.iterator();
        return (Map.Entry<String, JsonElement>) itr.next();
    }

    /**
     * Given a json array, this method will return the map
     *
     * @param array input array.
     * @return map entry of json array
     */
    public static Map.Entry<String, JsonElement> getMapFromJsonArray(JsonArray array) {
        JsonObject sample = new JsonObject();
        sample.add("test", array);
        Set<Map.Entry<String, JsonElement>> entries = sample.entrySet();
        return entries.iterator().next();
    }
}
