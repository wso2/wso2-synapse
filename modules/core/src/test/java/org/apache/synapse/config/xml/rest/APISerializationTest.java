/*
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

package org.apache.synapse.config.xml.rest;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.synapse.config.xml.AbstractTestCase;
import org.apache.synapse.rest.API;

public class APISerializationTest extends AbstractTestCase {

    public void testAPISerialization1() throws Exception {
        String xml =
                "<api name=\"test\" context=\"/dictionary\" transports=\"https\" xmlns=\"http://ws.apache.org/ns/synapse\" statisticId=\"186104\">" +
                "<resource url-mapping=\"/admin/view\" inSequence=\"in\" outSequence=\"out\" statisticId=\"186104\"/>" +
                "</api>";
        OMElement om = AXIOMUtil.stringToOM(xml);
        API api = APIFactory.createAPI(om);
        OMElement out = APISerializer.serializeAPI(api);
        assertXMLEqual(xml, out.toString());
    }

    public void testAPISerialization2() throws Exception {
        String xml = "<api name=\"test\" context=\"/dictionary\" transports=\"https\" hostname=\"apache.org\" " +
                     "port=\"8243\"" +
                     " xmlns=\"http://ws.apache.org/ns/synapse\" statisticId=\"186104\">" +
                     "<resource url-mapping=\"/admin/view\" statisticId=\"186104\" inSequence=\"in\" outSequence=\"out\"/>" +
                     "</api>";
        OMElement om = AXIOMUtil.stringToOM(xml);
        API api = APIFactory.createAPI(om);
        OMElement out = APISerializer.serializeAPI(api);
        assertXMLEqual(xml, out.toString());
    }

    public void testAPISerialization3() throws Exception {
        String xml =
                "<api name=\"test\" context=\"/dictionary\" transports=\"https\" hostname=\"apache.org\" port=\"8243\"" +
                " xmlns=\"http://ws.apache.org/ns/synapse\" statisticId=\"186104\">" +
                "<resource url-mapping=\"/admin/view\" inSequence=\"in\" statisticId=\"186104\">" +
                "<outSequence statisticId=\"186104\">" +
                "<log statisticId=\"186104\"/>" +
                "<send statisticId=\"186104\"/>" +
                "</outSequence>" +
                "</resource>" +
                "</api>";
        OMElement om = AXIOMUtil.stringToOM(xml);
        API api = APIFactory.createAPI(om);
        OMElement out = APISerializer.serializeAPI(api);
        assertXMLEqual(xml, out.toString());
    }

    public void testAPISerialization4() throws Exception {
        String xml =
                "<api name=\"test\" context=\"/dictionary\" transports=\"https\" hostname=\"apache.org\" port=\"8243\"" +
                " xmlns=\"http://ws.apache.org/ns/synapse\" statisticId=\"186104\">" +
                "<resource url-mapping=\"/admin/view\" outSequence=\"out\" statisticId=\"186104\">" +
                "<inSequence statisticId=\"186104\">" +
                "<log statisticId=\"186104\"/>" +
                "<send statisticId=\"186104\"/>" +
                "</inSequence>" +
                "</resource>" +
                "</api>";
        OMElement om = AXIOMUtil.stringToOM(xml);
        API api = APIFactory.createAPI(om);
        OMElement out = APISerializer.serializeAPI(api);
        assertXMLEqual(xml, out.toString());
    }

    public void testAPISerialization5() throws Exception {
        String xml =
                "<api name=\"test\" context=\"/dictionary\" transports=\"https\" hostname=\"apache.org\" port=\"8243\"" +
                " xmlns=\"http://ws.apache.org/ns/synapse\" statisticId=\"186104\">" +
                "<resource url-mapping=\"/admin/view/*\" statisticId=\"186104\">" +
                "<inSequence statisticId=\"186104\">" +
                "<log statisticId=\"186104\"/>" +
                "<send statisticId=\"186104\"/>" +
                "</inSequence>" +
                "<outSequence statisticId=\"186104\">" +
                "<log statisticId=\"186104\"/>" +
                "<send statisticId=\"186104\"/>" +
                "</outSequence>" +
                "</resource>" +
                "<resource url-mapping=\"/admin/*\" statisticId=\"186104\">" +
                "<inSequence statisticId=\"186104\">" +
                "<log statisticId=\"186104\"/>" +
                "<send statisticId=\"186104\"/>" +
                "</inSequence>" +
                "<outSequence statisticId=\"186104\">" +
                "<log statisticId=\"186104\"/>" +
                "<send statisticId=\"186104\"/>" +
                "</outSequence>" +
                "</resource>" +
                "<resource uri-template=\"/{char}/{word}\" statisticId=\"186104\">" +
                "<inSequence statisticId=\"186104\">" +
                "<send statisticId=\"186104\"/>" +
                "</inSequence>" +
                "<faultSequence statisticId=\"186104\">" +
                "<log level=\"full\" statisticId=\"186104\"/>" +
                "</faultSequence>" +
                "</resource>" +
                "</api>";
        OMElement om = AXIOMUtil.stringToOM(xml);
        API api = APIFactory.createAPI(om);
        OMElement out = APISerializer.serializeAPI(api);
        assertXMLEqual(xml, out.toString());
    }
}
