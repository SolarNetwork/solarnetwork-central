/* ==================================================================
 * LocusEnergyConfig.java - 30/09/2024 12:03:52 pm
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
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.DefaultPasswordTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.DefaultRefreshTokenTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.web.client.RestOperations;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationService;
import net.solarnetwork.central.c2c.biz.CloudIntegrationsExpressionService;
import net.solarnetwork.central.c2c.biz.impl.LocusEnergyCloudDatumStreamService;
import net.solarnetwork.central.c2c.biz.impl.LocusEnergyCloudIntegrationService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPropertyConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.http.ClientCredentialsClientRegistrationRepository;
import net.solarnetwork.central.c2c.http.OAuth2Utils;
import net.solarnetwork.central.security.jdbc.JdbcOAuth2AuthorizedClientService;

/**
 * Configuration for the Locus Energy cloud integration services.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
@Profile(CLOUD_INTEGRATIONS)
public class LocusEnergyConfig {

	/** A qualifier for Locus Energy configuraiton. */
	public static final String LOCUS_ENERGY = "locus-energy";

	@Autowired
	private UserEventAppenderBiz userEventAppender;

	@Autowired
	private CloudIntegrationConfigurationDao integrationConfigurationDao;

	@Autowired
	private CloudDatumStreamConfigurationDao datumStreamConfigurationDao;

	@Autowired
	private CloudDatumStreamPropertyConfigurationDao datumStreamPropertyConfigurationDao;

	@Autowired
	private JdbcOperations jdbcOperations;

	@Autowired
	private ClientHttpRequestFactory reqFactory;

	@Autowired
	private RestOperations restOps;

	@Autowired
	@Qualifier(CLOUD_INTEGRATIONS)
	private TextEncryptor encryptor;

	@Autowired
	@Qualifier(CLOUD_INTEGRATIONS)
	private BytesEncryptor bytesEncryptor;

	@Autowired
	private CloudIntegrationsExpressionService expressionService;

	@Bean
	@Qualifier(LOCUS_ENERGY)
	public OAuth2AuthorizedClientManager oauthAuthorizedClientManager() {
		var repo = new ClientCredentialsClientRegistrationRepository(integrationConfigurationDao,
				LocusEnergyCloudIntegrationService.TOKEN_URI,
				ClientAuthenticationMethod.CLIENT_SECRET_POST, encryptor,
				integrationServiceIdentifier -> LocusEnergyCloudIntegrationService.SECURE_SETTINGS);

		var clientService = new JdbcOAuth2AuthorizedClientService(bytesEncryptor, jdbcOperations, repo);

		// @formatter:off
		var authRestOps = new RestTemplateBuilder()
				.requestFactory(t -> reqFactory)
				.messageConverters(Arrays.asList(
						new FormHttpMessageConverter(),
						new OAuth2AccessTokenResponseHttpMessageConverter()))
				.errorHandler(new OAuth2ErrorResponseErrorHandler())
				.build();
		// @formatter:on

		@SuppressWarnings("deprecation")
		OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder()
				.password(b -> {
					var client = new DefaultPasswordTokenResponseClient();
					client.setRestOperations(authRestOps);
					b.accessTokenResponseClient(client);
				}).clientCredentials(b -> {
					var client = new DefaultClientCredentialsTokenResponseClient();
					client.setRestOperations(authRestOps);
					b.accessTokenResponseClient(client);
				}).refreshToken(b -> {
					var client = new DefaultRefreshTokenTokenResponseClient();
					client.setRestOperations(authRestOps);
					b.accessTokenResponseClient(client);
				}).build();

		var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(repo, clientService);
		manager.setAuthorizedClientProvider(provider);
		manager.setContextAttributesMapper(OAuth2Utils::principalCredentialsContextAttributes);
		return manager;
	}

	@Bean
	@Qualifier(LOCUS_ENERGY)
	public CloudDatumStreamService locusEnergyCloudDatumStreamService(
			@Qualifier(LOCUS_ENERGY) OAuth2AuthorizedClientManager oauthClientManager) {
		var service = new LocusEnergyCloudDatumStreamService(Executors::newVirtualThreadPerTaskExecutor,
				userEventAppender, encryptor, expressionService, integrationConfigurationDao,
				datumStreamConfigurationDao, datumStreamPropertyConfigurationDao, restOps,
				oauthClientManager);

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(LocusEnergyCloudDatumStreamService.class.getName());
		service.setMessageSource(msgSource);

		return service;
	}

	@Bean
	public CloudIntegrationService locusEnergyCloudIntegrationService(
			@Qualifier(LOCUS_ENERGY) OAuth2AuthorizedClientManager oauthClientManager,
			@Qualifier(LOCUS_ENERGY) Collection<CloudDatumStreamService> datumStreamServices) {
		var service = new LocusEnergyCloudIntegrationService(datumStreamServices, userEventAppender,
				encryptor, restOps, oauthClientManager);

		ResourceBundleMessageSource msgSource = new ResourceBundleMessageSource();
		msgSource.setBasenames(LocusEnergyCloudIntegrationService.class.getName());
		service.setMessageSource(msgSource);

		return service;
	}

}
