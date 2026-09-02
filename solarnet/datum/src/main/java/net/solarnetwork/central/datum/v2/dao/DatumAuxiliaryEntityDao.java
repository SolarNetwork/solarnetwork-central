/* ==================================================================
 * DatumAuxiliaryEntityDao.java - 28/11/2020 8:40:43 am
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

import java.time.Instant;
import java.util.LinkedHashMap;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliaryPK;
import net.solarnetwork.dao.FilterableDao;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.domain.datum.DatumAuxiliaryType;
import net.solarnetwork.util.SearchFilter;
import net.solarnetwork.util.SearchFilter.CompareOperator;
import net.solarnetwork.util.SearchFilter.LogicOperator;

/**
 * DAO API for {@link DatumAuxiliaryEntity} objects.
 *
 * @author matt
 * @version 1.1
 * @since 2.8
 */
public interface DatumAuxiliaryEntityDao extends GenericDao<DatumAuxiliaryEntity, DatumAuxiliaryPK>,
		FilterableDao<DatumAuxiliary, DatumAuxiliaryPK, DatumAuxiliaryCriteria> {

	/**
	 * Move auxiliary data from one primary key to another.
	 *
	 * <p>
	 * This essentially performs an update of an existing record, so no changes
	 * will be made if a record with a primary key of {@code from} does not
	 * exist.
	 * </p>
	 *
	 * @param from
	 *        the primary key to move the data from
	 * @param to
	 *        the data to store, including the primary key to store it at
	 * @return {@literal true} if the record existed and was moved
	 */
	boolean move(DatumAuxiliaryPK from, DatumAuxiliaryEntity to);

	/**
	 * Delete auxiliary data matching a search criteria.
	 *
	 * <p>
	 * At a minimum, the following criteria are supported:
	 * </p>
	 *
	 * <ul>
	 * <li>node IDs</li>
	 * <li>source IDs</li>
	 * <li>date range (start/end dates)</li>
	 * <li>auxiliary kind</li>
	 * <li>search filter (LDAP-style metadata filter)</li>
	 * </ul>
	 *
	 * @param filter
	 *        the search criteria
	 * @return the number of auxiliary deleted
	 * @since 1.1
	 */
	long deleteFiltered(DatumAuxiliaryCriteria filter);

	/**
	 * Create a search filter out of common auxiliary criteria.
	 *
	 * @param kind
	 *        the auxiliary kind
	 * @param nodeIds
	 *        the node IDs
	 * @param sourceIds
	 *        the source IDs
	 * @param from
	 *        the starting date (inclusive)
	 * @param to
	 *        the ending date (exclusive)
	 * @param types
	 *        values to match {@link DatumAuxiliary#TYPE_META_KEY}
	 * @param generatedBy
	 *        a value to match {@link DatumAuxiliary#GENERATED_BY_META_KEY}
	 * @return the new filter
	 * @since 1.1
	 */
	static BasicDatumCriteria datumAuxiliaryCriteria(DatumAuxiliaryType kind, Long[] nodeIds,
			String[] sourceIds, Instant from, Instant to, String @Nullable [] types,
			@Nullable String generatedBy) {
		var result = new BasicDatumCriteria();
		result.setDatumAuxiliaryType(kind);
		result.setNodeIds(nodeIds);
		result.setSourceIds(sourceIds);
		result.setStartDate(from);
		result.setEndDate(to);

		var metaFilter = new LinkedHashMap<String, SearchFilter>(2);
		if ( generatedBy != null ) {
			metaFilter.put("generatedBy", new SearchFilter("/m/" + DatumAuxiliary.GENERATED_BY_META_KEY,
					generatedBy, CompareOperator.EQUAL));
		}

		if ( types != null ) {
			var m = new LinkedHashMap<String, String>(types.length);
			for ( String type : types ) {
				m.put("/m/" + DatumAuxiliary.TYPE_META_KEY, type);
			}
			metaFilter.put("types", new SearchFilter(m, LogicOperator.OR));
		}

		if ( !metaFilter.isEmpty() ) {
			result.setSearchFilter(new SearchFilter(metaFilter).asLDAPSearchFilterString());
		}

		return result;
	}

}
