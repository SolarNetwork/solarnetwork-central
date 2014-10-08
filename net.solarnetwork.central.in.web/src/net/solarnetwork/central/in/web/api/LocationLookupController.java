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

import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SourceLocationMatch;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.in.web.GenericSourceLocationFilter.LocationType;
import net.solarnetwork.central.support.PriceLocationFilter;
import net.solarnetwork.central.support.SourceLocationFilter;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import net.solarnetwork.web.domain.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller for querying location data.
 * 
 * @author matt
 * @version 1.1
 */
@Controller("v1LocationLookupController")
@RequestMapping({ "/api/v1/pub/location", "/api/v1/sec/location" })
public class LocationLookupController extends WebServiceControllerSupport {

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

	@ResponseBody
	@RequestMapping(value = { "", "/", "/query" }, method = RequestMethod.GET)
	public Response<FilterResults<SourceLocationMatch>> findLocations(SourceLocationFilter criteria,
			@RequestParam("type") String locationType) {
		LocationType type = null;
		try {
			type = LocationType.valueOf(locationType);
		} catch ( IllegalArgumentException e ) {
			// ignore
		}

		if ( type == LocationType.Price ) {
			return findPriceLocations(new PriceLocationFilter(criteria));
		} else if ( type == LocationType.Weather ) {
			return findWeatherLocations(criteria);
		} else {
			BindException errors = new BindException(criteria, "criteria");
			errors.reject("error.field.invalid", new Object[] { "locationType" }, "Invalid value.");
			throw new ValidationException(errors);
		}
	}

	@ResponseBody
	@RequestMapping(value = "/price", method = RequestMethod.GET)
	public Response<FilterResults<SourceLocationMatch>> findPriceLocations(PriceLocationFilter criteria) {
		// convert empty strings to null
		criteria.removeEmptyValues();

		FilterResults<SourceLocationMatch> matches = dataCollectorBiz.findPriceLocations(criteria,
				criteria.getSortDescriptors(), criteria.getOffset(), criteria.getMax());
		return new Response<FilterResults<SourceLocationMatch>>(matches);
	}

	@ResponseBody
	@RequestMapping(value = "/weather", method = RequestMethod.GET)
	public Response<FilterResults<SourceLocationMatch>> findWeatherLocations(
			SourceLocationFilter criteria) {
		// convert empty strings to null
		criteria.removeEmptyValues();

		FilterResults<SourceLocationMatch> matches = dataCollectorBiz.findWeatherLocations(criteria,
				criteria.getSortDescriptors(), criteria.getOffset(), criteria.getMax());
		return new Response<FilterResults<SourceLocationMatch>>(matches);
	}

}
