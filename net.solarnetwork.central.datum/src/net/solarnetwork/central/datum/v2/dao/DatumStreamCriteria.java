/* ==================================================================
 * DatumStreamCriteria.java - 20/11/2020 10:44:40 am
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
import net.solarnetwork.dao.OptimizedQueryCriteria;
import net.solarnetwork.dao.PaginationCriteria;
import net.solarnetwork.dao.RecentCriteria;
import net.solarnetwork.dao.SortCriteria;

/**
 * Search criteria for datum streams.
 * 
 * <p>
 * Since this API extends <b>both</b> {@link NodeMetadataCriteria} and
 * {@code LocationMetadataCriteria}, the {@link ObjectMetadataCriteria} API is
 * implemented here such that if a location ID is available the location IDs
 * will be returned, otherwise any node IDs will be returned.
 * </p>
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public interface DatumStreamCriteria extends DateRangeCriteria, LocalDateRangeCriteria,
		NodeMetadataCriteria, LocationMetadataCriteria, UserCriteria, AggregationCriteria,
		RecentCriteria, OptimizedQueryCriteria, PaginationCriteria, SortCriteria {

	/**
	 * Test if this filter has any location, node, or source criteria.
	 * 
	 * @return {@literal true} if a location, node, or source ID is non-null
	 */
	default boolean hasDatumMetadataCriteria() {
		return (getLocationId() != null || getNodeId() != null || getSourceId() != null);
	}

	/**
	 * Test if the filter as a local date range specified.
	 * 
	 * @return {@literal true} if both a local start and end date are non-null
	 */
	default boolean hasLocalDateRange() {
		return (getLocalStartDate() != null && getLocalEndDate() != null);
	}

	/**
	 * Test if the filter as a local start date specified.
	 * 
	 * @return {@literal true} if the local start date is non-null
	 */
	default boolean hasLocalStartDate() {
		return getLocalStartDate() != null;
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
