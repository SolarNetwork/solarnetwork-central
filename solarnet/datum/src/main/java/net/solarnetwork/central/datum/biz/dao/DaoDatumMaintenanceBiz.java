/* ==================================================================
 * DaoDatumMaintenanceBiz.java - 10/04/2019 9:03:02 am
 *
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.biz.dao;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.datum.biz.DatumMaintenanceBiz;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumKindPK;
import net.solarnetwork.central.datum.domain.StaleAggregateDatum;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumMaintenanceDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.support.DatumUtils;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.DatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * DAO based implementation of {@link DatumMaintenanceBiz}.
 *
 * @author matt
 * @version 2.1
 * @since 1.6
 */
public class DaoDatumMaintenanceBiz implements DatumMaintenanceBiz {

	private static final Logger log = LoggerFactory.getLogger(DaoDatumMaintenanceBiz.class);

	private final DatumMaintenanceDao datumDao;
	private final DatumStreamMetadataDao metaDao;

	/**
	 * Constructor.
	 *
	 * @param datumDao
	 *        the datum DAO to use
	 * @param metaDao
	 *        the metadata DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoDatumMaintenanceBiz(DatumMaintenanceDao datumDao, DatumStreamMetadataDao metaDao) {
		super();
		this.datumDao = requireNonNullArgument(datumDao, "datumDao");
		this.metaDao = requireNonNullArgument(metaDao, "metaDao");
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void markDatumAggregatesStale(GeneralNodeDatumFilter criteria) {
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(criteria);
		DatumUtils.populateAggregationType(criteria, c);
		int count = datumDao.markDatumAggregatesStale(c);
		log.info("Marked {} aggregate datum stale for criteria {}", count, c);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<StaleAggregateDatum, GeneralNodeDatumKindPK> findStaleAggregateDatum(
			GeneralNodeDatumFilter criteria, List<SortDescriptor> sortDescriptors, Long offset,
			Integer max) {
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(criteria, sortDescriptors, offset, max);
		c.setObjectKind(ObjectDatumKind.Node);
		DatumUtils.populateAggregationType(criteria, c);
		net.solarnetwork.dao.FilterResults<net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum, net.solarnetwork.central.datum.v2.domain.StreamKindPK> r = datumDao
				.findStaleAggregateDatum(c);
		List<StaleAggregateDatum> data = new ArrayList<>(r.getReturnedResultCount());
		if ( r.getReturnedResultCount() > 0 ) {
			Map<UUID, ObjectDatumStreamMetadata> metas = stream(
					metaDao.findDatumStreamMetadata(c).spliterator(), false)
							.collect(toMap(DatumStreamMetadata::getStreamId, identity()));
			for ( net.solarnetwork.central.datum.v2.domain.StaleAggregateDatum d : r ) {
				ObjectDatumStreamMetadata meta = metas.get(d.getStreamId());
				StaleAggregateDatum stale = new StaleAggregateDatum();
				if ( meta != null ) {
					stale.setCreated(d.getTimestamp());
					stale.setNodeId(meta.getObjectId());
					stale.setSourceId(meta.getSourceId());
					stale.setKind(d.getKind().getKey());
					data.add(stale);
				}
			}
		}
		return new BasicFilterResults<>(data, r.getTotalResults(), r.getStartingOffset(),
				r.getReturnedResultCount());
	}

}
