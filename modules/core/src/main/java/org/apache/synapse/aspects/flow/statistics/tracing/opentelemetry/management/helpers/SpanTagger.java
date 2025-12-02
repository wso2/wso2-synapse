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

package org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.helpers;

import io.opentelemetry.api.trace.Span;
import org.apache.synapse.MessageContext;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticDataUnit;
import org.apache.synapse.aspects.flow.statistics.data.raw.StatisticsLog;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.OpenTelemetryManagerHolder;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.management.TelemetryConstants;
import org.apache.synapse.aspects.flow.statistics.tracing.opentelemetry.models.SpanWrapper;
import org.apache.synapse.commons.CorrelationConstants;

/**
 * Applies tags to Spans.
 */
public class SpanTagger {

    /**
     * Prevents Instantiation.
     */
    private SpanTagger() {}

    /**
     * Sets tags to the span which is contained in the provided span wrapper, from information acquired from the
     * given basic statistic data unit.
     * @param spanWrapper               Span wrapper that contains the target span.
     * @param synCtx Synapse message context
     */
    public static void setSpanTags(SpanWrapper spanWrapper, MessageContext synCtx) {
        StatisticsLog openStatisticsLog = new StatisticsLog(spanWrapper.getStatisticDataUnit());
        Span span = spanWrapper.getSpan();
        if (OpenTelemetryManagerHolder.isCollectingPayloads() || OpenTelemetryManagerHolder.isCollectingProperties()
                || OpenTelemetryManagerHolder.isCollectingVariables()) {
            if (OpenTelemetryManagerHolder.isCollectingPayloads()) {
                if(openStatisticsLog.getBeforePayload() != null) {
                    span.setAttribute(TelemetryConstants.BEFORE_PAYLOAD_ATTRIBUTE_KEY,
                            openStatisticsLog.getBeforePayload());
                }
                StatisticDataUnit statisticDataUnit = spanWrapper.getCloseEventStatisticDataUnit();
                if (statisticDataUnit != null) {
                    if (statisticDataUnit.getPayload() != null) {
                        span.setAttribute(TelemetryConstants.AFTER_PAYLOAD_ATTRIBUTE_KEY,
                                statisticDataUnit.getPayload());
                    }
                } else if (openStatisticsLog.getBeforePayload() !=null){
                    //This means a close event hasn't been triggered so payload is equal to before payload
                    span.setAttribute(TelemetryConstants.AFTER_PAYLOAD_ATTRIBUTE_KEY,
                            openStatisticsLog.getBeforePayload());
                }
            }

            if (OpenTelemetryManagerHolder.isCollectingProperties()) {
                if (spanWrapper.getStatisticDataUnit().getContextPropertyMap() != null) {
                    span.setAttribute(TelemetryConstants.BEFORE_CONTEXT_PROPERTY_MAP_ATTRIBUTE_KEY,
                            spanWrapper.getStatisticDataUnit().getContextPropertyMap().toString());
                }
                if (spanWrapper.getCloseEventStatisticDataUnit() != null) {
                    if (spanWrapper.getCloseEventStatisticDataUnit().getContextPropertyMap() != null) {
                        span.setAttribute(TelemetryConstants.AFTER_CONTEXT_PROPERTY_MAP_ATTRIBUTE_KEY,
                                spanWrapper.getCloseEventStatisticDataUnit().getContextPropertyMap().toString());
                    }
                } else if (openStatisticsLog.getContextPropertyMap() != null) {
                    span.setAttribute(TelemetryConstants.AFTER_CONTEXT_PROPERTY_MAP_ATTRIBUTE_KEY,
                            openStatisticsLog.getContextPropertyMap().toString());
                }
                if (spanWrapper.getCloseEventStatisticDataUnit() != null &&
                        spanWrapper.getCloseEventStatisticDataUnit().getPropertyValue() != null) {
                    span.setAttribute(TelemetryConstants.PROPERTY_MEDIATOR_VALUE_ATTRIBUTE_KEY,
                            spanWrapper.getCloseEventStatisticDataUnit().getPropertyValue());
                }
            }

            if (OpenTelemetryManagerHolder.isCollectingVariables()) {
                if (spanWrapper.getStatisticDataUnit().getContextVariableMap() != null) {
                    span.setAttribute(TelemetryConstants.BEFORE_CONTEXT_VARIABLE_MAP_ATTRIBUTE_KEY,
                            spanWrapper.getStatisticDataUnit().getContextVariableMap().toString());
                }
                if (spanWrapper.getCloseEventStatisticDataUnit() != null) {
                    if (spanWrapper.getCloseEventStatisticDataUnit().getContextVariableMap() != null) {
                        span.setAttribute(TelemetryConstants.AFTER_CONTEXT_VARIABLE_MAP_ATTRIBUTE_KEY,
                                spanWrapper.getCloseEventStatisticDataUnit().getContextVariableMap().toString());
                    }
                } else if (openStatisticsLog.getContextVariableMap() != null) {
                    span.setAttribute(TelemetryConstants.AFTER_CONTEXT_VARIABLE_MAP_ATTRIBUTE_KEY,
                            openStatisticsLog.getContextVariableMap().toString());
                }
            }
        }
        if (openStatisticsLog.getComponentName() != null) {
            span.setAttribute(TelemetryConstants.COMPONENT_NAME_ATTRIBUTE_KEY, openStatisticsLog.getComponentName());
        }
        if(openStatisticsLog.getComponentTypeToString() != null){
            span.setAttribute(TelemetryConstants.COMPONENT_TYPE_ATTRIBUTE_KEY,
                    openStatisticsLog.getComponentTypeToString());
        }
        span.setAttribute(TelemetryConstants.THREAD_ID_ATTRIBUTE_KEY, Thread.currentThread().getId());
        if(openStatisticsLog.getComponentId() != null){
            span.setAttribute(TelemetryConstants.COMPONENT_ID_ATTRIBUTE_KEY, openStatisticsLog.getComponentId());
        }
        if(openStatisticsLog.getHashCode() != null){
            span.setAttribute(TelemetryConstants.HASHCODE_ATTRIBUTE_KEY, openStatisticsLog.getHashCode());
        }
        if (openStatisticsLog.getTransportHeaderMap() != null) {
            span.setAttribute(TelemetryConstants.TRANSPORT_HEADERS_ATTRIBUTE_KEY,
                    openStatisticsLog.getTransportHeaderMap().toString());
        }

        if (openStatisticsLog.getStatusCode() != null) {
            span.setAttribute(TelemetryConstants.STATUS_CODE_ATTRIBUTE_KEY, openStatisticsLog.getStatusCode());
        }
        if (openStatisticsLog.getStatusDescription() != null) {
            span.setAttribute(TelemetryConstants.STATUS_DESCRIPTION_ATTRIBUTE_KEY,
                    openStatisticsLog.getStatusDescription());
        }
        if (openStatisticsLog.getEndpoint() != null) {
            span.setAttribute(TelemetryConstants.ENDPOINT_ATTRIBUTE_KEY,
                    String.valueOf(openStatisticsLog.getEndpoint().getJsonRepresentation()));
        }
        if (synCtx.getProperty(CorrelationConstants.CORRELATION_ID) != null) {
            span.setAttribute(TelemetryConstants.CORRELATION_ID_ATTRIBUTE_KEY,
                    synCtx.getProperty(CorrelationConstants.CORRELATION_ID).toString());
        }

        if (openStatisticsLog.getCustomProperties() != null) {
            openStatisticsLog.getCustomProperties().forEach(
                    (key, value) -> span.setAttribute(key, String.valueOf(value))
            );
        }
        if (spanWrapper.getCloseEventStatisticDataUnit() != null) {
            if (spanWrapper.getCloseEventStatisticDataUnit().getCustomProperties() != null) {
                spanWrapper.getCloseEventStatisticDataUnit().getCustomProperties().forEach(
                        (key, value) -> span.setAttribute(key, String.valueOf(value))
                );
            }
        }
    }

