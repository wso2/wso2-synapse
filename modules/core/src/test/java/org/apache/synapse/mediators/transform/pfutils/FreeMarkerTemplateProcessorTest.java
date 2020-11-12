/*
 *Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.mediators.transform.pfutils;

import junit.framework.TestCase;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.TestUtils;
import org.apache.synapse.mediators.transform.Argument;
import org.apache.synapse.mediators.transform.PayloadFactoryMediator;
import org.apache.synapse.util.xpath.SynapseXPath;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for FreeMarker Template Processor
 */
public class FreeMarkerTemplateProcessorTest extends TestCase {

    private static final String template = "<p:addCustomer xmlns:p=\"http://ws.wso2.org/dataservice\">\n" +
            " <xs:name xmlns:xs=\"http://ws.wso2.org/dataservice\">${args.arg1}</xs:name>\n" +
            " <xs:request_time xmlns:xs=\"http://ws.wso2.org/dataservice\">${args.arg2}</xs:request_time>\n" +
            " <xs:tp_number xmlns:xs=\"http://ws.wso2.org/dataservice\">${args.arg3}</xs:tp_number>\n" +
            " <xs:address xmlns:xs=\"http://ws.wso2.org/dataservice\">${args.arg4}</xs:address>\n" +
            " </p:addCustomer>";

    private static final String inputPayload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap" +
            ".org/soap/envelope/\">\n"
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
     * Test FreeMarkerTemplateProcessor with static arguments set
     *
     * @throws Exception in case of argument evaluation issue
     */
    public void testWithStaticArgumentsFreeMarker() throws Exception {

        PayloadFactoryMediator payloadFactoryMediator = new PayloadFactoryMediator();
        TemplateProcessor templateProcessor = new FreeMarkerTemplateProcessor();
        payloadFactoryMediator.setFormat(template);
        templateProcessor.setFormat(template);
        templateProcessor.init();
        payloadFactoryMediator.setTemplateProcessor(templateProcessor);

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
        payloadFactoryMediator.getTemplateProcessor().addPathArgument(argument1);
        payloadFactoryMediator.getTemplateProcessor().addPathArgument(argument2);
        payloadFactoryMediator.getTemplateProcessor().addPathArgument(argument3);
        payloadFactoryMediator.getTemplateProcessor().addPathArgument(argument4);

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

        assertEquals("FreeMarker Template Processor has not "
                + "set expected format", expectedEnv, synCtx.getEnvelope().getBody().toString());
    }

    /**
     * Test FreeMarkerTemplateProcessor with dynamic expressions set
     *
     * @throws Exception in case of argument evaluation issue
     */
    public void testWithExpressionsAsArguments() throws Exception {

        PayloadFactoryMediator payloadFactoryMediator = new PayloadFactoryMediator();
        TemplateProcessor templateProcessor = new FreeMarkerTemplateProcessor();
        payloadFactoryMediator.setFormat(template);
        templateProcessor.setFormat(template);
        templateProcessor.init();
        payloadFactoryMediator.setTemplateProcessor(templateProcessor);

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
        payloadFactoryMediator.getTemplateProcessor().addPathArgument(argument1);
        payloadFactoryMediator.getTemplateProcessor().addPathArgument(argument2);
        payloadFactoryMediator.getTemplateProcessor().addPathArgument(argument3);
        payloadFactoryMediator.getTemplateProcessor().addPathArgument(argument4);

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

        assertEquals("FreeMarker Template Processor has not "
                + "set expected format", expectedEnvelope, synCtx.getEnvelope().getBody().toString());
    }

