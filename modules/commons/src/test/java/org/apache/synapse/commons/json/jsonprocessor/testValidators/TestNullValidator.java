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

package org.apache.synapse.commons.json.jsonprocessor.testValidators;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.synapse.commons.json.jsonprocessor.exceptions.ValidatorException;
import org.apache.synapse.commons.json.jsonprocessor.validators.NullValidator;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * This class will test the functionality of the NullValidator class.
 */
public class TestNullValidator {

    private static JsonParser parser;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void init() {

        parser = new JsonParser();
    }

    /**
     * This test checks null string input validation.
     */
    @Test
    public void testNullStringInput() throws ValidatorException {
        String schema = "{\"type\":\"null\"}";
        String testPayload = "null";
        JsonObject schemaObject = (JsonObject) parser.parse(schema);
        NullValidator.validateNull(schemaObject, testPayload);
    }

    /**
     * This test checks "" input validation.
     */
    @Test
    public void testEmptyInput() throws ValidatorException {
        thrown.expect(ValidatorException.class);
        String schema = "{\"type\":\"null\"}";
        String testPayload = "";
        JsonObject schemaObject = (JsonObject) parser.parse(schema);
        NullValidator.validateNull(schemaObject, testPayload);
    }

    /**
     * This test checks null input validation.
     */
    @Test
    public void testNullInput() throws ValidatorException {
        String schema = "{\"type\":\"null\"}";
        JsonObject schemaObject = (JsonObject) parser.parse(schema);
        NullValidator.validateNull(schemaObject, null);
    }

    /**
     * This test checks an invalid null input.
     */
    @Test
    public void testInvalidNullInput() throws ValidatorException {
        thrown.expect(ValidatorException.class);
        String schema = "{\"type\":\"null\"}";
        String testPayload = "Banana";
        JsonObject schemaObject = (JsonObject) parser.parse(schema);
        NullValidator.validateNull(schemaObject, testPayload);
    }
}
