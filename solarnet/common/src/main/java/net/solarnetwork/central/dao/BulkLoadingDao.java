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
import net.solarnetwork.dao.Entity;

/**
 * API for entity batch loading DAO operations.
 *
 * @param <T>
 *        the domain object type
 * @param <K>
 *        the primary key type
 * @author matt
 * @version 2.0
 * @since 1.43
 */
public interface BulkLoadingDao<T extends Entity<K>, K extends Serializable & Comparable<K>> {

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
		String getName();

		/**
		 * Get a batch size.
		 *
		 * <p>
		 * If specified, perform loading in batches of this size.
		 * </p>
		 *
		 * @return a batch size, or {@literal null} for no hint
		 */
		Integer getBatchSize();

		/**
		 * Get the desired transaction mode.
		 *
		 * @return the transaction mode
		 */
		LoadingTransactionMode getTransactionMode();

		/**
		 * Get optional additional parameters, implementation specific.
		 *
		 * @return parameters
		 */
		Map<String, ?> getParameters();

	}

	/**
	 * API for handling an exception thrown during a bulk loading operation.
	 *
	 * @param <T>
	 *        the entity type
	 * @param <K>
	 *        the primary key type
	 */
	interface LoadingExceptionHandler<T extends Entity<K>, K extends Serializable & Comparable<K>> {

		/**
		 * Handle a loading exception.
		 *
		 * @param t
		 *        the exception
		 * @param context
		 *        the context
		 */
		void handleLoadingException(Throwable t, LoadingContext<T, K> context);

	}

	/**
	 * API for a bulk loading operational context.
	 *
	 * <p>
	 * This is the main API used to perform the bulk loading operation.
	 *
	 * @param <T>
	 *        the entity type
	 * @param <K>
	 *        the primary key type
	 */
	interface LoadingContext<T extends Entity<K>, K extends Serializable & Comparable<K>>
			extends AutoCloseable {

		/**
		 * Get the loading options used to create the context.
		 *
		 * @return the loading options
		 */
		LoadingOptions getOptions();

		/**
		 * Load an entity.
		 *
		 * @param entity
		 *        the entity to load
		 */
		void load(T entity);

		/**
		 * Get the count of entities loaded thus far using this context.
		 *
		 * <p>
		 * If {@link #rollback()} is called, this value will reset back to the
		 * count of currently committed entities.
		 * </p>
		 *
		 * @return the loaded count
		 */
		long getLoadedCount();

		/**
		 * Get the count of entities committed thus far using this context.
		 *
		 * <p>
		 * How this value increments depends on the
		 * {@link LoadingTransactionMode} defined in the options that were used
		 * to create this context:
		 * </p>
		 *
		 * <dl>
		 * <dt>{@link LoadingTransactionMode#NoTransaction}</dt>
		 * <dd>This count will match {@link #getLoadedCount()} and increment as
		 * each entity is loaded.</dd>
		 * <dt>{@link LoadingTransactionMode#BatchTransactions}</dt>
		 * <dd>This count will increment only after each batch of loaded entity
		 * have been committed, and thus can lag behind
		 * {@link #getLoadedCount()}.</dd>
		 * <dd>{@link LoadingTransactionMode#TransactionCheckpoints}</dd>
		 * <dt>This count will only increment after calls to
		 * {@link #createCheckpoint()} are made. If {@link #rollback()} is
		 * called, this count will reset back to the count at the previous time
		 * {@link #createCheckpoint()} was called.</dt>
		 * <dt>{@link LoadingTransactionMode#SingleTransaction}</dt>
		 * <dd>This count will remain at {@literal 0} until {@link #commit()} is
		 * called, at which point it will match {@link #getLoadedCount()}.</dd>
		 * </dl>
		 *
		 * @return the committed entity count
		 */
		long getCommittedCount();

		/**
		 * Get the entity that was last passed to the {@link #load(Entity)}
		 * method.
		 *
		 * @return the last loaded entity
		 */
		T getLastLoadedEntity();

		/**
		 * Create a checkpoint that can be rolled back to.
		 *
		 * <p>
		 * The {@link LoadingTransactionMode#TransactionCheckpoints} mode must
		 * have been set in the options used to create this context.
		 * </p>
		 */
		void createCheckpoint();

		/**
		 * Commit the current transaction.
		 *
		 * <p>
		 * The nature of the current transaction depends on the transaction mode
		 * set in the options used to create this context:
		 * </p>
		 * <dl>
		 * <dt>{@code TransactionCheckpoints} or {@code SingleTransaction}</dt>
		 * <dd>All entities loaded via {@link #load(Entity)} are committed.</dd>
		 * <dt>{@code BatchTransactions}</dt>
		 * <dd>The entities loaded via {@link #load(Entity)} since the last
		 * automatic batch commit are committed.</dd>
		 * </dl>
		 */
		void commit();

		/**
		 * Discard the entities loaded within the current transaction.
		 *
		 * <p>
		 * The nature of the current transaction depends on the transaction mode
		 * set in the options used to create this context:
		 * </p>
		 *
		 * <dl>
		 * <dt>{@code SingleTransaction}</dt>
		 * <dd>All entities loaded are discarded.</dd>
		 * <dt>{@code TransactionCheckpoints}</dt>
		 * <dd>All entities loaded via {@link #load(Entity)} since the last call
		 * to {@link #createCheckpoint()} are discarded.</dd>
		 * <dt>{@code BatchTransactions}</dt>
		 * <dd>The entities loaded via {@link #load(Entity)} since the last
		 * automatic batch commit are discarded.</dd>
		 * </dl>
		 */
		void rollback();

		/**
		 * Close any temporary resources.
		 */
		@Override
		void close();

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
	LoadingContext<T, K> createBulkLoadingContext(LoadingOptions options,
			LoadingExceptionHandler<T, K> exceptionHandler);

}
