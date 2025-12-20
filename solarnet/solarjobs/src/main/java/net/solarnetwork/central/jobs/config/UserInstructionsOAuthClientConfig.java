/* ==================================================================
 * OAuthClientConfig.java - 2/12/2025 11:06:04 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.jobs.config;

import static net.solarnetwork.central.common.config.SolarNetCommonConfiguration.HTTP_TRACE;
import static net.solarnetwork.central.common.config.SolarNetCommonConfiguration.OAUTH_CLIENT_REGISTRATION;
import static net.solarnetwork.central.user.config.SolarNetUserConfiguration.USER_INSTRUCTIONS;
import static net.solarnetwork.central.user.dao.UserSecretAccessDao.userSecretDecryptorFunction;
import java.time.Clock;
import java.util.Arrays;
import java.util.function.Function;
import javax.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.endpoint.DefaultPasswordTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.RestClientRefreshTokenTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.endpoint.DefaultMapOAuth2AccessTokenResponseConverter;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import net.solarnetwork.central.common.dao.UserServiceConfigurationDao;
import net.solarnetwork.central.common.http.ClientCredentialsClientRegistrationRepository;
import net.solarnetwork.central.common.http.OAuth2Utils;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.security.jdbc.JdbcOAuth2AuthorizedClientService;
import net.solarnetwork.central.security.service.CachingOAuth2ClientRegistrationRepository;
import net.solarnetwork.central.security.service.JwtOAuth2AccessTokenResponseConverter;
import net.solarnetwork.central.security.service.RetryingOAuth2AuthorizedClientManager;
import net.solarnetwork.central.user.biz.InstructionsExpressionService;
import net.solarnetwork.central.user.config.SolarNetUserConfiguration;
import net.solarnetwork.central.user.dao.UserNodeInstructionTaskDao;
import net.solarnetwork.central.user.dao.UserSecretAccessDao;

/**
 * OAuth support for user instructions.
 *
 * @author matt
 * @version 1.0
 */
@Profile(USER_INSTRUCTIONS)
@Configuration(proxyBeanMethods = false)
public class UserInstructionsOAuthClientConfig implements SolarNetUserConfiguration {

	@Autowired
	private RestTemplate restOps;

	@Autowired
	private Environment environment;

	@Autowired
	private JdbcOperations jdbcOperations;

	@Autowired
	private ClientHttpRequestFactory reqFactory;

	@Autowired
	private UserNodeInstructionTaskDao instructionTaskDao;

	@Autowired(required = false)
	private UserSecretAccessDao userSecretAccessDao;

	@Autowired
	@Qualifier(USER_INSTRUCTIONS)
	private BytesEncryptor bytesEncryptor;

	@Qualifier(USER_INSTRUCTIONS_HTTP)
	@Bean
	public OAuth2AuthorizedClientProvider userInstructionsOauthAuthorizedClientProvider() {
		var tokenResponseConverter = new OAuth2AccessTokenResponseHttpMessageConverter();
		tokenResponseConverter.setAccessTokenResponseConverter(new JwtOAuth2AccessTokenResponseConverter(
				Clock.systemUTC(), new DefaultMapOAuth2AccessTokenResponseConverter()));

		// @formatter:off
		var authRestOps = new RestTemplateBuilder()
				.requestFactory(() -> environment.matchesProfiles(HTTP_TRACE)
						? new BufferingClientHttpRequestFactory(reqFactory)
						: reqFactory)
				.messageConverters(Arrays.asList(
						new FormHttpMessageConverter(),
						tokenResponseConverter))
				.errorHandler(new OAuth2ErrorResponseErrorHandler())
				.interceptors(restOps.getInterceptors())
				.build();

		var authRestClient = RestClient.create(authRestOps);
		// @formatter:on

		@SuppressWarnings("removal")
		OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder()
				.password(b -> {
					var client = new DefaultPasswordTokenResponseClient();
					client.setRestOperations(authRestOps);
					b.accessTokenResponseClient(client);
				}).clientCredentials(b -> {
					var client = new RestClientClientCredentialsTokenResponseClient();
					client.setRestClient(authRestClient);
					b.accessTokenResponseClient(client);
				}).refreshToken(b -> {
					var client = new RestClientRefreshTokenTokenResponseClient();
					client.setRestClient(authRestClient);
					b.accessTokenResponseClient(client);
				}).build();

		return provider;
	}

	@Qualifier(USER_INSTRUCTIONS_HTTP)
	@Bean
	public OAuth2AuthorizedClientManager userInstructionsOauthAuthorizedClientManager(
			@Qualifier(USER_INSTRUCTIONS_HTTP) OAuth2AuthorizedClientProvider provider, @Autowired(
					required = false) @Qualifier(OAUTH_CLIENT_REGISTRATION) Cache<String, ClientRegistration> cache) {
		return createOAuth2AuthorizedClientManager(instructionTaskDao, provider, cache);
	}

	@Qualifier(USER_INSTRUCTIONS_HTTP)
	@Bean
	public Function<UserServiceConfigurationDao<UserLongCompositePK>, OAuth2AuthorizedClientManager> userInstructionsOauthAuthorizedClientManagerProvider(
			@Qualifier(USER_INSTRUCTIONS_HTTP) OAuth2AuthorizedClientProvider provider) {
		return (configurationDao) -> createOAuth2AuthorizedClientManager(configurationDao, provider,
				null);
	}

	private OAuth2AuthorizedClientManager createOAuth2AuthorizedClientManager(
			UserServiceConfigurationDao<UserLongCompositePK> configurationDao,
			OAuth2AuthorizedClientProvider provider, Cache<String, ClientRegistration> cache) {
		ClientRegistrationRepository repo = new ClientCredentialsClientRegistrationRepository(
				configurationDao, userSecretDecryptorFunction(userSecretAccessDao,
						InstructionsExpressionService.USER_SECRET_TOPIC_ID));
		if ( cache != null ) {
			repo = new CachingOAuth2ClientRegistrationRepository(cache, repo);
		}

		var clientService = new JdbcOAuth2AuthorizedClientService(bytesEncryptor, jdbcOperations, repo);

		var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(repo, clientService);
		manager.setAuthorizedClientProvider(provider);
		manager.setContextAttributesMapper(OAuth2Utils::principalCredentialsContextAttributes);
		return new RetryingOAuth2AuthorizedClientManager(manager, clientService);
	}

}
