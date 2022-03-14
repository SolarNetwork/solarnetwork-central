/* ==================================================================
 * NodeDataController.java - 15/10/2016 7:14:56 AM
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

package net.solarnetwork.central.reg.web;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import static net.solarnetwork.web.domain.Response.response;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.web.domain.Response;

/**
 * REST controller to support data queries.
 * 
 * @author matt
 * @version 1.1
 */
@RestController("v1NodeDataController")
@RequestMapping(value = "/u/sec/node-data")
@GlobalExceptionRestController
public class NodeDataController {

	private final DatumMetadataBiz datumMetadataBiz;

	@Autowired
	public NodeDataController(DatumMetadataBiz datumMetadataBiz) {
		super();
		this.datumMetadataBiz = requireNonNullArgument(datumMetadataBiz, "datumMetadataBiz");
	}

	/**
	 * Get the set of source IDs available for the available GeneralNodeData for
	 * a single node, optionally constrained within a date range.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @return the available sources
	 */
	@ResponseBody
	@RequestMapping(value = "/{nodeId}/sources", method = RequestMethod.GET)
	public Response<Set<String>> getAvailableSources(@PathVariable("nodeId") Long nodeId) {
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setNodeId(nodeId);
		Set<ObjectDatumStreamMetadataId> data = datumMetadataBiz.findDatumStreamMetadataIds(filter);
		Set<String> sourceIds = data.stream().map(ObjectDatumStreamMetadataId::getSourceId)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		return response(sourceIds);
	}

}
