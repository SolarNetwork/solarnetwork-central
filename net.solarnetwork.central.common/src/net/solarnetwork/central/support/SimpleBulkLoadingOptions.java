/* ==================================================================
 * SimpleBulkLoadingOptions.java - 11/11/2018 8:45:12 PM
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

package net.solarnetwork.central.support;

import java.util.Map;
import net.solarnetwork.central.dao.BulkLoadingDao.LoadingOptions;
import net.solarnetwork.central.dao.BulkLoadingDao.LoadingTransactionMode;

/**
 * Basic immutable implementation of {@link LoadingOptions}.
 * 
 * @author matt
 * @version 1.0
 * @since 1.43
 */
public class SimpleBulkLoadingOptions implements LoadingOptions {

	private final String name;
	private final Integer batchSize;
	private final LoadingTransactionMode transactionMode;
	private final Map<String, ?> parameters;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *        the name
	 * @param batchSize
	 *        the batch size hint
	 * @param transactionMode
	 *        the transaction mode
	 * @param parameters
	 *        the parameters
	 */
	public SimpleBulkLoadingOptions(String name, Integer batchSize,
			LoadingTransactionMode transactionMode, Map<String, ?> parameters) {
		super();
		this.name = name;
		this.batchSize = batchSize;
		this.transactionMode = transactionMode;
		this.parameters = parameters;
	}

	/**
	 * Get the name.
	 * 
	 * @return the name
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Get the batch size hint.
	 * 
	 * @return the batchSize
	 */
	@Override
	public Integer getBatchSize() {
		return batchSize;
	}

	/**
	 * Get the transaction mode.
	 * 
	 * @return the batchTransactionMode
	 */
	@Override
	public LoadingTransactionMode getTransactionMode() {
		return transactionMode;
	}

	/**
	 * Get the parameters.
	 * 
	 * @return the parameters
	 */
	@Override
	public Map<String, ?> getParameters() {
		return parameters;
	}

}
