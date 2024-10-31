package org.apache.synapse.transport.dynamicconfigurations;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.ParameterInclude;

public interface IKeyStoreLoader {

    void loadKeyStore(ParameterInclude transport) throws AxisFault;
}
