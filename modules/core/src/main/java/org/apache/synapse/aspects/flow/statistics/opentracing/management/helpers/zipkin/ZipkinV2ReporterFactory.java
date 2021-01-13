/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.aspects.flow.statistics.opentracing.management.helpers.zipkin;

import io.jaegertracing.zipkin.ZipkinV2Reporter;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.urlconnection.URLConnectionSender;

/**
 * ZipkinV2ReporterFactory receives the Zipkin backend url and generate a Zipkin reporter.
 * This class is implemented for the purpose of prevent loading Zipkin related(dependencies) classes while,
 * initialising the MI, if Zipkin is disabled in the synapse.properties file.
 */
public class ZipkinV2ReporterFactory {

    private ZipkinV2Reporter reporter;

    public ZipkinV2ReporterFactory(String zipkinBackendURL) {
        reporter = new ZipkinV2Reporter(AsyncReporter.create(URLConnectionSender.create(zipkinBackendURL)));
    }

    public ZipkinV2Reporter getReporter() {
        return reporter;
    }
}