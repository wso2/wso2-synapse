package org.apache.synapse.transport.nhttp.util;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.commons.jmx.MBeanRegistrar;
import org.apache.synapse.transport.nhttp.HttpCoreNIOMultiSSLListener;
import org.apache.synapse.transport.nhttp.config.ServerConnFactoryBuilder;

/*
* This class is for reloading the Multi profiles for the listener
*
*
*
*/


public class SSLReloader {

    private HttpCoreNIOMultiSSLListener httpCoreNIOMultiSSLListener;
    private TransportInDescription transportInDescription;


    public SSLReloader(HttpCoreNIOMultiSSLListener multiSSLListener, TransportInDescription inDescription) {
        this.httpCoreNIOMultiSSLListener = multiSSLListener;
        this.transportInDescription = inDescription;
    }

    public String reloadSSLProfileConfig() throws AxisFault {

        //reload the ssl profiles for the listener side

        Parameter oldParameter = transportInDescription.getParameter("SSLProfiles");
        Parameter profilePathParam = transportInDescription.getParameter("SSLProfilesConfigPath");
        if(oldParameter!=null && profilePathParam!=null) {
            transportInDescription.removeParameter(oldParameter);
            ServerConnFactoryBuilder builder = new ServerConnFactoryBuilder(transportInDescription, null);
            TransportInDescription loadedTransportIn = builder.loadMultiProfileSSLConfig();
            if (loadedTransportIn != null) {
                transportInDescription=loadedTransportIn;
                httpCoreNIOMultiSSLListener.reload(transportInDescription);
                return "SSLProfiles reloaded Successfully";
            }
            //add old value back
            transportInDescription.addParameter(oldParameter);
        }
        return "Failed to reload SSLProfiles";
    }
}
