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
import org.jspecify.annotations.Nullable;

/**
 * Search criteria for arbitrary parameters.
 * 
 * @author matt
 * @version 1.3
 */
public interface ParameterCriteria {

	/**
	 * Get a map of parameter values.
	 * 
	 * @return parameters (may be {@code null})
	 */
	@Nullable
	Map<String, ?> getParameters();

	/**
	 * Test if this filter has any parameter criteria.
	 * 
	 * @return {@code true} if the parameters map is non-empty
	 */
	default boolean hasParameterCriteria() {
		final var params = getParameters();
		return params != null && !params.isEmpty();
	}

	/**
	 * Test if a parameter key is present.
	 * 
	 * @param key
	 *        the key to test
	 * @return {@code true} if the parameters map contains the key {@code key}
	 * @since 1.1
	 */
	default boolean hasParameter(String key) {
		final var params = getParameters();
		return params != null && params.containsKey(key);
	}

	/**
	 * Get a parameter value.
	 * 
	 * @param key
	 *        the key to get
	 * @return the parameter value, or {@code null} if not found
	 * @since 1.2
	 */
	default @Nullable Object parameter(String key) {
		final var params = getParameters();
		return params != null ? params.get(key) : null;
	}

	/**
	 * Get a map of parameter values.
	 * 
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasParameterCriteria()} returns {@code true}, to avoid nullness
	 * warnings.
	 * </p>
	 * 
	 * @return the parameters (presumed non-null)
	 * @since 1.3
	 */
	@SuppressWarnings("NullAway")
	default Map<String, ?> parameters() {
		return getParameters();
	}

	/**
	 * Get a parameter value.
	 * 
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasParameter(String)} returns {@code true}, to avoid nullness
	 * warnings.
	 * </p>
	 * 
	 * @param key
	 *        the key to get
	 * @return the parameter value (presumed non-null)
	 * @since 1.3
	 */
	@SuppressWarnings("NullAway")
	default Object param(String key) {
		return parameter(key);
	}

}
