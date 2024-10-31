package org.apache.synapse.transport.dynamicconfigurations;

import java.util.ArrayList;
import java.util.List;

public class KeyStoreReloaderHolder {

    private static KeyStoreReloaderHolder instance = new KeyStoreReloaderHolder();
    private List<KeyStoreReloader> keyStoreLoaders;

    private KeyStoreReloaderHolder() {
        keyStoreLoaders = new ArrayList<>();
    }

    public static KeyStoreReloaderHolder getInstance() {
        return instance;
    }

    public void addKeyStoreLoader(KeyStoreReloader keyStoreLoader) {
        keyStoreLoaders.add(keyStoreLoader);
    }

    public void reloadAllKeyStores() {
        for (KeyStoreReloader keyStoreLoader : keyStoreLoaders) {
            keyStoreLoader.update();
        }
    }
}
