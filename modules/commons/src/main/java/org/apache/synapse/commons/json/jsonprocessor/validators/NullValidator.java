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

import com.google.gson.JsonObject;
import org.apache.synapse.commons.json.jsonprocessor.exceptions.ValidatorException;

/**
 * This class validate null values against a given schema.
 */
public class NullValidator {

    // use without instantiating.
    private NullValidator() {
    }

    /**
     * Validate a null input against schema.
     *
     * @param inputObject input schema.
     * @param value       null value.
     * @throws ValidatorException exception occurs in validation.
     */
    public static void validateNull(JsonObject inputObject, String value) throws ValidatorException {
        if (value != null && !(value.equals("null") || value.equals("\"null\""))) {
            throw new ValidatorException("Expected a null but found a value. " +
                    "Received not null input" + value + " to be validated with : " + inputObject
                    .toString());
        }
    }
}
