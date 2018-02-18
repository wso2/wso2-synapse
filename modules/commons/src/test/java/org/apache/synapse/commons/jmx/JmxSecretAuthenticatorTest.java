/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.commons.jmx;

import junit.framework.TestCase;
import org.wso2.securevault.secret.SecretInformation;
import javax.security.auth.Subject;
import java.util.Properties;

/**
 * Test cases for JmxSecretAuthenticator
 */
public class JmxSecretAuthenticatorTest extends TestCase {

    /**
     * Test authenticate with correct credentials
     */
    public void testAuthenticate() {
        JmxSecretAuthenticator secretAuthenticator = createSecretAuthenticator();
        Subject subject = secretAuthenticator.authenticate(new String[]{"user1", "password1"});
        assertNotNull("Subject expected", subject);
    }

    /**
     * Test authenticate with incorrect credentials
     */
    public void testAuthenticateWithIncorrectCredentials() {
        JmxSecretAuthenticator secretAuthenticator = createSecretAuthenticator();
        try {
            secretAuthenticator.authenticate(new String[]{"user2", "password2"});
            fail("SecurityException expected");
        } catch (SecurityException e) {
            assertEquals("Incorrect SecurityException message",
                    "Username and/or password are incorrect, or you do not have the necessary access rights.",
                    e.getMessage());
        }
    }

    /**
     * Test authenticate with null credentials
     */
    public void testAuthenticateWithNullCredentials() {
        JmxSecretAuthenticator secretAuthenticator = createSecretAuthenticator();
        try {
            secretAuthenticator.authenticate(null);
            fail("SecurityException expected");
        } catch (SecurityException e) {
            assertEquals("Incorrect SecurityException message", "Credentials required", e.getMessage());
        }
    }

    /**
     * Helper method to create a @{@link JmxSecretAuthenticator}
     * @return
     */
    private JmxSecretAuthenticator createSecretAuthenticator() {
        Properties properties = new Properties();
        properties.put("synapse.jmx.username", "user1");
        properties.put("synapse.jmx.password", "password1");
        JmxInformation jmxInformation = JmxInformationFactory.createJmxInformation(properties, "localhost");
        SecretInformation secretInformation = jmxInformation.getSecretInformation();
        return new JmxSecretAuthenticator(secretInformation);
    }
}
