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
 *  KIND, either express or implied.  See // TreeMap used to keep given scripts order if needed the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.mediators.bsf;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.SynapseException;
import org.apache.synapse.config.SynapsePropertiesLoader;
import org.apache.synapse.config.xml.AbstractMediatorFactory;
import org.apache.synapse.config.xml.SynapsePathFactory;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.config.xml.ValueFactory;
import org.apache.synapse.mediators.v2.Utils;
import org.apache.synapse.mediators.v2.ext.InputArgument;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.*;

import static org.apache.synapse.mediators.bsf.ScriptMediatorConstants.DEFAULT_SCRIPT_ENGINE;
import static org.apache.synapse.mediators.bsf.ScriptMediatorConstants.GRAAL_JAVA_SCRIPT;
import static org.apache.synapse.mediators.bsf.ScriptMediatorConstants.JAVA_SCRIPT;
import static org.apache.synapse.mediators.bsf.ScriptMediatorConstants.RHINO_JAVA_SCRIPT;

/**
 * Creates an instance of a Script mediator for inline or external script mediation for BSF
 * scripting languages.
 * <p/>
 * * <pre>
 *    &lt;script [key=&quot;entry-key&quot;]
 *      [function=&quot;script-function-name&quot;] language="javascript|groovy|ruby"&gt
 *      (text | xml)?
 *      &lt;include key=&quot;entry-key&quot; /&gt;
 *    &lt;/script&gt;
 * </pre>
 * <p/>
 * The boolean response from the in-lined mediator is either the response from the evaluation of the
 * script statements or if that result is not a boolean then a response of true is assumed.
 * <p/>
 * The MessageContext passed in to the script mediator has additional methods over the Synapse
 * MessageContext to enable working with the XML in a way natural to the scripting language. For
 * example when using JavaScript get/setPayloadXML use E4X XML objects, when using Ruby they
 * use REXML documents.
 * <p/>
 * For external script mediation, that is when using key, function, language attributes,
 * &lt;include key&quot;entry-key&quot; /&gt; is used to include one or more additional script files.
 */
public class ScriptMediatorFactory extends AbstractMediatorFactory {

