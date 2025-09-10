package org.apache.synapse.inbound;

public class DynamicControllOperationResult {
    private final boolean success;
    private final String message;

    public DynamicControllOperationResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
