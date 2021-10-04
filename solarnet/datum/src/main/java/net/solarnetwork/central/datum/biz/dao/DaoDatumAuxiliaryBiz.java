/* ==================================================================
 * DaoDatumAuxiliaryBiz.java - 4/02/2019 12:24:16 pm
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

import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.datum.v2.support.DatumUtils.toGeneralNodeDatumAuxiliaryFilterMatch;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.datum.biz.DatumAuxiliaryBiz;
import net.solarnetwork.central.datum.domain.DatumAuxiliaryType;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliary;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumAuxiliaryPK;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntity;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliary;
import net.solarnetwork.central.datum.v2.domain.DatumAuxiliaryPK;
import net.solarnetwork.central.datum.v2.domain.DatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.support.DatumUtils;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.support.BasicFilterResults;

/**
 * DAO based implementation of {@link DatumAuxiliaryBiz}.
 * 
 * @author matt
 * @version 2.0
 * @since 1.4
 */
public class DaoDatumAuxiliaryBiz implements DatumAuxiliaryBiz {

	private final DatumAuxiliaryEntityDao datumAuxiliaryDao;
	private final DatumStreamMetadataDao metaDao;

	/**
	 * Constructor.
	 * 
	 * @param datumAuxiliaryDao
	 *        the DAO to use
	 * @param metaDao
	 *        the metadata DAO to use
	 */
	public DaoDatumAuxiliaryBiz(DatumAuxiliaryEntityDao datumAuxiliaryDao,
			DatumStreamMetadataDao metaDao) {
		super();
		if ( datumAuxiliaryDao == null ) {
			throw new IllegalArgumentException("The datumAuxiliaryDao argument must not be null.");
		}
		this.datumAuxiliaryDao = datumAuxiliaryDao;
		if ( metaDao == null ) {
			throw new IllegalArgumentException("The metaDao argument must not be null.");
		}
		this.metaDao = metaDao;
	}

	private ObjectDatumStreamMetadata metaForId(GeneralNodeDatumAuxiliaryPK id) {
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(id.getNodeId());
		filter.setSourceId(id.getSourceId());
		filter.setObjectKind(ObjectDatumKind.Node);
		Iterable<ObjectDatumStreamMetadata> metas = metaDao.findDatumStreamMetadata(filter);
		for ( ObjectDatumStreamMetadata meta : metas ) {
			return meta;
		}
		throw new AuthorizationException(Reason.UNKNOWN_OBJECT, id);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public GeneralNodeDatumAuxiliary getGeneralNodeDatumAuxiliary(GeneralNodeDatumAuxiliaryPK id) {
		ObjectDatumStreamMetadata meta = metaForId(id);
		DatumAuxiliaryPK auxId = new DatumAuxiliaryPK(meta.getStreamId(), id.getCreated(),
				id.getType() != null ? id.getType() : DatumAuxiliaryType.Reset);
		DatumAuxiliaryEntity datum = datumAuxiliaryDao.get(auxId);
		if ( datum == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, id);
		}
		return DatumUtils.toGeneralNodeDatumAuxiliary(datum, meta);
	}

	private DatumAuxiliaryEntity convert(GeneralNodeDatumAuxiliary d, UUID streamId) {
		return new DatumAuxiliaryEntity(streamId, d.getCreated(),
				d.getType() != null ? d.getType() : DatumAuxiliaryType.Reset, d.getCreated(),
				d.getSamplesFinal(), d.getSamplesStart(), d.getNotes(), d.getMeta());
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void storeGeneralNodeDatumAuxiliary(GeneralNodeDatumAuxiliary datum) {
		ObjectDatumStreamMetadata meta = metaForId(datum.getId());
		DatumAuxiliaryEntity d = convert(datum, meta.getStreamId());
		datumAuxiliaryDao.save(d);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean moveGeneralNodeDatumAuxiliary(GeneralNodeDatumAuxiliaryPK from,
			GeneralNodeDatumAuxiliary to) {
		ObjectDatumStreamMetadata fromMeta = metaForId(from);
		DatumAuxiliaryPK fromId = new DatumAuxiliaryPK(fromMeta.getStreamId(), from.getCreated(),
				from.getType() != null ? from.getType() : DatumAuxiliaryType.Reset);

		// re-use the same metadata if node+source same in from & to
		ObjectDatumStreamMetadata toMeta = (from.getNodeId().equals(to.getId().getNodeId())
				&& from.getSourceId().equals(to.getId().getSourceId()) ? fromMeta
						: metaForId(to.getId()));
		DatumAuxiliaryEntity toDatum = convert(to, toMeta.getStreamId());
		return datumAuxiliaryDao.move(fromId, toDatum);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void removeGeneralNodeDatumAuxiliary(GeneralNodeDatumAuxiliaryPK id) {
		ObjectDatumStreamMetadata meta = metaForId(id);
		DatumAuxiliaryPK datumId = new DatumAuxiliaryPK(meta.getStreamId(), id.getCreated(),
				id.getType());
		DatumAuxiliaryEntity aux = datumAuxiliaryDao.get(datumId);
		if ( aux == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, id);
		}
		datumAuxiliaryDao.delete(aux);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<GeneralNodeDatumAuxiliaryFilterMatch> findGeneralNodeDatumAuxiliary(
			GeneralNodeDatumAuxiliaryFilter criteria, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max) {
		BasicDatumCriteria c = DatumUtils.criteriaFromFilter(criteria, sortDescriptors, offset, max);
		c.setObjectKind(ObjectDatumKind.Node);

		// ignore all date ranges for meta query here
		BasicDatumCriteria metaCriteria = c.clone();
		metaCriteria.setStartDate(null);
		metaCriteria.setEndDate(null);
		metaCriteria.setLocalStartDate(null);
		metaCriteria.setLocalEndDate(null);

		Map<UUID, ObjectDatumStreamMetadata> metas = StreamSupport
				.stream(metaDao.findDatumStreamMetadata(metaCriteria).spliterator(), false)
				.collect(toMap(DatumStreamMetadata::getStreamId, Function.identity()));
		net.solarnetwork.dao.FilterResults<DatumAuxiliary, DatumAuxiliaryPK> r = datumAuxiliaryDao
				.findFiltered(c);
		List<GeneralNodeDatumAuxiliaryFilterMatch> data = stream(r.spliterator(), false)
				.map(d -> toGeneralNodeDatumAuxiliaryFilterMatch(d, metas.get(d.getStreamId())))
				.collect(Collectors.toList());
		return new BasicFilterResults<>(data, r.getTotalResults(), r.getStartingOffset(),
				r.getReturnedResultCount());
	}

}
