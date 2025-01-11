/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.expression.impl.ast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LazilyParsedNumber;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.synapse.expression.impl.exception.EvaluationException;

import java.util.List;

/**
 * This class represents the result of an expression evaluation.
 * It can hold values of different types such as String, Number, Boolean, JsonElement, and null.
 */
public class ExpressionResult {
    private final Object value;

    public ExpressionResult() {
        this.value = null;
    }

    public ExpressionResult(String value) {
        this.value = value;
    }

    public ExpressionResult(OMNode value) {
        this.value = value;
    }

    public ExpressionResult(List value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public ExpressionResult(Number value) {
        this.value = value;
    }

    public ExpressionResult(int value) {
        this.value = value;
    }

    public ExpressionResult(Long value) {
        this.value = value;
    }

    public ExpressionResult(double value) {
        this.value = value;
    }

    public ExpressionResult(boolean value) {
        this.value = value;
    }

    public ExpressionResult(JsonElement value) {
        this.value = value;
    }

    public boolean isNull() {
        return value == null || (value instanceof JsonElement && value.equals(JsonNull.INSTANCE));
    }

    // Method to get value as String
    public String asString() {
        if (value == null) {
            return null;
        } else if (value instanceof String) {
            // if quoted, remove quotes
            if (((String) value).startsWith("\"") && ((String) value).endsWith("\"")) {
                return ((String) value).substring(1, ((String) value).length() - 1);
            } else if (((String) value).startsWith("'") && ((String) value).endsWith("'")) {
                return ((String) value).substring(1, ((String) value).length() - 1);
            }
            return (String) value;
        } else if (value instanceof JsonPrimitive && ((JsonPrimitive) value).isString()) {
            return ((JsonPrimitive) value).getAsString();
        }
        return value.toString(); // Fallback to toString() for other types
    }

    // Method to get value as int
    public int asInt() {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof JsonPrimitive && ((JsonPrimitive) value).isNumber()) {
            return ((JsonPrimitive) value).getAsInt();
        }
        throw new EvaluationException("Value : " + value + " cannot be converted to int");
    }

