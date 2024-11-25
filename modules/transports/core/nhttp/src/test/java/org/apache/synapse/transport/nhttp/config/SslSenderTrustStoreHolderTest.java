package org.apache.synapse.transport.nhttp.config;

import org.junit.Assert;
import org.junit.Test;

public class SslSenderTrustStoreHolderTest {

    @Test
    public void testGetInstance() {
        SslSenderTrustStoreHolder instance = SslSenderTrustStoreHolder.getInstance();
        SslSenderTrustStoreHolder instance2 = SslSenderTrustStoreHolder.getInstance();
        Assert.assertEquals(instance, instance2);
    }
}
