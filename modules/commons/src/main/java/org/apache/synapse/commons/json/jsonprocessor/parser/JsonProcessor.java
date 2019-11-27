/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.commons.json.jsonprocessor.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang.StringUtils;
import org.apache.synapse.commons.json.jsonprocessor.constants.ValidatorConstants;
import org.apache.synapse.commons.json.jsonprocessor.exceptions.ParserException;
import org.apache.synapse.commons.json.jsonprocessor.exceptions.ValidatorException;
import org.apache.synapse.commons.json.jsonprocessor.utils.GSONDataTypeConverter;
import org.apache.synapse.commons.json.jsonprocessor.utils.JsonProcessorUtils;
import org.apache.synapse.commons.json.jsonprocessor.validators.ArrayValidator;
import org.apache.synapse.commons.json.jsonprocessor.validators.BooleanValidator;
import org.apache.synapse.commons.json.jsonprocessor.validators.NullValidator;
import org.apache.synapse.commons.json.jsonprocessor.validators.NumericValidator;
import org.apache.synapse.commons.json.jsonprocessor.validators.ObjectValidator;
import org.apache.synapse.commons.json.jsonprocessor.validators.StringValidator;

/**
 * This class will parse a given JSON input according to a given schema.
 * Supported inout formats - String and Gson JsonObject
 */
public class JsonProcessor {

    // JSON parser instance
    private static JsonParser parser = new JsonParser();

    // Use without instantiating
    private JsonProcessor() {

    }

    /**
     * This method parse a given JSON string according to the given schema. Both as string.
     *
     * @param inputString input String.
     * @param inputSchema input Schema.
     * @return corrected String.
     * @throws ValidatorException Exception occurs in validation process.
     * @throws ParserException    Exception occurs in data type parsing.
     */
    public static String parseJson(String inputString, String inputSchema) throws ValidatorException, ParserException {
        if (StringUtils.isNotEmpty(inputString) && StringUtils.isNotEmpty(inputSchema)) {
            JsonObject schemaObject;
            JsonElement schema;
            try {
                schema = parser.parse(inputSchema);
            } catch (JsonSyntaxException ex) {
                throw new ValidatorException("Invalid JSON schema", ex);
            }
            if (schema.isJsonObject()) {
                // Handling empty JSON objects - valid for all inputs
                if (schema.toString().replaceAll("\\s+","").equals("{}")) {
                    return inputString;
                }
                schemaObject = schema.getAsJsonObject();
            } else if (schema.isJsonPrimitive()) {
                // if schema is primitive it should be a boolean
                boolean valid = schema.getAsBoolean();
                if (valid) {
                    return inputString;
                } else {
                    throw new ValidatorException("JSON schema is false, so all validations will fail");
                }
            } else {
                throw new ValidatorException("JSON schema should be an object or boolean");
            }
            return parseJson(inputString, schemaObject);
        } else {
            throw new ParserException("Input json and schema should not be null");
        }
    }

    /**
     * This method will parse a given JSON string according to the given schema. Schema as an Object.
     * Can use this method when using caching.
     *
     * @param inputString input JSON string.
     * @param schema      already parsed JSON schema.
     * @return corrected JSON string.
     * @throws ValidatorException Exception occurs in validation process.
     * @throws ParserException    Exception occurs in data type parsing.
     */
    private static String parseJson(String inputString, Object schema) throws ValidatorException, ParserException {
        if (StringUtils.isNotEmpty(inputString) && schema instanceof JsonObject) {
            JsonElement result = null;
            JsonObject schemaObject = (JsonObject) schema;
            if (((JsonObject) schema).has(ValidatorConstants.TYPE_KEY)) {
                String type = JsonProcessorUtils.replaceEnclosingQuotes(
                        schemaObject.get(ValidatorConstants.TYPE_KEY).toString());
                if (ValidatorConstants.BOOLEAN_KEYS.contains(type)) {
                    result = BooleanValidator.validateBoolean(schemaObject, inputString);
                } else if (ValidatorConstants.NOMINAL_KEYS.contains(type)) {
                    result = StringValidator.validateNominal(schemaObject, inputString);
                } else if (ValidatorConstants.NUMERIC_KEYS.contains(type)) {
                    result = NumericValidator.validateNumeric(schemaObject, inputString);
                } else if (ValidatorConstants.ARRAY_KEYS.contains(type)) {
                    result = ArrayValidator.validateArray(GSONDataTypeConverter.getMapFromString(inputString),
                            schemaObject);
                } else if (ValidatorConstants.NULL_KEYS.contains(type)) {
                    NullValidator.validateNull(schemaObject, inputString);
                    result = JsonNull.INSTANCE;
                } else if (ValidatorConstants.OBJECT_KEYS.contains(type)) {
                    JsonElement input = parser.parse(inputString);
                    if (input.isJsonObject()) {
                        result = ObjectValidator.validateObject(input.getAsJsonObject(), schemaObject);
                    } else {
                        throw new ValidatorException(
                                "Expected a JSON as input but found : " + inputString);
                    }
                }
                if (result != null) {
                    return result.toString();
                }
                return null;
            } else {
                throw new ValidatorException("JSON schema should contain a type declaration");
            }
        } else {
            throw new ParserException("Input json and schema should not be null, " +
                    "schema should be a JSON object");
        }
    }
}
