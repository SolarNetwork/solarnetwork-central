/* ==================================================================
 * KillbillRestClient.java - 21/08/2017 10:37:52 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.killbill;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

/**
 * REST implementation of {@link KillbillClient}.
 * 
 * @author matt
 * @version 1.0
 */
public class KillbillRestClient {

	/** The default base URL for the production service. */
	public static final String DEFAULT_BASE_URL = "https://billing.solarnetwork.net";

	private String baseUrl = DEFAULT_BASE_URL;
	private String username = "tester";
	private String password = "changeit";
	private String apiKey = "solarnetwork";
	private String apiSecret = "solarnetwork";

	private final RestOperations client;

	/**
	 * Default constructor.
	 */
	public KillbillRestClient() {
		this(new RestTemplate());
	}

	/**
	 * Construct with a RestTemplate.
	 * 
	 * @param template
	 *        the template to use
	 */
	public KillbillRestClient(RestTemplate template) {
		super();
		client = template;
		setupRestTemplateInterceptors();
	}

	private void setupRestTemplateInterceptors() {
		RestTemplate restTemplate = (RestTemplate) client;
		List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
		if ( restTemplate.getInterceptors() != null ) {
			interceptors.addAll(restTemplate.getInterceptors().stream()
					.filter(o -> !(o instanceof KillbillAuthorizationInterceptor))
					.collect(Collectors.toList()));
		}
		interceptors.add(0, new KillbillAuthorizationInterceptor(username, password, apiKey, apiSecret));
		restTemplate.setInterceptors(interceptors);
	}

	/**
	 * The Killbill base URL for REST operations.
	 * 
	 * @param baseUrl
	 *        the baseUrl to set
	 */
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	/**
	 * The Killbill username to use.
	 * 
	 * @param username
	 *        the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * The Killbill password to use.
	 * 
	 * @param password
	 *        the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * The Killbill API key to use.
	 * 
	 * @param apiKey
	 *        the apiKey to set
	 */
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	/**
	 * The Killbill API secret to use.
	 * 
	 * @param apiSecret
	 *        the apiSecret to set
	 */
	public void setApiSecret(String apiSecret) {
		this.apiSecret = apiSecret;
	}

}
