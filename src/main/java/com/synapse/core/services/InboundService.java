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
