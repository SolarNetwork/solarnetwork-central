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

import static net.solarnetwork.domain.Result.success;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import net.solarnetwork.central.datum.domain.DatumFilter;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.security.SecurityActor;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.web.BaseTransientDataAccessRetryController;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.central.web.WebUtils;
import net.solarnetwork.domain.Result;

/**
 * Controller for querying node related data.
 * 
 * @author matt
 * @version 1.2
 */
@Controller("v1NodesController")
@RequestMapping("/api/v1/sec/nodes")
@Tag(name = "node", description = "Methods to query nodes.")
@GlobalExceptionRestController
public class NodesController extends BaseTransientDataAccessRetryController {

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
	 * @param req
	 *        the HTTP request
	 * @return The list of nodes available to the active user.
	 */
	@Operation(operationId = "nodeList", summary = "List the node IDs allowed for the active user")
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	@ResponseBody
	public Result<Set<Long>> getAvailableNodes(final HttpServletRequest req) {
		SecurityActor actor = SecurityUtils.getCurrentActor();
		return WebUtils.doWithTransientDataAccessExceptionRetry(() -> {
			Set<Long> result = queryBiz.findAvailableNodes(actor);
			return success(result);
		}, req, getTransientExceptionRetryCount(), getTransientExceptionRetryDelay(), log);
	}

	/**
	 * Find available node sources that match a search criteria.
	 * 
	 * @param req
	 *        the HTTP request
	 * @param criteria
	 *        the search criteria
	 * @return the matching set of node sources
	 * @since 1.1
	 */
	@Operation(operationId = "nodeSourceList",
			summary = "List the node and source ID combinations allowed for the active user",
			parameters = { @Parameter(name = "criteria", description = """
					The search and pagination criteria.""",
					schema = @Schema(implementation = DatumFilter.class), style = ParameterStyle.FORM,
					explode = Explode.TRUE) })
	@ResponseBody
	@RequestMapping(value = "/sources", method = RequestMethod.GET)
	public Result<Set<NodeSourcePK>> findAvailableSources(final HttpServletRequest req,
			final DatumFilterCommand criteria) {
		SecurityActor actor = SecurityUtils.getCurrentActor();
		return WebUtils.doWithTransientDataAccessExceptionRetry(() -> {
			Set<NodeSourcePK> result = queryBiz.findAvailableSources(actor, criteria);
			return success(result);
		}, req, getTransientExceptionRetryCount(), getTransientExceptionRetryDelay(), log);
	}

}
