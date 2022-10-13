/* ==================================================================
 * LocationLookupController.java - Nov 19, 2013 7:30:21 AM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilterMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.SecurityUtils;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.domain.Location;
import net.solarnetwork.web.domain.Response;

/**
 * Controller for querying location data.
 * 
 * @author matt
 * @version 2.4
 */
@Controller("v1LocationLookupController")
@RequestMapping({ "/solarin/api/v1/pub/location", "/solarin/api/v1/sec/location" })
@GlobalExceptionRestController
public class LocationLookupController {

	private final DataCollectorBiz dataCollectorBiz;

	/**
	 * Constructor.
	 * 
	 * @param dataCollectorBiz
	 *        the DataCollectorBiz to use
	 */
	@Autowired
	public LocationLookupController(DataCollectorBiz dataCollectorBiz) {
		super();
		this.dataCollectorBiz = dataCollectorBiz;
	}

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.setIgnoreInvalidFields(true);
	}

	/**
	 * Query for general location datum metadata.
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
	@RequestMapping(value = { "", "/", "/query" }, method = RequestMethod.GET, params = "!type")
	public Response<FilterResults<GeneralLocationDatumMetadataFilterMatch>> findGeneralLocationMetadata(
			@RequestParam(value = "query", required = false) String query, DatumFilterCommand command) {
		SolarLocation loc;
		if ( command != null ) {
			loc = new SolarLocation(command.getLocation());
		} else {
			loc = new SolarLocation();
		}
		if ( query != null ) {
			loc.setName(query);
		} else if ( loc.getRegion() != null ) {
			// backwards-compat for SolarNode that posts query as location.region
			loc.setName(loc.getRegion());
			loc.setRegion(null);
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
		FilterResults<GeneralLocationDatumMetadataFilterMatch> results = dataCollectorBiz
				.findGeneralLocationDatumMetadata(criteria, command.getSortDescriptors(),
						command.getOffset(), command.getMax());
		return response(results);
	}

	/**
	 * Query for general location datum metadata.
	 * 
	 * @param locationId
	 *        a location ID
	 * @param sourceId
	 *        the source ID
	 * @return the results
	 * @since 1.2
	 */
	@ResponseBody
	@RequestMapping(value = { "/{locationId}" }, method = RequestMethod.GET)
	public Response<GeneralLocationDatumMetadataFilterMatch> getGeneralLocationMetadata(
			@PathVariable("locationId") Long locationId,
			@RequestParam(value = "sourceId") String sourceId) {
		DatumFilterCommand criteria = new DatumFilterCommand();
		criteria.setLocationId(locationId);
		criteria.setSourceId(sourceId);
		FilterResults<GeneralLocationDatumMetadataFilterMatch> results = dataCollectorBiz
				.findGeneralLocationDatumMetadata(criteria, null, 0, 1);
		if ( results.getReturnedResultCount() < 1 ) {
			throw new AuthorizationException(AuthorizationException.Reason.UNKNOWN_OBJECT, sourceId);
		}
		return response(results.getResults().iterator().next());
	}

	/**
	 * Update a node's own location details.
	 * 
	 * <p>
	 * Only authenticated nodes are allowed to call this method. This is
	 * designed for nodes to be able to update their own GPS coordinates
	 * primarily.
	 * </p>
	 * 
	 * @param location
	 *        the location details to save
	 * @return the response
	 * @since 2.2
	 */
	@ResponseBody
	@RequestMapping(value = { "/update" }, method = RequestMethod.POST)
	public Response<Void> updateLocation(@RequestBody Location location) {
		Long nodeId = SecurityUtils.getCurrentNode().getNodeId();
		dataCollectorBiz.updateLocation(nodeId, location);
		return response(null);
	}

	/**
	 * Get the location for a node.
	 * 
	 * <p>
	 * Only authenticated nodes are allowed to call this method.
	 * </p>
	 * 
	 * @return the location details of the node
	 * @since 2.4
	 */
	@ResponseBody
	@RequestMapping(value = { "/view" }, method = RequestMethod.POST)
	public Response<Location> getLocation() {
		Long nodeId = SecurityUtils.getCurrentNode().getNodeId();
		return response(dataCollectorBiz.getLocationForNode(nodeId));
	}

}
