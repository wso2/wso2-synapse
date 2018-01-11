/**
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p>
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.synapse.message.store.impl.jdbc.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Statement class for raw SQL statement
 */
public abstract class Statement {
    /**
     * Defines the SQL statement which should be executed.
     */
    private String statement = null;

    /**
     * List of parameters which should be included in the statement.
     */
    private final List<Object> parameters = new ArrayList<Object>();

    /**
     * Provides the de-serialized outcome of the query.
     *
     * @param resultSet the result-set obtained from the DB.
     * @return the result which contain each row and the corresponding column.
     */
    public abstract List<Map> getResult(ResultSet resultSet) throws SQLException;

    public Statement(String rawStatement) {
        this.statement = rawStatement;
    }

    public String getStatement() {
        return statement;
    }

    public void addParameter(Object o) {
        parameters.add(o);
    }

    public List<Object> getParameters() {
        return parameters;
    }
}
