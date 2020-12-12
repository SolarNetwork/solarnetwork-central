/* ==================================================================
 * DaoUserExportBiz.java - 25/03/2018 1:01:05 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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
import static net.solarnetwork.util.JodaDateUtils.fromJodaToInstant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.joda.time.DateTime;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.PathMatcher;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.export.biz.DatumExportDestinationService;
import net.solarnetwork.central.datum.export.biz.DatumExportOutputFormatService;
import net.solarnetwork.central.datum.export.domain.BasicConfiguration;
import net.solarnetwork.central.datum.export.domain.BasicDataConfiguration;
import net.solarnetwork.central.datum.export.domain.DatumExportState;
import net.solarnetwork.central.datum.export.domain.DatumExportStatus;
import net.solarnetwork.central.datum.export.domain.OutputCompressionType;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.export.biz.UserExportBiz;
import net.solarnetwork.central.user.export.biz.UserExportTaskBiz;
import net.solarnetwork.central.user.export.dao.UserAdhocDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.dao.UserDataConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserDatumExportConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserDatumExportTaskInfoDao;
import net.solarnetwork.central.user.export.dao.UserDestinationConfigurationDao;
import net.solarnetwork.central.user.export.dao.UserOutputConfigurationDao;
import net.solarnetwork.central.user.export.domain.BaseExportConfigurationEntity;
import net.solarnetwork.central.user.export.domain.UserAdhocDatumExportTaskInfo;
import net.solarnetwork.central.user.export.domain.UserDataConfiguration;
import net.solarnetwork.central.user.export.domain.UserDatumExportConfiguration;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskInfo;
import net.solarnetwork.central.user.export.domain.UserDatumExportTaskPK;
import net.solarnetwork.central.user.export.domain.UserDestinationConfiguration;
import net.solarnetwork.central.user.export.domain.UserIdentifiableConfiguration;
import net.solarnetwork.central.user.export.domain.UserOutputConfiguration;
import net.solarnetwork.domain.BasicLocalizedServiceInfo;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.settings.SettingSpecifier;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.OptionalServiceCollection;
import net.solarnetwork.util.StringUtils;

/**
 * DAO implementation of {@link UserExportBiz}.
 * 
 * @author matt
 * @version 1.2
 */
public class DaoUserExportBiz implements UserExportBiz, UserExportTaskBiz, EventHandler {

	private final UserDatumExportConfigurationDao datumExportConfigDao;
	private final UserDataConfigurationDao dataConfigDao;
	private final UserDestinationConfigurationDao destinationConfigDao;
	private final UserOutputConfigurationDao outputConfigDao;
	private final UserDatumExportTaskInfoDao taskDao;
	private final UserAdhocDatumExportTaskInfoDao adhocTaskDao;
	private final UserNodeDao userNodeDao;
	private final DatumStreamMetadataDao metaDao;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private OptionalServiceCollection<DatumExportOutputFormatService> outputFormatServices;
	private OptionalServiceCollection<DatumExportDestinationService> destinationServices;

	private MessageSource messageSource;
	private PathMatcher pathMatcher;

	/**
	 * Constructor.
	 * 
	 * @param datumExportConfigDao
	 *        the datum export configuration DAO to use
	 * @param dataConfigDao
	 *        the data configuration DAO to use
	 * @param destinationConfigDao
	 *        the destination configuration DAO to use
	 * @param outputConfigDao
	 *        the output configuration DAO to use
	 * @param taskDao
	 *        the task DAO to use
	 * @param adhocTaskDao
	 *        the ad hoc task DAO to use
	 * @param userNodeDao
	 *        the user node DAO to use
	 * @param metaDao
	 *        the metadata DAO to use
	 */
	public DaoUserExportBiz(UserDatumExportConfigurationDao datumExportConfigDao,
			UserDataConfigurationDao dataConfigDao, UserDestinationConfigurationDao destinationConfigDao,
			UserOutputConfigurationDao outputConfigDao, UserDatumExportTaskInfoDao taskDao,
			UserAdhocDatumExportTaskInfoDao adhocTaskDao, UserNodeDao userNodeDao,
			DatumStreamMetadataDao metaDao) {
		super();
		this.datumExportConfigDao = datumExportConfigDao;
		this.dataConfigDao = dataConfigDao;
		this.destinationConfigDao = destinationConfigDao;
		this.outputConfigDao = outputConfigDao;
		this.taskDao = taskDao;
		this.adhocTaskDao = adhocTaskDao;
		this.userNodeDao = userNodeDao;
		this.metaDao = metaDao;
	}

