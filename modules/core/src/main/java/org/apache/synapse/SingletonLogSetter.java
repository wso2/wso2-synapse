package org.apache.synapse;

import java.util.HashMap;

/**
 * Created by nadeeshaan on 7/3/15.
 */
public class SingletonLogSetter {
    private static SingletonLogSetter instance = null;
    private static ThreadLocal<String> logAppender = new ThreadLocal<>();
    private static HashMap<String, String> axisServicesMapper = new HashMap<>();

    protected SingletonLogSetter() {
        // Exists only to defeat instantiation.
    }
    public static SingletonLogSetter getInstance() {
        if(instance == null) {
            instance = new SingletonLogSetter();
        }
        return instance;
    }

    public void setLogAppender (String appenderContent) {
        logAppender.set(appenderContent);
    }

    public String getLogAppenederContent () {
        return logAppender.get();
    }

    public void addAxisService (String serviceName, String carName) {
        axisServicesMapper.put(serviceName, carName);
    }

    public HashMap<String,String> getAxisServicesMapper () {
        return axisServicesMapper;
    }
}
