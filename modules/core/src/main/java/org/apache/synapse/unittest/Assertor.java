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

    private static Logger logger = Logger.getLogger(Assertor.class.getName());

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
            logger.info("Unit testing passed for test case - " + testCaseNumber);
        } else {
            logger.error("Unit testing failed for test case - " + testCaseNumber);
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
            logger.error("Error while reading response from the service HttpResponse", e);
        }

        if ((isAssertEqualComplete && isAssertNotNullComplete) || (isAssertEqualComplete && assertNotNulls.isEmpty()) ||
                (isAssertNotNullComplete && assertEquals.isEmpty())) {
            isServiceAssertComplete = true;
            logger.info("Unit testing passed for test case - " + testCaseNumber);
        }  else {
            logger.error("Unit testing failed for test case - " + testCaseNumber);
        }

        return new Pair<>(isServiceAssertComplete, assertMessage);
    }


    /**
     * Method of assertionEquals for Sequence test cases.
     *
     * @param assertEquals array of assertEquals
     * @param msgCtxt      message context
     * @return Pair<Boolean, String> which has status of assertEquals and message if any error occurred
     */
    private static Pair<Boolean, String> startAssertEqualsForSequence(List<AssertEqual> assertEquals,
                                                                      MessageContext msgCtxt) {

        logger.info("\n");
        logger.info("---------------------Assert Equals---------------------\n");
        boolean isAssertEqualFailed = false;
        String messageOfAssertEqual = null;

        for (AssertEqual assertItem : assertEquals) {

            if (!isAssertEqualFailed) {

                String actual = assertItem.getActual();
                String expected = Trimmer.trimStrings(assertItem.getExpected());
                String message = assertItem.getMessage();
                boolean isAssert;
                String[] actualType = actual.split(":");
                String mediatedResult;

                switch (actualType[0]) {

                    case INPUT_PROPERTY_BODY:
                        mediatedResult =
                                Trimmer.trimStrings(msgCtxt.getEnvelope().getBody().getFirstElement().toString());
                        isAssert = expected.equals(mediatedResult);

                        break;

                    case INPUT_PROPERTY_CONTEXT:
                        mediatedResult = Trimmer.trimStrings(msgCtxt.getProperty(actualType[1]).toString());
                        isAssert = expected.equals(mediatedResult);

                        break;

                    case INPUT_PROPERTY_AXIS2:
                        Axis2MessageContext axis2smc = (Axis2MessageContext) msgCtxt;
                        org.apache.axis2.context.MessageContext axis2MessageCtx =
                                axis2smc.getAxis2MessageContext();

                        mediatedResult = Trimmer.trimStrings(axis2MessageCtx.getProperty(actualType[1]).toString());
                        isAssert = expected.equals(mediatedResult);

                        break;

                    case INPUT_PROPERTY_TRANSPORT:
                        Axis2MessageContext axis2smcTra = (Axis2MessageContext) msgCtxt;
                        org.apache.axis2.context.MessageContext axis2MessageCtxTransport =
                                axis2smcTra.getAxis2MessageContext();
                        Object headers = axis2MessageCtxTransport.getProperty(
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

                logger.info("Sequence Assert Actual - " + actual);
                logger.info("Sequence Assert Expected - " + expected);
                logger.info("Sequence mediated result for actual - " + mediatedResult);
                if (isAssert) {
                    logger.info("Sequence assertEqual for " + actualType[0] + " type passed successfully");
                } else {
                    isAssertEqualFailed = true;
                    messageOfAssertEqual = message;
                    logger.error("Sequence assertEqual for " + actualType[0] + " type failed - " + message + "\n");
                }
            }

        }

        logger.info("AssertEquals assertion success - " + !isAssertEqualFailed);
        return new Pair<>(!isAssertEqualFailed, messageOfAssertEqual);
    }

    /**
     * Method of assertionNotNull for Sequence test cases.
     *
     * @param assertNotNull array of assertNotNull
     * @param msgCtxt       message context
     * @return Pair<Boolean, String> which has status of assertNotNull and message if any error occurred
     */
    private static Pair<Boolean, String> startAssertNotNullsForSequence(List<AssertNotNull> assertNotNull,
                                                                        MessageContext msgCtxt) {
        logger.info("\n");
        logger.info("---------------------Assert Not Null---------------------\n");
        boolean isAssertNotNullFailed = false;
        String messageOfAssertNotNull = null;

        for (AssertNotNull assertItem : assertNotNull) {

            if (!isAssertNotNullFailed) {

                String actual = assertItem.getActual();
                String message = assertItem.getMessage();
                boolean isAssertNull;
                String[] actualType = actual.split(":");
                String mediatedResult;

                switch (actualType[0]) {

                    case INPUT_PROPERTY_BODY:
                        mediatedResult = Trimmer.trimStrings(
                                msgCtxt.getEnvelope().getBody().getFirstElement().toString());
                        isAssertNull = mediatedResult.isEmpty();

                        break;

                    case INPUT_PROPERTY_CONTEXT:
                        mediatedResult = Trimmer.trimStrings(msgCtxt.getProperty(actualType[1]).toString());
                        isAssertNull = mediatedResult.isEmpty();

                        break;

                    case INPUT_PROPERTY_AXIS2:
                        Axis2MessageContext axis2smc = (Axis2MessageContext) msgCtxt;
                        org.apache.axis2.context.MessageContext axis2MessageCtx =
                                axis2smc.getAxis2MessageContext();

                        mediatedResult = Trimmer.trimStrings(axis2MessageCtx.getProperty(actualType[1]).toString());
                        isAssertNull = mediatedResult.isEmpty();

                        break;

                    case INPUT_PROPERTY_TRANSPORT:
                        Axis2MessageContext axis2smcTra = (Axis2MessageContext) msgCtxt;
                        org.apache.axis2.context.MessageContext axis2MessageCtxTransport =
                                axis2smcTra.getAxis2MessageContext();
                        Object headers = axis2MessageCtxTransport.getProperty(
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

                logger.info("Sequence Assert Actual - " + actual);
                logger.info("Sequence mediated result for actual is not null - " + !mediatedResult.isEmpty());

                if (!isAssertNull) {
                    logger.info("Sequence assertNotNull for " + actualType[0] + " type passed successfully");
                } else {
                    isAssertNotNullFailed = true;
                    messageOfAssertNotNull = message;
                    logger.error("Sequence assertNotNull for " + actualType[0] + " type failed - " + message + "/n");
                }
            }

        }

        logger.info("AssertNotNull assertion success - " + !isAssertNotNullFailed);
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

        logger.info("\n");
        logger.info("---------------------Assert Equals---------------------\n");
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
            String mediatedResult = "null";

            switch (actualType[0]) {

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

            logger.info("Service Assert Actual - " + actual);
            logger.info("Service Assert Expected - " + expected);
            logger.info("Service mediated result for actual - " + mediatedResult);

            if (isAssert) {
                logger.info("Service assertEqual for " + actualType[0] + " passed successfully");
            } else {
                isAssertEqualFailed = true;
                messageOfAssertEqual = message;
                logger.error("Service assertEqual for " + actualType[0] + " failed - " + message + "\n");
            }
        }

        logger.info("AssertEquals assertion success - " + !isAssertEqualFailed);
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

        logger.info("\n");
        logger.info("---------------------Assert Not Null---------------------\n");
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
            String mediatedResult = "null";

            switch (actualType[0]) {

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

            logger.info("Service Assert Actual - " + actual);
            logger.info("Service mediated result for actual is not null- " + !mediatedResult.isEmpty());

            if (!isAssertNull) {
                logger.info("Service assertNotNull for " + actualType[0] + " passed successfully");
            } else {
                isAssertNotNullFailed = true;
                messageOfAssertNotNull = message;
                logger.error("Service assertNotNull for " + actualType[0] + " failed - " + message + "\n");
            }
        }

        logger.info("AssertNotNull assertion success - " + !isAssertNotNullFailed);
        return new Pair<>(!isAssertNotNullFailed, messageOfAssertNotNull);
    }
}
