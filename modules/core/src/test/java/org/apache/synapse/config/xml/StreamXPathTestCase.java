/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.synapse.config.xml;

import junit.framework.TestCase;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.commons.io.IOUtils;
import org.apache.synapse.util.streaming_xpath.StreamingXPATH;
import org.apache.synapse.util.streaming_xpath.exception.StreamingXPATHException;

import javax.xml.stream.XMLStreamException;

/**
 * Tests stream xpath related operations
 */
public class StreamXPathTestCase extends TestCase {

    private static final String ELEMENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "\n" + "<bookstore>\n" + "\n"
            + "<book category=\"cooking\">\n" + "  <title lang=\"en\">Everyday Italian</title>\n"
            + "  <author>Giada De Laurentiis</author>\n" + "  <year>2005</year>\n" + "  <price>30.00</price>\n"
            + "</book>\n </bookstore>";

    private static final String XPATH1 = "/bookstore/book/title";
    private static final String XPATH2 = "/bookstore/book[1]/year";

    public void testStreamValidXpath() throws StreamingXPATHException, XMLStreamException {
        StreamingXPATH parser = new StreamingXPATH(XPATH1);
        String result = parser.getStringValue(AXIOMUtil.stringToOM(ELEMENT));
        assertNotNull(result, "No result of xpath is provided");
        assertTrue("Invalid result from xpath execution", result.contains("Everyday Italian"));
    }

    public void testValidXpathForSingleElement() throws StreamingXPATHException, XMLStreamException {
        StreamingXPATH parser = new StreamingXPATH(XPATH2);
        String result = parser.getStringValue(AXIOMUtil.stringToOM(ELEMENT));
        assertNotNull(result, "No result of xpath is provided");
        assertTrue("Invalid result from xpath execution", result.contains("2005"));
    }

    public void testStreamXpathAsInputStream() throws Exception {

        StreamingXPATH parser = new StreamingXPATH(XPATH1);
        String result = parser.getStringValue(IOUtils.toInputStream(ELEMENT, "UTF-8"));
        assertNotNull(result, "No result of xpath is provided");
        assertTrue("Invalid result from xpath execution", result.contains("Everyday Italian"));
    }

}
