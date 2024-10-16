/* ==================================================================
 * CloudDatumStreamMappingCriteria.java - 16/10/2024 7:26:54 am
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

package net.solarnetwork.central.c2c.dao;

/**
 * Search criteria for cloud datum stream related data.
 *
 * @author matt
 * @version 1.0
 */
public interface CloudDatumStreamMappingCriteria {

	/**
	 * Get the first datum stream mapping ID.
	 *
	 * <p>
	 * This returns the first available datum stream mapping ID from the
	 * {@link #getDatumStreamPropertyMappingIds()} array, or {@literal null} if
	 * not available.
	 * </p>
	 *
	 * @return the first datum stream mapping ID, or {@literal null} if not
	 *         available
	 */
	default Long getDatumStreamMappingId() {
		final Long[] array = getDatumStreamMappingIds();
		return (array != null && array.length > 0 ? array[0] : null);
	}

	/**
	 * Get an array of datum stream mapping IDs.
	 *
	 * @return array of datum stream mapping IDs (may be {@literal null})
	 */
	Long[] getDatumStreamMappingIds();

	/**
	 * Test if this filter has any datum stream mapping criteria.
	 *
	 * @return {@literal true} if the datum stream mapping ID is non-null
	 */
	default boolean hasDatumStreamMappingCriteria() {
		return getDatumStreamMappingId() != null;
	}

}
