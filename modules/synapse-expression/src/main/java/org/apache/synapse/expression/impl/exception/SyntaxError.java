/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.synapse.expression.impl.exception;

import org.antlr.v4.runtime.RecognitionException;
/**
 * Represents an exception that occurs during the parsing of Synapse Expressions.
 */
public class SyntaxError {
    private final int line;
    private final int charPositionInLine;
    private final String message;
    private final Object offendingSymbol;
    private final RecognitionException exception;

    public SyntaxError(int line, int charPositionInLine, String message, Object offendingSymbol, RecognitionException exception) {
        this.line = line;
        this.charPositionInLine = charPositionInLine;
        this.message = message;
        this.offendingSymbol = offendingSymbol;
        this.exception = exception;
    }

    public int getLine() {
        return line;
    }

    public int getCharPositionInLine() {
        return charPositionInLine;
    }

    public String getMessage() {
        return message;
    }

    public Object getOffendingSymbol() {
        return offendingSymbol;
    }

    public RecognitionException getException() {
        return exception;
    }

    @Override
    public String toString() {
        return "SyntaxError{" +
                "line=" + line +
                ", charPositionInLine=" + charPositionInLine +
                ", message='" + message + '\'' +
                ", offendingSymbol=" + offendingSymbol +
                ", exception=" + (exception != null ? exception.getClass().getSimpleName() : "None") +
                '}';
    }
}
