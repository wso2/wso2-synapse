package org.apache.synapse.inbound;

import java.util.Properties;

import org.apache.synapse.core.SynapseEnvironment;

public interface PollingProcessorFactory {

	public PollingProcessor creatPollingProcessor(String protocol, String name, Properties vfsProperties, long scanInterval, String injectingSeq, String onErrorSeq, SynapseEnvironment synapseEnvironment);
	
}
