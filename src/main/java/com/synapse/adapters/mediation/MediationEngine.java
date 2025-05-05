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

package com.synapse.adapters.mediation;

import com.synapse.core.artifacts.ConfigContext;
import com.synapse.core.ports.InboundMessageMediator;
import com.synapse.core.synctx.MsgContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MediationEngine implements InboundMessageMediator {
    private final ConfigContext configContext;
    private final ExecutorService executorService;

    private static final Logger log = LogManager.getLogger(MediationEngine.class);

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
                    log.error("Sequence {} not found", seqName);
                    return;
                }

                sequence.execute(msgContext);
            } catch (Exception e) {
                log.error("Error executing sequence {}", seqName, e);
            }
        });
    }
}
