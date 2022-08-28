/* ==================================================================
 * ExternalSystemClientRegistrationRepository.java - 28/08/2022 7:41:14 am
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

package net.solarnetwork.central.oscp.http;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.Map;
import java.util.function.Function;
import javax.cache.Cache;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import net.solarnetwork.central.biz.SecretsBiz;
import net.solarnetwork.central.oscp.dao.ExternalSystemSupportDao;
import net.solarnetwork.central.oscp.domain.AuthRoleInfo;
import net.solarnetwork.central.oscp.domain.ExternalSystemConfiguration;
import net.solarnetwork.central.oscp.domain.ExternalSystemServiceProperties;
import net.solarnetwork.central.oscp.domain.OAuthClientSettings;
import net.solarnetwork.central.oscp.util.AuthRoleSecretKeyFormatter;

/**
 * Repository of OAuth client registrations based on external system
 * configuration.
 * 
 * <p>
 * The {@code registrationId} passed to {@link #findByRegistrationId(String)}
 * must be in the form as returned by {@link AuthRoleInfo#asIdentifier()}.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class ExternalSystemClientRegistrationRepository implements ClientRegistrationRepository {

	private final Cache<String, ClientRegistration> cache;
	private final ExternalSystemSupportDao systemSupportDao;
	private Function<AuthRoleInfo, String> secretsKeyFormatter;
	private SecretsBiz secretsBiz;

	/**
	 * Constructor.
	 * 
	 * @param cache
	 *        the cache
	 * @param systemSupportDao
	 *        the system support DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public ExternalSystemClientRegistrationRepository(Cache<String, ClientRegistration> cache,
			ExternalSystemSupportDao systemSupportDao) {
		super();
		this.cache = requireNonNullArgument(cache, "cache");
		this.systemSupportDao = requireNonNullArgument(systemSupportDao, "systemSupportDao");
	}

	@Override
	public ClientRegistration findByRegistrationId(String registrationId) {
		ClientRegistration result = cache.get(registrationId);
		if ( result != null ) {
			return result;
		}
		AuthRoleInfo role = AuthRoleInfo.forIdentifier(registrationId);
		ExternalSystemConfiguration conf = systemSupportDao.externalSystemConfiguration(role.role(),
				role.id());
		if ( conf == null ) {
			throw new EmptyResultDataAccessException(
					"Configuration for %s %s not found.".formatted(role.role(), role.id().ident()), 1);
		}
		OAuthClientSettings settings = conf.oauthClientSettings();
		if ( settings == null ) {
			throw new IllegalArgumentException("%s %s configuration does not support OAuth."
					.formatted(role.role(), role.id().ident()));
		}
		String secret = settings.clientSecret();
		if ( secret == null && secretsBiz != null && secretsKeyFormatter != null ) {
			String secretKey = secretsKeyFormatter.apply(role);
			Map<String, Object> data = secretsBiz.getSecretMap(secretKey);
			if ( data != null ) {
				Object v = data.get(ExternalSystemServiceProperties.OAUTH_CLIENT_SECRET);
				if ( v != null ) {
					secret = v.toString();
				}
			}
		}
		if ( secret == null ) {
			throw new IllegalArgumentException(
					"%s %s configured to use OAuth but does not have client secret available."
							.formatted(role.role(), role.id().ident()));
		}
		// @formatter:off
		result = ClientRegistration.withRegistrationId(registrationId)
				.tokenUri(settings.tokenUrl())
				.clientId(settings.clientId())
				.clientSecret(secret)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.build();
		// @formatter:on
		cache.put(registrationId, result);
		return result;
	}

	/**
	 * Set the secrets service.
	 * 
	 * <p>
	 * If {@link #setSecretsKeyFormatter(Function)} has not be called when this
	 * method is called, a default {@link AuthRoleSecretKeyFormatter#INSTANCE}
	 * will be configured.
	 * </p>
	 * 
	 * @param secretsBiz
	 *        the service to set
	 */
	public void setSecretsBiz(SecretsBiz secretsBiz) {
		this.secretsBiz = secretsBiz;
		if ( secretsBiz != null && secretsKeyFormatter == null ) {
			setSecretsKeyFormatter(AuthRoleSecretKeyFormatter.INSTANCE);
		}
	}

	/**
	 * Set a formatter for secret key values.
	 * 
	 * <p>
	 * This must be configured if {@link #setSecretsBiz(SecretsBiz)} is
	 * configured.
	 * </p>
	 * 
	 * @param secretsKeyFormatter
	 *        the formatter to use
	 */
	public void setSecretsKeyFormatter(Function<AuthRoleInfo, String> secretsKeyFormatter) {
		this.secretsKeyFormatter = secretsKeyFormatter;
	}

}