    /**
     * Test FreeMarkerTemplateProcessor with JSON payload
     */
    public void testWithJSONPayload() throws Exception {

        final String jsonInputPayload = "{\n" +
                "  \"hotelName\": \"Kingsbury\",\n" +
                "  \"hotelCode\": \"001\",\n" +
                "  \"rooms\": [\n" +
                "    {\n" +
                "      \"code\": \"LX\",\n" +
                "      \"available\": 23\n" +
                "    },\n" +
                "    {\n" +
                "      \"code\": \"SUITE\",\n" +
                "      \"available\": 3\n" +
                "    },\n" +
                "    {\n" +
                "      \"code\": \"SV\",\n" +
                "      \"available\": 10\n" +
                "    }\n" +
                "  ],\n" +
                "  \"agent\" : {\n" +
                "    \"name\": \"walkers tours\",\n" +
                "    \"id\": \"1567\"\n" +
                "  }\n" +
                "}";

        final String jsonToXmlTemplate = "<contract xmlns=\"\">\n" +
                "<agent-info>\n" +
                "    <id>agn${payload.agent.id}</id>\n" +
                "    <name>${payload.agent.name?capitalize}</name>\n" +
                "</agent-info>\n" +
                "<hotel-info>\n" +
                "    <id>htl${payload.hotelCode}</id>\n" +
                "    <name>${payload.hotelName?capitalize}</name>\n" +
                "    <#list payload.rooms as room>\n" +
                "         <room>\n" +
                "             <id>${room.code}</id>\n" +
                "             <number-of-available-rooms>${room.available}</number-of-available-rooms>\n" +
                "         </room>\n" +
                "    </#list>\n" +
                "</hotel-info>\n" +
                "</contract>";

        PayloadFactoryMediator payloadFactoryMediator = new PayloadFactoryMediator();
        TemplateProcessor templateProcessor = new FreeMarkerTemplateProcessor();
        payloadFactoryMediator.setFormat(jsonToXmlTemplate);
        templateProcessor.setFormat(jsonToXmlTemplate);
        templateProcessor.init();
        payloadFactoryMediator.setTemplateProcessor(templateProcessor);

        //do mediation
        MessageContext synCtx = TestUtils.getTestContextJson(jsonInputPayload, null);
        payloadFactoryMediator.mediate(synCtx);

        String expectedEnvelope =
                "<soapenv:Body xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><contract>\n" +
                        "<agent-info>\n" +
                        "    <id>agn1567</id>\n" +
                        "    <name>Walkers Tours</name>\n" +
                        "</agent-info>\n" +
                        "<hotel-info>\n" +
                        "    <id>htl001</id>\n" +
                        "    <name>Kingsbury</name>\n" +
                        "         <room>\n" +
                        "             <id>LX</id>\n" +
                        "             <number-of-available-rooms>23</number-of-available-rooms>\n" +
                        "         </room>\n" +
                        "         <room>\n" +
                        "             <id>SUITE</id>\n" +
                        "             <number-of-available-rooms>3</number-of-available-rooms>\n" +
                        "         </room>\n" +
                        "         <room>\n" +
                        "             <id>SV</id>\n" +
                        "             <number-of-available-rooms>10</number-of-available-rooms>\n" +
                        "         </room>\n" +
                        "</hotel-info>\n" +
                        "</contract></soapenv:Body>";

        assertEquals("FreeMarker Template Processor has not "
                + "set expected format", expectedEnvelope, synCtx.getEnvelope().getBody().toString());
    }
    
    /**
     * Test FreeMarkerTemplateProcessor with a JSON array payload
     */
    public void testWithJSONArrayPayload() throws Exception {

        final String jsonInputPayload = "[{\n" +
                "  \"id\": 1,\n" +
                "  \"first_name\": \"Veronika\",\n" +
                "  \"last_name\": \"Lacroux\"\n" +
                "}, {\n" +
                "  \"id\": 2,\n" +
                "  \"first_name\": \"Trescha\",\n" +
                "  \"last_name\": \"Campaigne\"\n" +
                "}, {\n" +
                "  \"id\": 3,\n" +
                "  \"first_name\": \"Mayor\",\n" +
                "  \"last_name\": \"Moscrop\"\n" +
                "}]";

        final String jsonToXmlTemplate = "<people>\n" +
                "<#list payload as person>\n" +
                "  <person>\n" +
                "    <index>${person.id}</index>\n" +
                "    <name>${person.first_name} ${person.last_name}</name>\n" +
                "  </person>\n" +
                "</#list>\n" +
                "</people>";

        PayloadFactoryMediator payloadFactoryMediator = new PayloadFactoryMediator();
        TemplateProcessor templateProcessor = new FreeMarkerTemplateProcessor();
        payloadFactoryMediator.setFormat(jsonToXmlTemplate);
        templateProcessor.setFormat(jsonToXmlTemplate);
        templateProcessor.init();
        payloadFactoryMediator.setTemplateProcessor(templateProcessor);

        //do mediation
        MessageContext synCtx = TestUtils.getTestContextJson(jsonInputPayload, null);
        payloadFactoryMediator.mediate(synCtx);

        String expectedEnvelope = "<soapenv:Body xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><people>\n" +
                "  <person>\n" +
                "    <index>1</index>\n" +
                "    <name>Veronika Lacroux</name>\n" +
                "  </person>\n" +
                "  <person>\n" +
                "    <index>2</index>\n" +
                "    <name>Trescha Campaigne</name>\n" +
                "  </person>\n" +
                "  <person>\n" +
                "    <index>3</index>\n" +
                "    <name>Mayor Moscrop</name>\n" +
                "  </person>\n" +
                "</people></soapenv:Body>";

        assertEquals("FreeMarker Template Processor has not "
                + "set expected format", expectedEnvelope, synCtx.getEnvelope().getBody().toString());
    }

