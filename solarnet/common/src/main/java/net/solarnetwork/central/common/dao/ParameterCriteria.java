/* ==================================================================
 * ParameterCriteria.java - 15/10/2024 1:28:18 pm
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao;

import java.util.Map;

/**
 * Search criteria for arbitrary parameters.
 * 
 * @author matt
 * @version 1.0
 */
public interface ParameterCriteria {

	/**
	 * Get a map of parameter values.
	 * 
	 * @return parameters (may be {@literal null})
	 */
	Map<String, ?> getParameters();

	/**
	 * Test if this filter has any parameter criteria.
	 * 
	 * @return {@literal true} if the parameters map is non-empty
	 */
	default boolean hasParameterCriteria() {
		final var params = getParameters();
		return params != null && !params.isEmpty();
	}

}