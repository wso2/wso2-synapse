# File Inbound Endpoint Implementation

The `FileInboundEndpoint` is a critical component that polls files from a directory, processes them, and then performs post-processing actions (e.g., moving or deleting the files).

## Key Features

1. **Periodic Polling**: Uses a scheduler to poll files at configurable intervals.
2. **File Filtering**: Supports filtering files based on name patterns.
3. **Sequential/Parallel Processing**: Can process files sequentially or in parallel.
4. **File Locking**: Implements a locking mechanism to prevent concurrent processing of the same file.
5. **Post-processing Actions**: Supports moving or deleting files after processing.

## Core Implementation

### File Polling Logic

```java
private void pollFiles(FileObject folder) throws FileSystemException {
    if (!folder.exists()) return;

    String patternStr = config.getParameters().getOrDefault("transport.vfs.FileNamePattern", ".*.");
    Pattern filePattern = Pattern.compile(patternStr);

    FileObject[] children = folder.getChildren(); // Retrieves all files in the directory.

    for (FileObject file : children) {
        if (file.getType() == FileType.FILE) {
            Matcher matcher = filePattern.matcher(file.getName().getBaseName());
            if (matcher.matches()) {
                if (isSequentialProcessing()) {
                    handleFile(file); // Processes the file sequentially.
                } else {
                    Thread.ofVirtual().start(() -> handleFile(file)); // Processes the file in parallel.
                }
            }
        }
    }
}
```

File Processing

```java
private void handleFile(FileObject file) {
    String filePath = file.getName().getURI();
    if (!processingFiles.add(filePath)) return; // Ensures the file is not already being processed.

    try {
        if (!tryLockFile(file)) return; // Locks the file to prevent concurrent processing.

        processSingleFile(file); // Reads the file content and passes it to the mediator.

        handleFileAction(file, "Process"); // Moves or deletes the file after processing.
    } catch (Exception e) {
        handleFileAction(file, "Failure"); // Handles failures (e.g., moves the file to a failure directory).
    } finally {
        releaseLockFile(file); // Releases the file lock.
        processingFiles.remove(filePath);
    }
}
```

Message Context Creation

```java
private void processSingleFile(FileObject file) throws Exception {
    byte[] content;
    try (InputStream inputStream = file.getContent().getInputStream()) {
        content = inputStream.readAllBytes(); // Reads the file content.
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

    context.setHeaders(headers);

    mediator.mediateInboundMessage(config.getSequenceName(), context); // Passes the message to the mediator.
}
```

<!-- Configuration Parameters
Parameter	Description	Default
interval	Polling interval in milliseconds	5000
sequential	Whether to process files sequentially	true
transport.vfs.FileURI	URI to the directory to poll	Required
transport.vfs.FileNamePattern	Regex pattern for file names	.*
transport.vfs.ActionAfterProcess	Action after processing (MOVE, DELETE)	None
transport.vfs.MoveAfterProcess	Directory to move files to after processing	None
transport.vfs.ActionAfterFailure	Action after failure (MOVE, DELETE)	None
transport.vfs.MoveAfterFailure	Directory to move files to after failure	None
Implementation Considerations
Error Handling: The implementation includes comprehensive error handling to ensure that failures don't stop the polling process.
Performance: Using virtual threads allows for efficient parallel processing of files.
Resource Management: All file resources are properly closed using try-with-resources.
Concurrency: A concurrent set tracks which files are being processed to prevent duplicate processing. -->

### Configuration Parameters

| Parameter                         | Description                                          | Default   |
|----------------------------------|------------------------------------------------------|-----------|
| `interval`                       | Polling interval in milliseconds                     | `5000`    |
| `sequential`                     | Whether to process files sequentially                | `true`    |
| `transport.vfs.FileURI`          | URI to the directory to poll                         | **Required** |
| `transport.vfs.FileNamePattern`  | Regex pattern for file names                         | `.*`      |
| `transport.vfs.ActionAfterProcess` | Action after processing (`MOVE`, `DELETE`)          | `DELETE`    |
| `transport.vfs.MoveAfterProcess` | Directory to move files to after processing          | `None`    |
| `transport.vfs.ActionAfterFailure` | Action after failure (`MOVE`, `DELETE`)             | `DELETE`    |
| `transport.vfs.MoveAfterFailure` | Directory to move files to after failure             | `None`    |

### Implementation Considerations

- **Error Handling**: The implementation includes comprehensive error handling to ensure that failures don't stop the polling process.  
- **Performance**: Using virtual threads allows for efficient parallel processing of files.  
- **Resource Management**: All file resources are properly closed using `try-with-resources`.  
- **Concurrency**: A concurrent set tracks which files are being processed to prevent duplicate processing.
