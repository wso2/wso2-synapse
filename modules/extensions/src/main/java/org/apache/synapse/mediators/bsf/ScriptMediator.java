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
 */

package org.apache.synapse.mediators.bsf;

import com.google.gson.JsonParser;
import com.sun.phobos.script.javascript.RhinoScriptEngineFactory;
import com.sun.script.groovy.GroovyScriptEngineFactory;
import com.sun.script.jruby.JRubyScriptEngineFactory;
import com.sun.script.jython.JythonScriptEngineFactory;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMText;
import org.apache.bsf.xml.XMLHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;
import org.apache.synapse.SynapseLog;
import org.apache.synapse.commons.json.JsonUtil;
import org.apache.synapse.commons.util.MiscellaneousUtil;
import org.apache.synapse.config.Entry;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.eip.EIPUtils;
import org.mozilla.javascript.Context;
import javax.activation.DataHandler;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A Synapse mediator that calls a function in any scripting language supported by the BSF.
 * The ScriptMediator supports scripts specified in-line or those loaded through a registry
 * <p/>
 * <pre>
 *    &lt;script [key=&quot;entry-key&quot;]
 *      [function=&quot;script-function-name&quot;] language="nashornJs|javascript|groovy|ruby"&gt
 *      (text | xml)?
 *    &lt;/script&gt;
 * </pre>
 * <p/>
 * <p/>
 * The function is an optional attribute defining the name of the script function to call,
 * if not specified it defaults to a function named 'mediate'. The function takes a single
 * parameter which is the Synapse MessageContext. The function may return a boolean, if it
 * does not then true is assumed.
 */
public class ScriptMediator extends AbstractMediator {

    private static final Log logger = LogFactory.getLog(ScriptMediator.class.getName());

    /**
     * The name of the variable made available to the scripting language to access the message
     */
    private static final String MC_VAR_NAME = "mc";

    /**
     * Name of the java script language
     */
    private static final String JAVA_SCRIPT = "js";

    /**
     * Name of the java script language with usage of nashorn engine.
     */
    private static final String NASHORN_JAVA_SCRIPT = "nashornJs";

    /**
     * Name of the nashorn java script engine.
     */
    private static final String NASHORN = "nashorn";
    /**
     * The registry entry key for a script loaded from the registry
     * Handle both static and dynamic(Xpath) Keys
     */
    private Value key;
    /**
     * The language of the script code
     */
    private String language;
    /**
     * The map of included scripts; key = registry entry key, value = script source
     */
    private final Map<Value, Object> includes;
    /**
     * The optional name of the function to be invoked, defaults to mediate
     */
    private String function = "mediate";
    /**
     * The source code of the script
     */
    private String scriptSourceCode;
    /**
     * The BSF engine created to process each message through the script
     */
    protected ScriptEngine scriptEngine;
    /**
     * The BSF engine created to validate each JSON payload
     */
    protected ScriptEngine jsEngine;
    /**
     * Does the ScriptEngine support multi-threading
     */
    private boolean multiThreadedEngine;
    /**
     * Reference to an empty JSON object.
     */
    private ScriptObjectMirror emptyJsonObject;

    /**
     * Reference to JSON object which is used to serialize json.
     */
    private ScriptObjectMirror jsonSerializer;
    /**
     * The compiled script. Only used for inline scripts
     */
    private CompiledScript compiledScript;
    /**
     * The BSF helper to convert between the XML representations used by Java
     * and the scripting language
     */
    private XMLHelper xmlHelper;
    /**
     * Script Engine Manger
     */
    private ScriptEngineManager engineManager;
    /**
     * Default Pool Size
     */
    private int DEFAULT_POOL_SIZE = 15;
    /**
     * Pool size
     */
    private int poolSize = DEFAULT_POOL_SIZE;
    /**
     * Pool size property name
     */
    private static String POOL_SIZE_PROPERTY = "synapse.script.mediator.pool.size";
    /**
     * Pool ScriptEngine Resources
     */
    private BlockingQueue<ScriptEngineWrapper> pool;
    /**
     * JSON parser used to parse JSON strings
     */
    private JsonParser jsonParser;

    /**
     * Store the class loader from properties
     */
    private ClassLoader loader;

