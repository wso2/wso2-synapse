/*
 *  Copyright (c) 2025, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
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

package org.apache.synapse;

import junit.framework.TestCase;
import org.apache.synapse.mediators.TestUtils;

/**
 * Unit tests for FaultHandler.handleFault() robustness.
 *
 * Regression test for issue #4165: PathNotFoundException.getMessage() returns null, causing a
 * secondary NullPointerException in FaultHandler.handleFault() at the getMessage().split() call.
 */
public class FaultHandlerNullMessageTest extends TestCase {

    /**
     * Concrete no-op FaultHandler for testing.
     */
    private static class NoOpFaultHandler extends FaultHandler {
        @Override
        public void onFault(MessageContext synCtx) {
            // no-op: do not rethrow or forward
        }
    }

    /**
     * Verifies that handleFault does NOT throw a NullPointerException when the supplied
     * exception has a null message (e.g. PathNotFoundException from Jayway JsonPath).
     * Before the fix, e.getMessage().split("\n") would throw NPE.
     */
    public void testHandleFaultWithNullExceptionMessage() throws Exception {

        MessageContext synCtx = TestUtils.createLightweightSynapseMessageContext("<empty/>");

        FaultHandler handler = new NoOpFaultHandler();

        // An exception whose getMessage() returns null — matches the PathNotFoundException case
        Exception nullMessageException = new RuntimeException((String) null);
        assertNull("Precondition: exception message must be null", nullMessageException.getMessage());

        // Must not throw NPE
        handler.handleFault(synCtx, nullMessageException);

        // ERROR_CODE must be set to the default value
        assertNotNull(synCtx.getProperty(SynapseConstants.ERROR_CODE));

        // ERROR_MESSAGE must be an empty string, not null
        Object errorMessage = synCtx.getProperty(SynapseConstants.ERROR_MESSAGE);
        assertNotNull("ERROR_MESSAGE must not be null when exception message is null", errorMessage);
        assertEquals("ERROR_MESSAGE must be empty string when exception message is null", "", errorMessage);
    }

    /**
     * Verifies that handleFault correctly extracts the first line of a multi-line exception message.
     * This is the normal (pre-existing) behaviour that must continue to work after the null-guard fix.
     */
    public void testHandleFaultWithMultilineExceptionMessage() throws Exception {

        MessageContext synCtx = TestUtils.createLightweightSynapseMessageContext("<empty/>");

        FaultHandler handler = new NoOpFaultHandler();

        Exception multilineException = new RuntimeException("First line\nSecond line\nThird line");

        handler.handleFault(synCtx, multilineException);

        Object errorMessage = synCtx.getProperty(SynapseConstants.ERROR_MESSAGE);
        assertEquals("ERROR_MESSAGE must contain only the first line", "First line", errorMessage);
    }

    /**
     * Verifies that handleFault with a non-null, single-line message continues to work as expected.
     */
    public void testHandleFaultWithNormalExceptionMessage() throws Exception {

        MessageContext synCtx = TestUtils.createLightweightSynapseMessageContext("<empty/>");

        FaultHandler handler = new NoOpFaultHandler();

        Exception normalException = new RuntimeException("Something went wrong");

        handler.handleFault(synCtx, normalException);

        assertEquals("Something went wrong", synCtx.getProperty(SynapseConstants.ERROR_MESSAGE));
    }
}
