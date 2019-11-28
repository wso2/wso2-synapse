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
import com.google.gson.JsonPrimitive;
import org.apache.synapse.commons.json.jsonprocessor.exceptions.ParserException;
import org.apache.synapse.commons.json.jsonprocessor.exceptions.ValidatorException;
import org.apache.synapse.commons.json.jsonprocessor.validators.StringValidator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * This class will test the StringValidator functionalities according to the schema.
 */
public class TestStringValidator {

    private static JsonParser parser;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void init() {
        parser = new JsonParser();
    }

    /**
     * This test checks null schema validation.
     */
    @Test
    public void testNullSchema() throws ValidatorException, ParserException {
        String schema = "{}";
        String testPayload = "Banana";
        JsonPrimitive expected = new JsonPrimitive(testPayload);
        JsonObject schemaObject = (JsonObject) parser.parse(schema);
        JsonPrimitive result = StringValidator.validateNominal(schemaObject, testPayload);
        Assert.assertNotNull("Validator didn't respond with a JSON primitive", result);
        Assert.assertEquals("Didn't receive the expected primitive", expected, result);
    }

    /**
     * This test checks a valid minLength condition.
     */
    @Test
    public void testMinLengthConstraint() throws ValidatorException, ParserException {
        String schema = "{ \"type\": \"string\",\"minLength\": 6}";
        String testPayload = "Banana";
        JsonPrimitive expected = new JsonPrimitive(testPayload);
        JsonObject schemaObject = (JsonObject) parser.parse(schema);
        JsonPrimitive result = StringValidator.validateNominal(schemaObject, testPayload);
        Assert.assertNotNull("Validator didn't respond with a JSON primitive", result);
        Assert.assertEquals("Didn't receive the expected primitive", expected, result);
    }

    /**
     * This test checks an invalid minLength condition.
     */
    @Test
    public void testInvalidMinLengthConstraint() throws ValidatorException, ParserException {
        thrown.expect(ValidatorException.class);
        //thrown.expectMessage("Name is empty!"); - not using since not finalized

        String schema = "{ \"type\": \"string\",\"minLength\": 7}";
        String testPayload = "Banana";
        JsonObject schemaObject = (JsonObject) parser.parse(schema);
        StringValidator.validateNominal(schemaObject, testPayload);
    }

    /**
     * This test checks a valid maxLength condition.
     */
    @Test
    public void testMaxLengthConstraint() throws ValidatorException, ParserException {
        String schema = "{ \"type\": \"string\",\"maxLength\": 6}";
        String testPayload = "Banana";
        JsonPrimitive expected = new JsonPrimitive(testPayload);
        JsonObject schemaObject = (JsonObject) parser.parse(schema);
        JsonPrimitive result = StringValidator.validateNominal(schemaObject, testPayload);
        Assert.assertNotNull("Validator didn't respond with a JSON primitive", result);
        Assert.assertEquals("Didn't receive the expected primitive", expected, result);
    }

    /**
     * This test checks an invalid maxLength condition.
     */
    @Test
    public void testInvalidMaxLengthConstraint() throws ValidatorException, ParserException {
        thrown.expect(ValidatorException.class);
        //thrown.expectMessage("Name is empty!"); - not using since not finalized

        String schema = "{ \"type\": \"string\",\"maxLength\": 5}";
        String testPayload = "Banana";
        JsonObject schemaObject = (JsonObject) parser.parse(schema);
        StringValidator.validateNominal(schemaObject, testPayload);
    }

    /**
     * This test checks a valid pattern constraint.
     */
    @Test
    public void testPatternConstraint() throws ValidatorException, ParserException {
        // pattern to detect numbers
        String schema = "{ \"type\": \"string\",\"pattern\": \"^[0-9]{1,45}$\"}";
        String testPayload = "2048";
        JsonPrimitive expected = new JsonPrimitive(testPayload);
        JsonObject schemaObject = (JsonObject) parser.parse(schema);
        JsonPrimitive result = StringValidator.validateNominal(schemaObject, testPayload);
        Assert.assertNotNull("Validator didn't respond with a JSON primitive", result);
        Assert.assertEquals("Didn't receive the expected primitive", expected, result);
    }

