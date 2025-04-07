//package com.synapse.utils;
//
//import org.apache.commons.vfs2.*;
//import org.apache.commons.vfs2.impl.StandardFileSystemManager;
//import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
//
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//
//public class Dummy {
//
//    private static final String SFTP_USER = "sftpuser";
//    private static final String SFTP_PASS = "1234Hello";
//    private static final String SFTP_HOST = "127.0.0.1";
//    private static final int SFTP_PORT = 22;
//
//    private static final String SFTP_BASE_URI = "sftp://" + SFTP_USER + ":" + SFTP_PASS + "@" + SFTP_HOST + ":" + SFTP_PORT;
//    private static final String REMOTE_IN = SFTP_BASE_URI + "/remote/path/in";
//    private static final String REMOTE_OUT = SFTP_BASE_URI + "/remote/path/out";
//
//    public static void main(String[] args) throws Exception {
//        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
//        StandardFileSystemManager manager = null;
//
//        try {
//            manager = new StandardFileSystemManager();
//            manager.init();
//
//            FileSystemOptions opts = new FileSystemOptions();
//            SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(opts, "no");
//
//            FileObject inputDir = manager.resolveFile(REMOTE_IN, opts);
//            FileObject outputDir = manager.resolveFile(REMOTE_OUT, opts);
//
//            if (!inputDir.exists()) {
//                System.err.println("Error: Input directory " + REMOTE_IN + " does not exist.");
//                return;
//            }
//
//            if (!outputDir.exists()) {
//                outputDir.createFolder();
//                System.out.println("Created output directory: " + REMOTE_OUT);
//            }
//
//            FileObject[] files = inputDir.getChildren();
//            System.out.println("Found " + files.length + " files in " + REMOTE_IN);
//
//            for (FileObject file : files) {
////                if (!file.isFile()) {
////                    System.out.println("Skipping non-file: " + file.getName().getBaseName());
////                    continue;
////                }
//
//                FileObject finalFile = file; // Create a final variable for the lambda
//                StandardFileSystemManager finalManager = manager;
//                executor.submit(() -> {
//                    String filename = finalFile.getName().getBaseName();
//                    FileObject innerManager = null;
//                    try {
//                        innerManager = finalManager.resolveFile(REMOTE_OUT + "/" + filename, opts);
//                        finalFile.moveTo(innerManager);
//                        System.out.println("Moved " + filename + " to " + REMOTE_OUT);
//                    } catch (Exception e) {
//                        System.err.println("Error moving " + filename + ": " + e.getMessage());
//                    } finally {
//                        // Ensure FileObject is closed if moveTo fails or for other scenarios
//                        if (innerManager != null) {
//                            try {
//                                innerManager.close();
//                            } catch (FileSystemException e) {
//                                System.err.println("Error closing target file object for " + filename + ": " + e.getMessage());
//                            }
//                        }
//                        try {
//                            finalFile.close();
//                        } catch (FileSystemException e) {
//                            System.err.println("Error closing source file object " + filename + ": " + e.getMessage());
//                        }
//                    }
//                });
//            }
//
//            executor.shutdown();
//            boolean terminated = executor.awaitTermination(5, TimeUnit.MINUTES);
//
//            if (terminated) {
//                System.out.println("All files processed.");
//            } else {
//                System.err.println("Timeout waiting for file processing to complete.");
//            }
//
//        } catch (FileSystemException e) {
//            System.err.println("Error initializing or accessing file systems: " + e.getMessage());
//        } finally {
//            if (manager != null) {
//                manager.close();
//            }
//        }
//    }
//}


package com.synapse.utils;

import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.impl.StandardFileSystemManager;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Dummy {

    private static final String SFTP_USER = "sftpuser";
    private static final String SFTP_PASS = "1234Hello";
    private static final String SFTP_HOST = "127.0.0.1";
    private static final int SFTP_PORT = 22;

    private static final String SFTP_BASE_URI = "sftp://" + SFTP_USER + ":" + SFTP_PASS + "@" + SFTP_HOST + ":" + SFTP_PORT;
    private static final String REMOTE_IN = SFTP_BASE_URI + "/remote/path/in";
    private static final String REMOTE_OUT = SFTP_BASE_URI + "/remote/path/out";

    public static void main(String[] args) throws Exception {
        StandardFileSystemManager manager = null;
        List<Thread> virtualThreads = new ArrayList<>();

        try {
            manager = new StandardFileSystemManager();
            manager.init();

            FileSystemOptions opts = new FileSystemOptions();
            SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(opts, "no");

            FileObject inputDir = manager.resolveFile(REMOTE_IN, opts);
            FileObject outputDir = manager.resolveFile(REMOTE_OUT, opts);

            if (!inputDir.exists()) {
                System.err.println("Error: Input directory " + REMOTE_IN + " does not exist.");
                return;
            }

            if (!outputDir.exists()) {
                outputDir.createFolder();
                System.out.println("Created output directory: " + REMOTE_OUT);
            }

            FileObject[] files = inputDir.getChildren();
            System.out.println("Found " + files.length + " files in " + REMOTE_IN);

            for (FileObject file : files) {
                FileObject finalFile = file;
                StandardFileSystemManager finalManager = manager;

                Thread virtualThread = Thread.ofVirtual().start(() -> {
                    String filename = finalFile.getName().getBaseName();
                    FileObject innerManager = null;
                    try {
                        innerManager = finalManager.resolveFile(REMOTE_OUT + "/" + filename, opts);
                        finalFile.moveTo(innerManager);
                        System.out.println("Moved (Virtual Thread) " + filename + " to " + REMOTE_OUT);
                    } catch (Exception e) {
                        System.err.println("Error moving (Virtual Thread) " + filename + ": " + e.getMessage());
                    } finally {
                        if (innerManager != null) {
                            try {
                                innerManager.close();
                            } catch (FileSystemException e) {
                                System.err.println("Error closing target file object (Virtual Thread) for " + filename + ": " + e.getMessage());
                            }
                        }
                        try {
                            finalFile.close();
                        } catch (FileSystemException e) {
                            System.err.println("Error closing source file object (Virtual Thread) " + filename + ": " + e.getMessage());
                        }
                    }
                });
                virtualThreads.add(virtualThread);
            }

            // Wait for all virtual threads to complete
            for (Thread thread : virtualThreads) {
                thread.join(TimeUnit.MINUTES.toMillis(5)); // Set a timeout for each thread
                if (thread.isAlive()) {
                    System.err.println("Timeout waiting for a virtual thread to complete.");
                }
            }

            System.out.println("All files processed by virtual threads.");

        } catch (FileSystemException e) {
            System.err.println("Error initializing or accessing file systems: " + e.getMessage());
        } finally {
            if (manager != null) {
                manager.close();
            }
        }
    }
}