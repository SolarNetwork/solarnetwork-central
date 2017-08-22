/* ==================================================================
 * KillbillAuthorizationInterceptor.java - 21/08/2017 10:47:27 AM
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

import java.io.IOException;
import java.nio.charset.Charset;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Base64Utils;

/**
 * Authorization interceptor for Killbill.
 * 
 * @author matt
 * @version 1.0
 */
public class KillbillAuthorizationInterceptor implements ClientHttpRequestInterceptor {

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	/** The Killbill API key HTTP header name. */
	public static final String API_KEY_HEADER_NAME = "X-Killbill-ApiKey";

	/** The Killbill API secret HTTP header name. */
	public static final String API_SECRET_HEADER_NAME = "X-Killbill-ApiSecret";

	private final String auth;
	private final String apiKey;
	private final String apiSecret;

	/**
	 * Create a new interceptor which adds a BASIC authorization header for the
	 * given username and password.
	 * 
	 * @param username
	 *        the username to use
	 * @param password
	 *        the password to use
	 * @param apiKey
	 *        the API key to use
	 * @param apiSecret
	 *        the API secret to use
	 */
	public KillbillAuthorizationInterceptor(String username, String password, String apiKey,
			String apiSecret) {
		assert username != null && username.length() > 0;
		this.auth = Base64Utils
				.encodeToString((username + ":" + (password != null ? password : "")).getBytes(UTF_8));
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body,
			ClientHttpRequestExecution execution) throws IOException {
		request.getHeaders().add("Authorization", "Basic " + this.auth);
		request.getHeaders().set(API_KEY_HEADER_NAME, this.apiKey);
		request.getHeaders().set(API_SECRET_HEADER_NAME, this.apiSecret);
		return execution.execute(request, body);
	}

}
