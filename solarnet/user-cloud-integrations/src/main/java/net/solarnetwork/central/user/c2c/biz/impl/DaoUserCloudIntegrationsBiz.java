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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.OAUTH_ACCESS_TOKEN_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.OAUTH_CLIENT_ID_SETTING;
import static net.solarnetwork.central.c2c.biz.CloudIntegrationService.OAUTH_REFRESH_TOKEN_SETTING;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.CollectionUtils.getMapString;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
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
import net.solarnetwork.central.c2c.dao.CloudDatumStreamSettingsEntityDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationsFilter;
import net.solarnetwork.central.c2c.dao.UserSettingsEntityDao;
import net.solarnetwork.central.c2c.domain.BasicCloudDatumStreamSettings;
import net.solarnetwork.central.c2c.domain.CloudDataValue;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPollTaskEntity;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamSettings;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamSettingsEntity;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsConfigurationEntity;
import net.solarnetwork.central.c2c.domain.UserSettingsEntity;
import net.solarnetwork.central.common.dao.ClientAccessTokenDao;
import net.solarnetwork.central.dao.UserModifiableEnabledStatusDao;
import net.solarnetwork.central.dao.UserRelatedStdIdentifiableConfigurationEntity;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserIdentifiableSystem;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserLongIntegerCompositePK;
import net.solarnetwork.central.domain.UserRelatedCompositeKey;
import net.solarnetwork.central.domain.UserStringStringCompositePK;
import net.solarnetwork.central.security.ClientAccessTokenEntity;
import net.solarnetwork.central.support.ExceptionUtils;
import net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamPollTaskEntityInput;
import net.solarnetwork.central.user.c2c.domain.CloudDatumStreamPropertyConfigurationInput;
import net.solarnetwork.central.user.c2c.domain.CloudIntegrationsConfigurationInput;
import net.solarnetwork.central.user.c2c.domain.UserSettingsEntityInput;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.dao.FilterableDao;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.settings.SettingSpecifierProvider;
import net.solarnetwork.settings.support.SettingUtils;

/**
 * DAO based implementation of {@link UserCloudIntegrationsBiz}.
 *
 * @author matt
 * @version 1.8
 */
public class DaoUserCloudIntegrationsBiz implements UserCloudIntegrationsBiz {

	/** The {@code defaultDatumStreamSettings} default value. */
	public static final CloudDatumStreamSettings DEFAULT_DATUM_STREAM_SETTINGS = new BasicCloudDatumStreamSettings(
			true, false);

	private final InstantSource clock;
	private final UserSettingsEntityDao userSettingsDao;
	private final CloudIntegrationConfigurationDao integrationDao;
	private final CloudDatumStreamConfigurationDao datumStreamDao;
	private final CloudDatumStreamSettingsEntityDao datumStreamSettingsDao;
	private final CloudDatumStreamMappingConfigurationDao datumStreamMappingDao;
	private final CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao;
	private final CloudDatumStreamPollTaskDao datumStreamPollTaskDao;
	private final ClientAccessTokenDao clientAccessTokenDao;
	private final TextEncryptor textEncryptor;
	private final Map<String, CloudIntegrationService> integrationServices;
	private final Map<String, CloudDatumStreamService> datumStreamServices;
	private final Map<String, Set<String>> serviceSecureKeys;

