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
import net.solarnetwork.central.datum.domain.GeneralLocationDatumMetadataFilterMatch;
import net.solarnetwork.central.datum.domain.LocationSourcePK;
import net.solarnetwork.central.domain.PaginationFilter;
import net.solarnetwork.central.domain.SolarLocation;
import net.solarnetwork.central.query.domain.LocationDatumMetadataSearchFilter;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Result;

/**
 * Controller for location metadata actions.
 * 
 * @author matt
 * @version 2.0
 */
@Controller("v1LocationMetadataController")
@RequestMapping({ "/api/v1/pub/location/meta", "/api/v1/sec/location/meta" })
@Tag(name = "location-meta", description = "Methods to query location datum stream metadata.")
@GlobalExceptionRestController
public class LocationMetadataController {

	private final DatumMetadataBiz datumMetadataBiz;

	/**
	 * Constructor.
	 * 
	 * @param datumMetadataBiz
	 *        the DatumMetadataBiz to use
	 */
	@Autowired
	public LocationMetadataController(DatumMetadataBiz datumMetadataBiz) {
		super();
		this.datumMetadataBiz = datumMetadataBiz;
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
	 * @param criteria
	 *        specific criteria, such as source ID, sort order, max results,
	 *        etc.
	 * @return the results
	 * @since 1.2
	 */
	@Operation(operationId = "locationDatumMetadataList",
			summary = "List location datum stream metadata matching search criteria",
			parameters = {
					@Parameter(name = "query", in = ParameterIn.QUERY, required = false,
							description = "A general text matching criteria."),
					@Parameter(name = "criteria", description = """
							The search and pagination criteria, such as location and source IDs.""",
							schema = @Schema(implementation = LocationDatumMetadataSearchFilter.class),
							style = ParameterStyle.FORM, explode = Explode.TRUE) })
	@ResponseBody
	@RequestMapping(value = { "", "/", "/query" }, method = RequestMethod.GET)
	public Result<FilterResults<GeneralLocationDatumMetadataFilterMatch, LocationSourcePK>> findGeneralLocations(
			@RequestParam(value = "query", required = false) String query, DatumFilterCommand criteria) {
		SolarLocation loc;
		if ( criteria != null ) {
			loc = new SolarLocation(criteria.getLocation());
		} else {
			loc = new SolarLocation();
		}
		if ( query != null ) {
			loc.setName(query);
		}
		DatumFilterCommand c = new DatumFilterCommand(loc);
		if ( criteria != null ) {
			if ( criteria.getLocationIds() != null ) {
				c.setLocationIds(criteria.getLocationIds());
			}
			if ( criteria.getSourceIds() != null ) {
				c.setSourceIds(criteria.getSourceIds());
			}
			if ( criteria.getTags() != null ) {
				c.setTags(criteria.getTags());
			}
		}
		FilterResults<GeneralLocationDatumMetadataFilterMatch, LocationSourcePK> results = datumMetadataBiz
				.findGeneralLocationDatumMetadata(c, criteria.getSortDescriptors(), criteria.getOffset(),
						criteria.getMax());
		return success(results);
	}

	/**
	 * Find all metadata for a location ID.
	 * 
	 * @param locationId
	 *        the location ID
	 * @param sourceId
	 *        an optional source ID to match
	 * @param criteria
	 *        any sort or limit criteria
	 * @return the results
	 */
	@Operation(operationId = "locationDatumMetadataListForLocation",
			summary = "List location datum stream metadata for a location",
			parameters = {
					@Parameter(name = "locationId", in = ParameterIn.PATH,
							description = "The location ID to list metadata for."),
					@Parameter(name = "sourceId", in = ParameterIn.QUERY, required = false,
							description = "The source ID to restrict results to."),
					@Parameter(name = "criteria", description = """
							The search and pagination criteria, such as location and source IDs.""",
							schema = @Schema(implementation = PaginationFilter.class),
							style = ParameterStyle.FORM, explode = Explode.TRUE) })
	@ResponseBody
	@RequestMapping(value = "/{locationId}", method = RequestMethod.GET)
	public Result<FilterResults<GeneralLocationDatumMetadataFilterMatch, LocationSourcePK>> findMetadataForLocation(
			@PathVariable("locationId") Long locationId,
			@RequestParam(name = "sourceId", required = false) String sourceId,
			DatumFilterCommand criteria) {
		return success(findForLocationAndSource(locationId, sourceId, criteria));
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
	 * @deprecated use
	 *             {@link #findMetadataForLocation(Long, String, DatumFilterCommand)}
	 *             instead
	 */
	@Operation(operationId = "datumMetadataListForLocationSource", deprecated = true,
			summary = "List location datum stream metadata for a location and source",
			description = """
					This API accepts the source ID as a URL path parameter, but source IDs often contain slash
					delimiters, making them unsuitable for URL paths. Instead, provide the source ID as a
					`sourceId` query parameter.""",
			parameters = {
					@Parameter(name = "locationId", in = ParameterIn.PATH,
							description = "The location ID."),
					@Parameter(name = "sourceId", in = ParameterIn.PATH, description = "The source ID."),
					@Parameter(name = "criteria", description = "The pagination criteria.",
							schema = @Schema(implementation = PaginationFilter.class),
							style = ParameterStyle.FORM, explode = Explode.TRUE) })
	@Deprecated
	@ResponseBody
	@RequestMapping(value = { "/{locationId}/{sourceId}" }, method = RequestMethod.GET)
	public Result<FilterResults<GeneralLocationDatumMetadataFilterMatch, LocationSourcePK>> findMetadata(
			@PathVariable("locationId") Long locationId, @PathVariable("sourceId") String sourceId,
			DatumFilterCommand criteria) {
		return success(findForLocationAndSource(locationId, sourceId, criteria));
	}

	private FilterResults<GeneralLocationDatumMetadataFilterMatch, LocationSourcePK> findForLocationAndSource(
			Long locationId, String sourceId, DatumFilterCommand criteria) {
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setLocationId(locationId);
		filter.setSourceId(sourceId);
		return datumMetadataBiz.findGeneralLocationDatumMetadata(filter, criteria.getSortDescriptors(),
				criteria.getOffset(), criteria.getMax());
	}

}
