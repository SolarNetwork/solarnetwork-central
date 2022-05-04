/* ==================================================================
 * StreamDatumFilter.java - 29/04/2022 4:24:41 pm
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

package net.solarnetwork.central.datum.domain;

import java.util.UUID;
import net.solarnetwork.central.domain.DateRangeFilter;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.central.domain.LocalDateRangeFilter;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Filter for datum stream data.
 * 
 * @author matt
 * @version 1.0
 * @since 1.3
 */
public interface StreamDatumFilter
		extends Filter, DateRangeFilter, LocalDateRangeFilter, MostRecentFilter, SourceFilter {

	/**
	 * Get the first stream ID.
	 * 
	 * <p>
	 * This returns the first available stream ID from the
	 * {@link #getStreamIds()} array, or {@literal null} if not available.
	 * </p>
	 * 
	 * @return the first stream ID, or {@literal null} if not available
	 */
	UUID getStreamId();

	/**
	 * Get an array of stream IDs.
	 * 
	 * @return array of stream IDs (may be {@literal null})
	 */
	UUID[] getStreamIds();

	/**
	 * Get the stream object kind.
	 * 
	 * @return the object kind (may be {@literal null})
	 */
	ObjectDatumKind getKind();

	/**
	 * Get the first object ID.
	 * 
	 * <p>
	 * This returns the first available object ID from the
	 * {@link #getObjectIds()} array, or {@literal null} if not available.
	 * </p>
	 * 
	 * @return the first object ID, or {@literal null} if not available
	 */
	Long getObjectId();

	/**
	 * Get an array of object IDs.
	 * 
	 * @return array of object IDs (may be {@literal null})
	 */
	Long[] getObjectIds();

}
