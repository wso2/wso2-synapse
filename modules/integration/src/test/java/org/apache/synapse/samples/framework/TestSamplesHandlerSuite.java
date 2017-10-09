/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.samples.framework;

import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.samples.framework.tests.rest.Sample10002;
import org.apache.synapse.samples.framework.tests.rest.Sample10003;
import org.apache.synapse.samples.framework.tests.tasks.*;
import org.apache.synapse.samples.framework.tests.transport.Sample10101;
import org.apache.synapse.samples.framework.tests.transport.Sample250;
import org.apache.synapse.samples.framework.tests.advanced.*;
import org.apache.synapse.samples.framework.tests.endpoint.*;
import org.apache.synapse.samples.framework.tests.message.*;
import org.apache.synapse.samples.framework.tests.proxy.*;
import org.apache.synapse.samples.framework.tests.qos.Sample100;
import org.apache.synapse.samples.framework.tests.qos.Sample101;
import org.apache.synapse.samples.framework.tests.rest.Sample10001;
import org.apache.synapse.samples.framework.tests.rest.Sample800;
import org.apache.synapse.samples.framework.tests.transport.Sample251;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is executed by maven and handles which samples to run
 */
public class TestSamplesHandlerSuite extends TestSuite {

    private static final Log log = LogFactory.getLog(TestSamplesHandlerSuite.class);
    private static HashMap<String, Object> sampleClassRepo
            = new HashMap<String, Object>();

    public static TestSuite suite() {

        //Adding all samples available
        populateSamplesMap();

        ArrayList<Class> suiteClassesList = new ArrayList<Class>();
        TestSuite suite = new TestSuite();

        String inputSuiteName = System.getProperty("suite");
        String tests = System.getProperty("tests");
        String suiteName = "SamplesSuite";

        //preparing suites, if specified
        if (inputSuiteName != null) {
            if (inputSuiteName.equalsIgnoreCase("message")) {
                suiteName = "MessageMediationSamplesSuite";
                for (int i = 0; i <= 15; i++) {
                    Class testClass = (Class) sampleClassRepo.get(Integer.toString(i));
                    if (testClass != null) {
                        suiteClassesList.add(testClass);
                    }
                }
            }
            if (inputSuiteName.equalsIgnoreCase("endpoint")) {
                suiteName = "EndpointSamplesSuite";
                for (int i = 50; i <= 60; i++) {
                    Class testClass = (Class) sampleClassRepo.get(Integer.toString(i));
                    if (testClass != null) {
                        suiteClassesList.add(testClass);
                    }
                }
            }
            if (inputSuiteName.equalsIgnoreCase("qos")) {
                suiteName = "QoSSamplesSuite";
                for (int i = 100; i <= 110; i++) {
                    Class testClass = (Class) sampleClassRepo.get(Integer.toString(i));
                    if (testClass != null) {
                        suiteClassesList.add(testClass);
                    }
                }
            }
            if (inputSuiteName.equalsIgnoreCase("proxy")) {
                suiteName = "ProxySamplesSuite";
                for (int i = 150; i <= 170; i++) {
                    Class testClass = (Class) sampleClassRepo.get(Integer.toString(i));
                    if (testClass != null) {
                        suiteClassesList.add(testClass);
                    }
                }
            }
            if (inputSuiteName.equalsIgnoreCase("transport")) {
                suiteName = "TransportSamplesSuite";
                for (int i = 250; i <= 280; i++) {
                    Class testClass = (Class) sampleClassRepo.get(Integer.toString(i));
                    if (testClass != null) {
                        suiteClassesList.add(testClass);
                    }
                }
            }
            if (inputSuiteName.equalsIgnoreCase("tasks")) {
                suiteName = "TasksSamplesSuite";
                for (int i = 300; i <= 310; i++) {
                    Class testClass = (Class) sampleClassRepo.get(Integer.toString(i));
                    if (testClass != null) {
                        suiteClassesList.add(testClass);
                    }
                }
            }
            if (inputSuiteName.equalsIgnoreCase("advanced")) {
                suiteName = "AdvancedSamplesSuite";
                for (int i = 350; i <= 490; i++) {
                    Class testClass = (Class) sampleClassRepo.get(Integer.toString(i));
                    if (testClass != null) {
                        suiteClassesList.add(testClass);
                    }
                }
            }
            if (inputSuiteName.equalsIgnoreCase("eventing")) {
                suiteName = "EventingSamplesSuite";
                for (int i = 500; i <= 510; i++) {
                    Class testClass = (Class) sampleClassRepo.get(Integer.toString(i));
                    if (testClass != null) {
                        suiteClassesList.add(testClass);
                    }
                }
            }
        } else if (tests != null) {
            String[] testArray = tests.split(",");
            suiteName = "SelectedSamplesSuite";
            for (String testNum : testArray) {
                Class testClass = (Class) sampleClassRepo.get(testNum);
                if (testClass != null) {
                    suiteClassesList.add(testClass);
                }
            }
        } else {
            suiteName = "AllSamplesSuite";
            for (int i = 0; i <= 20000; i++) {
                Class testClass = (Class) sampleClassRepo.get(Integer.toString(i));
                if (testClass != null) {
                    suiteClassesList.add(testClass);
                }
            }
        }

        for (Class testClass : suiteClassesList) {
            suite.addTestSuite(testClass);
            log.info("Adding Sample:" + testClass);
        }
        suite.setName(suiteName);

        return suite;
    }

