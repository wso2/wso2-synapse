package org.apache.synapse.transport.nhttp;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.description.TransportInDescription;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.transport.nhttp.util.SSLReloader;
import org.apache.synapse.transport.passthru.PassThroughHttpSender;

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

class ListenerProfileNotifierThread implements Runnable{

    private HttpCoreNIOMultiSSLListener nioListener;
    private TransportInDescription transportInDescription;
    private String path="";
    private File file;

    ListenerProfileNotifierThread(HttpCoreNIOMultiSSLListener nioListener,TransportInDescription trpIn) {
        this.nioListener=nioListener;
        this.transportInDescription=trpIn;

        //get the config file path from the transportIn parameter
        Parameter outPathParam=transportInDescription.getParameter("SSLProfilesConfigPath");
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
        TimerTask task=new FileChange(file,nioListener,transportInDescription);
        Timer timer=new Timer();

        //Start polling thread to catch the file modification
        timer.schedule(task,new Date(),1000);
    }
}

class FileChange extends TimerTask {

    private static final Log log = LogFactory.getLog(FileChange.class);

    private long timestamp;
    private File configFile;
    private HttpCoreNIOMultiSSLListener nioListener;
    private TransportInDescription transportIn;
    public FileChannel fileCh;
    private boolean status=false;
    FileLock fLock;

    public FileChange(File file,HttpCoreNIOMultiSSLListener nioListener,TransportInDescription transportIn){
        this.nioListener=nioListener;
        this.configFile=file;
        if(configFile!=null){
            this.timestamp=file.lastModified();
            status=true;
        }
        this.transportIn=transportIn;
        fLock=null;
    }

    @Override
    public void run(){

        if(configFile!=null){
            try {
                fileCh=new RandomAccessFile(configFile,"rw").getChannel();
                fLock=fileCh.tryLock();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }catch (IOException e) {
                e.printStackTrace();
            }

            //Check whether the lock is valid
            if(fLock.isValid()){
                long timeStamp2 = configFile.lastModified();
                if (timeStamp2!=timestamp){
                    timestamp=timeStamp2;

                    log.info("Listener multi profile configuration Change occurred");

                    try {
                        //reload the SSL profile
                        String reloadMessage=new SSLReloader(nioListener,this.transportIn).reloadSSLProfileConfig();
                        log.info(reloadMessage);

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
                String reloadMessage="";
                try {
                    reloadMessage = new SSLReloader(nioListener,this.transportIn).reloadSSLProfileConfig();
                    log.info(reloadMessage);
                    status=true;
                } catch (AxisFault axisFault) {
                    axisFault.printStackTrace();
                }

            }
        }
    }
}
