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

import static java.util.stream.Collectors.toUnmodifiableMap;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.dao.UserRelatedIdentifiableConfigurationEntity;
import net.solarnetwork.central.datum.export.biz.DatumExportDestinationService;
import net.solarnetwork.central.datum.export.biz.DatumExportOutputFormatService;
import net.solarnetwork.central.datum.export.domain.DatumExportState;
import net.solarnetwork.central.datum.export.domain.DatumExportStatus;
import net.solarnetwork.central.datum.export.domain.OutputCompressionType;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.security.SecurityUtils;
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
import net.solarnetwork.central.user.export.domain.UserDestinationConfiguration;
import net.solarnetwork.central.user.export.domain.UserOutputConfiguration;
import net.solarnetwork.domain.BasicLocalizedServiceInfo;
import net.solarnetwork.domain.LocalizedServiceInfo;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.event.AppEvent;
import net.solarnetwork.event.AppEventHandler;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.SettingUtils;
import net.solarnetwork.util.StringUtils;

/**
 * DAO implementation of {@link UserExportBiz}.
 *
 * @author matt
 * @version 2.5
 */
public class DaoUserExportBiz implements UserExportBiz, AppEventHandler {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final UserDatumExportConfigurationDao datumExportConfigDao;
	private final UserDataConfigurationDao dataConfigDao;
	private final UserDestinationConfigurationDao destinationConfigDao;
	private final UserOutputConfigurationDao outputConfigDao;
	private final UserDatumExportTaskInfoDao taskDao;
	private final UserAdhocDatumExportTaskInfoDao adhocTaskDao;
	private final UserExportTaskBiz userExportTaskBiz;
	private final TextEncryptor textEncryptor;

	private final List<DatumExportOutputFormatService> outputFormatServices;
	private final List<DatumExportDestinationService> destinationServices;
	private final Map<String, Set<String>> serviceSecureKeys;

	private MessageSource messageSource;

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
	 * @param userExportTaskBiz
	 *        the export task service to use
	 * @param textEncryptor
	 *        the encryptor to handle sensitive properties with
	 * @param outputFormatServices
	 *        the output format services
	 * @param destinationServices
	 *        the destination services
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoUserExportBiz(UserDatumExportConfigurationDao datumExportConfigDao,
			UserDataConfigurationDao dataConfigDao, UserDestinationConfigurationDao destinationConfigDao,
			UserOutputConfigurationDao outputConfigDao, UserDatumExportTaskInfoDao taskDao,
			UserAdhocDatumExportTaskInfoDao adhocTaskDao, UserExportTaskBiz userExportTaskBiz,
			TextEncryptor textEncryptor, List<DatumExportOutputFormatService> outputFormatServices,
			List<DatumExportDestinationService> destinationServices) {
		super();
		this.datumExportConfigDao = requireNonNullArgument(datumExportConfigDao, "datumExportConfigDao");
		this.dataConfigDao = requireNonNullArgument(dataConfigDao, "dataConfigDao");
		this.destinationConfigDao = requireNonNullArgument(destinationConfigDao, "destinationConfigDao");
		this.outputConfigDao = requireNonNullArgument(outputConfigDao, "outputConfigDao");
		this.taskDao = requireNonNullArgument(taskDao, "taskDao");
		this.adhocTaskDao = requireNonNullArgument(adhocTaskDao, "adhocTaskDao");
		this.userExportTaskBiz = requireNonNullArgument(userExportTaskBiz, "userExportTaskBiz");
		this.textEncryptor = requireNonNullArgument(textEncryptor, "textEncryptor");
		this.outputFormatServices = requireNonNullArgument(outputFormatServices, "outputFormatServices");
		this.destinationServices = requireNonNullArgument(destinationServices, "destinationServices");

		// create a map of all services to their corresponding secure keys
		// we assume here that all integration and datum stream identifiers are globally unique
		this.serviceSecureKeys = Stream.of(outputFormatServices, destinationServices)
				.flatMap(Collection::stream).map(SettingSpecifierProvider.class::cast)
				.collect(toUnmodifiableMap(SettingSpecifierProvider::getSettingUid,
						s -> SettingUtils.secureKeys(s.getSettingSpecifiers())));
	}

	@Override
	public Iterable<DatumExportOutputFormatService> availableOutputFormatServices() {
		List<DatumExportOutputFormatService> svcs = this.outputFormatServices;
		return (svcs != null ? svcs : Collections.emptyList());
	}

