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

package org.apache.synapse.commons.json.jsonprocessor.utils;

import org.apache.synapse.commons.json.jsonprocessor.constants.ValidatorConstants;
import org.apache.synapse.commons.json.jsonprocessor.exceptions.ParserException;

/**
 * Handle data type conversions for JSON parser.
 */
public class DataTypeConverter {

    // use without instantiating
    private DataTypeConverter() {
    }

    public static Boolean convertToBoolean(String value) throws ParserException {
        if (value != null && !value.isEmpty()) {
            value = JsonProcessorUtils.replaceEnclosingQuotes(value);
            if (value.equals("true") || value.equals("false")) {
                return Boolean.parseBoolean(value);
            }
            throw new ParserException("Cannot convert the sting : " + value + " to boolean");
        }
        throw new ParserException("Cannot convert an empty string to boolean");
    }

    public static int convertToInt(String value) throws ParserException {
        if (value != null && !value.isEmpty()) {
            value = JsonProcessorUtils.replaceEnclosingQuotes(value);
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException nfe) {
                throw new ParserException("NumberFormatException: " + nfe.getMessage(), nfe);
            }
        }
        throw new ParserException("Empty value cannot convert to int");
    }

    public static double convertToDouble(String value) throws ParserException {
        if (value != null && !value.isEmpty()) {
            value = JsonProcessorUtils.replaceEnclosingQuotes(value);
            try {
                return Double.parseDouble(value.trim());
            } catch (NumberFormatException nfe) {
                throw new ParserException("NumberFormatException: " + nfe.getMessage(), nfe);
            }
        }
        throw new ParserException("Empty value cannot convert to double");
    }
}


