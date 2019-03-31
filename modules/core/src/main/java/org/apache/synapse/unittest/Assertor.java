/*
 Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.unittest;

import javafx.util.Pair;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.unittest.testcase.data.classes.AssertEqual;
import org.apache.synapse.unittest.testcase.data.classes.AssertNotNull;
import org.apache.synapse.unittest.testcase.data.classes.TestCase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Map;

import static org.apache.synapse.unittest.Constants.WHITESPACE_REGEX;

/**
 * Class responsible for the validation of testing with expected results.
 */
class Assertor {

    private static Logger logger = Logger.getLogger(TestingAgent.class.getName());

    private Assertor() {}

    /**
     * Assertion of results of sequence mediation and expected payload and properties.
     *
     * @param currentTestCase current test case
     * @param mediateMsgCtxt message context used for the mediation
     * @return true if assertion is success otherwise false
     */
    static Pair<Boolean,String> doAssertionSequence(TestCase currentTestCase, MessageContext mediateMsgCtxt, int testCaseNumber) {

        boolean isSequenceAssertComplete = false;
        boolean isAssertEqualComplete = false;
        boolean isAssertNotNullComplete = false;
        String assertMessage = null;

        ArrayList<AssertEqual> assertEquals = currentTestCase.getAssertEquals();
        ArrayList<AssertNotNull> assertNotNulls = currentTestCase.getAssertNotNull();

        if (!assertEquals.isEmpty()) {
            Pair<Boolean,String> assertSequence = startAssertEqualsForSequence(assertEquals, mediateMsgCtxt);
            isAssertEqualComplete = assertSequence.getKey();
            assertMessage += assertSequence.getValue();
        }

        if (!assertNotNulls.isEmpty()) {
            Pair<Boolean,String> assertSequence = startAssertNotNullsForSequence(assertNotNulls, mediateMsgCtxt);
            isAssertNotNullComplete = assertSequence.getKey();
            assertMessage += assertSequence.getValue();
        }


        if (isAssertEqualComplete && isAssertNotNullComplete) {
            isSequenceAssertComplete = true;
            logger.info("Unit testing passed for test case - " + testCaseNumber);
        } else {
            logger.error("Unit testing failed for test case - " + testCaseNumber);
        }

        return new Pair<>(isSequenceAssertComplete, assertMessage);
    }

    /**
     * Assertion of results of proxy/API invoke and expected payload.
     * @param currentTestCase current testcase
     * @param response response from the service
     * @param testCaseNumber asserting test case number
     * @return true if assertion is success otherwise false
     */
    static Pair<Boolean,String> doAssertionService(TestCase currentTestCase, HttpResponse response, int testCaseNumber) {

        boolean isServiceAssertComplete = false;
        boolean isAssertEqualComplete = false;
        boolean isAssertNotNullComplete = false;
        String assertMessage = null;

        ArrayList<AssertEqual> assertEquals = currentTestCase.getAssertEquals();
        ArrayList<AssertNotNull> assertNotNulls = currentTestCase.getAssertNotNull();

        try {
            String responseAsString = trimStrings(EntityUtils.toString(response.getEntity(), "UTF-8"));
            Header[] responseHeaders = response.getAllHeaders();

            if (!assertEquals.isEmpty()) {
                Pair<Boolean,String> assertService = startAssertEqualsForServices(assertEquals, responseAsString, responseHeaders);
                isAssertEqualComplete = assertService.getKey();
                assertMessage += assertService.getValue();
            }

            if (!assertNotNulls.isEmpty()) {
                Pair<Boolean,String> assertService = startAssertNotNullsForServices(assertNotNulls, responseAsString, responseHeaders);
                isAssertNotNullComplete = assertService.getKey();
                assertMessage += assertService.getValue();
            }
        } catch (IOException e) {
            logger.error("Error while reading response from the service HttpResponse", e);
        }

        if (isAssertEqualComplete && isAssertNotNullComplete) {
            isServiceAssertComplete = true;
            logger.info("Unit testing passed for test case - " + testCaseNumber);
        } else {
            logger.error("Unit testing failed for test case - " + testCaseNumber);
        }

        return new Pair<>(isServiceAssertComplete, assertMessage);
    }

