package com.synapse.core.services;

import com.synapse.core.ports.InboundMessageMediator;
import com.synapse.core.synctx.MsgContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MediationService {
    private final InboundMessageMediator inboundMediationService;
    private final ExecutorService executorService;

    public MediationService(InboundMessageMediator inboundMediationService) {
        this.inboundMediationService = inboundMediationService;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void mediateInboundMessage(String seqName, MsgContext msgContext) {
        executorService.submit(() -> {
            try {
                inboundMediationService.mediateInboundMessage(seqName, msgContext);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
