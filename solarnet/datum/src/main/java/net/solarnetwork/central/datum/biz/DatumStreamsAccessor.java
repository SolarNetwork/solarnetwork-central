/* ==================================================================
 * DatumStreamAccessor.java - 15/11/2024 6:55:46 am
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

package net.solarnetwork.central.datum.biz;

import java.util.Collection;
import net.solarnetwork.domain.datum.Datum;

/**
 * API for accessing datum streams.
 *
 * <p>
 * This API is meant for specialized use cases where access to some subset of
 * datum is needed for processing, for example a set of recent datum used with
 * expression calculations.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
public interface DatumStreamsAccessor {

	/**
	 * Get an earlier offset from the latest available datum per source ID.
	 *
	 * @param sourceIdPattern
	 *        an optional Ant-style source ID pattern to filter by; use
	 *        {@code null} to return all available sources
	 * @param offset
	 *        the offset from the latest, {@code 0} being the latest and
	 *        {@code 1} the next later, and so on
	 * @return the matching datum, never {@code null}
	 */
	Collection<Datum> offsetMatching(String sourceIdPattern, int offset);

	/**
	 * Get the latest available datum per source ID.
	 *
	 * <p>
	 * This is equivalent to calling {@code offsetMatching(sourceIdFilter, 0)}.
	 * </p>
	 *
	 * @param sourceIdPattern
	 *        an optional Ant-style source ID pattern to filter by
	 * @return the matching datum, never {@literal null}
	 * @see #offsetMatching(String, int)
	 */
	default Collection<Datum> latestMatching(String sourceIdPattern) {
		return offsetMatching(sourceIdPattern, 0);
	}

	/**
	 * Get an offset from the latest available datum matching a specific source
	 * ID.
	 *
	 * @param sourceId
	 *        the source ID to find the offset datum for
	 * @param offset
	 *        the offset from the latest, {@code 0} being the latest and
	 *        {@code 1} the next later, and so on
	 * @return the matching datum, or {@literal null} if not available
	 */
	Datum offset(String sourceId, int offset);

	/**
	 * Get the latest available datum matching a specific source IDs.
	 *
	 * <p>
	 * This is equivalent to calling {@code offset(sourceId, 0)}.
	 * </p>
	 *
	 * @param sourceId
	 *        the source ID to find
	 * @return the matching datum, or {@literal null} if not available
	 * @see #offset(String, int)
	 */
	default Datum latest(String sourceId) {
		return offset(sourceId, 0);
	}

}
