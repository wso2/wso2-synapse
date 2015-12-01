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

import net.sf.saxon.s9api.*;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axiom.om.impl.dom.DOOMAbstractFactory;
import org.apache.axiom.om.util.ElementHelper;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.config.Entry;
import org.apache.synapse.config.SynapseConfigUtils;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.MediatorProperty;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.util.xpath.SourceXPathSupport;
import org.apache.synapse.util.xpath.SynapseXPath;
import org.w3c.dom.Element;

import javax.activation.DataHandler;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.*;


/**
 * The XQueryMediator  provides the means to extract and manipulate data from XML documents  using
 * XQuery . It is possible to query against the current  SOAP Message or external XML. To query
 * against the current  SOAP Message ,it is need to define custom variable with any name and type as
 * element,document,document_element By providing a expression ,It is possible to select a custom
 * node for querying.The all the variable  that  have defined  in the mediator will be available
 * during the query process .Basic variable can use bind basic type.
 * currently only support * string,int,byte,short,double,long,float and boolean * types.
 * Custom Variable can use to bind XML documents ,SOAP payload and any basic type which create
 * through the XPath expression .
 */

public class XQueryMediator extends AbstractMediator {

    /* Properties that must set to the Processor  */
    private final List<MediatorProperty> processorProperties = new ArrayList<MediatorProperty>();

    /* The key for lookup the xquery (Supports both static and dynamic keys)*/
    private Value queryKey;

    /* The source of the xquery */
    private String querySource;

    /*The target node*/
    private final SourceXPathSupport target = new SourceXPathSupport();

    /* The list of variables for binding to the DyanamicContext in order to available for querying */
    private final List<MediatorVariable> variables = new ArrayList<MediatorVariable>();

    /*Lock used to ensure thread-safe lookup of the object from the registry */
    private final Object resourceLock = new Object();

    /* Is it need to use DOMSource and DOMResult? */
    private boolean useDOMSource = false;

    /*The Processor allows global Saxon configuration options to be set &  it acts as a factory for generating XQuery compiler */
    private Processor cachedProcessor = null;

    /* XQueryCompiler allows to compile XQuery 1.0 queries */
    private XQueryCompiler cachedQueryCompiler = null;

    /* An XQueryEvaluator that use for represents a compiled and loaded query ready for execution. XQueryEvaluator will recreate if query has changed */
    private Map<String, XQueryEvaluator> cachedXQueryEvaluatorMap = new Hashtable<String, XQueryEvaluator>();

    public XQueryMediator() {
    }

