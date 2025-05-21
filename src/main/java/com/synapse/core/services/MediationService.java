package com.synapse.core.services;

import com.synapse.core.ports.InboundMessageMediator;
import com.synapse.core.synctx.MsgContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MediationService {
    private final InboundMessageMediator inboundMessageMediator;
    private final ExecutorService executorService;

    public MediationService(InboundMessageMediator inboundMessageMediator) {
        this.inboundMessageMediator = inboundMessageMediator;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void mediateInboundMessage(String seqName, MsgContext msgContext) {
        executorService.submit(() -> {
            try {
                inboundMessageMediator.mediateInboundMessage(seqName, msgContext);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
