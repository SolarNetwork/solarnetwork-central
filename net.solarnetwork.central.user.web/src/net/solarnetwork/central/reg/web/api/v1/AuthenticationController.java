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
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import net.solarnetwork.central.security.SecurityUser;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import net.solarnetwork.web.domain.Response;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Remote authentication for nodes.
 * 
 * @author matt
 * @version 1.1
 */
@Controller("v1authenticationController")
public class AuthenticationController extends WebServiceControllerSupport {

	@Resource
	private AuthenticationManager authenticationManager;

	@ExceptionHandler(AuthenticationException.class)
	@ResponseBody
	public Response<?> handleException(AuthenticationException e, HttpServletResponse response) {
		log.debug("AuthenticationException in {} controller: {}", getClass().getSimpleName(),
				e.getMessage());
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		return new Response<Object>(Boolean.FALSE, null, e.getMessage(), null);
	}

	@ResponseBody
	@RequestMapping(value = "/v1/pub/authenticate", method = RequestMethod.GET)
	public Response<?> authenticate(@RequestParam String username, @RequestParam String password) {
		UsernamePasswordAuthenticationToken tok = new UsernamePasswordAuthenticationToken(username,
				password);
		Authentication auth = authenticationManager.authenticate(tok);
		Map<String, Object> data = new LinkedHashMap<String, Object>(3);
		SecurityUser user = (SecurityUser) auth.getPrincipal();
		data.put("username", user.getEmail());
		data.put("userId", user.getUserId());
		data.put("name", user.getDisplayName());
		return new Response<Object>(data);
	}

	public void setAuthenticationManager(AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

}
