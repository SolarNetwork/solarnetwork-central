/* ==================================================================
 * UserAuthTokenAuthenticationEntryPoint.java - Nov 26, 2012 4:48:36 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.security.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Entry point for SolarNetworkWS authentication.
 * 
 * @author matt
 * @version 1.1
 */
public class UserAuthTokenAuthenticationEntryPoint implements AuthenticationEntryPoint, Ordered {

	private int order = Integer.MAX_VALUE;
	private Map<String, String> httpHeaders = defaultHttpHeaders();

	private static Map<String, String> defaultHttpHeaders() {
		Map<String, String> headers = new HashMap<String, String>(2);
		headers.put("Access-Control-Allow-Origin", "*");
		headers.put("Access-Control-Allow-Headers", "Authorization, X-SN-Date");
		return headers;
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException, ServletException {
		response.addHeader("WWW-Authenticate", UserAuthTokenAuthenticationFilter.AUTHORIZATION_SCHEME);
		response.addHeader(WebConstants.HEADER_ERROR_MESSAGE, authException.getMessage());
		if ( httpHeaders != null ) {
			for ( Map.Entry<String, String> me : httpHeaders.entrySet() ) {
				response.addHeader(me.getKey(), me.getValue());
			}
		}
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
	}

	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Get the currently configured HTTP headers that are included in each
	 * response.
	 * 
	 * @return The HTTP headers to include in each response.
	 * @since 1.1
	 */
	public Map<String, String> getHttpHeaders() {
		return httpHeaders;
	}

	/**
	 * Set additional HTTP headers to include in the response. By default the
	 * {@code Access-Control-Allow-Origin} header is set to {@code *} and
	 * {@code Access-Control-Allow-Headers} header is set to
	 * {@code Authorization, X-SN-Date}.
	 * 
	 * @param httpHeaders
	 *        The HTTP headers to include in each response.
	 * @since 1.1
	 */
	public void setHttpHeaders(Map<String, String> httpHeaders) {
		this.httpHeaders = httpHeaders;
	}

}
