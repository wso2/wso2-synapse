/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.apache.synapse.transport.passthru.connections;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.protocol.HttpContext;
import org.apache.synapse.transport.passthru.PassThroughConstants;
import org.apache.synapse.transport.passthru.config.ConnectionTimeoutConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.support.membermodification.MemberModifier;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.times;

@RunWith(DataProviderRunner.class)
public class HostConnectionsTest {

    @DataProvider
    public static Object[][] data() {

        return new Object[][]{
                {10, 20, 3},
                {20, 25, 5},
        };
    }

    @Test
    @UseDataProvider("data")
    public void testGetConnection(final int connectionIdleTime, final int maximumConnectionLifeSpan,
                                  final int connectionGraceTime) throws Exception {

        NHttpClientConnection nHttpClientConnection = Mockito.mock(NHttpClientConnection.class);
        List<NHttpClientConnection> freeConnections = new ArrayList<>();
        freeConnections.add(nHttpClientConnection);
        ConnectionTimeoutConfiguration conf = new ConnectionTimeoutConfiguration(connectionIdleTime,
                maximumConnectionLifeSpan, connectionGraceTime);
        HostConnections hostConnections = new HostConnections(null, 1, conf);
        MemberModifier.field(HostConnections.class, "freeConnections").set(hostConnections, freeConnections);
        long currentTime = System.currentTimeMillis();
        Mockito.when((nHttpClientConnection.getContext())).thenReturn(Mockito.mock(HttpContext.class));
        Mockito.when((Long) nHttpClientConnection.getContext().getAttribute(PassThroughConstants.CONNECTION_INIT_TIME))
                .thenReturn(0L);
        Mockito.when((Long) nHttpClientConnection.getContext().getAttribute(PassThroughConstants
                                                                                    .CONNECTION_EXPIRY_TIME))
                .thenReturn(currentTime);
        hostConnections.getConnection();
        Mockito.verify(nHttpClientConnection, times(1)).shutdown();
        Mockito.when((Long) nHttpClientConnection.getContext().getAttribute(PassThroughConstants.CONNECTION_INIT_TIME))
                .thenReturn(currentTime);
        Mockito.when((Long) nHttpClientConnection.getContext().getAttribute(PassThroughConstants
                                                                                    .CONNECTION_EXPIRY_TIME))
                .thenReturn(0L);
        hostConnections.getConnection();
        Mockito.verify(nHttpClientConnection, times(1)).shutdown();
    }
}