    // Method to get value as double
    public double asDouble() {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof JsonPrimitive && ((JsonPrimitive) value).isNumber()) {
            return ((JsonPrimitive) value).getAsDouble();
        }
        throw new EvaluationException("Value : " + value + " cannot be converted to double");
    }

    // Method to get value as Long
    public long asLong() {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof JsonPrimitive && ((JsonPrimitive) value).isNumber()) {
            return ((JsonPrimitive) value).getAsLong();
        }
        throw new EvaluationException("Value : " + value + " cannot be converted to double");
    }

    // Method to get value as boolean
    public boolean asBoolean() {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof JsonPrimitive && ((JsonPrimitive) value).isBoolean()) {
            return ((JsonPrimitive) value).getAsBoolean();
        }
        throw new EvaluationException("Value : " + value + " cannot be converted to boolean");
    }

    // Method to get value as JsonElement
    public JsonElement asJsonElement() {
        if (value instanceof JsonElement) {
            return (JsonElement) value;
        }
        throw new EvaluationException("Value is not a JsonElement");
    }

    public JsonObject asJsonObject() {
        if (value instanceof JsonObject) {
            return (JsonObject) value;
        } else if (value instanceof String) {
            return parseStringToJsonObject((String) value);
        }
        throw new EvaluationException("Value is not a JsonObject");
    }

    public JsonArray asJsonArray() {
        if (value instanceof JsonArray) {
            return (JsonArray) value;
        } else if (value instanceof String) {
            return parseStringToJsonArray((String) value);
        }
        throw new EvaluationException("Value is not a JsonArray");
    }

    // Method to check the actual type of the result
    public Class<?> getType() {
        if (value == null) {
            return null;
        }
        return value.getClass();
    }

    public boolean isNumeric() {
        return isInteger() || isDouble() || isLong();
    }

    public boolean isInteger() {
        return value instanceof Integer || (value instanceof JsonPrimitive && isInteger((JsonPrimitive) value));
    }

    public boolean isLong() {
        return value instanceof Long || (value instanceof JsonPrimitive && isLong((JsonPrimitive) value));
    }

    public boolean isDouble() {
        return value instanceof Double || (value instanceof JsonPrimitive && isDouble((JsonPrimitive) value));
    }

    public boolean isBoolean() {
        return value instanceof Boolean || (value instanceof JsonPrimitive && ((JsonPrimitive) value).isBoolean());
    }

    public boolean isString() {
        return value instanceof String || (value instanceof JsonPrimitive && ((JsonPrimitive) value).isString());
    }

    public boolean isObject() {
        if (value instanceof JsonElement && ((JsonElement) value).isJsonObject()) {
            return true;
        } else if (value instanceof String) {
            try {
                parseStringToJsonObject((String) value);
                return true;
            } catch (EvaluationException e) {
                return false;
            }
        }
        return false;
    }

    public boolean isArray() {
        if (value instanceof JsonElement && ((JsonElement) value).isJsonArray()) {
            return true;
        } else if (value instanceof String) {
            try {
                parseStringToJsonArray((String) value);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public boolean isJsonPrimitive() {
        return value instanceof JsonPrimitive;
    }

    private boolean isInteger(JsonPrimitive jsonPrimitive) {
        if (jsonPrimitive.isNumber()) {
            Number number = jsonPrimitive.getAsNumber();
            // Check if the number is an instance of integer types (int, long, short)
            if (number instanceof Long && number.longValue() <= Integer.MAX_VALUE) {
                return true;
            }
            if (number instanceof LazilyParsedNumber) {
                // Check if the number is an instance of integer types (int, long, short)
                String numberString = number.toString();
                try {
                    Integer.parseInt(numberString);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
        return false;
    }

    private boolean isLong(JsonPrimitive jsonPrimitive) {
        if (jsonPrimitive.isNumber()) {
            Number number = jsonPrimitive.getAsNumber();
            // Check if the number is an instance of integer types (int, long, short)
            if (number instanceof Long && number.longValue() > Integer.MAX_VALUE) {
                return true;
            }
            if (number instanceof LazilyParsedNumber) {
                // Check if the number is an instance of integer types (int, long, short)
                String numberString = number.toString();
                try {
                    Long.parseLong(numberString);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
        return false;
    }

    private boolean isDouble(JsonPrimitive jsonPrimitive) {
        if (jsonPrimitive.isNumber()) {
            Number number = jsonPrimitive.getAsNumber();
            // Check if the number is an instance of floating-point types (float, double)
            if (number instanceof Float || number instanceof Double) {
                return true;
            }
            if (number instanceof LazilyParsedNumber) {
                // Check if the number is an instance of integer types (int, long, short)
                String numberString = number.toString();
                try {
                    Double.parseDouble(numberString);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
        return false;
    }

    public boolean isOMElement() {
        return value instanceof OMElement;
    }

    private JsonObject parseStringToJsonObject(String value) throws EvaluationException {
        String stringValue = value;
        if (stringValue.startsWith("\"") && stringValue.endsWith("\"")) {
            stringValue = stringValue.substring(1, stringValue.length() - 1);
        }
        try {
            JsonElement jsonElement = JsonParser.parseString(stringValue.replace("\\", ""));
            if (jsonElement.isJsonObject()) {
                return jsonElement.getAsJsonObject();
            }
        } catch (JsonSyntaxException e) {
            throw new EvaluationException("Value is not a JsonObject");
        }
        throw new EvaluationException("Value is not a JsonObject");
    }

    private JsonArray parseStringToJsonArray(String value) throws EvaluationException {
        String stringValue = value;
        if (stringValue.startsWith("\"") && stringValue.endsWith("\"")) {
            stringValue = stringValue.substring(1, stringValue.length() - 1);
        }
        try {
            JsonElement jsonElement = JsonParser.parseString(stringValue.replace("\\", ""));
            if (jsonElement.isJsonArray()) {
                return jsonElement.getAsJsonArray();
            }
        } catch (JsonSyntaxException e) {
            throw new EvaluationException("Value is not a JsonArray");
        }
        throw new EvaluationException("Value is not a JsonArray");
    }
}
