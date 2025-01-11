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

package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapsePropertiesLoader;
import org.apache.synapse.util.xpath.SynapseJsonPath;
import org.apache.synapse.util.xpath.SynapseExpression;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;

/**
 * 
 */
public class SynapsePathFactory {

    private static final Log log = LogFactory.getLog(SynapsePathFactory.class);

    public static org.apache.synapse.config.xml.SynapsePath getSynapsePath(OMElement elem, QName attribName)
        throws JaxenException {

        org.apache.synapse.config.xml.SynapsePath path = null;
        OMAttribute pathAttrib = elem.getAttribute(attribName);

        if (pathAttrib != null && pathAttrib.getAttributeValue() != null) {

            if(pathAttrib.getAttributeValue().startsWith("json-eval(")) {
                path = new SynapseJsonPath(pathAttrib.getAttributeValue().substring(10, pathAttrib.getAttributeValue().length() - 1));
            } else if (pathAttrib.getAttributeValue().startsWith(SynapseConstants.SYNAPSE_EXPRESSION_IDENTIFIER_START) &&
                    pathAttrib.getAttributeValue().endsWith(SynapseConstants.SYNAPSE_EXPRESSION_IDENTIFIER_END)) {
                path = new SynapseExpression(pathAttrib.getAttributeValue().substring(2, pathAttrib.getAttributeValue().length() - 1));
            } else {
                try {
                    path = new SynapseXPath(pathAttrib.getAttributeValue());
                } catch (org.jaxen.XPathSyntaxException ex) {
                    /* Try and see whether the expression can be compiled with XPath 2.0
                     * This will only be done if the failover DOM XPath 2.0 is enabled */
                    if (Boolean.parseBoolean(SynapsePropertiesLoader.loadSynapseProperties().
                            getProperty(SynapseConstants.FAIL_OVER_DOM_XPATH_PROCESSING))) {
                        if(log.isDebugEnabled()) {
                            log.debug(
                                    "Trying to compile the expression in XPath 2.0: " + pathAttrib.getAttributeValue());
                        }
                        path = new SynapseXPath(pathAttrib.getAttributeValue(), elem);
                    } else {
                        throw ex;
                    }
                }
            }

            OMElementUtils.addNameSpaces(path, elem, log);
            path.addNamespacesForFallbackProcessing(elem);

        } else {
            handleException("Couldn't find the XPath attribute with the QName : "
                + attribName.toString() + " in the element : " + elem.toString());
        }       

        return path;
    }

    public static org.apache.synapse.config.xml.SynapsePath getSynapsePathfromExpression(OMElement elem, String expression) throws JaxenException {
        org.apache.synapse.config.xml.SynapsePath path = null;
        if (expression != null) {
            if (expression.startsWith("json-eval(")) {
                path = new SynapseJsonPath(expression.substring(10, expression.length() - 1));
            } else {
                try {
                    path = new SynapseXPath(expression);
                } catch (org.jaxen.XPathSyntaxException ex) {
                    /* Try and see whether the expression can be compiled with XPath 2.0
                     * This will only be done if the failover DOM XPath 2.0 is enabled */
                    if (Boolean.parseBoolean(SynapsePropertiesLoader.loadSynapseProperties().
                            getProperty(SynapseConstants.FAIL_OVER_DOM_XPATH_PROCESSING))) {
                        if (log.isDebugEnabled()) {
                            log.debug("Trying to compile the expression in XPath 2.0: " + expression);
                        }
                        path = new SynapseXPath(expression, elem);
                    } else {
                        throw ex;
                    }
                }
            }

            OMElementUtils.addNameSpaces(path, elem, log);
            path.addNamespacesForFallbackProcessing(elem);

        } else {
            handleException("Couldn't find the XPath expression");
        }

        return path;
    }

    public static org.apache.synapse.config.xml.SynapsePath getSynapsePath(OMElement elem, String expression)
        throws JaxenException {

        if (expression == null) {
            handleException("XPath expression cannot be null");
        }


        SynapseXPath xpath = new SynapseXPath(expression);
        OMElementUtils.addNameSpaces(xpath, elem, log);

        return xpath;
    }

    private static void handleException(String message) {
        log.error(message);
        throw new SynapseException(message);
    }
}
