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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.commons.json.jsonprocessor.validators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.synapse.commons.json.jsonprocessor.constants.ValidatorConstants;
import org.apache.synapse.commons.json.jsonprocessor.exceptions.ParserException;
import org.apache.synapse.commons.json.jsonprocessor.exceptions.ValidatorException;
import org.apache.synapse.commons.json.jsonprocessor.utils.DataTypeConverter;
import org.apache.synapse.commons.json.jsonprocessor.utils.JsonProcessorUtils;

/**
 * This class validate strings against the given schema object.
 */
public class StringValidator {

    private static final String MIN_LENGTH = "minLength";
    private static final String MAX_LENGTH = "maxLength";
    private static final String STR_PATTERN = "pattern";

    // Use without instantiating.
    private StringValidator() {

    }

    /**
     * Validate a given string against its schema.
     *
     * @param inputObject json schema as an object.
     * @param value       input string.
     * @return if valid return a JsonPrimitive created using input string.
     * @throws ValidatorException Didn't met validation criteria.
     * @throws ParserException    Exception occurs in data type conversions.
     */
    public static JsonPrimitive validateNominal(JsonObject inputObject, String value) throws ValidatorException,
            ParserException {
        if (value == null) {
            throw new ValidatorException("Expected a string in the schema " +
                    inputObject.toString() + " but found null input");
        }
        // String length validations
        if (inputObject.has(MAX_LENGTH)) {
            String maxLengthString = JsonProcessorUtils.replaceEnclosingQuotes(
                    inputObject.get(MAX_LENGTH).getAsString());
            if (!maxLengthString.isEmpty()) {
                int maxLength = DataTypeConverter.convertToInt(maxLengthString);
                if (value.length() > maxLength) {
                    throw new ValidatorException("String \"" + value + "\" violated the max " +
                            "length constraint. Input string : " + value +
                            " violated the maxLength constraint defined in : " + inputObject.toString());
                }
            }
        }
        if (inputObject.has(MIN_LENGTH)) {
            String minLengthString = JsonProcessorUtils.replaceEnclosingQuotes(
                    inputObject.get(MIN_LENGTH).getAsString());
            if (!minLengthString.isEmpty()) {
                int minLength = DataTypeConverter.convertToInt(minLengthString);
                if (value.length() < minLength) {
                    throw new ValidatorException("String \"" + value +
                            "\" violated the min " + "length constraint. Input string : " + value +
                            " violated the minLength constraint defined in : " + inputObject.toString());
                }
            }
        }
        // String pattern validations
        if (inputObject.has(STR_PATTERN)) {
            String patternString = JsonProcessorUtils.replaceEnclosingQuotes(
                    inputObject.get(STR_PATTERN).getAsString());
            if (!patternString.isEmpty() && !value.matches(patternString)) {
                throw new ValidatorException("String \"" + value + "\" violated the regex " +
                        "constraint " + patternString + ". Input string : " + value +
                        " not matching with any regex defined in : " + inputObject.toString());
            }
        }
        // Enum validations
        if (inputObject.has(ValidatorConstants.ENUM)) {
            JsonArray enumElements = inputObject.getAsJsonArray(ValidatorConstants.ENUM);
            if (enumElements.size() > 0 && !enumElements.contains(new JsonPrimitive(value))) {
                throw new ValidatorException("String \"" + value + "\" not contains any " +
                        "element from the enum. Input string : " + value +
                        " not contains any value defined in the enum of : " + inputObject.toString());
            }
        }
        //Const validation
        if (inputObject.has(ValidatorConstants.CONST) && !value.equals(inputObject.getAsJsonPrimitive
                (ValidatorConstants.CONST).getAsString())) {
            throw new ValidatorException("String \"" + value +
                    "\" is not equal to the const" + " value. Input string : " + value +
                    " not contains the const value defined in : " + inputObject.toString());
        }
        return new JsonPrimitive(value);
    }
}
