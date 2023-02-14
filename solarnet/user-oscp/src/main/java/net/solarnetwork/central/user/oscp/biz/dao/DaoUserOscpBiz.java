/* ==================================================================
 * DaoUserOscpBiz.java - 15/08/2022 10:40:52 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.oscp.biz.dao;

import static net.solarnetwork.central.domain.UserLongCompositePK.unassignedEntityIdKey;
import static net.solarnetwork.central.security.AuthorizationException.requireNonNullObject;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.biz.SecretsBiz;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.AssetConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityGroupConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityGroupSettingsDao;
import net.solarnetwork.central.oscp.dao.CapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.CapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.FlexibilityProviderDao;
import net.solarnetwork.central.oscp.dao.UserSettingsDao;
import net.solarnetwork.central.oscp.domain.AssetConfiguration;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;
import net.solarnetwork.central.oscp.domain.BaseOscpExternalSystemConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityGroupConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityGroupSettings;
import net.solarnetwork.central.oscp.domain.CapacityOptimizerConfiguration;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.ExternalSystemServiceProperties;
import net.solarnetwork.central.oscp.domain.OAuthClientSettings;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.UserSettings;
import net.solarnetwork.central.oscp.util.AuthRoleSecretKeyFormatter;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.user.oscp.biz.UserOscpBiz;
import net.solarnetwork.central.user.oscp.domain.AssetConfigurationInput;
import net.solarnetwork.central.user.oscp.domain.CapacityGroupConfigurationInput;
import net.solarnetwork.central.user.oscp.domain.CapacityGroupSettingsInput;
import net.solarnetwork.central.user.oscp.domain.CapacityOptimizerConfigurationInput;
import net.solarnetwork.central.user.oscp.domain.CapacityProviderConfigurationInput;
import net.solarnetwork.central.user.oscp.domain.UserSettingsInput;

/**
 * DAO implementation of {@link UserOscpBiz}.
 * 
 * @author matt
 * @version 1.1
 */
public class DaoUserOscpBiz implements UserOscpBiz {

	private final UserSettingsDao userSettingsDao;
	private final FlexibilityProviderDao flexibilityProviderDao;
	private final CapacityProviderConfigurationDao capacityProviderDao;
	private final CapacityOptimizerConfigurationDao capacityOptimizerDao;
	private final CapacityGroupConfigurationDao capacityGroupDao;
	private final CapacityGroupSettingsDao capacityGroupSettingsDao;
	private final AssetConfigurationDao assetDao;

	private SecretsBiz secretsBiz;
	private Function<AuthRoleInfo, String> configSecretsNameFormatter = AuthRoleSecretKeyFormatter.INSTANCE;

