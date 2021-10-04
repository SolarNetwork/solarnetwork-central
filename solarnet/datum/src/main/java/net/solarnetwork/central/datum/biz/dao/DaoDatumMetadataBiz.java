/* ==================================================================
 * DaoDatumMetadataBiz.java - Oct 3, 2014 4:02:04 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.LocationSourcePK;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.domain.ObjectSourcePK;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.dao.ObjectStreamCriteria;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.support.DatumUtils;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.support.BasicFilterResults;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.util.MapPathMatcher;
import net.solarnetwork.util.SearchFilter;

/**
 * DAO-based implementation of {@link DatumMetadataBiz}.
 * 
 * @author matt
 * @version 2.0
 */
public class DaoDatumMetadataBiz implements DatumMetadataBiz {

	private final DatumStreamMetadataDao metaDao;

	/**
	 * Constructor.
	 * 
	 * @param metaDao
	 *        the metadata DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoDatumMetadataBiz(DatumStreamMetadataDao metaDao) {
		super();
		if ( metaDao == null ) {
			throw new IllegalArgumentException("The metaDao argument must not be null.");
		}
		this.metaDao = metaDao;
	}

	private static GeneralDatumMetadata extractGeneralDatumMetadata(
			Iterable<ObjectDatumStreamMetadata> metas) {
		if ( metas != null ) {
			// assume at most 1 result... use first available
			for ( ObjectDatumStreamMetadata m : metas ) {
				return JsonUtils.getObjectFromJSON(m.getMetaJson(), GeneralDatumMetadata.class);
			}
		}
		return null;
	}

	private void mergeMetadata(ObjectSourcePK id, GeneralDatumMetadata meta) {
		BasicDatumCriteria filter = new BasicDatumCriteria();
		if ( id instanceof LocationSourcePK ) {
			filter.setLocationId(id.getObjectId());
			filter.setObjectKind(ObjectDatumKind.Location);
		} else {
			filter.setNodeId(id.getObjectId());
			filter.setObjectKind(ObjectDatumKind.Node);
		}
		filter.setSourceId(id.getSourceId());
		Iterable<ObjectDatumStreamMetadata> metas = metaDao.findDatumStreamMetadata(filter);

		final GeneralDatumMetadata existingMeta = extractGeneralDatumMetadata(metas);
		GeneralDatumMetadata newMeta = meta;
		if ( existingMeta == null ) {
			newMeta = meta;
		} else if ( existingMeta != null && !existingMeta.equals(meta) ) {
			newMeta = new GeneralDatumMetadata(existingMeta);
			newMeta.merge(meta, true);
		}
		if ( newMeta != null && !newMeta.equals(existingMeta) ) {
			// have changes, so persist
			String json = JsonUtils.getJSONString(newMeta, null);
			metaDao.replaceJsonMeta(id, json);
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void addGeneralNodeDatumMetadata(Long nodeId, String sourceId, GeneralDatumMetadata meta) {
		assert nodeId != null;
		assert sourceId != null;
		assert meta != null;
		mergeMetadata(new NodeSourcePK(nodeId, sourceId), meta);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void storeGeneralNodeDatumMetadata(Long nodeId, String sourceId, GeneralDatumMetadata meta) {
		assert nodeId != null;
		assert sourceId != null;
		assert meta != null;
		String json = JsonUtils.getJSONString(meta, null);
		metaDao.replaceJsonMeta(new NodeSourcePK(nodeId, sourceId), json);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void removeGeneralNodeDatumMetadata(Long nodeId, String sourceId) {
		metaDao.replaceJsonMeta(new NodeSourcePK(nodeId, sourceId), null);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<GeneralNodeDatumMetadataFilterMatch> findGeneralNodeDatumMetadata(
			GeneralNodeDatumMetadataFilter filter, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		BasicDatumCriteria criteria = DatumUtils.criteriaFromFilter(filter, sortDescriptors, offset,
				max);
		criteria.setObjectKind(ObjectDatumKind.Node);
		Iterable<ObjectDatumStreamMetadata> data = metaDao.findDatumStreamMetadata(criteria);
		List<GeneralNodeDatumMetadataFilterMatch> matches = stream(data.spliterator(), false)
				.map(DatumUtils::toGeneralNodeDatumMetadataMatch).collect(toList());
		return new BasicFilterResults<>(matches, (long) matches.size(), 0, matches.size());
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void addGeneralLocationDatumMetadata(Long locationId, String sourceId,
			GeneralDatumMetadata meta) {
		assert locationId != null;
		assert sourceId != null;
		assert meta != null;
		mergeMetadata(new LocationSourcePK(locationId, sourceId), meta);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void storeGeneralLocationDatumMetadata(Long locationId, String sourceId,
			GeneralDatumMetadata meta) {
		assert locationId != null;
		assert sourceId != null;
		assert meta != null;
		String json = JsonUtils.getJSONString(meta, null);
		metaDao.replaceJsonMeta(new LocationSourcePK(locationId, sourceId), json);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void removeGeneralLocationDatumMetadata(Long locationId, String sourceId) {
		metaDao.replaceJsonMeta(new LocationSourcePK(locationId, sourceId), null);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<GeneralLocationDatumMetadataFilterMatch> findGeneralLocationDatumMetadata(
			GeneralLocationDatumMetadataFilter filter, List<SortDescriptor> sortDescriptors,
			Integer offset, Integer max) {
		BasicDatumCriteria criteria = DatumUtils.criteriaFromFilter(filter, sortDescriptors, offset,
				max);
		criteria.setObjectKind(ObjectDatumKind.Location);
		Iterable<ObjectDatumStreamMetadata> data = metaDao.findDatumStreamMetadata(criteria);
		List<GeneralLocationDatumMetadataFilterMatch> matches = stream(data.spliterator(), false)
				.map(DatumUtils::toGeneralLocationDatumMetadataMatch).collect(toList());
		return new BasicFilterResults<>(matches, (long) matches.size(), 0, matches.size());
	}

	private <T extends ObjectSourcePK> Set<T> findMetadataForMetadataFilter(
			ObjectStreamCriteria criteria, String metadataFilter, BiFunction<Long, String, T> factory) {
		// parse metadata filter into SearchFilter
		SearchFilter filter = SearchFilter.forLDAPSearchFilterString(metadataFilter);
		if ( filter == null ) {
			throw new IllegalArgumentException("Invalid metadata filter.");
		}

		// execute query to find all metadata matching IDS
		Iterable<ObjectDatumStreamMetadata> metas = metaDao.findDatumStreamMetadata(criteria);

		// filter out only those matching the SearchFilter
		return StreamSupport.stream(metas.spliterator(), false).filter(m -> {
			Map<String, Object> map = JsonUtils.getStringMap(m.getMetaJson());
			return (map != null && MapPathMatcher.matches(map, filter));
		}).map(m -> {
			return factory.apply(m.getObjectId(), m.getSourceId());
		}).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Set<NodeSourcePK> getGeneralNodeDatumMetadataFilteredSources(Long[] nodeIds,
			String metadataFilter) {
		if ( nodeIds == null || nodeIds.length < 1 || metadataFilter == null
				|| metadataFilter.isEmpty() ) {
			return Collections.emptySet();
		}
		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setNodeIds(nodeIds);
		return findMetadataForMetadataFilter(criteria, metadataFilter, NodeSourcePK::new);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Set<LocationSourcePK> getGeneralLocationDatumMetadataFilteredSources(Long[] locationIds,
			String metadataFilter) {
		if ( locationIds == null || locationIds.length < 1 || metadataFilter == null
				|| metadataFilter.isEmpty() ) {
			return Collections.emptySet();
		}
		BasicDatumCriteria criteria = new BasicDatumCriteria();
		criteria.setLocationIds(locationIds);
		return findMetadataForMetadataFilter(criteria, metadataFilter, LocationSourcePK::new);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Iterable<ObjectDatumStreamMetadata> findDatumStreamMetadata(ObjectStreamCriteria filter) {
		return metaDao.findDatumStreamMetadata(filter);
	}

}
