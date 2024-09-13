package org.apache.synapse.mediators.bsf;

public class ScriptMediatorConstants {

    /**
     * The name of the variable made available to the scripting language to access the message
     */
    public static final String MC_VAR_NAME = "mc";

    /**
     * Name of the java script language
     */
    public static final String JAVA_SCRIPT = "js";

    /**
     * Name of the java script language with usage of nashorn engine.
     */
    public static final String NASHORN_JAVA_SCRIPT = "nashornJs";

    /**
     * Name of the java script language with usage of rhino engine.
     */
    public static final String RHINO_JAVA_SCRIPT = "rhinoJs";

    /**
     * Name of the java script language with usage of rhino engine.
     */
    public static final String GRAAL_JAVA_SCRIPT = "graalJs";

    /**
     * Name of the nashorn java script engine.
     */
    public static final String NASHORN = "nashorn";

    /**
     * Name of the graalvm js engine.
     */
    public static final String GRAALVM = "graal.js";

    /**
     * Factory Name for Oracle Nashorn Engine. Built-in Nashorn engine in JDK 8 to JDK 11
     */
    public static final String ORACLE_NASHORN_NAME = "Oracle Nashorn";

    /**
     * Pool size property name
     */
    public static String POOL_SIZE_PROPERTY = "synapse.script.mediator.pool.size";

    /**
     * Default Script Engine
     */
    public static String DEFAULT_SCRIPT_ENGINE = "synapse.script.mediator.default.engine";
}
