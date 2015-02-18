package org.apache.synapse.mediators.collector;

import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.SynapsePropertiesLoader;

 
public class CollectorEnabler {

/**
 * This method will check the Synapse.Properties and check whether the data collection is enalbed or not. Data 
 *collection will only take part if the value is enabled by the user
 * @return  the DATA_COLLECTOR value is set or not
 */
public static boolean checkCollectorRequired(){
   
        String enable = SynapsePropertiesLoader.getPropertyValue(SynapseConstants.MEDIATOR_DATA_COLLECTOR, SynapseConstants.DEFAULT_MEDIATOR_DATA_COLLECTOR);
        if(enable.equalsIgnoreCase("true"))
            return true;
        else return false;
    }

}
