package org.apache.synapse.mediators.util;

/**
 * Represents a namespace in XML schema
 */
public class XmlNamespace {
    private String prefix;
    private String uri;

    /**
     * Create namespace representation with prefix and uri
     *
     * @param prefix namespace prefix
     * @param uri    namespace uri
     */
    public XmlNamespace(String prefix, String uri) {

        this.prefix = prefix;
        this.uri = uri;
    }

    public String getPrefix() {

        return prefix;
    }

    public void setPrefix(String prefix) {

        this.prefix = prefix;
    }

    public String getUri() {

        return uri;
    }

    public void setUri(String uri) {

        this.uri = uri;
    }
}