	@Override
	public Iterable<DatumExportOutputFormatService> availableOutputFormatServices() {
		OptionalServiceCollection<DatumExportOutputFormatService> svcs = this.outputFormatServices;
		return (svcs != null ? svcs.services() : Collections.emptyList());
	}

	@Override
	public Iterable<DatumExportDestinationService> availableDestinationServices() {
		OptionalServiceCollection<DatumExportDestinationService> svcs = this.destinationServices;
		return (svcs != null ? svcs.services() : Collections.emptyList());
	}

	@Override
	public Iterable<LocalizedServiceInfo> availableOutputCompressionTypes(Locale locale) {
		List<LocalizedServiceInfo> results = new ArrayList<>(OutputCompressionType.values().length);
		for ( OutputCompressionType type : OutputCompressionType.values() ) {
			String name = type.toString();
			String desc = null;
			if ( messageSource != null ) {
				name = messageSource.getMessage("compressionType." + type.name() + ".key", null, name,
						locale);
				desc = messageSource.getMessage("compressionType." + type.name() + ".desc", null, desc,
						locale);
			}
			results.add(new BasicLocalizedServiceInfo(String.valueOf(type.getKey()), locale, name, desc,
					null));
		}
		return results;
	}

	@Override
	public Iterable<LocalizedServiceInfo> availableScheduleTypes(Locale locale) {
		List<LocalizedServiceInfo> results = new ArrayList<>(ScheduleType.values().length);
		for ( ScheduleType type : ScheduleType.values() ) {
			if ( type == ScheduleType.Adhoc ) {
				// ad hoc not a supported user export type here
				continue;
			}
			String name = type.toString();
			String desc = null;
			if ( messageSource != null ) {
				name = messageSource.getMessage("scheduleType." + type.name() + ".key", null, name,
						locale);
				desc = messageSource.getMessage("scheduleType." + type.name() + ".desc", null, desc,
						locale);
			}
			results.add(new BasicLocalizedServiceInfo(String.valueOf(type.getKey()), locale, name, desc,
					null));
		}
		return results;
	}

