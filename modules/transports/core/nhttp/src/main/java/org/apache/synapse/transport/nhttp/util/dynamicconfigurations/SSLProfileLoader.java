package org.apache.synapse.transport.nhttp.util.dynamicconfigurations;

import org.apache.axis2.description.ParameterInclude;

public interface SSLProfileLoader {

    public void reloadConfig(SSLProfileLoader profileLoader, ParameterInclude transport);
}