	/**
	 * Constructor.
	 * 
	 * @param userSettingsDao
	 *        the user settings DAO
	 * @param flexibilityProviderDao
	 *        the flexibility provider DAO
	 * @param capacityProviderDao
	 *        the capacity provider DAO
	 * @param capcityOptimizerDao
	 *        the capacity optimizer DAO
	 * @param capacityGroupDao
	 *        the capacity group DAO
	 * @param capacityGroupSettingsDao
	 *        the capacity group settings DAO
	 * @param assetDao
	 *        the asset DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoUserOscpBiz(UserSettingsDao userSettingsDao, FlexibilityProviderDao flexibilityProviderDao,
			CapacityProviderConfigurationDao capacityProviderDao,
			CapacityOptimizerConfigurationDao capacityOptimizerDao,
			CapacityGroupConfigurationDao capacityGroupDao,
			CapacityGroupSettingsDao capacityGroupSettingsDao, AssetConfigurationDao assetDao) {
		super();
		this.userSettingsDao = requireNonNullArgument(userSettingsDao, "userSettingsDao");
		this.flexibilityProviderDao = requireNonNullArgument(flexibilityProviderDao,
				"flexibilityProviderDao");
		this.capacityProviderDao = requireNonNullArgument(capacityProviderDao, "capacityProviderDao");
		this.capacityOptimizerDao = requireNonNullArgument(capacityOptimizerDao, "capacityOptimizerDao");
		this.capacityGroupDao = requireNonNullArgument(capacityGroupDao, "capacityGroupDao");
		this.capacityGroupSettingsDao = requireNonNullArgument(capacityGroupSettingsDao,
				"capacityGroupSettingsDao");
		this.assetDao = requireNonNullArgument(assetDao, "assetDao");
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public UserSettings settingsForUser(Long userId) {
		return userSettingsDao.get(userId);
	}

	private <T extends BaseOscpExternalSystemConfiguration<C>, C extends BaseOscpExternalSystemConfiguration<C>> T withoutSensitiveProperties(
			T entity) {
		if ( entity.getServiceProps() != null ) {
			Map<String, Object> props = entity.getServiceProps();

			// remove any OAuth client secret
			props.remove(ExternalSystemServiceProperties.OAUTH_CLIENT_SECRET);
		}
		return entity;
	}

	private <T extends BaseOscpExternalSystemConfiguration<C>, C extends BaseOscpExternalSystemConfiguration<C>> Collection<T> withoutSensitiveProperties(
			Collection<T> entities) {
		for ( T entity : entities ) {
			withoutSensitiveProperties(entity);
		}
		return entities;
	}

	private CapacityProviderConfiguration capacityProviderForUser(UserLongCompositePK pk) {
		return withoutSensitiveProperties(requireNonNullObject(capacityProviderDao.get(pk), pk));
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public CapacityProviderConfiguration capacityProviderForUser(Long userId, Long entityId) {
		return capacityProviderForUser(new UserLongCompositePK(userId, entityId));
	}

	private CapacityOptimizerConfiguration capacityOptimizerForUser(UserLongCompositePK pk) {
		return withoutSensitiveProperties(requireNonNullObject(capacityOptimizerDao.get(pk), pk));
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public CapacityOptimizerConfiguration capacityOptimizerForUser(Long userId, Long entityId) {
		return capacityOptimizerForUser(new UserLongCompositePK(userId, entityId));
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public CapacityGroupConfiguration capacityGroupForUser(Long userId, Long entityId) {
		return requireNonNullObject(capacityGroupDao.get(new UserLongCompositePK(userId, entityId)),
				entityId);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Collection<CapacityGroupSettings> capacityGroupSettingsForUser(Long userId) {
		return capacityGroupSettingsDao.findAll(userId, null);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public CapacityGroupSettings capacityGroupSettingsForUser(Long userId, Long entityId) {
		return capacityGroupSettingsDao.get(new UserLongCompositePK(userId, entityId));
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public AssetConfiguration assetForUser(Long userId, Long entityId) {
		return requireNonNullObject(assetDao.get(new UserLongCompositePK(userId, entityId)), entityId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteUserSettings(Long userId) {
		userSettingsDao.delete(new UserSettings(userId));
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteCapacityProvider(Long userId, Long entityId) {
		capacityProviderDao.delete(new CapacityProviderConfiguration(userId, entityId, Instant.now()));
		deleteOauthClientSecret(OscpRole.CapacityProvider, userId, entityId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteCapacityOptimizer(Long userId, Long entityId) {
		capacityOptimizerDao.delete(new CapacityOptimizerConfiguration(userId, entityId, Instant.now()));
		deleteOauthClientSecret(OscpRole.CapacityOptimizer, userId, entityId);
	}

	private void deleteOauthClientSecret(OscpRole role, Long userId, Long entityId) {
		if ( secretsBiz != null ) {
			AuthRoleInfo authRole = new AuthRoleInfo(new UserLongCompositePK(userId, entityId), role);
			String secretName = configSecretsNameFormatter.apply(authRole);
			secretsBiz.deleteSecret(secretName);
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteCapacityGroup(Long userId, Long entityId) {
		capacityGroupDao.delete(new CapacityGroupConfiguration(userId, entityId, Instant.now()));
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteCapacityGroupSettings(Long userId, Long entityId) {
		capacityGroupSettingsDao.delete(new CapacityGroupSettings(userId, entityId));
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteAsset(Long userId, Long entityId) {
		assetDao.delete(new AssetConfiguration(userId, entityId, Instant.now()));
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Collection<CapacityProviderConfiguration> capacityProvidersForUser(Long userId) {
		return withoutSensitiveProperties(
				capacityProviderDao.findAll(requireNonNullArgument(userId, "userId"), null));
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Collection<CapacityOptimizerConfiguration> capacityOptimizersForUser(Long userId) {
		return withoutSensitiveProperties(
				capacityOptimizerDao.findAll(requireNonNullArgument(userId, "userId"), null));
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Collection<CapacityGroupConfiguration> capacityGroupsForUser(Long userId) {
		return capacityGroupDao.findAll(requireNonNullArgument(userId, "userId"), null);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Collection<AssetConfiguration> assetsForUser(Long userId) {
		return assetDao.findAll(requireNonNullArgument(userId, "userId"), null);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	@Override
	public Collection<AssetConfiguration> assetsForUserCapacityGroup(Long userId, Long groupId) {
		return assetDao.findAllForCapacityGroup(requireNonNullArgument(userId, "userId"),
				requireNonNullArgument(groupId, "groupId"), null);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public CapacityProviderConfiguration createCapacityProvider(Long userId,
			CapacityProviderConfigurationInput input) throws AuthorizationException {
		UserLongCompositePK unassignedId = unassignedEntityIdKey(
				requireNonNullArgument(userId, "userId"));

		CapacityProviderConfiguration conf = input.toEntity(unassignedId);

		OAuthClientSettings oauthSettings = null;
		if ( conf.hasOauthClientSettings() ) {
			oauthSettings = conf.oauthClientSettings();
			if ( oauthSettings.clientSecret() != null && secretsBiz != null ) {
				// have to save this in SecretsBiz so clear from props
				conf.getServiceProps().remove(ExternalSystemServiceProperties.OAUTH_CLIENT_SECRET);
			}
		}

		// create auth token
		String token = requireNonNullObject(flexibilityProviderDao.createAuthToken(unassignedId),
				"token");
		UserLongCompositePK authId = requireNonNullObject(
				flexibilityProviderDao.idForToken(token, false), "authId");

		conf.setFlexibilityProviderId(authId.getEntityId());
		UserLongCompositePK pk = capacityProviderDao.create(userId, conf);

		if ( oauthSettings != null && oauthSettings.clientSecret() != null && secretsBiz != null ) {
			AuthRoleInfo role = new AuthRoleInfo(pk, OscpRole.CapacityProvider);
			String secretName = configSecretsNameFormatter.apply(role);
			secretsBiz.putSecret(secretName, oauthSettings.asMap());
		}

		CapacityProviderConfiguration result = capacityProviderDao.get(pk);
		result.setToken(token);
		return result;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public CapacityOptimizerConfiguration createCapacityOptimizer(Long userId,
			CapacityOptimizerConfigurationInput input) throws AuthorizationException {
		UserLongCompositePK unassignedId = unassignedEntityIdKey(
				requireNonNullArgument(userId, "userId"));

		CapacityOptimizerConfiguration conf = input.toEntity(unassignedId);

		OAuthClientSettings oauthSettings = null;
		if ( conf.hasOauthClientSettings() ) {
			oauthSettings = conf.oauthClientSettings();
			if ( oauthSettings.clientSecret() != null && secretsBiz != null ) {
				// have to save this in SecretsBiz so clear from props
				conf.getServiceProps().remove(ExternalSystemServiceProperties.OAUTH_CLIENT_SECRET);
			}
		}

		// create auth token
		String token = requireNonNullObject(flexibilityProviderDao.createAuthToken(unassignedId),
				"token");
		UserLongCompositePK authId = requireNonNullObject(
				flexibilityProviderDao.idForToken(token, false), "authId");

		conf.setFlexibilityProviderId(authId.getEntityId());
		UserLongCompositePK pk = capacityOptimizerDao.create(userId, conf);

		if ( oauthSettings != null && oauthSettings.clientSecret() != null && secretsBiz != null ) {
			AuthRoleInfo role = new AuthRoleInfo(pk, OscpRole.CapacityProvider);
			String secretName = configSecretsNameFormatter.apply(role);
			secretsBiz.putSecret(secretName, oauthSettings.asMap());
		}

		CapacityOptimizerConfiguration result = capacityOptimizerDao.get(pk);
		result.setToken(token);
		return result;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public CapacityGroupConfiguration createCapacityGroup(Long userId,
			CapacityGroupConfigurationInput input) throws AuthorizationException {
		CapacityGroupConfiguration conf = input
				.toEntity(unassignedEntityIdKey(requireNonNullArgument(userId, "userId")));
		UserLongCompositePK pk = capacityGroupDao.create(userId, conf);
		return capacityGroupDao.get(pk);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public AssetConfiguration createAsset(Long userId, AssetConfigurationInput input)
			throws AuthorizationException {
		AssetConfiguration conf = input
				.toEntity(unassignedEntityIdKey(requireNonNullArgument(userId, "userId")));
		UserLongCompositePK pk = assetDao.create(userId, conf);
		return assetDao.get(pk);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public UserSettings updateUserSettings(Long userId, UserSettingsInput input)
			throws AuthorizationException {
		UserSettings settings = input.toEntity(requireNonNullArgument(userId, "userId"));
		Long pk = requireNonNullObject(userSettingsDao.save(settings), userId);
		return userSettingsDao.get(pk);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public CapacityProviderConfiguration updateCapacityProvider(Long userId, Long entityId,
			CapacityProviderConfigurationInput input) throws AuthorizationException {
		CapacityProviderConfiguration conf = input.toEntity(new UserLongCompositePK(
				requireNonNullArgument(userId, "userId"), requireNonNullArgument(entityId, "entityId")));

		saveOauthClientSecret(conf);

		UserLongCompositePK pk = requireNonNullObject(capacityProviderDao.save(conf), entityId);
		return capacityProviderForUser(pk);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public CapacityOptimizerConfiguration updateCapacityOptimizer(Long userId, Long entityId,
			CapacityOptimizerConfigurationInput input) throws AuthorizationException {
		CapacityOptimizerConfiguration conf = input.toEntity(new UserLongCompositePK(
				requireNonNullArgument(userId, "userId"), requireNonNullArgument(entityId, "entityId")));

		saveOauthClientSecret(conf);

		UserLongCompositePK pk = requireNonNullObject(capacityOptimizerDao.save(conf), entityId);
		return capacityOptimizerForUser(pk);
	}

	private void saveOauthClientSecret(BaseOscpExternalSystemConfiguration<?> conf) {
		if ( conf.hasOauthClientSettings() ) {
			OAuthClientSettings oauthSettings = conf.oauthClientSettings();
			if ( oauthSettings.clientSecret() != null && secretsBiz != null ) {
				AuthRoleInfo role = conf.getAuthRole();
				String secretName = configSecretsNameFormatter.apply(role);
				secretsBiz.putSecret(secretName, oauthSettings.asMap());
				conf.getServiceProps().remove(ExternalSystemServiceProperties.OAUTH_CLIENT_SECRET);
			}
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public CapacityGroupConfiguration updateCapacityGroup(Long userId, Long entityId,
			CapacityGroupConfigurationInput input) throws AuthorizationException {
		CapacityGroupConfiguration conf = input.toEntity(new UserLongCompositePK(
				requireNonNullArgument(userId, "userId"), requireNonNullArgument(entityId, "entityId")));
		UserLongCompositePK pk = requireNonNullObject(capacityGroupDao.save(conf), entityId);
		return capacityGroupDao.get(pk);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public CapacityGroupSettings updateCapacityGroupSettings(Long userId, Long entityId,
			CapacityGroupSettingsInput input) throws AuthorizationException {
		CapacityGroupSettings settings = input.toEntity(new UserLongCompositePK(
				requireNonNullArgument(userId, "userId"), requireNonNullArgument(entityId, "entityId")));
		UserLongCompositePK pk = requireNonNullObject(capacityGroupSettingsDao.save(settings), entityId);
		return capacityGroupSettingsDao.get(pk);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public AssetConfiguration updateAsset(Long userId, Long entityId, AssetConfigurationInput input)
			throws AuthorizationException {
		AssetConfiguration conf = input.toEntity(new UserLongCompositePK(
				requireNonNullArgument(userId, "userId"), requireNonNullArgument(entityId, "entityId")));
		UserLongCompositePK pk = requireNonNullObject(assetDao.save(conf), entityId);
		return assetDao.get(pk);
	}

	/**
	 * Configure a {@link SecretsBiz} to use for storing OAuth client secrets.
	 * 
	 * @param secretsBiz
	 *        the service to use
	 */
	public void setSecretsBiz(SecretsBiz secretsBiz) {
		this.secretsBiz = secretsBiz;
	}

	/**
	 * Set the configuration secret key name formatter to use.
	 * 
	 * @param configSecretsNameFormatter
	 *        the formatter; defaults to
	 *        {@link AuthRoleSecretKeyFormatter#INSTANCE}
	 */
	public void setConfigSecretsNameFormatter(
			Function<AuthRoleInfo, String> configSecretsNameFormatter) {
		if ( configSecretsNameFormatter == null ) {
			configSecretsNameFormatter = AuthRoleSecretKeyFormatter.INSTANCE;
		}
		this.configSecretsNameFormatter = configSecretsNameFormatter;
	}

}
