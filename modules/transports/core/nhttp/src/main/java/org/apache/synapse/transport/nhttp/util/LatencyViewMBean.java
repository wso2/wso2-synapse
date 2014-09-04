/*
*  Licensed to the Apache Software Foundation (ASF) under one
*  or more contributor license agreements.  See the NOTICE file
*  distributed with this work for additional information
*  regarding copyright ownership.  The ASF licenses this file
*  to you under the Apache License, Version 2.0 (the
*  "License"); you may not use this file except in compliance
*  with the License.  You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/

package org.apache.synapse.transport.nhttp.util;

import java.util.Date;

/**
 * Provides metrics related to the latency added by the NHTTP transport while mediating
 * messages through.
 */
public interface LatencyViewMBean {
    /** All time averages */
    public double getAvg_Latency();
    public double getAvg_Latency_BackEnd();
    public double getAvg_Client_To_Esb_RequestReadTime();
    public double getAvg_Esb_To_BackEnd_RequestWriteTime();
    public double getAvg_BackEnd_To_Esb_ResponseReadTime();
    public double getAvg_Esb_To_Client_ResponseWriteTime();
    public double getAvg_ServerWorker_QueuedTime();
    public double getAvg_ClientWorker_QueuedTime();
    public double getAvg_request_Mediation_Latency();
    public double getAvg_response_Mediation_Latency();

    /** 1m averages */
    public double get1m_Avg_Latency();
    public double get1m_Avg_Latency_BackEnd();
    public double get1m_Avg_Client_To_Esb_RequestReadTime();
    public double get1m_Avg_Esb_To_BackEnd_RequestWriteTime();
    public double get1m_Avg_BackEnd_To_Esb_ResponseReadTime();
    public double get1m_Avg_Esb_To_Client_ResponseWriteTime();
    public double get1m_Avg_ServerWorker_QueuedTime();
    public double get1m_Avg_ClientWorker_QueuedTime();
    public double get1m_Avg_request_Mediation_Latency();
    public double get1m_Avg_response_Mediation_Latency();

    public double get5m_Avg_Latency();
    public double get5m_Avg_Latency_BackEnd();
    public double get5m_Avg_Client_To_Esb_RequestReadTime();
    public double get5m_Avg_Esb_To_BackEnd_RequestWriteTime();
    public double get5m_Avg_BackEnd_To_Esb_ResponseReadTime();
    public double get5m_Avg_Esb_To_Client_ResponseWriteTime();
    public double get5m_Avg_ServerWorker_QueuedTime();
    public double get5m_Avg_ClientWorker_QueuedTime();
    public double get5m_Avg_request_Mediation_Latency();
    public double get5m_Avg_response_Mediation_Latency();

    public double get15m_Avg_Latency();
    public double get15m_Avg_Latency_BackEnd();
    public double get15m_Avg_Client_To_Esb_RequestReadTime();
    public double get15m_Avg_Esb_To_BackEnd_RequestWriteTime();
    public double get15m_Avg_BackEnd_To_Esb_ResponseReadTime();
    public double get15m_Avg_Esb_To_Client_ResponseWriteTime();
    public double get15m_Avg_ServerWorker_QueuedTime();
    public double get15m_Avg_ClientWorker_QueuedTime();
    public double get15m_Avg_request_Mediation_Latency();
    public double get15m_Avg_response_Mediation_Latency();

    public double get1h_Avg_Latency();
    public double get1h_Avg_Latency_BackEnd();
    public double get1h_Avg_Client_To_Esb_RequestReadTime();
    public double get1h_Avg_Esb_To_BackEnd_RequestWriteTime();
    public double get1h_Avg_BackEnd_To_Esb_ResponseReadTime();
    public double get1h_Avg_Esb_To_Client_ResponseWriteTime();
    public double get1h_Avg_ServerWorker_QueuedTime();
    public double get1h_Avg_ClientWorker_QueuedTime();
    public double get1h_Avg_request_Mediation_Latency();
    public double get1h_Avg_response_Mediation_Latency();

    public double get8h_Avg_Latency();
    public double get8h_Avg_Latency_BackEnd();
    public double get8h_Avg_Client_To_Esb_RequestReadTime();
    public double get8h_Avg_Esb_To_BackEnd_RequestWriteTime();
    public double get8h_Avg_BackEnd_To_Esb_ResponseReadTime();
    public double get8h_Avg_Esb_To_Client_ResponseWriteTime();
    public double get8h_Avg_ServerWorker_QueuedTime();
    public double get8h_Avg_ClientWorker_QueuedTime();
    public double get8h_Avg_request_Mediation_Latency();
    public double get8h_Avg_response_Mediation_Latency();

    public double get24h_Avg_Latency();
    public double get24h_Avg_Latency_BackEnd();
    public double get24h_Avg_Client_To_Esb_RequestReadTime();
    public double get24h_Avg_Esb_To_BackEnd_RequestWriteTime();
    public double get24h_Avg_BackEnd_To_Esb_ResponseReadTime();
    public double get24h_Avg_Esb_To_Client_ResponseWriteTime();
    public double get24h_Avg_ServerWorker_QueuedTime();
    public double get24h_Avg_ClientWorker_QueuedTime();
    public double get24h_Avg_request_Mediation_Latency();
    public double get24h_Avg_response_Mediation_Latency();

    public void reset();

    public Date getLastResetTime();
}
