/* ==================================================================
 * DatumController.java - Mar 22, 2013 4:36:00 PM
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

import static net.solarnetwork.central.query.web.api.ReportableIntervalController.filterSources;
import java.util.Set;
import java.util.TimeZone;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.util.PathMatcher;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumReadingType;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import net.solarnetwork.util.JodaDateFormatEditor;
import net.solarnetwork.util.JodaDateFormatEditor.ParseMode;
import net.solarnetwork.web.domain.Response;

/**
 * Controller for querying datum related data.
 * 
 * @author matt
 * @version 2.4
 */
@Controller("v1DatumController")
@RequestMapping({ "/api/v1/sec/datum", "/api/v1/pub/datum" })
public class DatumController extends WebServiceControllerSupport {

	private final QueryBiz queryBiz;
	private final PathMatcher pathMatcher;

	private String[] requestDateFormats = new String[] { DEFAULT_DATE_TIME_FORMAT, DEFAULT_DATE_FORMAT };

	/**
	 * Constructor.
	 * 
	 * @param queryBiz
	 *        the QueryBiz to use
	 */
	public DatumController(QueryBiz queryBiz) {
		this(queryBiz, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param queryBiz
	 *        the QueryBiz to use
	 * @param pathMatcher
	 *        the source ID path matcher to use
	 * @since 2.1
	 */
	@Autowired
	public DatumController(QueryBiz queryBiz,
			@Qualifier("sourceIdPathMatcher") PathMatcher pathMatcher) {
		super();
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
		binder.registerCustomEditor(LocalDateTime.class,
				new JodaDateFormatEditor(this.requestDateFormats, null, ParseMode.LocalDateTime));
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

	@ResponseBody
	@RequestMapping(value = "/list", method = RequestMethod.GET, params = "!type")
	public Response<FilterResults<?>> filterGeneralDatumData(final DatumFilterCommand cmd) {

		// support filtering based on sourceId path pattern, by simply finding the sources that match first
		resolveSourceIdPattern(cmd);

		FilterResults<?> results;
		if ( cmd.getAggregation() != null ) {
			results = queryBiz.findFilteredAggregateGeneralNodeDatum(cmd, cmd.getSortDescriptors(),
					cmd.getOffset(), cmd.getMax());
		} else {
			results = queryBiz.findFilteredGeneralNodeDatum(cmd, cmd.getSortDescriptors(),
					cmd.getOffset(), cmd.getMax());
		}
		return new Response<FilterResults<?>>(results);
	}

	@ResponseBody
	@RequestMapping(value = "/mostRecent", method = RequestMethod.GET, params = "!type")
	public Response<FilterResults<?>> getMostRecentGeneralNodeDatumData(final DatumFilterCommand cmd) {
		cmd.setMostRecent(true);
		return filterGeneralDatumData(cmd);
	}

	/**
	 * Query for reading datum.
	 * 
	 * @param cmd
	 *        the filter
	 * @param readingType
	 *        the reading type
	 * @param tolerance
	 *        the query tolerance
	 * @return
	 * @since 2.3
	 */
	@ResponseBody
	@RequestMapping(value = "/reading", method = RequestMethod.GET)
	public Response<FilterResults<?>> datumReading(final DatumFilterCommand cmd,
			@RequestParam("readingType") DatumReadingType readingType,
			@RequestParam(value = "tolerance", required = false, defaultValue = "P1M") Period tolerance) {

		// support filtering based on sourceId path pattern, by simply finding the sources that match first
		resolveSourceIdPattern(cmd);

		FilterResults<?> results;
		if ( cmd.getAggregation() != null && cmd.getAggregation() != Aggregation.None ) {
			results = queryBiz.findFilteredAggregateReading(cmd, readingType, tolerance,
					cmd.getSortDescriptors(), cmd.getOffset(), cmd.getMax());
		} else {
			results = queryBiz.findFilteredReading(cmd, readingType, tolerance);
		}
		return new Response<FilterResults<?>>(results);
	}

	public void setRequestDateFormats(String[] requestDateFormats) {
		this.requestDateFormats = requestDateFormats;
	}

}
