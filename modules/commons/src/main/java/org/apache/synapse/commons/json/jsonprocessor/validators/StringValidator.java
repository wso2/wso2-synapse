/**
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.commons.json.jsonprocessor.validators;

import com.google.gson.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.json.jsonprocessor.constants.ValidatorConstants;
import org.apache.synapse.commons.json.jsonprocessor.exceptions.ParserException;
import org.apache.synapse.commons.json.jsonprocessor.exceptions.ValidatorException;
import org.apache.synapse.commons.json.jsonprocessor.utils.DataTypeConverter;

/**
 * This class validate strings against the given schema object.
 */
public class StringValidator {

    // Use without instantiating.
    private StringValidator() {
    }

    private static final String MIN_LENGTH = "minLength";
    private static final String MAX_LENGTH = "maxLength";
    private static final String STR_PATTERN = "pattern";

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
            ValidatorException exception = new ValidatorException("Expected a string in the schema " +
                    inputObject.toString() + " but found null input");
            throw exception;
        }
        // String length validations
        if (inputObject.has(MAX_LENGTH)) {
            String maxLengthString = inputObject.get(MAX_LENGTH).
                    getAsString().replaceAll(ValidatorConstants.REGEX, "");
            if (!maxLengthString.isEmpty()) {
                int maxLength = DataTypeConverter.convertToInt(maxLengthString);
                if (value.length() > maxLength) {
                    ValidatorException exception = new ValidatorException("String \"" + value + "\" violated the max " +
                            "length constraint. Input string : " + value +
                            " violated the maxLength constraint defined in : " + inputObject.toString());
                    throw exception;
                }
            }
        }
        if (inputObject.has(MIN_LENGTH)) {
            String minLengthString = inputObject.get(MIN_LENGTH).getAsString().
                    replaceAll(ValidatorConstants.REGEX, "");
            if (!minLengthString.isEmpty()) {
                int minLength = DataTypeConverter.convertToInt(minLengthString);
                if (value.length() < minLength) {
                    ValidatorException exception = new ValidatorException("String \"" + value +
                            "\" violated the min " + "length constraint. Input string : " + value +
                            " violated the minLength constraint defined in : " + inputObject.toString());
                    throw exception;
                }
            }
        }
        // String pattern validations
        if (inputObject.has(STR_PATTERN)) {
            String patternString = inputObject.get(STR_PATTERN).getAsString().replaceAll(ValidatorConstants.REGEX, "");
            if (!patternString.isEmpty() && !value.matches(patternString)) {
                ValidatorException exception = new ValidatorException("String \"" + value + "\" violated the regex " +
                        "constraint " + patternString + ". Input string : " + value +
                        " not matching with any regex defined in : " + inputObject.toString());
                throw exception;
            }
        }
        // Enum validations
        if (inputObject.has(ValidatorConstants.ENUM)) {
            JsonArray enumElements = inputObject.getAsJsonArray(ValidatorConstants.ENUM);
            if (enumElements.size() > 0 && !enumElements.contains(new JsonPrimitive(value))) {
                ValidatorException exception = new ValidatorException("String \"" + value + "\" not contains any " +
                        "element from the enum. Input string : " + value +
                        " not contains any value defined in the enum of : " + inputObject.toString());
                throw exception;
            }
        }
        //Const validation
        if (inputObject.has(ValidatorConstants.CONST) && !value.equals(inputObject.getAsJsonPrimitive
                (ValidatorConstants.CONST).getAsString())) {
            ValidatorException exception = new ValidatorException("String \"" + value +
                    "\" is not equal to the const" + " value. Input string : " + value +
                    " not contains the const value defined in : " + inputObject.toString());
            throw exception;
        }
        return new JsonPrimitive(value);
    }
}
