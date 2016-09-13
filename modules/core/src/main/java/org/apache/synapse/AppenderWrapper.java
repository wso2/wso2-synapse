/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.synapse.transport.customlogsetter.CustomLogSetter;

import java.lang.reflect.Field;

public class AppenderWrapper extends PatternLayout {

    private static final Log log = LogFactory.getLog(AppenderWrapper.class);

    public AppenderWrapper() {
        super();
    }

    public AppenderWrapper(String pattern) {
        super(pattern);
    }

    @Override
    public String format(LoggingEvent event) {
        if (event.getMessage() != null && event.getMessage() instanceof String) {
            String logMessage = event.getMessage().toString();
            logMessage = StringUtils.trim(
                    ((CustomLogSetter.getInstance().getLogAppenderContent() != null) ?
                            CustomLogSetter.getInstance().getLogAppenderContent() : "") + logMessage);
            try {
                Field field = LoggingEvent.class.getDeclaredField("message");
                field.setAccessible(true);
                field.set(event, logMessage);
            } catch (Exception ex) {
                log.error("Error Setting the Logging Event: " + ex);
            }
        }
        return super.format(event);
    }

}
