/* ==================================================================
 * BulkLoadingDaoSupport.java - 12/11/2018 6:06:32 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package net.solarnetwork.central.common.dao.jdbc;

import static net.solarnetwork.central.dao.BulkLoadingDao.LoadingTransactionMode.BatchTransactions;
import static net.solarnetwork.central.dao.BulkLoadingDao.LoadingTransactionMode.NoTransaction;
import static net.solarnetwork.central.dao.BulkLoadingDao.LoadingTransactionMode.SingleTransaction;
import static net.solarnetwork.central.dao.BulkLoadingDao.LoadingTransactionMode.TransactionCheckpoints;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import net.solarnetwork.central.dao.BulkLoadingDao;
import net.solarnetwork.central.dao.BulkLoadingDao.LoadingExceptionHandler;
import net.solarnetwork.central.dao.BulkLoadingDao.LoadingOptions;
import net.solarnetwork.central.domain.Entity;

/**
 * Helper class for {@link net.solarnetwork.central.dao.BulkLoadingDao}
 * implementations that uses a JDBC prepared statement for bulk loading
 * operations.
 * 
 * <p>
 * To use this class, create an instance and configure the {@link DataSource},
 * {@link PlatformTransactionManager}, and {@code jdbcCall} to use, which can be
 * a simple prepared statement or a callable statement. Then create a subclass
 * of {@link BulkLoadingDaoSupport.BulkLoadingContext} and implement the
 * {@link BulkLoadingDaoSupport.BulkLoadingContext.doLoad(T, PreparedStatement,
 * long)} method to set the prepared statement parameters and execute the update
 * for each entity to be loaded.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class BulkLoadingDaoSupport {

	/** The default batch size. */
	public static final int DEFAULT_BATCH_SIZE = 100;

	private final Logger log;
	private PlatformTransactionManager txManager;
	private DataSource dataSource;
	private String jdbcCall;

	/**
	 * Constructor.
	 * 
	 * @param log
	 *        the logger to use
	 */
	public BulkLoadingDaoSupport(Logger log) {
		super();
		this.log = log;
	}

	private static class CountAwareCheckpoint {

		private final long count;
		private final Object savepoint;

		private CountAwareCheckpoint(Object savepoint, long count) {
			super();
			this.savepoint = savepoint;
			this.count = count;
		}
	}

	/**
	 * Abstract implementation of a bulk loading context.
	 * 
	 * <p>
	 * This class implements the majority of a bulk loading context. Extending
	 * classes must override {@link #doLoad(Entity, long)} to perform the actual
	 * loading of an entity.
	 * </p>
	 * 
	 * @param <T>
	 *        the entity type
	 * @param <PK>
	 *        the primary key type
	 */
	public abstract class BulkLoadingContext<T extends Entity<PK>, PK extends Serializable>
			implements BulkLoadingDao.LoadingContext<T, PK> {

		private final LoadingOptions options;
		private final LoadingExceptionHandler<T, PK> exceptionHandler;
		private final int batchSize;
		private long numLoaded;
		private long numCommitted;
		private final TransactionStatus transaction;
		private Connection con;
		private PreparedStatement stmt;
		private TransactionStatus batchTransaction;
		private CountAwareCheckpoint transactionCheckpoint;
		private T lastLoadedEntity;

		public BulkLoadingContext(LoadingOptions options,
				LoadingExceptionHandler<T, PK> exceptionHandler) throws SQLException {
			super();
			if ( options == null ) {
				throw new IllegalArgumentException("The LoadingOptions argument cannot be null");
			}
			this.options = options;
			this.exceptionHandler = exceptionHandler;
			this.numLoaded = 0;
			if ( options.getTransactionMode() == SingleTransaction
					|| options.getTransactionMode() == TransactionCheckpoints ) {
				log.debug("Starting new bulk load [{}] overall transaction", options.getName());
				DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
				txDef.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
				transaction = txManager.getTransaction(txDef);
			} else {
				transaction = null;
			}
			if ( options.getBatchSize() != null && options.getBatchSize() > 0 ) {
				this.batchSize = options.getBatchSize();
			} else {
				this.batchSize = DEFAULT_BATCH_SIZE;
			}
		}

		@Override
		public LoadingOptions getOptions() {
			return options;
		}

		@Override
		public long getLoadedCount() {
			return numLoaded;
		}

		@Override
		public long getCommittedCount() {
			return numCommitted;
		}

		private Connection getConnection() throws SQLException {
			Connection c = this.con;
			if ( c != null ) {
				return c;
			}
			c = DataSourceUtils.getConnection(dataSource);
			c.setAutoCommit(options.getTransactionMode() == NoTransaction);
			this.con = c;
			return c;
		}

		private PreparedStatement getPreparedStatement() throws SQLException {
			if ( this.stmt != null ) {
				return this.stmt;
			}
			PreparedStatement ps = createJdbcCall(getConnection());
			this.stmt = ps;
			return ps;

		}

		@Override
		public T getLastLoadedEntity() {
			return lastLoadedEntity;
		}

		@Override
		public final void load(T entity) {
			lastLoadedEntity = entity;
			try {
				if ( options.getTransactionMode() == BatchTransactions ) {
					if ( numLoaded % batchSize == 0 ) {
						if ( batchTransaction != null ) {
							commit();
						}
						log.debug("Starting new bulk load [{}] batch transaction @ row {}",
								options.getName(), numLoaded);
						DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
						txDef.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
						batchTransaction = txManager.getTransaction(txDef);
					}
				}
				if ( doLoad(entity, getPreparedStatement(), numLoaded) ) {
					numLoaded++;
				}
			} catch ( Exception e ) {
				if ( exceptionHandler != null ) {
					exceptionHandler.handleLoadingException(e, this);
				}
			}
		}

		protected abstract boolean doLoad(T entity, PreparedStatement stmt, long index)
				throws SQLException;

		@Override
		public void createCheckpoint() {
			if ( options.getTransactionMode() == TransactionCheckpoints && transaction != null
					&& !transaction.isCompleted() ) {
				Object checkpoint = transaction.createSavepoint();
				if ( transactionCheckpoint != null ) {
					transaction.releaseSavepoint(transactionCheckpoint.savepoint);
				}
				transactionCheckpoint = new CountAwareCheckpoint(checkpoint, numLoaded);
				numCommitted = numLoaded;
			}
		}

		@Override
		public void commit() {
			if ( batchTransaction != null ) {
				log.debug("Committing bulk load [{}] batch transaction @ row {}", options.getName(),
						numLoaded);
				txManager.commit(batchTransaction);
				batchTransaction = null;
			} else if ( transaction != null && !transaction.isCompleted() ) {
				log.debug("Committing bulk load [{}] overall transaction @ row {}", options.getName(),
						numLoaded);
				txManager.commit(transaction);
			}
			numCommitted = numLoaded;
			close();
			stmt = null;
			con = null;
		}

		@Override
		public void rollback() {
			if ( transactionCheckpoint != null && transaction != null ) {
				transaction.rollbackToSavepoint(transactionCheckpoint.savepoint);
				transaction.releaseSavepoint(transactionCheckpoint.savepoint);
				numLoaded = transactionCheckpoint.count;
				transactionCheckpoint = null;
			} else if ( batchTransaction != null && !batchTransaction.isCompleted() ) {
				batchTransaction.setRollbackOnly();
				txManager.rollback(batchTransaction);
				numLoaded = numCommitted;
				batchTransaction = null;
			} else if ( transaction != null && !transaction.isCompleted() ) {
				txManager.rollback(transaction);
				numLoaded = numCommitted;
			}
		}

		@Override
		public void close() {
			if ( stmt != null ) {
				try {
					if ( !stmt.isClosed() ) {
						stmt.close();
					}
				} catch ( SQLException e ) {
					log.warn("Error closing bulk loading statement", e);
				}
			}
			try {
				if ( batchTransaction != null && !batchTransaction.isCompleted() ) {
					txManager.rollback(batchTransaction);
				} else if ( transaction != null && !transaction.isCompleted() ) {
					txManager.rollback(transaction);
				}
			} catch ( Exception e ) {
				log.warn("Error rolling back transaction", e);
			}
			if ( con != null ) {
				try {
					if ( !con.isClosed() ) {
						con.close();
					}
				} catch ( SQLException e ) {
					log.warn("Error closing bulk loading connection", e);
				} finally {
					DataSourceUtils.releaseConnection(con, dataSource);
				}
			}
		}

	}

	private PreparedStatement createJdbcCall(Connection con) throws SQLException {
		PreparedStatement stmt;
		if ( jdbcCall.startsWith("{call") ) {
			stmt = con.prepareCall(jdbcCall);
		} else {
			stmt = con.prepareStatement(jdbcCall);
		}
		return stmt;
	}

	/**
	 * Get the transaction manager.
	 * 
	 * @return the manager
	 */
	public PlatformTransactionManager getTransactionManager() {
		return txManager;
	}

	/**
	 * Set the transaction manager.
	 * 
	 * @param txManager
	 *        the manager to set
	 */
	public void setTransactionManager(PlatformTransactionManager txManager) {
		this.txManager = txManager;
	}

	/**
	 * Get the JDBC data source.
	 * 
	 * @return the dataSource
	 */
	public DataSource getDataSource() {
		return dataSource;
	}

	/**
	 * Set the JDBC data source.
	 * 
	 * @param dataSource
	 *        the dataSource to set
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Get the JDBC statement to use for bulk loading.
	 * 
	 * @return the JDBC statement
	 */
	public String getJdbcCall() {
		return jdbcCall;
	}

	/**
	 * Set the JDBC statement to use for bulk loading.
	 * 
	 * @param jdbcCall
	 *        the JDBC statement to set
	 */
	public void setJdbcCall(String jdbcCall) {
		this.jdbcCall = jdbcCall;
	}

}
