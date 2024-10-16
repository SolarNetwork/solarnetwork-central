/* ==================================================================
 * DaoUserCloudIntegrationsBiz.java - 30/09/2024 11:24:58 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.c2c.biz.impl;

import static java.time.Instant.now;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.StreamSupport;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.dao.BasicFilter;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPollTaskDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPollTaskFilter;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationsFilter;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPollTaskEntity;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity;
import net.solarnetwork.central.dao.UserModifiableEnabledStatusDao;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;
import net.solarnetwork.central.support.ExceptionUtils;
import net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamPollTaskEntityInput;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamPropertyConfigurationInput;
import net.solarnetwork.central.user.c2c.domain.CloudIntegrationsConfigurationInput;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.dao.FilterableDao;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.settings.support.SettingUtils;

/**
 * DAO based implementation of {@link UserCloudIntegrationsBiz}.
 *
 * @author matt
 * @version 1.2
 */
public class DaoUserCloudIntegrationsBiz implements UserCloudIntegrationsBiz {

	private final CloudIntegrationConfigurationDao integrationDao;
	private final CloudDatumStreamConfigurationDao datumStreamDao;
	private final CloudDatumStreamMappingConfigurationDao datumStreamMappingDao;
	private final CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao;
	private final CloudDatumStreamPollTaskDao datumStreamPollTaskDao;
	private final TextEncryptor textEncryptor;
	private final Map<String, CloudIntegrationService> integrationServices;
	private final Map<String, CloudDatumStreamService> datumStreamServices;
	private final Map<String, Set<String>> integrationServiceSecureKeys;

	private Validator validator;

