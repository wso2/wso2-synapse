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

import org.apache.log4j.Logger;
import org.apache.synapse.MessageContext;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Set;

import static org.apache.synapse.unittest.Constants.WHITESPACE_REGEX;

/**
 * Class responsible for the validation of testing with expected results.
 */
public class Assert {

    private static Logger logger = Logger.getLogger(TestingAgent.class.getName());

    /**
     * Assertion of results of sequence mediation and expected payload and properties
     * @param expectedPayload expected payload
     * @param expectedPropertyValues expected property values
     * @param mediateMsgCtxt message context used for the mediation
     * @param testCaseNumber asserting test case number
     * @return true if assertion is success otherwise false
     */
    public static boolean doAssertionSequence(String expectedPayload, String expectedPropertyValues,
                                              MessageContext mediateMsgCtxt, int testCaseNumber) {
        JSONObject expectedPropertyValueJSON = new JSONObject(expectedPropertyValues);

        boolean assertProperty = (trimStrings(expectedPropertyValueJSON.toString())
                .equals(trimStrings(generatePropertyValues(mediateMsgCtxt))));
        boolean assertPayload = (trimStrings(expectedPayload)
                .equals(trimStrings(mediateMsgCtxt.getEnvelope().getBody().getFirstElement().toString())));

        System.out.println(" ");
        System.out.println("---------------------Expected Payload---------------");
        System.out.println(trimStrings(expectedPayload));
        System.out.println("---------------------Mediated Payload---------------");
        System.out.println(trimStrings(mediateMsgCtxt.getEnvelope().getBody().getFirstElement().toString()));
        logger.info("Payload assertion - " + assertPayload);
        System.out.println(" ");

        System.out.println("---------------------Expected Property Values---------------");
        System.out.println(trimStrings(expectedPropertyValueJSON.toString()));
        System.out.println("---------------------Mediated Property Values---------------");
        System.out.println(trimStrings(generatePropertyValues(mediateMsgCtxt)));
        logger.info("Property assertion - " + assertProperty);
        System.out.println(" ");

        if (assertProperty && assertPayload) {
            logger.info("Unit testing passed for test case - " + testCaseNumber);
            return true;
        } else {
            logger.error("Unit testing failed for test case - " + testCaseNumber);
            return false;
        }

    }

    /**
     * Assertion of results of proxy/API invoke and expected payload
     * @param expectedPayload expected payload
     * @param invokedResult response from the procy service
     * @param testCaseNumber asserting test case number
     * @return true if assertion is success otherwise false
     */
    public static boolean doAssertionService(String expectedPayload, String invokedResult, int testCaseNumber) {

        boolean assertPayload = (trimStrings(expectedPayload).equals(trimStrings(invokedResult)));
        System.out.println("---------------------Expected Payload---------------");
        System.out.println(trimStrings(expectedPayload));
        System.out.println("---------------------Response Payload---------------");
        System.out.println(trimStrings(invokedResult));
        logger.info("Payload assertion - " + assertPayload);

        if (assertPayload) {
            logger.info("Unit testing passed for test case - " + testCaseNumber);
            return true;
        } else {
            logger.error("Unit testing failed for test case - " + testCaseNumber);
            return false;
        }
    }

    /**
     * Create a JSON object using the property key and values of message context
     * @param mediateMsgCtxt message context used for the mediation
     * @return String of created JSON object which include property set
     */
    private static String generatePropertyValues(MessageContext mediateMsgCtxt) {
        Set propertyKeys = mediateMsgCtxt.getPropertyKeySet();
        JSONObject properties = new JSONObject();

        for (Object key: propertyKeys) {
            properties.put(key.toString(), mediateMsgCtxt.getProperty(key.toString()));
        }

        return properties.toString();
    }

    /**
     * Remove irrelevant whitespaces from the input string
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
}
