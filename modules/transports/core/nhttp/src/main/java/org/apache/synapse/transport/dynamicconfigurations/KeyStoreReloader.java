package org.apache.synapse.transport.dynamicconfigurations;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.ParameterInclude;

public class KeyStoreReloader {

    private IKeyStoreLoader keyStoreLoader;
    private ParameterInclude transportOutDescription;

    public KeyStoreReloader(IKeyStoreLoader keyStoreLoader, ParameterInclude transportOutDescription) {

        this.keyStoreLoader = keyStoreLoader;
        this.transportOutDescription = transportOutDescription;

        registerListener(transportOutDescription);
    }

    private void registerListener(ParameterInclude transportOutDescription) {

        KeyStoreReloaderHolder.getInstance().addKeyStoreLoader(this);
    }

    public void update() {

        try {
            keyStoreLoader.loadKeyStore(transportOutDescription);
        } catch (AxisFault e) {
            throw new RuntimeException(e);
        }
    }
}