    /**
     * Create a script mediator for the given language and given script source.
     *
     * @param language         the BSF language
     * @param scriptSourceCode the source code of the script
     */
    public ScriptMediator(String language, String scriptSourceCode, ClassLoader classLoader) {
        this.language = language;
        this.scriptSourceCode = scriptSourceCode;
        this.setLoader(classLoader);
        this.includes = new TreeMap<Value, Object>();
        initInlineScript();
    }

    /**
     * Create a script mediator for the given language and given script entry key and function.
     *
     * @param language       the BSF language
     * @param includeKeysMap Include script keys
     * @param key            the registry entry key to load the script
     * @param function       the function to be invoked
     */
    public ScriptMediator(String language, Map<Value, Object> includeKeysMap,
                          Value key, String function, ClassLoader classLoader) {
        this.language = language;
        this.key = key;
        this.setLoader(classLoader);
        this.includes = includeKeysMap;
        if (function != null) {
            this.function = function;
        }

        Properties properties = MiscellaneousUtil.loadProperties("synapse.properties");
        poolSize = Integer.parseInt(properties.getProperty(POOL_SIZE_PROPERTY, String.valueOf(DEFAULT_POOL_SIZE)));

        initScriptEngine();
        if (!(scriptEngine instanceof Invocable)) {
            throw new SynapseException("Script engine is not an Invocable" +
                    " engine for language: " + language);
        }

    }