    /**
     * Test FreeMarkerTemplateProcessor with XML payload
     */
    public void testWithXMLPayload() throws Exception {

        final String xmlInput = "<contract>\n" +
                "    <hotelName>Kingsbury</hotelName>\n" +
                "    <hotelCode>001</hotelCode>\n" +
                "    <agent>\n" +
                "        <name>walkers tours</name>\n" +
                "        <id>1567</id>\n" +
                "    </agent>\n" +
                "    <rooms>\n" +
                "        <room>\n" +
                "            <code>LX</code>\n" +
                "            <available>23</available>\n" +
                "        </room>\n" +
                "        <room>\n" +
                "            <code>SUITE</code>\n" +
                "            <available>33</available>\n" +
                "        </room>\n" +
                "        <room>\n" +
                "            <code>SV</code>\n" +
                "            <available>10</available>\n" +
                "        </room>\n" +
                "    </rooms>\n" +
                "</contract>";

        final String xmlToJsonTemplate = "{\n" +
                "  \"agentInfo\": {\n" +
                "    \"id\": \"agn${payload.contract.agent.id}\",\n" +
                "    \"name\": \"${payload.contract.agent.name?capitalize}\"\n" +
                "  },\n" +
                "  \"hotelInfo\": {\n" +
                "    \"id\": \"htl${payload.contract.hotelCode}\",\n" +
                "    \"name\": \"${payload.contract.hotelName?capitalize}\"\n" +
                "  },\n" +
                "  \"roomInfo\": [\n" +
                "    <#list payload.contract.rooms.room as room>\n" +
                "    {\n" +
                "      \"room-code\": \"${room.code}\",\n" +
                "      \"available-rooms\": ${room.available}\n" +
                "    }<#if room_has_next>,</#if>\n" +
                "    </#list>\n" +
                "  ]\n" +
                "}\n";

        PayloadFactoryMediator payloadFactoryMediator = new PayloadFactoryMediator();
        TemplateProcessor templateProcessor = new FreeMarkerTemplateProcessor();
        payloadFactoryMediator.setFormat(xmlToJsonTemplate);
        payloadFactoryMediator.setType("json");
        templateProcessor.setMediaType("json");
        templateProcessor.setFormat(xmlToJsonTemplate);
        templateProcessor.init();
        payloadFactoryMediator.setTemplateProcessor(templateProcessor);

        //do mediation
        MessageContext synCtx = TestUtils.getAxis2MessageContext(xmlInput, null);
        payloadFactoryMediator.mediate(synCtx);

        String expectedEnvelope =
                "<soapenv:Body xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><jsonObject><agentInfo><id>" +
                        "agn1567</id><name>Walkers Tours</name></agentInfo><hotelInfo><id>htl001</id><name>Kingsbury" +
                        "</name></hotelInfo><roomInfo><room-code>LX</room-code><available-rooms>23</available-rooms>" +
                        "</roomInfo><roomInfo><room-code>SUITE</room-code><available-rooms>33</available-rooms>" +
                        "</roomInfo><roomInfo><room-code>SV</room-code><available-rooms>10</available-rooms></roomInfo>" +
                        "</jsonObject></soapenv:Body>";

        assertEquals("FreeMarker Template Processor has not "
                + "set expected format", expectedEnvelope, synCtx.getEnvelope().getBody().toString());
    }

    /**
     * Test FreeMarkerTemplateProcessor with Text payload
     */
    public void testWithTextPayload() throws Exception {

        final String textInput = "<text xmlns=\"http://ws.apache.org/commons/ns/payload\">hello</text>";

        final String xmlToJsonTemplate = "{\n" +
                "  \"text\" : \"${payload}\"\n" +
                "}";
        PayloadFactoryMediator payloadFactoryMediator = new PayloadFactoryMediator();
        TemplateProcessor templateProcessor = new FreeMarkerTemplateProcessor();
        payloadFactoryMediator.setFormat(xmlToJsonTemplate);
        payloadFactoryMediator.setType("json");
        templateProcessor.setMediaType("json");
        templateProcessor.setFormat(xmlToJsonTemplate);
        templateProcessor.init();
        payloadFactoryMediator.setTemplateProcessor(templateProcessor);

        //do mediation
        MessageContext synCtx = TestUtils.getAxis2MessageContext(textInput, null);
        payloadFactoryMediator.mediate(synCtx);

        String expectedEnvelope =
                "<soapenv:Body xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><jsonObject><text>hello</text></jsonObject></soapenv:Body>";

        assertEquals("FreeMarker Template Processor has not "
                + "set expected format", expectedEnvelope, synCtx.getEnvelope().getBody().toString());
    }

