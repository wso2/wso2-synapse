/**
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
package org.apache.synapse.commons.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Context;
import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TranscationManger {

	protected static final Log log = LogFactory.getLog(TranscationManger.class);
	
	private static class ConnectionMapper{
		private final Connection realConn;
		private final String key;
		
		private ConnectionMapper(final Connection conn) {
			super();
			this.realConn = conn;
			this.key = conn.toString();
		}

		private Connection getConnection() {
			return realConn;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((key == null) ? 0 : key.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ConnectionMapper other = (ConnectionMapper) obj;
			if (key == null) {
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key))
				return false;
			return true;
		}
		
		
	}
	
	private static ConcurrentHashMap<Long, ConnectionMapper> connections = new ConcurrentHashMap<Long, ConnectionMapper>();

	private static final String TRANSCATION_MANGER_LOOKUP_STR = "java:comp/TransactionManager";

	/**
	 * This is used to keep the enlisted XADatasource objects
	 */
	private static ThreadLocal<Map<Long,XAResource>> enlistedXADataSources = new ThreadLocal<Map<Long,XAResource>>() {
		protected Map<Long,XAResource> initialValue() {
			return new HashMap<Long,XAResource>();
		}
	};
	
	private static ThreadLocal<Map<Long,TransactionManager>> txManagers = new ThreadLocal<Map<Long,TransactionManager>>() {
		protected Map<Long,TransactionManager> initialValue() {
			return new 	HashMap<Long,TransactionManager>();
		}
	};
	
	private static ThreadLocal<Map<Long,Transaction>> transactions = new ThreadLocal<Map<Long,Transaction>>() {
		protected Map<Long,Transaction> initialValue() {
			return new 	HashMap<Long,Transaction>();
		}
	};

	public static void lookUp(Context txContext) throws Exception {
		long key = Thread.currentThread().getId();
		Map<Long,TransactionManager> txMgrMap = txManagers.get();
		if(txMgrMap.containsKey(key)){
			
		}else{
			TransactionManager transactionManager = (TransactionManager) txContext
					.lookup(TRANSCATION_MANGER_LOOKUP_STR);
			txMgrMap.put(key, transactionManager);
			if(log.isDebugEnabled()){
				StringBuilder logMsg = new StringBuilder();
				logMsg
				.append(" Transaction Mgr Hashcode : " + transactionManager.hashCode())
				.append("\n")
				.append(" Transaction Mgr  : " + transactionManager);
				log.debug(logMsg.toString());

			}
						
		}

	}

	private static boolean isXAResourceEnlisted(XAResource resource)
			throws Exception {
		long key = Thread.currentThread().getId();
		return enlistedXADataSources.get().containsKey(key) && enlistedXADataSources.get().containsValue(resource);
	} 
	
	public static boolean checkConnectionAlreadyUse(Connection conn)
			throws SQLException {
		boolean isUsed = false;
		if (connections.containsValue(new ConnectionMapper(conn))) {
			isUsed = true;
			log.debug(" Connection toString : " + conn.toString());
		}

		return isUsed;
	}
	
	public static void removeConnectionUsed(long key) {
		boolean contains = false;
		try {
			if (connections.containsKey(key)) {
				contains = true;
				Connection conn = connections.get(key).getConnection();
				if (conn != null) {
					log.debug(" Connection close for Thread Id : " + key);
					conn.close();
				}
			}
		} catch (Exception ex) {
			log.error(" Ignore this error " + ex);
		} finally {
				if (contains) {
					connections.remove(key);
				}			
		}

	}
	
	public static Connection addConnection(final DataSource ds) throws Exception{
		long key = Thread.currentThread().getId();
		Connection conn = getConnection();
		if(conn != null){
			log.debug(" Connection can get from map : "+ key);
			return conn;
		}

		int count = 0;
		do{
    		conn = ds.getConnection();
    		Connection actual = ((javax.sql.PooledConnection)conn).getConnection();
    		if(conn == null || actual == null){
    			continue;
    		}
    		if(!TranscationManger.checkConnectionAlreadyUse(conn) && !actual.isClosed()){
    			if(!connections.containsKey(key)){
					connections.putIfAbsent(key, new ConnectionMapper(conn));
					log.debug(" Connection added to map in attempt : "+ count + " Thread : "+key);
				}
    			break;
    		}else{
    			conn.close();
    			conn = null;
    			Thread.sleep(500l);
    			continue;
    		}
    	}
    	while(++count < 5);
		
    	if(conn == null && count >= 5){
    		throw new Exception (" Not enough Connections in the pool, Cache size : "+ connections.size());
    	}		
		return conn;
		
	}
	
	public static Connection getConnection(){
		long key = Thread.currentThread().getId();
		ConnectionMapper connMapper = connections.get(key);
		Connection conn = connMapper != null ? connMapper.getConnection() : null;
		return conn;
	}
	
	public static boolean isThreadHasEnlistment() {
		// check there is an enlistment for current thread
		long key = Thread.currentThread().getId();
		boolean hasEnlistment = enlistedXADataSources.get().containsKey(key) ? enlistedXADataSources
				.get().get(key) != null : false;
		return hasEnlistment;
	}
	

	public static void bindConnection(final Connection conn) throws Exception {
		long key = Thread.currentThread().getId();
		try {			
			if (conn instanceof XAConnection) {
				Transaction tx = transactions.get().get(key);
				XAResource xaRes = ((XAConnection) conn).getXAResource();
				
				if (!isXAResourceEnlisted(xaRes)) {
					tx.enlistResource(xaRes);
					addToEnlistedXADataSources(xaRes, key);
					log.debug(" DS enlisted in thread " + key + " XA Resource : "+ xaRes.hashCode());
				}
			}

		} catch (Exception ex) {
			StringBuilder logMsg = new StringBuilder();
			Connection actual = ((javax.sql.PooledConnection)conn).getConnection();
			logMsg
			.append(" Thread Id : "+key)
			.append(" BIND ERROR , Transaction Manager status : " + txManagers.get().get(key).getStatus())
			.append("\n")
			.append(" BIND ERROR , Transaction status : " + transactions.get().get(key).getStatus())
			.append("\n")
			.append(" JDBC Connection status : " + actual.isClosed())
			.append("\n")
			.append(" BIND ERROR  : " + ex);
			log.error(logMsg.toString());
			rollbackTransaction(true,key);
			throw ex;
		}
	}

	public static void delistResource(int flag, long key) throws Exception {
		Map<Long,XAResource> enlistedResources = enlistedXADataSources.get();
		XAResource resource = null;
		try {
			if (enlistedResources != null && !enlistedResources.isEmpty()) {
				Transaction tx = transactions.get().get(key);
				resource = enlistedResources.get(key);
				if (tx != null && resource != null) {
					tx.delistResource(resource, flag);
				}					
			}			
			
		} catch (Exception ex) {
			throw new Exception("Error occurred while delisting datasource "
					+ "connection: " + ex.getMessage(), ex);
		}finally{
			removeConnectionUsed(key);
			removeTransaction(key);
			enlistedResources.remove(key);
		}
	}
	
	public static void removeTransaction(long key){
		transactions.get().remove(key);
	}

	private static void addToEnlistedXADataSources(final XAResource resource, long key)
			throws Exception {
		if(resource != null){
			enlistedXADataSources.get().put(key,resource);
		}		
	}

	public static void rollbackTransaction(boolean insideSynapse, long key) throws Exception {
		int xaResourceStatus = XAResource.TMFAIL;
		try {
			if (log.isDebugEnabled()) {
				log.debug("rollbackTransaction()");
			}
			
			if (insideSynapse && transactions.get() == null) {
				log.warn(" ROLLBACK Thread Local null ");
				return;
			}

			if (insideSynapse && transactions.get().get(key) == null) {
				log.warn(" ROLLBACK Some How TX null ");
				return;
			}

			if (transactions.get().get(key) != null	&& javax.transaction.Status.STATUS_ACTIVE == transactions
							.get().get(key).getStatus()) {
				txManagers.get().get(key).rollback();
				xaResourceStatus = XAResource.TMFAIL;
			}

		} catch (Exception ex) {
			log.error(" ROLLBACK ERROR  : "	+ txManagers.get().get(key).getStatus());
			throw ex;
		}finally{
			// delist
			delistResource(xaResourceStatus,key);
		}

	}

	public static void endTransaction(boolean insideSynapse, long key) throws Exception {
		int xaResourceStatus = XAResource.TMNOFLAGS;
		try {
			if(insideSynapse && transactions.get() == null){
				log.warn(" END Thread Local null ");			
				return;
			}
			
			if(insideSynapse && transactions.get().get(key) == null){
				log.warn(" END Some How TX null ");			
				return;
			}
			if (transactions.get().get(key) != null	&& javax.transaction.Status.STATUS_ACTIVE == transactions
							.get().get(key).getStatus()) {
				txManagers.get().get(key).commit();
				xaResourceStatus = XAResource.TMSUCCESS;
				
			}
			
		} catch (Exception ex) {
			xaResourceStatus = XAResource.TMFAIL;
			log.error(" END ERROR : " + txManagers.get().get(key).getStatus());			
			throw ex;
		}finally{
			// delist
			delistResource(xaResourceStatus, key);
		}

	}

	public static void beginTransaction() throws Exception {
		long key = Thread.currentThread().getId();
		try {
			if (log.isDebugEnabled()) {
				log.debug("beginTransaction()");
			}
						
			TransactionManager txMgr = txManagers.get().get(key);
			txMgr.begin();
			Transaction tx = txMgr.getTransaction();
			transactions.get().put(key, tx);
			log.debug(" BEGIN  : " + transactions.get().get(key).getStatus());

		} catch (Exception ex) {			
			log.debug(" BEGIN ERROR  : " + txManagers.get().get(key).getStatus());			
			throw ex;
		}

	}

	public static TransactionManager getTransactionManager() throws Exception {
	    long key = Thread.currentThread().getId();
	    try {
		    if (log.isDebugEnabled()) {
			    log.debug("getTransactionManager Called");
		    }

		    TransactionManager txMgr = txManagers.get().get(key);
		    return txMgr;
	    } catch (Exception ex) {
		    log.error(" BEGIN ERROR  : " + txManagers.get().get(key).getStatus());
		    throw ex;
	    }

	}

	public static Transaction getTransaction() throws Exception {
	    long key = Thread.currentThread().getId();
	    try {
		    if (log.isDebugEnabled()) {
			    log.debug("getTransaction Called");
		    }
		    return transactions.get().get(key);

	    } catch (Exception ex) {
		    log.error(" BEGIN ERROR  : " + txManagers.get().get(key).getStatus());
		    throw ex;
	    }

	}
	
	public static int getStatus() throws Exception{
		long key = Thread.currentThread().getId();
		int status = javax.transaction.Status.STATUS_UNKNOWN;
		if(transactions.get().get(key) == null){
			if(enlistedXADataSources.get().containsKey(key) && enlistedXADataSources.get().get(key) != null){
				log.warn(" END Some How TX null ");	
			}else{
				status = javax.transaction.Status.STATUS_NO_TRANSACTION;
			}
					
		}else{
			status = transactions.get().get(key).getStatus();
		}
		return status;	
	}
}