	private Validator validator;
	private CloudDatumStreamSettings defaultDatumStreamSettings = DEFAULT_DATUM_STREAM_SETTINGS;

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock
	 * @param userSettingsDao
	 *        the user settings DAO
	 * @param integrationDao
	 *        the configuration DAO
	 * @param datumStreamDao
	 *        the datum stream DAO
	 * @param datumStreamSettingsDao
	 *        the datum stream settings DAO
	 * @param datumStreamMappingDao
	 *        the datum stream mapping DAO
	 * @param datumStreamPropertyDao
	 *        the datum stream property DAO
	 * @param datumStreamPollTaskDao
	 *        the datum stream poll task DAO
	 * @param clientAccessTokenDao
	 *        the client access token DAO
	 * @param textEncryptor
	 *        the encryptor to handle sensitive properties with
	 * @param integrationServices
	 *        the integration services
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoUserCloudIntegrationsBiz(InstantSource clock, UserSettingsEntityDao userSettingsDao,
			CloudIntegrationConfigurationDao integrationDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamSettingsEntityDao datumStreamSettingsDao,
			CloudDatumStreamMappingConfigurationDao datumStreamMappingDao,
			CloudDatumStreamPropertyConfigurationDao datumStreamPropertyDao,
			CloudDatumStreamPollTaskDao datumStreamPollTaskDao,
			ClientAccessTokenDao clientAccessTokenDao, TextEncryptor textEncryptor,
			Collection<CloudIntegrationService> integrationServices) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		this.userSettingsDao = requireNonNullArgument(userSettingsDao, "userSettingsDao");
		this.integrationDao = requireNonNullArgument(integrationDao, "integrationDao");
		this.datumStreamDao = requireNonNullArgument(datumStreamDao, "datumStreamDao");
		this.datumStreamSettingsDao = requireNonNullArgument(datumStreamSettingsDao,
				"datumStreamSettingsDao");
		this.datumStreamMappingDao = requireNonNullArgument(datumStreamMappingDao,
				"datumStreamMappingDao");
		this.datumStreamPropertyDao = requireNonNullArgument(datumStreamPropertyDao,
				"datumStreamPropertyDao");
		this.datumStreamPollTaskDao = requireNonNullArgument(datumStreamPollTaskDao,
				"datumStreamPollTaskDao");
		this.clientAccessTokenDao = requireNonNullArgument(clientAccessTokenDao, "clientAccessTokenDao");
		this.textEncryptor = requireNonNullArgument(textEncryptor, "textEncryptor");
		this.integrationServices = Collections
				.unmodifiableMap(requireNonNullArgument(integrationServices, "integrationServices")
						.stream().sorted(Identity.sortByIdentity())
						.collect(Collectors.toMap(CloudIntegrationService::getId, Function.identity(),
								(l, r) -> l, LinkedHashMap::new)));
		this.datumStreamServices = Collections.unmodifiableMap(integrationServices.stream()
				.flatMap(s -> StreamSupport.stream(s.datumStreamServices().spliterator(), false))
				.sorted(Identity.sortByIdentity())
				.collect(Collectors.toMap(CloudDatumStreamService::getId, Function.identity(),
						(l, r) -> l, LinkedHashMap::new)));

		// create a map of all services to their corresponding secure keys
		// we assume here that all integration and datum stream identifiers are globally unique
		this.serviceSecureKeys = Stream.of(integrationServices, datumStreamServices.values())
				.flatMap(Collection::stream).map(SettingSpecifierProvider.class::cast)
				.collect(toUnmodifiableMap(SettingSpecifierProvider::getSettingUid,
						s -> SettingUtils.secureKeys(s.getSettingSpecifiers())));

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
	public UserSettingsEntity settingsForUser(Long userId) {
		return userSettingsDao.get(requireNonNullArgument(userId, "userId"));
	}

