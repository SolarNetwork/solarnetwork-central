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

import java.time.Duration;
import java.util.Arrays;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
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
 * @version 1.2
 */
@Configuration(proxyBeanMethods = false)
public class HttpClientConfig {

	/** Settings for the HTTP client. */
	public static class HttpClientSettings {

		/** The HTTP client connect timeout. */
		private Duration connectTimeout = Duration.ofSeconds(10);
		private Duration connectionRequestTimeout = Duration.ofSeconds(15);
		private Duration socketTimeout = Duration.ofSeconds(20);

		/**
		 * Get the connection timeout.
		 * 
		 * @return the timeout
		 */
		public Duration getConnectTimeout() {
			return connectTimeout;
		}

		/**
		 * Set the connection timeout.
		 * 
		 * @param connectTimeout
		 *        the timeout to set
		 */
		public void setConnectTimeout(Duration connectTimeout) {
			this.connectTimeout = connectTimeout;
		}

		/**
		 * Get the connection pool borrow timeout.
		 * 
		 * @return the timeout
		 */
		public Duration getConnectionRequestTimeout() {
			return connectionRequestTimeout;
		}

		/**
		 * Set the connection pool borrow timeout.
		 * 
		 * @param connectionRequestTimeout
		 *        the connectionRequestTimeout to set
		 */
		public void setConnectionRequestTimeout(Duration connectionRequestTimeout) {
			this.connectionRequestTimeout = connectionRequestTimeout;
		}

		/**
		 * Get the socket timeout.
		 * 
		 * @return the timeout
		 */
		public Duration getSocketTimeout() {
			return socketTimeout;
		}

		/**
		 * Set the socket timeout.
		 * 
		 * @param socketTimeout
		 *        the timeout to set
		 */
		public void setSocketTimeout(Duration socketTimeout) {
			this.socketTimeout = socketTimeout;
		}

	}

	@Bean
	@ConfigurationProperties(prefix = "app.http.client.settings")
	public HttpClientSettings asyncDatumCollectorSettings() {
		return new HttpClientSettings();
	}

	@Bean
	public RequestConfig httpRequestConfig(HttpClientSettings settings) {
		RequestConfig config = RequestConfig.custom()
				.setConnectionRequestTimeout(Timeout.of(settings.connectionRequestTimeout)).build();
		return config;
	}

	@ConfigurationProperties(prefix = "app.http.client.connections")
	@Bean
	public PoolingHttpClientConnectionManager poolingConnectionManager(HttpClientSettings settings) {
		ConnectionConfig config = ConnectionConfig.custom()
				.setConnectTimeout(Timeout.of(settings.connectTimeout))
				.setSocketTimeout(Timeout.of(settings.socketTimeout)).build();
		var poolingConnectionManager = new PoolingHttpClientConnectionManager();
		poolingConnectionManager.setDefaultConnectionConfig(config);
		return poolingConnectionManager;
	}

	@Bean
	public CloseableHttpClient httpClient(HttpClientConnectionManager connectionManager,
			RequestConfig requestConfig) {
		// @formatter:off
        return HttpClients.custom()
        		.setDefaultRequestConfig(requestConfig)
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
