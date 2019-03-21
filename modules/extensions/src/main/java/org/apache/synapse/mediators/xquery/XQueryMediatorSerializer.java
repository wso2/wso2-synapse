/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.mediators.xquery;

import net.sf.saxon.s9api.ItemType;
import net.sf.saxon.s9api.XdmNodeKind;
import org.apache.axiom.om.OMElement;
import org.apache.synapse.Mediator;
import org.apache.synapse.config.xml.AbstractMediatorSerializer;
import org.apache.synapse.config.xml.SynapseXPathSerializer;
import org.apache.synapse.config.xml.ValueSerializer;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.mediators.MediatorProperty;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.util.xpath.SourceXPathSupport;
import org.apache.synapse.util.xpath.SynapseXPath;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * Serialize the given XQuery mediator into a XML
 * <p/>
 * <pre>
 * &lt;xquery key="string" [target="xpath"]&gt;
 *   &lt;variable name="string" type="string" [key="string"] [expression="xpath"]
 *      [value="string"]/&gt;?
 * &lt;/xquery&gt;
 * </pre>
 */
public class XQueryMediatorSerializer extends AbstractMediatorSerializer {

    public OMElement serializeSpecificMediator(Mediator m) {

        if (!(m instanceof XQueryMediator)) {
            handleException("Invalid Mediator has passed to serializer");
        }
        XQueryMediator queryMediator = (XQueryMediator) m;

        OMElement xquery = fac.createOMElement("xquery", synNS);
        Value key = queryMediator.getQueryKey();
        if (key != null) {
            // Serialize Key using KeySerializer
            ValueSerializer keySerializer = new ValueSerializer();
            keySerializer.serializeValue(key, XMLConfigConstants.KEY, xquery);

        }

        saveTracingState(xquery, queryMediator);

        SynapseXPath targetXPath = queryMediator.getTarget();
        if (targetXPath != null &&
                !SourceXPathSupport.DEFAULT_XPATH.equals(targetXPath.toString())) {
            SynapseXPathSerializer.serializeXPath(targetXPath, xquery, "target");
        }

        List<MediatorProperty> pros = queryMediator.getProcessorProperties();
        if (pros != null && !pros.isEmpty()) {
            OMElement dataSource = fac.createOMElement("dataSource", synNS);
            serializeProperties(dataSource, pros);
            xquery.addChild(dataSource);
        }

        List list = queryMediator.getVariables();
        if (list != null && !list.isEmpty()) {
            for (Object o : list) {
                if (o instanceof MediatorBaseVariable) {
                    MediatorBaseVariable variable = (MediatorBaseVariable) o;
                    QName name = variable.getName();
                    Object value = variable.getValue();
                    if (name != null && value != null) {
                        OMElement baseElement = fac.createOMElement("variable", synNS);
                        baseElement.addAttribute(fac.createOMAttribute(
                                "name", nullNS, name.getLocalPart()));
                        baseElement.addAttribute(fac.createOMAttribute(
                                "value", nullNS, (String) value));
                        String type = null;
                        ItemType variableType = variable.getType();
                        XdmNodeKind nodeKind = variable.getNodeKind();

                        if (ItemType.INT == variableType) {
                            type = "INT";
                        } else if (ItemType.INTEGER == variableType) {
                            type = "INTEGER";
                        } else if (ItemType.BOOLEAN == variableType) {
                            type = "BOOLEAN";
                        } else if (ItemType.BYTE == variableType) {
                            type = "BYTE";
                        } else if (ItemType.DOUBLE == variableType) {
                            type = "DOUBLE";
                        } else if (ItemType.SHORT == variableType) {
                            type = "SHORT";
                        } else if (ItemType.LONG == variableType) {
                            type = "LONG";
                        } else if (ItemType.FLOAT == variableType) {
                            type = "FLOAT";
                        } else if (ItemType.STRING == variableType) {
                            type = "STRING";
                        } else if (XdmNodeKind.DOCUMENT == nodeKind) {
                            type = "DOCUMENT";
                        } else if (XdmNodeKind.ELEMENT == nodeKind) {
                            type = "ELEMENT";
                        } else {
                            handleException("Unknown Type " + variableType);
                        }
                        if (type != null) {
                            baseElement.addAttribute(fac.createOMAttribute(
                                    "type", nullNS, type));

                        }
                        xquery.addChild(baseElement);
                    }
                } else if (o instanceof MediatorCustomVariable) {
                    MediatorCustomVariable variable = (MediatorCustomVariable) o;
                    QName name = variable.getName();
                    if (name != null) {
                        OMElement customElement = fac.createOMElement("variable", synNS);
                        customElement.addAttribute(fac.createOMAttribute(
                                "name", nullNS, name.getLocalPart()));
                        String regkey = variable.getRegKey();
                        if (regkey != null) {
                            customElement.addAttribute(fac.createOMAttribute(
                                    "key", nullNS, regkey));
                        }
                        SynapseXPath expression = variable.getExpression();
                        if (expression != null &&
                                !SourceXPathSupport.DEFAULT_XPATH.equals(expression.toString())) {
                            SynapseXPathSerializer.serializeXPath(expression,
                                    customElement, "expression");
                        }
                        String type = null;
                        ItemType variableType = variable.getType();
                        XdmNodeKind nodeKind = variable.getNodeKind();

                        if (XdmNodeKind.DOCUMENT == nodeKind) {
                            type = "DOCUMENT";
                        } else if (XdmNodeKind.ELEMENT == nodeKind) {
                            type = "ELEMENT";
                        } else if (ItemType.INT == variableType) {
                            type = "INT";
                        } else if (ItemType.INTEGER == variableType) {
                            type = "INTEGER";
                        } else if (ItemType.BOOLEAN == variableType) {
                            type = "BOOLEAN";
                        } else if (ItemType.BYTE == variableType) {
                            type = "BYTE";
                        } else if (ItemType.DOUBLE == variableType) {
                            type = "DOUBLE";
                        } else if (ItemType.SHORT == variableType) {
                            type = "SHORT";
                        } else if (ItemType.LONG == variableType) {
                            type = "LONG";
                        } else if (ItemType.FLOAT == variableType) {
                            type = "FLOAT";
                        } else if (ItemType.STRING == variableType) {
                            type = "STRING";
                        } else {
                            handleException("Unknown Type " + variableType);
                        }
                        if (type != null) {
                            customElement.addAttribute(fac.createOMAttribute(
                                    "type", nullNS, type));

                        }
                        xquery.addChild(customElement);
                    }
                }
            }
        }

        serializeComments(xquery, queryMediator.getCommentsList());

        return xquery;
    }

    public String getMediatorClassName() {
        return XQueryMediator.class.getName();
    }


}
