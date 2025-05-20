/*
 * Copyright (c) 2025, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.synapse.adapters.inbound;

import com.synapse.core.domain.InboundConfig;
import com.synapse.core.ports.InboundEndpoint;
import com.synapse.core.ports.InboundMessageMediator;
import com.synapse.core.synctx.Message;
import com.synapse.core.synctx.MsgContext;
import org.apache.commons.vfs2.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileInboundEndpoint implements InboundEndpoint {

    private static final Logger logger = LogManager.getLogger(FileInboundEndpoint.class);

    private final InboundConfig config;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
//    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
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

        scheduler.scheduleAtFixedRate(() -> {
            if (!isRunning.get()) {
                logger.debug("isRunning is false");
                return;
            }

            try {
                Thread.ofVirtual().start(() -> {
                    try {

                        String fileUri = config.getParameters().get("transport.vfs.FileURI");
                        FileObject folder = fsManager.resolveFile(fileUri);
                        logger.debug("Submitting pollfiles");
                        pollFiles(folder);
                        logger.debug("Finishing pollfiles");

                    } catch (Exception e) {
                        logger.error("Error in pollFiles: {}", e.getMessage());
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                System.err.println("Error launching virtual thread: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, interval, TimeUnit.MILLISECONDS);

    }

    @Override
    public void stop() throws Exception {

        logger.info("Stopping file inbound endpoint: {}", config.getName());
        isRunning.set(false);
//        boolean finished = scheduler.awaitTermination(60, TimeUnit.SECONDS);
        scheduler.shutdown();
        logger.info("Stopped file inbound endpoint: {}", config.getName());
    }

    private void pollFiles(FileObject folder) throws FileSystemException {
        if (!folder.exists()) {
            logger.error("Invalid folder URI: {}", folder.getPublicURIString());
            return;
        }

        String patternStr = config.getParameters().getOrDefault("transport.vfs.FileNamePattern", ".*.");
        Pattern filePattern = Pattern.compile(patternStr);

        FileObject[] children = null;

        if (folder.getType() == FileType.FOLDER) {
            children = folder.getChildren();
        }

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

        String filePath = file.getName().getURI();
        if (!processingFiles.add(filePath)) {
            logger.debug("Skipping already-processing file: {}", filePath);
            return;
        }
        try {
            if (!tryLockFile(file)) {
                logger.debug("File locked, skipping: {}", filePath);
                return;
            }

            processSingleFile(file);

            logger.debug("File processed: {}", filePath);
            handleFileAction(file, "Process");
        } catch (Exception e) {
            logger.error("Error processing file [{}]: {}", filePath, e.getMessage());
            handleFileAction(file, "Failure");
        } finally {
            releaseLockFile(file);
            processingFiles.remove(filePath);
        }
    }

    private void processSingleFile(FileObject file) throws Exception {
        logger.debug("Processing file: {}", file.getName().getBaseName());
        byte[] content;
        try (InputStream inputStream = file.getContent().getInputStream()) {
            content = inputStream.readAllBytes();
        }

        Thread.sleep(10000);

        MsgContext context = new MsgContext();
        String contentType = config.getParameters().getOrDefault("transport.vfs.ContentType", "text/plain");
        Message msg = new Message(content, contentType);
        context.setMessage(msg);

//        Thread.sleep(100);

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

    }

    private void handleFileAction(FileObject file, String actionType) {
        String actionKey = "transport.vfs.ActionAfter" + actionType;
        String action = config.getParameters().get(actionKey);

        if ("MOVE".equalsIgnoreCase(action)) {
            String moveKey = "transport.vfs.MoveAfter" + actionType;
            String movePath = config.getParameters().get(moveKey);
            if (movePath == null || movePath.isEmpty()) {
                logger.error("No move path specified for {}", actionType);
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
                logger.debug("Created destination folder: {}", destFolder.getName().getURI());
            }

            FileObject destFile = destFolder.resolveFile(file.getName().getBaseName());

            file.moveTo(destFile);

            logger.debug("Moved file [{}] to [{}]", file.getName().getURI(), destFile.getName().getURI());

        } catch (Exception e) {
            System.err.println("File move failed: " + e.getMessage());
        }
    }

    private void deleteFile(FileObject file) {
        try {
            file.delete();
            logger.debug("Deleted file: {}", file.getName().getURI());
        } catch (Exception e) {
            logger.error("File delete failed: {}", e.getMessage());
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
            logger.error("Could not create lock for file [{}]: {}", file.getName().getURI(), e.getMessage());
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
            logger.error("Could not release lock for file [{}]: {}", file.getName().getURI(), e.getMessage());
        }
    }

    private boolean isSequentialProcessing() {
        String seq = config.getParameters().get("sequential");
        return Boolean.parseBoolean(seq);
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
