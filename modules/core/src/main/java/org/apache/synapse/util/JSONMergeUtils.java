/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.synapse.JSONObjectExtensionException;

import java.util.Map;

/**
 * This utility class contains various methods related to merging and manipulating JSON elements.
 */
public class JSONMergeUtils {

    public enum ConflictStrategy {
        THROW_EXCEPTION, PREFER_FIRST_OBJECT, PREFER_SECOND_OBJECT, MERGE_INTO_ARRAY, PREFER_NON_NULL;
    }

    /**
     * Extends a particular JSON Object by merging multiple objects under a given conflict resolution strategy.
     *
     * @param destinationObject Destination JSON Object.
     * @param conflictResolutionStrategy Conflict resolution strategy.
     * @param jsonObjects JSON Objects to be merged.
     * @throws JSONObjectExtensionException
     */
    public static void extendJSONObject(JsonObject destinationObject, ConflictStrategy conflictResolutionStrategy,
                                        JsonObject... jsonObjects) throws JSONObjectExtensionException {
        for (JsonObject obj : jsonObjects) {
            extendJSONObject(destinationObject, obj, conflictResolutionStrategy);
        }
    }

    /**
     * Merges two JSON objects under a given strategy.
     *
     * @param leftObj First object.
     * @param rightObj Second object.
     * @param conflictStrategy Conflict resolution strategy.
     * @throws JSONObjectExtensionException
     */
    private static void extendJSONObject(JsonObject leftObj, JsonObject rightObj, ConflictStrategy conflictStrategy)
            throws JSONObjectExtensionException {
        for (Map.Entry<String, JsonElement> rightEntry : rightObj.entrySet()) {
            String rightKey = rightEntry.getKey();
            JsonElement rightVal = rightEntry.getValue();
            if (leftObj.has(rightKey)) {
                // Handle conflict
                JsonElement leftVal = leftObj.get(rightKey);
                if (leftVal.isJsonArray() && rightVal.isJsonArray()) {
                    JsonArray leftArr = leftVal.getAsJsonArray();
                    JsonArray rightArr = rightVal.getAsJsonArray();
                    // Concatenate arrays - no conflicts
                    for (int i = 0; i < rightArr.size(); i++) {
                        leftArr.add(rightArr.get(i));
                    }
                } else if (leftVal.isJsonObject() && rightVal.isJsonObject()) {
                    // Merge recursively
                    extendJSONObject(leftVal.getAsJsonObject(), rightVal.getAsJsonObject(), conflictStrategy);
                } else {
                    // Merge with conflict resolution
                    handleMergeConflict(rightKey, leftObj, leftVal, rightVal, conflictStrategy);
                }
            } else {
                // No conflict: add to the object
                leftObj.add(rightKey, rightVal);
            }
        }
    }

    /**
     * Merges a source JSON object into a target JSON object.
     * 1. If fields have equal names, merge recursively.
     * 2. Null values in source will remove the field from the target.
     * 3. Override target values with source values
     * 4. Keys not supplied in source will remain unchanged in target
     *
     * @param sourceObj Source JSON Object.
     * @param targetObj Target JSON Object.
     *
     * @return Target JSON Object.
     * @throws UnsupportedOperationException
     */
    public static JsonObject extendJSONObject(JsonObject sourceObj, JsonObject targetObj) {

        for (Map.Entry<String,JsonElement> sourceEntry : sourceObj.entrySet()) {
            String key = sourceEntry.getKey();
            JsonElement value = sourceEntry.getValue();
            if (!targetObj.has(key)) {
                if (!value.isJsonNull())
                    targetObj.add(key, value);
            } else {
                if (!value.isJsonNull()) {
                    if (value.isJsonObject()) {
                        extendJSONObject(value.getAsJsonObject(), targetObj.get(key).getAsJsonObject());
                    } else {
                        targetObj.add(key,value);
                    }
                } else {
                    targetObj.remove(key);
                }
            }
        }
        return targetObj;
    }

    /**
     * Handles merge conflicts between JSON objects.
     *
     * @param key Object Key.
     * @param leftObject First object.
     * @param leftValue Value of first object.
     * @param rightValue Value of second object.
     * @param conflictStrategy Conflict resolution strategy.
     * @throws JSONObjectExtensionException
     */
    private static void handleMergeConflict(String key, JsonObject leftObject, JsonElement leftValue, JsonElement rightValue,
                                            ConflictStrategy conflictStrategy)
            throws JSONObjectExtensionException, UnsupportedOperationException {

        switch (conflictStrategy) {
            case PREFER_FIRST_OBJECT:
                // The right value gets ignored
                break;
            case PREFER_SECOND_OBJECT:
                // Replace right value with left value
                leftObject.add(key, rightValue);
                break;
            case MERGE_INTO_ARRAY:
                // Merge into an array
                if (leftValue.isJsonArray()) {
                    leftValue.getAsJsonArray().add(rightValue);
                } else {
                    JsonElement tempElement = leftValue;
                    leftValue = new JsonArray();
                    leftValue.getAsJsonArray().add(tempElement);
                    leftValue.getAsJsonArray().add(rightValue);
                    leftObject.add(key, leftValue);
                }
                break;
            case PREFER_NON_NULL:
                // Check if right value is not null, and left value is null and use the right value.
                if (leftValue.isJsonNull() && !rightValue.isJsonNull()) {
                    leftObject.add(key, rightValue);
                }
                break;
            case THROW_EXCEPTION:
                throw new JSONObjectExtensionException("Key " + key + " exists in both objects and" +
                        " the conflict resolution strategy is " + conflictStrategy);
            default:
                throw new UnsupportedOperationException("The conflict strategy " + conflictStrategy +
                        " is unknown and cannot be processed");
        }
    }

    /**
     * Creates a JSON object from a string.
     *
     * @param jsonString JSON object as a string.
     * @return JSON Object
     */
    public static JsonObject getJsonObject(String jsonString) {

        JsonObject jsonObject = new JsonObject();
        JsonParser parser;

        parser = new JsonParser();

        if (jsonString != null) {
            jsonObject = parser.parse(jsonString).getAsJsonObject();
        }

        return jsonObject;
    }

    /**
     * Convert a string to JSON Array.
     *
     * @param jsonString JSON array as string.
     * @return JSON array.
     */
    public static JsonArray getJsonArray(String jsonString) {

        JsonArray jsonArray = new JsonArray();
        JsonParser parser;

        parser = new JsonParser();

        try {
            jsonArray = parser.parse(jsonString).getAsJsonArray();
        } catch (Exception ignore) {
        }

        return jsonArray;
    }

    /**
     * Count given elements in array.
     *
     * @param element Element to find.
     * @return Amount of given elements in array.
     */
    public static int count(JsonArray array, JsonElement element) {

        int count = 0;

        for (JsonElement currentElement : array) {
            if (currentElement.isJsonPrimitive()) {
                // Primitive types
                if (currentElement.equals(element)) {
                    count++;
                }
            }

            if (currentElement.isJsonObject() || currentElement.isJsonArray()) {
                // Complex types
                if (currentElement.toString().equals(element.toString())) {
                    count++;
                }
            }
        }

        return count;
    }
}
