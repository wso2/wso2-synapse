/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.transport.http.conn;

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests for {@link ReplayRecord}.
 */
public class ReplayRecordTest {

    @Test
    public void testAccessorsReturnConstructorValues() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("HTTP_METHOD", "POST");
        byte[] data = "payload".getBytes(StandardCharsets.UTF_8);

        ReplayRecord record = new ReplayRecord("urn:uuid:1", metadata, data);

        Assert.assertEquals("urn:uuid:1", record.getMessageId());
        Assert.assertEquals("POST", record.getMetadata().get("HTTP_METHOD"));
        Assert.assertArrayEquals(data, record.getData());
    }

    @Test
    public void testDataIsCopiedOnConstructionAndOnRead() {
        byte[] data = "abc".getBytes(StandardCharsets.UTF_8);
        ReplayRecord record = new ReplayRecord("id", null, data);

        data[0] = 'x';
        Assert.assertEquals("Changing the source array must not change the record", "abc",
                new String(record.getData(), StandardCharsets.UTF_8));

        byte[] read = record.getData();
        read[0] = 'y';
        Assert.assertEquals("Changing a returned array must not change the record", "abc",
                new String(record.getData(), StandardCharsets.UTF_8));
    }

    @Test
    public void testNullDataIsAllowed() {
        ReplayRecord record = new ReplayRecord("id", null, null);
        Assert.assertNull(record.getData());
    }

    @Test
    public void testNullMetadataBecomesEmptyMap() {
        ReplayRecord record = new ReplayRecord("id", null, new byte[0]);
        Assert.assertNotNull(record.getMetadata());
        Assert.assertTrue(record.getMetadata().isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testMetadataCannotBeModifiedThroughRecord() {
        ReplayRecord record = new ReplayRecord("id", new HashMap<>(), new byte[0]);
        record.getMetadata().put("key", "value");
    }
}
