/*
*Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.apache.synapse.mediators.transform;

import junit.framework.TestCase;
import org.apache.synapse.MessageContext;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.util.xpath.SynapseXPath;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Unit test case for payloadFactory mediator
 */
public class PayloadFactoryMediatorTest extends TestCase {

    private static String format = "<p:addCustomer xmlns:p=\"http://ws"
            + ".wso2.org/dataservice\">\n"
            + " <xs:name xmlns:xs=\"http://ws.wso2.org/dataservice\">$1</xs:name>\n"
            + " <xs:request_time xmlns:xs=\"http://ws"
            + ".wso2.org/dataservice\">$2</xs:request_time>\n"
            + " <xs:tp_number xmlns:xs=\"http://ws"
            + ".wso2.org/dataservice\">$3</xs:tp_number>\n"
            + " <xs:address xmlns:xs=\"http://ws.wso2.org/dataservice\">$4</xs:address>\n"
            + " </p:addCustomer>";

    private static String inputPayload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
            + "   <soapenv:Header/>\n"
            + "   <soapenv:Body>\n"
            + "        <addCustomer>\n"
            + "            <name>Smith</name>\n"
            + "            <tpNumber>0834558649</tpNumber>\n"
            + "            <address>No. 456, Gregory Road, Los Angeles</address>\n"
            + "        </addCustomer>\n"
            + "   </soapenv:Body>\n"
            + "</soapenv:Envelope>  ";

    /**
     * Test payloadFactory Mediator with static arguments set
     * @throws Exception in case of argument evaluation issue
     */
    public void testWithStaticArguments() throws Exception {

        PayloadFactoryMediator payloadFactoryMediator = new PayloadFactoryMediator();
        payloadFactoryMediator.setFormat(format);

        //prepare arguments
        Argument argument1 = new Argument();
        argument1.setValue("John");
        Argument argument2 = new Argument();
        argument2.setValue("2017.09.26");
        Argument argument3 = new Argument();
        argument3.setValue("1234564632");
        Argument argument4 = new Argument();
        argument4.setValue("Colombo, Sri Lanka");

        //add arguments
        payloadFactoryMediator.addPathArgument(argument1);
        payloadFactoryMediator.addPathArgument(argument2);
        payloadFactoryMediator.addPathArgument(argument3);
        payloadFactoryMediator.addPathArgument(argument4);

        //do mediation
        MessageContext synCtx = TestUtils.getAxis2MessageContext(inputPayload, null);
        payloadFactoryMediator.mediate(synCtx);

        String expectedEnv = "<soapenv:Body xmlns:soapenv=\"http://schemas.xmlsoap"
                + ".org/soap/envelope/\"><p:addCustomer xmlns:p=\"http://ws.wso2.org/dataservice\">\n"
                + " <xs:name xmlns:xs=\"http://ws.wso2.org/dataservice\">John</xs:name>\n"
                + " <xs:request_time xmlns:xs=\"http://ws.wso2.org/dataservice\">2017.09.26</xs:request_time>\n"
                + " <xs:tp_number xmlns:xs=\"http://ws.wso2.org/dataservice\">1234564632</xs:tp_number>\n"
                + " <xs:address xmlns:xs=\"http://ws.wso2.org/dataservice\">Colombo, Sri Lanka</xs:address>\n"
                + " </p:addCustomer></soapenv:Body>";

        assertEquals("PayloadFactory mediator has not "
                + "set expected format", expectedEnv, synCtx.getEnvelope().getBody().toString());
    }

    /**
     * Test payloadFactory Mediator with dynamic expressions set
     * @throws Exception in case of argument evaluation issue
     */
    public void testWithExpressionsAsArguments() throws Exception {

        PayloadFactoryMediator payloadFactoryMediator = new PayloadFactoryMediator();
        payloadFactoryMediator.setFormat(format);
        //prepare arguments
        Argument argument1 = new Argument();
        argument1.setExpression(new SynapseXPath("//name"));
        Argument argument2 = new Argument();
        argument2.setExpression(new SynapseXPath("get-property('SYSTEM_DATE', 'yyyy.MM.dd')"));
        Argument argument3 = new Argument();
        argument3.setExpression(new SynapseXPath("//tpNumber"));
        Argument argument4 = new Argument();
        argument4.setExpression(new SynapseXPath("//address"));

        //add arguments
        payloadFactoryMediator.addPathArgument(argument1);
        payloadFactoryMediator.addPathArgument(argument2);
        payloadFactoryMediator.addPathArgument(argument3);
        payloadFactoryMediator.addPathArgument(argument4);

        //do mediation
        MessageContext synCtx = TestUtils.getAxis2MessageContext(inputPayload, null);
        payloadFactoryMediator.mediate(synCtx);

        String expectedEnvelope = "<soapenv:Body xmlns:soapenv=\"http://schemas.xmlsoap"
                + ".org/soap/envelope/\"><p:addCustomer xmlns:p=\"http://ws.wso2.org/dataservice\">\n"
                + " <xs:name xmlns:xs=\"http://ws.wso2.org/dataservice\">Smith</xs:name>\n"
                + " <xs:request_time xmlns:xs=\"http://ws.wso2.org/dataservice\">"
                + new SimpleDateFormat("yyyy.MM.dd").format(Calendar.getInstance().getTime())
                + "</xs:request_time>\n"
                + " <xs:tp_number xmlns:xs=\"http://ws.wso2.org/dataservice\">0834558649</xs:tp_number>\n"
                + " <xs:address xmlns:xs=\"http://ws.wso2.org/dataservice\">No. 456, Gregory Road, Los "
                + "Angeles</xs:address>\n"
                + " </p:addCustomer></soapenv:Body>";

        assertEquals("PayloadFactory mediator has not "
                + "set expected format", expectedEnvelope, synCtx.getEnvelope().getBody().toString());
    }
}