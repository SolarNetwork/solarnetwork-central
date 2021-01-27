/* ==================================================================
 * DatumMaintenanceController.java - 10/04/2019 11:54:16 am
 * 
 * Copyright 2019 SolarNetwork.net Dev Team
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

import static net.solarnetwork.central.datum.support.DatumUtils.filterSources;
import static net.solarnetwork.web.domain.Response.response;
import java.util.Set;
import java.util.TimeZone;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.PathMatcher;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.datum.biz.DatumMaintenanceBiz;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.StaleAggregateDatum;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import net.solarnetwork.util.JodaDateFormatEditor;
import net.solarnetwork.web.domain.Response;

/**
 * Web controller for datum maintenance functions.
 * 
 * @author matt
 * @version 1.1
 * @since 1.39
 */
@RestController("v1DatumMaintenanceController")
@RequestMapping(value = { "/sec/datum/maint", "/v1/sec/datum/maint" })
public class DatumMaintenanceController extends WebServiceControllerSupport {

	private final DatumMaintenanceBiz datumMaintenanceBiz;
	private final QueryBiz queryBiz;
	private final PathMatcher pathMatcher;

	private final String[] requestDateFormats = new String[] { DEFAULT_TIMESTAMP_FORMAT,
			DEFAULT_TIMESTAMP_FORMAT_Z, ALT_TIMESTAMP_FORMAT, ALT_TIMESTAMP_FORMAT_Z,
			DEFAULT_DATE_FORMAT, DEFAULT_DATE_TIME_FORMAT, DEFAULT_DATE_TIME_FORMAT_Z,
			ALT_DATE_TIME_FORMAT, ALT_DATE_TIME_FORMAT_Z };

	/**
	 * Constructor.
	 * 
	 * @param datumMaintenanceBiz
	 *        the biz to use
	 * @param queryBiz
	 *        the query biz to use
	 * @param pathMatcher
	 *        the source ID path matcher to use
	 */
	@Autowired
	public DatumMaintenanceController(DatumMaintenanceBiz datumMaintenanceBiz, QueryBiz queryBiz,
			@Qualifier("sourceIdPathMatcher") PathMatcher pathMatcher) {
		super();
		this.datumMaintenanceBiz = datumMaintenanceBiz;
		this.queryBiz = queryBiz;
		this.pathMatcher = pathMatcher;
	}

	/**
	 * Web binder initialization.
	 * 
	 * @param binder
	 *        the binder to initialize
	 */
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(DateTime.class,
				new JodaDateFormatEditor(this.requestDateFormats, TimeZone.getTimeZone("UTC")));
	}

	private void resolveSourceIdPattern(DatumFilterCommand cmd) {
		if ( cmd == null || pathMatcher == null || queryBiz == null ) {
			return;
		}
		String sourceId = cmd.getSourceId();
		if ( sourceId != null && pathMatcher.isPattern(sourceId) && cmd.getNodeIds() != null ) {
			Set<String> allSources = queryBiz.getAvailableSources(cmd);
			allSources = filterSources(allSources, pathMatcher, sourceId);
			if ( !allSources.isEmpty() ) {
				cmd.setSourceIds(allSources.toArray(new String[allSources.size()]));
			}
		}
	}

	/**
	 * Find datum aggregates maarked as "stale".
	 * 
	 * <p>
	 * The following criteria should be specified at a minimum:
	 * </p>
	 * 
	 * <ul>
	 * <li>node ID(s)</li>
	 * <li>source ID(s)</li>
	 * <li>start date (inclusive)</li>
	 * <li>end date (exclusive)</li>
	 * </ul>
	 * 
	 * @param criteria
	 *        the datum criteria to mark
	 * @return empty response
	 * @since 1.1
	 */
	@ResponseBody
	@RequestMapping(value = "/agg/stale", method = RequestMethod.GET)
	public Response<FilterResults<StaleAggregateDatum>> findStaleAggregatesDatum(
			DatumFilterCommand criteria) {
		// support filtering based on sourceId path pattern, by simply finding the sources that match first
		resolveSourceIdPattern(criteria);

		FilterResults<StaleAggregateDatum> results = datumMaintenanceBiz.findStaleAggregateDatum(
				criteria, criteria.getSortDescriptors(), criteria.getOffset(), criteria.getMax());
		return response(results);
	}

	/**
	 * Mark a set of datum aggregates as "stale" so they are re-computed.
	 * 
	 * <p>
	 * The following criteria should be specified at a minimum:
	 * </p>
	 * 
	 * <ul>
	 * <li>node ID(s)</li>
	 * <li>source ID(s)</li>
	 * <li>start date (inclusive)</li>
	 * <li>end date (exclusive)</li>
	 * </ul>
	 * 
	 * @param criteria
	 *        the datum criteria to mark
	 * @return empty response
	 */
	@ResponseBody
	@RequestMapping(value = { "/agg/stale", "/agg/mark-stale" }, method = RequestMethod.POST)
	public Response<Void> markDatumAggregatesStale(DatumFilterCommand criteria) {
		// support filtering based on sourceId path pattern, by simply finding the sources that match first
		resolveSourceIdPattern(criteria);

		datumMaintenanceBiz.markDatumAggregatesStale(criteria);
		return response(null);
	}
}
