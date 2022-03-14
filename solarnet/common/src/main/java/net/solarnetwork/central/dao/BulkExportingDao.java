/* ==================================================================
 * BulkExportingDao.java - 31/01/2019 1:39:54 pm
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

import java.util.Map;

/**
 * DAO API for bulk exporting.
 * 
 * @param <T>
 *        the domain object type
 * @author matt
 * @version 1.0
 * @since 1.45
 */
public interface BulkExportingDao<T> {

	/**
	 * Export processing options.
	 */
	interface ExportOptions {

		/**
		 * Get a unique name for this export operation.
		 * 
		 * @return a name
		 */
		String getName();

		/**
		 * Get a batch size hint.
		 * 
		 * @return a batch size
		 */
		Integer getBatchSize();

		/**
		 * Get optional additional parameters, implementation specific.
		 * 
		 * @return parameters
		 */
		Map<String, Object> getParameters();

	}

	/**
	 * Handler for export processing.
	 * 
	 * @param <T>
	 *        the domain object type
	 */
	interface ExportCallback<T> {

		/**
		 * Called when the export has begun, before any call to
		 * {@link #handle(Object)}.
		 * 
		 * <p>
		 * This method will always be called once, before any calls to
		 * {@link #handle(Object)}.
		 * </p>
		 * 
		 * @param totalResultCountEstimate
		 *        the total result count estimate, or {@literal null} if not
		 *        known
		 */
		void didBegin(Long totalResultCountEstimate);

		/**
		 * Handle a single domain instance batch operation.
		 * 
		 * @param domainObject
		 *        the domain object
		 * @return the operation results
		 */
		ExportCallbackAction handle(T domainObject);
	}

	/**
	 * The action to perform after a single export callback.
	 */
	enum ExportCallbackAction {

		/** Continue processing. */
		CONTINUE,

		/** We should stop processing immediately. */
		STOP,
	}

	/**
	 * The result of the entire export processing.
	 */
	interface ExportResult {

		/**
		 * Return the number of domain objects processed.
		 * 
		 * @return the number of objects processed
		 */
		long getNumProcessed();

	}

	/**
	 * Export a set of domain objects.
	 * 
	 * @param callback
	 *        the export callback handler
	 * @param options
	 *        the export processing options
	 * @return the export results
	 */
	ExportResult batchExport(ExportCallback<T> callback, ExportOptions options);

}
