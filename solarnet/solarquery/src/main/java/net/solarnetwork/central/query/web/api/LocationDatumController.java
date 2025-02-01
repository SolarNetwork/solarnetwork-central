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

import static net.solarnetwork.central.datum.support.DatumUtils.filterSources;
import static net.solarnetwork.domain.Result.success;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.util.PathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import net.solarnetwork.central.datum.domain.AggregateGeneralLocationDatumFilter;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralLocationDatumPK;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.query.domain.ReportableInterval;
import net.solarnetwork.central.query.web.domain.GeneralReportableIntervalCommand;
import net.solarnetwork.central.web.BaseTransientDataAccessRetryController;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.central.web.WebUtils;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.Result;

/**
 * Controller for location-based data.
 * 
 * @author matt
 * @version 2.2
 */
@Controller("v1LocationDatumController")
@RequestMapping({ "/api/v1/sec/location/datum", "/api/v1/pub/location/datum" })
@Tag(name = "location-datum", description = "Methods to query location datum streams.")
@GlobalExceptionRestController
public class LocationDatumController extends BaseTransientDataAccessRetryController {

	private final QueryBiz queryBiz;
	private final PathMatcher pathMatcher;

	/**
	 * Constructor.
	 * 
	 * @param queryBiz
	 *        the QueryBiz to use
	 */
	public LocationDatumController(QueryBiz queryBiz) {
		this(queryBiz, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param queryBiz
	 *        the QueryBiz to use
	 * @param pathMatcher
	 *        the source ID path matcher to use
	 * @since 1.1
	 */
	@Autowired
	public LocationDatumController(QueryBiz queryBiz,
			@Qualifier("sourceIdPathMatcher") PathMatcher pathMatcher) {
		super();
		this.queryBiz = queryBiz;
		this.pathMatcher = pathMatcher;
	}

	/**
	 * Get the set of source IDs available for the available
	 * GeneralLocationDatum for a single location, optionally constrained within
	 * a date range.
	 * 
	 * <p>
	 * A <code>sourceId</code> path pattern may also be provided, to restrict
	 * the resulting source ID set to.
	 * </p>
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
	 * @param criteria
	 *        the input command
	 * @return the available sources
	 */
	@Operation(operationId = "locationDatumSources", summary = "List location datum available sources",
			description = "Get the source IDs available for location datum matching a single location ID and an optional source ID.")
	@ResponseBody
	@RequestMapping(value = "/sources", method = RequestMethod.GET)
	public Result<Set<String>> getAvailableSources(final HttpServletRequest req,
			final GeneralReportableIntervalCommand criteria) {
		return WebUtils.doWithTransientDataAccessExceptionRetry(() -> {
			Set<String> data = queryBiz.getLocationAvailableSources(criteria.getLocationId(),
					criteria.getStartDate(), criteria.getEndDate());

			// support filtering based on sourceId path pattern
			data = filterSources(data, this.pathMatcher, criteria.getSourceId());

			return success(data);
		}, req, getTransientExceptionRetryCount(), getTransientExceptionRetryDelay(), log);
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
	 * @param req
	 *        the HTTP request
	 * @param locationId
	 *        the location ID
	 * @param sourceId
	 *        the optional source ID
	 * @return the {@link ReportableInterval}
	 */
	@Operation(operationId = "locationDatumReportableInterval",
			summary = "List location datum available date range",
			description = "Get a date range of available location datum for a single location and an optional source ID.")
	@ResponseBody
	@RequestMapping(value = "/interval", method = RequestMethod.GET)
	public Result<ReportableInterval> getReportableInterval(final HttpServletRequest req,
			@RequestParam(name = "locationId") Long locationId,
			@RequestParam(name = "sourceId", required = false) String sourceId) {
		return WebUtils.doWithTransientDataAccessExceptionRetry(() -> {
			ReportableInterval data = queryBiz.getLocationReportableInterval(locationId, sourceId);
			return success(data);
		}, req, getTransientExceptionRetryCount(), getTransientExceptionRetryDelay(), log);
	}

	private void resolveSourceIdPattern(DatumFilterCommand cmd) {
		if ( cmd == null || pathMatcher == null || queryBiz == null ) {
			return;
		}
		String sourceId = cmd.getSourceId();
		if ( sourceId != null && pathMatcher.isPattern(sourceId) && cmd.getLocationIds() != null ) {
			Set<String> allSources = new LinkedHashSet<String>();
			for ( Long locationId : cmd.getLocationIds() ) {
				Set<String> data = queryBiz.getLocationAvailableSources(locationId, cmd.getStartDate(),
						cmd.getEndDate());
				if ( data != null ) {
					allSources.addAll(data);
				}
			}
			allSources = filterSources(allSources, pathMatcher, sourceId);
			if ( !allSources.isEmpty() ) {
				cmd.setSourceIds(allSources.toArray(new String[allSources.size()]));
			}
		}
	}

	@Operation(operationId = "locationDatumList",
			summary = "List location datum matching filter criteria",
			description = "Query for location datum that match criteria like location ID, source ID, and date range.",
			parameters = @Parameter(name = "criteria", description = """
					The search criteria. A maximum result count will be enforced.
					""", schema = @Schema(implementation = AggregateGeneralLocationDatumFilter.class),
					style = ParameterStyle.FORM, explode = Explode.TRUE))
	@ResponseBody
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public Result<FilterResults<? extends GeneralLocationDatumFilterMatch, GeneralLocationDatumPK>> filterGeneralDatumData(
			final HttpServletRequest req, final DatumFilterCommand criteria) {
		return WebUtils.doWithTransientDataAccessExceptionRetry(() -> {
			// support filtering based on sourceId path pattern, by simply finding the sources that match first
			resolveSourceIdPattern(criteria);

			FilterResults<? extends GeneralLocationDatumFilterMatch, GeneralLocationDatumPK> results;
			if ( criteria.getAggregation() != null ) {
				results = queryBiz.findAggregateGeneralLocationDatum(criteria,
						criteria.getSortDescriptors(), criteria.getOffset(), criteria.getMax());
			} else {
				results = queryBiz.findGeneralLocationDatum(criteria, criteria.getSortDescriptors(),
						criteria.getOffset(), criteria.getMax());
			}
			return success(results);
		}, req, getTransientExceptionRetryCount(), getTransientExceptionRetryDelay(), log);
	}

	@Operation(operationId = "locationDatumListMostRecent",
			summary = "List the most recently posted location datum matching filter criteria",
			description = """
					Query for location datum that match criteria like location ID, source ID, and date range.
					The most recently posted datum for each matching source will be returned.
					""",
			parameters = @Parameter(name = "criteria", description = """
					The search criteria. A maximum result count will be enforced.
					""", schema = @Schema(implementation = AggregateGeneralLocationDatumFilter.class),
					style = ParameterStyle.FORM, explode = Explode.TRUE))
	@ResponseBody
	@RequestMapping(value = "/mostRecent", method = RequestMethod.GET)
	public Result<FilterResults<? extends GeneralLocationDatumFilterMatch, GeneralLocationDatumPK>> getMostRecentGeneralNodeDatumData(
			final HttpServletRequest req, final DatumFilterCommand criteria) {
		criteria.setMostRecent(true);
		return filterGeneralDatumData(req, criteria);
	}

}
