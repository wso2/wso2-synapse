/*
 * Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;

/**
 * This class is used to generate logs representing OpenTelemetry spans.
 */
public class LogExporter implements SpanExporter {

    private final Log log = LogFactory.getLog(TelemetryConstants.TRACER);
    private final JsonFactory jsonFactory = new JsonFactory();

    public static LogExporter create() {

        return new LogExporter();
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {

        Iterator iterator = spans.iterator();
        while (iterator.hasNext()) {
            String traceId = null;
            String spanId = null;
            SpanData span = (SpanData) iterator.next();
            try {
                StringWriter writer = new StringWriter();
                JsonGenerator generator = this.jsonFactory.createGenerator(writer);
                generator.writeStartObject();
                traceId = span.getTraceId();
                spanId = span.getSpanId();
                generator.writeStringField(TelemetryConstants.SPAN_ID, spanId);
                generator.writeStringField(TelemetryConstants.TRACE_ID, traceId);
                generator.writeStringField(TelemetryConstants.SPAN_NAME, span.getName());
                generator.writeStringField(TelemetryConstants.LATENCY,
                        ((int) (span.getEndEpochNanos() - span.getStartEpochNanos()) / 1000000) + "ms");
                generator.writeStringField(TelemetryConstants.ATTRIBUTES, String.valueOf(span.getAttributes()));
                generator.writeEndObject();
                generator.close();
                writer.close();
                log.trace(writer.toString());
            } catch (IOException e) {
                log.error("Error while structuring the log message when exporting Trace ID: " + traceId + ", Span ID:" +
                        " " + spanId, e);
            }
        }

        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {

        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {

        return CompletableResultCode.ofSuccess();
    }
}

