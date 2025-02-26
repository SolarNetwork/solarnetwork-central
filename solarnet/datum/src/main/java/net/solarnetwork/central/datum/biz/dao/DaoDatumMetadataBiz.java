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
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.context.MessageSource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.common.dao.BasicLocationRequestCriteria;
import net.solarnetwork.central.common.dao.LocationRequestCriteria;
import net.solarnetwork.central.common.dao.LocationRequestDao;
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
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.central.datum.v2.support.DatumUtils;
import net.solarnetwork.central.domain.LocationRequest;
import net.solarnetwork.central.domain.LocationRequestInfo;
import net.solarnetwork.central.domain.LocationRequestStatus;
import net.solarnetwork.central.mail.MailService;
import net.solarnetwork.central.mail.support.BasicMailAddress;
import net.solarnetwork.central.mail.support.ClasspathResourceMessageTemplateDataSource;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.BasicLocation;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.util.MapPathMatcher;
import net.solarnetwork.util.SearchFilter;

/**
 * DAO-based implementation of {@link DatumMetadataBiz}.
 *
 * @author matt
 * @version 2.2
 */
public class DaoDatumMetadataBiz implements DatumMetadataBiz {

	private final DatumStreamMetadataDao metaDao;
	private final LocationRequestDao locationRequestDao;
	private final ObjectMapper objectMapper;

	private String locationRequestSubmittedAlertEmailRecipient;
	private MailService mailService;
	private MessageSource messageSource;
	private TaskExecutor taskExecutor;

