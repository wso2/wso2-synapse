/*
 Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.apache.synapse.unittest.Constants.INPUT_PROPERTY_AXIS2;
import static org.apache.synapse.unittest.Constants.INPUT_PROPERTY_BODY;
import static org.apache.synapse.unittest.Constants.INPUT_PROPERTY_CONTEXT;
import static org.apache.synapse.unittest.Constants.INPUT_PROPERTY_TRANSPORT;


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
     * @return true if assertion is success otherwise false
     */
    static Pair<Boolean, String> doAssertionSequence(TestCase currentTestCase,
                                                     MessageContext mediateMsgCtxt, int testCaseNumber) {

        boolean isSequenceAssertComplete = false;
        boolean isAssertEqualComplete = false;
        boolean isAssertNotNullComplete = false;
        String assertMessage = null;

        List<AssertEqual> assertEquals = currentTestCase.getAssertEquals();
        List<AssertNotNull> assertNotNulls = currentTestCase.getAssertNotNull();

        if (!assertEquals.isEmpty()) {
            Pair<Boolean, String> assertSequence = startAssertEqualsForSequence(assertEquals, mediateMsgCtxt);
            isAssertEqualComplete = assertSequence.getKey();
            assertMessage = assertSequence.getValue();
        }

        if (!assertNotNulls.isEmpty() && assertMessage == null) {
            Pair<Boolean, String> assertSequence = startAssertNotNullsForSequence(assertNotNulls, mediateMsgCtxt);
            isAssertNotNullComplete = assertSequence.getKey();
            assertMessage = assertSequence.getValue();
        }


        if ((isAssertEqualComplete && isAssertNotNullComplete) || (isAssertEqualComplete && assertNotNulls.isEmpty()) ||
                (isAssertNotNullComplete && assertEquals.isEmpty())) {
            isSequenceAssertComplete = true;
            log.info("Unit testing passed for test case - " + testCaseNumber);
        } else {
            log.error("Unit testing failed for test case - " + testCaseNumber);
        }

        return new Pair<>(isSequenceAssertComplete, assertMessage);
    }

    /**
     * Assertion of results of proxy/API invoke and expected payload.
     *
     * @param currentTestCase current testcase
     * @param response        response from the service
     * @param testCaseNumber  asserting test case number
     * @return true if assertion is success otherwise false
     */
    static Pair<Boolean, String> doAssertionService(TestCase currentTestCase,
                                                    HttpResponse response, int testCaseNumber) {

        boolean isServiceAssertComplete = false;
        boolean isAssertEqualComplete = false;
        boolean isAssertNotNullComplete = false;
        String assertMessage = null;

        List<AssertEqual> assertEquals = currentTestCase.getAssertEquals();
        List<AssertNotNull> assertNotNulls = currentTestCase.getAssertNotNull();

        try {
            String responseAsString = EntityUtils.toString(response.getEntity(), "UTF-8");
            Header[] responseHeaders = response.getAllHeaders();

            if (!assertEquals.isEmpty()) {
                Pair<Boolean, String> assertService
                        = startAssertEqualsForServices(assertEquals, responseAsString, responseHeaders);
                isAssertEqualComplete = assertService.getKey();
                assertMessage = assertService.getValue();
            }

            if (!assertNotNulls.isEmpty() && assertMessage == null) {
                Pair<Boolean, String> assertService
                        = startAssertNotNullsForServices(assertNotNulls, responseAsString, responseHeaders);
                isAssertNotNullComplete = assertService.getKey();
                assertMessage = assertService.getValue();
            }
        } catch (IOException e) {
            log.error("Error while reading response from the service HttpResponse", e);
        }

        if ((isAssertEqualComplete && isAssertNotNullComplete) || (isAssertEqualComplete && assertNotNulls.isEmpty()) ||
                (isAssertNotNullComplete && assertEquals.isEmpty())) {
            isServiceAssertComplete = true;
            log.info("Unit testing passed for test case - " + testCaseNumber);
        } else {
            log.error("Unit testing failed for test case - " + testCaseNumber);
        }

        return new Pair<>(isServiceAssertComplete, assertMessage);
    }


    /**
     * Method of assertionEquals for Sequence test cases.
     *
     * @param assertEquals   array of assertEquals
     * @param messageContext message context
     * @return Pair<Boolean, String> which has status of assertEquals and message if any error occurred
     */
    private static Pair<Boolean, String> startAssertEqualsForSequence(List<AssertEqual> assertEquals,
                                                                      MessageContext messageContext) {

        log.info("\n");
        log.info("---------------------Assert Equals---------------------\n");
        boolean isAssertEqualFailed = false;
        String messageOfAssertEqual = null;

        for (AssertEqual assertItem : assertEquals) {

            if (!isAssertEqualFailed) {

                String actual = assertItem.getActual();
                String expected = Trimmer.trimStrings(assertItem.getExpected());
                String message = assertItem.getMessage();
                boolean isAssert;
                String[] actualType = actual.split(":");
                String actualProperty = actualType[0];
                String mediatedResult;

                Axis2MessageContext axis2MessageContext = (Axis2MessageContext) messageContext;
                org.apache.axis2.context.MessageContext axis2MessageCtx =
                        axis2MessageContext.getAxis2MessageContext();

                switch (actualProperty) {

                    case INPUT_PROPERTY_BODY:
                        mediatedResult =
                                Trimmer.trimStrings(messageContext.getEnvelope().getBody().getFirstElement()
                                        .toString());
                        isAssert = expected.equals(mediatedResult);

                        break;

                    case INPUT_PROPERTY_CONTEXT:
                        mediatedResult = Trimmer.trimStrings(messageContext.getProperty(actualType[1]).toString());
                        isAssert = expected.equals(mediatedResult);

                        break;

                    case INPUT_PROPERTY_AXIS2:
                        mediatedResult = Trimmer.trimStrings(axis2MessageCtx.getProperty(actualType[1]).toString());
                        isAssert = expected.equals(mediatedResult);

                        break;

                    case INPUT_PROPERTY_TRANSPORT:
                        Object headers = axis2MessageContext.getProperty(
                                org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

                        @SuppressWarnings("unchecked")
                        Map<String, Object> headersMap = (Map) headers;

                        mediatedResult = Trimmer.trimStrings(headersMap.get(actualType[1]).toString());
                        isAssert = expected.equals(mediatedResult);

                        break;

                    default:
                        isAssert = false;
                        mediatedResult = message;
                        message = "Received assert actual value for sequences not defined";
                }

                log.info("Sequence Assert Actual - " + actual);
                log.info("Sequence Assert Expected - " + expected);
                log.info("Sequence mediated result for actual - " + mediatedResult);
                if (isAssert) {
                    log.info("Sequence assertEqual for " + actualProperty + " type passed successfully");
                } else {
                    isAssertEqualFailed = true;
                    messageOfAssertEqual = message;
                    log.error("Sequence assertEqual for " + actualProperty + " type failed - " + message + "\n");
                }
            }

        }

        log.info("AssertEquals assertion success - " + !isAssertEqualFailed);
        return new Pair<>(!isAssertEqualFailed, messageOfAssertEqual);
    }

    /**
     * Method of assertionNotNull for Sequence test cases.
     *
     * @param assertNotNull  array of assertNotNull
     * @param messageContext message context
     * @return Pair<Boolean, String> which has status of assertNotNull and message if any error occurred
     */
    private static Pair<Boolean, String> startAssertNotNullsForSequence(List<AssertNotNull> assertNotNull,
                                                                        MessageContext messageContext) {
        log.info("\n");
        log.info("---------------------Assert Not Null---------------------\n");
        boolean isAssertNotNullFailed = false;
        String messageOfAssertNotNull = null;

        for (AssertNotNull assertItem : assertNotNull) {

            if (!isAssertNotNullFailed) {

                String actual = assertItem.getActual();
                String message = assertItem.getMessage();
                boolean isAssertNull;
                String[] actualType = actual.split(":");
                String actualProperty = actualType[0];
                String mediatedResult;

                Axis2MessageContext axis2MessageContext = (Axis2MessageContext) messageContext;
                org.apache.axis2.context.MessageContext axis2MessageCtx =
                        axis2MessageContext.getAxis2MessageContext();

                switch (actualProperty) {

                    case INPUT_PROPERTY_BODY:
                        mediatedResult = Trimmer.trimStrings(
                                messageContext.getEnvelope().getBody().getFirstElement().toString());
                        isAssertNull = mediatedResult.isEmpty();

                        break;

                    case INPUT_PROPERTY_CONTEXT:
                        mediatedResult = Trimmer.trimStrings(messageContext.getProperty(actualType[1]).toString());
                        isAssertNull = mediatedResult.isEmpty();

                        break;

                    case INPUT_PROPERTY_AXIS2:
                        mediatedResult = Trimmer.trimStrings(axis2MessageCtx.getProperty(actualType[1]).toString());
                        isAssertNull = mediatedResult.isEmpty();

                        break;

                    case INPUT_PROPERTY_TRANSPORT:
                        Object headers = axis2MessageCtx.getProperty(
                                org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

                        @SuppressWarnings("unchecked")
                        Map<String, Object> headersMap = (Map) headers;

                        mediatedResult = Trimmer.trimStrings(headersMap.get(actualType[1]).toString());
                        isAssertNull = mediatedResult.isEmpty();

                        break;

                    default:
                        isAssertNull = true;
                        mediatedResult = message;
                        message = "Received assert actual value for sequences not defined";
                }

                log.info("Sequence Assert Actual - " + actual);
                log.info("Sequence mediated result for actual is not null - " + !mediatedResult.isEmpty());

                if (!isAssertNull) {
                    log.info("Sequence assertNotNull for " + actualProperty + " type passed successfully");
                } else {
                    isAssertNotNullFailed = true;
                    messageOfAssertNotNull = message;
                    log.error("Sequence assertNotNull for " + actualProperty + " type failed - " + message + "/n");
                }
            }

        }

        log.info("AssertNotNull assertion success - " + !isAssertNotNullFailed);
        return new Pair<>(!isAssertNotNullFailed, messageOfAssertNotNull);
    }

    /**
     * Method of assertionEquals for Service test cases.
     *
     * @param assertEquals array of assertEquals
     * @param response     service response body
     * @param headers      service response headers
     * @return Pair<Boolean, String> which has status of assertEquals and message if any error occurred
     */
    private static Pair<Boolean, String> startAssertEqualsForServices(
            List<AssertEqual> assertEquals, String response, Header[] headers) {

        log.info("\n");
        log.info("---------------------Assert Equals---------------------\n");
        boolean isAssertEqualFailed = false;
        String messageOfAssertEqual = null;

        for (AssertEqual assertItem : assertEquals) {

            if (isAssertEqualFailed) {
                break;
            }

            String actual = assertItem.getActual();
            String expected = Trimmer.trimStrings(assertItem.getExpected());
            String message = assertItem.getMessage();
            boolean isAssert = false;
            String[] actualType = actual.split(":");
            String actualProperty = actualType[0];
            String mediatedResult = "null";

            switch (actualProperty) {

                case INPUT_PROPERTY_BODY:
                    mediatedResult = Trimmer.trimStrings(response);
                    isAssert = expected.equals(mediatedResult);
                    break;

                case INPUT_PROPERTY_TRANSPORT:

                    for (Header header : headers) {
                        if (header.getName().equals(actualType[1])) {
                            mediatedResult = Trimmer.trimStrings(header.getValue());
                            isAssert = expected.equals(mediatedResult);
                            break;
                        }
                    }
                    break;

                default:
                    message = "Received assert actual value not defined";
                    mediatedResult = message;
            }

            log.info("Service Assert Actual - " + actual);
            log.info("Service Assert Expected - " + expected);
            log.info("Service mediated result for actual - " + mediatedResult);

            if (isAssert) {
                log.info("Service assertEqual for " + actualProperty + " passed successfully");
            } else {
                isAssertEqualFailed = true;
                messageOfAssertEqual = message;
                log.error("Service assertEqual for " + actualProperty + " failed - " + message + "\n");
            }
        }

        log.info("AssertEquals assertion success - " + !isAssertEqualFailed);
        return new Pair<>(!isAssertEqualFailed, messageOfAssertEqual);
    }

    /**
     * Method of assertNotNull for Service test cases.
     *
     * @param assertNotNull array of assertNotNull
     * @param response      service response body
     * @param headers       service response headers
     * @return Pair<Boolean, String> which has status of assertNotNull and message if any error occurred
     */
    private static Pair<Boolean, String> startAssertNotNullsForServices(
            List<AssertNotNull> assertNotNull, String response, Header[] headers) {

        log.info("\n");
        log.info("---------------------Assert Not Null---------------------\n");
        boolean isAssertNotNullFailed = false;
        String messageOfAssertNotNull = null;

        for (AssertNotNull assertItem : assertNotNull) {

            if (isAssertNotNullFailed) {
                break;
            }

            String actual = assertItem.getActual();
            String message = assertItem.getMessage();
            boolean isAssertNull = false;
            String[] actualType = actual.split(":");
            String actualProperty = actualType[0];
            String mediatedResult = "null";

            switch (actualProperty) {

                case INPUT_PROPERTY_BODY:
                    mediatedResult = response;
                    isAssertNull = mediatedResult.isEmpty();

                    break;

                case INPUT_PROPERTY_TRANSPORT:
                    for (Header header : headers) {
                        if (header.getName().equals(actualType[1])) {
                            mediatedResult = Trimmer.trimStrings(header.getValue());
                            isAssertNull = mediatedResult.isEmpty();
                            break;
                        }
                    }
                    break;

                default:
                    message = "Received assert actual value for service not defined";
                    mediatedResult = message;
            }

            log.info("Service Assert Actual - " + actual);
            log.info("Service mediated result for actual is not null- " + !mediatedResult.isEmpty());

            if (!isAssertNull) {
                log.info("Service assertNotNull for " + actualProperty + " passed successfully");
            } else {
                isAssertNotNullFailed = true;
                messageOfAssertNotNull = message;
                log.error("Service assertNotNull for " + actualProperty + " failed - " + message + "\n");
            }
        }

        log.info("AssertNotNull assertion success - " + !isAssertNotNullFailed);
        return new Pair<>(!isAssertNotNullFailed, messageOfAssertNotNull);
    }
}
