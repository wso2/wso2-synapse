/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.commons.json.jsonparser.integrationTests;

import com.google.gson.JsonParser;
import org.apache.commons.io.IOUtils;
import org.apache.synapse.commons.json.jsonparser.exceptions.ParserException;
import org.apache.synapse.commons.json.jsonparser.exceptions.ValidatorException;
import org.apache.synapse.commons.json.jsonparser.parser.JavaJsonParser;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * This class will test integration scenarios among validators.
 */
public class TestJavaJsonParser {

    private static JsonParser parser;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void init() {
        parser = new JsonParser();
    }

    /**
     * This test checks method that accept string as schema
     * @throws ValidatorException
     * @throws ParserException
     * @throws IOException
     */
    @Test
    public void testStringMethod() throws ValidatorException, ParserException, IOException {
        //Reading input.json and validatingInput.json from files
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        //InputStream inputStream = classloader.getResourceAsStream("input.json");
        //String inputJson = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        InputStream inputStream = classloader.getResourceAsStream("schema.json");
        String inputJson = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        inputStream = classloader.getResourceAsStream("validatingInput.json");
        String validatingInput = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

        //creating instances
        String result = JavaJsonParser.parseJson(validatingInput, inputJson);
        String expected = "{\"fruit\":\"12345\",\"price\":7.5,\"simpleObject\":{\"age\":234}," +
                "\"simpleArray\":[true,false,\"true\"],\"objWithArray\":{\"marks\":[34,45,56,67]}," +
                "\"arrayOfObjects\":[{\"maths\":90},{\"physics\":95},{\"chemistry\":65}],\"singleObjArray\":[1.618]," +
                "\"nestedObject\":{\"Lahiru\":{\"age\":27},\"Nimal\":{\"married\":true},\"Kamal\":{\"scores\":[24,45," +
                "67]}},\"nestedArray\":[[12,23,34],[true,false],[\"Linking Park\",\"Coldplay\"]]," +
                "\"allNumericArray\":[3,1,4],\"Hello\":890,\"nullArray\":[null,null,null],\"league_goals\":10}";
        Assert.assertEquals("Didn't receive the expected payload after parsing", expected, result);
    }

    /**
     * This test checks data type and structure correction.
     */
    @Test
    public void testTypeAndStructureCorrection() throws ValidatorException, ParserException {
        String schema = "{\n" +
                "\"schema\": \"http://json-schema.org/draft-04/schema#\",\n" +
                "\"type\": \"object\",\n" +
                "\"properties\": {\n" +
                "\"singleObjArray\": {\n" +
                "\"type\": \"array\",\n" +
                "\"items\": [{\n" +
                "\"type\": \"object\",\n" +
                "\"properties\": {\n" +
                "\"bla\": {\n" +
                "\"type\": \"integer\"\n" +
                "}}}]}}}";
        String inputJson = "{\"singleObjArray\":{\"bla\":\"3\"}}";
        String expected = "{\"singleObjArray\":[{\"bla\":3}]}";
        String result = JavaJsonParser.parseJson(inputJson, schema);
        Assert.assertEquals("Didn't receive the expected payload after parsing", expected, result);
    }
}
