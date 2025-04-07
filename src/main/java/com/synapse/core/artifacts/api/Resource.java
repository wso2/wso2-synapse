package com.synapse.core.artifacts.api;

import com.synapse.core.artifacts.Sequence;

public class Resource {
    private String methods;
    private String uriTemplate;
    private Sequence inSequence;
    private Sequence faultSequence;

    public Resource(String methods, String uriTemplate, Sequence inSequence, Sequence faultSequence) {
        this.methods = methods;
        this.uriTemplate = uriTemplate;
        this.inSequence = inSequence;
        this.faultSequence = faultSequence;
    }

    public Resource() {

    }

    public String getMethods() {
        return methods;
    }

    public void setMethods(String methods) {
        this.methods = methods;
    }

    public String getUriTemplate() {
        return uriTemplate;
    }

    public void setUriTemplate(String uriTemplate) {
        this.uriTemplate = uriTemplate;
    }

    public Sequence getInSequence() {
        return inSequence;
    }

    public void setInSequence(Sequence inSequence) {
        this.inSequence = inSequence;
    }

    public Sequence getFaultSequence() {
        return faultSequence;
    }

    public void setFaultSequence(Sequence faultSequence) {
        this.faultSequence = faultSequence;
    }
}
