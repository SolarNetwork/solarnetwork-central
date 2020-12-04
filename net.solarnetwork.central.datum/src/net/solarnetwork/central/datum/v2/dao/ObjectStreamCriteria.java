/* ==================================================================
 * ObjectStreamCriteria.java - 28/11/2020 8:46:38 am
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.dao.DateRangeCriteria;
import net.solarnetwork.dao.LocalDateRangeCriteria;
import net.solarnetwork.dao.PaginationCriteria;
import net.solarnetwork.dao.SortCriteria;

/**
 * Search criteria for datum streams.
 * 
 * <p>
 * Since this API extends <b>both</b> {@link NodeMetadataCriteria} and
 * {@link LocationMetadataCriteria}, the {@link ObjectMetadataCriteria} API is
 * implemented here such that if a location ID is available the location IDs
 * will be returned, otherwise any node IDs will be returned.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public interface ObjectStreamCriteria
		extends DateRangeCriteria, LocalDateRangeCriteria, AggregationCriteria, UserCriteria,
		SecurityTokenCriteria, NodeMetadataCriteria, LocationMetadataCriteria, ObjectMappingCriteria,
		SourceMappingCriteria, CombiningCriteria, PaginationCriteria, SortCriteria {

	/**
	 * Test if this filter has any location, node, or source criteria.
	 * 
	 * @return {@literal true} if a location, node, or source ID is non-null
	 */
	default boolean hasDatumMetadataCriteria() {
		return (getLocationId() != null || getNodeId() != null || getSourceId() != null);
	}

	/**
	 * Test if the filter has either a date or local date range specified (or
	 * both).
	 * 
	 * @return {@literal true} if either {@link #hasDateRange()} or
	 *         {@link #hasLocalDateRange()} return {@literal true}
	 */
	default boolean hasDateOrLocalDateRange() {
		return (hasDateRange() || hasLocalDateRange());
	}

	/**
	 * Test if the filter has either an object ID or source ID mapping specified
	 * (or both).
	 * 
	 * @return {@literal true} if either {@link #getObjectIdMappings()} or
	 *         {@link #getSourceIdMappings()} is not empty
	 */
	default boolean hasIdMappings() {
		return ((getObjectIdMappings() != null && !getObjectIdMappings().isEmpty())
				|| (getSourceIdMappings() != null && !getSourceIdMappings().isEmpty()));
	}

	/**
	 * Get the first available object ID.
	 * 
	 * <p>
	 * This will return the location ID if {@link #effectiveObjectKind()}
	 * returns {@code Location}, otherwise the node ID.
	 * </p>
	 * 
	 * {@inheritDoc}
	 */
	@Override
	default Long getObjectId() {
		ObjectDatumKind kind = effectiveObjectKind();
		return (kind == ObjectDatumKind.Location ? getLocationId() : getNodeId());
	}

	/**
	 * Get the object IDs.
	 * 
	 * <p>
	 * This will return the location IDs if {@link #effectiveObjectKind()}
	 * returns {@code Location}, otherwise node IDs.
	 * </p>
	 * 
	 * {@inheritDoc}
	 */
	@Override
	default Long[] getObjectIds() {
		ObjectDatumKind kind = effectiveObjectKind();
		return (kind == ObjectDatumKind.Location ? getLocationIds() : getNodeIds());
	}

	/**
	 * Get the effective object kind.
	 * 
	 * <p>
	 * If an explicit {@code objectKind} is not defined, then if a location ID
	 * is defined {@code Location} will be returned, otherwise {@code Node} will
	 * be returned.
	 * </p>
	 * 
	 * @return the effective object kind, never {@literal null}
	 */
	default ObjectDatumKind effectiveObjectKind() {
		ObjectDatumKind kind = getObjectKind();
		if ( kind == null ) {
			if ( getLocationId() != null ) {
				kind = ObjectDatumKind.Location;
			} else {
				kind = ObjectDatumKind.Node;
			}
		}
		return kind;
	}

}
