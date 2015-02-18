package org.apache.synapse.mediators.collector;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MyTimerActionListener implements ActionListener{
	
	public void actionPerformed(ActionEvent e) {
		
		/*if(StoreList.storage.size()>=5 ||System.currentTimeMillis()-StoreList.timestamp  == 60000){
    		if(StoreList.storage.size()>=5){
    			
    		DataPublishClient.publishNow(5);
    		}else{
    			DataPublishClient.publishNow(StoreList.storage.size());	
    		}
    		
    		//StoreList.timestamp=System.currentTimeMillis();
    	   // StoreList.storage.subList(0, sizeToPublish).clear();
        }*/
		
		//uncomment this block 
		/*if(StoreList.storage.size()>=5){
			DataPublishClient.publishNow(5);
			StoreList.storage.subList(0, 5).clear();
		}
		
		else{
			DataPublishClient.publishNow(StoreList.storage.size());
			StoreList.storage.subList(0, StoreList.storage.size()).clear();
		}*/
			 
	}
	}