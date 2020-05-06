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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.unittest;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.apache.synapse.MessageContext;
import org.apache.synapse.commons.emulator.RequestProcessor;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.unittest.testcase.data.classes.AssertEqual;
import org.apache.synapse.unittest.testcase.data.classes.AssertNotNull;
import org.apache.synapse.unittest.testcase.data.classes.TestCase;
import org.apache.synapse.unittest.testcase.data.classes.TestCaseAssertionSummary;
import org.apache.synapse.unittest.testcase.data.classes.TestCaseSummary;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamException;

import static org.apache.synapse.unittest.Constants.EMPTY_VALUE;
import static org.apache.synapse.unittest.Constants.INPUT_PROPERTY_AXIS2;
import static org.apache.synapse.unittest.Constants.INPUT_PROPERTY_BODY;
import static org.apache.synapse.unittest.Constants.INPUT_PROPERTY_CONTEXT;
import static org.apache.synapse.unittest.Constants.INPUT_PROPERTY_TRANSPORT;
import static org.apache.synapse.unittest.Constants.RESPONSE_PROPERTY_HTTP_VERSION;
import static org.apache.synapse.unittest.Constants.RESPONSE_PROPERTY_STATUS_CODE;
import static org.apache.synapse.unittest.Constants.TEXT_NAMESPACE;


/**
 * Class responsible for the validation of testing with expected results.
 */
class Assertor {

    private static Logger log = Logger.getLogger(Assertor.class.getName());

    private Assertor() {
    }

    /**
     * Assertion of results of sequence mediation and expected payload and properties.
     *
     * @param currentTestCase current test case
     * @param mediateMsgCtxt  message context used for the mediation
     * @param testCaseSummary testSummary object for this test case
     */
    static void doAssertionSequence(
            TestCase currentTestCase, MessageContext mediateMsgCtxt, TestCaseSummary testCaseSummary) {

        List<AssertEqual> assertEquals = currentTestCase.getAssertEquals();
        List<AssertNotNull> assertNotNulls = currentTestCase.getAssertNotNull();
        String testCaseName = currentTestCase.getTestCaseName();

        if (!assertEquals.isEmpty()) {
            startAssertEqualsForSequence(assertEquals, mediateMsgCtxt, testCaseSummary);
        }

        if (!assertNotNulls.isEmpty()) {
            startAssertNotNullsForSequence(assertNotNulls, mediateMsgCtxt, testCaseSummary);
        }

        if (testCaseSummary.getTestCaseAssertionList().isEmpty()) {
            testCaseSummary.setAssertionStatus(Constants.PASSED_KEY);
            log.info("Unit testing passed for the test case - " + testCaseName);
        } else {
            testCaseSummary.setAssertionStatus(Constants.FAILED_KEY);
            log.error("Unit testing failed for the test case - " + testCaseName);
        }
    }

    /**
     * Assertion of results of proxy/API invoke and expected payload.
     *
     * @param currentTestCase current testcase
     * @param response        response from the service
     * @param testCaseSummary testSummary object for this test case
     */
    static void doAssertionService(TestCase currentTestCase, Map.Entry<String, HttpResponse> response,
                                   TestCaseSummary testCaseSummary) {
        List<AssertEqual> assertEquals = currentTestCase.getAssertEquals();
        List<AssertNotNull> assertNotNulls = currentTestCase.getAssertNotNull();
        String testCaseName = currentTestCase.getTestCaseName();

        try {
            if (!assertEquals.isEmpty()) {
                startAssertEqualsForServices(assertEquals, response, testCaseSummary);
            }

            if (!assertNotNulls.isEmpty()) {
                startAssertNotNullsForServices(assertNotNulls, response, testCaseSummary);
            }
        } catch (IOException e) {
            log.error("Error while reading response from the service HttpResponse", e);
        }

        if (testCaseSummary.getTestCaseAssertionList().isEmpty()) {
            testCaseSummary.setAssertionStatus(Constants.PASSED_KEY);
            log.info("Unit testing passed for the test case - " + testCaseName);
        } else {
            testCaseSummary.setAssertionStatus(Constants.FAILED_KEY);
            log.error("Unit testing failed for the test case - " + testCaseName);
        }
    }

