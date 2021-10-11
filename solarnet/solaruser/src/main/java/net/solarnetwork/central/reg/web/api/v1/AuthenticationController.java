/* ==================================================================
 * AuthenticationController.java - Nov 20, 2012 6:47:17 AM
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

package net.solarnetwork.central.reg.web.api.v1;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.central.security.SecurityActor;
import net.solarnetwork.central.security.SecurityToken;
import net.solarnetwork.central.security.SecurityUser;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.web.domain.Response;

/**
 * Remote authentication for nodes.
 * 
 * @author matt
 * @version 2.0
 */
@Controller("v1authenticationController")
@GlobalExceptionRestController
public class AuthenticationController {

	/**
	 * Check who the caller is.
	 * 
	 * <p>
	 * This is a convenient way to verify the credentials of a user.
	 * </p>
	 * 
	 * @return a response that details who the authenticated caller is
	 */
	@ResponseBody
	@RequestMapping(value = "/v1/sec/whoami", method = RequestMethod.GET)
	public Response<Map<String, ?>> validate() {
		SecurityActor actor = SecurityUtils.getCurrentActor();
		Map<String, Object> data = new LinkedHashMap<String, Object>(3);
		if ( actor instanceof SecurityUser ) {
			SecurityUser user = (SecurityUser) actor;
			data.put("userId", user.getUserId());
			data.put("username", user.getEmail());
			data.put("name", user.getDisplayName());
		} else if ( actor instanceof SecurityToken ) {
			SecurityToken token = (SecurityToken) actor;
			data.put("token", token.getToken());
			data.put("tokenType", token.getTokenType());
		}
		return Response.response(data);
	}

}
