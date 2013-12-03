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

import java.util.TimeZone;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.PriceLocation;
import net.solarnetwork.central.domain.SourceLocationMatch;
import net.solarnetwork.central.domain.WeatherLocation;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.support.PriceLocationFilter;
import net.solarnetwork.central.support.SourceLocationFilter;
import net.solarnetwork.central.web.domain.Response;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import net.solarnetwork.util.JodaDateFormatEditor;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller for querying location data.
 * 
 * @author matt
 * @version 1.0
 */
@Controller("v1LocationLookupController")
@RequestMapping({ "/api/v1/pub/location", "/api/v1/sec/location" })
public class LocationLookupController extends WebServiceControllerSupport {

	private final QueryBiz queryBiz;
	private String[] requestDateFormats = new String[] { DEFAULT_DATE_TIME_FORMAT, DEFAULT_DATE_FORMAT };

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
		binder.registerCustomEditor(DateTime.class, new JodaDateFormatEditor(this.requestDateFormats,
				TimeZone.getTimeZone("UTC")));
	}

	@ResponseBody
	@RequestMapping(value = "/price", method = RequestMethod.GET)
	public Response<FilterResults<SourceLocationMatch>> findPriceLocations(PriceLocationFilter cmd) {
		// convert empty strings to null
		cmd.removeEmptyValues();

		FilterResults<SourceLocationMatch> results = queryBiz.findFilteredLocations(PriceLocation.class,
				cmd, cmd.getSortDescriptors(), cmd.getOffset(), cmd.getMax());
		return new Response<FilterResults<SourceLocationMatch>>(results);
	}

	@ResponseBody
	@RequestMapping(value = "/weather", method = RequestMethod.GET)
	public Response<FilterResults<SourceLocationMatch>> findWeatherLocations(SourceLocationFilter cmd) {
		// convert empty strings to null
		cmd.removeEmptyValues();

		FilterResults<SourceLocationMatch> matches = queryBiz.findFilteredLocations(
				WeatherLocation.class, cmd, cmd.getSortDescriptors(), cmd.getOffset(), cmd.getMax());
		return new Response<FilterResults<SourceLocationMatch>>(matches);
	}

	public void setRequestDateFormats(String[] requestDateFormats) {
		this.requestDateFormats = requestDateFormats;
	}

}
