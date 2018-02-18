/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.apache.synapse.commons.json;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Tests if certain json values are processed correctly.
 */
public class JsonValueTest extends TestCase {
    private static Log logger = LogFactory.getLog(JsonValueTest.class.getName());

    private static final String stringVal = "\"Hello world!\"";
    private static final String nullVal = "null";
    private static final String trueVal = "true";
    private static final String falseVal = "false";
    private static final String numberVal1 = "12.5";
    private static final String numberVal2 = "-12.5";
    private static final String numberVal3 = "123";
    private static final String failTestVal1 = "123abc";
    private static final String failTestVal2 = "task";
    private static final String failTestVal3 = "fun";
    private static final String failTestVal4 = "abc";
    private static final String failTestVal5 = "-abc";
    private static final String failTestVal6 = "\"Hello world!";

    /**
     * Tests JSON value enclosed in double quotes.
     */
    public void testCase1() {
        runTest(stringVal);
    }

    /**
     * Tests JSON value null.
     */
    public void testCase2() {
        runTest(nullVal);
    }

    /**
     * Tests JSON value true.
     */
    public void testCase3() {
        runTest(trueVal);
    }

    /**
     * Tests JSON value false.
     */
    public void testCase4() {
        runTest(falseVal);
    }

    /**
     * Tests JSON value number with a decimal point.
     */
    public void testCase5() {
        runTest(numberVal1);
    }

    /**
     * Tests JSON value negative number with a decimal point.
     */
    public void testCase6() {
        runTest(numberVal2);
    }

    /**
     * Tests JSON value number.
     */
    public void testCase7() {
        runTest(numberVal3);
    }

    /**
     * Convenient function to test correct JSON values.
     *
     * @param value the {@link String} value to be tested
     */
    private void runTest(String value) {
        try {
            InputStream inputStream = Util.newInputStream(value.getBytes());
            MessageContext messageContext = Util.newMessageContext();
            OMElement element = JsonUtil.getNewJsonPayload(messageContext, inputStream, true, true);
            OutputStream out = Util.newOutputStream();
            JsonUtil.writeAsJson(element, out);
            assertTrue(value.equals(out.toString()));
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            assertTrue(false);
        }
    }

    /**
     * Tests incorrect JSON value: text starting with number without quotes.
     */
    public void testCase8() {
        runFailTest(failTestVal1);
    }

    /**
     * Tests incorrect JSON value: text starting with t without quotes.
     */
    public void testCase9() {
        runFailTest(failTestVal2);
    }

    /**
     * Tests incorrect JSON value: text starting with f without quotes.
     */
    public void testCase10() {
        runFailTest(failTestVal3);
    }

    /**
     * Tests incorrect JSON value: text without quotes.
     */
    public void testCase11() {
        runFailTest(failTestVal4);
    }

    /**
     * Tests incorrect JSON value: text starting with - without quotes.
     */
    public void testCase12() {
        runFailTest(failTestVal5);
    }

    /**
     * Tests incorrect JSON value: text not having a closing quote.
     */
    public void testCase13() {
        runFailTest(failTestVal6);
    }

    /**
     * Convenient function to test incorrect JSON values
     *
     * @param value the {@link String} value to be tested
     */
    private void runFailTest(String value) {
        try {
            InputStream inputStream = Util.newInputStream(value.getBytes());
            MessageContext messageContext = Util.newMessageContext();
            JsonUtil.getNewJsonPayload(messageContext, inputStream, true, true);
            fail("AxisFault exception has to be thrown");
        } catch (AxisFault ex) {
            //Test succeeds
        }
    }

    public void testWriteAsJsonNullElement() throws IOException {
        OutputStream out = Util.newOutputStream();
        try {
            JsonUtil.writeAsJson((OMElement) null, out);
            Assert.fail("AxisFault expected");
        } catch (AxisFault axisFault) {
            Assert.assertEquals("Invalid fault message received", "OMElement is null. Cannot convert to JSON.",
                                axisFault.getMessage());
        } finally {
            out.close();
        }
    }
}
