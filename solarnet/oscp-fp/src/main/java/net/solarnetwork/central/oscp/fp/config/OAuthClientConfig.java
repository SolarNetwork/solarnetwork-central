/* ==================================================================
 * OAuthClientConfig.java - 28/08/2022 7:38:07 am
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

package net.solarnetwork.central.oscp.fp.config;

import static net.solarnetwork.central.common.config.SolarNetCommonConfiguration.OAUTH_CLIENT_REGISTRATION;
import java.util.Arrays;
import javax.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import net.solarnetwork.central.biz.SecretsBiz;
import net.solarnetwork.central.oscp.dao.ExternalSystemSupportDao;
import net.solarnetwork.central.oscp.http.ExternalSystemClientRegistrationRepository;
import net.solarnetwork.central.oscp.http.RestOpsExternalSystemClient;
import net.solarnetwork.central.security.service.CachingOAuth2ClientRegistrationRepository;

/**
 * OAuth client configuration.
 *
 * @author matt
 * @version 1.0
 */
@Configuration(proxyBeanMethods = false)
public class OAuthClientConfig {

	@Autowired
	private ExternalSystemSupportDao systemSupportDao;

	@Autowired
	private SecretsBiz secretsBiz;

	@Bean
	public ClientRegistrationRepository oauthClientRegistrationRepository(
			@Qualifier(OAUTH_CLIENT_REGISTRATION) Cache<String, ClientRegistration> cache) {
		var repo = new ExternalSystemClientRegistrationRepository(systemSupportDao);
		repo.setSecretsBiz(secretsBiz);
		return new CachingOAuth2ClientRegistrationRepository(cache, repo);
	}

	@Bean
	public OAuth2AuthorizedClientService oauthAuthorizedClientService(
			ClientRegistrationRepository repo) {
		return new InMemoryOAuth2AuthorizedClientService(repo);
	}

	@Bean
	public OAuth2AuthorizedClientManager oauthAuthorizedClientManager(ClientRegistrationRepository repo,
			OAuth2AuthorizedClientService clientService) {
		OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder()
				.clientCredentials(b -> {
				// @formatter:off
					RestTemplate restOps = new RestTemplateBuilder(rt -> rt.getInterceptors().add(
							new RestOpsExternalSystemClient.ExternalSystemExtraHeadersOAuthInterceptor()))
									.messageConverters(Arrays.asList(
											new FormHttpMessageConverter(),
											new OAuth2AccessTokenResponseHttpMessageConverter()))
									.errorHandler(new OAuth2ErrorResponseErrorHandler())
									.build();
					// @formatter:on
					var client = new RestClientClientCredentialsTokenResponseClient();
					client.setRestClient(RestClient.create(restOps));
					b.accessTokenResponseClient(client);
				}).build();

		var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(repo, clientService);
		manager.setAuthorizedClientProvider(provider);
		return manager;
	}

}