	@Override
	public UserSettingsEntity saveSettings(Long userId, UserSettingsEntityInput input) {
		UserSettingsEntity entity = requireNonNullArgument(input, "input").toEntity(userId, now());
		return userSettingsDao.get(userSettingsDao.save(entity));
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public void deleteSettings(Long userId) {
		UserSettingsEntity key = new UserSettingsEntity(requireNonNullArgument(userId, "userId"), now());
		userSettingsDao.delete(key);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public CloudDatumStreamSettings defaultDatumStreamSettings() {
		return defaultDatumStreamSettings;
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
		return digestSensitiveInformation(dao.findFiltered(f, f.getSorts(), f.getOffset(), f.getMax()));
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

		return digestSensitiveInformation(requireNonNullObject(dao.get(id), id));
	}

	@Transactional(propagation = Propagation.REQUIRED)
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

		// handle OAuth raw token values
		Map<String, Object> oauthTokenProperties = null;
		if ( config instanceof CloudIntegrationConfiguration integration
				&& integration.hasServiceProperty(OAUTH_CLIENT_ID_SETTING)
				&& integration.hasServiceProperty(OAUTH_ACCESS_TOKEN_SETTING)
				&& integration.hasServiceProperty(OAUTH_REFRESH_TOKEN_SETTING) ) {
			oauthTokenProperties = new HashMap<>(integration.getServiceProps());
		}

		// make sensitive properties
		config.maskSensitiveInformation(serviceSecureKeys::get, textEncryptor);

		@SuppressWarnings("unchecked")
		GenericDao<C, K> dao = genericDao((Class<C>) config.getClass());
		K updatedId = requireNonNullObject(dao.save(config), id);
		C result = requireNonNullObject(dao.get(updatedId), updatedId);

		if ( oauthTokenProperties != null ) {
			saveOAuthTokens((UserIdentifiableSystem) result, oauthTokenProperties);
		}

		return digestSensitiveInformation(result);
	}

	private void saveOAuthTokens(UserIdentifiableSystem config, Map<String, ?> oauthTokenProperties) {
		final String clientId = getMapString(OAUTH_CLIENT_ID_SETTING, oauthTokenProperties);
		final String accessTokenValue = getMapString(OAUTH_ACCESS_TOKEN_SETTING, oauthTokenProperties);
		final String refreshTokenValue = getMapString(OAUTH_REFRESH_TOKEN_SETTING, oauthTokenProperties);
		if ( clientId == null || accessTokenValue == null || refreshTokenValue == null ) {
			return;
		}

		// try to extract expiration date from JWT
		Instant accessTokenIssuedAt = null;
		Instant accessTokenExpiresAt = null;
		try {
			JWT jwt = JWTParser.parse(accessTokenValue);
			JWTClaimsSet claims = jwt.getJWTClaimsSet();
			if ( claims != null ) {
				Date exp = claims.getExpirationTime();
				if ( exp != null ) {
					accessTokenExpiresAt = exp.toInstant();
				}
				Date iat = claims.getIssueTime();
				if ( iat != null ) {
					accessTokenIssuedAt = iat.toInstant();
				}
			}
		} catch ( Exception e ) {
			// assume not a JWT, so abort as we have no
		}
		if ( accessTokenIssuedAt == null ) {
			accessTokenIssuedAt = clock.instant();
		}
		if ( accessTokenExpiresAt == null ) {
			accessTokenExpiresAt = accessTokenIssuedAt;
		}

		// try to extract issue date from JWT
		Instant refreshTokenIssuedAt = null;
		try {
			JWT jwt = JWTParser.parse(refreshTokenValue);
			JWTClaimsSet claims = jwt.getJWTClaimsSet();
			if ( claims != null ) {
				Date iat = claims.getIssueTime();
				if ( iat != null ) {
					refreshTokenIssuedAt = iat.toInstant();
				}
			}
		} catch ( Exception e ) {
			// assume not a JWT, so abort as we have no
		}
		if ( refreshTokenIssuedAt == null ) {
			refreshTokenIssuedAt = clock.instant();
		}

		var tokenId = new UserStringStringCompositePK(config.getUserId(), config.systemIdentifier(),
				clientId);
		var registration = new ClientAccessTokenEntity(tokenId, clock.instant());
		registration.setAccessTokenType("Bearer");
		registration.setAccessToken(accessTokenValue.getBytes(UTF_8));
		registration.setAccessTokenIssuedAt(accessTokenIssuedAt);
		registration.setAccessTokenExpiresAt(accessTokenExpiresAt);
		registration.setRefreshToken(refreshTokenValue.getBytes(UTF_8));
		registration.setRefreshTokenIssuedAt(refreshTokenIssuedAt);
		clientAccessTokenDao.save(registration);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public <C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>> C mergeConfigurationServiceProperties(
			K id, Map<String, ?> serviceProperties, Class<C> configurationClass) {
		requireNonNullArgument(id, "id");
		requireNonNullArgument(serviceProperties, "serviceProperties");
		if ( !id.userIdIsAssigned() ) {
			throw new IllegalArgumentException("The userId must be provided.");
		}

		// currently the only configuration this is supported on is Integration
		if ( !CloudIntegrationConfiguration.class.isAssignableFrom(configurationClass) ) {
			throw new UnsupportedOperationException(
					"Configuration class %s is not supported".formatted(configurationClass.getName()));
		}

		CloudIntegrationConfiguration config = integrationDao.get((UserLongCompositePK) id);

		// handle OAuth raw token values
		Map<String, Object> oauthTokenProperties = null;
		if ( serviceProperties.get(OAUTH_ACCESS_TOKEN_SETTING) != null
				&& serviceProperties.get(OAUTH_REFRESH_TOKEN_SETTING) != null ) {
			oauthTokenProperties = new LinkedHashMap<>(serviceProperties);
			oauthTokenProperties.put(OAUTH_CLIENT_ID_SETTING,
					config.serviceProperty(OAUTH_CLIENT_ID_SETTING, String.class));
		}

		if ( config.getServiceProps() == null ) {
			config.setServiceProps(new LinkedHashMap<>(serviceProperties));
		} else {
			var props = new LinkedHashMap<>(config.getServiceProps());
			props.putAll(serviceProperties);
			config.setServiceProps(props);
		}

		// make sensitive properties
		config.maskSensitiveInformation(serviceSecureKeys::get, textEncryptor);

		Map<String, Object> propsToMerge = new LinkedHashMap<>(serviceProperties.size());
		for ( Entry<String, Object> e : config.getServiceProps().entrySet() ) {
			String key = e.getKey();
			if ( !serviceProperties.containsKey(key) ) {
				continue;
			}
			propsToMerge.put(key, e.getValue());
		}

		integrationDao.mergeServiceProperties(config.getId(), propsToMerge);

		@SuppressWarnings("unchecked")
		C result = (C) integrationDao.get(config.getId());

		if ( oauthTokenProperties != null ) {
			saveOAuthTokens((UserIdentifiableSystem) result, oauthTokenProperties);
		}

		return digestSensitiveInformation(result);
	}

	@Transactional(propagation = Propagation.REQUIRED)
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

	@Transactional(propagation = Propagation.REQUIRED)
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

	@Transactional(propagation = Propagation.REQUIRED)
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
	public Iterable<CloudDataValue> listDatumStreamDataValues(UserLongCompositePK integrationId,
			String datumStreamServiceIdentifier, Map<String, ?> filters) {
		var integration = requireNonNullObject(
				integrationDao.get(requireNonNullArgument(integrationId, "integrationId")),
				"integration");
		var service = requireNonNullObject(resolveDatumStreamService(integration.getServiceIdentifier(),
				datumStreamServiceIdentifier), "datumStreamService");
		return service.dataValues(integration.getId(), filters);
	}

	private CloudDatumStreamService resolveDatumStreamService(String integrationServiceIdentifier,
			String datumStreamServiceIdentifier) {
		CloudDatumStreamService result = null;
		if ( datumStreamServiceIdentifier != null ) {
			result = datumStreamServices.get(datumStreamServiceIdentifier);
			if ( result != null && integrationServiceIdentifier != null ) {
				// verify service belongs to integration
				CloudIntegrationService integrationService = integrationServices
						.get(integrationServiceIdentifier);
				if ( integrationService != null ) {
					boolean found = false;
					for ( CloudDatumStreamService service : integrationService.datumStreamServices() ) {
						if ( service.getId().equals(datumStreamServiceIdentifier) ) {
							found = true;
							break;
						}
					}
					if ( !found ) {
						throw new IllegalArgumentException(
								"CloudDatumStreamService [%s] not supported by CloudIntegrationService [%s]"
										.formatted(datumStreamServiceIdentifier,
												integrationService.getId()));
					}
				}
			}
		} else if ( integrationServiceIdentifier != null ) {
			// get first provided by integration
			CloudIntegrationService integrationService = integrationServices
					.get(integrationServiceIdentifier);
			if ( integrationService != null ) {
				result = integrationService.datumStreamServices().iterator().next();
			}
		}
		return result;
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
	public Iterable<Datum> latestDatumStreamDatumForId(UserLongCompositePK id) {
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

	@Transactional(propagation = Propagation.REQUIRED)
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

	@Transactional(propagation = Propagation.REQUIRED)
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

	@Transactional(propagation = Propagation.REQUIRED)
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
		} else if ( CloudDatumStreamSettingsEntity.class.isAssignableFrom(clazz) ) {
			result = (GenericDao<C, K>) datumStreamSettingsDao;
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
		} else if ( CloudDatumStreamSettingsEntity.class.isAssignableFrom(clazz) ) {
			result = (FilterableDao<C, K, F>) datumStreamSettingsDao;
		}
		if ( result != null ) {
			return result;
		}
		throw new UnsupportedOperationException("Configuration type %s not supported.".formatted(clazz));
	}

	private <C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>> C digestSensitiveInformation(
			C entity) {
		if ( entity == null ) {
			return entity;
		}
		if ( entity instanceof UserRelatedStdIdentifiableConfigurationEntity<?, ?> u ) {
			u.digestSensitiveInformation(serviceSecureKeys::get);
		}
		return entity;
	}

	private <C extends CloudIntegrationsConfigurationEntity<C, K>, K extends UserRelatedCompositeKey<K>> FilterResults<C, K> digestSensitiveInformation(
			FilterResults<C, K> results) {
		if ( results == null || results.getReturnedResultCount() < 1 ) {
			return results;
		}
		for ( C entity : results ) {
			digestSensitiveInformation(entity);
		}
		return results;
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

	/**
	 * Get the default datum stream settings.
	 *
	 * @return the settings, never {@literal null}
	 * @since 1.4
	 */
	public final CloudDatumStreamSettings getDefaultDatumStreamSettings() {
		return defaultDatumStreamSettings;
	}

	/**
	 * Set the default datum stream settings.
	 *
	 * @param defaultDatumStreamSettings
	 *        the settings to set; if {@code null} then
	 *        {@link #DEFAULT_DATUM_STREAM_SETTINGS} will be used
	 * @since 1.4
	 */
	public final void setDefaultDatumStreamSettings(
			CloudDatumStreamSettings defaultDatumStreamSettings) {
		this.defaultDatumStreamSettings = (defaultDatumStreamSettings != null
				? defaultDatumStreamSettings
				: DEFAULT_DATUM_STREAM_SETTINGS);
	}

}