	@Override
	public Iterable<LocalizedServiceInfo> availableAggregationTypes(Locale locale) {
		List<LocalizedServiceInfo> results = new ArrayList<>(ScheduleType.values().length);
		for ( Aggregation type : Aggregation.values() ) {
			String name = type.toString();
			String desc = null;
			if ( messageSource != null ) {
				name = messageSource.getMessage("aggregation." + type.name() + ".key", null, name,
						locale);
				desc = messageSource.getMessage("aggregation." + type.name() + ".desc", null, desc,
						locale);
			}
			results.add(new BasicLocalizedServiceInfo(String.valueOf(type.getKey()), locale, name, desc,
					null));
		}
		return results;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public UserDatumExportConfiguration datumExportConfigurationForUser(Long userId, Long id) {
		return datumExportConfigDao.get(id, userId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Long saveDatumExportConfiguration(UserDatumExportConfiguration configuration) {
		return datumExportConfigDao.store(configuration);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteDatumExportConfiguration(UserDatumExportConfiguration configuration) {
		datumExportConfigDao.delete(configuration);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public List<UserDatumExportConfiguration> datumExportsForUser(Long userId) {
		return datumExportConfigDao.findConfigurationsForUser(userId);
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public <T extends UserIdentifiableConfiguration> T configurationForUser(Long userId,
			Class<T> configurationClass, Long id) {
		if ( UserDataConfiguration.class.isAssignableFrom(configurationClass) ) {
			return (T) dataConfigDao.get(id, userId);
		} else if ( UserDestinationConfiguration.class.isAssignableFrom(configurationClass) ) {
			return (T) destinationConfigDao.get(id, userId);
		} else if ( UserOutputConfiguration.class.isAssignableFrom(configurationClass) ) {
			return (T) outputConfigDao.get(id, userId);
		}
		throw new IllegalArgumentException("Unsupported configurationClass: " + configurationClass);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteConfiguration(UserIdentifiableConfiguration configuration) {
		if ( configuration instanceof UserDataConfiguration ) {
			dataConfigDao.delete((UserDataConfiguration) configuration);
		} else if ( configuration instanceof UserDestinationConfiguration ) {
			destinationConfigDao.delete((UserDestinationConfiguration) configuration);
		} else if ( configuration instanceof UserOutputConfiguration ) {
			outputConfigDao.delete((UserOutputConfiguration) configuration);
		} else {
			throw new IllegalArgumentException("Unsupported configuration type: " + configuration);
		}
	}

	private Iterable<? extends SettingSpecifierProvider> providersForServiceProperties(
			Class<? extends UserIdentifiableConfiguration> configurationClass) {
		if ( UserDestinationConfiguration.class.isAssignableFrom(configurationClass) ) {
			return availableDestinationServices();
		} else if ( UserOutputConfiguration.class.isAssignableFrom(configurationClass) ) {
			return availableOutputFormatServices();
		}
		return Collections.emptyList();
	}

	private List<SettingSpecifier> settingsForService(String identifier,
			Iterable<? extends SettingSpecifierProvider> providers) {
		if ( identifier != null && providers != null ) {
			for ( SettingSpecifierProvider provider : providers ) {
				if ( identifier.equals(provider.getSettingUID()) ) {
					return provider.getSettingSpecifiers();
				}
			}
		}
		return Collections.emptyList();
	}

	private <T extends BaseExportConfigurationEntity> T mergeServiceProperties(T entity) {
		if ( entity == null || entity.getId() == null ) {
			return entity;
		}
		Map<String, Object> serviceProps = entity.getServiceProps();
		if ( serviceProps == null || serviceProps.isEmpty() ) {
			return entity;
		}
		BaseExportConfigurationEntity existing = configurationForUser(entity.getUserId(),
				entity.getClass(), entity.getId());
		if ( existing == null ) {
			return entity;
		}
		Map<String, Object> existingServiceProps = existing.getServiceProps();
		if ( existingServiceProps == null || existingServiceProps.isEmpty() ) {
			return entity;
		}
		Iterable<? extends SettingSpecifierProvider> providers = providersForServiceProperties(
				entity.getClass());
		List<SettingSpecifier> settings = settingsForService(entity.getServiceIdentifier(), providers);
		Set<String> secureEntrySettings = SettingUtils.secureKeys(settings);
		for ( String secureKey : secureEntrySettings ) {
			Object val = serviceProps.get(secureKey);
			String secureVal = (val != null ? val.toString() : "");
			if ( secureVal.isEmpty()
					|| StringUtils.DIGEST_PREFIX_PATTERN.matcher(secureVal).matches() ) {
				// secure value is provided that is empty or is already a digest value; do not change existing value
				Object existingVal = existingServiceProps.get(secureKey);
				if ( existingVal != null ) {
					serviceProps.put(secureKey, existingVal);
				}
			}
		}
		return entity;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public Long saveConfiguration(UserIdentifiableConfiguration configuration) {
		if ( configuration instanceof UserDataConfiguration ) {
			return dataConfigDao.store(mergeServiceProperties((UserDataConfiguration) configuration));
		} else if ( configuration instanceof UserDestinationConfiguration ) {
			return destinationConfigDao
					.store(mergeServiceProperties((UserDestinationConfiguration) configuration));
		} else if ( configuration instanceof UserOutputConfiguration ) {
			return outputConfigDao
					.store(mergeServiceProperties((UserOutputConfiguration) configuration));
		}
		throw new IllegalArgumentException("Unsupported configuration: " + configuration);
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public <T extends UserIdentifiableConfiguration> List<T> configurationsForUser(Long userId,
			Class<T> configurationClass) {
		if ( UserDataConfiguration.class.isAssignableFrom(configurationClass) ) {
			return (List<T>) dataConfigDao.findConfigurationsForUser(userId);
		} else if ( UserDestinationConfiguration.class.isAssignableFrom(configurationClass) ) {
			return (List<T>) destinationConfigDao.findConfigurationsForUser(userId);
		} else if ( UserOutputConfiguration.class.isAssignableFrom(configurationClass) ) {
			return (List<T>) outputConfigDao.findConfigurationsForUser(userId);
		}
		throw new IllegalArgumentException("Unsupported configurationClass: " + configurationClass);
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
	public UserDatumExportTaskInfo submitDatumExportConfiguration(UserDatumExportConfiguration config,
			DateTime exportDate) {
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

		DateTime currExportDate = scheduleType
				.exportDate(exportDate != null ? exportDate : new DateTime());
		DateTime nextExportDate = scheduleType.nextExportDate(currExportDate);

		if ( taskDatumFilter.getSourceId() != null ) {
			Set<String> allSourceIds = new LinkedHashSet<>();
			BasicDatumCriteria filter = new BasicDatumCriteria();
			filter.setStartDate(fromJodaToInstant(currExportDate));
			filter.setEndDate(fromJodaToInstant(nextExportDate));
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
		task.setCreated(new DateTime());
		task.setUserId(config.getUserId());
		task.setExportDate(currExportDate);
		task.setScheduleType(scheduleType);
		task.setUserDatumExportConfigurationId(config.getId());
		task.setConfig(taskConfig);
		UserDatumExportTaskPK pk = taskDao.store(task);
		task.setId(pk);
		return task;
	}

	@Transactional(readOnly = false, propagation = Propagation.SUPPORTS)
	@Override
	public UserDatumExportTaskInfo saveDatumExportTaskForConfiguration(
			UserDatumExportConfiguration configuration, DateTime exportDate) {
		return submitDatumExportConfiguration(configuration, exportDate);
	}

	@Transactional(readOnly = false, propagation = Propagation.SUPPORTS)
	@Override
	public UserAdhocDatumExportTaskInfo saveAdhocDatumExportTaskForConfiguration(
			UserDatumExportConfiguration config) {
		return submitAdhocDatumExportConfiguration(config);
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
			DateTime startDate = taskDatumFilter.getStartDate();
			DateTime endDate = taskDatumFilter.getEndDate();

			Set<String> allSourceIds = new LinkedHashSet<>();
			BasicDatumCriteria filter = new BasicDatumCriteria();
			filter.setStartDate(fromJodaToInstant(startDate));
			filter.setEndDate(fromJodaToInstant(endDate));
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
		task.setCreated(new DateTime());
		task.setUserId(config.getUserId());
		task.setScheduleType(scheduleType);
		task.setConfig(taskConfig);
		UUID pk = adhocTaskDao.store(task);
		task.setId(pk);
		return task;
	}

	@Transactional(readOnly = false, propagation = Propagation.SUPPORTS)
	@Override
	public List<UserAdhocDatumExportTaskInfo> adhocExportTasksForUser(Long userId,
			Set<DatumExportState> states, Boolean success) {
		return adhocTaskDao.findTasksForUser(userId, states, success);
	}

	@Override
	public void handleEvent(Event event) {
		if ( event == null
				|| !DatumExportStatus.EVENT_TOPIC_JOB_STATUS_CHANGED.equals(event.getTopic()) ) {
			return;
		}
		if ( !(event.getProperty(DatumExportStatus.EVENT_PROP_JOB_STATE) instanceof Character
				&& event.getProperty(DatumExportStatus.EVENT_PROP_JOB_ID) instanceof String) ) {
			return;
		}
		DatumExportState jobState = DatumExportState
				.forKey((char) event.getProperty(DatumExportStatus.EVENT_PROP_JOB_STATE));
		if ( jobState != DatumExportState.Completed ) {
			return;
		}

		String jobId = (String) event.getProperty(DatumExportStatus.EVENT_PROP_JOB_ID); // TODO: maybe rename this TASK_ID?
		UUID taskId = UUID.fromString(jobId);

		UserDatumExportTaskInfo info = taskDao.getForTaskId(taskId);
		if ( info == null || info.getConfig() == null
				|| info.getUserDatumExportConfigurationId() == null ) {
			log.info("Datum export job {} has no associated user export task", jobId);
			return;
		}

		boolean success = false;
		if ( event.getProperty(DatumExportStatus.EVENT_PROP_SUCCESS) instanceof Boolean ) {
			success = ((Boolean) event.getProperty(DatumExportStatus.EVENT_PROP_SUCCESS)).booleanValue();
		}
		if ( !success ) {
			log.info("Datum export job {} was not successful; not updating minimum export date: {}",
					jobId, event.getProperty(DatumExportStatus.EVENT_PROP_MESSAGE));
			return;
		}

		ScheduleType schedule = info.getConfig().getSchedule();
		if ( schedule == null ) {
			return;
		}

		DateTime nextExportDate = schedule.nextExportDate(info.getExportDate());
		log.info("Updating user {} export task {} minimum epxort date to {} for job {}",
				info.getUserId(), info.getUserDatumExportConfigurationId(), nextExportDate, jobId);
		datumExportConfigDao.updateMinimumExportDate(info.getUserDatumExportConfigurationId(),
				info.getUserId(), nextExportDate);
	}

	/**
	 * Set the optional output format services.
	 * 
	 * @param outputFormatServices
	 *        the optional services
	 */
	public void setOutputFormatServices(
			OptionalServiceCollection<DatumExportOutputFormatService> outputFormatServices) {
		this.outputFormatServices = outputFormatServices;
	}

	/**
	 * Set the optional destination services.
	 * 
	 * @param destinationServices
	 *        the optional services
	 */
	public void setDestinationServices(
			OptionalServiceCollection<DatumExportDestinationService> destinationServices) {
		this.destinationServices = destinationServices;
	}

	/**
	 * A message source for resolving messages with.
	 * 
	 * @param messageSource
	 *        the message source
	 */
	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
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
