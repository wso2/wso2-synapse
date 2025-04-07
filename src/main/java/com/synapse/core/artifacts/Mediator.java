package com.synapse.core.artifacts;

import com.synapse.core.synctx.MsgContext;

public interface Mediator {
    boolean execute(MsgContext context) throws Exception;
}
