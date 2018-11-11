/* ==================================================================
 * BulkLoadingDao.java - 11/11/2018 8:18:55 PM
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

package net.solarnetwork.central.dao;

import java.io.Serializable;
import java.util.Map;
import net.solarnetwork.central.domain.Entity;

/**
 * API for entity batch loading DAO operations.
 * 
 * @author matt
 * @version 1.0
 * @since 1.43
 */
public interface BulkLoadingDao<T extends Entity<PK>, PK extends Serializable> {

	/**
	 * Batch loading transaction mode.
	 */
	enum LoadingTransactionMode {

		/** Perform the batch in a single transaction. */
		SingleTransaction,

		/**
		 * Support transaction checkpoints, within an overall single
		 * transaction.
		 */
		TransactionCheckpoints,

		/** Perform batch-size level transactions. */
		BatchTransactions,

		/** Load without any transaction. */
		NoTransaction;
	}

	/**
	 * Bulk loading options.
	 */
	interface LoadingOptions {

		/**
		 * Get a name for this batch operation.
		 * 
		 * @return a name
		 */
		public String getName();

		/**
		 * Get a batch size.
		 * 
		 * <p>
		 * If specified, perform loading in batches of this size.
		 * </p>
		 * 
		 * @return a batch size, or {@literal null} for no hint
		 */
		public Integer getBatchSize();

		/**
		 * Get the desired transaction mode.
		 * 
		 * @return the transaction mode
		 */
		public LoadingTransactionMode getTransactionMode();

		/**
		 * Get optional additional parameters, implementation specific.
		 * 
		 * @return parameters
		 */
		public Map<String, ?> getParameters();

	}

	/**
	 * API for handling an exception thrown during a bulk loading operation.
	 * 
	 * @param <T>
	 *        the entity type
	 * @param <PK>
	 *        the primary key type
	 */
	interface LoadingExceptionHandler<T extends Entity<PK>, PK extends Serializable> {

		void handleLoadingException(Throwable t, LoadingContext<T, PK> context);

	}

	/**
	 * API for a bulk loading operational context.
	 * 
	 * <p>
	 * This is the main API used to perform the bulk loading operation.
	 * 
	 * @param <T>
	 *        the entity type
	 * @param <PK>
	 *        the primary key type
	 */
	interface LoadingContext<T extends Entity<PK>, PK extends Serializable> extends AutoCloseable {

		LoadingOptions getOptions();

		void load(T entity);

		long getLoadedCount();

		void createCheckpoint();

		void commit();

		void rollback();

	}

	/**
	 * Initiate a bulk loading operation.
	 * 
	 * <p>
	 * The bulk loading operation works by calling this method to obtain a
	 * {@link LoadingContext} instance. You must call
	 * {@link LoadingContext#commit()} when the load is complete to finish the
	 * operation.
	 * </p>
	 * 
	 * @param options
	 *        the bulk loading options
	 * @param exceptionHandler
	 *        an exception handler
	 * @return the bulk loading context
	 */
	LoadingContext<T, PK> createBulkLoadingContext(LoadingOptions options,
			LoadingExceptionHandler<T, PK> exceptionHandler);

}
