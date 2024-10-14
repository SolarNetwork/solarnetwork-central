/* ==================================================================
 * CloudDatumStreamCriteria.java - 1/10/2024 7:48:21 am
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
public interface CloudDatumStreamCriteria {

	/**
	 * Get the first datum stream ID.
	 *
	 * <p>
	 * This returns the first available datum stream ID from the
	 * {@link #getDatumStreamIds()} array, or {@literal null} if not available.
	 * </p>
	 *
	 * @return the first datum stream ID, or {@literal null} if not available
	 */
	default Long getDatumStreamId() {
		final Long[] array = getDatumStreamIds();
		return (array != null && array.length > 0 ? array[0] : null);
	}

	/**
	 * Get an array of datum stream IDs.
	 *
	 * @return array of datum stream IDs (may be {@literal null})
	 */
	Long[] getDatumStreamIds();

	/**
	 * Test if this filter has any datum stream criteria.
	 *
	 * @return {@literal true} if the datum stream ID is non-null
	 */
	default boolean hasDatumStreamCriteria() {
		return getDatumStreamId() != null;
	}

}
