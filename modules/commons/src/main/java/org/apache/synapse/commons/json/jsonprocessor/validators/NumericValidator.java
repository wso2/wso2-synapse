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
 * validate numeric instances according to the given schema.
 */
public class NumericValidator {

    private static final String INTEGER_STRING = "integer";
    private static final String MINIMUM_VALUE = "minimum";
    private static final String MAXIMUM_VALUE = "maximum";
    private static final String EXCLUSIVE_MINIMUM = "exclusiveMinimum";
    private static final String EXCLUSIVE_MAXIMUM = "exclusiveMaximum";
    private static final String MULTIPLE_OF = "multipleOf";

    // use without instantiating.
    private NumericValidator() {

    }

    /**
     * Take JSON schema, number as a string input and validate.
     *
     * @param inputObject JSON schema.
     * @param value       numeric value
     * @return JsonPrimitive contains a number
     * @throws ParserException    Exception occurred in data type conversions.
     * @throws ValidatorException Exception occurred in schema validations.
     */
    public static JsonPrimitive validateNumeric(JsonObject inputObject, String value) throws ParserException,
            ValidatorException {
        Double multipleOf;
        if (value == null) {
            throw new ValidatorException("Expected a number but found null");
        }
        //replacing enclosing quotes
        value = JsonProcessorUtils.replaceEnclosingQuotes(value);
        if (isNumeric(value)) {
            String type = null;
            if (inputObject.has(ValidatorConstants.TYPE_KEY)) {
                type = JsonProcessorUtils.replaceEnclosingQuotes(
                        inputObject.get(ValidatorConstants.TYPE_KEY).getAsString());
            }
            // handling multiples of condition
            Double doubleValue = DataTypeConverter.convertToDouble(value);
            if (inputObject.has(MULTIPLE_OF)) {
                multipleOf = DataTypeConverter.convertToDouble(JsonProcessorUtils.replaceEnclosingQuotes(
                        inputObject.get(MULTIPLE_OF).getAsString()));
                if (doubleValue % multipleOf != 0) {
                    throw new ValidatorException("Number " + value + " is not a multiple of " +
                            "" + multipleOf + ". multipleOf constraint in " + inputObject.toString() + " is violated " +
                            "by the input " + value);
                }
            }
            // handling maximum and minimum
            if (inputObject.has(MINIMUM_VALUE)) {
                String minimumString = JsonProcessorUtils.replaceEnclosingQuotes(
                        inputObject.get(MINIMUM_VALUE).getAsString());
                if (!minimumString.isEmpty() && doubleValue < DataTypeConverter.convertToDouble(minimumString)) {
                    throw new ValidatorException("Number " + value + " is less than the " +
                            "minimum allowed value" + ". minimumValue constraint in " + inputObject.toString() +
                            " is violated by the input " + ": " + value);
                }
            }
            if (inputObject.has(MAXIMUM_VALUE)) {
                String maximumString = JsonProcessorUtils.replaceEnclosingQuotes(
                        inputObject.get(MAXIMUM_VALUE).getAsString());
                if (!maximumString.isEmpty() && doubleValue > DataTypeConverter.convertToDouble(maximumString)) {
                    throw new ValidatorException("Number " + value + " is greater than the " +
                            "maximum allowed value. maximumValue constraint in " + inputObject.toString() +
                            " is violated by the input " + ": " + value);
                }
            }
            // handling exclusive maximum and minimum
            if (inputObject.has(EXCLUSIVE_MINIMUM)) {
                String minimumString = JsonProcessorUtils.replaceEnclosingQuotes(
                        inputObject.get(EXCLUSIVE_MINIMUM).getAsString());
                if (!minimumString.isEmpty() && doubleValue <= DataTypeConverter.convertToDouble(minimumString)) {
                    throw new ValidatorException("Number " + value + " is less than the " +
                            "minimum allowed value. ExclusiveMinimum constraint in " + inputObject.toString() +
                            " is violated by the " + "input : " + value );
                }
            }
            if (inputObject.has(EXCLUSIVE_MAXIMUM)) {
                String maximumString = JsonProcessorUtils.replaceEnclosingQuotes(
                        inputObject.get(EXCLUSIVE_MAXIMUM).getAsString());
                if (!maximumString.isEmpty() && doubleValue >= DataTypeConverter.convertToDouble(maximumString)) {
                    throw new ValidatorException("Number " + value + " is greater than the " +
                            "maximum allowed value. ExclusiveMaximum constraint in " +
                            inputObject.toString() + " is violated by the " + "input : " + value);
                }
            }
            // Enum validations
            if (inputObject.has(ValidatorConstants.ENUM)) {
                JsonArray enumElements = inputObject.getAsJsonArray(ValidatorConstants.ENUM);
                if (enumElements.size() > 0 && !enumElements.contains(new JsonPrimitive(doubleValue))) {
                    throw new ValidatorException("Number \"" + value + "\" not contains any " +
                            "element from the enum. Input " + value + " not contains any value from the enum in " +
                            inputObject.toString());
                }
            }
            //Const validation
            if (inputObject.has(ValidatorConstants.CONST) && !doubleValue.equals(inputObject.getAsJsonPrimitive
                    (ValidatorConstants.CONST).getAsDouble())) {
                throw new ValidatorException("Number \"" + value + "\" is not equal to the " +
                        "const value input " + value + " not contains the const defined in " + inputObject.toString());
            }
            // convert to integer of give value is a float
            if (INTEGER_STRING.equals(type)) {
                return new JsonPrimitive(DataTypeConverter.convertToInt(value));
            } else {
                // this condition address both type number and empty json schemas
                return new JsonPrimitive(doubleValue);
            }
        }
        throw new ParserException("\"" + value + "\"" + " is not a number. " +
                "A number expected in the schema " + inputObject.toString() + " but received " + value);
    }

    /**
     * Check whether a given number is numeric. (alternative :- commons-lang3 isCreatable())
     * @param str input string.
     * @return number or not.
     */
    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
}
