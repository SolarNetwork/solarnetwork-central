/* ==================================================================
 * AuthTokenController.java - 30/05/2018 6:28:57 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.query.web.api;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.security.web.AuthenticationTokenService;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.web.jakarta.domain.Response;
import net.solarnetwork.web.jakarta.security.AuthenticationScheme;

/**
 * REST controller for authorization token API.
 * 
 * @author matt
 * @version 1.0
 */
@RestController("v1AuthTokenController")
@RequestMapping(value = "/api/v1/sec/auth-tokens")
@GlobalExceptionRestController
public class AuthTokenController {

	private final AuthenticationTokenService tokenService;

	/**
	 * Constructor.
	 * 
	 * @param tokenService
	 *        the token service to use
	 */
	@Autowired
	public AuthTokenController(AuthenticationTokenService tokenService) {
		super();
		assert tokenService != null;
		this.tokenService = tokenService;
	}

	/**
	 * Refresh a valid security token.
	 * 
	 * @param signDate
	 *        the sign date
	 * @return a map with a {@literal key} property that is a hex-encoded
	 *         refreshed signing key
	 */
	@ResponseBody
	@RequestMapping(value = "/refresh/v2", method = RequestMethod.GET)
	public Response<Map<String, ?>> refreshV2(@RequestParam("date") LocalDate signDate) {
		SecurityToken actor = SecurityUtils.getCurrentToken();
		Instant date = signDate.atStartOfDay(ZoneOffset.UTC).toInstant();
		if ( date.isAfter(Instant.now()) ) {
			throw new IllegalArgumentException("date parameter cannot be in the future");
		}
		byte[] key = tokenService.computeAuthenticationTokenSigningKey(AuthenticationScheme.V2, actor,
				Collections.singletonMap(AuthenticationTokenService.SIGN_DATE_PROP, date));
		Map<String, Object> data = new LinkedHashMap<>(3);
		data.put("key", Hex.encodeHexString(key));
		return Response.response(data);
	}

}
