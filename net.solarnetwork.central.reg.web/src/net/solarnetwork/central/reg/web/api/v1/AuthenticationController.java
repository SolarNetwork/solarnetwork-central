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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.reg.web.api.v1;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Resource;

import net.solarnetwork.central.reg.web.api.domain.Response;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Remote authentication for nodes.
 * 
 * @author matt
 * @version $Revision$
 */
@Controller
public class AuthenticationController {

	@Resource
	private AuthenticationManager authenticationManager;
	
	@ResponseBody
	@RequestMapping("/api/v1/pub/authenticate")
	public Response authenticate(String username, String password) {
		UsernamePasswordAuthenticationToken tok = new UsernamePasswordAuthenticationToken(username, password);
		Authentication auth = authenticationManager.authenticate(tok);
		Map<String, Object> data = new LinkedHashMap<String, Object>(3);
		data.put("principal", auth.getPrincipal());
		return new Response(data);
	}
	
}
