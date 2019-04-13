package org.apache.synapse.transport.passthru;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.ParameterInclude;
import org.apache.axis2.description.TransportInDescription;
import org.apache.http.HttpHost;
import org.apache.synapse.transport.http.conn.Scheme;
import org.apache.synapse.transport.nhttp.config.ServerConnFactoryBuilder;
import org.apache.synapse.transport.dynamicconfigurations.ListenerProfileReloader;
import org.apache.synapse.transport.dynamicconfigurations.SSLProfileLoader;

public class PassThroughHttpMultiSSLListener extends PassThroughHttpListener implements SSLProfileLoader{

    @Override
    public void init(ConfigurationContext cfgCtx, TransportInDescription transportInDescription)
            throws AxisFault {
        super.init(cfgCtx, transportInDescription);
        new ListenerProfileReloader(this, transportInDescription);
    }

    @Override
    protected Scheme initScheme() {
        return new Scheme("https", 443, true);
    }

    @Override
    protected ServerConnFactoryBuilder initConnFactoryBuilder(final TransportInDescription transportIn,
            final HttpHost host, ConfigurationContext configurationContext) throws AxisFault {
        return new ServerConnFactoryBuilder(transportIn, host, configurationContext)
                .parseSSL()
                .parseMultiProfileSSL();
    }

    /**
     * Reload SSL profiles and reset connections in PassThroughHttpMultiSSLListener
     *
     * @param transport TransportInDescription of the configuration
     * @throws AxisFault
     */
    public void reloadConfig(ParameterInclude transport) throws AxisFault {
        reloadDynamicSSLConfig((TransportInDescription) transport);
    }

}
