/* ==================================================================
 * OscpTokenAuthorizationHeaderAuthenticationFilter.java - 17/08/2022 10:49:36 am
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

import static net.solarnetwork.central.oscp.web.OscpWebUtils.OSCP_TOKEN_AUTHORIZATION_SCHEME;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

/**
 * Extract the OSCP authorization token from the {@code Authorization} HTTP
 * header.
 * 
 * @author matt
 * @version 1.0
 */
public class OscpTokenAuthorizationHeaderAuthenticationFilter extends RequestHeaderAuthenticationFilter {

	/**
	 * Constructor.
	 */
	public OscpTokenAuthorizationHeaderAuthenticationFilter() {
		super();
		setPrincipalRequestHeader(HttpHeaders.AUTHORIZATION);
	}

	@Override
	protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
		String value = (String) super.getPreAuthenticatedPrincipal(request);
		if ( value != null ) {
			String[] components = value.split(" ", 2);
			if ( components.length == 2 && OSCP_TOKEN_AUTHORIZATION_SCHEME.equals(components[0]) ) {
				return components[1];
			}
		}
		return null;
	}

}
