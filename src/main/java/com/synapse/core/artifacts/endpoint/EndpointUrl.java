package com.synapse.core.artifacts.endpoint;

public class EndpointUrl {
    private String method;
    private String url;

    public EndpointUrl() {
    }

    public EndpointUrl(String method, String url) {
        this.method = method;
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "EndpointUrl{" +
                "method='" + method + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}