	/**
	 * Constructor.
	 *
	 * @param metaDao
	 *        the metadata DAO to use
	 * @param locationRequestDao
	 *        the location request DAO to use
	 * @param objectMapper
	 *        the mapper to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoDatumMetadataBiz(DatumStreamMetadataDao metaDao, LocationRequestDao locationRequestDao,
			ObjectMapper objectMapper) {
		super();
		this.metaDao = requireNonNullArgument(metaDao, "metaDao");
		this.locationRequestDao = requireNonNullArgument(locationRequestDao, "locationRequestDao");
		this.objectMapper = requireNonNullArgument(objectMapper, "objectMapper");
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
		if ( existingMeta != null && !existingMeta.equals(meta) ) {
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
	public FilterResults<GeneralNodeDatumMetadataFilterMatch, NodeSourcePK> findGeneralNodeDatumMetadata(
			GeneralNodeDatumMetadataFilter filter, List<SortDescriptor> sortDescriptors, Long offset,
			Integer max) {
		BasicDatumCriteria criteria = DatumUtils.criteriaFromFilter(filter, sortDescriptors, offset,
				max);
		criteria.setObjectKind(ObjectDatumKind.Node);
		Iterable<ObjectDatumStreamMetadata> data = metaDao.findDatumStreamMetadata(criteria);
		List<GeneralNodeDatumMetadataFilterMatch> matches = stream(data.spliterator(), false)
				.map(DatumUtils::toGeneralNodeDatumMetadataMatch).collect(toList());
		return new BasicFilterResults<>(matches, (long) matches.size(), 0L, matches.size());
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
	public FilterResults<GeneralLocationDatumMetadataFilterMatch, LocationSourcePK> findGeneralLocationDatumMetadata(
			GeneralLocationDatumMetadataFilter filter, List<SortDescriptor> sortDescriptors, Long offset,
			Integer max) {
		BasicDatumCriteria criteria = DatumUtils.criteriaFromFilter(filter, sortDescriptors, offset,
				max);
		criteria.setObjectKind(ObjectDatumKind.Location);
		Iterable<ObjectDatumStreamMetadata> data = metaDao.findDatumStreamMetadata(criteria);
		List<GeneralLocationDatumMetadataFilterMatch> matches = stream(data.spliterator(), false)
				.map(DatumUtils::toGeneralLocationDatumMetadataMatch).collect(toList());
		return new BasicFilterResults<>(matches, (long) matches.size(), 0L, matches.size());
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
		}).map(m -> factory.apply(m.getObjectId(), m.getSourceId()))
				.collect(Collectors.toCollection(LinkedHashSet::new));
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

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Set<ObjectDatumStreamMetadataId> findDatumStreamMetadataIds(ObjectStreamCriteria filter) {
		Iterable<ObjectDatumStreamMetadataId> result = metaDao.findDatumStreamMetadataIds(filter);
		if ( result instanceof Set ) {
			return (Set<ObjectDatumStreamMetadataId>) result;
		}
		return StreamSupport.stream(result.spliterator(), false)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public net.solarnetwork.dao.FilterResults<LocationRequest, Long> findLocationRequests(
			final Long userId, final LocationRequestCriteria filter,
			final List<SortDescriptor> sortDescriptors, final Long offset, final Integer max) {
		requireNonNullArgument(userId, "userId");
		BasicLocationRequestCriteria criteria = new BasicLocationRequestCriteria(filter);
		criteria.setUserId(userId);
		return locationRequestDao.findFiltered(criteria, sortDescriptors, offset, max);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public LocationRequest getLocationRequest(final Long userId, final Long id) {
		BasicLocationRequestCriteria criteria = new BasicLocationRequestCriteria();
		criteria.setUserId(requireNonNullArgument(userId, "userId"));
		List<LocationRequest> results = locationRequestDao.find(requireNonNullArgument(id, "id"),
				criteria);
		if ( results.isEmpty() ) {
			throw new EmptyResultDataAccessException(1);
		}
		return results.getFirst();
	}

	private LocationRequestInfo normalizedInfo(LocationRequestInfo info) {
		LocationRequestInfo infoToSave = requireNonNullArgument(info, "info").clone();
		if ( infoToSave.getLocationId() != null ) {
			infoToSave.setLocation(null);
		} else {
			BasicLocation norm = BasicLocation
					.normalizedLocation(requireNonNullArgument(info.getLocation(), "info.location"));
			if ( !norm.hasLocationCriteria() || norm.getCountry() == null || norm.getTimeZoneId() == null
					|| norm.getStateOrProvince() == null || norm.getLocality() == null ) {
				throw new IllegalArgumentException(
						"Location details must be provided, i.e. country, zone, stateOrProvince, locality, etc.");
			}
			infoToSave.setLocation(norm);
		}
		return infoToSave;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public LocationRequest submitLocationRequest(final Long userId, final LocationRequestInfo info) {
		LocationRequest entity = new LocationRequest();
		entity.setUserId(requireNonNullArgument(userId, "userId"));
		LocationRequestInfo infoToSave = normalizedInfo(info);
		entity.setLocationId(infoToSave.getLocationId());
		try {
			entity.setJsonData(objectMapper.writeValueAsString(infoToSave));
		} catch ( JsonProcessingException e ) {
			throw new IllegalArgumentException("Invalid JSON data: " + e.getMessage(), e);
		}
		entity.setStatus(LocationRequestStatus.Submitted);
		Long id = locationRequestDao.save(entity);

		if ( mailService != null && messageSource != null
				&& locationRequestSubmittedAlertEmailRecipient != null ) {
			Runnable task = () -> {
				Map<String, Object> mailModel = new HashMap<>(4);
				mailModel.put("userId", userId);
				mailModel.put("requestId", id);
				mailModel.put("info", infoToSave);
				mailService.sendMail(
						new BasicMailAddress(null, locationRequestSubmittedAlertEmailRecipient),
						new ClasspathResourceMessageTemplateDataSource(Locale.getDefault(),
								messageSource.getMessage("location.request.submitted.mail.subject", null,
										Locale.getDefault()),
								"META-INF/mail/location-request-submitted.txt", mailModel));
			};
			if ( taskExecutor != null ) {
				taskExecutor.execute(task);
			} else {
				task.run();
			}
		}

		return locationRequestDao.get(id);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public LocationRequest updateLocationRequest(final Long userId, final Long id,
			final LocationRequestInfo info) {
		LocationRequest entity = getLocationRequest(userId, id);
		if ( entity == null ) {
			throw new EmptyResultDataAccessException("Entity not found.", 1);
		} else if ( entity.getStatus() != LocationRequestStatus.Submitted ) {
			throw new DataIntegrityViolationException(String.format(
					"Only requests with status %s can be updated.", LocationRequestStatus.Submitted));
		}
		LocationRequestInfo infoToSave = normalizedInfo(info);
		entity.setLocationId(infoToSave.getLocationId());
		try {
			entity.setJsonData(objectMapper.writeValueAsString(infoToSave));
		} catch ( JsonProcessingException e ) {
			throw new RuntimeException("Error generating JSON data: " + e.getMessage(), e);
		}
		return locationRequestDao.get(locationRequestDao.save(entity));
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void removeLocationRequest(final Long userId, final Long id) {
		BasicLocationRequestCriteria criteria = new BasicLocationRequestCriteria();
		criteria.setUserId(requireNonNullArgument(userId, "userId"));
		int count = locationRequestDao.delete(requireNonNullArgument(id, "id"), criteria);
		if ( count < 1 ) {
			throw new EmptyResultDataAccessException("Entity not found.", 1);
		}
	}

	/**
	 * Set the recipient mail address for location request submission alerts.
	 *
	 * @param locationRequestSubmittedAlertEmailRecipient
	 *        the locationRequestSubmittedAlertEmailRecipient to set
	 */
	public void setLocationRequestSubmittedAlertEmailRecipient(
			String locationRequestSubmittedAlertEmailRecipient) {
		this.locationRequestSubmittedAlertEmailRecipient = locationRequestSubmittedAlertEmailRecipient;
	}

	/**
	 * Set a mail service to send emails with.
	 *
	 * @param mailService
	 *        the service to set
	 */
	public void setMailService(MailService mailService) {
		this.mailService = mailService;
	}

	/**
	 * Set a message source.
	 *
	 * @param messageSource
	 *        the source to set
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * Set a task executor.
	 *
	 * @param taskExecutor
	 *        the taskExecutor to set
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

}
