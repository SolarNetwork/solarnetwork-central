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

import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import net.solarnetwork.central.datum.domain.Datum;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumQueryCommand;
import net.solarnetwork.central.datum.domain.NodeDatum;
import net.solarnetwork.central.domain.EntityMatch;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.web.domain.Response;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import net.solarnetwork.util.JodaDateFormatEditor;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller for querying datum related data.
 * 
 * @author matt
 * @version 1.0
 */
@Controller("v1DatumController")
@RequestMapping({ "/api/v1/sec/datum", "/api/v1/pub/datum" })
public class DatumController extends WebServiceControllerSupport {

	private final QueryBiz queryBiz;

	private Map<String, Class<? extends NodeDatum>> typeMap;
	private Map<String, Class<? extends Datum>> filterTypeMap;

	private String[] requestDateFormats = new String[] { DEFAULT_DATE_TIME_FORMAT, DEFAULT_DATE_FORMAT };

	/**
	 * Constructor.
	 * 
	 * @param queryBiz
	 *        the QueryBiz to use
	 */
	@Autowired
	public DatumController(QueryBiz queryBiz) {
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

	@ResponseBody
	@RequestMapping(value = "/query", method = RequestMethod.GET)
	public Response<List<? extends NodeDatum>> getDatumData(final DatumQueryCommand cmd) {
		final String datumType = (cmd == null || cmd.getDatumType() == null ? null : cmd.getDatumType()
				.toLowerCase());
		final Class<? extends NodeDatum> datumClass = typeMap.get(datumType);
		if ( datumClass == null ) {
			log.info("Datum type {} not found in {}", datumType, typeMap);
			return new Response<List<? extends NodeDatum>>(false, "unsupported.type",
					"Unsupported datum type", null);
		}
		final List<? extends NodeDatum> results = queryBiz.getAggregatedDatum(datumClass, cmd);
		return new Response<List<? extends NodeDatum>>(results);
	}

	@ResponseBody
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public Response<FilterResults<?>> filterDatumData(final DatumFilterCommand cmd) {
		final String datumType = (cmd == null || cmd.getType() == null ? null : cmd.getType()
				.toLowerCase());
		final Class<? extends Datum> datumClass = filterTypeMap.get(datumType);
		if ( datumClass == null ) {
			log.info("Datum type {} not found in {}", datumType, filterTypeMap);
			return new Response<FilterResults<?>>(false, "unsupported.type", "Unsupported datum type",
					null);
		}
		FilterResults<? extends EntityMatch> results = queryBiz.findFilteredDatum(datumClass, cmd,
				cmd.getSortDescriptors(), cmd.getOffset(), cmd.getMax());
		return new Response<FilterResults<?>>(results);
	}

	@ResponseBody
	@RequestMapping(value = "/mostRecent", method = RequestMethod.GET)
	public Response<List<? extends NodeDatum>> getMostRecentDatumData(final DatumQueryCommand cmd) {
		cmd.setMostRecent(true);
		return getDatumData(cmd);
	}

	// this awful mess is because in OpenJDK on BSD, Spring could not wire up the util:map properly!
	// no amount of tinkering would work, other than this nastiness
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Autowired(required = true)
	@Qualifier("datumControllerTypeMap")
	public void setTypeMap(Object typeMap) {
		this.typeMap = (Map) typeMap;
	}

	// this awful mess is because in OpenJDK on BSD, Spring could not wire up the util:map properly!
	// no amount of tinkering would work, other than this nastiness
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Autowired(required = true)
	@Qualifier("datumControllerFilterTypeMap")
	public void setFilterTypeMap(Object typeMap) {
		this.filterTypeMap = (Map) typeMap;
	}

	public void setRequestDateFormats(String[] requestDateFormats) {
		this.requestDateFormats = requestDateFormats;
	}

}