	/**
	 * Constructor.
	 *
	 * @param integrationDao
	 *        the configuration DAO
	 * @param datumStreamDao
	 *        the datum stream DAO
	 * @param datumStreamMappingDao
	 *        the datum stream mapping DAO
	 * @param datumStreamPropertyDao
	 *        the datum stream property DAO
	 * @param datumStreamPollTaskDao
	 *        the datum stream poll task DAO
	 * @param textEncryptor
	 *        the encryptor to handle sensitive properties with
	 * @param integrationServices
	 *        the integration services
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoUserCloudIntegrationsBiz(CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao,
			CloudDatumStreamPollTaskDao datumStreamPollTaskDao, TextEncryptor textEncryptor,
			Collection<CloudIntegrationService> integrationServices) {
		super();
		this.integrationDao = requireNonNullArgument(integrationDao, "integrationDao");
		this.datumStreamDao = requireNonNullArgument(datumStreamDao, "datumStreamDao");
		this.datumStreamMappingDao = requireNonNullArgument(datumStreamMappingDao,
				"datumStreamMappingDao");
		this.datumStreamPropertyDao = requireNonNullArgument(datumStreamPropertyDao,
				"datumStreamPropertyDao");
		this.datumStreamPollTaskDao = requireNonNullArgument(datumStreamPollTaskDao,
				"datumStreamPollTaskDao");
		this.textEncryptor = requireNonNullArgument(textEncryptor, "textEncryptor");
		this.integrationServices = requireNonNullArgument(integrationServices, "integrationServices")
				.stream()
				.collect(toUnmodifiableMap(CloudIntegrationService::getId, Function.identity()));
		this.datumStreamServices = integrationServices.stream()
				.flatMap(s -> StreamSupport.stream(s.datumStreamServices().spliterator(), false))
				.collect(toUnmodifiableMap(CloudDatumStreamService::getId, Function.identity()));
		this.integrationServiceSecureKeys = integrationServices.stream().collect(toUnmodifiableMap(
				CloudIntegrationService::getId, s -> SettingUtils.secureKeys(s.getSettingSpecifiers())));

	}

	@Override
	public Iterable<CloudIntegrationService> availableIntegrationServices() {
		return integrationServices.values();
	}

	@Override
	public CloudIntegrationService integrationService(String identifier) {
		return integrationServices.get(requireNonNullArgument(identifier, "identifier"));
	}

	@Override
	public CloudDatumStreamService datumStreamService(String identifier) {
		return datumStreamServices.get(requireNonNullArgument(identifier, "identifier"));
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public <C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>> FilterResults<C, K> listConfigurationsForUser(
			Long userId, CloudIntegrationsFilter filter, Class<C> configurationClass) {
		requireNonNullArgument(userId, "userId");
		requireNonNullArgument(configurationClass, "configurationClass");
		BasicFilter f = new BasicFilter(filter);
		f.setUserId(userId);
		FilterableDao<C, K, CloudIntegrationsFilter> dao = filterableDao(configurationClass);
		FilterResults<C, K> result = dao.findFiltered(f, f.getSorts(), f.getOffset(), f.getMax());
		return result;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public <C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>> C configurationForId(
			K id, Class<C> configurationClass) {
		requireNonNullArgument(id, "id");
		requireNonNullArgument(configurationClass, "configurationClass");
		if ( !id.userIdIsAssigned() ) {
			throw new IllegalArgumentException("The userId must be provided.");
		}

		GenericDao<C, K> dao = genericDao(configurationClass);
		C result = requireNonNullObject(dao.get(id), id);

		return result;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends CloudIntegrationsConfigurationInput<C, K>, C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>> C saveConfiguration(
			K id, T input) {
		requireNonNullArgument(id, "id");
		requireNonNullArgument(input, "input");
		if ( !id.userIdIsAssigned() ) {
			throw new IllegalArgumentException("The userId must be provided.");
		}

		validateInput(input);

		C config = input.toEntity(id);

		// make sensitive properties
		config.maskSensitiveInformation(integrationServiceSecureKeys::get, textEncryptor);

		@SuppressWarnings("unchecked")
		GenericDao<C, K> dao = genericDao((Class<C>) config.getClass());
		K updatedId = requireNonNullObject(dao.save(config), id);
		C result = requireNonNullObject(dao.get(updatedId), updatedId);

		return result;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public List<CloudDatumStreamPropertyConfiguration> replaceDatumStreamPropertyConfiguration(
			UserLongCompositePK datumStreamMappingId,
			List<CloudDatumStreamPropertyConfigurationInput> inputs) {
		requireNonNullArgument(datumStreamMappingId, "datumStreamMappingId");
		if ( !(datumStreamMappingId.userIdIsAssigned() && datumStreamMappingId.entityIdIsAssigned()) ) {
			throw new IllegalArgumentException("The userId and entityId components must be provided.");
		}

		requireNonNullArgument(inputs, "inputs");

		// delete all for given datum stream ID
		final UserLongIntegerCompositePK deleteId = UserLongIntegerCompositePK.unassignedEntityIdKey(
				datumStreamMappingId.getUserId(), datumStreamMappingId.getEntityId());
		datumStreamPropertyDao.delete(datumStreamPropertyDao.entityKey(deleteId));

		// then insert properties
		final Instant now = now();
		final var result = new ArrayList<CloudDatumStreamPropertyConfiguration>(inputs.size());
		int idx = 0;
		for ( var input : inputs ) {
			validateInput(input);
			var config = input.toEntity(new UserLongIntegerCompositePK(datumStreamMappingId.getUserId(),
					datumStreamMappingId.getEntityId(), idx++), now);
			datumStreamPropertyDao.save(config);
			result.add(config);
		}
		return result;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>> void updateConfigurationEnabled(
			K id, boolean enabled, Class<C> configurationClass) {
		requireNonNullArgument(id, "id");
		requireNonNullArgument(id.getUserId(), "id.userId");
		requireNonNullArgument(configurationClass, "configurationClass");
		if ( !id.userIdIsAssigned() ) {
			throw new IllegalArgumentException("The userId must be provided.");
		}

		BasicFilter filter = new BasicFilter();
		filter.setUserId(id.getUserId());
		if ( id instanceof UserLongCompositePK pk && pk.entityIdIsAssigned() ) {
			if ( CloudIntegrationConfiguration.class.isAssignableFrom(configurationClass) ) {
				filter.setIntegrationId(pk.getEntityId());
			} else if ( CloudDatumStreamConfiguration.class.isAssignableFrom(configurationClass) ) {
				filter.setDatumStreamId(pk.getEntityId());
			}
		} else if ( id instanceof UserLongIntegerCompositePK pk
				&& CloudDatumStreamPropertyConfiguration.class.isAssignableFrom(configurationClass) ) {
			filter.setDatumStreamId(pk.getGroupId());
			filter.setIndex(pk.getEntityId());
		}

		UserModifiableEnabledStatusDao<CloudIntegrationsFilter> dao = statusDao(configurationClass);
		dao.updateEnabledStatus(id.getUserId(), filter, enabled);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>> void deleteConfiguration(
			K id, Class<C> configurationClass) {
		requireNonNullArgument(id, "id");
		requireNonNullArgument(id.getUserId(), "id.userId");
		requireNonNullArgument(configurationClass, "configurationClass");
		GenericDao<C, K> dao = genericDao(configurationClass);
		C pk = dao.entityKey(id);
		dao.delete(pk);
	}

	@Override
	public Iterable<CloudDataValue> listDatumStreamDataValues(UserLongCompositePK id,
			Map<String, ?> filters) {
		var datumStream = requireNonNullObject(datumStreamDao.get(requireNonNullArgument(id, "id")),
				"datumStream");
		var service = requireNonNullObject(datumStreamService(datumStream.getServiceIdentifier()),
				"datumStreamService");
		return service.dataValues(id, filters);
	}

	@Override
	public Result<Void> validateIntegrationConfigurationForId(UserLongCompositePK id, Locale locale) {
		final CloudIntegrationConfiguration conf = requireNonNullObject(
				integrationDao.get(requireNonNullArgument(id, "id")), id);

		final CloudIntegrationService service = requireNonNullObject(
				integrationServices.get(conf.getServiceIdentifier()), conf.getServiceIdentifier());

		return service.validate(conf, Locale.getDefault());
	}

	@Override
	public Datum latestDatumStreamDatumForId(UserLongCompositePK id) {
		var datumStream = requireNonNullObject(datumStreamDao.get(requireNonNullArgument(id, "id")),
				"datumStream");
		var service = requireNonNullObject(datumStreamService(datumStream.getServiceIdentifier()),
				"datumStreamService");
		return service.latestDatum(datumStream);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public FilterResults<CloudDatumStreamPollTaskEntity, UserLongCompositePK> listDatumStreamPollTasksForUser(
			Long userId, CloudDatumStreamPollTaskFilter filter) {
		requireNonNullArgument(userId, "userId");
		BasicFilter f = new BasicFilter(filter);
		f.setUserId(userId);
		return datumStreamPollTaskDao.findFiltered(f, f.getSorts(), f.getOffset(), f.getMax());
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public CloudDatumStreamPollTaskEntity updateDatumStreamPollTaskState(UserLongCompositePK id,
			BasicClaimableJobState desiredState, BasicClaimableJobState... expectedStates) {
		requireNonNullArgument(id, "id");
		requireNonNullArgument(desiredState, "desiredState");
		if ( !id.allKeyComponentsAreAssigned() ) {
			throw new IllegalArgumentException(
					"The userId and datumStreamId components must be provided.");
		}
		// only update state if a user-settable value (start, stop)
		if ( desiredState == BasicClaimableJobState.Queued
				|| desiredState == BasicClaimableJobState.Completed ) {
			datumStreamPollTaskDao.updateTaskState(id, desiredState, expectedStates);
		}
		return datumStreamPollTaskDao.get(id);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public CloudDatumStreamPollTaskEntity saveDatumStreamPollTask(UserLongCompositePK id,
			CloudDatumStreamPollTaskEntityInput input, BasicClaimableJobState... expectedStates) {
		requireNonNullArgument(id, "id");
		requireNonNullArgument(input, "input");
		if ( !id.allKeyComponentsAreAssigned() ) {
			throw new IllegalArgumentException(
					"The userId and datumStreamId components must be provided.");
		}

		validateInput(input);

		CloudDatumStreamPollTaskEntity entity = input.toEntity(id);
		if ( expectedStates == null || expectedStates.length < 1 ) {
			datumStreamPollTaskDao.save(entity);
		} else {
			datumStreamPollTaskDao.updateTask(entity, expectedStates);
		}
		return datumStreamPollTaskDao.get(id);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteDatumStreamPollTask(UserLongCompositePK id) {
		requireNonNullArgument(id, "id");
		if ( !id.allKeyComponentsAreAssigned() ) {
			throw new IllegalArgumentException(
					"The userId and datumStreamId components must be provided.");
		}
		datumStreamPollTaskDao.delete(datumStreamPollTaskDao.entityKey(id));
	}

	@Override
	public CloudDatumStreamQueryResult listDatumStreamDatum(UserLongCompositePK id,
			CloudDatumStreamQueryFilter filter) {
		var datumStream = requireNonNullObject(datumStreamDao.get(requireNonNullArgument(id, "id")),
				"datumStream");
		var service = requireNonNullObject(datumStreamService(datumStream.getServiceIdentifier()),
				"datumStreamService");
		return service.datum(datumStream, filter);
	}

	private void validateInput(final Object input) {
		validateInput(input, getValidator());
	}

	private static void validateInput(final Object input, final Validator v) {
		if ( input == null || v == null ) {
			return;
		}
		var violations = v.validate(input);
		if ( violations == null || violations.isEmpty() ) {
			return;
		}
		BindingResult errors = ExceptionUtils
				.toBindingResult(new ConstraintViolationException(violations), v);
		if ( errors.hasErrors() ) {
			throw new ValidationException(errors);
		}
	}

	@SuppressWarnings("unchecked")
	private <C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>> GenericDao<C, K> genericDao(
			Class<C> clazz) {
		GenericDao<C, K> result = null;
		if ( CloudIntegrationConfiguration.class.isAssignableFrom(clazz) ) {
			result = (GenericDao<C, K>) integrationDao;
		} else if ( CloudDatumStreamConfiguration.class.isAssignableFrom(clazz) ) {
			result = (GenericDao<C, K>) datumStreamDao;
		} else if ( CloudDatumStreamMappingConfiguration.class.isAssignableFrom(clazz) ) {
			result = (GenericDao<C, K>) datumStreamMappingDao;
		} else if ( CloudDatumStreamPropertyConfiguration.class.isAssignableFrom(clazz) ) {
			result = (GenericDao<C, K>) datumStreamPropertyDao;
		}
		if ( result != null ) {
			return result;
		}
		throw new UnsupportedOperationException("Configuration type %s not supported.".formatted(clazz));
	}

	@SuppressWarnings("unchecked")
	private <F extends CloudIntegrationsFilter> UserModifiableEnabledStatusDao<F> statusDao(
			Class<?> clazz) {
		UserModifiableEnabledStatusDao<F> result = null;
		if ( CloudIntegrationConfiguration.class.isAssignableFrom(clazz) ) {
			result = (UserModifiableEnabledStatusDao<F>) integrationDao;
		} else if ( CloudDatumStreamConfiguration.class.isAssignableFrom(clazz) ) {
			result = (UserModifiableEnabledStatusDao<F>) datumStreamDao;
		} else if ( CloudDatumStreamPropertyConfiguration.class.isAssignableFrom(clazz) ) {
			result = (UserModifiableEnabledStatusDao<F>) datumStreamPropertyDao;
		}
		if ( result != null ) {
			return result;
		}
		throw new UnsupportedOperationException("Configuration type %s not supported.".formatted(clazz));
	}

	@SuppressWarnings("unchecked")
	private <C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>, F extends CloudIntegrationsFilter> FilterableDao<C, K, F> filterableDao(
			Class<C> clazz) {
		FilterableDao<C, K, F> result = null;
		if ( CloudIntegrationConfiguration.class.isAssignableFrom(clazz) ) {
			result = (FilterableDao<C, K, F>) integrationDao;
		} else if ( CloudDatumStreamConfiguration.class.isAssignableFrom(clazz) ) {
			result = (FilterableDao<C, K, F>) datumStreamDao;
		} else if ( CloudDatumStreamMappingConfiguration.class.isAssignableFrom(clazz) ) {
			result = (FilterableDao<C, K, F>) datumStreamMappingDao;
		} else if ( CloudDatumStreamPropertyConfiguration.class.isAssignableFrom(clazz) ) {
			result = (FilterableDao<C, K, F>) datumStreamPropertyDao;
		}
		if ( result != null ) {
			return result;
		}
		throw new UnsupportedOperationException("Configuration type %s not supported.".formatted(clazz));
	}

	/**
	 * Get the validator.
	 *
	 * @return the validator
	 */
	public Validator getValidator() {
		return validator;
	}

	/**
	 * Set the validator.
	 *
	 * @param validator
	 *        the validator to set
	 */
	public void setValidator(Validator validator) {
		this.validator = validator;
	}
}
