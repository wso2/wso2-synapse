package com.synapse.adapters.mediation;

import com.synapse.core.artifacts.ConfigContext;
import com.synapse.core.ports.InboundMessageMediator;
import com.synapse.core.synctx.MsgContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MediationEngine implements InboundMessageMediator {
    private final ConfigContext configContext;
    private final ExecutorService executorService;

    public MediationEngine(ConfigContext configContext) {
        this.configContext = configContext;
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public void mediateInboundMessage(String seqName, MsgContext msgContext) {
        executorService.submit(() -> {
            try {

                var sequence = configContext.getSequenceMap().get(seqName);
                if (sequence == null) {
                    System.out.println("Sequence " + seqName + " not found");
                    return;
                }

                sequence.execute(msgContext);
            } catch (Exception e) {
                System.out.println("Error during message mediation: " + e.getMessage());
            }
        });
    }
}
