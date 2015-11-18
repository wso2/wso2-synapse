/**
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.flowtracer;

import org.apache.synapse.flowtracer.data.MessageFlowComponentEntry;
import org.apache.synapse.flowtracer.data.MessageFlowTraceEntry;

public class MessageFlowDbReporterTask implements Runnable{

    private boolean running = true;

    public void terminate(){
        running = false;
    }

    @Override
    public void run() {
        while(running){
            MessageFlowComponentEntry componentInfoEntry = MessageFlowDataHolder.getComponentInfoEntry();

            MessageFlowTraceEntry flowInfoEntry = MessageFlowDataHolder.getFlowInfoEntry();

            if(componentInfoEntry!=null){
                MessageFlowDbConnector.getInstance().persistMessageFlowComponentEntry(componentInfoEntry);
            }

            if(flowInfoEntry!=null){
                MessageFlowDbConnector.getInstance().persistMessageFlowTraceEntry(flowInfoEntry);
            }
        }
    }

}
