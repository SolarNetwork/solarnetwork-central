/* ==================================================================
 * HttpConstants.java - 2/12/2025 11:57:12 am
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

package net.solarnetwork.central.common.http;

/**
 * Common HTTP related constants.
 * 
 * @author matt
 * @version 1.0
 */
public final class HttpConstants {

	private HttpConstants() {
		super();
	}

	/** An API OAuth token-granting URL used by the external service API. */
	public static String OAUTH_TOKEN_URL_SETTING = "oauthTokenUrl";

	/** An OAuth authorization-granting URL used by the external service API. */
	public static String OAUTH_AUTHORIZATION_URL_SETTING = "oauthAuthorizationUrl";

	/**
	 * An OAuth authentication method used by the external service API (one of
	 * {@link OAuth2AuthenticationMethod}).
	 */
	public static String OAUTH_AUTHENTICATION_METHOD_SETTING = "oauthAuthenticationMethod";

	/** A standard OAuth client identifier setting name. */
	public static String OAUTH_CLIENT_ID_SETTING = "oauthClientId";

	/** A standard OAuth client secret setting name. */
	public static String OAUTH_CLIENT_SECRET_SETTING = "oauthClientSecret";

	/** A standard OAuth access token. */
	public static String OAUTH_ACCESS_TOKEN_SETTING = "oauthAccessToken";

	/** A standard OAuth refresh token. */
	public static String OAUTH_REFRESH_TOKEN_SETTING = "oauthRefreshToken";

	/** A standard OAuth state value. */
	public static String OAUTH_STATE_SETTING = "oauthState";

	/** A standard username setting name. */
	public static String USERNAME_SETTING = "username";

	/** A standard password setting name. */
	public static String PASSWORD_SETTING = "password";

	/** An OAuth authorization code parameter. */
	public static String AUTHORIZATION_CODE_PARAM = "code";

	/** An OAuth authorization state parameter. */
	public static String AUTHORIZATION_STATE_PARAM = "state";

	/** An OAuth redirect URI parameter. */
	public static String REDIRECT_URI_PARAM = "redirect_uri";

}
