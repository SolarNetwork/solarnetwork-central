/* ==================================================================
 * AlsoEnergyConfig.java - 22/11/2024 9:43:55 am
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

package net.solarnetwork.central.c2c.config;

import static net.solarnetwork.central.c2c.config.SolarNetCloudIntegrationsConfiguration.CLOUD_INTEGRATIONS;
import static net.solarnetwork.central.common.config.SolarNetCommonConfiguration.HTTP_TRACE;
import static net.solarnetwork.central.common.config.SolarNetCommonConfiguration.OAUTH_CLIENT_REGISTRATION;
import java.time.Clock;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.locks.Lock;
import javax.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.TextEncryptor;
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
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.DefaultMapOAuth2AccessTokenResponseConverter;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.biz.UserServiceAuditor;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.AlsoEnergyCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.AlsoEnergyCloudIntegrationService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.http.CachableRequestEntity;
import net.solarnetwork.central.c2c.http.ClientCredentialsClientRegistrationRepository;
import net.solarnetwork.central.c2c.http.OAuth2Utils;
import net.solarnetwork.central.datum.biz.QueryAuditor;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.security.jdbc.JdbcOAuth2AuthorizedClientService;
import net.solarnetwork.central.security.service.CachingOAuth2ClientRegistrationRepository;
import net.solarnetwork.central.security.service.JwtOAuth2AccessTokenResponseConverter;
import net.solarnetwork.central.security.service.RetryingOAuth2AuthorizedClientManager;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;

/**
 * Configuration for the AlsoEnergy cloud integration services.
 *
 * @author matt
 * @version 1.3
 */
@Configuration(proxyBeanMethods = false)
@Profile(CLOUD_INTEGRATIONS)
public class AlsoEnergyConfig implements SolarNetCloudIntegrationsConfiguration {

	/** A qualifier for AlsoEnergy configuration. */
	public static final String ALSO_ENERGY = "also-energy";

	@Autowired
	private UserEventAppenderBiz userEventAppender;

	@Autowired
	private CloudIntegrationConfigurationDao integrationConfigurationDao;

	@Autowired
	private CloudDatumStreamConfigurationDao datumStreamConfigurationDao;

	@Autowired
	private CloudDatumStreamMappingConfigurationDao datumStreamMappingConfigurationDao;

	@Autowired
	private CloudDatumStreamPropertyConfigurationDao datumStreamPropertyConfigurationDao;

	@Autowired
	private JdbcOperations jdbcOperations;

	@Autowired
	private ClientHttpRequestFactory reqFactory;

	@Autowired
	private RestTemplate restOps;

	@Autowired
	@Qualifier(CLOUD_INTEGRATIONS)
	private TextEncryptor encryptor;

	@Autowired
	@Qualifier(CLOUD_INTEGRATIONS)
	private BytesEncryptor bytesEncryptor;

	@Autowired
	private CloudIntegrationsExpressionService expressionService;

	@Autowired(required = false)
	private UserServiceAuditor userServiceAuditor;

	@Autowired
	private DatumEntityDao datumDao;

	@Autowired(required = false)
	private QueryAuditor queryAuditor;

	@Autowired
	private Environment environment;

	@Autowired(required = false)
	@Qualifier(CLOUD_INTEGRATIONS_INTEGRATION_LOCKS)
	private Cache<UserLongCompositePK, Lock> integrationLocksCache;

	@Autowired
	private DatumStreamMetadataDao datumStreamMetadataDao;

	@Autowired(required = false)
	@Qualifier(CLOUD_INTEGRATIONS_DATUM_STREAM_METADATA)
	private Cache<ObjectDatumStreamMetadataId, GeneralDatumMetadata> datumStreamMetadataCache;

	@Autowired(required = false)
	@Qualifier(CLOUD_INTEGRATIONS_HTTP)
	private Cache<CachableRequestEntity, Result<?>> httpCache;

	@Value("${app.c2c.allow-http-local-hosts:false}")
	private boolean allowHttpLocalHosts;

	@Bean
	@Qualifier(ALSO_ENERGY)
	public OAuth2AuthorizedClientManager alsoEnergyOauthAuthorizedClientManager(@Autowired(
			required = false) @Qualifier(OAUTH_CLIENT_REGISTRATION) Cache<String, ClientRegistration> cache) {
		ClientRegistrationRepository repo = new ClientCredentialsClientRegistrationRepository(
				integrationConfigurationDao, AlsoEnergyCloudIntegrationService.TOKEN_URI,
				ClientAuthenticationMethod.CLIENT_SECRET_POST, encryptor,
				integrationServiceIdentifier -> AlsoEnergyCloudIntegrationService.SECURE_SETTINGS);
		if ( cache != null ) {
			repo = new CachingOAuth2ClientRegistrationRepository(cache, repo);
		}

		var clientService = new JdbcOAuth2AuthorizedClientService(bytesEncryptor, jdbcOperations, repo);

		// AlsoEnergy not providing expires_in in token response, so extract from JWT exp claim
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

		@SuppressWarnings("deprecation")
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

		var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(repo, clientService);
		manager.setAuthorizedClientProvider(provider);
		manager.setContextAttributesMapper(OAuth2Utils::principalCredentialsContextAttributes);
		return new RetryingOAuth2AuthorizedClientManager(manager, clientService);
	}

	@Bean
	@Qualifier(ALSO_ENERGY)
	public CloudDatumStreamService alsoEnergyCloudDatumStreamService(
			@Qualifier(ALSO_ENERGY) OAuth2AuthorizedClientManager oauthClientManager) {
		var service = new AlsoEnergyCloudDatumStreamService(userEventAppender, encryptor,
				expressionService, integrationConfigurationDao, datumStreamConfigurationDao,
				datumStreamMappingConfigurationDao, datumStreamPropertyConfigurationDao, restOps,
				oauthClientManager, Clock.systemUTC(), integrationLocksCache);

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(AlsoEnergyCloudDatumStreamService.class.getName(),
				BaseCloudDatumStreamService.class.getName());
		service.setMessageSource(msgSource);

		service.setUserServiceAuditor(userServiceAuditor);
		service.setDatumDao(datumDao);
		service.setQueryAuditor(queryAuditor);
		service.setDatumStreamMetadataCache(datumStreamMetadataCache);
		service.setDatumStreamMetadataDao(datumStreamMetadataDao);
		service.setHttpCache(httpCache);
		service.setAllowLocalHosts(allowHttpLocalHosts);

		return service;
	}

	@Bean
	@Qualifier(ALSO_ENERGY)
	public CloudIntegrationService alsoEnergyCloudIntegrationService(
			@Qualifier(ALSO_ENERGY) OAuth2AuthorizedClientManager oauthClientManager,
			@Qualifier(ALSO_ENERGY) Collection<CloudDatumStreamService> datumStreamServices) {
		var service = new AlsoEnergyCloudIntegrationService(datumStreamServices, userEventAppender,
				encryptor, restOps, oauthClientManager, Clock.systemUTC(), integrationLocksCache);

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(AlsoEnergyCloudIntegrationService.class.getName(),
				BaseCloudIntegrationService.class.getName());
		service.setMessageSource(msgSource);

		service.setUserServiceAuditor(userServiceAuditor);

		return service;
	}

}