    /**
     * This test checks an invalid pattern constraint.
     */
    @Test
    public void testInvalidPatternConstraint() throws ValidatorException, ParserException {
        thrown.expect(ValidatorException.class);
        //thrown.expectMessage("Name is empty!"); - not using since not finalized
        String schema = "{ \"type\": \"string\",\"pattern\": \"^[0-9]{1,45}$\"}";
        String testPayload = "1.618";
        JsonObject schemaObject = (JsonObject) parser.parse(schema);
        StringValidator.validateNominal(schemaObject, testPayload);
    }

    /**
     * This test check valid string contains enums constraint.
     */
    @Test
    public void testEnumConstraint() throws ValidatorException, ParserException {
        // pattern to detect numbers
        String schema = "{ \"type\": \"string\",\"enum\": [\"Red\",\"Amber\",\"Green\"]}";
        String testPayload = "Red";
        JsonPrimitive expected = new JsonPrimitive(testPayload);
        JsonObject schemaObject = (JsonObject) parser.parse(schema);
        JsonPrimitive result = StringValidator.validateNominal(schemaObject, testPayload);
        Assert.assertNotNull("Validator didn't respond with a JSON primitive", result);
        Assert.assertEquals("Didn't receive the expected primitive", expected, result);
    }

    /**
     * This test checks an invalid enum constraint.
     */
    @Test
    public void testInvalidEnumConstraint() throws ValidatorException, ParserException {
        thrown.expect(ValidatorException.class);
        //thrown.expectMessage("Name is empty!"); - not using since not finalized
        String schema = "{ \"type\": \"string\",\"enum\": [\"Red\",\"Amber\",\"Green\"]}";
        String testPayload = "1.618";
        JsonObject schemaObject = (JsonObject) parser.parse(schema);
        StringValidator.validateNominal(schemaObject, testPayload);
    }

    /**
     * This test check null string allowed in  enum.
     */
    @Test
    public void testNullEnumConstraint() throws ValidatorException, ParserException {
        thrown.expect(ValidatorException.class);
        //thrown.expectMessage("Name is empty!"); - not using since not finalized
        String schema = "{ \"type\": \"string\",\"enum\": [\"Red\",\"Amber\",\"Green\"," + null + "]}";
        String testPayload = null;
        JsonObject schemaObject = (JsonObject) parser.parse(schema);
        StringValidator.validateNominal(schemaObject, testPayload);
    }

    /**
     * This test checks a valid const constraint.
     */
    @Test
    public void testConstConstraint() throws ValidatorException, ParserException {
        // pattern to detect numbers
        String schema = "{ \"type\": \"string\",\"const\": \"pi\"}";
        String testPayload = "pi";
        JsonPrimitive expected = new JsonPrimitive(testPayload);
        JsonObject schemaObject = (JsonObject) parser.parse(schema);
        JsonPrimitive result = StringValidator.validateNominal(schemaObject, testPayload);
        Assert.assertNotNull("Validator didn't respond with a JSON primitive", result);
        Assert.assertEquals("Didn't receive the expected primitive", expected, result);
    }

    /**
     * This test checks an invalid const constraint.
     */
    @Test
    public void testInvalidConstConstraint() throws ValidatorException, ParserException {
        thrown.expect(ValidatorException.class);
        //thrown.expectMessage("Name is empty!"); - not using since not finalized
        String schema = "{ \"type\": \"string\",\"const\": \"pi\"}";
        String testPayload = "ip";
        JsonObject schemaObject = (JsonObject) parser.parse(schema);
        StringValidator.validateNominal(schemaObject, testPayload);
    }
}
