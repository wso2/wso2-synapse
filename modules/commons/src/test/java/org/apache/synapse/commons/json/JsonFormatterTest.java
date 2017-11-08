/**
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.commons.json;

import junit.framework.TestCase;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.AxisFault;
import org.apache.axis2.transport.MessageFormatter;
import org.apache.axis2.context.MessageContext;

import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

public class JsonFormatterTest extends TestCase {

    private static final String xmlInput = "<pizza>\n" +
                                           "    <name>Meat Sizzler</name>\n" +
                                           "    <price>500.0</price>\n" +
                                           "    <toppings>\n" +
                                           "        <topping>\n" +
                                           "            <id>9999</id>\n" +
                                           "            <name>Steak</name>\n" +
                                           "            <extraPrice>4.00</extraPrice>\n" +
                                           "            <category>NONVEG</category>\n" +
                                           "        </topping>\n" +
                                           "        <topping>\n" +
                                           "            <id>9998</id>\n" +
                                           "            <name>Sun Dried Tomato</name>\n" +
                                           "            <extraPrice>4.00</extraPrice>\n" +
                                           "            <category>VEGETARIAN</category>\n" +
                                           "        </topping>\n" +
                                           "        <topping>\n" +
                                           "            <id>9997</id>\n" +
                                           "            <name>Mixed Peppers</name>\n" +
                                           "            <extraPrice>3.00</extraPrice>\n" +
                                           "            <category>VEGETARIAN</category>\n" +
                                           "        </topping>\n" +
                                           "        <topping>\n" +
                                           "            <id>9996</id>\n" +
                                           "            <name>Cajun Chicken</name>\n" +
                                           "            <extraPrice>3.00</extraPrice>\n" +
                                           "            <category>NONVEG</category>\n" +
                                           "        </topping>\n" +
                                           "        <topping>\n" +
                                           "            <id>9995</id>\n" +
                                           "            <name>Chorizo Sausage</name>\n" +
                                           "            <extraPrice>4.00</extraPrice>\n" +
                                           "            <category>NONVEG</category>\n" +
                                           "        </topping>\n" +
                                           "    </toppings>\n" +
                                           "</pizza>";

    public static final String jsonOut = "{\"pizza\":{\"name\":\"Meat Sizzler\",\"price\":500.0,\"toppings\":{\"topping\":[{\"id\":9999,\"name\":\"Steak\",\"extraPrice\":4.00,\"category\":\"NONVEG\"},{\"id\":9998,\"name\":\"Sun Dried Tomato\",\"extraPrice\":4.00,\"category\":\"VEGETARIAN\"},{\"id\":9997,\"name\":\"Mixed Peppers\",\"extraPrice\":3.00,\"category\":\"VEGETARIAN\"},{\"id\":9996,\"name\":\"Cajun Chicken\",\"extraPrice\":3.00,\"category\":\"NONVEG\"},{\"id\":9995,\"name\":\"Chorizo Sausage\",\"extraPrice\":4.00,\"category\":\"NONVEG\"}]}}}";

    public static final String xmlInput_1 = "<pizza>\n" +
                                           "    <name>Meat Sizzler</name>\n" +
                                           "    <price>500.0</price>\n" +
                                           "    <toppings>\n" +
                                           "        <topping>\n" +
                                           "            <id>9999</id>\n" +
                                           "            <name>Steak</name>\n" +
                                           "            <extraPrice>4.00</extraPrice>\n" +
                                           "            <category>NONVEG</category>\n" +
                                           "        </topping>\n" +
                                           "    </toppings>\n" +
                                           "</pizza>";

    public static final String jsonOut_1 = "{\"pizza\":{\"name\":\"Meat Sizzler\",\"price\":500.0,\"toppings\":{\"topping\":{\"id\":9999,\"name\":\"Steak\",\"extraPrice\":4.00,\"category\":\"NONVEG\"}}}}";

    public static final String xmlInput_2 = "<pizza>\n" +
                                            "    <name>Meat Sizzler</name>\n" +
                                            "    <price>500.0</price>\n" +
                                            "    <toppings>\n" +
                                            "        <topping>\n" +
                                            "            <id>9999</id>\n" +
                                            "            <name>Steak</name>\n" +
                                            "            <extraPrice>4.00</extraPrice>\n" +
                                            "            <category>NONVEG</category>\n" +
                                            "        </topping>\n" +
                                            "        <topping/>\n" +
                                            "    </toppings>\n" +
                                            "</pizza>";

    public static final String jsonOut_2 = "{\"pizza\":{\"name\":\"Meat Sizzler\",\"price\":500.0,\"toppings\":{\"topping\":[{\"id\":9999,\"name\":\"Steak\",\"extraPrice\":4.00,\"category\":\"NONVEG\"},null]}}}";

    public static final String xmlInput_3 = "<pizza>\n" +
                                            "    <name>Meat Sizzler</name>\n" +
                                            "    <price>500.0</price>\n" +
                                            "    <toppings>\n" +
                                            "        <topping>\n" +
                                            "            <id>9999</id>\n" +
                                            "            <name>Steak</name>\n" +
                                            "            <extraPrice>4.00</extraPrice>\n" +
                                            "            <category>NONVEG</category>\n" +
                                            "        </topping>\n" +
                                            "        <topping></topping>\n" +
                                            "    </toppings>\n" +
                                            "</pizza>";

    public static final String xmlInput_4 = "<jsonObject>\n" +
                                            "    <pizza>\n" +
                                            "        <name>Meat Sizzler</name>\n" +
                                            "        <price>500.0</price>\n" +
                                            "        <toppings>\n" +
                                            "            <topping>\n" +
                                            "                <id>9999</id>\n" +
                                            "                <name>Steak</name>\n" +
                                            "                <extraPrice>4.00</extraPrice>\n" +
                                            "                <category>NONVEG</category>\n" +
                                            "            </topping>\n" +
                                            "        </toppings>\n" +
                                            "    </pizza>\n" +
                                            "</jsonObject>";

    public static final String xmlInput_5 = "<jsonArray>\n" +
                                            "    <jsonElement>\n" +
                                            "        <pizza>\n" +
                                            "            <name>Meat Sizzler</name>\n" +
                                            "            <price>500.0</price>\n" +
                                            "            <toppings>\n" +
                                            "                <topping>\n" +
                                            "                    <id>9999</id>\n" +
                                            "                    <name>Steak</name>\n" +
                                            "                    <extraPrice>4.00</extraPrice>\n" +
                                            "                    <category>NONVEG</category>\n" +
                                            "                </topping>\n" +
                                            "            </toppings>\n" +
                                            "        </pizza>\n" +
                                            "    </jsonElement>\n" +
                                            "</jsonArray>";

    public static final String xmlInput_6 = "<jsonArray>\n" +
                                               "    <jsonElement>\n" +
                                               "        <pizza>\n" +
                                               "            <name>Meat Sizzler</name>\n" +
                                               "            <price>500.0</price>\n" +
                                               "            <toppings>\n" +
                                               "                <topping>\n" +
                                               "                    <id>9999</id>\n" +
                                               "                    <name>Steak</name>\n" +
                                               "                    <extraPrice>4.00</extraPrice>\n" +
                                               "                    <category>NONVEG</category>\n" +
                                               "                </topping>\n" +
                                               "            </toppings>\n" +
                                               "        </pizza>\n" +
                                               "    </jsonElement>\n" +
                                               "    <jsonElement/>\n" +
                                               "</jsonArray>";

    public static final String jsonOut_6 = "[{\"pizza\":{\"name\":\"Meat Sizzler\",\"price\":500.0,\"toppings\":{\"topping\":{\"id\":9999,\"name\":\"Steak\",\"extraPrice\":4.00,\"category\":\"NONVEG\"}}}},null]";

    // XML nil="true" to Json
    public static final String xmlInput_7 = "<root>\n" +
                                            "    <abc nil=\"true\"></abc>\n" +
                                            "</root>";

    public static final String jsonOut_7 = "{\"root\":{\"abc\":{\"@nil\":\"true\"}}}";

    // XML attribute to Json
    public static final String xmlInput_8 = "<pizza name=\"Meat Sizzler\">\n" +
                                            "    <price>500.0</price>\n" +
                                            "</pizza>";

    public static final String jsonOut_8 = "{\"pizza\":{\"@name\":\"Meat Sizzler\",\"price\":500.0}}";

    // XML text node with attribute to Json
    public static final String xmlInput_9 = "<pizza-topping category=\"NONVEG\">Steak</pizza-topping>";

    public static final String jsonOut_9 = "{\"pizza-topping\":{\"@category\":\"NONVEG\",\"$\":\"Steak\"}}";

    // XML empty node to Json
    public static final String xmlInput_10 = "<pizza>" +
                                             "    <name>Meat Sizzler</name>\n" +
                                             "    <description></description>\n" +
                                             "</pizza>";

    public static final String jsonOut_10 = "{\"pizza\":{\"name\":\"Meat Sizzler\",\"description\":null}}";

    /**
     * NOTE: Under this test class, we cannot test the creation of JSON arrays by giving xml processing instructions to the
     * xml input string object. The reason is that when doing stringToOm and setting the SOAP child with addChild() method,
     * the addChild() method throws an exception saying that it does not support the xml PI node type.
     */

    public void testCase() {
        try {
            InputStream inputStream = Util.getJson(0);

            MessageFormatter formatter = Util.newJsonFormatter();
            MessageContext messageContext = Util.newMessageContext();
            JsonUtil.getNewJsonPayload(messageContext, inputStream, false, false);
            OutputStream out = Util.newOutputStream();
            formatter.writeTo(messageContext, null, out, false);
            assertTrue(JsonDataSourceTest.expectedJSON.equals(out.toString()));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            assertTrue(false);
        } catch (AxisFault axisFault) {
            axisFault.printStackTrace();
            assertTrue(false);
        }
    }

    public void testCase2() {
        try {
            InputStream inputStream = Util.getJson(0);
            runTest(xmlInput, jsonOut, inputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    public void testCase3() {
        runTest(xmlInput_1, jsonOut_1, null);
    }

    public void testCase4() {
        runTest(xmlInput_2, jsonOut_2, null);
    }

    public void testCase5() {
        runTest(xmlInput_3, jsonOut_2, null);
    }

    public void testCase6() {
        runTest(xmlInput_4, jsonOut_1, null);
    }

    public void testCase7() {
        runTest(xmlInput_5, jsonOut_1, null);
    }

    public void testCase8() {
        runTest(xmlInput_6, jsonOut_6, null);
    }

    public void testCase9() {
        runTest(xmlInput_7, jsonOut_7, null);
    }

    public void testCase10() {
        runTest(xmlInput_8, jsonOut_8, null);
    }

    public void testCase11() {
        runTest(xmlInput_9, jsonOut_9, null);
    }

    public void testCase12() {
        runTest(xmlInput_10, jsonOut_10, null);
    }

    public void testGetBytesXML() throws AxisFault, XMLStreamException {
        JsonFormatter formatter = new JsonFormatter();
        MessageContext messageContext = Util.newMessageContext("<name>WSO2</name>");
        byte[] bytes = formatter.getBytes(messageContext, null);
        assertEquals("Invalid content received", "{\"name\":\"WSO2\"}", new String(bytes));
    }

    public void testGetBytesJson() throws AxisFault {
        JsonFormatter formatter = new JsonFormatter();
        MessageContext messageContext = Util.newMessageContext();
        messageContext.setProperty("JSON_STRING", "{\"name\":\"WSO2\"}");
        byte[] bytes = formatter.getBytes(messageContext, null);
        assertEquals("Invalid content received", "{\"name\":\"WSO2\"}", new String(bytes));
    }

    public void testWriteTo() throws AxisFault, XMLStreamException {
        JsonFormatter formatter = new JsonFormatter();
        MessageContext messageContext = Util.newMessageContext("<jsonArray><jsonElement>10</jsonElement><jsonElement>20</jsonElement></jsonArray>");
        OutputStream out = new ByteArrayOutputStream();
        formatter.writeTo(messageContext, null, out, true);
        assertEquals("Formatter output is invalid", "[10,20]", out.toString());
    }

    private void runTest(String xmlInput, String jsonOut, InputStream inputStream) {
        try {
            MessageFormatter formatter = Util.newJsonFormatter();
            MessageContext messageContext = Util.newMessageContext(xmlInput);
            JsonUtil.getNewJsonPayload(messageContext, inputStream, false, false);
            OutputStream out = Util.newOutputStream();
            formatter.writeTo(messageContext, null, out, false);
            assertTrue(jsonOut.equals(out.toString()));
        } catch (AxisFault axisFault) {
            axisFault.printStackTrace();
            assertTrue(false);
        } catch (XMLStreamException e) {
            e.printStackTrace();
            assertTrue(false);
        } catch (Exception e) {
            System.out.println(e);
            assertTrue(false);
        }
    }
}
