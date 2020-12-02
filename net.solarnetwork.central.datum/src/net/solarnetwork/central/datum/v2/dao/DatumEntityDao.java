/* ==================================================================
 * DatumEntityDao.java - 23/10/2020 10:49:09 am
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

import java.util.List;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.central.datum.v2.domain.DatumDateInterval;
import net.solarnetwork.central.datum.v2.domain.DatumPK;
import net.solarnetwork.dao.BulkLoadingDao;
import net.solarnetwork.dao.FilterableDao;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.domain.SortDescriptor;

/**
 * DAO API for {@link DatumEntity} objects.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public interface DatumEntityDao extends GenericDao<DatumEntity, DatumPK>,
		FilterableDao<Datum, DatumPK, DatumCriteria>, BulkLoadingDao<GeneralNodeDatum> {

	/**
	 * API for querying for a filtered set of results from all possible results.
	 * 
	 * {@inheritDoc}
	 */
	@Override
	ObjectDatumStreamFilterResults<Datum, DatumPK> findFiltered(DatumCriteria filter,
			List<SortDescriptor> sorts, Integer offset, Integer max);

	@Override
	default ObjectDatumStreamFilterResults<Datum, DatumPK> findFiltered(DatumCriteria filter) {
		return findFiltered(filter, null, null, null);
	}

	/**
	 * Store a general node datum, saving as a datum entity.
	 * 
	 * @param datum
	 *        the datum to store
	 * @return the stored primary key
	 */
	DatumPK store(GeneralNodeDatum datum);

	/**
	 * Store a general location datum, saving as a datum entity.
	 * 
	 * @param datum
	 *        the datum to store
	 * @return the stored primary key
	 */
	DatumPK store(GeneralLocationDatum datum);

	/**
	 * Find date intervals for the available data for a set of datum streams.
	 * 
	 * @param filter
	 *        the search filter
	 * @return the matching date intervals
	 */
	Iterable<DatumDateInterval> findAvailableInterval(DatumStreamCriteria filter);

}
