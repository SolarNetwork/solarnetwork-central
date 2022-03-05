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
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.util.PathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.query.domain.ReportableInterval;
import net.solarnetwork.central.query.web.domain.GeneralReportableIntervalCommand;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.web.domain.Response;

/**
 * Controller for location-based data.
 * 
 * @author matt
 * @version 2.1
 */
@Controller("v1LocationDatumController")
@RequestMapping({ "/api/v1/sec/location/datum", "/api/v1/pub/location/datum" })
@GlobalExceptionRestController
public class LocationDatumController {

	/** The {@code transientExceptionRetryCount} property default value. */
	public static final int DEFAULT_TRANSIENT_EXCEPTION_RETRY_COUNT = 1;

	/** The {@code transientExceptionRetryDelay} property default value. */
	public static final long DEFAULT_TRANSIENT_EXCEPTION_RETRY_DELAY = 2000L;

	private static final Logger log = LoggerFactory.getLogger(LocationDatumController.class);

	private final QueryBiz queryBiz;
	private final PathMatcher pathMatcher;
	private int transientExceptionRetryCount = DEFAULT_TRANSIENT_EXCEPTION_RETRY_COUNT;
	private long transientExceptionRetryDelay = DEFAULT_TRANSIENT_EXCEPTION_RETRY_DELAY;

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
	 * @param cmd
	 *        the input command
	 * @return the available sources
	 */
	@ResponseBody
	@RequestMapping(value = "/sources", method = RequestMethod.GET)
	public Response<Set<String>> getAvailableSources(GeneralReportableIntervalCommand cmd) {
		int retries = transientExceptionRetryCount;
		while ( true ) {
			try {
				Set<String> data = queryBiz.getLocationAvailableSources(cmd.getLocationId(),
						cmd.getStartDate(), cmd.getEndDate());

				// support filtering based on sourceId path pattern
				data = filterSources(data, this.pathMatcher, cmd.getSourceId());

				return new Response<Set<String>>(data);
			} catch ( TransientDataAccessException | DataAccessResourceFailureException e ) {
				if ( retries > 0 ) {
					log.warn(
							"Transient {} exception in /sources request {}, will retry up to {} more times after a delay of {}ms: {}",
							e.getClass().getSimpleName(), cmd, retries, transientExceptionRetryDelay,
							e.toString());
					if ( transientExceptionRetryDelay > 0 ) {
						try {
							Thread.sleep(transientExceptionRetryDelay);
						} catch ( InterruptedException e2 ) {
							// ignore
						}
					}
				} else {
					throw e;
				}
			}
			retries--;
		}
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
		int retries = transientExceptionRetryCount;
		while ( true ) {
			try {
				ReportableInterval data = queryBiz.getLocationReportableInterval(cmd.getLocationId(),
						cmd.getSourceId());
				return new Response<ReportableInterval>(data);
			} catch ( TransientDataAccessException | DataAccessResourceFailureException e ) {
				if ( retries > 0 ) {
					log.warn(
							"Transient {} exception in /interval request {}, will retry up to {} more times after a delay of {}ms: {}",
							e.getClass().getSimpleName(), cmd, retries, transientExceptionRetryDelay,
							e.toString());
					if ( transientExceptionRetryDelay > 0 ) {
						try {
							Thread.sleep(transientExceptionRetryDelay);
						} catch ( InterruptedException e2 ) {
							// ignore
						}
					}
				} else {
					throw e;
				}
			}
			retries--;
		}
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

	@ResponseBody
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public Response<FilterResults<?>> filterGeneralDatumData(final DatumFilterCommand cmd) {
		int retries = transientExceptionRetryCount;
		while ( true ) {
			try {
				// support filtering based on sourceId path pattern, by simply finding the sources that match first
				resolveSourceIdPattern(cmd);

				FilterResults<?> results;
				if ( cmd.getAggregation() != null ) {
					results = queryBiz.findAggregateGeneralLocationDatum(cmd, cmd.getSortDescriptors(),
							cmd.getOffset(), cmd.getMax());
				} else {
					results = queryBiz.findGeneralLocationDatum(cmd, cmd.getSortDescriptors(),
							cmd.getOffset(), cmd.getMax());
				}
				return new Response<FilterResults<?>>(results);
			} catch ( TransientDataAccessException | DataAccessResourceFailureException e ) {
				if ( retries > 0 ) {
					log.warn(
							"Transient {} exception in /list request {}, will retry up to {} more times after a delay of {}ms: {}",
							e.getClass().getSimpleName(), cmd, retries, transientExceptionRetryDelay,
							e.toString());
					if ( transientExceptionRetryDelay > 0 ) {
						try {
							Thread.sleep(transientExceptionRetryDelay);
						} catch ( InterruptedException e2 ) {
							// ignore
						}
					}
				} else {
					throw e;
				}
			}
			retries--;
		}
	}

	@ResponseBody
	@RequestMapping(value = "/mostRecent", method = RequestMethod.GET)
	public Response<FilterResults<?>> getMostRecentGeneralNodeDatumData(final DatumFilterCommand cmd) {
		cmd.setMostRecent(true);
		return filterGeneralDatumData(cmd);
	}

	/**
	 * Get the number of retry attempts for transient DAO exceptions.
	 * 
	 * @return the retry count; defaults to
	 *         {@link #DEFAULT_TRANSIENT_EXCEPTION_RETRY_COUNT}.
	 * @since 1.3
	 */
	public int getTransientExceptionRetryCount() {
		return transientExceptionRetryCount;
	}

	/**
	 * Set the number of retry attempts for transient DAO exceptions.
	 * 
	 * @param transientExceptionRetryCount
	 *        the retry count, or {@literal 0} for no retries
	 * @since 1.3
	 */
	public void setTransientExceptionRetryCount(int transientExceptionRetryCount) {
		this.transientExceptionRetryCount = transientExceptionRetryCount;
	}

	/**
	 * Get the length of time, in milliseconds, to sleep before retrying a
	 * request after a transient exception.
	 * 
	 * @return the delay, in milliseconds; defaults to
	 *         {@link #DEFAULT_TRANSIENT_EXCEPTION_RETRY_DELAY}
	 * @since 2.1
	 */
	public long getTransientExceptionRetryDelay() {
		return transientExceptionRetryDelay;
	}

	/**
	 * Set the length of time, in milliseconds, to sleep before retrying a
	 * request after a transient exception.
	 * 
	 * @param transientExceptionRetryDelay
	 *        the delay to set
	 * @since 2.1
	 */
	public void setTransientExceptionRetryDelay(long transientExceptionRetryDelay) {
		this.transientExceptionRetryDelay = transientExceptionRetryDelay;
	}

}
