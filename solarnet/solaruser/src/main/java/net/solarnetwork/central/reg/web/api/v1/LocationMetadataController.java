/* ==================================================================
 * LocationMetadataController.java - Oct 19, 2014 5:08:40 PM
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

package net.solarnetwork.central.reg.web.api.v1;

import static net.solarnetwork.central.security.SecurityUtils.getCurrentActorUserId;
import static net.solarnetwork.domain.Result.success;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import jakarta.validation.Valid;
import net.solarnetwork.central.common.dao.BasicLocationRequestCriteria;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.LocationSourcePK;
import net.solarnetwork.central.domain.LocationRequest;
import net.solarnetwork.central.domain.LocationRequestInfo;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.reg.web.domain.LocationRequestInfoValidator;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Result;
import net.solarnetwork.domain.datum.GeneralDatumMetadata;

/**
 * Controller for location metadata actions.
 *
 * @author matt
 * @version 2.2
 */
@Controller("v1LocationMetadataController")
@RequestMapping({ "/api/v1/pub/location/meta", "/api/v1/sec/location/meta" })
@GlobalExceptionRestController
public class LocationMetadataController {

	private final DatumMetadataBiz datumMetadataBiz;
	private final Validator locationRequestInfoValidator;

	/**
	 * Constructor.
	 *
	 * @param datumMetadataBiz
	 *        the DatumMetadataBiz to use
	 */
	@Autowired
	public LocationMetadataController(DatumMetadataBiz datumMetadataBiz,
			@Qualifier(LocationRequestInfoValidator.LOCATION_REQUEST_INFO) Validator locationRequestInfoValidator) {
		super();
		this.datumMetadataBiz = requireNonNullArgument(datumMetadataBiz, "datumMetadataBiz");
		this.locationRequestInfoValidator = requireNonNullArgument(locationRequestInfoValidator,
				"locationRequestInfoValidator");
	}

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.setIgnoreInvalidFields(true);
	}

	/**
	 * Query for metadata.
	 *
	 * @param query
	 *        a general search query
	 * @param command
	 *        specific criteria, such as source ID, sort order, max results,
	 *        etc.
	 * @return the results
	 * @since 1.2
	 */
	@ResponseBody
	@RequestMapping(value = { "", "/", "/query" }, method = RequestMethod.GET)
	public Result<FilterResults<GeneralLocationDatumMetadataFilterMatch, LocationSourcePK>> findGeneralLocations(
			@RequestParam(value = "query", required = false) String query, DatumFilterCommand command) {
		SolarLocation loc;
		if ( command != null ) {
			loc = new SolarLocation(command.getLocation());
		} else {
			loc = new SolarLocation();
		}
		if ( query != null ) {
			loc.setName(query);
		}
		DatumFilterCommand criteria = new DatumFilterCommand(loc);
		if ( command != null ) {
			if ( command.getLocationIds() != null ) {
				criteria.setLocationIds(command.getLocationIds());
			}
			if ( command.getSourceIds() != null ) {
				criteria.setSourceIds(command.getSourceIds());
			}
			if ( command.getTags() != null ) {
				criteria.setTags(command.getTags());
			}
		}
		FilterResults<GeneralLocationDatumMetadataFilterMatch, LocationSourcePK> results = datumMetadataBiz
				.findGeneralLocationDatumMetadata(criteria, command.getSortDescriptors(),
						command.getOffset(), command.getMax());
		return success(results);
	}

	/**
	 * Find all metadata for a location ID.
	 *
	 * @param locationId
	 *        the location ID
	 * @param criteria
	 *        any sort or limit criteria
	 * @return the results
	 */
	@ResponseBody
	@RequestMapping(value = "/{locationId}", method = RequestMethod.GET)
	public Result<FilterResults<GeneralLocationDatumMetadataFilterMatch, LocationSourcePK>> findMetadata(
			@PathVariable("locationId") Long locationId, DatumFilterCommand criteria) {
		return findMetadata(locationId, null, criteria);
	}

	/**
	 * Get metadata for a single location ID and source ID combination.
	 *
	 * @param locationId
	 *        the location ID
	 * @param sourceId
	 *        the source ID
	 * @param criteria
	 *        any sort or limit criteria
	 * @return the results
	 */
	@ResponseBody
	@RequestMapping(value = { "/{locationId}/{sourceId}" }, method = RequestMethod.GET)
	public Result<FilterResults<GeneralLocationDatumMetadataFilterMatch, LocationSourcePK>> findMetadata(
			@PathVariable("locationId") Long locationId, @PathVariable("sourceId") String sourceId,
			DatumFilterCommand criteria) {
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setLocationId(locationId);
		filter.setSourceId(sourceId);
		FilterResults<GeneralLocationDatumMetadataFilterMatch, LocationSourcePK> results = datumMetadataBiz
				.findGeneralLocationDatumMetadata(filter, criteria.getSortDescriptors(),
						criteria.getOffset(), criteria.getMax());
		return success(results);
	}

	@ResponseBody
	@RequestMapping(value = "/{locationId}", method = RequestMethod.GET, params = { "sourceId" })
	public Result<FilterResults<GeneralLocationDatumMetadataFilterMatch, LocationSourcePK>> findMetadataAlt(
			@PathVariable("locationId") Long locationId, @RequestParam("sourceId") String sourceId,
			DatumFilterCommand criteria) {
		return findMetadata(locationId, sourceId, criteria);
	}

	/**
	 * Add metadata to a source. The metadata is merged only, and will not
	 * replace existing values.
	 *
	 * @param locationId
	 *        the location ID
	 * @param sourceId
	 *        the source ID
	 * @param meta
	 *        the metadata to merge
	 * @return the results
	 */
	@ResponseBody
	@RequestMapping(value = { "/{locationId}/{sourceId}" }, method = RequestMethod.POST)
	public Result<Object> addMetadata(@PathVariable("locationId") Long locationId,
			@PathVariable("sourceId") String sourceId, @RequestBody GeneralDatumMetadata meta) {
		datumMetadataBiz.addGeneralLocationDatumMetadata(locationId, sourceId, meta);
		return success();
	}

	@ResponseBody
	@RequestMapping(value = "/{locationId}", method = RequestMethod.POST, params = { "sourceId" })
	public Result<Object> addMetadataAlt(@PathVariable("locationId") Long locationId,
			@RequestParam("sourceId") String sourceId, @RequestBody GeneralDatumMetadata meta) {
		return addMetadata(locationId, sourceId, meta);
	}

	/**
	 * Completely replace the metadata for a given source ID, or create it if it
	 * doesn't already exist.
	 *
	 * @param locationId
	 *        the location ID
	 * @param sourceId
	 *        the source ID
	 * @param meta
	 *        the metadata to store
	 * @return the results
	 */
	@ResponseBody
	@RequestMapping(value = { "/{locationId}/{sourceId}" }, method = RequestMethod.PUT)
	public Result<Object> replaceMetadata(@PathVariable("locationId") Long locationId,
			@PathVariable("sourceId") String sourceId, @RequestBody GeneralDatumMetadata meta) {
		datumMetadataBiz.storeGeneralLocationDatumMetadata(locationId, sourceId, meta);
		return success();
	}

	@ResponseBody
	@RequestMapping(value = "/{locationId}", method = RequestMethod.PUT, params = { "sourceId" })
	public Result<Object> replaceMetadataAlt(@PathVariable("locationId") Long locationId,
			@RequestParam("sourceId") String sourceId, @RequestBody GeneralDatumMetadata meta) {
		return replaceMetadata(locationId, sourceId, meta);
	}

	/**
	 * Completely remove the metadata for a given source ID.
	 *
	 * @param locationId
	 *        the location ID
	 * @param sourceId
	 *        the source ID
	 * @return the results
	 */
	@ResponseBody
	@RequestMapping(value = "/{locationId}/{sourceId}", method = RequestMethod.DELETE)
	public Result<Object> deleteMetadata(@PathVariable("locationId") Long locationId,
			@PathVariable("sourceId") String sourceId) {
		datumMetadataBiz.removeGeneralLocationDatumMetadata(locationId, sourceId);
		return success();
	}

	@ResponseBody
	@RequestMapping(value = "/{locationId}", method = RequestMethod.DELETE, params = { "sourceId" })
	public Result<Object> deleteMetadataAlt(@PathVariable("locationId") Long locationId,
			@RequestParam("sourceId") String sourceId) {
		return deleteMetadata(locationId, sourceId);
	}

	/**
	 * List location requests matching a search filter.
	 *
	 * @param filter
	 *        the search filter
	 * @return the matching results
	 */
	@ResponseBody
	@RequestMapping(value = "/request", method = RequestMethod.GET)
	public Result<net.solarnetwork.dao.FilterResults<LocationRequest, Long>> findLocationRequests(
			BasicLocationRequestCriteria filter) {
		return success(datumMetadataBiz.findLocationRequests(getCurrentActorUserId(), filter, null, null,
				null));
	}

	@InitBinder(value = LocationRequestInfoValidator.LOCATION_REQUEST_INFO)
	public void initLocationRequestInfoBinder(WebDataBinder binder) {
		binder.setValidator(locationRequestInfoValidator);
	}

	/**
	 * Submit a location request.
	 *
	 * @param locationRequestInfo
	 *        the info to submit
	 * @return an empty result
	 */
	@ResponseBody
	@RequestMapping(value = "/request", method = RequestMethod.POST)
	public Result<LocationRequest> submitLocationRequest(
			@RequestBody @Valid LocationRequestInfo locationRequestInfo) {
		return success(
				datumMetadataBiz.submitLocationRequest(getCurrentActorUserId(), locationRequestInfo));
	}

	/**
	 * View a specific location request.
	 *
	 * @param id
	 *        the ID of the request to view
	 * @return the request, or an empty result
	 */
	@ResponseBody
	@RequestMapping(value = "/request/{id}", method = RequestMethod.GET)
	public Result<LocationRequest> getLocationRequest(@PathVariable("id") Long id) {
		return success(datumMetadataBiz.getLocationRequest(getCurrentActorUserId(), id));
	}

	/**
	 * Delete a specific location request.
	 *
	 * @param id
	 *        the ID of the request to delete
	 * @return an empty result
	 */
	@ResponseBody
	@RequestMapping(value = "/request/{id}", method = RequestMethod.POST)
	public Result<LocationRequest> updateLocationRequest(@PathVariable("id") Long id,
			@RequestBody @Valid LocationRequestInfo locationRequestInfo) {
		return success(datumMetadataBiz.updateLocationRequest(getCurrentActorUserId(), id,
				locationRequestInfo));
	}

	/**
	 * Delete a specific location request.
	 *
	 * @param id
	 *        the ID of the request to delete
	 * @return an empty result
	 */
	@ResponseBody
	@RequestMapping(value = "/request/{id}", method = RequestMethod.DELETE)
	public Result<LocationRequest> deleteLocationRequest(@PathVariable("id") Long id) {
		datumMetadataBiz.removeLocationRequest(getCurrentActorUserId(), id);
		return success();
	}

}
