/* ==================================================================
 * ConfigurationCriteria.java - 12/08/2022 4:44:36 pm
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
 * Criteria API for a configuration entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface ConfigurationCriteria {

	/**
	 * Test if any configuration criteria exists.
	 * 
	 * @return {@literal true} if a configuration criteria exists
	 */
	default boolean hasConfigurationCriteria() {
		Long kind = getConfigurationId();
		return (kind != null);
	}

	/**
	 * Get an array of configuration IDs.
	 * 
	 * @return array of IDs (may be {@literal null})
	 */
	Long[] getConfigurationIds();

	/**
	 * Get the first configuration ID.
	 * 
	 * <p>
	 * This returns the first available ID from the
	 * {@link #getConfigurationIds()} array, or {@literal null} if not
	 * available.
	 * </p>
	 * 
	 * @return the configuration ID, or {@literal null} if not available
	 */
	default Long getConfigurationId() {
		Long[] kinds = getConfigurationIds();
		return (kinds != null && kinds.length > 0 ? kinds[0] : null);
	}

}