    /**
     * Test FreeMarkerTemplateProcessor with ctx property injection
     *
     */
    public void testWithCtxPropertyInjection() throws Exception {

        final String propertyInjectionTemplate = "<p:addCustomer xmlns:p=\"http://ws.wso2.org/dataservice\">\n" +
                " <xs:name xmlns:xs=\"http://ws.wso2.org/dataservice\">${ctx.name}</xs:name>\n" +
                " <xs:request_time xmlns:xs=\"http://ws.wso2.org/dataservice\">${ctx" +
                ".request_time}</xs:request_time>\n" +
                " <xs:tp_number xmlns:xs=\"http://ws.wso2.org/dataservice\">${ctx.tp_number}</xs:tp_number>\n" +
                " <xs:address xmlns:xs=\"http://ws.wso2.org/dataservice\">${ctx.address}</xs:address>\n" +
                " </p:addCustomer>";

        PayloadFactoryMediator payloadFactoryMediator = new PayloadFactoryMediator();
        TemplateProcessor templateProcessor = new FreeMarkerTemplateProcessor();
        payloadFactoryMediator.setFormat(propertyInjectionTemplate);
        templateProcessor.setFormat(propertyInjectionTemplate);
        templateProcessor.init();
        payloadFactoryMediator.setTemplateProcessor(templateProcessor);

        //do mediation
        MessageContext synCtx = TestUtils.getAxis2MessageContext(inputPayload, null);
        
        synCtx.setProperty("name","Smith");
        synCtx.setProperty("request_time",2021);
        synCtx.setProperty("tp_number","0834558649");
        synCtx.setProperty("address","No. 456, Gregory Road, Los Angeles");
        
        payloadFactoryMediator.mediate(synCtx);

        String expectedEnvelope = "<soapenv:Body xmlns:soapenv=\"http://schemas.xmlsoap"
                + ".org/soap/envelope/\"><p:addCustomer xmlns:p=\"http://ws.wso2.org/dataservice\">\n"
                + " <xs:name xmlns:xs=\"http://ws.wso2.org/dataservice\">Smith</xs:name>\n"
                + " <xs:request_time xmlns:xs=\"http://ws.wso2.org/dataservice\">"
                + "2021" 
                + "</xs:request_time>\n"
                + " <xs:tp_number xmlns:xs=\"http://ws.wso2.org/dataservice\">0834558649</xs:tp_number>\n"
                + " <xs:address xmlns:xs=\"http://ws.wso2.org/dataservice\">No. 456, Gregory Road, Los "
                + "Angeles</xs:address>\n"
                + " </p:addCustomer></soapenv:Body>";

        assertEquals("FreeMarker Template Processor has not "
                + "set expected format", expectedEnvelope, synCtx.getEnvelope().getBody().toString());
    }
    
    
    /**
     * Test FreeMarkerTemplateProcessor with transport header property injection
     *
     */
    public void testWithTransportHeaderPropertyInjection() throws Exception {

        final String propertyInjectionTemplate = "<p:addCustomer xmlns:p=\"http://ws.wso2.org/dataservice\">\n" +
                " <xs:name xmlns:xs=\"http://ws.wso2.org/dataservice\">${trp.name}</xs:name>\n" +
                " <xs:request_time xmlns:xs=\"http://ws.wso2.org/dataservice\">${trp" +
                ".request_time}</xs:request_time>\n" +
                " <xs:tp_number xmlns:xs=\"http://ws.wso2.org/dataservice\">${trp.tp_number}</xs:tp_number>\n" +
                " <xs:address xmlns:xs=\"http://ws.wso2.org/dataservice\">${trp.address}</xs:address>\n" +
                " </p:addCustomer>";

        PayloadFactoryMediator payloadFactoryMediator = new PayloadFactoryMediator();
        TemplateProcessor templateProcessor = new FreeMarkerTemplateProcessor();
        payloadFactoryMediator.setFormat(propertyInjectionTemplate);
        templateProcessor.setFormat(propertyInjectionTemplate);
        templateProcessor.init();
        payloadFactoryMediator.setTemplateProcessor(templateProcessor);

        //do mediation
        MessageContext synCtx = TestUtils.getAxis2MessageContext(inputPayload, null);


        Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
        org.apache.axis2.context.MessageContext axis2MessageCtx =
                axis2smc.getAxis2MessageContext();
        axis2MessageCtx.setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, new HashMap<String
                , Object>());
        Object headers = axis2MessageCtx.getProperty(
                org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);

