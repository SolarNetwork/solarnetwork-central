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

package net.solarnetwork.central.query.web.api;

import static net.solarnetwork.domain.Result.error;
import static net.solarnetwork.domain.Result.success;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.solarnetwork.central.domain.LocationMatch;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.query.domain.LocationSearchFilter;
import net.solarnetwork.central.support.SourceLocationFilter;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Result;

/**
 * Controller for querying location data.
 * 
 * @author matt
 * @version 2.0
 */
@Controller("v1LocationLookupController")
@RequestMapping({ "/api/v1/pub/location", "/api/v1/sec/location" })
@Tag(name = "location-lookup", description = "Methods to find location entities.")
@GlobalExceptionRestController
public class LocationLookupController {

	private final QueryBiz queryBiz;

	/**
	 * Constructor.
	 * 
	 * @param queryBiz
	 *        the QueryBiz to use
	 */
	@Autowired
	public LocationLookupController(QueryBiz queryBiz) {
		super();
		this.queryBiz = queryBiz;
	}

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.setIgnoreInvalidFields(true);
	}

	/**
	 * Search for locations.
	 * 
	 * @param criteria
	 *        the search criteria
	 * @return the search results
	 * @since 1.2
	 */
	// @formatter:off
	@Operation(operationId = "locationList",
			summary = "List locations matching filter criteria",
			description = "Query for locations that match criteria like country and postal code.",
			parameters = @Parameter(name = "criteria", description = "The search criteria", 
					schema = @Schema(implementation = LocationSearchFilter.class),
							style = ParameterStyle.FORM, explode = Explode.TRUE))
	// @formatter:on
	@ResponseBody
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	public Result<FilterResults<LocationMatch, Long>> findLocations(SourceLocationFilter criteria) {
		if ( criteria == null ) {
			return error("422", "Search filter is required.");
		}
		// convert empty strings to null
		criteria.removeEmptyValues();

		FilterResults<LocationMatch, Long> results = queryBiz.findFilteredLocations(
				criteria.getLocation(), criteria.getSortDescriptors(), criteria.getOffset(),
				criteria.getMax());
		return success(results);
	}

}
