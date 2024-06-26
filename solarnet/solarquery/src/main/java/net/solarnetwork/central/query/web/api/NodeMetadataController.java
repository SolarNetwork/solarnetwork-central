/* ==================================================================
 * NodeMetadataController.java - 14/11/2016 8:48:21 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

import static net.solarnetwork.central.security.SecurityUtils.authorizedNodeIdsForCurrentActor;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.web.jakarta.domain.Response.response;
import java.util.NoSuchElementException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import jakarta.servlet.http.HttpServletRequest;
import net.solarnetwork.central.biz.SolarNodeMetadataBiz;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SolarNodeMetadataFilterMatch;
import net.solarnetwork.central.web.BaseTransientDataAccessRetryController;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.central.web.WebUtils;
import net.solarnetwork.web.jakarta.domain.Response;

/**
 * Controller for read-only node metadata access.
 * 
 * @author matt
 * @version 2.1
 */
@Controller("v1NodeMetadataController")
@RequestMapping({ "/api/v1/pub/nodes/meta", "/api/v1/sec/nodes/meta" })
@GlobalExceptionRestController
public class NodeMetadataController extends BaseTransientDataAccessRetryController {

	private final SolarNodeOwnershipDao nodeOwnershipDao;
	private final SolarNodeMetadataBiz solarNodeMetadataBiz;

	/**
	 * Constructor.
	 * 
	 * @param nodeOwnershipDao
	 *        the node ownership DAO to use
	 * @param solarNodeMetadataBiz
	 *        the SolarNodeMetadataBiz to use
	 */
	@Autowired
	public NodeMetadataController(SolarNodeOwnershipDao nodeOwnershipDao,
			SolarNodeMetadataBiz solarNodeMetadataBiz) {
		super();
		this.nodeOwnershipDao = requireNonNullArgument(nodeOwnershipDao, "nodeOwnershipDao");
		this.solarNodeMetadataBiz = requireNonNullArgument(solarNodeMetadataBiz, "solarNodeMetadataBiz");
	}

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.setIgnoreInvalidFields(true);
	}

	/**
	 * Find all metadata for any number of node IDs.
	 * 
	 * @param criteria
	 *        any sort or limit criteria
	 * @return the results
	 */
	@ResponseBody
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	public Response<FilterResults<SolarNodeMetadataFilterMatch>> findMetadata(
			final HttpServletRequest req, final DatumFilterCommand criteria) {
		return WebUtils.doWithTransientDataAccessExceptionRetry(() -> {
			if ( criteria.getNodeId() == null ) {
				// default to all nodes for actor
				criteria.setNodeIds(authorizedNodeIdsForCurrentActor(nodeOwnershipDao));
			}
			FilterResults<SolarNodeMetadataFilterMatch> results = solarNodeMetadataBiz
					.findSolarNodeMetadata(criteria, criteria.getSortDescriptors(), criteria.getOffset(),
							criteria.getMax());
			return response(results);
		}, req, getTransientExceptionRetryCount(), getTransientExceptionRetryDelay(), log);
	}

	/**
	 * Find all metadata for a specific node ID.
	 * 
	 * @param nodeId
	 *        the node ID to find
	 * @return the results
	 */
	@ResponseBody
	@RequestMapping(value = { "/{nodeId}" }, method = RequestMethod.GET)
	public Response<SolarNodeMetadataFilterMatch> getMetadata(final HttpServletRequest req,
			@PathVariable("nodeId") final Long nodeId) {
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setNodeId(nodeId);
		return WebUtils.doWithTransientDataAccessExceptionRetry(() -> {
			FilterResults<SolarNodeMetadataFilterMatch> results = solarNodeMetadataBiz
					.findSolarNodeMetadata(criteria, null, null, null);
			SolarNodeMetadataFilterMatch result = null;
			if ( results != null ) {
				try {
					result = results.iterator().next();
				} catch ( NoSuchElementException e ) {
					// ignore
				}
			}
			return response(result);
		}, req, getTransientExceptionRetryCount(), getTransientExceptionRetryDelay(), log);
	}

}
