package org.apache.synapse.transport.nhttp.config;

/**
 * A SSL Sender TrustStore Holder class to store the client trust store's configurable details.
 */
public class SslSenderTrustStoreHolder {

    private static volatile SslSenderTrustStoreHolder instance;

    private SslSenderTrustStoreHolder() {}

    private String location;
    private String password;
    private String type;

    public static SslSenderTrustStoreHolder getInstance() {

        if (instance == null) {
            synchronized (TrustStoreHolder.class) {
                if (instance == null) {
                    instance = new SslSenderTrustStoreHolder();
                }
            }
        }
        return instance;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocation() {
        return this.location;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return this.password;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }
}