    /**
     * Method of assertionEquals for Sequence test cases.
     *
     * @param assertEquals   array of assertEquals
     * @param messageContext message context
     * @param testCaseSummary  testSummary object for this test case
     */
    private static void startAssertEqualsForSequence(
            List<AssertEqual> assertEquals, MessageContext messageContext, TestCaseSummary testCaseSummary) {

        log.info("AssertEquals - assert property for sequences started");

        for (AssertEqual assertItem : assertEquals) {
            TestCaseAssertionSummary testAssertion = new TestCaseAssertionSummary();
            Assertor.AssertionPrerequisite.setAssertEqualPrerequisite(assertItem);
            String mediatedResult = Constants.STRING_NULL;
            boolean isAssert = false;

            Axis2MessageContext axis2MessageContext = (Axis2MessageContext) messageContext;
            org.apache.axis2.context.MessageContext axis2MessageCtx =
                    axis2MessageContext.getAxis2MessageContext();

            switch (Assertor.AssertionPrerequisite.getExpressionPrefix()) {

                case INPUT_PROPERTY_BODY:
                    try {
                        org.apache.axis2.context.MessageContext axis2MsgContxt =
                                ((Axis2MessageContext) messageContext).getAxis2MessageContext();
                        if (JsonUtil.hasAJsonPayload(axis2MsgContxt)) {
                            if (JsonUtil.getJsonPayload(axis2MsgContxt) != null) {
                                mediatedResult = RequestProcessor.trimStrings(
                                        IOUtils.toString(JsonUtil.getJsonPayload(axis2MsgContxt)));
                            } else {
                                mediatedResult = EMPTY_VALUE;
                            }
                        } else {
                            String omElement = messageContext.getEnvelope().getBody().getFirstElement().toString();
                            if (omElement.contains(TEXT_NAMESPACE)) {
                                OMElement omElementOfText = AXIOMUtil.stringToOM(omElement);
                                mediatedResult = RequestProcessor.trimStrings(omElementOfText.getText());
                            } else {
                                mediatedResult = RequestProcessor.trimStrings(omElement);
                            }
                        }
                    } catch (XMLStreamException e) {
                        mediatedResult = EMPTY_VALUE;
                        log.error("Exception while reading the text output from the message context", e);
                    } catch (IOException e) {
                        mediatedResult = EMPTY_VALUE;
                        log.error("Exception while reading the JSON output from the message context", e);
                    }
                    isAssert = Assertor.AssertionPrerequisite.getExpected().equals(mediatedResult);
                    break;

                case INPUT_PROPERTY_CONTEXT:
                    if (messageContext.getProperty(Assertor.AssertionPrerequisite.getExpressionProperty()) != null) {
                        mediatedResult = RequestProcessor.trimStrings(messageContext.getProperty(
                                Assertor.AssertionPrerequisite.getExpressionProperty()).toString());
                        isAssert = Assertor.AssertionPrerequisite.getExpected().equals(mediatedResult);
                    }
                    break;

                case INPUT_PROPERTY_AXIS2:
                    if (axis2MessageCtx.getProperty(Assertor.AssertionPrerequisite.getExpressionProperty()) != null) {
                        mediatedResult = RequestProcessor.trimStrings(axis2MessageCtx.getProperty(
                                Assertor.AssertionPrerequisite.getExpressionProperty()).toString());
                        isAssert = Assertor.AssertionPrerequisite.getExpected().equals(mediatedResult);
                    }
                    break;

                case INPUT_PROPERTY_TRANSPORT:
                    Object headers = axis2MessageContext.getProperty(
                            org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

                    @SuppressWarnings("unchecked")
                    Map<String, Object> headersMap = (Map) headers;
                    if (headersMap.get(Assertor.AssertionPrerequisite.getExpressionProperty()) != null) {
                        mediatedResult = RequestProcessor.trimStrings(
                                headersMap.get(Assertor.AssertionPrerequisite.getExpressionProperty()).toString());
                        isAssert = Assertor.AssertionPrerequisite.getExpected().equals(mediatedResult);
                    }
                    break;

                default:
                    mediatedResult = "Received assert expression: "
                            + Assertor.AssertionPrerequisite.getAssertExpression()
                            + " is not a valid operation type for sequences";
                    log.error(mediatedResult);
            }

            log.info("Sequence Assert Expression - " + Assertor.AssertionPrerequisite.getAssertExpression());
            log.info("Sequence mediated result for Actual - " + mediatedResult);
            log.info("Sequence Assert Expected - " + Assertor.AssertionPrerequisite.getExpected());
            if (isAssert) {
                log.info("Sequence assertEqual for " + Assertor.AssertionPrerequisite.getAssertExpression()
                        + " expression passed successfully");
            } else {
                testAssertion.setAssertionType(Constants.TEST_CASE_ASSERTION_EQUALS);
                testAssertion.setAssertionExpression(Assertor.AssertionPrerequisite.getAssertExpression());
                testAssertion.setAssertionExpectedValue(Assertor.AssertionPrerequisite.getExpected());
                testAssertion.setAssertionActualValue(mediatedResult);
                testAssertion.setAssertionErrorMessage(Assertor.AssertionPrerequisite.getMessage());
                testCaseSummary.addTestCaseAssertion(testAssertion);
                log.error("Sequence assertEqual for " + Assertor.AssertionPrerequisite.getAssertExpression()
                        + " expression failed with a message - " + Assertor.AssertionPrerequisite.getMessage());
            }
        }
    }

    /**
     * Method of assertionNotNull for Sequence test cases.
     *
     * @param assertNotNull  array of assertNotNull
     * @param messageContext message context
     * @param testCaseSummary testSummary object for this test case
     */
    private static void startAssertNotNullsForSequence(
            List<AssertNotNull> assertNotNull, MessageContext messageContext, TestCaseSummary testCaseSummary) {

        log.info("Assert Not Null - assert property for sequences started");

        for (AssertNotNull assertItem : assertNotNull) {
            TestCaseAssertionSummary testAssertion = new TestCaseAssertionSummary();
            Assertor.AssertionPrerequisite.setAssertNotNullPrerequisite(assertItem);
            boolean isAssertNull = true;
            String mediatedResult = Constants.STRING_NULL;

            Axis2MessageContext axis2MessageContext = (Axis2MessageContext) messageContext;
            org.apache.axis2.context.MessageContext axis2MessageCtx =
                    axis2MessageContext.getAxis2MessageContext();

            switch (Assertor.AssertionPrerequisite.getExpressionPrefix()) {

                case INPUT_PROPERTY_BODY:
                    isAssertNull = messageContext.getEnvelope().getBody().getFirstElement() == null;
                    break;

                case INPUT_PROPERTY_CONTEXT:
                    isAssertNull =
                            messageContext.getProperty(Assertor.AssertionPrerequisite.getExpressionProperty()) == null;
                    break;

                case INPUT_PROPERTY_AXIS2:
                    isAssertNull =
                            axis2MessageCtx.getProperty(Assertor.AssertionPrerequisite.getExpressionProperty()) == null;
                    break;

                case INPUT_PROPERTY_TRANSPORT:
                    Object headers = axis2MessageCtx.getProperty(
                            org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

                    @SuppressWarnings("unchecked")
                    Map<String, Object> headersMap = (Map) headers;
                    isAssertNull = headersMap.get(Assertor.AssertionPrerequisite.getExpressionProperty()) == null;
                    break;

                default:
                    mediatedResult = "Received assert expression: "
                            + Assertor.AssertionPrerequisite.getAssertExpression()
                            + " is not a valid operation type for sequences";
                    log.error(mediatedResult);
            }

            log.info("Sequence Assertion Expression - " + Assertor.AssertionPrerequisite.getAssertExpression());
            log.info("Sequence mediated result for assertNotNull is " + !isAssertNull);

            if (!isAssertNull) {
                log.info("Sequence assertNotNull for " + Assertor.AssertionPrerequisite.getAssertExpression()
                        + " expression passed successfully");
            } else {
                testAssertion.setAssertionType(Constants.TEST_CASE_ASSERTION_NOTNULL);
                testAssertion.setAssertionExpression(Assertor.AssertionPrerequisite.getAssertExpression());
                testAssertion.setAssertionActualValue(mediatedResult);
                testAssertion.setAssertionErrorMessage(Assertor.AssertionPrerequisite.getMessage());
                testCaseSummary.addTestCaseAssertion(testAssertion);
                log.error("Sequence assertNotNull for " + Assertor.AssertionPrerequisite.getAssertExpression()
                        + " expression failed with a message - " + Assertor.AssertionPrerequisite.getMessage());
            }
        }
    }

    /**
     * Method of assertionEquals for Service test cases.
     *
     * @param assertEquals array of assertEquals
     * @param response     service's http response
     * @param testCaseSummary testSummary object for this test case
     * @throws IOException when converting http response into a string
     */
    private static void startAssertEqualsForServices(List<AssertEqual> assertEquals, Map.Entry<String,
            HttpResponse> response, TestCaseSummary testCaseSummary) throws IOException {

        log.info("Assert Equals - assert property for services started");

        for (AssertEqual assertItem : assertEquals) {
            TestCaseAssertionSummary testAssertion = new TestCaseAssertionSummary();
            Assertor.AssertionPrerequisite.setAssertEqualPrerequisite(assertItem);
            String mediatedResult = Constants.STRING_NULL;
            HttpResponse serviceResponse = response.getValue();
            HttpEntity responseEntity = serviceResponse.getEntity();
            boolean isAssert = false;

            switch (Assertor.AssertionPrerequisite.getExpressionPrefix()) {

                case INPUT_PROPERTY_BODY:
                    if (responseEntity != null) {
                        mediatedResult = RequestProcessor.trimStrings(
                                EntityUtils.toString(responseEntity, Constants.STRING_UTF8));
                        isAssert = Assertor.AssertionPrerequisite.getExpected().equals(mediatedResult);
                    }
                    break;

                case RESPONSE_PROPERTY_STATUS_CODE:
                    if (serviceResponse.getStatusLine() != null) {
                        mediatedResult = Integer.toString(serviceResponse.getStatusLine().getStatusCode());
                        isAssert = Assertor.AssertionPrerequisite.getExpected().equals(mediatedResult);
                    }
                    break;

                case RESPONSE_PROPERTY_HTTP_VERSION:
                    if (serviceResponse.getStatusLine().getProtocolVersion() != null) {
                        mediatedResult = serviceResponse.getStatusLine().getProtocolVersion().toString();
                        isAssert = Assertor.AssertionPrerequisite.getExpected().equals(mediatedResult);
                    }
                    break;

                case INPUT_PROPERTY_TRANSPORT:
                    Header[] responseHeaders = serviceResponse.getAllHeaders();
                    if (responseHeaders == null) {
                        break;
                    }
                    for (Header header : responseHeaders) {
                        if (header.getName() != null &&
                                header.getName().equals(Assertor.AssertionPrerequisite.getExpressionProperty())) {
                            if (header.getValue() != null) {
                                mediatedResult = RequestProcessor.trimStrings(header.getValue());
                                isAssert = Assertor.AssertionPrerequisite.getExpected().equals(mediatedResult);
                            }
                            break;
                        }
                    }
                    break;

                default:
                    mediatedResult = "Received assert expression: "
                            + Assertor.AssertionPrerequisite.getAssertExpression()
                            + " is not a valid operation type for services";
                    log.error(mediatedResult);
            }

            log.info("Service Assert Expression - " + Assertor.AssertionPrerequisite.getAssertExpression());
            log.info("Service mediated result for Actual - " + mediatedResult);
            log.info("Service Assert Expected - " + Assertor.AssertionPrerequisite.getExpected());

            if (isAssert) {
                log.info("Service assertEquals for " + Assertor.AssertionPrerequisite.getAssertExpression()
                        + " expression passed successfully");
            } else {
                testAssertion.setAssertionType(Constants.TEST_CASE_ASSERTION_EQUALS);
                testAssertion.setAssertionExpression(Assertor.AssertionPrerequisite.getAssertExpression());
                testAssertion.setAssertionExpectedValue(Assertor.AssertionPrerequisite.getExpected());
                testAssertion.setAssertionActualValue(mediatedResult);
                if (mediatedResult.equals(Constants.STRING_NULL)) {
                    testAssertion.setAssertionDescription("Tested service url - " + response.getKey()
                            + "\nReceived status code - " + (serviceResponse.getStatusLine() != null ?
                            serviceResponse.getStatusLine().getStatusCode() : "null"));
                }
                testAssertion.setAssertionErrorMessage(Assertor.AssertionPrerequisite.getMessage());
                testCaseSummary.addTestCaseAssertion(testAssertion);
                log.error("Service assertEquals for " + Assertor.AssertionPrerequisite.getAssertExpression()
                        + " expression failed with a message - " + Assertor.AssertionPrerequisite.getMessage());
            }
        }
    }

    /**
     * Method of assertNotNull for Service test cases.
     *
     * @param assertNotNull array of assertNotNull
     * @param response     service's http response
     * @param testCaseSummary testSummary object for this test case
     */
    private static void startAssertNotNullsForServices(List<AssertNotNull> assertNotNull, Map.Entry<String, HttpResponse> response,
                                                       TestCaseSummary testCaseSummary) {
        log.info("Assert Not Null - assert property for services started");

        for (AssertNotNull assertItem : assertNotNull) {
            TestCaseAssertionSummary testAssertion = new TestCaseAssertionSummary();
            Assertor.AssertionPrerequisite.setAssertNotNullPrerequisite(assertItem);
            boolean isAssertNull = true;
            HttpResponse serviceResponse = response.getValue();
            String mediatedResult = Constants.STRING_NULL;

            switch (Assertor.AssertionPrerequisite.getExpressionPrefix()) {

                case INPUT_PROPERTY_BODY:
                    isAssertNull = serviceResponse.getEntity() == null;
                    break;

                case RESPONSE_PROPERTY_STATUS_CODE:
                    isAssertNull = serviceResponse.getStatusLine() == null;
                    break;

                case RESPONSE_PROPERTY_HTTP_VERSION:
                    if (serviceResponse.getStatusLine() != null) {
                        isAssertNull = serviceResponse.getStatusLine().getProtocolVersion() == null;
                    }
                    break;

                case INPUT_PROPERTY_TRANSPORT:
                    Header[] responseHeaders = serviceResponse.getAllHeaders();
                    if (responseHeaders != null) {
                        for (Header header : responseHeaders) {
                            if (header.getName().equals(Assertor.AssertionPrerequisite.getExpressionProperty())) {
                                isAssertNull = header.getValue() == null;
                                break;
                            }
                        }
                    }
                    break;

                default:
                    mediatedResult = "Received assert expression: "
                            + Assertor.AssertionPrerequisite.getAssertExpression()
                            + " is not a valid operation type for services";
                    log.error(mediatedResult);
            }

            log.info("Service Assert Actual - " + Assertor.AssertionPrerequisite.getAssertExpression());
            log.info("Service mediated result for assertNotNull is - " + !isAssertNull);

            if (!isAssertNull) {
                log.info("Service assertNotNull for " + Assertor.AssertionPrerequisite.getAssertExpression()
                        + " expression passed successfully");
            } else {
                testAssertion.setAssertionType(Constants.TEST_CASE_ASSERTION_NOTNULL);
                testAssertion.setAssertionExpression(Assertor.AssertionPrerequisite.getAssertExpression());
                testAssertion.setAssertionActualValue(mediatedResult);
                testAssertion.setAssertionDescription("Tested service url - " + response.getKey()
                        + "\nRecieved status code - " + (serviceResponse.getStatusLine() != null ?
                        serviceResponse.getStatusLine().getStatusCode() : "null"));
                testAssertion.setAssertionErrorMessage(Assertor.AssertionPrerequisite.getMessage());
                testCaseSummary.addTestCaseAssertion(testAssertion);
                log.error("Service assertNotNull for " + Assertor.AssertionPrerequisite.getAssertExpression()
                        + " expression failed with a message - " + Assertor.AssertionPrerequisite.getMessage());
            }
        }
    }

    /**
     * Inner static class for store the Assertion type common data.
     */
    static class AssertionPrerequisite {

        private static String assertExpression;
        private static String message;
        private static String expected;
        private static String expressionPrefix;
        private static String expressionProperty;

        private AssertionPrerequisite(){
        }

        static void setAssertEqualPrerequisite(AssertEqual assertItem) {
            assertExpression = assertItem.getActual();
            message = assertItem.getMessage();
            expected = RequestProcessor.trimStrings(assertItem.getExpected());
            String[] expressionType = assertExpression.split(Constants.STRING_COLON, 2);
            expressionPrefix = expressionType[0];
            if (expressionType.length == 2) {
                expressionProperty = expressionType[1];
            }
        }

        static void setAssertNotNullPrerequisite(AssertNotNull assertItem) {
            assertExpression = assertItem.getActual();
            message = assertItem.getMessage();
            String[] expressionType = assertExpression.split(Constants.STRING_COLON, 2);
            expressionPrefix = expressionType[0];
            if (expressionType.length == 2) {
                expressionProperty = expressionType[1];
            }
        }

        static String getAssertExpression() {
            return assertExpression;
        }

        static String getMessage() {
            return message;
        }

        static String getExpressionPrefix() {
            return expressionPrefix;
        }

        static String getExpressionProperty() {
            return expressionProperty;
        }

        static String getExpected() {
            return expected;
        }
    }
}
