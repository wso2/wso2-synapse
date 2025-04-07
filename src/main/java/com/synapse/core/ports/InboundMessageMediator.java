package com.synapse.core.ports;

import com.synapse.core.synctx.MsgContext;

public interface InboundMessageMediator {
    void mediateInboundMessage(String seqName, MsgContext msg) throws Exception;
}
