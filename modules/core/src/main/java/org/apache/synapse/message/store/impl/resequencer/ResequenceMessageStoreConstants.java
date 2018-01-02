/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.synapse.message.store.impl.resequencer;

/**
 * Holds the constants pertaining to re-sequencing
 */
class ResequenceMessageStoreConstants {
    /**
     * xpath expression to extract the sequence number
     */
    static final String SEQUENCE_NUMBER_XPATH = "store.resequence.id.path";
    /**
     * Specifies the property name of maximum number of peeks the gap would be detected for.
     */
    static final String MAX_NUMBER_OF_WAITING_COUNT = "store.resequence.timeout";
    /**
     * Sequence id column name.
     */
    static final String SEQ_ID = "seq_id";
    /**
     * Sequence id column name, which is stored in the db.
     */
    static final String SEQUENCE_ID_COLUMN = "seq_id";
    /**
     * Specifies the column value of the statement.
     */
    static final String STATEMENT_COLUMN = "statement";
    /**
     * Specifies the table name which will be used to maintain the last process id.
     */
    static final String LAST_PROCESS_ID_TABLE_NAME = "tbl_lastprocessid";
    /**
     * Specifies the meta info key value of the last process id.
     */
    private static final String LAST_PROCESS_ID_META_INFO = "LastProcessId";

}
