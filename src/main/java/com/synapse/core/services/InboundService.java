package com.synapse.core.services;

import com.synapse.core.ports.InboundEndpoint;
import com.synapse.core.ports.InboundMessageMediator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InboundService {
    private final InboundEndpoint inbound;
    private final ExecutorService executorService;

    public InboundService(InboundEndpoint inbound) {
        this.inbound = inbound;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void start(InboundMessageMediator inboundMessageMediator) {
        executorService.submit(() -> {
            try {
                inbound.start(inboundMessageMediator);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void stop() {
        executorService.submit(() -> {
            try {
                inbound.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
