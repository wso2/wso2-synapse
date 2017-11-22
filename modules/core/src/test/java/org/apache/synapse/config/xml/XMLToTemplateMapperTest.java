/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.endpoints.Template;
import org.apache.synapse.mediators.template.TemplateMediator;
import org.junit.Assert;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;
import java.util.Properties;

/**
 * Unit tests for XMLToTemplateMapper class.
 */
public class XMLToTemplateMapperTest {

    private XMLToTemplateMapper mapper = new XMLToTemplateMapper();
    private Properties properties = new Properties();
    private final String NAME = "HelloWordLogger";

    /**
     * Test getObjectFromOMNode by parsing a XML with sequence tag.
     *
     * @throws XMLStreamException
     */
    @Test
    public void testGetObjectFromOMNodeSequence() throws XMLStreamException {
        String input = "<template xmlns=\"http://ws.apache.org/ns/synapse\" name=\"" + NAME + "\">\n" +
                "   <sequence>\n" +
                "      <log level=\"full\">\n" +
                "         <property xmlns:ns2=\"http://org.apache.synapse/xsd\" name=\"message\" " +
                "expression=\"$func:message\"></property>\n" +
                "      </log>\n" +
                "   </sequence>\n" +
                "</template>";
        OMElement element = AXIOMUtil.stringToOM(input);
        TemplateMediator templateMediator = (TemplateMediator) mapper.getObjectFromOMNode(element, properties);
        Assert.assertEquals("name should be parsed into the element", NAME, templateMediator.getName());
    }

    /**
     * Test getObjectFromOMNode by parsing a XML with endpoint tag.
     *
     * @throws XMLStreamException
     */
    @Test
    public void testGetObjectFromOMNodeEndpoint() throws XMLStreamException {
        String input = "<template xmlns=\"http://ws.apache.org/ns/synapse\" name=\"HelloWordLogger\">\n" +
                "<endpoint>\n" +
                "         <address uri=\"http://localhost:9000/services/SimpleStockQuoteService\"/>\n" +
                "      </endpoint>" +
                "</template>";
        OMElement element = AXIOMUtil.stringToOM(input);
        Template template = (Template) mapper.getObjectFromOMNode(element, properties);
        Assert.assertEquals("name should be parsed into the template", NAME, template.getName());
    }
}
