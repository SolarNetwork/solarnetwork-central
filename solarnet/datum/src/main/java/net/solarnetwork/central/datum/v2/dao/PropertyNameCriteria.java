/* ==================================================================
 * PropertyNameCriteria.java - 2/10/2023 12:40:58 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.dao;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Search criteria for datum property name data.
 * 
 * @author matt
 * @version 1.0
 */
public interface PropertyNameCriteria {

	/**
	 * Get the first property name.
	 * 
	 * <p>
	 * This returns the first available property name from the
	 * {@link #getPropertyNames()} array, or {@literal null} if not available.
	 * </p>
	 * 
	 * @return the property name, or {@literal null} if not available
	 */
	@JsonIgnore
	default String getPropertyName() {
		String[] names = getPropertyNames();
		return (names != null && names.length > 0 ? names[0] : null);
	}

	/**
	 * Get an array of property names.
	 * 
	 * @return array of property names (may be {@literal null})
	 */
	String[] getPropertyNames();

	/**
	 * Get the first instantaneous property name.
	 * 
	 * <p>
	 * This returns the first available instantaneous property name from the
	 * {@link #getInstantaneousPropertyNames()} array, or {@literal null} if not
	 * available.
	 * </p>
	 * 
	 * @return the instantaneous property name, or {@literal null} if not
	 *         available
	 */
	@JsonIgnore
	default String getInstantaneousPropertyName() {
		String[] names = getInstantaneousPropertyNames();
		return (names != null && names.length > 0 ? names[0] : null);
	}

	/**
	 * Get an array of instantaneous property names.
	 * 
	 * @return array of instantaneous property names (may be {@literal null})
	 */
	String[] getInstantaneousPropertyNames();

	/**
	 * Get the first accumulating property name.
	 * 
	 * <p>
	 * This returns the first available accumulating property name from the
	 * {@link #getAccumulatingPropertyNames()} array, or {@literal null} if not
	 * available.
	 * </p>
	 * 
	 * @return the accumulating property name, or {@literal null} if not
	 *         available
	 */
	@JsonIgnore
	default String getAccumulatingPropertyName() {
		String[] names = getAccumulatingPropertyNames();
		return (names != null && names.length > 0 ? names[0] : null);
	}

	/**
	 * Get an array of accumulating property names.
	 * 
	 * @return array of accumulating property names (may be {@literal null})
	 */
	String[] getAccumulatingPropertyNames();

	/**
	 * Get the first status property name.
	 * 
	 * <p>
	 * This returns the first available status property name from the
	 * {@link #getStatusPropertyNames()} array, or {@literal null} if not
	 * available.
	 * </p>
	 * 
	 * @return the status property name, or {@literal null} if not available
	 */
	@JsonIgnore
	default String getStatusPropertyName() {
		String[] names = getStatusPropertyNames();
		return (names != null && names.length > 0 ? names[0] : null);
	}

	/**
	 * Get an array of status property names.
	 * 
	 * @return array of status property names (may be {@literal null})
	 */
	String[] getStatusPropertyNames();

	/**
	 * Test if this filter has any property name criteria.
	 * 
	 * @return {@literal true} if the property name is non-null
	 */
	@JsonIgnore
	default boolean hasPropertyNameCriteria() {
		return getPropertyName() != null;
	}

	/**
	 * Test if this filter has any instantaneous property name criteria.
	 * 
	 * @return {@literal true} if the instantaneous property name is non-null
	 */
	@JsonIgnore
	default boolean hasInstantatneousPropertyNameCriteria() {
		return getInstantaneousPropertyName() != null;
	}

	/**
	 * Test if this filter has any accumulating property name criteria.
	 * 
	 * @return {@literal true} if the accumulating property name is non-null
	 */
	@JsonIgnore
	default boolean hasAccumulatingPropertyNameCriteria() {
		return getAccumulatingPropertyName() != null;
	}

	/**
	 * Test if this filter has any status property name criteria.
	 * 
	 * @return {@literal true} if the status property name is non-null
	 */
	@JsonIgnore
	default boolean hasStatusPropertyNameCriteria() {
		return getStatusPropertyName() != null;
	}

	/**
	 * Test if this filter has some property name (generic, instantaneous,
	 * accumulating, or status) criteria.
	 * 
	 * @return {@literal true} if the some property name criteria is non-null
	 */
	@JsonIgnore
	default boolean hasAnyPropertyNameCriteria() {
		return hasPropertyNameCriteria() || hasInstantatneousPropertyNameCriteria()
				|| hasAccumulatingPropertyNameCriteria() || hasStatusPropertyNameCriteria();
	}

}
