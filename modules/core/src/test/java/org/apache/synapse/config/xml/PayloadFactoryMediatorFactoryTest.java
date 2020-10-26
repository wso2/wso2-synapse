/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.Mediator;
import org.apache.synapse.config.xml.PayloadFactoryMediatorFactory;
import org.junit.Test;
import java.util.Properties;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test the method PayloadFactoryMediatorFactory.
 */
public class PayloadFactoryMediatorFactoryTest {

    private OMElement inputElement;
    private Properties properties = null;
    private PayloadFactoryMediatorFactory payloadFactoryMediatorFactory;
    private Mediator payloadFactoryMediator;
    private String inputXML;
    private String mediaType;

    @Test
    /**
     * Test the media type xml/xpath expression/iterating arguments.
     */
    public void testXMLMediaType() throws Exception {

        inputXML = " <payloadFactory xmlns=\"http://ws.apache.org/ns/synapse\" media-type=\"xml\">\n" + " "
                + "               <format>\n"
                + "                    <m:CheckPriceResponse xmlns:m=\"http://services.samples/xsd\">\n"
                + "                        <m:Code>$1</m:Code>\n" + "                        <m:Price>$2</m:Price>\n"
                + "                    </m:CheckPriceResponse>\n" + "                </format>\n"
                + "                <args>\n"
                + "                    <arg xmlns:m0=\"http://services.samples/xsd\" expression=\"//m0:symbol\"/>\n"
                + "                    <arg xmlns:m0=\"http://services.samples/xsd\" expression=\"//m0:last\"/>\n"
                + "                </args>\n" + "            </payloadFactory>";

        inputElement = AXIOMUtil.stringToOM(inputXML);
        payloadFactoryMediatorFactory = new PayloadFactoryMediatorFactory();
        payloadFactoryMediator = payloadFactoryMediatorFactory.createSpecificMediator(inputElement, properties);
        assertNotNull(payloadFactoryMediator);
        mediaType = payloadFactoryMediator.getType();
        assertEquals("Media type is not xml", "xml", mediaType);
    }

    @Test
    /**
     * Test the media type json and json expression.
     */
    public void testJsonMediaType() throws Exception {

        inputXML = "<payloadFactory xmlns=\"http://ws.apache.org/ns/synapse\" media-type=\"json\">\n"
                + "            <format>\n" + "                {\n" + "    \"coordinates\": null,\n"
                + "    \"created_at\": \"Fri Jun 24 17:43:26 +0000 2011\",\n" + "    \"truncated\": false,\n"
                + "    \"favorited\": false,\n" + "    \"id_str\": \"$1\",\n" + "   }\n" + "            </format>\n"
                + "            <args>\n" + "               <arg expression=\"$.user.id\" evaluator=\"json\"/>\n"
                + "            </args>\n" + "</payloadFactory>";

        inputElement = AXIOMUtil.stringToOM(inputXML);
        payloadFactoryMediatorFactory = new PayloadFactoryMediatorFactory();
        payloadFactoryMediator = payloadFactoryMediatorFactory.createSpecificMediator(inputElement, properties);
        assertNotNull(payloadFactoryMediator);
        mediaType = payloadFactoryMediator.getType();
        assertEquals("Media type is not json", "json", mediaType);
    }

    @Test
    /**
     * Test when the media type is not set. It should take the default value which is xml.
     */
    public void testNoMediaType() throws Exception {

        inputXML = " <payloadFactory xmlns=\"http://ws.apache.org/ns/synapse\">\n" + "                <format>\n"
                + "                    <m:CheckPriceResponse xmlns:m=\"http://services.samples/xsd\">\n"
                + "                        <m:Code>$1</m:Code>\n" + "                    </m:CheckPriceResponse>\n"
                + "                </format>\n" + "                <args>\n"
                + "                    <arg xmlns:m0=\"http://services.samples/xsd\" expression=\"//m0:symbol\"/>\n"
                + "                </args>\n" + "            </payloadFactory>";

        inputElement = AXIOMUtil.stringToOM(inputXML);
        payloadFactoryMediatorFactory = new PayloadFactoryMediatorFactory();
        payloadFactoryMediator = payloadFactoryMediatorFactory.createSpecificMediator(inputElement, properties);
        assertNotNull(payloadFactoryMediator);
        mediaType = payloadFactoryMediator.getType();
        assertEquals("Media type is not xml", "xml", mediaType);
    }
}
