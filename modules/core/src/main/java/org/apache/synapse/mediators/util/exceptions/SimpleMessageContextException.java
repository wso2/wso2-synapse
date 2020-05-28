/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.apache.synapse.mediators.util.exceptions;

/**
 * RuntimeException representing a exception happen in processing the SimpleMessageContext. This can be thrown by the
 * payload processors and data transformers.
 * This exception can be used to stop the mediator process return false as the response for mediation flow.
 */
public class SimpleMessageContextException extends RuntimeException {

    public SimpleMessageContextException(Throwable cause) {

        super(cause);
    }

    public SimpleMessageContextException(String message) {

        super(message);
    }

    public SimpleMessageContextException(String message, Throwable cause) {

        super(message, cause);
    }
}