    /**
     * Perform Script mediation.
     *
     * @param synCtx the Synapse message context
     * @return the boolean result from the script invocation
     */
    public boolean mediate(MessageContext synCtx) {

        if (synCtx.getEnvironment().isDebuggerEnabled()) {
            if (super.divertMediationRoute(synCtx)) {
                return true;
            }
        }

        SynapseLog synLog = getLog(synCtx);


        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Start : Script mediator");

            if (synLog.isTraceTraceEnabled()) {
                synLog.traceTrace("Message : " + synCtx.getEnvelope());
            }
        }

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("Scripting language : " + language + " source " +
                    (key == null ? ": specified inline " : " loaded with key : " + key) +
                    (function != null ? " function : " + function : ""));
        }

        boolean returnValue;
        if (multiThreadedEngine) {
            returnValue = invokeScript(synCtx);
        } else {
            // TODO: change to use a pool of script engines (requires an update to BSF)
            synchronized (scriptEngine.getClass()) {
                returnValue = invokeScript(synCtx);
            }
        }

        if (synLog.isTraceTraceEnabled()) {
            synLog.traceTrace("Result message after execution of script : " + synCtx.getEnvelope());
        }

        if (synLog.isTraceOrDebugEnabled()) {
            synLog.traceOrDebug("End : Script mediator return value : " + returnValue);
        }
        return returnValue;
    }

    private boolean invokeScript(MessageContext synCtx) {
        boolean returnValue;
        try {
            //if the engine is Rhino then needs to set the class loader specifically
            if (language.equals("js")) {
                Context cx = Context.enter();
                cx.setApplicationClassLoader(this.loader);

            }

            Object returnObject;
            if (key != null) {
                returnObject = mediateWithExternalScript(synCtx);
            } else {
                returnObject = mediateForInlineScript(synCtx);
            }
            returnValue = !(returnObject != null && returnObject instanceof Boolean) || (Boolean) returnObject;

        } catch (ScriptException e) {
            handleException("The script engine returned an error executing the " +
                    (key == null ? "inlined " : "external ") + language + " script" +
                    (key != null ? " : " + key : "") +
                    (function != null ? " function " + function : ""), e, synCtx);
            returnValue = false;
        } catch (NoSuchMethodException e) {
            handleException("The script engine returned a NoSuchMethodException executing the " +
                    "external " + language + " script" + " : " + key +
                    (function != null ? " function " + function : ""), e, synCtx);
            returnValue = false;
        } catch (Exception e) {
            handleException("The script engine returned an Exception executing the " +
                    "external " + language + " script" + " : " + key +
                    (function != null ? " function " + function : ""), e, synCtx);
            returnValue = false;
        } finally {
            if (language.equals("js")) {
                Context.exit();
            }
        }

        return returnValue;
    }

    /**
     * Mediation implementation when the script to be executed should be loaded from the registry
     *
     * @param synCtx the message context
     * @return script result
     * @throws ScriptException       For any errors , when compile, run the script
     * @throws NoSuchMethodException If the function is not defined in the script
     */
    private Object mediateWithExternalScript(MessageContext synCtx)
            throws ScriptException, NoSuchMethodException {
        ScriptEngineWrapper sew = null;
        Object obj;
        try {
            sew = prepareExternalScript(synCtx);
            XMLHelper helper;
            if (language.equalsIgnoreCase(JAVA_SCRIPT) || language.equals(NASHORN_JAVA_SCRIPT)) {
                helper = xmlHelper;
            } else {
                helper = XMLHelper.getArgHelper(sew.getEngine());
            }
            ScriptMessageContext scriptMC;
            scriptMC = getScriptMessageContext(synCtx, helper);
            processJSONPayload(synCtx, scriptMC);
            Invocable invocableScript = (Invocable) sew.getEngine();

            obj = invocableScript.invokeFunction(function, new Object[]{scriptMC});
        } finally {
          if(sew != null){
              // return engine to front of queue or drop if queue is full (i.e. if getNewScriptEngine() spawns a new engine)
              pool.offer(sew);
          }
        }


        return obj;
    }

    /**
     * Get script message context according to scripting language.
     *
     * @param synCtx message context
     * @param helper Object which help to convert xml into OMelemnt
     * @return Nashorn or Common script message context according to language attribute
     */
    private ScriptMessageContext getScriptMessageContext(MessageContext synCtx, XMLHelper helper) {
        ScriptMessageContext scriptMC;
        if (language.equals(NASHORN_JAVA_SCRIPT)) {
            try {
                emptyJsonObject = (ScriptObjectMirror) scriptEngine.eval("({})");
                jsonSerializer = (ScriptObjectMirror) scriptEngine.eval("JSON");
            } catch (ScriptException e) {
                throw new SynapseException("Error occurred while evaluating empty json object", e);
            }
            scriptMC = new NashornJavaScriptMessageContext(synCtx, helper, emptyJsonObject, jsonSerializer);
        } else {
            scriptMC = new CommonScriptMessageContext(synCtx, helper);
        }
        return scriptMC;
    }

    /**
     * Perform mediation with static inline script of the given scripting language
     *
     * @param synCtx message context
     * @return true, or the script return value
     * @throws ScriptException For any errors , when compile , run the script
     */
    private Object mediateForInlineScript(MessageContext synCtx) throws ScriptException {
        ScriptMessageContext scriptMC;
        scriptMC = getScriptMessageContext(synCtx, xmlHelper);
        processJSONPayload(synCtx, scriptMC);
        Bindings bindings = scriptEngine.createBindings();
        bindings.put(MC_VAR_NAME, scriptMC);

        Object response;
        if (compiledScript != null) {
            response = compiledScript.eval(bindings);
        } else {
            response = scriptEngine.eval(scriptSourceCode, bindings);
        }
        return response;
    }

    private void processJSONPayload(MessageContext synCtx, ScriptMessageContext scriptMC) throws ScriptException {
        if (!(synCtx instanceof Axis2MessageContext)) {
            return;
        }
        org.apache.axis2.context.MessageContext messageContext = ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        String jsonString = (String) messageContext.getProperty("JSON_STRING");
        Object jsonObject = null;
        prepareForJSON(scriptMC);
        if (JsonUtil.hasAJsonPayload(messageContext)) {
            try {
                String jsonPayload = JsonUtil.jsonPayloadToString(messageContext);
                if (NASHORN_JAVA_SCRIPT.equals(language)) {
                    jsonObject = jsonSerializer.callMember("parse", jsonPayload);
                } else {
                    String scriptWithJsonParser = "JSON.parse(JSON.stringify(" + jsonPayload + "))";
                    jsonObject = this.jsEngine.eval('(' + scriptWithJsonParser + ')');
                }
            } catch (ScriptException e) {
                throw new ScriptException("Invalid JSON payload", e.getFileName(), e.getLineNumber(),
                        e.getColumnNumber());
            }
        } else if (jsonString != null) {
            String jsonPayload = EIPUtils.tryParseJsonString(jsonParser, jsonString).toString();
            jsonObject = this.jsEngine.eval('(' + jsonPayload + ')');
        }
        if (jsonObject != null) {
            scriptMC.setJsonObject(synCtx, jsonObject);
        }
    }

    private void prepareForJSON(ScriptMessageContext scriptMC) {
        if (jsonParser == null) {
            jsonParser = new JsonParser();
        }
        scriptMC.setScriptEngine(this.jsEngine);
    }

    /**
     * Initialise the Mediator for the inline script
     */
    protected void initInlineScript() {
        try {
            initScriptEngine();

            if (scriptEngine instanceof Compilable) {
                if (log.isDebugEnabled()) {
                    log.debug("Script engine supports Compilable interface, " +
                            "compiling script code..");
                }
                compiledScript = ((Compilable) scriptEngine).compile(scriptSourceCode);
            } else {
                // do nothing. If the script engine doesn't support Compilable then
                // the inline script will be evaluated on each invocation
                if (log.isDebugEnabled()) {
                    log.debug("Script engine does not support the Compilable interface, " +
                            "in-lined script would be evaluated on each invocation..");
                }
            }

        } catch (ScriptException e) {
            throw new SynapseException("Exception initializing inline script", e);
        }
    }

    /**
     * Prepares the mediator for the invocation of an external script
     *
     * @param synCtx MessageContext script
     * @throws ScriptException For any errors , when compile the script
     */
    protected ScriptEngineWrapper prepareExternalScript(MessageContext synCtx)
            throws ScriptException {

        // Derive actual key from xpath expression or get static key
        String generatedScriptKey = key.evaluateValue(synCtx);
        Entry entry = synCtx.getConfiguration().getEntryDefinition(generatedScriptKey);
        boolean needsReload = (entry != null) && entry.isDynamic() &&
                (!entry.isCached() || entry.isExpired());

        ScriptEngineWrapper sew = getNewScriptEngine();
        Bindings engineBinding = sew.getEngine().getBindings(ScriptContext.ENGINE_SCOPE);
        engineBinding.clear(); // if we don't do this, previous state can affect successive executions! ESBJAVA-4583

        if (scriptSourceCode == null || needsReload || !sew.isInitialized()) {
            Object o = synCtx.getEntry(generatedScriptKey);
            if (o instanceof OMElement) {
                scriptSourceCode = ((OMElement) (o)).getText();
                sew.getEngine().eval(scriptSourceCode, engineBinding);
            } else if (o instanceof String) {
                scriptSourceCode = (String) o;
                sew.getEngine().eval(scriptSourceCode, engineBinding);
            } else if (o instanceof OMText) {
                DataHandler dataHandler = (DataHandler) ((OMText) o).getDataHandler();
                if (dataHandler != null) {
                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(
                                new InputStreamReader(dataHandler.getInputStream()));
                        StringBuilder scriptSB = new StringBuilder();
                        String currentLine;
                        while ((currentLine = reader.readLine()) != null) {
                            scriptSB.append(currentLine).append('\n');
                        }
                        scriptSourceCode = scriptSB.toString();
                        sew.getEngine().eval(scriptSourceCode, engineBinding);
                    } catch (IOException e) {
                        handleException("Error in reading script as a stream ", e, synCtx);
                    } finally {

                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException e) {
                                handleException("Error in closing input stream ", e, synCtx);
                            }
                        }

                    }
                }
            }

        } else {
            sew.getEngine().eval(scriptSourceCode, engineBinding); // Will drop TPS, but is required for ESBJAVA-4583
        }

        // load <include /> scripts; reload each script if needed
        for (Value includeKey : includes.keySet()) {

            String includeSourceCode = (String) includes.get(includeKey);

            String generatedKey = includeKey.evaluateValue(synCtx);

            Entry includeEntry = synCtx.getConfiguration().getEntryDefinition(generatedKey);
            boolean includeEntryNeedsReload = (includeEntry != null) && includeEntry.isDynamic()
                    && (!includeEntry.isCached() || includeEntry.isExpired());
            if (includeSourceCode == null || includeEntryNeedsReload || !sew.isInitialized()) {
                log.debug("Re-/Loading the include script with key " + includeKey);
                Object o = synCtx.getEntry(generatedKey);
                if (o instanceof OMElement) {
                    includeSourceCode = ((OMElement) (o)).getText();
                    sew.getEngine().eval(includeSourceCode, engineBinding);
                } else if (o instanceof String) {
                    includeSourceCode = (String) o;
                    sew.getEngine().eval(includeSourceCode, engineBinding);
                } else if (o instanceof OMText) {
                    DataHandler dataHandler = (DataHandler) ((OMText) o).getDataHandler();
                    if (dataHandler != null) {
                        BufferedReader reader = null;
                        try {
                            reader = new BufferedReader(
                                    new InputStreamReader(dataHandler.getInputStream()));
                            StringBuilder scriptSB = new StringBuilder();
                            String currentLine;
                            while ((currentLine = reader.readLine()) != null) {
                                scriptSB.append(currentLine).append('\n');
                            }
                            includeSourceCode = scriptSB.toString();
                            sew.getEngine().eval(includeSourceCode, engineBinding);
                        } catch (IOException e) {
                            handleException("Error in reading script as a stream ", e, synCtx);
                        } finally {

                            if (reader != null) {
                                try {
                                    reader.close();
                                } catch (IOException e) {
                                    handleException("Error in closing input" +
                                            " stream ", e, synCtx);
                                }
                            }
                        }
                    }
                }
                includes.put(includeKey, includeSourceCode);
            } else {
                sew.getEngine().eval(includeSourceCode, engineBinding); // Will drop TPS, but required for ESBJAVA-4583
            }
        }

        sew.setInitialized(true);

        return sew;
    }

    protected void initScriptEngine() {
        if (log.isDebugEnabled()) {
            log.debug("Initializing script mediator for language : " + language);
        }

        engineManager = new ScriptEngineManager();
        if (!language.equals(NASHORN_JAVA_SCRIPT)) {
            engineManager.registerEngineExtension("jsEngine", new RhinoScriptEngineFactory());
        }

        engineManager.registerEngineExtension("js", new RhinoScriptEngineFactory());
        engineManager.registerEngineExtension("groovy", new GroovyScriptEngineFactory());
        engineManager.registerEngineExtension("rb", new JRubyScriptEngineFactory());
        engineManager.registerEngineExtension("py", new JythonScriptEngineFactory());
        if (language.equals(NASHORN_JAVA_SCRIPT)) {
            this.scriptEngine = engineManager.getEngineByName(NASHORN);
        } else {
            this.scriptEngine = engineManager.getEngineByExtension(language);
        }

        pool = new LinkedBlockingQueue<ScriptEngineWrapper>(poolSize);

        for (int i = 0; i< poolSize; i++) {
            ScriptEngineWrapper sew;
            if (language.equals(NASHORN_JAVA_SCRIPT)) {
                sew = new ScriptEngineWrapper(engineManager.getEngineByName(NASHORN));
            } else {
                sew = new ScriptEngineWrapper(engineManager.getEngineByExtension(language));
            }
            pool.add(sew);
        }
        if (language.equals(NASHORN_JAVA_SCRIPT)) {
            this.jsEngine = engineManager.getEngineByName(NASHORN);
        } else {
            this.jsEngine = engineManager.getEngineByExtension("jsEngine");
        }
        if (scriptEngine == null) {
            handleException("No script engine found for language: " + language);
        }
        //Invoking a custom Helper class for api change in rhino17 and also for Nashorn engine based implimentation
        if (language.equalsIgnoreCase(JAVA_SCRIPT)) {
            xmlHelper = new JavaScriptXmlHelper();
        } else if (language.equals(NASHORN_JAVA_SCRIPT)) {
            xmlHelper = new NashornJavaScriptXmlHelper();
        } else {
            xmlHelper = XMLHelper.getArgHelper(scriptEngine);
        }


        this.multiThreadedEngine = scriptEngine.getFactory().getParameter("THREADING") != null;
        log.debug("Script mediator for language : " + language +
                " supports multithreading? : " + multiThreadedEngine);
    }

    public String getLanguage() {
        return language;
    }

    public Value getKey() {
        return key;
    }

    public String getFunction() {
        return function;
    }

    public String getScriptSrc() {
        return scriptSourceCode;
    }

    private void handleException(String msg) {
        log.error(msg);
        throw new SynapseException(msg);
    }

    public Map<Value, Object> getIncludeMap() {
        return includes;
    }

    public ClassLoader getLoader() {
        return loader;
    }

    public void setLoader(ClassLoader loader) {
        this.loader = loader;
    }

    public ScriptEngineWrapper getNewScriptEngine() {

        ScriptEngineWrapper scriptEngineWrapper = pool.poll();
        if (scriptEngineWrapper == null) {
            if (language.equals(NASHORN_JAVA_SCRIPT)) {
                scriptEngineWrapper = new ScriptEngineWrapper(engineManager.getEngineByName(NASHORN));
            } else {
                scriptEngineWrapper = new ScriptEngineWrapper(engineManager.getEngineByExtension(language));
            }
        }
        // fall back
        return scriptEngineWrapper;
    }

    public boolean isContentAltering() {
        return true;
    }

}