    /**
     * Sets tags to the span which is contained in the provided span wrapper, from information acquired from the
     * given basic statistic data unit.
     *
     * @param spanWrapper Span wrapper that contains the target span.
     * @param msgCtx      Axis2 message context
     */
    public static void setSpanTags(SpanWrapper spanWrapper, org.apache.axis2.context.MessageContext msgCtx) {
        StatisticDataUnit openEventStatisticDataUnit = spanWrapper.getStatisticDataUnit();
        Span span = spanWrapper.getSpan();
        StatisticDataUnit closeEventStatisticDataUnit = spanWrapper.getCloseEventStatisticDataUnit();
        if (OpenTelemetryManagerHolder.isCollectingPayloads()) {
            if (openEventStatisticDataUnit.getPayload() != null) {
                span.setAttribute(TelemetryConstants.BEFORE_PAYLOAD_ATTRIBUTE_KEY,
                        openEventStatisticDataUnit.getPayload());
            }
            if (closeEventStatisticDataUnit != null) {
                if (closeEventStatisticDataUnit.getPayload() != null) {
                    span.setAttribute(TelemetryConstants.AFTER_PAYLOAD_ATTRIBUTE_KEY,
                            closeEventStatisticDataUnit.getPayload());
                }
            }
        }

        if (openEventStatisticDataUnit.getComponentName() != null) {
            span.setAttribute(TelemetryConstants.COMPONENT_NAME_ATTRIBUTE_KEY,
                    openEventStatisticDataUnit.getComponentName());
        }

        if (openEventStatisticDataUnit.getComponentType() != null) {
            span.setAttribute(TelemetryConstants.COMPONENT_TYPE_ATTRIBUTE_KEY,
                    openEventStatisticDataUnit.getComponentType().toString());
        } else if (openEventStatisticDataUnit.getComponentTypeString() != null) {
            span.setAttribute(TelemetryConstants.COMPONENT_TYPE_ATTRIBUTE_KEY,
                    openEventStatisticDataUnit.getComponentTypeString());
        }

        span.setAttribute(TelemetryConstants.THREAD_ID_ATTRIBUTE_KEY, Thread.currentThread().getId());
        if (openEventStatisticDataUnit.getComponentId() != null) {
            span.setAttribute(TelemetryConstants.COMPONENT_ID_ATTRIBUTE_KEY,
                    openEventStatisticDataUnit.getComponentId());
        }
        if (openEventStatisticDataUnit.getHashCode() != null) {
            span.setAttribute(TelemetryConstants.HASHCODE_ATTRIBUTE_KEY, openEventStatisticDataUnit.getHashCode());
        }
        if (openEventStatisticDataUnit.getTransportHeaderMap() != null) {
            span.setAttribute(TelemetryConstants.TRANSPORT_HEADERS_ATTRIBUTE_KEY,
                    openEventStatisticDataUnit.getTransportHeaderMap().toString());
        }

        if (openEventStatisticDataUnit.getStatusCode() != null) {
            span.setAttribute(TelemetryConstants.STATUS_CODE_ATTRIBUTE_KEY, openEventStatisticDataUnit.getStatusCode());
        }
        if (openEventStatisticDataUnit.getStatusDescription() != null) {
            span.setAttribute(TelemetryConstants.STATUS_DESCRIPTION_ATTRIBUTE_KEY,
                    openEventStatisticDataUnit.getStatusDescription());
        }
        if (msgCtx.getProperty(CorrelationConstants.CORRELATION_ID) != null) {
            span.setAttribute(TelemetryConstants.CORRELATION_ID_ATTRIBUTE_KEY,
                    msgCtx.getProperty(CorrelationConstants.CORRELATION_ID).toString());
        }

        if (openEventStatisticDataUnit.getCustomProperties() != null) {
            openEventStatisticDataUnit.getCustomProperties().forEach(
                    (key, value) -> span.setAttribute(key, String.valueOf(value))
            );
        }

        if (closeEventStatisticDataUnit != null) {
            if (closeEventStatisticDataUnit.getErrorCode() != null) {
                span.setAttribute(TelemetryConstants.ERROR_CODE_ATTRIBUTE_KEY,
                        closeEventStatisticDataUnit.getErrorCode());
            }

            if (closeEventStatisticDataUnit.getErrorMessage() != null) {
                span.setAttribute(TelemetryConstants.ERROR_MESSAGE_ATTRIBUTE_KEY,
                        closeEventStatisticDataUnit.getErrorMessage());
            }

            if (closeEventStatisticDataUnit.getCustomProperties() != null) {
                closeEventStatisticDataUnit.getCustomProperties().forEach(
                        (key, value) -> span.setAttribute(key, String.valueOf(value))
                );
            }
        }

    }
}
