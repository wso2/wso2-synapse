package org.apache.synapse.transport.passthru;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

class SenderProfileNotifierThread implements Runnable{
    PassThroughHttpSender httpSender;
    TransportOutDescription transportOutDescription;
    String path="";
    private File file;

    SenderProfileNotifierThread(PassThroughHttpSender httpSender, TransportOutDescription tod) {
        this.httpSender=httpSender;
        this.transportOutDescription=tod;
        Parameter outPathParam=transportOutDescription.getParameter("OutBoundSSLProfilesConfigPath");

        if(outPathParam==null){
            file=null;
        }
        else{
            OMElement outPathElement=outPathParam.getParameterElement();
            path=outPathElement.getFirstChildWithName(new QName("filePath")).getText();
            file=new File(path);
            if(!file.exists()){
                file=null;
            }
        }
    }

    public void run(){
        TimerTask task=new FileChange(file,this.httpSender,transportOutDescription);
        Timer timer=new Timer();

        //Start polling thread to catch the file modification
        timer.schedule(task,new Date(),1000);
    }
}

class FileChange extends TimerTask {

    private static final Log log=LogFactory.getLog(TimerTask.class);

    private long timestamp;
    private File configFile;
    private PassThroughHttpSender httpSender;
    private TransportOutDescription transportOutDescription;
    private FileChannel fileCh;
    private FileLock fLock;
    private boolean status=false;

    public FileChange(File file,PassThroughHttpSender httpSender,TransportOutDescription tod){
        this.configFile=file;
        if(configFile!=null){
            this.timestamp=file.lastModified();
            status=true;
        }
        this.httpSender=httpSender;
        this.transportOutDescription=tod;
        fLock=null;
    }

    @Override
    public void run(){

        Thread senderThread=null;

        if(configFile!=null){
            try {
                //get a lock for the file in order to access it
                fileCh=new RandomAccessFile(configFile,"rw").getChannel();
                fLock=fileCh.tryLock();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }catch (IOException e) {
                e.printStackTrace();
            }

            //If the file is not being accessing by another process check  whether the
            //file is modified
            if(fLock.isValid()){
                long timeStamp2 = configFile.lastModified();
                if (timeStamp2!=timestamp){
                    timestamp=timeStamp2;
                    try {
                        //start the sender thread
                        httpSender.startSenderThread(transportOutDescription,senderThread);

                        //release the lock and the file chanel
                        fLock.release();
                        fileCh.close();
                        fLock=null;
                        fileCh=null;

                    } catch (AxisFault axisFault) {
                        axisFault.printStackTrace();
                    }
                    catch (IOException ioe){
                        ioe.printStackTrace();
                    }
                }
                else{
                    try {
                        fLock.release();
                        fileCh.close();
                        fLock=null;
                        fileCh=null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            else {
                try {
                    fLock.release();
                    fileCh.close();
                    fLock=null;
                    fileCh=null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                log.info("Failed to acquire File lock");
            }
        }

        else{
            if(!status){
                try {
                    httpSender.startSenderThread(transportOutDescription,senderThread);
                    status=true;
                } catch (AxisFault axisFault) {
                    axisFault.printStackTrace();
                }
            }

        }
    }

}