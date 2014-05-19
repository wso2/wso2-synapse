/*
*  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.apache.synapse.inboundfactory;

import java.util.Properties;

import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.inbound.PollingProcessor;
import org.apache.synapse.inbound.PollingProcessorFactory;
import org.apache.synapse.protocol.file.VFSProcessor;
import org.apache.synapse.protocol.jms.JMSProcessor;

public class PollingProcessorFactoryImpl implements PollingProcessorFactory{

	private static final Object obj = new Object();
	public static enum Protocols {jms, file};
	
	public PollingProcessor creatPollingProcessor(String protocol, String name, Properties properties, long scanInterval, String injectingSeq, String onErrorSeq, SynapseEnvironment synapseEnvironment){
		synchronized (obj) {
			PollingProcessor pollingProcessor = null;
			if(Protocols.jms.toString().equals(protocol)){
				pollingProcessor = new JMSProcessor(name, properties, scanInterval, injectingSeq, onErrorSeq, synapseEnvironment);
			}else if(Protocols.file.toString().equals(protocol)){
				pollingProcessor = new VFSProcessor(name, properties, scanInterval, injectingSeq, onErrorSeq, synapseEnvironment);
			}
			return pollingProcessor;
		}
	}
	
}