	@Override
	public Iterable<DatumExportDestinationService> availableDestinationServices() {
		List<DatumExportDestinationService> svcs = this.destinationServices;
		return (svcs != null ? svcs : Collections.emptyList());
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
				desc = messageSource.getMessage("compressionType." + type.name() + ".desc", null, null,
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
				desc = messageSource.getMessage("scheduleType." + type.name() + ".desc", null, null,
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
				desc = messageSource.getMessage("aggregation." + type.name() + ".desc", null, null,
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
		return datumExportConfigDao.get(new UserLongCompositePK(userId, id), userId);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public Long saveDatumExportConfiguration(UserDatumExportConfiguration configuration) {
		if ( configuration.getMinimumExportDate() == null && configuration.getSchedule() != null ) {
			ZonedDateTime ts = Instant.now().atZone(configuration.zone());
			ts = configuration.getSchedule().exportDate(ts);
			configuration.setMinimumExportDate(ts.toInstant());
		}
		configuration.setTokenId(SecurityUtils.currentTokenId());
		return datumExportConfigDao.create(configuration.getUserId(), configuration).getEntityId();
	}

	@Transactional(propagation = Propagation.REQUIRED)
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
	public <T extends UserRelatedIdentifiableConfigurationEntity<?>> T configurationForUser(Long userId,
			Class<T> configurationClass, Long id) {
		final var pk = new UserLongCompositePK(userId, id);
		if ( UserDataConfiguration.class.isAssignableFrom(configurationClass) ) {
			return (T) dataConfigDao.get(pk, userId);
		} else if ( UserDestinationConfiguration.class.isAssignableFrom(configurationClass) ) {
			return (T) destinationConfigDao.get(pk, userId);
		} else if ( UserOutputConfiguration.class.isAssignableFrom(configurationClass) ) {
			return (T) outputConfigDao.get(pk, userId);
		}
		throw new IllegalArgumentException("Unsupported configurationClass: " + configurationClass);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public void deleteConfiguration(UserRelatedIdentifiableConfigurationEntity<?> configuration) {
		switch (configuration) {
			case UserDataConfiguration userDataConfiguration -> dataConfigDao
					.delete(userDataConfiguration);
			case UserDestinationConfiguration userDestinationConfiguration -> destinationConfigDao
					.delete(userDestinationConfiguration);
			case UserOutputConfiguration userOutputConfiguration -> outputConfigDao
					.delete(userOutputConfiguration);
			case null, default -> throw new IllegalArgumentException(
					"Unsupported configuration type: " + configuration);
		}
	}

	private <T extends BaseExportConfigurationEntity<?>> T mergeServiceProperties(T entity) {
		if ( entity == null ) {
			return null;
		}
		Map<String, Object> serviceProps = entity.getServiceProps();
		if ( serviceProps == null || serviceProps.isEmpty() ) {
			return entity;
		}
		serviceProps = new LinkedHashMap<>(serviceProps);

		BaseExportConfigurationEntity<?> existing = (entity.getId() != null
				? configurationForUser(entity.getUserId(), entity.getClass(), entity.getConfigId())
				: null);
		Map<String, Object> existingServiceProps = (existing != null ? existing.getServiceProps()
				: null);
		Set<String> secureEntrySettings = serviceSecureKeys.get(entity.getServiceIdentifier());
		for ( Iterator<Entry<String, Object>> propItr = serviceProps.entrySet().iterator(); propItr
				.hasNext(); ) {
			Entry<String, Object> prop = propItr.next();
			String propName = prop.getKey();
			String propStringVal = prop.getValue() != null ? prop.getValue().toString() : null;
			if ( secureEntrySettings.contains(propName) ) {
				if ( propStringVal != null && !propStringVal.isEmpty() && propStringVal.isBlank() ) {
					// provided non-empty but blank password: remove password from config
					propItr.remove();
				} else if ( propStringVal == null || propStringVal.isEmpty()
						|| StringUtils.DIGEST_PREFIX_PATTERN.matcher(propStringVal).matches() ) {
					// secure value is provided that is empty or is already a digest value; do not change existing value
					Object existingVal = (existingServiceProps != null
							? existingServiceProps.get(propName)
							: null);
					if ( existingVal != null && !existingVal.toString().isBlank() ) {
						serviceProps.put(propName, existingVal);
					} else {
						// remove blank property
						propItr.remove();
					}
				}
			} else if ( prop.getValue() == null || (propStringVal != null && propStringVal.isBlank()) ) {
				// remove blank property
				propItr.remove();
			}
		}

		if ( !serviceProps.equals(entity.getServiceProps()) ) {
			entity.setServiceProps(serviceProps);
		}

		// make sensitive properties
		entity.maskSensitiveInformation(serviceSecureKeys::get, textEncryptor);

		return entity;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public Long saveConfiguration(UserRelatedIdentifiableConfigurationEntity<?> configuration) {
		if ( configuration instanceof UserDataConfiguration ) {
			return dataConfigDao.save(mergeServiceProperties((UserDataConfiguration) configuration))
					.getEntityId();
		} else if ( configuration instanceof UserDestinationConfiguration ) {
			return destinationConfigDao
					.save(mergeServiceProperties((UserDestinationConfiguration) configuration))
					.getEntityId();
		} else if ( configuration instanceof UserOutputConfiguration ) {
			return outputConfigDao.save(mergeServiceProperties((UserOutputConfiguration) configuration))
					.getEntityId();
		}
		throw new IllegalArgumentException("Unsupported configuration: " + configuration);
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public <T extends UserRelatedIdentifiableConfigurationEntity<?>> List<T> configurationsForUser(
			Long userId, Class<T> configurationClass) {
		if ( UserDataConfiguration.class.isAssignableFrom(configurationClass) ) {
			return (List<T>) dataConfigDao.findConfigurationsForUser(userId);
		} else if ( UserDestinationConfiguration.class.isAssignableFrom(configurationClass) ) {
			return (List<T>) destinationConfigDao.findConfigurationsForUser(userId);
		} else if ( UserOutputConfiguration.class.isAssignableFrom(configurationClass) ) {
			return (List<T>) outputConfigDao.findConfigurationsForUser(userId);
		}
		throw new IllegalArgumentException("Unsupported configurationClass: " + configurationClass);
	}

	@Transactional(propagation = Propagation.SUPPORTS)
	@Override
	public UserDatumExportTaskInfo saveDatumExportTaskForConfiguration(
			UserDatumExportConfiguration configuration, Instant exportDate) {
		return userExportTaskBiz.submitDatumExportConfiguration(configuration, exportDate);
	}

	@Transactional(propagation = Propagation.SUPPORTS)
	@Override
	public UserAdhocDatumExportTaskInfo saveAdhocDatumExportTaskForConfiguration(
			UserDatumExportConfiguration config) {
		if ( config.getUserOutputConfiguration() != null ) {
			config.getUserOutputConfiguration().maskSensitiveInformation(serviceSecureKeys::get,
					textEncryptor);
		}
		if ( config.getUserDestinationConfiguration() != null ) {
			config.getUserDestinationConfiguration().maskSensitiveInformation(serviceSecureKeys::get,
					textEncryptor);
		}
		return userExportTaskBiz.submitAdhocDatumExportConfiguration(config);
	}

	@Transactional(propagation = Propagation.SUPPORTS)
	@Override
	public List<UserAdhocDatumExportTaskInfo> adhocExportTasksForUser(Long userId,
			Set<DatumExportState> states, Boolean success) {
		return adhocTaskDao.findTasksForUser(userId, states, success);
	}

	@Override
	public void handleEvent(AppEvent event) {
		if ( event == null || !DatumExportStatus.EVENT_TOPIC_JOB_STATUS_CHANGED.equals(event.getTopic())
				|| !(event.getProperty(DatumExportStatus.EVENT_PROP_JOB_STATE) instanceof Character
						&& event.getProperty(
								DatumExportStatus.EVENT_PROP_JOB_ID) instanceof String jobId) ) {
			return;
		}
		DatumExportState jobState = DatumExportState
				.forKey((char) event.getProperty(DatumExportStatus.EVENT_PROP_JOB_STATE));
		if ( jobState != DatumExportState.Completed ) {
			return;
		}

		// TODO: maybe rename this TASK_ID?
		UUID taskId = UUID.fromString(jobId);

		UserDatumExportTaskInfo info = taskDao.getForTaskId(taskId);
		if ( info == null || info.getConfig() == null
				|| info.getUserDatumExportConfigurationId() == null ) {
			log.info("Datum export job {} has no associated user export task", jobId);
			return;
		}

		boolean success = false;
		if ( event.getProperty(DatumExportStatus.EVENT_PROP_SUCCESS) instanceof Boolean ) {
			success = (Boolean) event.getProperty(DatumExportStatus.EVENT_PROP_SUCCESS);
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

		ZoneId zone = (info.getConfig().getTimeZoneId() != null
				? ZoneId.of(info.getConfig().getTimeZoneId())
				: ZoneOffset.UTC);
		ZonedDateTime nextExportDate = schedule.nextExportDate(info.getExportDate().atZone(zone));
		log.info("Updating user {} export task {} minimum epxort date to {} for job {}",
				info.getUserId(), info.getUserDatumExportConfigurationId(), nextExportDate, jobId);
		datumExportConfigDao.updateMinimumExportDate(info.getUserDatumExportConfigurationId(),
				info.getUserId(), nextExportDate.toInstant());
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

}
