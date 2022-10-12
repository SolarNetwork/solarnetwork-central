/* ==================================================================
 * OptimizerCriteria.java - 31/08/2022 11:03:53 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.dao;

/**
 * Criteria API for a optimizer entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface OptimizerCriteria {

	/**
	 * Test if any optimizer criteria exists.
	 * 
	 * @return {@literal true} if a optimizer criteria exists
	 */
	default boolean hasOptimizerCriteria() {
		Long id = getOptimizerId();
		return (id != null);
	}

	/**
	 * Get an array of optimizer IDs.
	 * 
	 * @return array of IDs (may be {@literal null})
	 */
	Long[] getOptimizerIds();

	/**
	 * Get the first optimizer ID.
	 * 
	 * <p>
	 * This returns the first available ID from the {@link #getOptimizerIds()}
	 * array, or {@literal null} if not available.
	 * </p>
	 * 
	 * @return the optimizer ID, or {@literal null} if not available
	 */
	default Long getOptimizerId() {
		Long[] ids = getOptimizerIds();
		return (ids != null && ids.length > 0 ? ids[0] : null);
	}

}
