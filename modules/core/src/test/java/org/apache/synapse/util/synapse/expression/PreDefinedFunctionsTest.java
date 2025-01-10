/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.util.synapse.expression;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Test class for pre-defined functions.
 */
public class PreDefinedFunctionsTest {
    @Test
    public void testLength() {
        Assert.assertEquals("6", TestUtils.evaluateExpression("length(\"Lahiru\")"));
        Assert.assertEquals("16", TestUtils.evaluateExpression("length(\"Lahiru\") + 10"));
        Assert.assertEquals("3", TestUtils.evaluateExpression("length([\"LAHIRU\",3,5])"));
        Assert.assertEquals("6", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "length(payload.store.book)", 2, 0));
        Assert.assertEquals("6", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "length(vars.cars)", 2, 1));
        Assert.assertEquals("0", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "length(vars[\"empty\"])", 2, 1));
        Assert.assertEquals("", TestUtils.evaluateExpression("length(34)"));
        Assert.assertEquals("", TestUtils.evaluateExpression("length(null)"));
    }

    @Test
    public void testToUpper() {
        Assert.assertEquals("LAHIRU", TestUtils.evaluateExpression("toUpper(\"lahiru\")"));
        Assert.assertEquals("JOHN", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "toUpper(payload.name)", 1, 1));
        Assert.assertEquals("SPACE X", TestUtils.evaluateExpression("toUpper(\"space\") + \" X\""));
        Assert.assertEquals("GEORGE ORWELL",
                TestUtils.evaluateExpressionWithPayload("toUpper(payload.store.book[4].author)", 2));
        Assert.assertEquals("", TestUtils.evaluateExpression("toUpper(null)"));
        Assert.assertEquals("", TestUtils.evaluateExpression("toUpper(34)"));
        Assert.assertEquals("", TestUtils.evaluateExpressionWithPayloadAndVariables("toUpper(vars[\"empty\"])", 0, 1));
    }

    @Test
    public void testToLower() {
        Assert.assertEquals("lahiru", TestUtils.evaluateExpression("toLower(\"LAHIRU\")"));
        Assert.assertEquals("the diary of a young girl", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "toLower(payload[\"store\"][\"book\"][5][\"title\"])", 2, 1));
        Assert.assertEquals("europa clipper", TestUtils.evaluateExpression("\"europa \" + toLower(\"CLIpper\")"));
        Assert.assertEquals("george orwell",
                TestUtils.evaluateExpressionWithPayload("toLower(payload.store.book[4].author)", 2));
        Assert.assertEquals("", TestUtils.evaluateExpression("toLower(34)"));
        Assert.assertEquals("", TestUtils.evaluateExpression("toLower(null)"));
        Assert.assertEquals("", TestUtils.evaluateExpressionWithPayloadAndVariables("toLower(vars[\"empty\"])",
                0, 1));
    }

    @Test
    public void testSubString() {
        Assert.assertEquals("hiru", TestUtils.evaluateExpression("subString(\"Lahiru\",2)"));
        Assert.assertEquals("hi", TestUtils.evaluateExpression("subString(\"Lahiru\",2,4)"));
        Assert.assertEquals("2", TestUtils.evaluateExpression("length(toUpper(subString(\"Lahiru\",2,4)))"));
        Assert.assertEquals("", TestUtils.evaluateExpression("subString(\"Hello\",5)"));
        Assert.assertEquals("", TestUtils.evaluateExpression("subString(null,5)"));
        Assert.assertEquals("", TestUtils.evaluateExpression("subString(\"hello\",null)"));
        Assert.assertEquals("", TestUtils.evaluateExpression("subString(\"Hello\",\"a\",4)"));
        Assert.assertEquals("", TestUtils.evaluateExpression("subString(\"Hello\",\"a\")"));
        Assert.assertEquals("", TestUtils.evaluateExpression("subString(\"Hello\",20)"));
        Assert.assertEquals("", TestUtils.evaluateExpression("subString(\"Hello\",-2)"));
        Assert.assertEquals("", TestUtils.evaluateExpression("subString(\"Hello\",-2,4)"));
        Assert.assertEquals("", TestUtils.evaluateExpression("subString(\"Hello\",2,vars.num1)"));
    }

    @Test
    public void testStartsWith() {
        Assert.assertEquals("true", TestUtils.evaluateExpression("startsWith(\"Curiosity\",\"Curi\")"));
        Assert.assertEquals("false", TestUtils.evaluateExpression("startsWith(\"Curiosity\",\"sity\")"));
        Assert.assertEquals("false", TestUtils.evaluateExpressionWithPayload(
                "startsWith(\"Curiosity\",$.store.bicycle.color)", 2));
        Assert.assertEquals("true", TestUtils.evaluateExpressionWithPayload(
                "startsWith(\"red flag\",$.store.bicycle.color)", 2));
        Assert.assertEquals("", TestUtils.evaluateExpression("startsWith(\"Curiosity\",34)"));
        Assert.assertEquals("", TestUtils.evaluateExpressionWithPayload(
                "startsWith(payload.cars,\"Ford\")", 1));
    }

    @Test
    public void testEndsWith() {
        Assert.assertEquals("true", TestUtils.evaluateExpression("endsWith(\"Opportunity\",\"unity\")"));
        Assert.assertEquals("false", TestUtils.evaluateExpression("endsWith(\"Opportunity\",\"tune\")"));
        Assert.assertEquals("false", TestUtils.evaluateExpressionWithPayload(
                "endsWith(\"Curiosity\",$.store.bicycle.color)", 2));
        Assert.assertEquals("true", TestUtils.evaluateExpressionWithPayload(
                "endsWith(\"discovered\",$.store.bicycle.color)", 2));
        Assert.assertEquals("", TestUtils.evaluateExpression("endsWith(\"Curiosity\",34)"));
        Assert.assertEquals("", TestUtils.evaluateExpressionWithPayload(
                "endsWith(payload.cars,\"Ford\")", 1));
    }

    @Test
    public void testContains() {
        Assert.assertEquals("true", TestUtils.evaluateExpression("contains(\"Perseverance\",\"sever\")"));
        Assert.assertEquals("false", TestUtils.evaluateExpression("contains(\"Perseverance\",\"server\")"));
        Assert.assertEquals("false", TestUtils.evaluateExpressionWithPayload(
                "contains(\"Perseverance\",$.store.bicycle.color)", 2));
        Assert.assertEquals("true", TestUtils.evaluateExpressionWithPayload(
                "contains(\"discovered\",$.store.bicycle.color)", 2));
        Assert.assertEquals("false", TestUtils.evaluateExpression("contains(\"sever\",\"Perseverance\")"));
        Assert.assertEquals("", TestUtils.evaluateExpression("contains(\"Curiosity\",34)"));
        Assert.assertEquals("", TestUtils.evaluateExpressionWithPayload(
                "contains(payload.cars,\"Ford\")", 1));
    }

    @Test
    public void testTrim() {
        Assert.assertEquals("Ingenuity", TestUtils.evaluateExpression("trim(\" Ingenuity \")"));
        Assert.assertEquals("true", TestUtils.evaluateExpression("trim(\" Ingenuity \") == \"Ingenuity\""));
        Assert.assertEquals("Hello World", TestUtils.evaluateExpressionWithPayload("trim($[\"string\"])", 1));
        Assert.assertEquals("", TestUtils.evaluateExpression("trim(34)"));
    }

    @Test
    public void testReplace() {
        Assert.assertEquals("Heppo", TestUtils.evaluateExpression("replace(\"Hello\", \"l\", \"p\")"));
        Assert.assertEquals("Hello", TestUtils.evaluateExpression("replace(\"Hello\", \"p\", \"q\")"));
        Assert.assertEquals("Hello", TestUtils.evaluateExpressionWithPayload(
                "replace(\"Hello\", \"p\", payload.name)", 1));
        Assert.assertEquals("John has a BMW", TestUtils.evaluateExpressionWithPayload(
                "replace(\"John has a \" + payload.cars[2], \"Fiat\" , payload.cars[1])", 1));
        Assert.assertEquals("", TestUtils.evaluateExpressionWithPayload(
                "replace(\"John has a \" + payload.cars[2], \"Fiat\" , payload.cars)", 1));
    }

    @Test
    public void testSplit() {
        Assert.assertEquals("[\"Split\",\"a\",\"string\",\"by\",\"spaces\",\"and\",\"comma\"]",
                TestUtils.evaluateExpression("split(\"Split a string by spaces and,comma\", \"[, ]\")"));
        Assert.assertEquals("7", TestUtils.evaluateExpression(
                "length(split(\"Split a string by spaces and,comma\", \"[, ]\"))"));
        Assert.assertEquals("6", TestUtils.evaluateExpression(
                "length(split(\"NASA, launches; rovers to explore. Mars\", \"[,; .]+\"))"));
        Assert.assertEquals("[\" Moon Mars \"]", TestUtils.evaluateExpression(
                "split(\" Moon Mars \", \",\")"));
        Assert.assertEquals("", TestUtils.evaluateExpressionWithPayload(
                "split(payload.cars, \"34\")", 1));
    }

    @Test
    public void testAbs() {
        Assert.assertEquals("5", TestUtils.evaluateExpression("abs(-5)"));
        Assert.assertEquals("5", TestUtils.evaluateExpression("abs(5)"));
        Assert.assertEquals("5.0", TestUtils.evaluateExpression("abs(-5.0)"));
        Assert.assertEquals("2.0", TestUtils.evaluateExpressionWithPayloadAndVariables("abs(vars.num4)", 0, 1));
        Assert.assertEquals("8.0", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "abs(vars.num4 + payload.expensive )", 2, 1));
        Assert.assertEquals("4.0", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "abs(vars.num1 / vars.num3)", 2, 1));
        Assert.assertEquals("", TestUtils.evaluateExpressionWithPayload("abs(payload.cars)", 1));
    }

    @Test
    public void testFloor() {
        Assert.assertEquals("-6.0", TestUtils.evaluateExpression("floor(-5.4)"));
        Assert.assertEquals("5.0", TestUtils.evaluateExpression("floor(5.9)"));
        Assert.assertEquals("-3.0", TestUtils.evaluateExpressionWithPayloadAndVariables("floor(vars.num3)", 0, 1));
        Assert.assertEquals("2.0", TestUtils.evaluateExpressionWithPayloadAndVariables("floor(-1 * vars.num3)", 0, 1));
        Assert.assertEquals("", TestUtils.evaluateExpressionWithPayload("floor(payload.cars)", 1));
    }

    @Test
    public void testCeil() {
        Assert.assertEquals("-5.0", TestUtils.evaluateExpression("ceil(-5.4)"));
        Assert.assertEquals("6.0", TestUtils.evaluateExpression("ceil(5.9)"));
        Assert.assertEquals("-2.0", TestUtils.evaluateExpressionWithPayloadAndVariables("ceil(vars.num3)", 0, 1));
        Assert.assertEquals("3.0", TestUtils.evaluateExpressionWithPayloadAndVariables("ceil(-1 * vars.num3)", 0, 1));
        Assert.assertEquals("", TestUtils.evaluateExpressionWithPayload("ceil(payload.cars)", 1));
    }

    @Test
    public void testSqrt() {
        Assert.assertEquals("5.0", TestUtils.evaluateExpression("sqrt(25)"));
        Assert.assertEquals("NaN", TestUtils.evaluateExpression("sqrt(-25.0)"));
        Assert.assertEquals("10.0", TestUtils.evaluateExpressionWithPayloadAndVariables("sqrt(vars.num1 * vars.num1)", 0, 1));
        Assert.assertEquals("2.5", TestUtils.evaluateExpressionWithPayloadAndVariables("sqrt(vars.num3 * vars.num3)", 0, 1));
        Assert.assertEquals("", TestUtils.evaluateExpressionWithPayload("sqrt(payload.cars)", 1));
    }

    @Test
    public void testPow() {
        Assert.assertEquals("8.0", TestUtils.evaluateExpression("pow(2,3)"));
        Assert.assertEquals("1.0", TestUtils.evaluateExpression("pow(2,0)"));
        Assert.assertEquals("0.25", TestUtils.evaluateExpression("pow(2,-2)"));
        Assert.assertEquals("1.0", TestUtils.evaluateExpressionWithPayloadAndVariables("pow(vars.num1,0)", 0, 1));
        Assert.assertEquals("1.0", TestUtils.evaluateExpressionWithPayloadAndVariables("pow(vars.num3,0)", 0, 1));
        Assert.assertEquals("0.16", TestUtils.evaluateExpressionWithPayloadAndVariables("pow(vars.num3,-2)", 0, 1));
        Assert.assertEquals("", TestUtils.evaluateExpressionWithPayload("pow(payload.cars,2)", 1));
    }

    @Test
    public void testB64Encode() {
        Assert.assertEquals("SGVsbG8gV29ybGQ=", TestUtils.evaluateExpression("base64encode(\"Hello World\")"));
        Assert.assertEquals("Sm9obg==", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "base64encode(payload[\"name\"])", 1, 0));
        Assert.assertEquals("Sm9obg==", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "base64encode(vars.name,\"UTF-8\")", 0, 1));
        Assert.assertEquals("", TestUtils.evaluateExpression("base64encode(34)"));
        Assert.assertEquals("", TestUtils.evaluateExpression("base64encode(\"Hello\",\"UTF-99\")"));
    }

    @Test
    public void testB64Decode() {
        Assert.assertEquals("admin:admin", TestUtils.evaluateExpression("base64decode(\"YWRtaW46YWRtaW4=\")"));
        Assert.assertEquals("WSO2MI", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "base64decode(vars.encoded)", 0, 1));
        Assert.assertEquals("", TestUtils.evaluateExpression("base64decode(34)"));
    }

    @Test
    public void testUrlEncode() {
        Assert.assertEquals("Hello+World", TestUtils.evaluateExpression("urlEncode(\"Hello World\")"));
        Assert.assertEquals("+Hello+World+", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "urlEncode(payload[\"string\"])", 1, 0));
        Assert.assertEquals("", TestUtils.evaluateExpression("urlEncode(34)"));
        Assert.assertEquals("", TestUtils.evaluateExpression("urlEncode(\"Hello\",\"UTF-99\")"));
    }

    @Test
    public void testUrlDecode() {
        Assert.assertEquals("Hello World", TestUtils.evaluateExpression("urlDecode(\"Hello+World\")"));
        Assert.assertEquals(" Hello World  &", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "urlDecode(payload[\"string\"]) + \" \" + urlDecode(\"%26\")", 1, 0));
        Assert.assertEquals("", TestUtils.evaluateExpression("urlDecode(34)"));
    }

    @Test
    public void testIsNumber() {
        Assert.assertEquals("true", TestUtils.evaluateExpression("isNumber(34)"));
        Assert.assertEquals("false", TestUtils.evaluateExpression("isNumber(\"34\")"));
        Assert.assertEquals("true", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "isNumber(payload[\"age\"]) && isNumber(vars.num1)", 1, 1));
        Assert.assertEquals("false", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "isNumber(payload[\"string\"]) || isNumber(vars.name)", 1, 1));
        Assert.assertEquals("false", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "isNumber(vars[\"empty\"])", 1, 1));
        Assert.assertEquals("", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "isNumber(vars[\"empty2\"])", 1, 1));
        Assert.assertEquals("false", TestUtils.evaluateExpression("isNumber(\"Hello\")"));
        Assert.assertEquals("", TestUtils.evaluateExpression("isNumber(null)"));
    }

    @Test
    public void testIsString() {
        Assert.assertEquals("true", TestUtils.evaluateExpression("isString(\"Hello\")"));
        Assert.assertEquals("false", TestUtils.evaluateExpression("isString(34)"));
        Assert.assertEquals("true", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "isString(payload[\"name\"]) && isString(vars.name)", 1, 1));
        Assert.assertEquals("false", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "isString(payload[\"age\"]) || isString(vars.num1)", 1, 1));
        Assert.assertEquals("true", TestUtils.evaluateExpression("isString(\"34\")"));
        Assert.assertEquals("", TestUtils.evaluateExpression("isString(null)"));
    }

    @Test
    public void testIsArray() {
        Assert.assertEquals("true", TestUtils.evaluateExpression("isArray([\"Hello\",34])"));
        Assert.assertEquals("false", TestUtils.evaluateExpression("isArray(\"Hello\")"));
        Assert.assertEquals("true", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "isArray(payload[\"cars\"]) && isArray(vars.cars)", 1, 1));
        Assert.assertEquals("false", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "isArray(payload[\"age\"]) || isArray(vars.num1)", 1, 1));
        Assert.assertEquals("true", TestUtils.evaluateExpression("isArray([\"34\"])"));
        Assert.assertEquals("true", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "isArray(payload)", 3, 1));
        Assert.assertEquals("", TestUtils.evaluateExpression("isArray(null)"));
        Assert.assertEquals("true", TestUtils.evaluateExpression("isArray(\"[1,2,{\\\"abc\\\":5}]\")"));
        Assert.assertEquals("false", TestUtils.evaluateExpression("isArray(\"[1,2,{\\\"abc:5}]\")"));
    }

    @Test
    public void testIsObject() {
        Assert.assertEquals("false", TestUtils.evaluateExpression("isObject(\"Hello\")"));
        Assert.assertEquals("false", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "isObject(payload) && isObject(vars.name)", 3, 1));
        Assert.assertEquals("false", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "isObject(payload[\"age\"]) || isObject(vars.num1)", 1, 1));
        Assert.assertEquals("true", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "isObject(payload.store)", 2, 1));
        Assert.assertEquals("", TestUtils.evaluateExpression("isObject(null)"));
        Assert.assertEquals("true", TestUtils.evaluateExpression("isObject(\"{\\\"hello\\\": \\\"world\\\"}\")"));
        Assert.assertEquals("false", TestUtils.evaluateExpression("isObject(\"{\\\"hello: \\\"world\\\"}\")"));
    }

    @Test
    public void testConvertToString() {
        Assert.assertEquals("12 Angry Men", TestUtils.evaluateExpression("string(12) + \" Angry Men\""));
        Assert.assertEquals("Hello", TestUtils.evaluateExpression("string(\"Hello\")"));
        Assert.assertEquals("true", TestUtils.evaluateExpression("string(true)"));
        Assert.assertEquals("false", TestUtils.evaluateExpression("string(false)"));
        Assert.assertEquals("[\"When\",\"my\",\"time\",\"comes\",\"Forget\",\"the\",\"wrong\",\"that\"," +
                "\"I've\",\"done\"]", TestUtils.evaluateExpressionWithPayload("string(payload)", 3));
        Assert.assertEquals("300", TestUtils.evaluateExpressionWithPayloadAndVariables("string(vars.age * 10)", 0, 1));
        Assert.assertEquals("", TestUtils.evaluateExpression("string(null)"));
    }

    @Test
    public void testConvertToInteger() {
        Assert.assertEquals("34", TestUtils.evaluateExpression("integer(34)"));
        Assert.assertEquals("20", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "integer(payload[\"expensive\"]) + vars.num1", 2, 1));
        Assert.assertEquals("", TestUtils.evaluateExpression("integer(\"Hello\")"));
        Assert.assertEquals("", TestUtils.evaluateExpression("integer(34.5)"));
        Assert.assertEquals("", TestUtils.evaluateExpression("integer(null)"));
        Assert.assertEquals("", TestUtils.evaluateExpressionWithPayload("integer(payload)", 3));
    }

    @Test
    public void testConvertToFloat() {
        Assert.assertEquals("-34.0", TestUtils.evaluateExpression("float(-34)"));
        Assert.assertEquals("15.0", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "float(payload[\"expensive\"]) + vars.num2", 2, 1));
        Assert.assertEquals("", TestUtils.evaluateExpression("float(\"Hello\")"));
        Assert.assertEquals("", TestUtils.evaluateExpressionWithPayload("float(payload)", 3));
    }

    @Test
    public void testConvertToBoolean() {
        Assert.assertEquals("true", TestUtils.evaluateExpression("boolean(\"true\")"));
        Assert.assertEquals("false", TestUtils.evaluateExpression("boolean(\"bla\")"));
        Assert.assertEquals("false", TestUtils.evaluateExpression("boolean(\"1\")"));
        Assert.assertEquals("true", TestUtils.evaluateExpression("boolean(\"0\") || (5 > 2)"));
        Assert.assertEquals("false", TestUtils.evaluateExpressionWithPayload("boolean(payload)", 3));
        Assert.assertEquals("", TestUtils.evaluateExpression("boolean(null)"));
    }

    @Test
    public void testRound() {
        Assert.assertEquals("5", TestUtils.evaluateExpression("round(5.4)"));
        Assert.assertEquals("6", TestUtils.evaluateExpression("round(5.6)"));
        Assert.assertEquals("-2", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "round(vars.num4)", 0, 1));
        Assert.assertEquals("15", TestUtils.evaluateExpressionWithPayloadAndVariables(
                "round(vars.num1 + vars.num2)", 0, 1));
        Assert. assertEquals("", TestUtils.evaluateExpressionWithPayload("round(payload)", 3));
    }

    @Test
    public void testExists() {
        TestUtils.clearMessageContext();
        Assert.assertEquals("false", TestUtils.evaluateExpression("exists(vars.num1)"));
        Assert.assertEquals("false", TestUtils.evaluateExpression("exists(null)"));
        Assert.assertEquals("John", TestUtils.evaluateExpressionWithPayload(
                "exists($.fullName) ? $.fullName : $.name", 1));
    }

    @Test
    public void testConvertToObject() {
        Assert.assertEquals("{\"hello\":\"world\"}", TestUtils.evaluateExpressionWithPayload(
                "object(\"{\\\"hello\\\":\\\"world\\\"}\")", 2));
        Assert.assertEquals("world", TestUtils.evaluateExpressionWithPayload(
                "object(\"{\\\"hello\\\":\\\"world\\\"}\").hello", 2));
        Assert.assertEquals("world", TestUtils.evaluateExpressionWithPayload(
                "object(\"{\\\"hello\\\":\\\"world\\\"}\")[\"hello\"]", 2));
        Assert.assertEquals("", TestUtils.evaluateExpression(
                "object(\"[\\\"Hello\\\",\\\"World\\\"]\")"));
        Assert.assertEquals("", TestUtils.evaluateExpression("object(34)"));
        Assert.assertEquals("", TestUtils.evaluateExpression("object(null)"));
        Assert.assertEquals("", TestUtils.evaluateExpressionWithPayload("object(payload)", 3));
    }

    @Test
    public void testConvertToArray() {
        Assert.assertEquals("", TestUtils.evaluateExpression("registry(\"Hello\").bla"));
        Assert.assertEquals("[\"Hello\",\"World\"]", TestUtils.evaluateExpression(
                "array(\"[\\\"Hello\\\",\\\"World\\\"]\")"));
        Assert.assertEquals("Hello", TestUtils.evaluateExpression(
                "array(\"[\\\"Hello\\\",\\\"World\\\"]\")[0]"));
        Assert.assertEquals("", TestUtils.evaluateExpression("array(34)"));
        Assert.assertEquals("", TestUtils.evaluateExpression("array(null)"));
        Assert.assertEquals("", TestUtils.evaluateExpressionWithPayload("array(payload)", 2));
        Assert.assertEquals("TIME", TestUtils.evaluateExpressionWithPayload(
                "toUpper(array(payload)[2])", 3));
    }

    @Test
    public void testNot() {
        TestUtils.clearMessageContext();
        Assert.assertEquals("false", TestUtils.evaluateExpression("not(true)"));
        Assert.assertEquals("true", TestUtils.evaluateExpression("not(false)"));
        Assert.assertEquals("true", TestUtils.evaluateExpression("not(5 > 6) ? true : false"));
        Assert.assertEquals("", TestUtils.evaluateExpression("not(123)"));
    }

    @Test
    public void testIndexOf() {
        Assert.assertEquals("6", TestUtils.evaluateExpression("indexOf(\"Hello World\", \"World\")"));
        Assert.assertEquals("-1", TestUtils.evaluateExpression("indexOf(\"Hello World\", \"World2\")"));
        Assert.assertEquals("8", TestUtils.evaluateExpression("indexOf(\"Hello World\", \"r\")"));
        Assert.assertEquals("9", TestUtils.evaluateExpression("indexOf(\"Hello World\", \"l\",5)"));
        Assert.assertEquals("-1", TestUtils.evaluateExpression("indexOf(\"Hello World\", \"l\",50)"));
        Assert.assertEquals("7", TestUtils.evaluateExpressionWithPayload("indexOf($.string, \"World\")", 1));
    }

    @Test
    public void testCharAt() {
        Assert.assertEquals("W", TestUtils.evaluateExpression("charAt(\"Hello World\", 6)"));
        Assert.assertEquals("", TestUtils.evaluateExpression("charAt(\"Hello World\", -1)"));
        Assert.assertEquals("", TestUtils.evaluateExpression("charAt(\"Hello World\", 100)"));
        Assert.assertEquals(" ", TestUtils.evaluateExpressionWithPayload("charAt($.string, 0)", 1));
    }

    @Test
    public void testFormatDateTime() {
        Assert.assertEquals(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")),
                TestUtils.evaluateExpression("formatDateTime(now(), \"dd-MM-yyyy HH:mm\")"));
        Assert.assertEquals(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                TestUtils.evaluateExpression("formatDateTime(now(), \"yyyy-MM-dd\")"));
        Assert.assertEquals("1988-09-29",
                TestUtils.evaluateExpression("formatDateTime(\"29/09/1988\",\"dd/MM/yyyy\", \"yyyy-MM-dd\")"));
        Assert.assertEquals("1988 Sep 29",
                TestUtils.evaluateExpression("formatDateTime(\"29-09-1988\",\"dd-MM-yyyy\", \"yyyy MMM dd\")"));
        Assert.assertEquals("11 22 33",
                TestUtils.evaluateExpression("formatDateTime(\"11-22-33\",\"HH-mm-ss\", \"HH mm ss\")"));
        Assert.assertEquals("", TestUtils.evaluateExpression("formatDateTime(\"50-22-33\",\"HH-mm-ss\", \"HH mm ss\")"));
    }
}
