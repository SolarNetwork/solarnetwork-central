/* ==================================================================
 * DatumEndpointAuthenticationDetailsSource.java - 23/02/2024 11:40:02 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.din.app.security;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import jakarta.servlet.http.HttpServletRequest;
import net.solarnetwork.central.din.dao.EndpointConfigurationDao;

/**
 * Authentication details source for datum
 * {@link EndpointAuthenticationDetails}.
 *
 * @author matt
 * @version 1.1
 */
public class DatumEndpointAuthenticationDetailsSource
		implements AuthenticationDetailsSource<HttpServletRequest, EndpointAuthenticationDetails> {

	public static final Pattern DEFAULT_ENDPOINT_ID_PATTERN = Pattern.compile("/endpoint/([^/]+)",
			Pattern.CASE_INSENSITIVE);

	private final Pattern endpointIdPattern;
	private final EndpointConfigurationDao endpointDao;

	/**
	 * Constructor.
	 *
	 * <p>
	 * The {@link #DEFAULT_ENDPOINT_ID_PATTERN} pattern will be used.
	 * </p>
	 *
	 * @param endpointDao
	 *        the endpoint DAO
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DatumEndpointAuthenticationDetailsSource(EndpointConfigurationDao endpointDao) {
		this(endpointDao, DEFAULT_ENDPOINT_ID_PATTERN);
	}

	/**
	 * Constructor.
	 *
	 * @param endpointDao
	 *        the endpoint DAO
	 * @param endpointIdPattern
	 *        the pattern whose single group returns the endpoint ID from a
	 *        request URL path
	 */
	public DatumEndpointAuthenticationDetailsSource(EndpointConfigurationDao endpointDao,
			Pattern endpointIdPattern) {
		super();
		this.endpointDao = requireNonNullArgument(endpointDao, "endpointDao");
		this.endpointIdPattern = requireNonNullArgument(endpointIdPattern, "endpointIdPattern");
	}

	@Override
	public EndpointAuthenticationDetails buildDetails(HttpServletRequest req) {
		UUID endpointId = null;
		Long userId = null;
		String path = req.getServletPath();
		if ( path != null ) {
			Matcher m = endpointIdPattern.matcher(path);
			if ( m.find() ) {
				try {
					endpointId = UUID.fromString(m.group(1));

					var endpoint = endpointDao.getForEndpointId(endpointId);
					if ( endpoint != null ) {
						userId = endpoint.getUserId();
					}
				} catch ( IllegalArgumentException e ) {
					// ignore
				}
			}
		}
		return new EndpointAuthenticationDetails(req, userId, endpointId);
	}

}
