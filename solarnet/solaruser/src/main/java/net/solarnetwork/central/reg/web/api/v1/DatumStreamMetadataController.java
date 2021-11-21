/* ==================================================================
 * DatumStreamMetadataController.java - 21/11/2021 3:09:54 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

import static net.solarnetwork.web.domain.Response.response;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.central.datum.biz.DatumStreamMetadataBiz;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.util.ObjectUtils;
import net.solarnetwork.web.domain.Response;

/**
 * Controller for managing datum stream metadata.
 * 
 * @author matt
 * @version 1.0
 * @since 2.0
 */
@GlobalExceptionRestController
@Controller("v1DatumStreamMetadataController")
@RequestMapping(value = "/api/v1/sec/datum/stream/meta")
public class DatumStreamMetadataController {

	private final DatumStreamMetadataBiz datumStreamMetadataBiz;

	/**
	 * Constructor.
	 * 
	 * @param datumStreamMetadataBiz
	 *        the stream metadata biz to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DatumStreamMetadataController(DatumStreamMetadataBiz datumStreamMetadataBiz) {
		super();
		this.datumStreamMetadataBiz = ObjectUtils.requireNonNullArgument(datumStreamMetadataBiz,
				"datumStreamMetadataBiz");
	}

	@RequestMapping(method = RequestMethod.PATCH, path = "/node")
	@ResponseBody
	public Response<ObjectDatumStreamMetadataId> updateNodeDatumStreamMetadataIdAttributes(
			@RequestParam("streamId") UUID streamId,
			@RequestParam(value = "nodeId", required = false) Long nodeId,
			@RequestParam(value = "sourceId", required = false) String sourceId) {
		if ( nodeId == null && sourceId == null ) {
			throw new IllegalArgumentException("One of nodeId or sourceId parameters is required.");
		}
		ObjectDatumStreamMetadataId result = datumStreamMetadataBiz
				.updateIdAttributes(ObjectDatumKind.Node, streamId, nodeId, sourceId);
		return response(result);
	}

	@RequestMapping(method = RequestMethod.PATCH, path = "/node/{streamId}")
	@ResponseBody
	public Response<ObjectDatumStreamMetadataId> updateNodeDatumStreamMetadataIdAttributesViaPath(
			@PathVariable("streamId") UUID streamId,
			@RequestParam(value = "nodeId", required = false) Long nodeId,
			@RequestParam(value = "sourceId", required = false) String sourceId) {
		if ( nodeId == null && sourceId == null ) {
			throw new IllegalArgumentException("One of nodeId or sourceId parameters is required.");
		}
		ObjectDatumStreamMetadataId result = datumStreamMetadataBiz
				.updateIdAttributes(ObjectDatumKind.Node, streamId, nodeId, sourceId);
		return response(result);
	}

}
