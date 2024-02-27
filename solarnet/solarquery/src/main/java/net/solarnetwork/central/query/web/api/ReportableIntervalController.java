/* ==================================================================
 * ReportableIntervalController.java - Dec 18, 2012 9:19:43 AM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.support.DatumUtils;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.query.domain.ReportableInterval;
import net.solarnetwork.central.query.web.domain.GeneralReportableIntervalCommand;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.web.jakarta.domain.Response;

/**
 * Controller for querying for reportable interval values.
 * 
 * <p>
 * See the {@link ReportableInterval} class for information about what is
 * returned to the view.
 * </p>
 * 
 * @author matt
 * @version 3.1
 */
@Controller("v1ReportableIntervalController")
@RequestMapping({ "/api/v1/sec/range", "/api/v1/pub/range" })
@GlobalExceptionRestController
public class ReportableIntervalController {

	/** The {@code transientExceptionRetryCount} property default value. */
	public static final int DEFAULT_TRANSIENT_EXCEPTION_RETRY_COUNT = 1;

	/** The {@code transientExceptionRetryDelay} property default value. */
	public static final long DEFAULT_TRANSIENT_EXCEPTION_RETRY_DELAY = 2000L;

	private static final Logger log = LoggerFactory.getLogger(ReportableIntervalController.class);

	private final QueryBiz queryBiz;
	private final DatumMetadataBiz datumMetadataBiz;
	private final PathMatcher pathMatcher;
	private int transientExceptionRetryCount = DEFAULT_TRANSIENT_EXCEPTION_RETRY_COUNT;
	private long transientExceptionRetryDelay = DEFAULT_TRANSIENT_EXCEPTION_RETRY_DELAY;

