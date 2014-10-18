/* ==================================================================
 * LocationDatumController.java - Oct 18, 2014 3:47:51 PM
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

import java.util.Set;
import java.util.TimeZone;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.query.domain.ReportableInterval;
import net.solarnetwork.central.query.web.domain.GeneralReportableIntervalCommand;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import net.solarnetwork.util.JodaDateFormatEditor;
import net.solarnetwork.web.domain.Response;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller for location-based data.
 * 
 * @author matt
 * @version 1.0
 */
@Controller("v1LocationDatumController")
@RequestMapping({ "/api/v1/sec/location/datum", "/api/v1/pub/location/datum" })
public class LocationDatumController extends WebServiceControllerSupport {

	private final QueryBiz queryBiz;
	private String[] requestDateFormats = new String[] { DEFAULT_DATE_TIME_FORMAT, DEFAULT_DATE_FORMAT };

	/**
	 * Constructor.
	 * 
	 * @param queryBiz
	 *        the QueryBiz
	 */
	@Autowired
	public LocationDatumController(QueryBiz queryBiz) {
		super();
		this.queryBiz = queryBiz;
	}

	/**
	 * Web binder initialization.
	 * 
	 * @param binder
	 *        the binder to initialize
	 */
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(DateTime.class, new JodaDateFormatEditor(this.requestDateFormats,
				TimeZone.getTimeZone("UTC")));
	}

	/**
	 * Get the set of source IDs available for the available
	 * GeneralLocationDatum for a single location, optionally constrained within
	 * a date range.
	 * 
	 * <p>
	 * Example URL: <code>/api/v1/sec/location/datum/sources?locationId=1</code>
	 * </p>
	 * 
	 * <p>
	 * Example JSON response:
	 * </p>
	 * 
	 * <pre>
	 * {
	 *   "success": true,
	 *   "data": [
	 *     "Main"
	 *   ]
	 * }
	 * </pre>
	 * 
	 * @param cmd
	 *        the input command
	 * @return the available sources
	 */
	@ResponseBody
	@RequestMapping(value = "/sources", method = RequestMethod.GET)
	public Response<Set<String>> getAvailableSources(GeneralReportableIntervalCommand cmd) {
		Set<String> data = queryBiz.getLocationAvailableSources(cmd.getLocationId(), cmd.getStart(),
				cmd.getEnd());
		return new Response<Set<String>>(data);
	}

	/**
	 * Get a date range of available GeneralLocationDatum for a single location
	 * and an optional source ID.
	 * 
	 * <p>
	 * This method returns a start/end date range.
	 * </p>
	 * 
	 * <p>
	 * Example URL:
	 * <code>/api/v1/sec/location/datum/interval?locationId=1</code>
	 * </p>
	 * 
	 * <p>
	 * Example JSON response:
	 * </p>
	 * 
	 * <pre>
	 * {
	 *   "success": true,
	 *   "data": {
	 *     "timeZone": "Pacific/Auckland",
	 *     "endDate": "2012-12-11 01:49",
	 *     "startDate": "2012-12-11 00:30",
	 *     "dayCount": 1683,
	 *     "monthCount": 56,
	 *     "yearCount": 6
	 *   }
	 * }
	 * </pre>
	 * 
	 * @param cmd
	 *        the input command
	 * @return the {@link ReportableInterval}
	 */
	@ResponseBody
	@RequestMapping(value = "/interval", method = RequestMethod.GET)
	public Response<ReportableInterval> getReportableInterval(GeneralReportableIntervalCommand cmd) {
		ReportableInterval data = queryBiz.getLocationReportableInterval(cmd.getLocationId(),
				cmd.getSourceId());
		return new Response<ReportableInterval>(data);
	}

	@ResponseBody
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public Response<FilterResults<?>> filterGeneralDatumData(final DatumFilterCommand cmd) {
		FilterResults<?> results;
		if ( cmd.getAggregation() != null ) {
			results = queryBiz.findAggregateGeneralLocationDatum(cmd, cmd.getSortDescriptors(),
					cmd.getOffset(), cmd.getMax());
		} else {
			results = queryBiz.findGeneralLocationDatum(cmd, cmd.getSortDescriptors(), cmd.getOffset(),
					cmd.getMax());
		}
		return new Response<FilterResults<?>>(results);
	}

	@ResponseBody
	@RequestMapping(value = "/mostRecent", method = RequestMethod.GET)
	public Response<FilterResults<?>> getMostRecentGeneralNodeDatumData(final DatumFilterCommand cmd) {
		cmd.setMostRecent(true);
		return filterGeneralDatumData(cmd);
	}

	public void setRequestDateFormats(String[] requestDateFormats) {
		this.requestDateFormats = requestDateFormats;
	}

}
