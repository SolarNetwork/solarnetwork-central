/* ==================================================================
 * NodesController.java - 28/05/2018 2:34:24 PM
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

import static net.solarnetwork.web.domain.Response.response;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.security.SecurityActor;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.web.domain.Response;

/**
 * Controller for querying node related data.
 * 
 * @author matt
 * @version 1.0
 */
@Controller("v1NodesController")
@RequestMapping("/api/v1/sec/nodes")
@GlobalExceptionRestController
public class NodesController {

	private final QueryBiz queryBiz;

	/**
	 * Constructor.
	 * 
	 * @param queryBiz
	 *        the QueryBiz to use
	 */
	@Autowired
	public NodesController(QueryBiz queryBiz) {
		super();
		this.queryBiz = queryBiz;
	}

	/**
	 * Get a listing of nodes for the active user.
	 * 
	 * @return The list of nodes available to the active user.
	 */
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	@ResponseBody
	public Response<Set<Long>> geAvailableNodes() {
		SecurityActor actor = SecurityUtils.getCurrentActor();
		return response(queryBiz.findAvailableNodes(actor));
	}

}
