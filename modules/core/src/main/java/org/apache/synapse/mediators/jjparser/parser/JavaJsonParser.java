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

package org.apache.synapse.mediators.jjparser.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.mediators.jjparser.constants.ValidatorConstants;
import org.apache.synapse.mediators.jjparser.exceptions.ParserException;
import org.apache.synapse.mediators.jjparser.exceptions.ValidatorException;
import org.apache.synapse.mediators.jjparser.utils.GSONDataTypeConverter;
import org.apache.synapse.mediators.jjparser.validators.ArrayValidator;
import org.apache.synapse.mediators.jjparser.validators.BooleanValidator;
import org.apache.synapse.mediators.jjparser.validators.NumericValidator;
import org.apache.synapse.mediators.jjparser.validators.ObjectValidator;
import org.apache.synapse.mediators.jjparser.validators.StringValidator;

/**
 * This class will parse a given JSON input according to a given schema.
 * Supported inout formats - String and Gson JsonObject
 */
public class JavaJsonParser {

    // Use without instantiating
    private JavaJsonParser() {
    }

    // Logger instance
    private static Log logger = LogFactory.getLog(JavaJsonParser.class.getName());

    // JSON parser instance
    private static JsonParser parser = new JsonParser();

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
        JsonObject schemaObject = null;
        JsonElement schema = parser.parse(inputSchema);
        if (schema.isJsonObject()) {
            schemaObject = schema.getAsJsonObject();
        } else if (schema.isJsonPrimitive()) {
            // if schema is primitive it should be a boolean
            boolean valid = schema.getAsBoolean();
            if (valid) {
                return inputString;
            } else if (!valid) {
                ValidatorException exception = new ValidatorException("JSON schema is not valid for all elements");
                logger.error("JSON schema is false, so all validations will fail", exception);
                throw exception;
            } else {
                ValidatorException exception = new ValidatorException("Unexpected JSON schema");
                logger.error("JSON schema should be an object or boolean", exception);
                throw exception;
            }
        } else {
            ValidatorException exception = new ValidatorException("Unexpected JSON schema");
            logger.error("JSON schema should be an object or boolean", exception);
            throw exception;
        }
        return parseJson(inputString, schemaObject);
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
    public static String parseJson(String inputString, Object schema) throws ValidatorException, ParserException {
        JsonElement input = parser.parse(inputString);
        JsonElement result = null;
        JsonObject schemaObject = (JsonObject) schema;
        String type = schemaObject.get(ValidatorConstants.TYPE_KEY).toString().replaceAll(ValidatorConstants
                .REGEX, "");
        if (ValidatorConstants.BOOLEAN_KEYS.contains(type)) {
            result = BooleanValidator.validateBoolean(schemaObject, input.toString());
        } else if (ValidatorConstants.NOMINAL_KEYS.contains(type)) {
            result = StringValidator.validateNominal(schemaObject, input.toString());
        } else if (ValidatorConstants.NUMERIC_KEYS.contains(type)) {
            result = NumericValidator.validateNumeric(schemaObject, input.toString());
        } else if (ValidatorConstants.ARRAY_KEYS.contains(type)) {
            result = ArrayValidator.validateArray(GSONDataTypeConverter.getMapFromString(input.toString()),
                    schemaObject);
        } else if (ValidatorConstants.NULL_KEYS.contains(type)) {
            // todo add null implementation
        } else if (ValidatorConstants.OBJECT_KEYS.contains(type)) {
            if (input.isJsonObject()) {
                result = ObjectValidator.validateObject(input.getAsJsonObject(), schemaObject);
            } else {
                ValidatorException exception = new ValidatorException("Expected a json object input");
                logger.error("Expected a JSON as input but found : " + input.toString(), exception);
                throw exception;
            }
        }
        return result.toString();
    }
}
