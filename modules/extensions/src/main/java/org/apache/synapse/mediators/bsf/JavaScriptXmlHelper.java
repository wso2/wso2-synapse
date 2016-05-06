/*
*  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.apache.synapse.mediators.bsf;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.bsf.xml.DefaultXMLHelper;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.xml.XMLObject;

import javax.script.ScriptException;
import javax.xml.stream.XMLStreamException;

/**
 * This class will provide the operation to getPayloadXML and setPayload of ScriptMessageContext to convert
 * between XML and ScriptXML object
 * since there is an api change in rhino17,This class is provided instead of getting Helper class by
 * XMLHelper.getArgHelper(engine) in bsf
 */

public class JavaScriptXmlHelper extends DefaultXMLHelper {
    private final Scriptable scope;

    JavaScriptXmlHelper() {
        Context cx = Context.enter();
        try {
            this.scope = cx.initStandardObjects();
        } finally {
            Context.exit();
        }
    }

    /**
     * This method will convert the message payload in to xml
     *
     * @param scriptXML from java script Scriptable
     * @return XML content as OMElement wrapped in Scriptable object
     * @throws ScriptException when error
     */
    public OMElement toOMElement(Object scriptXML)
            throws ScriptException {
        if (scriptXML == null) {
            return null;
        }
        if (!(scriptXML instanceof XMLObject)) {
            return null;
        }
        // TODO: E4X Bug? Shouldn't need this copy, but without it the outer element gets lost???
        Scriptable jsXML = (Scriptable) ScriptableObject.callMethod((Scriptable) scriptXML, "copy", new Object[0]);

        OMElement omElement;

        try {
            omElement = AXIOMUtil.stringToOM((String) ScriptableObject.callMethod(jsXML, "toXMLString", new Object[0]));
        } catch (XMLStreamException e) {
            throw new ScriptException(e);
        }

        return omElement;
    }

    /**
     * This method will convert the message payload in to ScriptXML Object
     *
     * @param omElement
     * @return Scriptable object by adding the xml content
     * @throws ScriptException when error
     */
    public Object toScriptXML(OMElement omElement)
            throws ScriptException {
        if (omElement == null) {
            return null;
        }
        Context cx = Context.enter();
        try {
            XmlObject xml;
            try {
                xml = XmlObject.Factory.parse(omElement.getXMLStreamReader());
            } catch (XmlException e) {
                throw new ScriptException(e);
            }
            Object wrappedXML = cx.getWrapFactory().wrap(cx, this.scope, xml, XmlObject.class);
            Object obj = cx.newObject(this.scope, "XML", new Object[]{wrappedXML});
            return obj;
        } finally {
            Context.exit();
        }
    }
}