    /**
     * Remove irrelevant whitespaces from the input string.
     * @param inputString string which needs to remove whitespaces
     * @return trim string not include irrelevant whitespaces
     */
    private static String trimStrings(String inputString) {
        String trimedString;
        BufferedReader reader = new BufferedReader(new StringReader(inputString));
        StringBuilder result = new StringBuilder();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line.trim());
            }
            trimedString =  result.toString();
        } catch (IOException e) {
            logger.error(e);
            trimedString = inputString;
        }

        return trimedString.replaceAll(WHITESPACE_REGEX, "");
    }

    private static Pair<Boolean, String> startAssertEqualsForSequence(ArrayList<AssertEqual> assertEquals, MessageContext msgCtxt) {

        logger.info("\n---------------------Assert Equals---------------------\n");
        boolean isAssertEqualFailed = false;
        String messageOfAssertEqual = null;

        for (AssertEqual assertItem : assertEquals) {

            if (!isAssertEqualFailed) {

                String actual = assertItem.getActual();
                String expected = assertItem.getExpected();
                String message = assertItem.getMessage();
                boolean isAssert;
                String[] actualType = actual.split(":");
                String mediatedResult = null;

                switch(actualType[0]) {

                    case "$body" :
                        mediatedResult = trimStrings(msgCtxt.getEnvelope().getBody().getFirstElement().toString());
                        isAssert = trimStrings(expected)
                                .equals(mediatedResult);

                        break;

                    case "$ctx" :
                        mediatedResult = trimStrings(msgCtxt.getProperty(actualType[1]).toString());
                        isAssert = trimStrings(expected)
                                .equals(mediatedResult);

                        break;

                    case "$axis2" :
                        Axis2MessageContext axis2smc = (Axis2MessageContext) msgCtxt;
                        org.apache.axis2.context.MessageContext axis2MessageCtx =
                                axis2smc.getAxis2MessageContext();

                        mediatedResult = trimStrings(axis2MessageCtx.getProperty(actualType[1]).toString());
                        isAssert = trimStrings(expected)
                                .equals(mediatedResult);

                        break;

                    case "$trp" :
                        Axis2MessageContext axis2smcTra = (Axis2MessageContext) msgCtxt;
                        org.apache.axis2.context.MessageContext axis2MessageCtxTransport =
                                axis2smcTra.getAxis2MessageContext();
                        Object headers = axis2MessageCtxTransport.getProperty(
                                org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

                        @SuppressWarnings("unchecked")
                        Map<String, Object> headersMap = (Map) headers;

                        mediatedResult = trimStrings(headersMap.get(actualType[1]).toString());
                        isAssert = trimStrings(expected)
                                .equals(mediatedResult);

                        break;

                    default:
                        isAssert = false;
                        mediatedResult = message;
                        message = "Received assert actual value not defined";
                }

                logger.info("\nAssert Actual - " + trimStrings(actual));
                logger.info("Assert Expected - " + trimStrings(expected));
                logger.info("Mediated result for actual - " + mediatedResult);
                if (isAssert) {
                    logger.info("AssertEqual for " +actualType[0] + " passed successfully");
                } else {
                    isAssertEqualFailed = true;
                    messageOfAssertEqual = message;
                    logger.error("AssertEqual for " +actualType[0] + " failed - " + message);
                }
            }

        }

        logger.info("AssertEquals assertion success - " + !isAssertEqualFailed);
        return new Pair<>(!isAssertEqualFailed, messageOfAssertEqual);
    }

    private static Pair<Boolean, String> startAssertNotNullsForSequence(ArrayList<AssertNotNull> assertNotNull, MessageContext msgCtxt) {

        logger.info("\n---------------------Assert Not Null---------------------\n");
        boolean isAssertNotNullFailed = false;
        String messageOfAssertNotNull = null;

        for (AssertNotNull assertItem : assertNotNull) {

            if (!isAssertNotNullFailed) {

                String actual = assertItem.getActual();
                String message = assertItem.getMessage();
                boolean isAssertNull;
                String[] actualType = actual.split(":");
                String mediatedResult = null;

                switch(actualType[0]) {

                    case "$body" :
                        mediatedResult = trimStrings(msgCtxt.getEnvelope().getBody().getFirstElement().toString());
                        isAssertNull = mediatedResult.isEmpty();

                        break;

                    case "$ctx" :
                        mediatedResult = trimStrings(msgCtxt.getProperty(actualType[1]).toString());
                        isAssertNull = mediatedResult.isEmpty();

                        break;

                    case "$axis2" :
                        Axis2MessageContext axis2smc = (Axis2MessageContext) msgCtxt;
                        org.apache.axis2.context.MessageContext axis2MessageCtx =
                                axis2smc.getAxis2MessageContext();

                        mediatedResult = trimStrings(axis2MessageCtx.getProperty(actualType[1]).toString());
                        isAssertNull = mediatedResult.isEmpty();

                        break;

                    case "$trp" :
                        Axis2MessageContext axis2smcTra = (Axis2MessageContext) msgCtxt;
                        org.apache.axis2.context.MessageContext axis2MessageCtxTransport =
                                axis2smcTra.getAxis2MessageContext();
                        Object headers = axis2MessageCtxTransport.getProperty(
                                org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

                        @SuppressWarnings("unchecked")
                        Map<String, Object> headersMap = (Map) headers;

                        mediatedResult = trimStrings(headersMap.get(actualType[1]).toString());
                        isAssertNull = mediatedResult.isEmpty();

                        break;

                    default:
                        isAssertNull = true;
                        mediatedResult = message;
                        message = "Received assert actual value not defined";
                }

                logger.info("\nAssert Actual - " + actual);
                logger.info("Mediated result for actual - " + mediatedResult);

                if (!isAssertNull) {
                    logger.info("AssertNotNull for " +actualType[0] + " passed successfully");
                } else {
                    isAssertNotNullFailed = true;
                    messageOfAssertNotNull = message;
                    logger.error("AssertNotNull for " +actualType[0] + " failed - " + message);
                }
            }

        }

        logger.info("AssertNotNull assertion success - " + !isAssertNotNullFailed);
        return new Pair<>(!isAssertNotNullFailed, messageOfAssertNotNull);
    }

    private static Pair<Boolean, String> startAssertEqualsForServices(ArrayList<AssertEqual> assertEquals, String response, Header[] headers) throws IOException{

        logger.info("\n---------------------Assert Equals---------------------\n");
        boolean isAssertEqualFailed = false;
        String messageOfAssertEqual = null;

        for (AssertEqual assertItem : assertEquals) {

            if (!isAssertEqualFailed) {

                String actual = assertItem.getActual();
                String expected = assertItem.getExpected();
                String message = assertItem.getMessage();
                boolean isAssert = false;
                String[] actualType = actual.split(":");
                String mediatedResult = null;

                switch(actualType[0]) {

                    case "$body" :
                        mediatedResult = response;
                        isAssert = trimStrings(expected)
                                .equals(mediatedResult);

                        break;

                    case "$trp" :

                        for (Header header : headers) {
                            if (header.getName().equals(actualType[1])) {
                                mediatedResult = trimStrings(header.getValue());
                                isAssert = trimStrings(expected).equals(mediatedResult);
                                break;
                            }
                        }
                        break;

                    default:
                        message = "Received assert actual value not defined";
                        mediatedResult = message;
                }

                logger.info("\nAssert Actual - " + actual);
                logger.info("Assert Expected - " + trimStrings(expected));
                logger.info("Mediated result for actual - " + mediatedResult);
                if (isAssert) {
                    logger.info("AssertEqual for " +actualType[0] + " passed successfully");
                } else {
                    isAssertEqualFailed = true;
                    messageOfAssertEqual = message;
                    logger.error("AssertEqual for " +actualType[0] + " failed - " + message);
                }
            }

        }

        logger.info("AssertEquals assertion success - " + !isAssertEqualFailed);
        return new Pair<>(!isAssertEqualFailed, messageOfAssertEqual);
    }

    private static Pair<Boolean, String> startAssertNotNullsForServices(ArrayList<AssertNotNull> assertNotNull, String response, Header[] headers) throws IOException{

        logger.info("\n---------------------Assert Not Null---------------------\n");
        boolean isAssertNotNullFailed = false;
        String messageOfAssertNotNull = null;

        for (AssertNotNull assertItem : assertNotNull) {

            if (!isAssertNotNullFailed) {

                String actual = assertItem.getActual();
                String message = assertItem.getMessage();
                boolean isAssertNull = false;
                String[] actualType = actual.split(":");
                String mediatedResult = null;

                switch(actualType[0]) {

                    case "$body" :
                        mediatedResult = response;
                        isAssertNull = mediatedResult.isEmpty();

                        break;

                    case "$trp" :
                        for (Header header : headers) {
                            if (header.getName().equals(actualType[1])) {
                                mediatedResult = trimStrings(header.getValue());
                                isAssertNull = mediatedResult.isEmpty();
                                break;
                            }
                        }
                        break;

                    default:
                        message = "Received assert actual value not defined";
                        mediatedResult = message;
                }

                logger.info("Assert Actual - " + actual);
                logger.info("Mediated result for actual - " + mediatedResult + "\n");

                if (!isAssertNull) {
                    logger.info("AssertEqual for " +actualType[0] + " passed successfully");
                } else {
                    isAssertNotNullFailed = true;
                    messageOfAssertNotNull = message;
                    logger.error("AssertEqual for " +actualType[0] + " failed - " + message);
                }
            }

        }

        logger.info("AssertNotNull assertion success - " + !isAssertNotNullFailed);
        return new Pair<>(!isAssertNotNullFailed, messageOfAssertNotNull);
    }
}