	/**
	 * Constructor.
	 * 
	 * @param queryBiz
	 *        the QueryBiz to use
	 * @param datumMetadataBiz
	 *        the DatumMetadataBiz to use
	 */
	public ReportableIntervalController(QueryBiz queryBiz, DatumMetadataBiz datumMetadataBiz) {
		this(queryBiz, datumMetadataBiz, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param queryBiz
	 *        the QueryBiz to use
	 * @param datumMetadataBiz
	 *        the DatumMetadataBiz to use
	 * @param pathMatcher
	 *        the PathMatcher to use
	 * @since 2.2
	 */
	@Autowired
	public ReportableIntervalController(QueryBiz queryBiz, DatumMetadataBiz datumMetadataBiz,
			@Qualifier("sourceIdPathMatcher") PathMatcher pathMatcher) {
		super();
		this.queryBiz = queryBiz;
		this.datumMetadataBiz = datumMetadataBiz;
		this.pathMatcher = pathMatcher;
	}

	/**
	 * Get a date range of available GeneralNodeData for a node and an optional
	 * source ID.
	 * 
	 * <p>
	 * This method returns a start/end date range.
	 * </p>
	 * 
	 * <p>
	 * Example URL: <code>/api/v1/sec/range/interval?nodeId=1</code>
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
	@RequestMapping(value = "/interval", method = RequestMethod.GET, params = "!types")
	public Response<ReportableInterval> getReportableInterval(GeneralReportableIntervalCommand cmd) {
		int retries = transientExceptionRetryCount;
		while ( true ) {
			try {
				ReportableInterval data = queryBiz.getReportableInterval(cmd.getNodeId(),
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

	/**
	 * Get the set of source IDs available for the available GeneralNodeData for
	 * a single node, optionally constrained within a date range.
	 * 
	 * <p>
	 * A <code>sourceId</code> path pattern may also be provided, to restrict
	 * the resulting source ID set to.
	 * </p>
	 * 
	 * <p>
	 * Example URL: <code>/api/v1/sec/range/sources?nodeId=1</code>
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
	@RequestMapping(value = "/sources", method = RequestMethod.GET, params = { "!types",
			"!metadataFilter", "!withNodeIds" })
	public Response<Set<String>> getAvailableSources(GeneralReportableIntervalCommand cmd) {
		int retries = transientExceptionRetryCount;
		while ( true ) {
			try {
				DatumFilterCommand f = new DatumFilterCommand();
				f.setNodeIds(cmd.getNodeIds());
				f.setStartDate(cmd.getStartDate());
				f.setEndDate(cmd.getEndDate());
				Set<String> data = queryBiz.getAvailableSources(f);

				// support filtering based on sourceId path pattern
				data = DatumUtils.filterSources(data, this.pathMatcher, cmd.getSourceId());

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
	 * Get all available node+source ID pairs.
	 * 
	 * <p>
	 * A <code>sourceId</code> path pattern may also be provided, to restrict
	 * the resulting source ID set to. Also {@code startDate} and
	 * {@code endDate} values may be provided to restrict the query further.
	 * </p>
	 * 
	 * <p>
	 * Example URL:
	 * <code>/api/v1/sec/range/sources?nodeIds=1,2&amp;withNodeIds=true</code>
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
	 *     { nodeId: 1, sourceId: "A" },
	 *     { nodeId: 2, "sourceId: "B" }
	 *   ]
	 * }
	 * </pre>
	 * 
	 * <p>
	 * If {@code withNodeIds} is specifed as {@literal false} then the result
	 * will be simply a set of string source ID values.
	 * </p>
	 * 
	 * @param cmd
	 *        the criteria
	 * @return the found source IDs
	 */
	@ResponseBody
	@RequestMapping(value = "/sources", method = RequestMethod.GET, params = { "!types",
			"!metadataFilter", "withNodeIds" })
	public Response<Set<?>> findAvailableSources(GeneralReportableIntervalCommand cmd) {
		int retries = transientExceptionRetryCount;
		while ( true ) {
			try {
				DatumFilterCommand f = new DatumFilterCommand();
				f.setNodeIds(cmd.getNodeIds());
				f.setStartDate(cmd.getStartDate());
				f.setEndDate(cmd.getEndDate());
				Set<NodeSourcePK> data = queryBiz.findAvailableSources(f);

				// support filtering based on sourceId path pattern
				data = DatumUtils.filterNodeSources(data, this.pathMatcher, cmd.getSourceId());

				if ( !cmd.isWithNodeIds() ) {
					Set<String> sourceIds = new LinkedHashSet<String>(data.size());
					for ( NodeSourcePK pk : data ) {
						sourceIds.add(pk.getSourceId());
					}
					return new Response<Set<?>>(sourceIds);
				}

				return new Response<Set<?>>(data);
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
	 * Get all available node+source ID pairs that match a node datum metadata
	 * search filter.
	 * 
	 * <p>
	 * A <code>sourceId</code> path pattern may also be provided, to restrict
	 * the resulting source ID set to.
	 * </p>
	 * 
	 * <p>
	 * Example URL:
	 * <code>/api/v1/sec/range/sources?nodeIds=1,2&amp;metadataFilter=(/m/foo=bar)</code>
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
	 *     { nodeId: 1, sourceId: "A" },
	 *     { nodeId: 2, "sourceId: "B" }
	 *   ]
	 * }
	 * </pre>
	 * 
	 * <p>
	 * If only a single node ID is specified, then the results will be
	 * simplified to just an array of strings for the source IDs, omitting the
	 * node ID which is redundant.
	 * </p>
	 * 
	 * @param cmd
	 *        the criteria
	 * @return the sources
	 */
	@ResponseBody
	@RequestMapping(value = "/sources", method = RequestMethod.GET, params = { "!types",
			"metadataFilter" })
	public Response<Set<?>> getMetadataFilteredAvailableSources(GeneralReportableIntervalCommand cmd) {
		int retries = transientExceptionRetryCount;
		while ( true ) {
			try {
				Set<NodeSourcePK> data = datumMetadataBiz.getGeneralNodeDatumMetadataFilteredSources(
						cmd.getNodeIds(), cmd.getMetadataFilter());

				// support filtering based on sourceId path pattern
				data = DatumUtils.filterNodeSources(data, this.pathMatcher, cmd.getSourceId());

				if ( !cmd.isWithNodeIds() && cmd.getNodeIds() != null && cmd.getNodeIds().length < 2 ) {
					// at most 1 node ID, so simplify results to just source ID values
					Set<String> sourceIds = new LinkedHashSet<String>(data.size());
					for ( NodeSourcePK pk : data ) {
						sourceIds.add(pk.getSourceId());
					}
					return new Response<Set<?>>(sourceIds);
				}
				return new Response<Set<?>>(data);
			} catch ( TransientDataAccessException | DataAccessResourceFailureException e ) {
				if ( retries > 0 ) {
					log.warn("Transient {} exception, will retry up to {} more times.",
							e.getClass().getSimpleName(), retries, e);
				} else {
					throw e;
				}
			}
			retries--;
		}
	}

	/**
	 * Get the number of retry attempts for transient DAO exceptions.
	 * 
	 * @return the retry count; defaults to
	 *         {@link #DEFAULT_TRANSIENT_EXCEPTION_RETRY_COUNT}.
	 * @since 2.5
	 */
	public int getTransientExceptionRetryCount() {
		return transientExceptionRetryCount;
	}

	/**
	 * Set the number of retry attempts for transient DAO exceptions.
	 * 
	 * @param transientExceptionRetryCount
	 *        the retry count, or {@literal 0} for no retries
	 * @since 2.5
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
	 * @since 3.1
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
	 * @since 3.1
	 */
	public void setTransientExceptionRetryDelay(long transientExceptionRetryDelay) {
		this.transientExceptionRetryDelay = transientExceptionRetryDelay;
	}

}
