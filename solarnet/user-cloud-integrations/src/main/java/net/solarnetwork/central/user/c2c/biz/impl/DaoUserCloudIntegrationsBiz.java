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

import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.dao.BasicFilter;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationsFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity;
import net.solarnetwork.central.dao.UserModifiableEnabledStatusDao;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;
import net.solarnetwork.central.support.ExceptionUtils;
import net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz;
import net.solarnetwork.central.user.c2c.domain.CloudIntegrationsConfigurationInput;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.dao.FilterableDao;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.domain.Result;
import net.solarnetwork.settings.support.SettingUtils;

/**
 * DAO based implementation of {@link UserCloudIntegrationsBiz}.
 *
 * @author matt
 * @version 1.0
 */
public class DaoUserCloudIntegrationsBiz implements UserCloudIntegrationsBiz {

	private final CloudIntegrationConfigurationDao integrationDao;
	private final CloudDatumStreamConfigurationDao datumStreamDao;
	private final CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao;
	private final Map<String, CloudIntegrationService> integrationServices;
	private final Map<String, Set<String>> integrationServiceSecureKeys;

	private Validator validator;

	/**
	 * Constructor.
	 *
	 * @param integrationDao
	 *        the configuration DAO
	 * @param datumStreamDao
	 *        the datum stream DAO
	 * @param datumStreamPropertyDao
	 *        the datum stream property DAO
	 * @param integrationServices
	 *        the integration services
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoUserCloudIntegrationsBiz(CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao,
			Collection<CloudIntegrationService> integrationServices) {
		super();
		this.integrationDao = requireNonNullArgument(integrationDao, "integrationDao");
		this.datumStreamDao = requireNonNullArgument(datumStreamDao, "datumStreamDao");
		this.datumStreamPropertyDao = requireNonNullArgument(datumStreamPropertyDao,
				"datumStreamPropertyDao");
		this.integrationServices = requireNonNullArgument(integrationServices, "integrationServices")
				.stream().collect(Collectors.toUnmodifiableMap(CloudIntegrationService::getId,
						Function.identity()));
		this.integrationServiceSecureKeys = integrationServices.stream()
				.collect(Collectors.toUnmodifiableMap(CloudIntegrationService::getId,
						s -> SettingUtils.secureKeys(s.getSettingSpecifiers())));

	}

	@Override
	public Iterable<CloudIntegrationService> availableIntegrationServices() {
		return integrationServices.values();
	}

	@Override
	public CloudIntegrationService integrationService(String identifier) {
		return integrationServices.get(identifier);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public <C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>> FilterResults<C, K> configurationsForUser(
			Long userId, CloudIntegrationsFilter filter, Class<C> configurationClass) {
		requireNonNullArgument(userId, "userId");
		requireNonNullArgument(configurationClass, "configurationClass");
		BasicFilter f = new BasicFilter(filter);
		f.setUserId(userId);
		FilterableDao<C, K, CloudIntegrationsFilter> dao = filterableDao(configurationClass);
		FilterResults<C, K> result = dao.findFiltered(f, f.getSorts(), f.getOffset(), f.getMax());

		// remove credentials before returning
		for ( C c : result ) {
			c.maskSensitiveInformation(integrationServiceSecureKeys::get);
		}

		return result;
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public <C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>> C configurationForId(
			K id, Class<C> configurationClass) {
		requireNonNullArgument(id, "id");
		requireNonNullArgument(id.getUserId(), "id.userId");
		requireNonNullArgument(configurationClass, "configurationClass");
		GenericDao<C, K> dao = genericDao(configurationClass);
		C result = requireNonNullObject(dao.get(id), id);

		// remove credentials before returning
		result.maskSensitiveInformation(integrationServiceSecureKeys::get);

		return result;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends CloudIntegrationsConfigurationInput<C, K>, C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>> C saveConfiguration(
			K id, T input) {
		requireNonNullArgument(id, "id");
		requireNonNullArgument(id.getUserId(), "id.userId");
		requireNonNullArgument(input, "input");

		validateInput(input);

		C config = input.toEntity(id);

		@SuppressWarnings("unchecked")
		GenericDao<C, K> dao = genericDao((Class<C>) config.getClass());
		K updatedId = requireNonNullObject(dao.save(config), id);
		C result = requireNonNullObject(dao.get(updatedId), updatedId);

		// remove credentials before returning
		result.maskSensitiveInformation(integrationServiceSecureKeys::get);

		return result;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>> void enableConfiguration(
			K id, boolean enabled, Class<C> configurationClass) {
		requireNonNullArgument(id, "id");
		requireNonNullArgument(id.getUserId(), "id.userId");
		requireNonNullArgument(configurationClass, "configurationClass");

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
	public Result<Void> validateIntegrationConfigurationForId(UserLongCompositePK id) {
		final CloudIntegrationConfiguration conf = requireNonNullObject(
				integrationDao.get(requireNonNullArgument(id, "id")), id);

		final CloudIntegrationService service = requireNonNullObject(
				integrationServices.get(conf.getServiceIdentifier()), conf.getServiceIdentifier());

		return service.validate(conf);
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
			result = (GenericDao<C, K>) (integrationDao);
		} else if ( CloudDatumStreamConfiguration.class.isAssignableFrom(clazz) ) {
			result = (GenericDao<C, K>) (datumStreamDao);
		} else if ( CloudDatumStreamPropertyConfiguration.class.isAssignableFrom(clazz) ) {
			result = (GenericDao<C, K>) (datumStreamPropertyDao);
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
			result = (UserModifiableEnabledStatusDao<F>) (integrationDao);
		} else if ( CloudDatumStreamConfiguration.class.isAssignableFrom(clazz) ) {
			result = (UserModifiableEnabledStatusDao<F>) (datumStreamDao);
		} else if ( CloudDatumStreamPropertyConfiguration.class.isAssignableFrom(clazz) ) {
			result = (UserModifiableEnabledStatusDao<F>) (datumStreamPropertyDao);
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
			result = (FilterableDao<C, K, F>) (integrationDao);
		} else if ( CloudDatumStreamConfiguration.class.isAssignableFrom(clazz) ) {
			result = (FilterableDao<C, K, F>) (datumStreamDao);
		} else if ( CloudDatumStreamPropertyConfiguration.class.isAssignableFrom(clazz) ) {
			result = (FilterableDao<C, K, F>) (datumStreamPropertyDao);
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
