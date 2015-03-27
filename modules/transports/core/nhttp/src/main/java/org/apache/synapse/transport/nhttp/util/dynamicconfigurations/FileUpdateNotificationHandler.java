package org.apache.synapse.transport.nhttp.util.dynamicconfigurations;

import org.apache.synapse.transport.nhttp.NhttpConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

/**
 * Periodically checks on configuration file updates and notify respective listeners
 */
public class FileUpdateNotificationHandler extends TimerTask{

    //Time interval in milliseconds to check files for new updates
    private int fileReadInterval;

    //List of registered file reloaders to be notified with updates
    private static List<DynamicProfileReloader> profileReloaders;

    /**
     * Constructor with file read interval as the input
     * @param sleepInterval integer in milliseconds
     */
    public FileUpdateNotificationHandler(int sleepInterval) {

        if(sleepInterval > 0){
            this.fileReadInterval = sleepInterval;
        }
        fileReadInterval = NhttpConstants.DYNAMIC_PROFILE_RELOAD_INTERVAL;

        profileReloaders = new ArrayList<DynamicProfileReloader>();

    }

    @Override
    public void run(){

        long recordedLastUpdatedTime = 0;
        long latestLastUpdatedTime = 0;
        File configFile = null;

        for (DynamicProfileReloader profileLoader : profileReloaders){

            recordedLastUpdatedTime = profileLoader.getLastUpdatedtime();
            String filePath = profileLoader.getFilePath();

            if(filePath != null){

                try {
                    configFile = new File(filePath);
                    latestLastUpdatedTime = configFile.lastModified();
                } catch (Exception e) {
                    //todo-Jagath-implement logger
                }

                if(latestLastUpdatedTime > recordedLastUpdatedTime){
                    //Notify file update to the respective file loader
                    profileLoader.notifyFileUpdate();
                    profileLoader.setLastUpdatedtime(latestLastUpdatedTime);
                }
            }
        }
    }

    /**
     * Register listeners for file update notifications
     * @param dynamicProfileReloader Listener to be notified
     */
    public static void registerListener(DynamicProfileReloader dynamicProfileReloader){
        profileReloaders.add(dynamicProfileReloader);
    }
}
