/*
 *  Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
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
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;

import java.io.InputStream;
import javax.script.ScriptException;

/**
 * The methods a GraalJS script can call on its message context in addition to {@link ScriptMessageContext}.
 */
public interface GraalScriptMessageContext extends ScriptMessageContext {

    Object jsonSerializerCallMember(String key, String value);

    Document parseXml(String text) throws ScriptException;

    OMElement getParsedOMElement(InputStream stream);

    AXIOMXPath getXpathResult(String expression) throws JaxenException;
}
