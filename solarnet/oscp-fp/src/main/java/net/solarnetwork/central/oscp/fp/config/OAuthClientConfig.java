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

import java.util.Arrays;
import javax.cache.Cache;
import javax.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
import org.springframework.security.oauth2.client.endpoint.DefaultClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import net.solarnetwork.central.biz.SecretsBiz;
import net.solarnetwork.central.oscp.dao.ExternalSystemSupportDao;
import net.solarnetwork.central.oscp.http.ExternalSystemClientRegistrationRepository;
import net.solarnetwork.central.oscp.http.RestOpsExternalSystemClient;
import net.solarnetwork.central.support.CacheSettings;

/**
 * OAuth client configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class OAuthClientConfig {

	/** A qualifier to use for OAuth client registration. */
	public static final String OAUTH_CLIENT_REGISTRATION = "oauth-client-reg";

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private ExternalSystemSupportDao systemSupportDao;

	@Autowired
	private SecretsBiz secretsBiz;

	@Bean
	@ConfigurationProperties(prefix = "app.oauth.client.registration-cache")
	public CacheSettings oauthClientRegistrationCacheSettings() {
		return new CacheSettings();
	}

	@Bean
	@Qualifier(OAUTH_CLIENT_REGISTRATION)
	public Cache<String, ClientRegistration> oauthClientRegistrationCache() {
		CacheSettings settings = oauthClientRegistrationCacheSettings();
		return settings.createCache(cacheManager, String.class, ClientRegistration.class,
				OAUTH_CLIENT_REGISTRATION);
	}

	@Bean
	public ClientRegistrationRepository oauthClientRegistrationRepository(
			@Qualifier(OAUTH_CLIENT_REGISTRATION) Cache<String, ClientRegistration> cache) {
		var repo = new ExternalSystemClientRegistrationRepository(cache, systemSupportDao);
		repo.setSecretsBiz(secretsBiz);
		return repo;
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
					var client = new DefaultClientCredentialsTokenResponseClient();
					client.setRestOperations(restOps);
					b.accessTokenResponseClient(client);
				}).build();

		var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(repo, clientService);
		manager.setAuthorizedClientProvider(provider);
		return manager;
	}

}
