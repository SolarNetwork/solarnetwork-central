/* ==================================================================
 * HttpClientConfig.java - 18/08/2022 10:55:37 am
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
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import net.solarnetwork.web.support.LoggingHttpRequestInterceptor;

/**
 * HTTP client configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class HttpClientConfig {

	@ConfigurationProperties(prefix = "app.http.client.connections")
	@Bean
	public PoolingHttpClientConnectionManager poolingConnectionManager() {
		var poolingConnectionManager = new PoolingHttpClientConnectionManager();
		return poolingConnectionManager;
	}

	@Bean
	public CloseableHttpClient httpClient(HttpClientConnectionManager connectionManager) {
		// @formatter:off
        return HttpClients.custom()
        		.useSystemProperties()
                .setConnectionManager(connectionManager)
                .build();
        // @formatter:on
	}

	@Bean
	public HttpComponentsClientHttpRequestFactory clientHttpRequestFactory(
			CloseableHttpClient httpClient) {
		return new HttpComponentsClientHttpRequestFactory(httpClient);
	}

	/**
	 * Get a REST client for production.
	 * 
	 * @param reqFactory
	 *        the request factory
	 * @return the service
	 */
	@Profile("!http-trace")
	@Bean
	public RestTemplate restTemplate(ClientHttpRequestFactory reqFactory) {
		return new RestTemplate(reqFactory);
	}

	/**
	 * Get a REST client for non-production use, with logging enabled.
	 * 
	 * @param reqFactory
	 *        the request factory
	 * @return the non-production service
	 */
	@Profile("http-trace")
	@Bean
	public RestTemplate testingRestTemplate(ClientHttpRequestFactory reqFactory) {
		//var reqFactory = new SimpleClientHttpRequestFactory();
		//reqFactory.setOutputStreaming(false);
		RestTemplate debugTemplate = new RestTemplate(new BufferingClientHttpRequestFactory(reqFactory));
		debugTemplate.setInterceptors(Arrays.asList(new LoggingHttpRequestInterceptor()));
		return debugTemplate;
	}

}
