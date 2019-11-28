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
import org.apache.synapse.commons.json.jsonprocessor.exceptions.ParserException;
import org.apache.synapse.commons.json.jsonprocessor.exceptions.ValidatorException;
import org.apache.synapse.commons.json.jsonprocessor.validators.ObjectValidator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * This class will test the functionality of ObjectValidator class.
 */
public class TestObjectValidator {

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
        String testPayload = "{\"name\":\"Lahiru\"}";
        JsonObject schemaObject = (JsonObject) parser.parse(schema);
        JsonObject expected = (JsonObject) parser.parse(testPayload);
        ObjectValidator.validateObject(expected, schemaObject);
    }

    private static String requiredValidationSchema = "{\n" +
            "  \"type\": \"object\",\n" +
            "  \"properties\": {\n" +
            "    \"name\":      { \"type\": \"string\" },\n" +
            "    \"email\":     { \"type\": \"string\" },\n" +
            "    \"age\":       { \"type\": \"integer\" }\n" +
            "  },\n" +
            "  \"required\": [\"name\", \"email\"]\n" +
            "}";

    /**
     * This test checks for invalid required constraint.
     */
    @Test
    public void testInvalidRequiredConstraint() throws ValidatorException, ParserException {
        String testPayload = "{\"name\": \"William Shakespeare\", \"email\": \"bill@stratford-upon-avon.co.uk\", " +
                "\"age\":\"45\"}";
        String expectedPayload = "{\"name\": \"William Shakespeare\", \"email\": \"bill@stratford-upon-avon.co.uk\", " +
                "\"age\":45}";
        JsonObject schemaObject = (JsonObject) parser.parse(requiredValidationSchema);
        JsonObject expected = (JsonObject) parser.parse(expectedPayload);
        JsonObject testObject = (JsonObject) parser.parse(testPayload);
        JsonObject result = ObjectValidator.validateObject(testObject, schemaObject);
        Assert.assertNotNull("Validator didn't respond with a JSON object", result);
        Assert.assertEquals("Didn't receive the expected object", expected, result);
    }

    /**
     * This test checks for additional properties than schema.
     */
    @Test
    public void testAdditionalPropertiesThanSchema() throws ValidatorException, ParserException {
        String testPayload = "{\"name\": \"William Shakespeare\", \"email\": \"bill@stratford-upon-avon.co.uk\", " +
                "\"additional\":\"45\"}";
        JsonObject schemaObject = (JsonObject) parser.parse(requiredValidationSchema);
        JsonObject expected = (JsonObject) parser.parse(testPayload);
        JsonObject result = ObjectValidator.validateObject(expected, schemaObject);
        Assert.assertNotNull("Validator didn't respond with a JSON object", result);
        Assert.assertEquals("Didn't receive the expected object", expected, result);
    }

    /**
     * This test checks for invalid properties constraint.
     */
    @Test
    public void testInvalidPropertiesConstraint() throws ValidatorException, ParserException {
        thrown.expect(ParserException.class);
        String testPayload = "{\"name\": \"William Shakespeare\", \"email\": \"bill@stratford-upon-avon.co.uk\", " +
                "\"age\":\"invalid\"}";
        JsonObject schemaObject = (JsonObject) parser.parse(requiredValidationSchema);
        JsonObject testObject = (JsonObject) parser.parse(testPayload);
        ObjectValidator.validateObject(testObject, schemaObject);
    }

    /**
     * This test checks for valid required constraint.
     */
    @Test
    public void testValidRequiredConstraint() throws ValidatorException, ParserException {
        thrown.expect(ValidatorException.class);
        String testPayload = "{\"name\": \"William Shakespeare\",\"age\":\"45\"}";
        JsonObject schemaObject = (JsonObject) parser.parse(requiredValidationSchema);
        JsonObject testObject = (JsonObject) parser.parse(testPayload);
        ObjectValidator.validateObject(testObject, schemaObject);
    }

    private static String propertyCountSchema = "{\"type\": \"object\",\"minProperties\": 2,\"maxProperties\":4 }";

    /**
     * This test checks for valid minProperties and maxProperties constraint.
     */
    @Test
    public void testValidMinAndMaxProperties() throws ValidatorException, ParserException {
        String testPayload = "{ \"a\": 0, \"b\": 1, \"c\": 2 }";
        JsonObject schemaObject = (JsonObject) parser.parse(propertyCountSchema);
        JsonObject expected = (JsonObject) parser.parse(testPayload);
        JsonObject result = ObjectValidator.validateObject(expected, schemaObject);
        Assert.assertNotNull("Validator didn't respond with a JSON object", result);
        Assert.assertEquals("Didn't receive the expected object", expected, result);
    }

    /**
     * This test checks for invalid minProperties constraint.
     */
    @Test
    public void testInvalidMinProperties() throws ValidatorException, ParserException {
        thrown.expect(ValidatorException.class);
        String testPayload = "{ \"a\": 0 }";
        JsonObject schemaObject = (JsonObject) parser.parse(propertyCountSchema);
        JsonObject testObject = (JsonObject) parser.parse(testPayload);
        ObjectValidator.validateObject(testObject, schemaObject);
    }


    /**
     * This test checks for invalid maxProperties constraint.
     */
    @Test
    public void testInvalidMaxProperties() throws ValidatorException, ParserException {
        thrown.expect(ValidatorException.class);
        String testPayload = "{ \"a\": 0, \"b\": 1, \"c\": 2 ,\"d\":3, \"e\":4}";
        JsonObject schemaObject = (JsonObject) parser.parse(propertyCountSchema);
        JsonObject testObject = (JsonObject) parser.parse(testPayload);
        ObjectValidator.validateObject(testObject, schemaObject);
    }

    private static String patternPropertyString = "{\"properties\": {\n" +
            "             \"car\":{\"type\":\"string\"}\n" +
            "       },\n" +
            "       \"patternProperties\": {\n" +
            "           \"p\": {\"type\":\"boolean\"},\n" +
            "           \"[0-9]\": {\"type\":\"number\"}}}";

    /**
     * This test checks for valid pattern properties condition.
     */
    @Test
    public void testValidPatternProperties() throws ValidatorException, ParserException {
        String testPayload = "  {\"car\":\"Lambogini\",\n" +
                "    \"palm\": \"true\",\n" +
                "       \"a32&o\": \"89\",\n" +
                "       \"fiddle\": \"42\",\n" +
                "       \"apple\": \"true\"}";
        JsonObject schemaObject = (JsonObject) parser.parse(patternPropertyString);
        JsonObject expected = (JsonObject) parser.parse(testPayload);
        JsonObject result = ObjectValidator.validateObject(expected, schemaObject);
        Assert.assertNotNull("Validator didn't respond with a JSON object", result);
        Assert.assertEquals("Didn't receive the expected object", expected, result);
    }

    /**
     * This test checks for an invalid pattern properties condition.
     */
    @Test
    public void testInvalidPatternProperties() throws ValidatorException, ParserException {
        thrown.expect(ParserException.class);
        String testPayload = "  {\"car\":\"Lambogini\",\n" +
                "    \"palm\": \"true\",\n" +
                "       \"a32&o\": \"89\",\n" +
                "       \"fiddle\": \"42\",\n" +
                "       \"apple\": \"1234\"}";
        JsonObject schemaObject = (JsonObject) parser.parse(patternPropertyString);
        JsonObject expected = (JsonObject) parser.parse(testPayload);
        ObjectValidator.validateObject(expected, schemaObject);
    }

    private static String additionalPropertyString = "   {\"properties\": {\n" +
            "             \"car\":{\"type\":\"string\"}\n" +
            "       },\n" +
            "       \"patternProperties\": {\n" +
            "           \"p\": {\"type\":\"boolean\"},\n" +
            "           \"[0-9]\": {\"type\":\"number\"}\n" +
            "       },\n" +
            "       \"additionalProperties\":false}\n";

    /**
     * This test checks for an invalid additional properties condition.
     */
    @Test
    public void testInvalidAdditionalProperties() throws ValidatorException, ParserException {
        thrown.expect(ValidatorException.class);
        String testPayload = "  {\n" +
                "    \"car\":\"Bugatti Veyron\",\n" +
                "    \"palm\": true,\n" +
                "       \"a32&o\": 89,\n" +
                "       \"fiddle\": 42,\n" +
                "       \"apple\": true\n" +
                "   }";
        JsonObject schemaObject = (JsonObject) parser.parse(additionalPropertyString);
        JsonObject expected = (JsonObject) parser.parse(testPayload);
        ObjectValidator.validateObject(expected, schemaObject);
    }

    /**
     * This test checks for a valid additional properties condition.
     */
    @Test
    public void testValidAdditionalProperties() throws ValidatorException, ParserException {
        String testPayload = "  {\n" +
                "    \"car\":\"Bugatti Veyron\",\n" +
                "    \"palm\": true,\n" +
                "       \"a32&o\": 89,\n" +
                "       \"apple\": true\n" +
                "   }";
        JsonObject schemaObject = (JsonObject) parser.parse(additionalPropertyString);
        JsonObject expected = (JsonObject) parser.parse(testPayload);
        JsonObject result = ObjectValidator.validateObject(expected, schemaObject);
        Assert.assertNotNull("Validator didn't respond with a JSON object", result);
        Assert.assertEquals("Didn't receive the expected object", expected, result);
    }

    private static String getAdditionalPropertyAsObject = "   {\n" +
            "       \"properties\": {\n" +
            "             \"car\":{\"type\":\"string\"}\n" +
            "       },\n" +
            "       \"patternProperties\": {\n" +
            "           \"p\": {\"type\":\"boolean\"},\n" +
            "           \"[0-9]\": {\"type\":\"number\"}\n" +
            "       },\n" +
            "       \"additionalProperties\":{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}\n" +
            "   }\n";


    /**
     * This test checks for a valid additional properties condition.
     */
    @Test
    public void testValidAdditionalPropObject() throws ValidatorException, ParserException {
        String testPayload = "  {\n" +
                "    \"car\":\"Lambogini\",\n" +
                "    \"palm\": true,\n" +
                "       \"a32&o\": 89,\n" +
                "       \"fiddle\": [\"34\",34],\n" +
                "       \"apple\": true\n" +
                "   }";
        JsonObject schemaObject = (JsonObject) parser.parse(getAdditionalPropertyAsObject);
        JsonObject expected = (JsonObject) parser.parse(testPayload);
        JsonObject result = ObjectValidator.validateObject(expected, schemaObject);
        Assert.assertNotNull("Validator didn't respond with a JSON object", result);
        Assert.assertEquals("Didn't receive the expected object", expected, result);
    }

    /**
     * This test checks for a invalid additional properties condition.
     */
    @Test
    public void testInvalidAdditionalPropObject() throws ValidatorException, ParserException {
        thrown.expect(ParserException.class);
        String testPayload = "  {\n" +
                "    \"car\":\"Lambogini\",\n" +
                "    \"palm\": true,\n" +
                "       \"a32&o\": 89,\n" +
                "       \"fiddle\": [\"34\",34.56],\n" +
                "       \"apple\": true\n" +
                "   }";
        JsonObject schemaObject = (JsonObject) parser.parse(getAdditionalPropertyAsObject);
        JsonObject expected = (JsonObject) parser.parse(testPayload);
        ObjectValidator.validateObject(expected, schemaObject);
    }

    private static String arrayInsideObject = "{\"type\": \"object\",\n" +
            "  \"properties\":{\n" +
            "  \t\"car\":{\"type\":\"string\"},\n" +
            "  \t\"prize\":{\"type\":\"integer\"},\n" +
            "  \t\"arr\" : {\"type\":\"array\",\n" +
            "            \"items\":[{\"type\":\"integer\"},{\"type\":\"number\"}]\n" +
            "            ,\"additionalItems\":false}\n" +
            "}}";

    /**
     * This test checks for a valid array inside object scenario.
     */
    @Test
    public void testValidArrayInsideObject() throws ValidatorException, ParserException {
        String testPayload = "{\"car\":123,\"prize\":34,\"arr\":[23,34.45]}";
        JsonObject schemaObject = (JsonObject) parser.parse(arrayInsideObject);
        JsonObject expected = (JsonObject) parser.parse(testPayload);
        JsonObject result = ObjectValidator.validateObject(expected, schemaObject);
        Assert.assertNotNull("Validator didn't respond with a JSON object", result);
        Assert.assertEquals("Didn't receive the expected object", expected, result);
    }

    /**
     * This test checks for a invalid array inside object scenario.
     */
    @Test
    public void testInvalidArrayInsideObject() throws ValidatorException, ParserException {
        thrown.expect(ValidatorException.class);
        String testPayload = "{\"car\":123,\"prize\":34,\"arr\":[23,34.45,34]}";
        JsonObject schemaObject = (JsonObject) parser.parse(arrayInsideObject);
        JsonObject expected = (JsonObject) parser.parse(testPayload);
        ObjectValidator.validateObject(expected, schemaObject);
    }

    private static String schemaWithNull = "   {\n" +
            "       \"properties\": {\n" +
            "             \"car\":{\"type\":\"string\"},  \"van\":{\"type\":\"null\"} }" +
            "       }";


    /**
     * This test checks for a valid null input.
     */
    @Test
    public void testValidObjectWithNull() throws ValidatorException, ParserException {
        String testPayload = "  {\n" +
                "    \"car\":\"Lambogini\",\n" +
                "    \"van\": null }";
        String expectedPayload = "  {\n" +
                "    \"car\":\"Lambogini\",\n" +
                "    \"van\": null }";
        JsonObject schemaObject = (JsonObject) parser.parse(schemaWithNull);
        JsonObject payload = (JsonObject) parser.parse(testPayload);
        JsonObject expected = (JsonObject) parser.parse(expectedPayload);
        JsonObject result = ObjectValidator.validateObject(payload, schemaObject);
        Assert.assertNotNull("Validator didn't respond with a JSON object", result);
        Assert.assertEquals("Didn't receive the expected object", expected, result);
    }

    /**
     * This test checks for an invalid null input.
     */
    @Test
    public void testInvalidObjectWithNull() throws ValidatorException, ParserException {
        thrown.expect(ValidatorException.class);
        String testPayload = "  {\n" +
                "    \"car\":\"Lambogini\",\n" +
                "    \"van\": \"bla\" }";
        JsonObject schemaObject = (JsonObject) parser.parse(schemaWithNull);
        JsonObject payload = (JsonObject) parser.parse(testPayload);
        ObjectValidator.validateObject(payload, schemaObject);
    }
}
