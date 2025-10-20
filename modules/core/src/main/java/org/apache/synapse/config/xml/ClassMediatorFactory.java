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
import org.apache.commons.lang3.StringUtils;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.ext.ClassMediator;
import org.apache.synapse.mediators.v2.Utils;
import org.apache.synapse.mediators.v2.ext.AbstractClassMediator;
import org.apache.synapse.mediators.v2.ext.InputArgument;
import org.jaxen.JaxenException;

import javax.xml.namespace.QName;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Creates an instance of a Class mediator using XML configuration specified
 * <p/>
 * <pre>
 * &lt;class name=&quot;class-name&quot;&gt;
 *   &lt;property name=&quot;string&quot; value=&quot;literal&quot;&gt;
 *      either literal or XML child
 *   &lt;/property&gt;
 * &lt;/class&gt;
 * </pre>
 */
public class ClassMediatorFactory extends AbstractMediatorFactory {

    private static final QName CLASS_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "class");

    public Mediator createSpecificMediator(OMElement elem, Properties properties) {

        ClassMediator classMediator = new ClassMediator();

        OMAttribute name = elem.getAttribute(ATT_NAME);
        if (name == null) {
            String msg = "The name of the actual mediator class is a required attribute";
            log.error(msg);
            throw new SynapseException(msg);
        }

        Class clazz = null;
        Mediator mediator;

        if (properties != null) {  // load from synapse libs or dynamic class mediators

            ClassLoader libLoader =
                    (ClassLoader) properties.get(SynapseConstants.SYNAPSE_LIB_LOADER);

            if (libLoader != null) {         // load from synapse lib
                try {
                    clazz = libLoader.loadClass(name.getAttributeValue());
                } catch (ClassNotFoundException e) {
                    String msg = "Error loading class : " + name.getAttributeValue() +
                                 " from Synapse library";
                    log.error(msg, e);
                    throw new SynapseException(msg, e);
                }

            } else {                                  // load from dynamic class mediators
                Map<String, ClassLoader> dynamicClassMediatorLoaderMap =
                        (Map<String, ClassLoader>) properties.get(SynapseConstants.CLASS_MEDIATOR_LOADERS);
                if (dynamicClassMediatorLoaderMap != null) {
                    // Has registered dynamic class mediator loaders in the deployment store.
                    // Try to load class from them.
                    Iterator<ClassLoader> dynamicClassMediatorLoaders =
                            dynamicClassMediatorLoaderMap.values().iterator();

                    while (dynamicClassMediatorLoaders.hasNext()) {
                        try {
                            clazz = dynamicClassMediatorLoaders.next().loadClass(name.getAttributeValue());
                            break;
                        } catch (Exception ignore) {
                        }
                    }
                }
            }
        }

        if (clazz == null) {
            try {
                clazz = getClass().getClassLoader().loadClass(name.getAttributeValue());
            } catch (ClassNotFoundException e) {
                String msg = "Error loading class : " + name.getAttributeValue()
                             + " - Class not found";
                log.error(msg, e);
                throw new SynapseException(msg, e);
            }
        }

        try {
            mediator = (Mediator) clazz.newInstance();
            if (mediator instanceof AbstractMediator && FactoryUtils.isVersionedDeployment(properties)) {
                ((AbstractMediator) mediator).setArtifactIdentifier(properties.getProperty(SynapseConstants.SYNAPSE_ARTIFACT_IDENTIFIER));
            }
        } catch (Throwable e) {
            String msg = "Error in instantiating class : " + name.getAttributeValue();
            log.error(msg, e);
            throw new SynapseException(msg, e);
        }

        String targetAtt = elem.getAttributeValue(ATT_TARGET);
        if (StringUtils.isNotBlank(targetAtt)) {
            // This a V2 class mediator. Set the result target and input arguments
            if (Utils.isTargetBody(targetAtt)) {
                classMediator.setResultTarget(Utils.TARGET_BODY);
            } else if (Utils.isTargetVariable(targetAtt)) {
                String variableNameAttr = elem.getAttributeValue(ATT_TARGET_VARIABLE);
                if (StringUtils.isBlank(variableNameAttr)) {
                    String msg = "The '" + AbstractMediatorFactory.ATT_TARGET_VARIABLE + "' attribute is required for the configuration of a " +
                            "Class mediator when the '" + AbstractMediatorFactory.ATT_TARGET + "' is 'variable'";
                    throw new SynapseException(msg);
                }
                classMediator.setResultTarget(Utils.TARGET_VARIABLE);
                classMediator.setVariableName(variableNameAttr);
            } else if (Utils.isTargetNone(targetAtt)) {
                classMediator.setResultTarget(Utils.TARGET_NONE);
            } else {
                throw new SynapseException("Invalid '" + AbstractMediatorFactory.ATT_TARGET + "' attribute value for " +
                        "class mediator : " + targetAtt + ". It should be either 'body', 'variable' or 'none'");
            }
            String methodAtt = elem.getAttributeValue(new QName(XMLConfigConstants.NULL_NAMESPACE,
                    "method"));
            if (StringUtils.isBlank(methodAtt)) {
                methodAtt = "mediate";
            }
            classMediator.setMethodName(methodAtt);
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodAtt)) {
                    Map<String, AbstractClassMediator.Arg> arguments = new LinkedHashMap<>();
                    for (Parameter parameter : method.getParameters()) {
                        if (parameter.isAnnotationPresent(AbstractClassMediator.Arg.class)) {
                            AbstractClassMediator.Arg arg = parameter.getAnnotation(AbstractClassMediator.Arg.class);
                            arguments.put(arg.name(), arg);
                        }
                    }
                    classMediator.setArguments(new ArrayList<>(arguments.values()));
                    if (!arguments.isEmpty()) {
                        OMElement inputArgsElement = elem.getFirstChildWithName(INPUTS);
                        if (inputArgsElement != null) {
                            Map<String, InputArgument> inputArgsMap = getInputArguments(arguments, inputArgsElement,
                                    clazz.getName() + " class");
                            classMediator.setInputArguments(inputArgsMap);
                        } else {
                            String msg = "The 'inputs' element is required for the configuration of a " +
                                    "Class mediator.";
                            throw new SynapseException(msg);
                        }
                    }
                    break;
                }
            }
        } else {
            classMediator.addAllProperties(MediatorPropertyFactory.getMediatorProperties(elem, mediator));
        }

        // after successfully creating the mediator
        // set its common attributes such as tracing etc
        classMediator.setMediator(mediator);
        processAuditStatus(classMediator, elem);

        addAllCommentChildrenToList(elem, classMediator.getCommentsList());

        return classMediator;
    }

    public QName getTagQName() {
        return CLASS_Q;
    }


    /**
     * This method is used to get the input arguments of the V2 class mediator.
     *
     * @param inputArgsElement input arguments element
     * @return Map of input arguments
     */
    private Map<String, InputArgument> getInputArguments(Map<String, AbstractClassMediator.Arg> args,
                                                           OMElement inputArgsElement, String mediator) {

        Map<String, InputArgument> inputArgsMap = new LinkedHashMap<>();
        Iterator inputIterator = inputArgsElement.getChildrenWithName(ATT_ARGUMENT);
        while (inputIterator.hasNext()) {
            OMElement inputElement = (OMElement) inputIterator.next();
            String nameAttribute = inputElement.getAttributeValue(ATT_NAME);
            if (args.containsKey(nameAttribute)) {
                String type = args.get(nameAttribute).type().getTypeName();
                String valueAttribute = inputElement.getAttributeValue(ATT_VALUE);
                String expressionAttribute = inputElement.getAttributeValue(ATT_EXPRN);
                InputArgument argument = new InputArgument(nameAttribute);
                if (valueAttribute != null) {
                    argument.setValue(valueAttribute, type);
                } else if (expressionAttribute != null) {
                    try {
                        argument.setExpression(SynapsePathFactory.getSynapsePath(inputElement,
                                new QName("expression")), type);
                    } catch (JaxenException e) {
                        handleException("Error setting expression : " + expressionAttribute + " as an input argument to " +
                                mediator + " mediator. " + e.getMessage(), e);
                    }
                }
                inputArgsMap.put(nameAttribute, argument);
            }
        }
        if (inputArgsMap.size() != args.size()) {
            handleException("The input arguments provided in the configuration of the " + mediator +
                    " mediator does not match with the method signature. Please provide the correct input arguments.");
        }
        return inputArgsMap;
    }
}
