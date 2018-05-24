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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TimeZone;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.util.PathMatcher;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.query.domain.ReportableInterval;
import net.solarnetwork.central.query.web.domain.GeneralReportableIntervalCommand;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import net.solarnetwork.util.JodaDateFormatEditor;
import net.solarnetwork.util.JodaDateFormatEditor.ParseMode;
import net.solarnetwork.web.domain.Response;

/**
 * Controller for querying for reportable interval values.
 * 
 * <p>
 * See the {@link ReportableInterval} class for information about what is
 * returned to the view.
 * </p>
 * 
 * @author matt
 * @version 2.3
 */
@Controller("v1ReportableIntervalController")
@RequestMapping({ "/api/v1/sec/range", "/api/v1/pub/range" })
public class ReportableIntervalController extends WebServiceControllerSupport {

	private final QueryBiz queryBiz;
	private final DatumMetadataBiz datumMetadataBiz;
	private final PathMatcher pathMatcher;

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
	 * Web binder initialization.
	 * 
	 * <p>
	 * Registers a {@link LocalDate} property editor using the
	 * {@link #DEFAULT_DATE_FORMAT} pattern.
	 * </p>
	 * 
	 * @param binder
	 *        the binder to initialize
	 */
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(LocalDate.class,
				new JodaDateFormatEditor(DEFAULT_DATE_FORMAT, ParseMode.LocalDate));
		binder.registerCustomEditor(DateTime.class,
				new JodaDateFormatEditor(new String[] { DEFAULT_DATE_TIME_FORMAT, DEFAULT_DATE_FORMAT },
						TimeZone.getTimeZone("UTC")));
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
		ReportableInterval data = queryBiz.getReportableInterval(cmd.getNodeId(), cmd.getSourceId());
		return new Response<ReportableInterval>(data);
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
			"!metadataFilter" })
	public Response<Set<String>> getAvailableSources(GeneralReportableIntervalCommand cmd) {
		DatumFilterCommand f = new DatumFilterCommand();
		f.setNodeIds(cmd.getNodeIds());
		f.setStartDate(cmd.getStartDate());
		f.setEndDate(cmd.getEndDate());
		Set<String> data = queryBiz.getAvailableSources(f);

		// support filtering based on sourceId path pattern
		data = filterSources(data, this.pathMatcher, cmd.getSourceId());

		return new Response<Set<String>>(data);
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
	 * <code>/api/v1/sec/range/sources?nodeIds=1,2&metadataFilter=(/m/foo=bar)</code>
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
	 * @return
	 */
	@ResponseBody
	@RequestMapping(value = "/sources", method = RequestMethod.GET, params = { "!types",
			"metadataFilter" })
	public Response<Set<?>> getMetadataFilteredAvailableSources(GeneralReportableIntervalCommand cmd) {
		Set<NodeSourcePK> data = datumMetadataBiz
				.getGeneralNodeDatumMetadataFilteredSources(cmd.getNodeIds(), cmd.getMetadataFilter());

		// support filtering based on sourceId path pattern
		data = filterNodeSources(data, this.pathMatcher, cmd.getSourceId());

		if ( cmd.getNodeIds() != null && cmd.getNodeIds().length < 2 ) {
			// at most 1 node ID, so simplify results to just source ID values
			Set<String> sourceIds = new LinkedHashSet<String>(data.size());
			for ( NodeSourcePK pk : data ) {
				sourceIds.add(pk.getSourceId());
			}
			return new Response<Set<?>>(sourceIds);
		}
		return new Response<Set<?>>(data);
	}

	/**
	 * Filter a set of node sources using a source ID path pattern.
	 * 
	 * <p>
	 * If any arguments are {@literal null}, or {@code pathMatcher} is not a
	 * path pattern, then {@code sources} will be returned without filtering.
	 * </p>
	 * 
	 * @param sources
	 *        the sources to filter
	 * @param pathMatcher
	 *        the path matcher to use
	 * @param pattern
	 *        the pattern to test
	 * @return the filtered sources
	 */
	public static Set<NodeSourcePK> filterNodeSources(Set<NodeSourcePK> sources, PathMatcher pathMatcher,
			String pattern) {
		if ( sources == null || sources.isEmpty() || pathMatcher == null || pattern == null
				|| !pathMatcher.isPattern(pattern) ) {
			return sources;
		}
		for ( Iterator<NodeSourcePK> itr = sources.iterator(); itr.hasNext(); ) {
			NodeSourcePK pk = itr.next();
			if ( !pathMatcher.match(pattern, pk.getSourceId()) ) {
				itr.remove();
			}
		}
		return sources;
	}

	/**
	 * Filter a set of sources using a source ID path pattern.
	 * 
	 * <p>
	 * If any arguments are {@literal null}, or {@code pathMatcher} is not a
	 * path pattern, then {@code sources} will be returned without filtering.
	 * </p>
	 * 
	 * @param sources
	 *        the sources to filter
	 * @param pathMatcher
	 *        the path matcher to use
	 * @param pattern
	 *        the pattern to test
	 * @return the filtered sources
	 */
	public static Set<String> filterSources(Set<String> sources, PathMatcher pathMatcher,
			String pattern) {
		if ( sources == null || sources.isEmpty() || pathMatcher == null || pattern == null
				|| !pathMatcher.isPattern(pattern) ) {
			return sources;
		}
		for ( Iterator<String> itr = sources.iterator(); itr.hasNext(); ) {
			String source = itr.next();
			if ( !pathMatcher.match(pattern, source) ) {
				itr.remove();
			}
		}
		return sources;
	}

}
