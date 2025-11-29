/* ==================================================================
 * SmaConfig.java - 29/03/2025 7:30:42 am
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

package net.solarnetwork.central.c2c.config;

import static net.solarnetwork.central.c2c.config.SolarNetCloudIntegrationsConfiguration.CLOUD_INTEGRATIONS;
import static net.solarnetwork.central.common.config.SolarNetCommonConfiguration.HTTP_TRACE;
import static net.solarnetwork.central.common.config.SolarNetCommonConfiguration.OAUTH_CLIENT_REGISTRATION;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.random.RandomGenerator;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
import net.solarnetwork.central.c2c.biz.impl.BaseCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.BaseCloudIntegrationService;
import net.solarnetwork.central.c2c.biz.impl.SmaCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.SmaCloudIntegrationService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.http.ClientCredentialsClientRegistrationRepository;
import net.solarnetwork.central.c2c.http.OAuth2Utils;
import net.solarnetwork.central.common.http.CachableRequestEntity;
import net.solarnetwork.central.datum.biz.QueryAuditor;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.security.jdbc.JdbcOAuth2AuthorizedClientService;
import net.solarnetwork.central.security.service.CachingOAuth2ClientRegistrationRepository;
import net.solarnetwork.central.security.service.JwtOAuth2AccessTokenResponseConverter;
import net.solarnetwork.central.security.service.RetryingOAuth2AuthorizedClientManager;
import net.solarnetwork.central.support.CacheSettings;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;

/**
 * Configuration for the Sma cloud integration services.
 *
 * @author matt
 * @version 1.1
 */
@Configuration(proxyBeanMethods = false)
@Profile(CLOUD_INTEGRATIONS)
public class SmaConfig implements SolarNetCloudIntegrationsConfiguration {

	/** A qualifier for Sma configuration. */
	public static final String SMA = "sma";

	/** A qualifier for SMA system time zone configuration. */
	public static final String SMA_SYSTEM_TZ = "solaredge-system-tz";

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
	private RandomGenerator rng;

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

	@Autowired
	private CacheManager cacheManager;

	@Bean
	@Qualifier(SMA_SYSTEM_TZ)
	@ConfigurationProperties(prefix = "app.c2c.cache.sma-system-tz")
	public CacheSettings smaSystemTimeZoneCacheSettings() {
		return new CacheSettings();
	}

	@Bean
	@Qualifier(SMA_SYSTEM_TZ)
	public Cache<String, ZoneId> smaSystemTimeZoneCache(
			@Qualifier(SMA_SYSTEM_TZ) CacheSettings settings) {
		return settings.createCache(cacheManager, String.class, ZoneId.class, SMA_SYSTEM_TZ + "-cache");
	}

	@Bean
	@Qualifier(SMA)
	public OAuth2AuthorizedClientManager smaOauthAuthorizedClientManager(@Autowired(
			required = false) @Qualifier(OAUTH_CLIENT_REGISTRATION) Cache<String, ClientRegistration> cache) {
		ClientRegistrationRepository repo = new ClientCredentialsClientRegistrationRepository(
				integrationConfigurationDao, SmaCloudIntegrationService.TOKEN_URI,
				ClientAuthenticationMethod.CLIENT_SECRET_BASIC, encryptor,
				integrationServiceIdentifier -> SmaCloudIntegrationService.SECURE_SETTINGS);
		if ( cache != null ) {
			repo = new CachingOAuth2ClientRegistrationRepository(cache, repo);
		}

		var clientService = new JdbcOAuth2AuthorizedClientService(bytesEncryptor, jdbcOperations, repo);

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

		OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder()
				.refreshToken(b -> {
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
	@Qualifier(SMA)
	public CloudIntegrationService smaCloudIntegrationService(
			@Qualifier(SMA) OAuth2AuthorizedClientManager oauthClientManager,
			@Qualifier(SMA) Collection<CloudDatumStreamService> datumStreamServices) {
		var service = new SmaCloudIntegrationService(datumStreamServices, userEventAppender, encryptor,
				integrationConfigurationDao, rng, restOps, oauthClientManager, Clock.systemUTC(),
				integrationLocksCache);

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(SmaCloudIntegrationService.class.getName(),
				BaseCloudIntegrationService.class.getName());
		service.setMessageSource(msgSource);

		service.setUserServiceAuditor(userServiceAuditor);

		return service;
	}

	@Bean
	@Qualifier(SMA)
	public CloudDatumStreamService smaCloudDatumStreamService(
			@Qualifier(SMA) OAuth2AuthorizedClientManager oauthClientManager,
			@Qualifier(SMA_SYSTEM_TZ) Cache<String, ZoneId> timeZoneCache) {
		var service = new SmaCloudDatumStreamService(userEventAppender, encryptor, expressionService,
				integrationConfigurationDao, datumStreamConfigurationDao,
				datumStreamMappingConfigurationDao, datumStreamPropertyConfigurationDao, restOps,
				oauthClientManager, Clock.systemUTC(), integrationLocksCache);

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(SmaCloudDatumStreamService.class.getName(),
				BaseCloudDatumStreamService.class.getName());
		service.setMessageSource(msgSource);

		service.setUserServiceAuditor(userServiceAuditor);
		service.setDatumDao(datumDao);
		service.setQueryAuditor(queryAuditor);
		service.setDatumStreamMetadataCache(datumStreamMetadataCache);
		service.setDatumStreamMetadataDao(datumStreamMetadataDao);
		service.setHttpCache(httpCache);
		service.setAllowLocalHosts(allowHttpLocalHosts);

		service.setSystemTimeZoneCache(timeZoneCache);

		return service;
	}

}
