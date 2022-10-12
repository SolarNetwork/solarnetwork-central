/* ==================================================================
 * OscpWebUtils.java - 11/08/2022 1:28:12 pm
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

package net.solarnetwork.central.oscp.web;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.WebRequest;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import net.solarnetwork.central.oscp.security.OscpSecurityUtils;
import net.solarnetwork.security.AuthorizationException;
import net.solarnetwork.security.AuthorizationException.Reason;

/**
 * Web-related utilities for OSCP.
 * 
 * @author matt
 * @version 1.0
 */
public final class OscpWebUtils {

	/** The HTTP header for a message request ID. */
	public static final String REQUEST_ID_HEADER = "X-Request-ID";

	/** The HTTP header for a message correlation ID. */
	public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

	/** The HTTP header for an error message. */
	public static final String ERROR_MESSAGE_HEADER = "X-Error-Message";

	/**
	 * The {@code Authorization} HTTP header scheme for OSCP token
	 * authentication.
	 */
	public static final String OSCP_TOKEN_AUTHORIZATION_SCHEME = "Token";

	/**
	 * The {@code Authorization} HTTP header scheme for OAuth token
	 * authentication.
	 */
	public static final String OAUTH_TOKEN_AUTHORIZATION_SCHEME = "Bearer";

	/** The URL path to the Capacity Provider API. */
	public static final String CAPACITY_PROVIDER_URL_PATH = "/oscp/cp";

	/** The URL path to the Capacity Optimizer API. */
	public static final String CAPACITY_OPTIMIZER_URL_PATH = "/oscp/co";

	/** The URL path to the Flexibility Provider API. */
	public static final String FLEXIBILITY_PROVIDER_URL_PATH = "/oscp/fp";

	/**
	 * The URL path to the register endpoint (constant across protocol
	 * versions).
	 */
	public static final String REGISTER_URL_PATH = "/register";

	/**
	 * A thread-local for dealing with "do something after returning response to
	 * client" pattern in OSCP.
	 */
	public static final ThreadLocal<CompletableFuture<Void>> RESPONSE_SENT = new ThreadLocal<>();

	/**
	 * Create a new "response sent" condition and save it in
	 * {@link #RESPONSE_SENT}.
	 * 
	 * @return the new condition
	 */
	public static CompletableFuture<Void> newResponseSentCondition() {
		CompletableFuture<Void> condition = new CompletableFuture<>();
		RESPONSE_SENT.set(condition);
		return condition;
	}

