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

import static net.solarnetwork.domain.Result.success;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.datum.biz.DatumStreamMetadataBiz;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.domain.ObjectDatumStreamMetadataId;
import net.solarnetwork.central.security.SecurityActor;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.util.ObjectUtils;

/**
 * Controller for managing datum stream metadata.
 *
 * @author matt
 * @version 1.1
 * @since 2.0
 */
@GlobalExceptionRestController
@RestController("v1DatumStreamMetadataController")
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

	/**
	 * Update the ID attributes of a datum stream.
	 *
	 * <p>
	 * One or both of {@code nodeId} and {@code sourceId} must be provided. If
	 * either is {@literal null} that ID will remain unchanged.
	 * </p>
	 *
	 * @param streamId
	 *        the ID of the stream to update
	 * @param nodeId
	 *        the new node ID to associate with the stream
	 * @param sourceId
	 *        the new source ID to associate with the stream
	 * @return the updated stream metadata ID
	 * @throws IllegalArgumentException
	 *         if {@code streamId} is {@literal null} or both {@code nodeId} and
	 *         {@code sourceId} are {@literal null}
	 */
	@RequestMapping(method = RequestMethod.PATCH, path = "/node")
	public Result<ObjectDatumStreamMetadataId> updateNodeDatumStreamMetadataIdAttributes(
			@RequestParam("streamId") UUID streamId,
			@RequestParam(value = "nodeId", required = false) Long nodeId,
			@RequestParam(value = "sourceId", required = false) String sourceId) {
		if ( nodeId == null && sourceId == null ) {
			throw new IllegalArgumentException("One of nodeId or sourceId parameters is required.");
		}
		ObjectDatumStreamMetadataId result = datumStreamMetadataBiz
				.updateIdAttributes(ObjectDatumKind.Node, streamId, nodeId, sourceId);
		return success(result);
	}

	/**
	 * Update the ID attributes of a datum stream.
	 *
	 * <p>
	 * One or both of {@code nodeId} and {@code sourceId} must be provided. If
	 * either is {@literal null} that ID will remain unchanged.
	 * </p>
	 *
	 * @param streamId
	 *        the ID of the stream to update
	 * @param nodeId
	 *        the new node ID to associate with the stream
	 * @param sourceId
	 *        the new source ID to associate with the stream
	 * @return the updated stream metadata ID
	 * @throws IllegalArgumentException
	 *         if {@code streamId} is {@literal null} or both {@code nodeId} and
	 *         {@code sourceId} are {@literal null}
	 */
	@RequestMapping(method = RequestMethod.PATCH, path = "/node/{streamId}")
	public Result<ObjectDatumStreamMetadataId> updateNodeDatumStreamMetadataIdAttributesViaPath(
			@PathVariable("streamId") UUID streamId,
			@RequestParam(value = "nodeId", required = false) Long nodeId,
			@RequestParam(value = "sourceId", required = false) String sourceId) {
		if ( nodeId == null && sourceId == null ) {
			throw new IllegalArgumentException("One of nodeId or sourceId parameters is required.");
		}
		ObjectDatumStreamMetadataId result = datumStreamMetadataBiz
				.updateIdAttributes(ObjectDatumKind.Node, streamId, nodeId, sourceId);
		return success(result);
	}

	/**
	 * Update the attributes of a datum stream.
	 *
	 * <p>
	 * One or both of {@code nodeId} and {@code sourceId} must be provided. If
	 * either is {@literal null} that ID will remain unchanged.
	 * </p>
	 *
	 * @param streamId
	 *        the ID of the stream metadata to update
	 * @param nodeId
	 *        the node ID to set, or {@literal null} to keep unchanged
	 * @param sourceId
	 *        the source ID to set, or {@literal null} to keep unchanged
	 * @param instantaneousProperties
	 *        the instantaneous property names to set, or {@literal null} to
	 *        keep unchanged
	 * @param accumulatingProperties
	 *        the instantaneous property names to set, or {@literal null} to
	 *        keep unchanged
	 * @param statusProperties
	 *        the instantaneous property names to set, or {@literal null} to
	 *        keep unchanged
	 * @return the updated stream metadata, or {@literal null} if the metadata
	 *         was not updated
	 * @throws IllegalArgumentException
	 *         if either {@code kind} or {@code streamId} is {@literal null} or
	 *         all other arguments are {@literal null}
	 * @since 1.1
	 */
	@RequestMapping(method = RequestMethod.POST, path = "/node")
	public Result<ObjectDatumStreamMetadata> updateNodeDatumStreamMetadataAttributes(
			@RequestParam("streamId") UUID streamId,
			@RequestParam(value = "nodeId", required = false) Long nodeId,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(name = "i", required = false) String[] instantaneousProperties,
			@RequestParam(name = "a", required = false) String[] accumulatingProperties,
			@RequestParam(name = "s", required = false) String[] statusProperties) {
		ObjectDatumStreamMetadata result = datumStreamMetadataBiz.updateAttributes(ObjectDatumKind.Node,
				streamId, nodeId, sourceId, instantaneousProperties, accumulatingProperties,
				statusProperties);
		return success(result);
	}

	/**
	 * Update the attributes of a datum stream.
	 *
	 * <p>
	 * One or both of {@code nodeId} and {@code sourceId} must be provided. If
	 * either is {@literal null} that ID will remain unchanged.
	 * </p>
	 *
	 * @param streamId
	 *        the ID of the stream metadata to update
	 * @param nodeId
	 *        the node ID to set, or {@literal null} to keep unchanged
	 * @param sourceId
	 *        the source ID to set, or {@literal null} to keep unchanged
	 * @param instantaneousProperties
	 *        the instantaneous property names to set, or {@literal null} to
	 *        keep unchanged
	 * @param accumulatingProperties
	 *        the instantaneous property names to set, or {@literal null} to
	 *        keep unchanged
	 * @param statusProperties
	 *        the instantaneous property names to set, or {@literal null} to
	 *        keep unchanged
	 * @return the updated stream metadata, or {@literal null} if the metadata
	 *         was not updated
	 * @throws IllegalArgumentException
	 *         if either {@code kind} or {@code streamId} is {@literal null} or
	 *         all other arguments are {@literal null}
	 * @since 1.1
	 */
	@RequestMapping(method = RequestMethod.POST, path = "/node/{streamId}")
	public Result<ObjectDatumStreamMetadata> updateNodeDatumStreamMetadataAttributesViaPath(
			@PathVariable("streamId") UUID streamId,
			@RequestParam(value = "nodeId", required = false) Long nodeId,
			@RequestParam(value = "sourceId", required = false) String sourceId,
			@RequestParam(name = "i", required = false) String[] instantaneousProperties,
			@RequestParam(name = "a", required = false) String[] accumulatingProperties,
			@RequestParam(name = "s", required = false) String[] statusProperties) {
		return updateNodeDatumStreamMetadataAttributes(streamId, nodeId, sourceId,
				instantaneousProperties, accumulatingProperties, statusProperties);
	}

	/**
	 * View the metadata of a datum stream.
	 *
	 * @param streamId
	 *        the ID of the stream metadata to get
	 * @since 1.1
	 */
	@RequestMapping(method = RequestMethod.GET, path = "/node/{streamId}")
	public Result<ObjectDatumStreamMetadata> viewNodeDatumStreamMetadataViaPath(
			@PathVariable("streamId") UUID streamId) {
		BasicDatumCriteria filter = new BasicDatumCriteria();
		filter.setStreamId(streamId);
		SecurityActor actor = SecurityUtils.getCurrentActor();
		List<ObjectDatumStreamMetadata> results = datumStreamMetadataBiz.findDatumStreamMetadata(actor,
				filter);
		return success(results != null && !results.isEmpty() ? results.getFirst() : null);
	}

}
