package org.apache.synapse.inbound;

import java.util.Properties;

import org.apache.synapse.core.SynapseEnvironment;

/**
 * Interface is required when implementing custom consumer
 * */
public abstract class GenericPollingConsumer implements PollingConsumer{

	protected Properties properties;
	protected String name;
	protected SynapseEnvironment synapseEnvironment;
	protected long scanInterval;
	protected String injectingSeq;
	protected String onErrorSeq;
	
	public GenericPollingConsumer(Properties properties, String name, SynapseEnvironment synapseEnvironment, long scanInterval, String injectingSeq, String onErrorSeq){
		this.properties = properties;
		this.name = name;
		this.synapseEnvironment = synapseEnvironment;
		this.scanInterval = scanInterval;
		this.injectingSeq = injectingSeq;
		this.onErrorSeq = onErrorSeq;
	}
	
	public final void registerHandler(InjectHandler processingHandler) {
		
	}

}
