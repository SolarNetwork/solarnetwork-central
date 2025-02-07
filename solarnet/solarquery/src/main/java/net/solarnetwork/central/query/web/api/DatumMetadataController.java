/* ==================================================================
 * DatumMetadataController.java - Oct 3, 2014 2:13:51 PM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.domain.PaginationFilter;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Result;

/**
 * Controller for datum metadata actions.
 * 
 * @author matt
 * @version 2.0
 */
@Controller("v1DatumMetadataController")
@RequestMapping({ "/api/v1/pub/datum/meta/{nodeId}", "/api/v1/sec/datum/meta/{nodeId}" })
@Tag(name = "datum-meta", description = "Methods to query datum stream metadata.")
@GlobalExceptionRestController
public class DatumMetadataController {

	private final DatumMetadataBiz datumMetadataBiz;

	/**
	 * Constructor.
	 * 
	 * @param datumMetadataBiz
	 *        the DatumMetadataBiz to use
	 */
	@Autowired
	public DatumMetadataController(DatumMetadataBiz datumMetadataBiz) {
		super();
		this.datumMetadataBiz = datumMetadataBiz;
	}

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.setIgnoreInvalidFields(true);
	}

	/**
	 * Find all metadata for a node ID.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the optional source ID
	 * @param criteria
	 *        any sort or limit criteria
	 * @return the results
	 */

	@Operation(operationId = "datumMetadataListForNode",
			summary = "List datum stream metadata for a node",
			parameters = {
					@Parameter(name = "nodeId", in = ParameterIn.PATH, description = "The node ID."),
					@Parameter(name = "sourceId", in = ParameterIn.QUERY, required = false,
							description = "The source ID."),
					@Parameter(name = "criteria", description = "The pagination criteria.",
							schema = @Schema(implementation = PaginationFilter.class),
							style = ParameterStyle.FORM, explode = Explode.TRUE) })
	@ResponseBody
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	public Result<FilterResults<GeneralNodeDatumMetadataFilterMatch, NodeSourcePK>> findMetadataForNode(
			@PathVariable("nodeId") Long nodeId,
			@RequestParam(name = "sourceId", required = false) String sourceId,
			DatumFilterCommand criteria) {
		return success(findForNodeAndSource(nodeId, sourceId, criteria));
	}

	/**
	 * Get metadata for a single node ID and source ID combination.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @param sourceId
	 *        the source ID
	 * @param criteria
	 *        any sort or limit criteria
	 * @return the results
	 * @deprecated use
	 *             {@link #findMetadataForNode(Long, String, DatumFilterCommand)}
	 *             instead
	 */
	@Operation(operationId = "datumMetadataListForNodeSource", deprecated = true,
			summary = "List datum stream metadata for a node and source",
			description = """
					This API accepts the source ID as a URL path parameter, but source IDs often contain slash
					delimiters, making them unsuitable for URL paths. Instead, provide the source ID as a
					`sourceId` query parameter.""",
			parameters = {
					@Parameter(name = "nodeId", in = ParameterIn.PATH, description = "The node ID."),
					@Parameter(name = "sourceId", in = ParameterIn.PATH, description = "The source ID."),
					@Parameter(name = "criteria", description = "The pagination criteria.",
							schema = @Schema(implementation = PaginationFilter.class),
							style = ParameterStyle.FORM, explode = Explode.TRUE) })
	@Deprecated
	@ResponseBody
	@RequestMapping(value = { "/{sourceId}" }, method = RequestMethod.GET)
	public Result<FilterResults<GeneralNodeDatumMetadataFilterMatch, NodeSourcePK>> findMetadataForSource(
			@PathVariable("nodeId") Long nodeId, @PathVariable("sourceId") String sourceId,
			DatumFilterCommand criteria) {
		return success(findForNodeAndSource(nodeId, sourceId, criteria));
	}

	private FilterResults<GeneralNodeDatumMetadataFilterMatch, NodeSourcePK> findForNodeAndSource(
			Long nodeId, String sourceId, DatumFilterCommand criteria) {
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(nodeId);
		filter.setSourceId(sourceId);
		return datumMetadataBiz.findGeneralNodeDatumMetadata(filter, criteria.getSortDescriptors(),
				criteria.getOffset(), criteria.getMax());
	}

}
