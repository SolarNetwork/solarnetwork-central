/* ==================================================================
 * DaoUserExportTaskBiz.java - 5/11/2021 9:15:33 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.export.biz.dao;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.PathMatcher;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.export.domain.BasicConfiguration;
import net.solarnetwork.central.datum.export.domain.BasicDataConfiguration;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.export.biz.UserExportTaskBiz;
import net.solarnetwork.central.user.export.dao.UserAdhocDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.dao.UserDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.domain.UserAdhocDatumExportTaskInfo;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskInfo;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskPK;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;

/**
 * DAO implementation of {@link UserExportTaskBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserExportTaskBiz implements UserExportTaskBiz {

	private final UserDatumExportTaskInfoDao taskDao;
	private final UserAdhocDatumExportTaskInfoDao adhocTaskDao;
	private final UserNodeDao userNodeDao;
	private final DatumStreamMetadataDao metaDao;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private PathMatcher pathMatcher;

	/**
	 * Constructor.
	 * 
	 * @param taskDao
	 *        the task DAO to use
	 * @param adhocTaskDao
	 *        the ad hoc task DAO to use
	 * @param userNodeDao
	 *        the user node DAO to use
	 * @param metaDao
	 *        the metadata DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoUserExportTaskBiz(UserDatumExportTaskInfoDao taskDao,
			UserAdhocDatumExportTaskInfoDao adhocTaskDao, UserNodeDao userNodeDao,
			DatumStreamMetadataDao metaDao) {
		super();
		this.taskDao = requireNonNullArgument(taskDao, "taskDao");
		this.adhocTaskDao = requireNonNullArgument(adhocTaskDao, "adhocTaskDao");
		this.userNodeDao = requireNonNullArgument(userNodeDao, "userNodeDao");
		this.metaDao = requireNonNullArgument(metaDao, "metaDao");
	}

	/**
	 * Filter a set of sources using a source ID path pattern.
	 * 
	 * <p>
	 * If any arguments are {@literal null}, or {@code pattern} is
	 * {@literal null} or empty, then {@code sources} will be returned without
	 * filtering. Otherwise a singleton set with just {@code pattern} will be
	 * returned.
	 * </p>
	 * 
	 * @param sources
	 *        the sources to filter
	 * @param pathMatcher
	 *        the path matcher to use
	 * @param pattern
	 *        the pattern to test
	 * @return the filtered sources
	 */
	public static Set<String> filterSources(Set<String> sources, PathMatcher pathMatcher,
			String pattern) {
		if ( sources == null || sources.isEmpty() || pathMatcher == null || pattern == null
				|| !pathMatcher.isPattern(pattern) ) {
			return (pattern == null || pattern.isEmpty() ? sources : Collections.singleton(pattern));
		}
		for ( Iterator<String> itr = sources.iterator(); itr.hasNext(); ) {
			String source = itr.next();
			if ( !pathMatcher.match(pattern, source) ) {
				itr.remove();
			}
		}
		return sources;
	}

	@Transactional(readOnly = false, propagation = Propagation.SUPPORTS)
	@Override
	public UserAdhocDatumExportTaskInfo submitAdhocDatumExportConfiguration(
			UserDatumExportConfiguration config) {
		ScheduleType scheduleType = ScheduleType.Adhoc;

		// set up the configuration for the task(s), in which we must resolve
		// the node IDs associated with the export
		BasicConfiguration taskConfig = new BasicConfiguration(config);
		taskConfig.setSchedule(scheduleType);
		BasicDataConfiguration taskDataConfig = new BasicDataConfiguration(
				taskConfig.getDataConfiguration());
		DatumFilterCommand taskDatumFilter = new DatumFilterCommand(taskDataConfig.getDatumFilter());

		if ( taskDatumFilter.getNodeId() == null ) {
			// set to all available node IDs
			Set<Long> nodeIds = userNodeDao.findNodeIdsForUser(config.getUserId());
			if ( nodeIds != null && !nodeIds.isEmpty() ) {
				taskDatumFilter.setNodeIds(nodeIds.toArray(new Long[nodeIds.size()]));
			} else {
				log.info("User {} has no nodes available for datum export", config.getUserId());
				return null;
			}
		}

		// if there is exactly one source ID, support pattern matching
		if ( taskDatumFilter.getSourceId() != null ) {
			Instant startDate = taskDatumFilter.getStartDate();
			Instant endDate = taskDatumFilter.getEndDate();

			Set<String> allSourceIds = new LinkedHashSet<>();
			BasicDatumCriteria filter = new BasicDatumCriteria();
			filter.setStartDate(startDate);
			filter.setEndDate(endDate);
			for ( Long nodeId : taskDatumFilter.getNodeIds() ) {
				filter.setNodeId(nodeId);
				Iterable<ObjectDatumStreamMetadata> results = metaDao.findDatumStreamMetadata(filter);
				Set<String> nodeSources = stream(results.spliterator(), false)
						.map(ObjectDatumStreamMetadata::getSourceId)
						.collect(toCollection(LinkedHashSet::new));
				if ( nodeSources != null ) {
					allSourceIds.addAll(nodeSources);
				}
			}
			Set<String> resolvedSourceIds = new LinkedHashSet<>(allSourceIds.size());
			for ( String sourceId : taskDatumFilter.getSourceIds() ) {
				Set<String> sources = filterSources(allSourceIds, this.pathMatcher, sourceId);
				if ( sources != null ) {
					resolvedSourceIds.addAll(sources);
				}
			}
			taskDatumFilter
					.setSourceIds(resolvedSourceIds.toArray(new String[resolvedSourceIds.size()]));
		}

		taskDataConfig.setDatumFilter(taskDatumFilter);
		taskConfig.setDataConfiguration(taskDataConfig);

		UserAdhocDatumExportTaskInfo task = new UserAdhocDatumExportTaskInfo();
		task.setCreated(Instant.now());
		task.setUserId(config.getUserId());
		task.setScheduleType(scheduleType);
		task.setConfig(taskConfig);
		task.setTokenId(SecurityUtils.currentTokenId());
		UUID pk = adhocTaskDao.store(task);
		task.setId(pk);
		return task;
	}

	@Transactional(readOnly = false, propagation = Propagation.SUPPORTS)
	@Override
	public UserDatumExportTaskInfo submitDatumExportConfiguration(UserDatumExportConfiguration config,
			Instant exportDate) {
		ScheduleType scheduleType = config.getSchedule();
		if ( scheduleType == null ) {
			throw new IllegalArgumentException("The schedule type is required.");
		}
		if ( scheduleType == ScheduleType.Adhoc ) {
			throw new IllegalArgumentException("The Adhoc schedule type is not allowed.");
		}
		// set up the configuration for the task(s), in which we must resolve
		// the node IDs associated with the export
		BasicConfiguration taskConfig = new BasicConfiguration(config);
		BasicDataConfiguration taskDataConfig = new BasicDataConfiguration(
				taskConfig.getDataConfiguration());
		DatumFilterCommand taskDatumFilter = new DatumFilterCommand(taskDataConfig.getDatumFilter());

		if ( taskDatumFilter.getNodeId() == null ) {
			// set to all available node IDs
			Set<Long> nodeIds = userNodeDao.findNodeIdsForUser(config.getUserId());
			if ( nodeIds != null && !nodeIds.isEmpty() ) {
				taskDatumFilter.setNodeIds(nodeIds.toArray(new Long[nodeIds.size()]));
			} else {
				log.info("User {} has no nodes available for datum export", config.getUserId());
				return null;
			}
		}
		ZoneId zone = (config.getTimeZoneId() != null ? ZoneId.of(config.getTimeZoneId())
				: ZoneOffset.UTC);
		ZonedDateTime currExportDate = scheduleType
				.exportDate(exportDate != null ? exportDate.atZone(zone) : ZonedDateTime.now(zone));
		ZonedDateTime nextExportDate = scheduleType.nextExportDate(currExportDate);

		if ( taskDatumFilter.getSourceId() != null ) {
			Set<String> allSourceIds = new LinkedHashSet<>();
			BasicDatumCriteria filter = new BasicDatumCriteria();
			filter.setStartDate(currExportDate.toInstant());
			filter.setEndDate(nextExportDate.toInstant());
			filter.setObjectKind(ObjectDatumKind.Node);
			for ( Long nodeId : taskDatumFilter.getNodeIds() ) {
				filter.setNodeId(nodeId);
				Iterable<ObjectDatumStreamMetadata> results = metaDao.findDatumStreamMetadata(filter);
				Set<String> nodeSources = stream(results.spliterator(), false)
						.map(ObjectDatumStreamMetadata::getSourceId)
						.collect(toCollection(LinkedHashSet::new));
				if ( nodeSources != null ) {
					allSourceIds.addAll(nodeSources);
				}
			}
			Set<String> resolvedSourceIds = new LinkedHashSet<>(allSourceIds.size());
			for ( String sourceId : taskDatumFilter.getSourceIds() ) {
				Set<String> sources = filterSources(allSourceIds, this.pathMatcher, sourceId);
				if ( sources != null ) {
					resolvedSourceIds.addAll(sources);
				}
			}
			taskDatumFilter
					.setSourceIds(resolvedSourceIds.toArray(new String[resolvedSourceIds.size()]));
		}

		taskDataConfig.setDatumFilter(taskDatumFilter);
		taskConfig.setDataConfiguration(taskDataConfig);

		UserDatumExportTaskInfo task = new UserDatumExportTaskInfo();
		task.setCreated(Instant.now());
		task.setUserId(config.getUserId());
		task.setExportDate(currExportDate.toInstant());
		task.setScheduleType(scheduleType);
		task.setUserDatumExportConfigurationId(config.getId());
		task.setConfig(taskConfig);
		UserDatumExportTaskPK pk = taskDao.store(task);
		task.setId(pk);
		return task;
	}

	/**
	 * Set a path matcher to resolve patterns against.
	 * 
	 * <p>
	 * If configured, this will be used to resolve source ID patterns.
	 * </p>
	 * 
	 * @param pathMatcher
	 *        the path matcher to use
	 */
	public void setPathMatcher(PathMatcher pathMatcher) {
		this.pathMatcher = pathMatcher;
	}

}
