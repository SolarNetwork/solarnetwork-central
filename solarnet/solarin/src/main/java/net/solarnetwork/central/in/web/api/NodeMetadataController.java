/* ==================================================================
 * NodeMetadataController.java - 21/06/2017 1:27:20 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.in.web.api;

import static net.solarnetwork.web.domain.Response.response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilterMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;
import net.solarnetwork.web.domain.Response;

/**
 * Controller for node metadata actions.
 * 
 * @author matt
 * @version 2.0
 * @since 1.21
 */
@Controller("v1NodeMetadataController")
@RequestMapping({ "/solarin/api/v1/pub/nodes/meta", "/solarin/api/v1/sec/nodes/meta" })
@GlobalExceptionRestController
public class NodeMetadataController {

	private final DataCollectorBiz dataCollectorBiz;

	/**
	 * Constructor.
	 * 
	 * @param dataCollectorBiz
	 *        the DataCollectorBiz to use
	 */
	@Autowired
	public NodeMetadataController(DataCollectorBiz dataCollectorBiz) {
		super();
		this.dataCollectorBiz = dataCollectorBiz;
	}

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.setIgnoreInvalidFields(true);
	}

	@ResponseBody
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	public Response<FilterResults<GeneralNodeDatumMetadataFilterMatch>> findMetadata(
			DatumFilterCommand criteria) {
		final Long nodeId = SecurityUtils.getCurrentNode().getNodeId();
		return findMetadata(nodeId, criteria);
	}

	@ResponseBody
	@RequestMapping(value = { "/{nodeId}" }, method = RequestMethod.GET)
	public Response<FilterResults<GeneralNodeDatumMetadataFilterMatch>> findMetadata(
			@PathVariable("nodeId") Long requestNodeId, DatumFilterCommand criteria) {
		final Long nodeId = SecurityUtils.getCurrentNode().getNodeId();
		if ( !nodeId.equals(requestNodeId) ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, requestNodeId);
		}
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(nodeId);
		FilterResults<GeneralNodeDatumMetadataFilterMatch> results = dataCollectorBiz
				.findGeneralNodeDatumMetadata(filter, criteria.getSortDescriptors(),
						criteria.getOffset(), criteria.getMax());
		return response(results);
	}

	@ResponseBody
	@RequestMapping(value = { "", "/" }, method = RequestMethod.POST)
	public Response<Object> addMetadata(@RequestBody GeneralDatumMetadata meta) {
		final Long nodeId = SecurityUtils.getCurrentNode().getNodeId();
		return addMetadata(nodeId, meta);
	}

	@ResponseBody
	@RequestMapping(value = { "/{nodeId}" }, method = RequestMethod.POST)
	public Response<Object> addMetadata(@PathVariable("nodeId") Long requestNodeId,
			@RequestBody GeneralDatumMetadata meta) {
		final Long nodeId = SecurityUtils.getCurrentNode().getNodeId();
		if ( !nodeId.equals(requestNodeId) ) {
			throw new AuthorizationException(Reason.ACCESS_DENIED, requestNodeId);
		}
		dataCollectorBiz.addSolarNodeMetadata(nodeId, meta);
		return response(null);
	}

}