	/**
	 * Get the OSCP or OAuth authorization token used in a request.
	 * 
	 * <p>
	 * Note that OAuth tokens are <b>not</b> verified by this method.
	 * </p>
	 * 
	 * @param request
	 *        the request
	 * @return the token
	 * @throws AuthorizationException
	 *         if the token cannot be extracted
	 */
	public static String authorizationTokenFromRequest(WebRequest request) {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		String[] comp = (header != null ? header.split(" ", 2) : null);
		if ( comp == null || comp.length != 2 ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, null);
		}
		if ( OSCP_TOKEN_AUTHORIZATION_SCHEME.equalsIgnoreCase(comp[0]) ) {
			return comp[1];
		} else if ( OAUTH_TOKEN_AUTHORIZATION_SCHEME.equalsIgnoreCase(comp[0]) ) {
			try {
				JWT jwt = JWTParser.parse(comp[1]);
				JWTClaimsSet claims = jwt.getJWTClaimsSet();
				String issuer = claims.getIssuer();
				String subject = claims.getSubject();
				return OscpSecurityUtils.jwtTokenIdentifier(issuer, subject);
			} catch ( ParseException pe ) {
				throw new AuthorizationException(Reason.ACCESS_DENIED, null);
			}
		} else {
			throw new AuthorizationException(Reason.ACCESS_DENIED, null);
		}
	}

	/**
	 * Generate a new random token.
	 * 
	 * <p>
	 * The token will generated from 48 random bytes and then base 64 encoded.
	 * </p>
	 * 
	 * @return the new token value
	 */
	public static String generateToken() {
		SecureRandom rng;
		try {
			rng = SecureRandom.getInstanceStrong();
		} catch ( NoSuchAlgorithmException e ) {
			throw new RuntimeException("Cannot generate new token value", e);
		}
		byte[] bytes = new byte[48];
		rng.nextBytes(bytes);
		return Base64.getUrlEncoder().encodeToString(bytes);
	}

	/**
	 * HTTP headers customizer that sets an OSCP token authorization header
	 * value.
	 * 
	 * @param <T>
	 *        the consumer type
	 * @param token
	 *        the authorization token
	 * @return the customizer
	 */
	public static <T> BiConsumer<T, HttpHeaders> tokenAuthorizer(String token) {
		return (body, headers) -> {
			headers.set(HttpHeaders.AUTHORIZATION, tokenAuthorizationHeader(token));
		};
	}

	/**
	 * Create a path to the Flexibility Provider API.
	 * 
	 * @param path
	 *        the path, relative to the Flexibility Provider base path
	 * @return the URL path
	 */
	public static String fpUrlPath(String path) {
		return FLEXIBILITY_PROVIDER_URL_PATH + path;
	}

	/**
	 * Create an OSCP {@code Token} HTTP {@code Authorization} header value.
	 * 
	 * @param token
	 *        the token value
	 * @return the header value
	 */
	public static String tokenAuthorizationHeader(String token) {
		return OSCP_TOKEN_AUTHORIZATION_SCHEME + " " + token;
	}

	/** URL paths for OSCP 2.0. */
	public static final class UrlPaths_20 {

		/** The 2.0 version constant. */
		public static final String V20 = "2.0";

		/** The URL path for the version 2.0 API. */
		public static final String V20_URL_PATH = "/" + V20;

		/** The URL path for the Capacity Provider version 2.0 API. */
		public static final String CAPACITY_PROVIDER_V20_URL_PATH = CAPACITY_PROVIDER_URL_PATH
				+ V20_URL_PATH;

		/** The URL path for the Capacity Optimizer version 2.0 API. */
		public static final String CAPACITY_OPTIMIZER_V20_URL_PATH = CAPACITY_OPTIMIZER_URL_PATH
				+ V20_URL_PATH;

		/** The URL path for the Flexibility Provider version 2.0 API. */
		public static final String FLEXIBILITY_PROVIDER_V20_URL_PATH = FLEXIBILITY_PROVIDER_URL_PATH
				+ V20_URL_PATH;

		/** The URL path to the handshake endpoint. */
		public static final String HANDSHAKE_URL_PATH = "/handshake";

		/** The URL path to the handshake acknowledge endpoint. */
		public static final String HANDSHAKE_ACK_URL_PATH = "/handshake_acknowledge";

		/** The URL path to the heartbeat endpoint. */
		public static final String HEARTBEAT_URL_PATH = "/heartbeat";

		/** The URL path to the update group capacity forecast endpoint. */
		public static final String UPDATE_GROUP_CAPACITY_FORECAST_URL_PATH = "/update_group_capacity_forecast";

		/** The URL path to the adjust group capacity forecast endpoint. */
		public static final String ADJUST_GROUP_CAPACITY_FORECAST_URL_PATH = "/adjust_group_capacity_forecast";

		/** The URL path to the group capacity compliance error endpoint. */
		public static final String GROUP_CAPACITY_COMPLIANCE_ERROR_URL_PATH = "/group_capacity_compliance_error";

		/** The URL path to the update group measurements endpoint. */
		public static final String UPDATE_GROUP_MEASUREMENTS_URL_PATH = "/update_group_measurements";

		/** The URL path to the update asset measurements endpoint. */
		public static final String UPDATE_ASSET_MEASUREMENTS_URL_PATH = "/update_asset_measurements";

		/**
		 * Create a path to the Capacity Provider API.
		 * 
		 * @param path
		 *        the path, relative to the Capacity Provider base path
		 * @return the URL path
		 */
		public static String cpUrlPath(String path) {
			return CAPACITY_PROVIDER_V20_URL_PATH + path;
		}

		/**
		 * Create a path to the Flexibility Provider API.
		 * 
		 * @param path
		 *        the path, relative to the Flexibility Provider base path
		 * @return the URL path
		 */
		public static String fpUrlPath(String path) {
			return FLEXIBILITY_PROVIDER_V20_URL_PATH + path;
		}

		private UrlPaths_20() {
			// not available
		}

	}

	private OscpWebUtils() {
		// not available
	}

}
