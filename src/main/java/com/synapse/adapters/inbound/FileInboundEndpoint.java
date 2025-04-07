package com.synapse.adapters.inbound;

import com.synapse.core.domain.InboundConfig;
import com.synapse.core.ports.InboundEndpoint;
import com.synapse.core.ports.InboundMessageMediator;
import com.synapse.core.synctx.Message;
import com.synapse.core.synctx.MsgContext;
import org.apache.commons.vfs2.*;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileInboundEndpoint implements InboundEndpoint {
    private final InboundConfig config;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final Set<String> processingFiles = ConcurrentHashMap.newKeySet();
    private final FileSystemManager fsManager;
    private InboundMessageMediator mediator;

    public FileInboundEndpoint(InboundConfig config) throws FileSystemException {
        this.config = config;
        this.fsManager = VFS.getManager();
    }

    @Override
    public void start(InboundMessageMediator mediator) throws Exception {
        this.mediator = mediator;
        isRunning.set(true);

        validateConfig();
        int interval = getIntervalParameterValue();
        if (interval <= 0) {
            throw new Exception("Invalid polling interval.");
        }

        System.out.println("Polling files every " + interval + " milliseconds");

//        scheduler.scheduleAtFixedRate(() -> {
//            if (!isRunning.get()) {
//                return;
//            }
//
//            System.out.println("outside polling thread");
////            try {
////                virtualExecutor.submit(() -> {
////                    try {
////                        System.out.println("Polling files");
////                        pollFiles();
////                    } catch (FileSystemException e) {
////                        throw new RuntimeException(e);
////                    }
////                });
////            } catch (Exception e) {
////                e.printStackTrace();
////            }
//
////            Thread.ofVirtual().start(() -> {
////                try {
////                    pollFiles();
////                    System.out.println("polling files");
////                } catch (Exception e) {
////                    e.printStackTrace();
////                }
////            });
//
//        }, 0, interval, TimeUnit.MILLISECONDS);

        scheduler.scheduleAtFixedRate(() -> {
            if (!isRunning.get()) {
                System.out.println("isRunning is false");
                return;
            }

            System.out.println("outside polling thread");

            try {
                Thread.ofVirtual().start(() -> {
                    try {
                        pollFiles();
                        System.out.println(">>> Submitting pollFiles at " + System.currentTimeMillis());
                    } catch (Exception e) {
                        System.err.println("Error in pollFiles: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                System.err.println("Error launching virtual thread: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, interval, TimeUnit.MILLISECONDS);



        System.out.println("outside scheduler");
    }

    @Override
    public void stop() throws Exception {
        isRunning.set(false);
        scheduler.shutdown();
        System.out.println("Stopping file inbound endpoint...");
    }

    private void pollFiles() throws FileSystemException {
//        System.out.println("poll");
        String fileUri = config.getParameters().get("transport.vfs.FileURI");

        FileObject folder = fsManager.resolveFile(fileUri);

        if (!folder.exists()) {
            System.err.println("Invalid folder URI: " + fileUri);
            return;
        }

        String patternStr = config.getParameters().getOrDefault("transport.vfs.FileNamePattern", ".*.");
        Pattern filePattern = Pattern.compile(patternStr);

        FileObject[] children = folder.getChildren();
        if (children == null || children.length == 0) {
            return;
        }

        boolean isSequential = isSequentialProcessing();
        for (FileObject file : children) {
            if (file.getType() == FileType.FILE) {
                Matcher matcher = filePattern.matcher(file.getName().getBaseName());
                if (matcher.matches()) {
                    if (isSequential) {
                        handleFile(file);
                    } else {
                        Thread.ofVirtual().start(() -> handleFile(file));
                    }
                }
            }
        }
    }

    private void handleFile(FileObject file) {

        System.out.println("inside handleFile " + file.getName());
        String filePath = file.getName().getURI();
        if (!processingFiles.add(filePath)) {
            System.out.println("Skipping already-processing file: " + filePath);
            return;
        }
        try {
            if (!tryLockFile(file)) {
                System.out.println("File locked, skipping: " + filePath);
                return;
            }

            processSingleFile(file);

            System.out.println("File processed: " + filePath);
            handleFileAction(file, "Process");
        } catch (Exception e) {
            System.err.println("Error processing file [" + filePath + "]: " + e.getMessage());
            handleFileAction(file, "Failure");
        } finally {
            releaseLockFile(file);
            processingFiles.remove(filePath);
        }
    }

    private void processSingleFile(FileObject file) throws Exception {
        System.out.println("Processing file: " + file.getName().getURI());
        byte[] content;
        try (InputStream inputStream = file.getContent().getInputStream()) {
            content = inputStream.readAllBytes();
        }

        MsgContext context = new MsgContext();
        String contentType = config.getParameters().getOrDefault("transport.vfs.ContentType", "text/plain");
        Message msg = new Message(content, contentType);
        context.setMessage(msg);

        Map<String, String> headers = Map.of(
                "FILE_LENGTH", String.valueOf(file.getContent().getSize()),
                "LAST_MODIFIED", String.valueOf(file.getContent().getLastModifiedTime()),
                "FILE_URI", file.getName().getURI(),
                "FILE_PATH", file.getName().getPath(),
                "FILE_NAME", file.getName().getBaseName()
        );

        Map<String, String> properties = Map.of(
                "isInbound", "true",
                "ARTIFACT_NAME", "inboundendpointfile",
                "inboundEndpointName", "file",
                "ClientApiNonBlocking", "true"
        );

        context.setHeaders(headers);
        context.setProperties(properties);

        mediator.mediateInboundMessage(config.getSequenceName(), context);
//        System.out.println(context.toString());

//        throw new Exception();
    }

    private void handleFileAction(FileObject file, String actionType) {
        String actionKey = "transport.vfs.ActionAfter" + actionType;
        String action = config.getParameters().get(actionKey);

        if ("MOVE".equalsIgnoreCase(action)) {
            String moveKey = "transport.vfs.MoveAfter" + actionType;
            String movePath = config.getParameters().get(moveKey);
            if (movePath == null || movePath.isEmpty()) {
                System.err.println("No move path specified for " + actionType);
                return;
            }
            moveFile(file, movePath);
        } else {
            deleteFile(file);
        }
//        try {
//            Thread.sleep(10000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
    }

    private void moveFile(FileObject file, String destinationUri) {
        try {
            FileObject destFolder = fsManager.resolveFile(destinationUri);

            if (!destFolder.exists()) {
                destFolder.createFolder();
                System.out.println("Created destination folder: " + destFolder.getName().getURI());
            }

            FileObject destFile = destFolder.resolveFile(file.getName().getBaseName());
            System.out.println("before move: " + destFile.getName().getBaseName());

            file.moveTo(destFile);
//            destFile.copyFrom(file, Selectors.SELECT_SELF);
//            file.delete();
            System.out.println("Moved file [" + file.getName().getURI() + "] to [" + destFile.getName().getURI() + "]");

        } catch (Exception e) {
            System.err.println("File move failed: " + e.getMessage());
        }
    }


    private void deleteFile(FileObject file) {
        try {
            file.delete();
            System.out.println("Deleted file: " + file.getName().getURI());
        } catch (Exception e) {
            System.err.println("File delete failed: " + e.getMessage());
        }
    }

    private boolean tryLockFile(FileObject file) {
        try {
            String lockName = file.getName().getURI() + ".lock";
            FileObject lockFile = fsManager.resolveFile(lockName);

            if (lockFile.exists()) {
                return false;
            }
            lockFile.createFile();
            return true;
        } catch (Exception e) {
            System.err.println("Could not create lock for file [" + file.getName().getURI() + "]: " + e.getMessage());
            return false;
        }
    }

    private void releaseLockFile(FileObject file) {
        try {
            String lockName = file.getName().getURI() + ".lock";
            FileObject lockFile = fsManager.resolveFile(lockName);
            if (lockFile.exists()) {
                lockFile.delete();
            }
        } catch (Exception e) {
            System.err.println("Could not release lock: " + e.getMessage());
        }
    }

    private boolean isSequentialProcessing() {
        String seq = config.getParameters().get("sequential");
        return seq != null && Boolean.parseBoolean(seq);
    }

    int getIntervalParameterValue() {
        String intervalStr = config.getParameters().get("interval");
        if (intervalStr != null && !intervalStr.isEmpty()) {
            try {
                return Integer.parseInt(intervalStr);
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    private void validateConfig() throws Exception {
        if (!"file".equalsIgnoreCase(config.getProtocol())) {
            throw new Exception("Unsupported protocol, should be 'file'");
        }
        if (!config.getParameters().containsKey("interval")) {
            throw new Exception("Missing 'interval' parameter");
        }
        String uri = config.getParameters().get("transport.vfs.FileURI");
        if (uri == null || uri.isEmpty()) {
            throw new Exception("Missing 'transport.vfs.FileURI' parameter");
        }
    }
}

//

//
//package com.synapse.adapters.inbound;
//
//import com.synapse.core.domain.InboundConfig;
//import com.synapse.core.ports.InboundEndpoint;
//import com.synapse.core.ports.InboundMessageMediator;
//import com.synapse.core.synctx.Message;
//import com.synapse.core.synctx.MsgContext;
//import org.apache.commons.vfs2.*;
//
//import java.io.InputStream;
//import java.util.Map;
//import java.util.Set;
//import java.util.concurrent.*;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//public class FileInboundEndpoint implements InboundEndpoint {
//    private final InboundConfig config;
//    private final AtomicBoolean isRunning = new AtomicBoolean(false);
//    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
//    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
//    private final Set<String> processingFiles = ConcurrentHashMap.newKeySet();
//    private final FileSystemManager fsManager;
//    private InboundMessageMediator mediator;
//
//    public FileInboundEndpoint(InboundConfig config) throws FileSystemException {
//        this.config = config;
//        this.fsManager = VFS.getManager();
//    }
//
//    @Override
//    public void start(InboundMessageMediator mediator) throws Exception {
//        this.mediator = mediator;
//        isRunning.set(true);
//
//        validateConfig();
//        int interval = getIntervalParameterValue();
//        if (interval <= 0) {
//            throw new Exception("Invalid polling interval.");
//        }
//
//        System.out.println("Polling files every " + interval + " milliseconds");
//
//        scheduler.scheduleAtFixedRate(() -> {
//            if (!isRunning.get()) {
//                return;
//            }
//            try {
//                virtualExecutor.submit(() -> {
//                    try {
//                        System.out.println("Polling files");
//                        pollFiles();
//                    } catch (FileSystemException e) {
//                        throw new RuntimeException(e);
//                    }
//                });
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }, 0, interval, TimeUnit.MILLISECONDS);
//    }
//
//    @Override
//    public void stop() throws Exception {
//        isRunning.set(false);
//        scheduler.shutdown();
//        System.out.println("Stopping file inbound endpoint...");
//    }
//
//    private void pollFiles() throws FileSystemException {
//        String fileUri = config.getParameters().get("transport.vfs.FileURI");
//        FileObject folder = fsManager.resolveFile(fileUri);
//
//        if (!folder.exists()) {
//            System.err.println("Invalid folder URI: " + fileUri);
//            return;
//        }
//
//        String patternStr = config.getParameters().getOrDefault("transport.vfs.FileNamePattern", ".*.");
//        Pattern filePattern = Pattern.compile(patternStr);
//
//        FileObject[] children = folder.getChildren();
//        if (children == null || children.length == 0) {
//            return;
//        }
//
//        boolean isSequential = isSequentialProcessing();
//        for (FileObject file : children) {
//            if (file.getType() == FileType.FILE) {
//                Matcher matcher = filePattern.matcher(file.getName().getBaseName());
//                if (matcher.matches()) {
//                    if (isSequential) {
//                        handleFile(file);
//                    } else {
//                        virtualExecutor.submit(() -> {
//                            try {
//                                handleFile(file);
//                            } catch (Exception e) {
//                                System.err.println("Error in virtual thread: " + e.getMessage());
//                            }
//                        });
//                    }
//                }
//            }
//        }
//    }
//
//
//    private void handleFile(FileObject file) {
//        String filePath = file.getName().getURI();
//        if (!processingFiles.add(filePath)) {
//            System.out.println("Skipping already-processing file: " + filePath);
//            return;
//        }
//
//        try {
//            if (!tryLockFile(file)) {
//                System.out.println("File locked, skipping: " + filePath);
//                return;
//            }
//
//            processSingleFile(file);
//            handleFileAction(file, "Process");
//        } catch (Exception e) {
//            System.err.println("Error processing file [" + filePath + "]: " + e.getMessage());
//            handleFileAction(file, "Failure");
//        } finally {
//            releaseLockFile(file);
//            processingFiles.remove(filePath); // Ensuring cleanup
//        }
//    }
//
//
//    private void processSingleFile(FileObject file) throws Exception {
//        System.out.println("Processing file: " + file.getName().getURI());
//        byte[] content;
//        try (InputStream inputStream = file.getContent().getInputStream()) {
//            content = inputStream.readAllBytes();
//        }
//
//        MsgContext context = new MsgContext();
//        String contentType = config.getParameters().getOrDefault("transport.vfs.ContentType", "text/plain");
//        Message msg = new Message(content, contentType);
//        context.setMessage(msg);
//
//        Map<String, String> headers = Map.of(
//                "FILE_LENGTH", String.valueOf(file.getContent().getSize()),
//                "LAST_MODIFIED", String.valueOf(file.getContent().getLastModifiedTime()),
//                "FILE_URI", file.getName().getURI(),
//                "FILE_PATH", file.getName().getPath(),
//                "FILE_NAME", file.getName().getBaseName()
//        );
//
//        Map<String, String> properties = Map.of(
//                "isInbound", "true",
//                "ARTIFACT_NAME", "inboundendpointfile",
//                "inboundEndpointName", "file",
//                "ClientApiNonBlocking", "true"
//        );
//
//        context.setHeaders(headers);
//        context.setProperties(properties);
//
//        mediator.mediateInboundMessage(config.getSequenceName(), context);
////        System.out.println(context.toString());
//
////        throw new Exception();
//    }
//
//    private void handleFileAction(FileObject file, String actionType) {
//        String actionKey = "transport.vfs.ActionAfter" + actionType;
//        String action = config.getParameters().get(actionKey);
//
//        if ("MOVE".equalsIgnoreCase(action)) {
//            String moveKey = "transport.vfs.MoveAfter" + actionType;
//            String movePath = config.getParameters().get(moveKey);
//            if (movePath == null || movePath.isEmpty()) {
//                System.err.println("No move path specified for " + actionType);
//                return;
//            }
//            System.out.println("hi");
//            moveFile(file, movePath);
//        } else {
//            deleteFile(file);
//        }
////        try {
////            Thread.sleep(10000);
////        } catch (InterruptedException e) {
////            throw new RuntimeException(e);
////        }
//    }
//
//    private void moveFile(FileObject file, String destinationUri) {
//        try {
//            FileObject destFolder = fsManager.resolveFile(destinationUri);
//
//            if (!destFolder.exists()) {
//                destFolder.createFolder();
//                System.out.println("Created destination folder: " + destFolder.getName().getURI());
//            }
//
//            FileObject destFile = destFolder.resolveFile(file.getName().getBaseName());
//            file.moveTo(destFile);
//            System.out.println("Moved file [" + file.getName().getURI() + "] to [" + destFile.getName().getURI() + "]");
////            Thread.sleep(10000);
//
//        } catch (Exception e) {
//            System.err.println("File move failed: " + e.getMessage());
//        }
//    }
//
//    private void deleteFile(FileObject file) {
//        try {
//            file.delete();
//            System.out.println("Deleted file: " + file.getName().getURI());
//        } catch (Exception e) {
//            System.err.println("File delete failed: " + e.getMessage());
//        }
//    }
//
//    private boolean tryLockFile(FileObject file) {
//        try {
//            String lockName = file.getName().getURI() + ".lock";
//            FileObject lockFile = fsManager.resolveFile(lockName);
//
//            if (lockFile.exists()) {
//                return false;
//            }
//            lockFile.createFile();
//            return true;
//        } catch (Exception e) {
//            System.err.println("Could not create lock for file [" + file.getName().getURI() + "]: " + e.getMessage());
//            return false;
//        }
//    }
//
//    private void releaseLockFile(FileObject file) {
//        try {
//            String lockName = file.getName().getURI() + ".lock";
//            FileObject lockFile = fsManager.resolveFile(lockName);
//            if (lockFile.exists()) {
//                lockFile.delete();
//            }
//        } catch (Exception e) {
//            System.err.println("Could not release lock: " + e.getMessage());
//        }
//    }
//
//    private boolean isSequentialProcessing() {
//        String seq = config.getParameters().get("sequential");
//        return seq != null && Boolean.parseBoolean(seq);
//    }
//
//    int getIntervalParameterValue() {
//        String intervalStr = config.getParameters().get("interval");
//        if (intervalStr != null && !intervalStr.isEmpty()) {
//            try {
//                return Integer.parseInt(intervalStr);
//            } catch (NumberFormatException ignored) {}
//        }
//        return 0;
//    }
//
//    private void validateConfig() throws Exception {
//        if (!"file".equalsIgnoreCase(config.getProtocol())) {
//            throw new Exception("Unsupported protocol, should be 'file'");
//        }
//        if (!config.getParameters().containsKey("interval")) {
//            throw new Exception("Missing 'interval' parameter");
//        }
//        String uri = config.getParameters().get("transport.vfs.FileURI");
//        if (uri == null || uri.isEmpty()) {
//            throw new Exception("Missing 'transport.vfs.FileURI' parameter");
//        }
//    }
//}
//