    private static void populateSamplesMap() {

        //Message Mediation
        sampleClassRepo.put("0", Sample0.class);
        sampleClassRepo.put("1", Sample1.class);
        sampleClassRepo.put("2", Sample2.class);
        sampleClassRepo.put("3", Sample3.class);
        sampleClassRepo.put("4", Sample4.class);
        sampleClassRepo.put("5", Sample5.class);
        sampleClassRepo.put("6", Sample6.class);
        sampleClassRepo.put("7", Sample7.class);
        sampleClassRepo.put("8", Sample8.class);
        sampleClassRepo.put("9", Sample9.class);
        sampleClassRepo.put("10", Sample10.class);
        sampleClassRepo.put("11", Sample11.class);
        sampleClassRepo.put("12", Sample12.class);
        sampleClassRepo.put("13", Sample13.class);
        sampleClassRepo.put("15", Sample15.class);
        sampleClassRepo.put("16", Sample16.class);

        //Endpoint
        sampleClassRepo.put("50", Sample50.class);
        sampleClassRepo.put("51", Sample51.class);
        sampleClassRepo.put("52", Sample52.class);
        sampleClassRepo.put("53", Sample53.class);
        sampleClassRepo.put("54", Sample54.class);
        sampleClassRepo.put("55", Sample55.class);
        sampleClassRepo.put("56", Sample56.class);
        //sampleClassRepo.put("57", Sample57.class);  // intermittently fail
        sampleClassRepo.put("58", Sample58.class);
        sampleClassRepo.put("59", Sample59.class);

        //QoS
        sampleClassRepo.put("100", Sample100.class);

        //Proxy Service
        sampleClassRepo.put("150", Sample150.class);
        sampleClassRepo.put("151", Sample151.class);
        sampleClassRepo.put("152", Sample152.class);
        //sampleClassRepo.put("153", Sample153.class); // unable to load the JKS files
        sampleClassRepo.put("154", Sample154.class);
        sampleClassRepo.put("155", Sample155.class);
        sampleClassRepo.put("156", Sample156.class);
        sampleClassRepo.put("157", Sample157.class);
        sampleClassRepo.put("160", Sample160.class);
        sampleClassRepo.put("161", Sample161.class);

        //Transport switching
        sampleClassRepo.put("250", Sample250.class);
        sampleClassRepo.put("251", Sample251.class);

        //Tasks
        sampleClassRepo.put("300", Sample300.class);
        sampleClassRepo.put("301", Sample301.class);
        sampleClassRepo.put("302", Sample302.class);

        //Advanced
        sampleClassRepo.put("350", Sample350.class);
        sampleClassRepo.put("351", Sample351.class);
        sampleClassRepo.put("352", Sample352.class);
        sampleClassRepo.put("353", Sample353.class);
        sampleClassRepo.put("354", Sample354.class);
        sampleClassRepo.put("360", Sample360.class);
        sampleClassRepo.put("361", Sample361.class);
        sampleClassRepo.put("362", Sample362.class);
        sampleClassRepo.put("370", Sample370.class);   // neethi 3.0.x and wso2throttle incompatibility
        sampleClassRepo.put("371", Sample371.class);
        sampleClassRepo.put("372", Sample372.class);
        sampleClassRepo.put("380", Sample380.class);
        sampleClassRepo.put("390", Sample390.class);
        sampleClassRepo.put("391", Sample391.class);
        sampleClassRepo.put("420", Sample420.class);
        sampleClassRepo.put("430", Sample430.class);
        sampleClassRepo.put("431", Sample431.class);
        sampleClassRepo.put("432", Sample432.class);
        sampleClassRepo.put("433", Sample433.class);
        sampleClassRepo.put("434", Sample434.class);
        sampleClassRepo.put("450", Sample450.class);
        sampleClassRepo.put("451", Sample451.class);
        sampleClassRepo.put("452", Sample452.class);
        sampleClassRepo.put("460", Sample460.class);

        sampleClassRepo.put("800", Sample800.class);
        sampleClassRepo.put("10001", Sample10001.class);
        sampleClassRepo.put("10002", Sample10002.class);
        sampleClassRepo.put("10003", Sample10003.class);

        sampleClassRepo.put("10101", Sample10101.class);
    }
}
