package org.apache.synapse.flowtracer;

public class MessageFlowDbReporterTask implements Runnable{

    private boolean running = true;

    public void terminate(){
//        running = false;
    }

    @Override
    public void run() {
        while(running){
            MessageFlowEntry entry = MessageFlowDataHolder.getEntry();

            if(entry!=null){
                MessageFlowDbConnector.getInstance().writeToDb(entry);
            }
            else {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
