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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

public class Assert {

    private static Logger logger = Logger.getLogger(TestingAgent.class.getName());

    public static boolean doAssertionSequence(String expectedPayload, String expectedPropertyValues, MessageContext mediateMsgCtxt, int testCaseNumber) {

        boolean assertProperty = (trimStrings(expectedPropertyValues).equals(trimStrings(mediateMsgCtxt.getEnvelope().toString())));
        boolean assertPayload = (trimStrings(expectedPayload).equals(trimStrings(mediateMsgCtxt.getEnvelope().getBody().getFirstElement().toString())));

        System.out.println("");
        System.out.println("---------------------Expected Payload---------------");
        System.out.println(trimStrings(expectedPayload));
        System.out.println("---------------------Mediated Payload---------------");
        System.out.println(trimStrings(mediateMsgCtxt.getEnvelope().getBody().getFirstElement().toString()));
        logger.info("Payload assertion - "+assertPayload);
        System.out.println(" ");

        System.out.println("---------------------Expected Property Values---------------");
        System.out.println(trimStrings(expectedPropertyValues));
        System.out.println("---------------------Mediated Property Values---------------");
        System.out.println(trimStrings(mediateMsgCtxt.getEnvelope().toString()));
        logger.info("Property assertion - "+assertProperty);
        System.out.println(" ");

        if (assertProperty && assertPayload) {
            logger.info("Unit testing passed for test case - "+testCaseNumber);
            return true;
        } else {
            logger.error("Unit testing failed for test case - "+testCaseNumber);
            return false;
        }

    }

    public static boolean doAssertionService(String expectedPayload, String invokedResult, int testCaseNumber) {

        boolean assertPayload = (trimStrings(expectedPayload).equals(trimStrings(invokedResult)));
        System.out.println("---------------------Expected Payload---------------");
        System.out.println(trimStrings(expectedPayload));
        System.out.println("---------------------Response Payload---------------");
        System.out.println(trimStrings(invokedResult));
        logger.info("Payload assertion - "+assertPayload);

        if (assertPayload) {
            logger.info("Unit testing passed for test case - "+testCaseNumber);
            return true;
        } else {
            logger.error("Unit testing failed for test case - "+testCaseNumber);
            return false;
        }
    }

    public static String trimStrings(String input) {
        BufferedReader reader = new BufferedReader(new StringReader(input));
        StringBuffer result = new StringBuffer();
        try {
            String line;
            while ( (line = reader.readLine() ) != null)
                result.append(line.trim());
            return result.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
