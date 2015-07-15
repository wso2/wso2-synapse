package org.apache.synapse;

/**
 * Created by nadeeshaan on 7/3/15.
 */
public class CustomLogSetter {
    private static CustomLogSetter instance = null;
    private static ThreadLocal<String> logAppender = new ThreadLocal<>();

    protected CustomLogSetter() {
        // Exists only to defeat instantiation.
    }
    public static CustomLogSetter getInstance() {
        if(instance == null) {
            instance = new CustomLogSetter();
        }
        return instance;
    }

    public void setLogAppender (String appenderContent) {
        if (!"".equals(appenderContent)){
            logAppender.set(appenderContent);
        }
    }

    public String getLogAppenederContent () {
        return logAppender.get();
    }
}
