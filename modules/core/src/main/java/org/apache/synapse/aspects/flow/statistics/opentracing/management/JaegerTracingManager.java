/*
 *  Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.aspects.flow.statistics.opentracing.management;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.JaegerTracer;
import org.apache.synapse.aspects.flow.statistics.opentracing.management.handling.span.JaegerSpanHandler;
import org.apache.synapse.aspects.flow.statistics.opentracing.management.scoping.TracingScopeManager;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Coordinates the Jaeger span handler with the tracer.
 */
public class JaegerTracingManager implements OpenTracingManager {

    private static final String SERVICE_NAME = "wso2-synapse";

    /**
     * The common tracer object.
     */
    private JaegerTracer tracer;

    /**
     * Controls Jaeger spans.
     */
    private JaegerSpanHandler handler;

    public JaegerTracingManager(Configuration.SamplerConfiguration sampler,
                                Configuration.ReporterConfiguration reporter) {
        initializeTracer(sampler, reporter);
        resolveHandler();
    }

    /**
     * Initializes the tracer object.
     *
     * @param sampler  Jaeger sampler configuration.
     * @param reporter Jaeger reporter configuration.
     */
    private void initializeTracer(Configuration.SamplerConfiguration sampler,
                                  Configuration.ReporterConfiguration reporter) {
        String serviceName = getServiceName();
        this.tracer = new Configuration(serviceName)
                .withSampler(sampler)
                .withReporter(reporter)
                .getTracer();
    }

    @Override
    public void resolveHandler() {
        this.handler = new JaegerSpanHandler(tracer, new TracingScopeManager());
    }

    @Override
    public JaegerSpanHandler getHandler() {
        return this.handler;
    }

    private static String getServiceName() {
        return SERVICE_NAME;
    }
}
