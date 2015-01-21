package org.apache.synapse.util.jaxp;

import org.w3c.dom.ls.LSInput;

import java.io.InputStream;
import java.io.Reader;

/**
 * External schema resource holder for {@link org.apache.synapse.util.jaxp.SchemaResourceResolver}
 * This will use to store {@link java.io.InputStream} of external schema resource resolved by
 * {@link org.apache.synapse.util.jaxp.SchemaResourceResolver}
 *
 * Current implementation is only using {@link java.io.InputStream} to store external schema resource. Methods other
 * than {@link org.apache.synapse.util.jaxp.SchemaResourceLSInput#getByteStream()} and
 * {@link org.apache.synapse.util.jaxp.SchemaResourceLSInput#setByteStream(java.io.InputStream)} are just place holders.
 */

/**
 * getter and setter for systemId is needed when resolving the resources
 * (used in {@link org.apache.xerces.util.DOMEntityResolverWrapper}), especially when there is a cyclic import or
 * include in the resources. systemId act as a key and eliminates duplicating resources being resolved.
 * <p/>
 * Also implementing the other getters and setters which are used in {@link org.apache.synapse.util.jaxp.SchemaResourceResolver}
 */
public class SchemaResourceLSInput implements LSInput {

    InputStream byteStream = null;
    String fSystemId = null;
    String fPublicId = null;
    String fBaseURI = null;

    public Reader getCharacterStream() {
        return null;
    }

    public void setCharacterStream(Reader characterStream) {

    }

    public InputStream getByteStream() {
        return byteStream;
    }

    public void setByteStream(InputStream byteStream) {
        this.byteStream = byteStream;
    }

    public String getStringData() {
        return null;
    }

    public void setStringData(String stringData) {

    }

    public String getSystemId() {
        return fSystemId;
    }

    public void setSystemId(String systemId) {
        this.fSystemId = systemId;
    }

    public String getPublicId() {
        return fPublicId;
    }

    public void setPublicId(String publicId) {
        this.fPublicId = publicId;
    }

    public String getBaseURI() {
        return fBaseURI;
    }

    public void setBaseURI(String baseURI) {
        this.fBaseURI = baseURI;
    }

    public String getEncoding() {
        return null;
    }

    public void setEncoding(String encoding) {

    }

    public boolean getCertifiedText() {
        return false;
    }

    public void setCertifiedText(boolean certifiedText) {

    }
}
