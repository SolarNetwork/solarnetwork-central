/* ==================================================================
 * IdentityController.java - 23/02/2024 6:07:23 pm
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

package net.solarnetwork.central.din.app.web.api;

import static net.solarnetwork.domain.Result.success;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.din.security.SecurityEndpointCredential;
import net.solarnetwork.central.security.SecurityActor;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.domain.Result;

/**
 * User identity controller.
 *
 * @author matt
 * @version 1.0
 */
@RestController("v1IdentityController")
@GlobalExceptionRestController
public class IdentityController {

	/**
	 * Constructor.
	 */
	public IdentityController() {
		super();
	}

	/**
	 * Check who the caller is.
	 *
	 * <p>
	 * This is a convenient way to verify the credentials of a user.
	 * </p>
	 *
	 * @return a result that details who the authenticated caller is
	 */
	@RequestMapping(value = "/api/v1/endpoint/{endpointId}/whoami", method = RequestMethod.GET)
	public Result<Map<String, ?>> validate(@PathVariable("endpointId") UUID endpointId) {
		SecurityActor actor = SecurityUtils.getCurrentActor();
		Map<String, Object> data = new LinkedHashMap<String, Object>(3);
		if ( actor instanceof SecurityEndpointCredential user ) {
			data.put("userId", user.getUserId());
			data.put("endpointId", user.getEndpointId());
			data.put("username", user.getUsername());
		}
		return success(data);
	}

}
