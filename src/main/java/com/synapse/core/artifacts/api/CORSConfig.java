package com.synapse.core.artifacts.api;

public class CORSConfig {

    private boolean enabled = false;
    private String[] allowOrigins = {"*"};
    private String[] allowMethods = {"GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"};
    private String[] allowHeaders = {"Origin", "Content-Type", "Accept", "Authorization"};
    private String[] exposeHeaders = {};
    private boolean allowCredentials = false;
    private int maxAge = 86400;

    public CORSConfig() {
        this.enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String[] getAllowOrigins() {
        return allowOrigins;
    }

    public void setAllowOrigins(String[] allowOrigins) {
        this.allowOrigins = allowOrigins;
    }

    public String[] getAllowMethods() {
        return allowMethods;
    }

    public void setAllowMethods(String[] allowMethods) {
        this.allowMethods = allowMethods;
    }

    public String[] getAllowHeaders() {
        return allowHeaders;
    }

    public void setAllowHeaders(String[] allowHeaders) {
        this.allowHeaders = allowHeaders;
    }

    public String[] getExposeHeaders() {
        return exposeHeaders;
    }

    public void setExposeHeaders(String[] exposeHeaders) {
        this.exposeHeaders = exposeHeaders;
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public int getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }
}