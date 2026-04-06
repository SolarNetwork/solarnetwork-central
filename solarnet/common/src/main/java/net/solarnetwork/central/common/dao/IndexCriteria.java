/* ==================================================================
 * IndexCriteria.java - 5/08/2023 12:15:02 pm
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

package net.solarnetwork.central.common.dao;

import org.jspecify.annotations.Nullable;

/**
 * Search criteria for indexed related data.
 * 
 * @author matt
 * @version 1.1
 */
public interface IndexCriteria {

	/**
	 * Get the first index.
	 * 
	 * <p>
	 * This returns the first available value from the {@link #getIndexes()}
	 * array, or {@code null} if not available.
	 * </p>
	 * 
	 * @return the first index, or {@code null} if not available
	 */
	default @Nullable Integer getIndex() {
		final Integer[] array = getIndexes();
		return (array != null && array.length > 0 ? array[0] : null);
	}

	/**
	 * Get an array of indexes.
	 * 
	 * @return array of indexes (may be {@code null})
	 */
	Integer @Nullable [] getIndexes();

	/**
	 * Test if this filter has any index criteria.
	 * 
	 * @return {@literal true} if the index is non-null
	 */
	default boolean hasIndexCriteria() {
		return getIndex() != null;
	}

	/**
	 * Get the first index ID.
	 * 
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasIndexCriteria()} returns {@code true}, to avoid nullness
	 * warnings.
	 * </p>
	 * 
	 * @return the first index ID (presumed non-null)
	 * @since 1.1
	 */
	@SuppressWarnings("NullAway")
	default Integer index() {
		return getIndex();
	}

	/**
	 * Get an array of index IDs.
	 *
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasIndexCriteria()} returns {@code true}, to avoid nullness
	 * warnings.
	 * </p>
	 *
	 * @return array of index IDs (presumed non-null)
	 * @since 1.1
	 */
	@SuppressWarnings("NullAway")
	default Integer[] indexes() {
		return getIndexes();
	}

}
