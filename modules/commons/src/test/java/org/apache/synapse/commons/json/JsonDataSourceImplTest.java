/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.commons.json;

import junit.framework.TestCase;
import org.apache.axiom.om.OMDataSource;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

public class JsonDataSourceImplTest extends TestCase {
    public static final String expectedJSON = "{\n"
            + "    \"id\":\"0001\",\n"
            + "    \"ok\":true,\n"
            + "    \"amount\":5250,\n"
            + "    \"url\" : [\n"
            + "            \"http://org.wso2.json/32_32\"\n"
            + "    ]\n"
            + "}\n";

    public static final String expectedXML = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<jsonObject>"
            + "<id>0001</id>"
            + "<ok>true</ok>"
            + "<amount>5250</amount>"
            + "<?xml-multiple url?>"
            + "<url>http://org.wso2.json/32_32</url>"
            + "</jsonObject>";

    public void testSerializeJson() throws XMLStreamException, FileNotFoundException {
        OMDataSource jsonDataSource = createJsonDatasource();
        OutputStream outputStream = Util.newOutputStream();
        jsonDataSource.serialize(outputStream, null);
        assertEquals("Invalid serialization", expectedJSON, outputStream.toString());
    }

    public void testSerializeStringWriter() throws FileNotFoundException, XMLStreamException {
        OMDataSource jsonDataSource = createJsonDatasource();
        Writer stringWriter = new StringWriter();
        jsonDataSource.serialize(stringWriter, null);
        assertEquals("Invalid serialization", expectedJSON, stringWriter.toString());
    }

    public void testSerializeXMLWriter() throws XMLStreamException, FileNotFoundException {
        OMDataSource jsonDataSource = createJsonDatasource();
        OutputStream outputStream = Util.newOutputStream();
        XMLStreamWriter xmlWriter = XMLOutputFactory.newFactory().createXMLStreamWriter(outputStream);
        jsonDataSource.serialize(xmlWriter);
        assertEquals("Invalid serialization", expectedXML, outputStream.toString());
    }

    private OMDataSource createJsonDatasource() throws FileNotFoundException {
        InputStream inputStream = Util.getJson(0);
        return Util.newJsonDataSource(inputStream);
    }
}