    /**
     * Performs the query and attached the result to the target Node
     *
     * @param synCtx The current message
     * @return true always
     */
    public boolean mediate(MessageContext synCtx) {

        try {

            if (synCtx.getEnvironment().isDebugEnabled()) {
                if (super.divertMediationRoute(synCtx)) {
                    return true;
                }
            }

            SynapseLog synLog = getLog(synCtx);

            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("Start : XQuery mediator");

                if (synLog.isTraceTraceEnabled()) {
                    synLog.traceTrace("Message : " + synCtx.getEnvelope());
                }
                synLog.traceOrDebug("Performing XQuery using query resource with key : " +
                        queryKey);
            }

            // perform the xquery
            performQuery(synCtx, synLog);

            synLog.traceOrDebug("End : XQuery mediator");

            return true;

        } catch (Exception e) {
            handleException("Unable to execute the query ", e);
        }
        return false;
    }

    /**
     * Perform the quering and get the result and attached to the target node
     *
     * @param synCtx The current MessageContext
     * @param synLog the Synapse log to use
     */
    private void performQuery(MessageContext synCtx, SynapseLog synLog) {

        boolean reLoad = false;
        boolean needSet = false;
        XQueryEvaluator queryEvaluator = null;
        String generatedQueryKey = null;
        XQueryExecutable xQueryExecutable = null;
        XdmValue xdmValue;
        boolean isQueryKeyGenerated = false;

        if (queryKey != null) {
            // Derive actual key from xpath or get static key
            generatedQueryKey = queryKey.evaluateValue(synCtx);
        }

        if (generatedQueryKey != null) {
            isQueryKeyGenerated = true;
        }

        if (generatedQueryKey != null && !"".equals(generatedQueryKey)) {

            Entry dp = synCtx.getConfiguration().getEntryDefinition(generatedQueryKey);
            // if the queryKey refers to a dynamic resource
            if (dp != null && dp.isDynamic()) {
                if (!dp.isCached() || dp.isExpired()) {
                    reLoad = true;
                }
            }
        }

        try {
            synchronized (resourceLock) {
                //creating processor
                if (cachedProcessor == null) {
                    cachedProcessor = new Processor(false);
                    //setting up the properties to the Processor
                    if (processorProperties != null && !processorProperties.isEmpty()) {
                        synLog.traceOrDebug("Setting up properties to the XQDataSource");
                        for (MediatorProperty processorProperty : processorProperties) {
                            if (processorProperty != null) {
                                cachedProcessor.setConfigurationProperty(processorProperty.getName(),
                                        processorProperty.getValue());
                            }
                        }
                    }
                }

                //creating XQueryCompiler
                if (cachedQueryCompiler == null) {
                    synLog.traceOrDebug("Creating a compiler from the Processor ");
                    cachedQueryCompiler = cachedProcessor.newXQueryCompiler();
                }

                //If already cached evaluator then load it from cachedXQueryEvaluatorMap
                if (isQueryKeyGenerated) {
                    queryEvaluator = cachedXQueryEvaluatorMap.get(generatedQueryKey);
                }

                if (reLoad || queryEvaluator == null) {

                    if (querySource != null && !"".equals(querySource)) {

                        if (synLog.isTraceOrDebugEnabled()) {
                            synLog.traceOrDebug("Using in-lined query source - " + querySource);
                            synLog.traceOrDebug("Prepare an expression for the query ");
                        }

                        xQueryExecutable = cachedQueryCompiler.compile(querySource);
                        queryEvaluator = xQueryExecutable.load();

                        // if queryEvaluator is created then put it in to cachedXQueryEvaluatorMap
                        if (isQueryKeyGenerated) {
                            cachedXQueryEvaluatorMap.put(generatedQueryKey, queryEvaluator);
                        }

                        // need set because the expression just has recreated
                        needSet = true;


                    } else {

                        Object o = synCtx.getEntry(generatedQueryKey);
                        if (o == null) {
                            if (synLog.isTraceOrDebugEnabled()) {
                                synLog.traceOrDebug("Couldn't find the xquery source with a key "
                                        + queryKey);
                            }
                            throw new SynapseException("No object found for the key '" + generatedQueryKey + "'");
                        }

                        String sourceCode = null;
                        InputStream inputStream = null;
                        if (o instanceof OMElement) {
                            sourceCode = ((OMElement) (o)).getText();
                        } else if (o instanceof String) {
                            sourceCode = (String) o;
                        } else if (o instanceof OMText) {
                            DataHandler dataHandler = (DataHandler) ((OMText) o).getDataHandler();
                            if (dataHandler != null) {
                                try {
                                    inputStream = dataHandler.getInputStream();
                                    if (inputStream == null) {
                                        if (synLog.isTraceOrDebugEnabled()) {
                                            synLog.traceOrDebug("Couldn't get" +
                                                    " the stream from the xquery source with a key "
                                                    + queryKey);
                                        }
                                        return;
                                    }

                                } catch (IOException e) {
                                    handleException("Error in reading content as a stream ");
                                }
                            }
                        }

                        if ((sourceCode == null || "".equals(sourceCode)) && inputStream == null) {
                            if (synLog.isTraceOrDebugEnabled()) {
                                synLog.traceOrDebug("Couldn't find the xquery source with a key "
                                        + queryKey);
                            }
                            return;
                        }

                        if (synLog.isTraceOrDebugEnabled()) {
                            synLog.traceOrDebug("Picked up the xquery source from the " +
                                    "key " + queryKey);
                            synLog.traceOrDebug("Prepare an expression for the query ");
                        }

                        try {

                            if (sourceCode != null) {
                                //create an xQueryExecutable using the query source
                                xQueryExecutable = cachedQueryCompiler.compile(sourceCode);
                            } else {
                                xQueryExecutable = cachedQueryCompiler.compile(inputStream);
                            }

                        } catch (IOException e) {
                            handleException("Error during the query inputStream compilation");
                        }

                        queryEvaluator = xQueryExecutable.load();


                        // if queryEvaluator is created then put it in to cachedXQueryEvaluatorMap
                        if (isQueryKeyGenerated) {
                            cachedXQueryEvaluatorMap.put(generatedQueryKey, queryEvaluator);
                        }

                        // need set because the evaluator just has recreated
                        needSet = true;
                    }
                }

                //Set the external variables to the queryEvaluator
                if (variables != null && !variables.isEmpty()) {
                    synLog.traceOrDebug("Binding  external variables to the DynamicContext");
                    for (MediatorVariable variable : variables) {
                        if (variable != null) {
                            boolean hasValueChanged = variable.evaluateValue(synCtx);
                            //if the value has changed or need set because the evaluator has recreated
                            if (hasValueChanged || needSet) {
                                //Set the external variable to the queryEvaluator
                                setVariable(queryEvaluator, variable, synLog);
                            }
                        }
                    }
                }

                //executing the query
                xdmValue = queryEvaluator.evaluate();

            }

            if (queryEvaluator == null) {
                synLog.traceOrDebug("Result Sequence is null");
                return;
            }

            //processing the result
            for (XdmItem xdmItem : xdmValue) {

                if (xdmItem == null) {
                    return;
                }

                XdmNodeKind xdmNodeKind = null;
                ItemType itemType = null;

                if (xdmItem.isAtomicValue()) {
                    itemType = getItemType(xdmItem, cachedProcessor);

                    if (itemType == null) {
                        return;
                    }

                } else {
                    xdmNodeKind = ((XdmNode) xdmItem).getNodeKind();
                }

                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("The XQuery Result " + xdmItem.toString());
                }

                //The target node that is going to modify
                OMNode destination = target.selectOMNode(synCtx, synLog);
                if (destination != null) {
                    if (synLog.isTraceOrDebugEnabled()) {
                        synLog.traceOrDebug("The target node " + destination);
                    }

                    //If the result is XML
                    if (XdmNodeKind.DOCUMENT == xdmNodeKind || XdmNodeKind.ELEMENT == xdmNodeKind) {

                        StAXOMBuilder builder = new StAXOMBuilder(XMLInputFactory.newInstance().createXMLStreamReader(
                                new StringReader(xdmItem.toString())));
                        OMElement resultOM = builder.getDocumentElement();

                        if (resultOM != null) {
                            //replace the target node from the result
                            destination.insertSiblingAfter(resultOM);
                            destination.detach();
                        }

                    } else if (ItemType.INTEGER == itemType || ItemType.INT == itemType) {
                        //replace the text value of the target node by the result ,If the result is
                        // a basic type
                        ((OMElement) destination).setText(String.valueOf(((XdmAtomicValue) xdmItem).getDecimalValue().intValue()));
                    } else if (ItemType.BOOLEAN == itemType) {
                        ((OMElement) destination).setText(String.valueOf(((XdmAtomicValue) xdmItem).getBooleanValue()));
                    } else if (ItemType.DOUBLE == itemType) {
                        ((OMElement) destination).setText(String.valueOf(((XdmAtomicValue) xdmItem).getDoubleValue()));
                    } else if (ItemType.FLOAT == itemType) {
                        ((OMElement) destination).setText(String.valueOf(((XdmAtomicValue) xdmItem).getDecimalValue().floatValue()));
                    } else if (ItemType.LONG == itemType) {
                        ((OMElement) destination).setText(String.valueOf(((XdmAtomicValue) xdmItem).getLongValue()));
                    } else if (ItemType.SHORT == itemType) {
                        ((OMElement) destination).setText(String.valueOf(((XdmAtomicValue) xdmItem).getDecimalValue().shortValue()));
                    } else if (ItemType.BYTE == itemType) {
                        ((OMElement) destination).setText(String.valueOf(((XdmAtomicValue) xdmItem).getDecimalValue().byteValue()));
                    } else if (ItemType.STRING == itemType) {
                        ((OMElement) destination).setText(String.valueOf(((XdmAtomicValue) xdmItem).getValue()));
                    }

                } else if (null == target.getXPath() && null == destination) {
                    //In the case soap body doesn't have the first element --> Empty soap body

                    destination = synCtx.getEnvelope().getBody();

                    if (synLog.isTraceOrDebugEnabled()) {
                        synLog.traceOrDebug("The target node " + destination);
                    }

                    //If the result is XML
                    if (XdmNodeKind.ELEMENT == xdmNodeKind || XdmNodeKind.DOCUMENT == xdmNodeKind) {
                        StAXOMBuilder builder = new StAXOMBuilder(
                                XMLInputFactory.newInstance().createXMLStreamReader(
                                        new StringReader(xdmItem.toString())));
                        OMElement resultOM = builder.getDocumentElement();
                        if (resultOM != null) {
                            ((OMElement) destination).addChild(resultOM);
                        }
                    }
                    //No else part since soap body could have only XML part not text values

                }
                break;   // Only take the *first* value of the result sequence
            }
            queryEvaluator.close();  // closing the result sequence

        } catch (SaxonApiException e) {
            handleException("Error during the querying " + e.getMessage(), e);
        } catch (XMLStreamException e) {
            handleException("Error during retrieving  the Document Node as  the result "
                    + e.getMessage(), e);
        }
    }

    /**
     * Binding a variable to the Dynamic Context in order to available during doing the querying
     *
     * @param queryEvaluator   The XQuery evaluator to which the variable will be added
     * @param variable         The variable which contains the name and vaule for adding
     * @param synLog           the Synapse log to use
     * @throws SaxonApiException throws if any error occurs when adding the variable
     */
    private void setVariable(XQueryEvaluator queryEvaluator, MediatorVariable variable,
                             SynapseLog synLog) throws SaxonApiException {

        QName name = new QName(variable.getName().getLocalPart());

        if (variable != null) {

            ItemType type = variable.getType();
            XdmNodeKind nodeKind = variable.getNodeKind();
            Object value = variable.getValue();

            if (value != null && (type != null || nodeKind != null)) {

                if (synLog.isTraceOrDebugEnabled()) {
                    synLog.traceOrDebug("Binding a variable to the DynamicContext with a name : "
                            + name + " and a value : " + value);
                }

                //Binding the basic type As-Is and XML element as an InputSource
                if (ItemType.BOOLEAN == type) {
                    boolean booleanValue = false;
                    if (value instanceof String) {
                        booleanValue = Boolean.parseBoolean((String) value);
                    } else if (value instanceof Boolean) {
                        booleanValue = (Boolean) value;
                    } else {
                        handleException("Incompatible type for the Boolean");
                    }
                    queryEvaluator.setExternalVariable(name, new XdmAtomicValue(String.valueOf(booleanValue), ItemType.BOOLEAN));
                } else if (ItemType.INTEGER == type) {
                    int intValue = -1;
                    if (value instanceof String) {
                        try {
                            intValue = Integer.parseInt((String) value);
                        } catch (NumberFormatException e) {
                            handleException("Incompatible value '" + value + "' " +
                                    "for the Integer", e);
                        }
                    } else if (value instanceof Integer) {
                        intValue = (Integer) value;
                    } else {
                        handleException("Incompatible type for the Integer");
                    }
                    if (intValue != -1) {
                        queryEvaluator.setExternalVariable(name, new XdmAtomicValue(String.valueOf(intValue), ItemType.INTEGER));
                    }

                } else if (ItemType.INT == type) {
                    int intValue = -1;
                    if (value instanceof String) {
                        try {
                            intValue = Integer.parseInt((String) value);
                        } catch (NumberFormatException e) {
                            handleException("Incompatible value '" + value +
                                    "' for the Int", e);
                        }
                    } else if (value instanceof Integer) {
                        intValue = (Integer) value;
                    } else {
                        handleException("Incompatible type for the Int");
                    }
                    if (intValue != -1) {
                        queryEvaluator.setExternalVariable(name, new XdmAtomicValue(String.valueOf(intValue), ItemType.INT));
                    }

                } else if (ItemType.LONG == type) {
                    long longValue = -1;
                    if (value instanceof String) {
                        try {
                            longValue = Long.parseLong((String) value);
                        } catch (NumberFormatException e) {
                            handleException("Incompatible value '" + value + "' " +
                                    "for the long ", e);
                        }
                    } else if (value instanceof Long) {
                        longValue = (Long) value;
                    } else {
                        handleException("Incompatible type for the Long");
                    }
                    if (longValue != -1) {
                        queryEvaluator.setExternalVariable(name, new XdmAtomicValue(String.valueOf(longValue), ItemType.LONG));
                    }

                } else if (ItemType.SHORT == type) {
                    short shortValue = -1;
                    if (value instanceof String) {
                        try {
                            shortValue = Short.parseShort((String) value);
                        } catch (NumberFormatException e) {
                            handleException("Incompatible value '" + value + "' " +
                                    "for the short ", e);
                        }
                    } else if (value instanceof Short) {
                        shortValue = (Short) value;
                    } else {
                        handleException("Incompatible type for the Short");
                    }
                    if (shortValue != -1) {
                        queryEvaluator.setExternalVariable(name, new XdmAtomicValue(String.valueOf(shortValue), ItemType.SHORT));
                    }

                } else if (ItemType.DOUBLE == type) {
                    double doubleValue = -1;
                    if (value instanceof String) {
                        try {
                            doubleValue = Double.parseDouble((String) value);
                        } catch (NumberFormatException e) {
                            handleException("Incompatible value '" + value + "' " +
                                    "for the double ", e);
                        }
                    } else if (value instanceof Double) {
                        doubleValue = (Double) value;
                    } else {
                        handleException("Incompatible type for the Double");
                    }
                    if (doubleValue != -1) {
                        queryEvaluator.setExternalVariable(name, new XdmAtomicValue(String.valueOf(doubleValue), ItemType.DOUBLE));
                    }

                } else if (ItemType.FLOAT == type) {
                    float floatValue = -1;
                    if (value instanceof String) {
                        try {
                            floatValue = Float.parseFloat((String) value);
                        } catch (NumberFormatException e) {
                            handleException("Incompatible value '" + value + "' " +
                                    "for the float ", e);
                        }
                    } else if (value instanceof Float) {
                        floatValue = (Float) value;
                    } else {
                        handleException("Incompatible type for the Float");
                    }
                    if (floatValue != -1) {
                        queryEvaluator.setExternalVariable(name, new XdmAtomicValue(String.valueOf(floatValue), ItemType.FLOAT));
                    }

                } else if (ItemType.BYTE == type) {
                    byte byteValue = -1;
                    if (value instanceof String) {
                        try {
                            byteValue = Byte.parseByte((String) value);
                        } catch (NumberFormatException e) {
                            handleException("Incompatible value '" + value + "' " +
                                    "for the byte ", e);
                        }
                    } else if (value instanceof Byte) {
                        byteValue = (Byte) value;
                    } else {
                        handleException("Incompatible type for the Byte");
                    }
                    if (byteValue != -1) {
                        queryEvaluator.setExternalVariable(name, new XdmAtomicValue(String.valueOf(byteValue), ItemType.BYTE));
                    }

                } else if (ItemType.STRING == type) {

                    if (value instanceof String) {
                        queryEvaluator.setExternalVariable(name, new XdmAtomicValue(String.valueOf(value), ItemType.STRING));
                    } else {
                        handleException("Incompatible type for the String");
                    }

                } else if (XdmNodeKind.DOCUMENT == nodeKind || XdmNodeKind.ELEMENT == nodeKind) {
                    setOMNode(name, value, queryEvaluator, cachedProcessor);

                } else {
                    handleException("Unsupported  type for the binding type" + type +
                            " in the variable name " + name);
                }
            }

        } else {
                /*
                The following block will invariably result in  "javax.xml.xquery.XQException: Argument value is null",
                    which is the proper behaviour for this case.
                This block was added to fix the issue where if a variable with non-null value is passed for evaluation
                    and if the subsequent request has a null value for the same variable (i.e. with the same name), the
                    previous non-null value is given out instead of considering the null-case.
                */
            if (synLog.isTraceOrDebugEnabled()) {
                synLog.traceOrDebug("Null variable value encountered for variable name: " + name);
            }

            queryEvaluator.setExternalVariable(name, null);

        }

    }

    private void setOMNode(QName name, Object value,
                           XQueryEvaluator queryEvaluator, Processor processor) throws SaxonApiException{

        OMElement variableValue = null;
        if (value instanceof String) {
            variableValue = SynapseConfigUtils.stringToOM((String) value);
        } else if (value instanceof OMElement) {
            variableValue = (OMElement) value;
        }

        if (variableValue != null) {
            DocumentBuilder documentBuilder = processor.newDocumentBuilder();
            if (useDOMSource) {


                XdmNode xdmNode = documentBuilder.build(new DOMSource(((Element) ElementHelper.
                        importOMElement(variableValue,
                                DOOMAbstractFactory.getOMFactory())).
                        getOwnerDocument()));

                queryEvaluator.setExternalVariable(name, xdmNode);

            } else {
                StreamSource streamSource = new StreamSource(SynapseConfigUtils.getInputStream(variableValue));
                XdmNode xdmNode = documentBuilder.build(streamSource);
                queryEvaluator.setExternalVariable(name, xdmNode);
            }
        }
    }

    private static ItemType getItemType(XdmItem item, Processor process)throws SaxonApiException{
        return new ItemTypeFactory(process).getAtomicType(((XdmAtomicValue) item).getPrimitiveTypeName());
    }


    private void handleException(String msg, Exception e) {
        log.error(msg, e);
        throw new SynapseException(msg, e);
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    public Value getQueryKey() {
        return queryKey;
    }

    public void setQueryKey(Value queryKey) {
        this.queryKey = queryKey;
    }

    public String getQuerySource() {
        return querySource;
    }

    public void setQuerySource(String querySource) {
        this.querySource = querySource;
    }

    public void addAllVariables(List<MediatorVariable> list) {
        this.variables.addAll(list);
    }

    public void addVariable(MediatorVariable variable) {
        this.variables.add(variable);
    }

    public List<MediatorProperty> getProcessorProperties() {
        return processorProperties;
    }

    public List<MediatorVariable> getVariables() {
        return variables;
    }

    public SynapseXPath getTarget() {
        return target.getXPath();
    }

    public void setTarget(SynapseXPath source) {
        this.target.setXPath(source);
    }

    public void addAllDataSourceProperties(List<MediatorProperty> list) {
        this.processorProperties.addAll(list);
    }

    public boolean isUseDOMSource() {
        return useDOMSource;
    }

    public void setUseDOMSource(boolean useDOMSource) {
        this.useDOMSource = useDOMSource;
    }
}