    private static final QName TAG_NAME
            = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "script");

    private static final QName INCLUDE_Q
            = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "include");

    public Mediator createSpecificMediator(OMElement elem, Properties properties) {

        ScriptMediator mediator;
        ClassLoader  classLoader = null;
        if (properties != null) {
            classLoader = (ClassLoader) properties.get(SynapseConstants.SYNAPSE_LIB_LOADER);
        }

        OMAttribute keyAtt = elem.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE,
                "key"));
        OMAttribute langAtt = elem.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE,
                "language"));
        OMAttribute functionAtt = elem.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE,
                "function"));

        if (langAtt == null) {
            throw new SynapseException("The 'language' attribute is required for" +
                    " a script mediator");
            // TODO: couldn't this be determined from the key in some scenarios?
        }
        if (keyAtt == null && functionAtt != null) {
            throw new SynapseException("Cannot use 'function' attribute without 'key' " +
                    "attribute for a script mediator");
        }

        Map<Value, Object> includeKeysMap = getIncludeKeysMap(elem);
        String language = langAtt.getAttributeValue();
        if (language.equals(JAVA_SCRIPT) &&
                (RHINO_JAVA_SCRIPT.equals(SynapsePropertiesLoader.getPropertyValue(
                        DEFAULT_SCRIPT_ENGINE, GRAAL_JAVA_SCRIPT)))) {
            language = RHINO_JAVA_SCRIPT;
        }
        if (keyAtt != null) {

            // ValueFactory for creating dynamic or static Key
            ValueFactory keyFac = new ValueFactory();
            // create dynamic or static key based on OMElement
            Value generatedKey = keyFac.createValue(XMLConfigConstants.KEY, elem);

            String functionName = (functionAtt == null ? null : functionAtt.getAttributeValue());
            mediator = new ScriptMediator(language, includeKeysMap, generatedKey, functionName, classLoader);
            String targetAtt = elem.getAttributeValue(ATT_TARGET);
            if (StringUtils.isNotBlank(targetAtt)) {
                // This is V2 script mediator
                if (StringUtils.isNotBlank(targetAtt)) {
                    if (Utils.isTargetBody(targetAtt)) {
                        mediator.setResultTarget(Utils.TARGET_BODY);
                    } else if (Utils.isTargetVariable(targetAtt)) {
                        String variableNameAttr = elem.getAttributeValue(ATT_TARGET_VARIABLE);
                        if (StringUtils.isBlank(variableNameAttr)) {
                            String msg = "The '" + AbstractMediatorFactory.ATT_TARGET_VARIABLE + "' attribute is required " +
                                    "for the configuration of a Script mediator when the '" +
                                    AbstractMediatorFactory.ATT_TARGET + "' is 'variable'";
                            throw new SynapseException(msg);
                        }
                        mediator.setResultTarget(Utils.TARGET_VARIABLE);
                        mediator.setVariableName(variableNameAttr);
                    } else if (Utils.isTargetNone(targetAtt)) {
                        mediator.setResultTarget(Utils.TARGET_NONE);
                    } else {
                        throw new SynapseException("Invalid '" + AbstractMediatorFactory.ATT_TARGET + "' attribute " +
                                "value for script mediator : " + targetAtt + ". It should be either 'body', 'variable' or 'none'");
                    }
                    OMElement inputArgsElement = elem.getFirstChildWithName(INPUTS);
                    if (inputArgsElement != null) {
                        Map<String, InputArgument> inputArgsMap = getInputArguments(inputArgsElement);
                        mediator.setInputArgumentMap(inputArgsMap);
                    }
                }
            }
        } else {
            mediator = new ScriptMediator(language, elem.getText(),classLoader);
        }

        processAuditStatus(mediator, elem);

        addAllCommentChildrenToList(elem, mediator.getCommentsList());

        return mediator;
    }

    private Map<String, InputArgument> getInputArguments(OMElement inputArgsElement) {

        Map<String, InputArgument> inputArgsMap = new LinkedHashMap<>();
        Iterator inputIterator = inputArgsElement.getChildrenWithName(ATT_ARGUMENT);
        while (inputIterator.hasNext()) {
            OMElement inputElement = (OMElement) inputIterator.next();
            String nameAttribute = inputElement.getAttributeValue(ATT_NAME);
            String typeAttribute = inputElement.getAttributeValue(ATT_TYPE);
            String valueAttribute = inputElement.getAttributeValue(ATT_VALUE);
            String expressionAttribute = inputElement.getAttributeValue(ATT_EXPRN);
            InputArgument argument = new InputArgument(nameAttribute);
            if (valueAttribute != null) {
                argument.setValue(valueAttribute, typeAttribute);
            } else if (expressionAttribute != null) {
                try {
                    argument.setExpression(SynapsePathFactory.getSynapsePath(inputElement,
                            new QName("expression")), typeAttribute);
                } catch (JaxenException e) {
                    handleException("Error setting expression : " + expressionAttribute + " as an input argument to " +
                            "script mediator. " + e.getMessage(), e);
                }
            }
            inputArgsMap.put(nameAttribute, argument);
        }
        return inputArgsMap;
    }

    private Map<Value, Object> getIncludeKeysMap(OMElement elem) {
        // get <include /> scripts
        // map key = registry entry key, value = script source
        // at this time map values are null, later loaded
        // from void ScriptMediator.prepareExternalScript(MessageContext synCtx)

        // TreeMap used to keep given scripts order if needed
        Map<Value, Object> includeKeysMap = new LinkedHashMap<Value, Object>();
        Iterator itr = elem.getChildrenWithName(INCLUDE_Q);
        while (itr.hasNext()) {
            OMElement includeElem = (OMElement) itr.next();
            OMAttribute key = includeElem.getAttribute(new QName(XMLConfigConstants.NULL_NAMESPACE,
                    "key"));
            // ValueFactory for creating dynamic or static Value
            ValueFactory keyFac = new ValueFactory();
            // create dynamic or static key based on OMElement
            Value generatedKey = keyFac.createValue(XMLConfigConstants.KEY, includeElem);

            if (key == null) {
                throw new SynapseException("Cannot use 'include' element without 'key'" +
                        " attribute for a script mediator");
            }

            includeKeysMap.put(generatedKey, null);
        }

        return includeKeysMap;
    }

    public QName getTagQName() {
        return TAG_NAME;
    }
}
