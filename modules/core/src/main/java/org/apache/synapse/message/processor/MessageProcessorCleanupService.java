package org.apache.synapse.message.processor;

import java.io.Serializable;
import java.util.concurrent.Callable;

public interface MessageProcessorCleanupService extends Callable<Void>, Serializable {
    void setName(final String name);

}