        if (headers instanceof Map) {
            Map headersMap = (Map) headers;
            headersMap.put("name","Smith");
            headersMap.put("request_time",2021);
            headersMap.put("tp_number","0834558649");
            headersMap.put("address","No. 456, Gregory Road, Los Angeles");
        }

        
        payloadFactoryMediator.mediate(synCtx);

        String expectedEnvelope = "<soapenv:Body xmlns:soapenv=\"http://schemas.xmlsoap"
                + ".org/soap/envelope/\"><p:addCustomer xmlns:p=\"http://ws.wso2.org/dataservice\">\n"
                + " <xs:name xmlns:xs=\"http://ws.wso2.org/dataservice\">Smith</xs:name>\n"
                + " <xs:request_time xmlns:xs=\"http://ws.wso2.org/dataservice\">"
                + "2021" 
                + "</xs:request_time>\n"
                + " <xs:tp_number xmlns:xs=\"http://ws.wso2.org/dataservice\">0834558649</xs:tp_number>\n"
                + " <xs:address xmlns:xs=\"http://ws.wso2.org/dataservice\">No. 456, Gregory Road, Los "
                + "Angeles</xs:address>\n"
                + " </p:addCustomer></soapenv:Body>";

        assertEquals("FreeMarker Template Processor has not "
                + "set expected format", expectedEnvelope, synCtx.getEnvelope().getBody().toString());
    }  
    
    /**
     * Test FreeMarkerTemplateProcessor with axis 2 property injection
     *
     */
    public void testWithAxis2PropertyInjection() throws Exception {

        final String propertyInjectionTemplate = "<p:addCustomer xmlns:p=\"http://ws.wso2.org/dataservice\">\n" +
                " <xs:name xmlns:xs=\"http://ws.wso2.org/dataservice\">${axis2.name}</xs:name>\n" +
                " <xs:request_time xmlns:xs=\"http://ws.wso2.org/dataservice\">${axis2" +
                ".request_time}</xs:request_time>\n" +
                " <xs:tp_number xmlns:xs=\"http://ws.wso2.org/dataservice\">${axis2.tp_number}</xs:tp_number>\n" +
                " <xs:address xmlns:xs=\"http://ws.wso2.org/dataservice\">${axis2.address}</xs:address>\n" +
                " </p:addCustomer>";

        PayloadFactoryMediator payloadFactoryMediator = new PayloadFactoryMediator();
        TemplateProcessor templateProcessor = new FreeMarkerTemplateProcessor();
        payloadFactoryMediator.setFormat(propertyInjectionTemplate);
        templateProcessor.setFormat(propertyInjectionTemplate);
        templateProcessor.init();
        payloadFactoryMediator.setTemplateProcessor(templateProcessor);

        //do mediation
        MessageContext synCtx = TestUtils.getAxis2MessageContext(inputPayload, null);


        Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
        org.apache.axis2.context.MessageContext axis2MessageCtx =
                axis2smc.getAxis2MessageContext();

            axis2MessageCtx.setProperty("name","Smith");
            axis2MessageCtx.setProperty("request_time",2021);
            axis2MessageCtx.setProperty("tp_number","0834558649");
            axis2MessageCtx.setProperty("address","No. 456, Gregory Road, Los Angeles");

        
        payloadFactoryMediator.mediate(synCtx);

        String expectedEnvelope = "<soapenv:Body xmlns:soapenv=\"http://schemas.xmlsoap"
                + ".org/soap/envelope/\"><p:addCustomer xmlns:p=\"http://ws.wso2.org/dataservice\">\n"
                + " <xs:name xmlns:xs=\"http://ws.wso2.org/dataservice\">Smith</xs:name>\n"
                + " <xs:request_time xmlns:xs=\"http://ws.wso2.org/dataservice\">"
                + "2021" 
                + "</xs:request_time>\n"
                + " <xs:tp_number xmlns:xs=\"http://ws.wso2.org/dataservice\">0834558649</xs:tp_number>\n"
                + " <xs:address xmlns:xs=\"http://ws.wso2.org/dataservice\">No. 456, Gregory Road, Los "
                + "Angeles</xs:address>\n"
                + " </p:addCustomer></soapenv:Body>";

        assertEquals("FreeMarker Template Processor has not "
                + "set expected format", expectedEnvelope, synCtx.getEnvelope().getBody().toString());
    }
}
