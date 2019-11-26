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

/**
 * This class validate boolean values against a given schema.
 */
public class BooleanValidator {

    // Use without instantiating.
    private BooleanValidator() {
    }

    /**
     * Validate a boolean string according to a given schema.
     *
     * @param inputObject json schema.
     * @param value       boolean string.
     * @return JsonPrimitive contains the parsed boolean.
     * @throws ValidatorException exception occurs in validation.
     * @throws ParserException    exception occurs when parsing.
     */
    public static JsonPrimitive validateBoolean(JsonObject inputObject, String value) throws ValidatorException,
            ParserException {
        if (value == null) {
            throw new ValidatorException("Received null input to be validated with : " +
                    inputObject.toString() + ". Expected a boolean but found null");
        }
        Boolean parsedValue = DataTypeConverter.convertToBoolean(value);
        // Enum validations
        if (inputObject.has(ValidatorConstants.ENUM)) {
            JsonArray enumElements = inputObject.getAsJsonArray(ValidatorConstants.ENUM);
            if (enumElements.size() > 0 && !enumElements.contains(new JsonPrimitive(parsedValue))) {
                throw new ValidatorException("input \"" + value + "\" not contains any " +
                        "element from the enum  : " + enumElements.toString());
            }
        }
        //Const validation
        if (inputObject.has(ValidatorConstants.CONST) && !parsedValue.equals(inputObject.getAsJsonPrimitive
                (ValidatorConstants.CONST).getAsBoolean())) {
            throw new ValidatorException("String \"" + value + "\" is not equal to the const" +
                    " value");
        }
        return new JsonPrimitive(parsedValue);
    }
}
