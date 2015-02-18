package org.apache.synapse.mediators.collector;

import org.apache.log4j.Logger;

/*import org.wso2.carbon.databridge.agent.thrift.Agent;
import org.wso2.carbon.databridge.agent.thrift.AsyncDataPublisher;
import org.wso2.carbon.databridge.agent.thrift.conf.AgentConfiguration;
import org.wso2.carbon.databridge.agent.thrift.exception.AgentException;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.StreamDefinition;

*/

import java.util.ArrayList;
import java.util.HashMap;
 
public class DataPublishClient {
   /* private static Logger logger = Logger.getLogger(DataPublishClient.class);
    public static final String MEDIATOR_DATA_STREAM = "mediator_data_stream4";
    public static final String VERSION = "1.0.0";
  
    public static void publishNow(int availableSize) {
    	
    	
         
        System.setProperty("javax.net.ssl.trustStore", "/home/pasadi/wso2bam-2.4.1/repository/resources/security/client-truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");
         
        //Using Asynchronous data publisher
        AsyncDataPublisher asyncDataPublisher = new AsyncDataPublisher("tcp://localhost:7611", "admin", "admin");
        String streamDefinition = "{" +
                " 'name':'" + MEDIATOR_DATA_STREAM + "'," +
                " 'version':'" + VERSION + "'," +
                " 'nickName': 'mediator_stat'," +
                " 'description': 'per mediator data'," +
                " 'metaData':[" +
                " {'name':'publishermediationIP','type':'STRING'}" +
                " ]," +
                " 'payloadData':[" +
                " {'name':'Name','type':'STRING'}," +
                " {'name':'Type','type':'STRING'}," +
                " {'name':'MsgId','type':'STRING'}," +
                " {'name':'Success','type':'BOOL'}," +
                " {'name':'StartTime','type':'LONG'}," +
                " {'name':'EndTime','type':'LONG'}," +
                " {'name':'ProxyName','type':'STRING'}," +
                " {'name':'MediatorId','type':'STRING'}," +
                " {'name':'Envelope','type':'STRING'}," +
                " {'name':'RootType','type':'STRING'}" +
                " ]" +
                "}";
      //  if(asyncDataPublisher.isStreamDefinitionAdded(streamDefinition))
        asyncDataPublisher.addStreamDefinition(streamDefinition, MEDIATOR_DATA_STREAM, VERSION);
       // StreamDefinition c = new StreamDefinition(streamDefinition);
        //StreamDefinition d = new StreamDefinition(streamDefinition, MEDIATOR_DATA_STREAM, VERSION);
        
        *//*********//*
        publishEvents(asyncDataPublisher,availableSize);
       // asyncDataPublisher.addStreamDefinition(definition)
    }
    *//**
     * @param asyncDataPublisher
     *//*
    private static void publishEvents(AsyncDataPublisher asyncDataPublisher,int sizeToPublish) {
       
        for(int i=0;i<sizeToPublish;i++){
        	Object[] payload = new Object[]{StoreList.storage.get(i).contents.getMediatorName(),StoreList.storage.get(i).contents.getType(),StoreList.storage.get(i).contents.getMsgID(),StoreList.storage.get(i).contents.isSuccess(),StoreList.storage.get(i).contents.getStartTime(),StoreList.storage.get(i).contents.getEndTime(),StoreList.storage.get(i).contents.getServiceName(),StoreList.storage.get(i).contents.getId(),StoreList.storage.get(i).contents.getEnvelop(),StoreList.storage.get(i).contents.getRootType() };
        
        	HashMap<String, String> map = new HashMap<String, String>();
           
            Event event = eventObject(null, new Object[]{"10.100.3.173"}, payload, map);
            try {
                asyncDataPublisher.publish(MEDIATOR_DATA_STREAM, VERSION, event);
               
            } catch (AgentException e) {
                logger.error("Failed to publish event", e);
            }	
        }
       // StoreList.timestamp=System.currentTimeMillis();
       // StoreList.storage.subList(0, sizeToPublish).clear();
    }
    private static Event eventObject(Object[] correlationData, Object[] metaData,
                                     Object[] payLoadData, HashMap<String, String> map) {
        Event event = new Event();
        event.setCorrelationData(correlationData);
        event.setMetaData(metaData);
        event.setPayloadData(payLoadData);
        event.setArbitraryDataMap(map);
        return event;
    }*/
}