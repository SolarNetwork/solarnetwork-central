/* ===================================================================
 * AggregatedDatumController.java
 * 
 * Created Aug 4, 2009 10:15:02 AM
 * 
 * Copyright (c) 2009 Solarnetwork.net Dev Team.
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
 * ===================================================================
 * $Id$
 * ===================================================================
 */

package net.solarnetwork.central.query.web;

import java.util.List;
import java.util.TimeZone;
import javax.servlet.http.HttpServletRequest;
import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.datum.domain.ConsumptionDatum;
import net.solarnetwork.central.datum.domain.DatumQueryCommand;
import net.solarnetwork.central.datum.domain.DayDatum;
import net.solarnetwork.central.datum.domain.HardwareControlDatum;
import net.solarnetwork.central.datum.domain.NodeDatum;
import net.solarnetwork.central.datum.domain.PowerDatum;
import net.solarnetwork.central.datum.domain.PriceDatum;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.web.AbstractNodeController;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for querying and viewing aggregated {@link NodeDatum} data.
 * 
 * <p>
 * The configurable properties of this class are:
 * </p>
 * 
 * <dl class="class-properties">
 * 
 * <dt>defaultTimeZone</dt>
 * <dd>The default time zone to use when no {@link SolarNode} ID is provided in
 * the command. For aggregated data across nodes, this is the time zone that
 * will be to create the start/end dates (if not provided in the command) and
 * also returned to the view on the {@link #MODEL_KEY_TZ} key. When the
 * {@link DatumQueryCommand#getNodeId()} is provided, the time zone for that
 * node will be used instead of this default value. Defaults to {@code GMT+12}.</dd>
 * </dl>
 *
 * @author matt
 * @version 1.1
 * @deprecated use the
 *             {@link net.solarnetwork.central.query.web.api.DatumController}
 *             API
 */
@Deprecated
@Controller
public class AggregatedDatumController extends AbstractNodeController {

	/** The model key for the {@code List&lt;NodeDatum&gt;} results. */
	public static final String MODEL_KEY_DATA_LIST = "data";

	/** The model key for the client {@link TimeZone} results. */
	public static final String MODEL_KEY_TZ = "tz";

	private TimeZone defaultTimeZone = TimeZone.getTimeZone("GMT+12");

	private final QueryBiz queryBiz;

	@Autowired
	public AggregatedDatumController(SolarNodeDao solarNodeDao, QueryBiz queryBiz) {
		super();
		setSolarNodeDao(solarNodeDao);
		this.queryBiz = queryBiz;
	}

	/**
	 * Query for consumption data.
	 * 
	 * @param cmd
	 *        the query criteria
	 * @param request
	 *        the servlet request
	 * @return model and view
	 */
	@RequestMapping(value = "/consumptionData.*", method = RequestMethod.GET)
	public ModelAndView getConsumptionData(DatumQueryCommand cmd, HttpServletRequest request) {
		return getData(cmd, request, ConsumptionDatum.class);
	}

	/**
	 * Query for power data.
	 * 
	 * @param cmd
	 *        the query criteria
	 * @param request
	 *        the servlet request
	 * @return model and view
	 */
	@RequestMapping(value = { "/generationData.*", "/powerData.*" }, method = RequestMethod.GET)
	private ModelAndView getPowerData(DatumQueryCommand cmd, HttpServletRequest request) {
		return getData(cmd, request, PowerDatum.class);
	}

	/**
	 * Query for hardware control data.
	 * 
	 * @param cmd
	 *        the query criteria
	 * @param request
	 *        the servlet request
	 * @return model and view
	 */
	@RequestMapping(value = "/hardwareControlData.*", method = RequestMethod.GET)
	private ModelAndView getHardwareControlData(DatumQueryCommand cmd, HttpServletRequest request) {
		return getData(cmd, request, HardwareControlDatum.class);
	}

	/**
	 * Query for price data.
	 * 
	 * @param cmd
	 *        the query criteria
	 * @param request
	 *        the servlet request
	 * @return model and view
	 */
	@RequestMapping(value = "/priceData.*", method = RequestMethod.GET)
	private ModelAndView getPriceData(DatumQueryCommand cmd, HttpServletRequest request) {
		return getData(cmd, request, PriceDatum.class);
	}

	/**
	 * Query for day data.
	 * 
	 * @param cmd
	 *        the query criteria
	 * @param request
	 *        the servlet request
	 * @return model and view
	 */
	@RequestMapping(value = "/dayData.*", method = RequestMethod.GET)
	private ModelAndView getDayData(DatumQueryCommand cmd, HttpServletRequest request) {
		return getData(cmd, request, DayDatum.class);
	}

	public ModelAndView getData(DatumQueryCommand cmd, HttpServletRequest request,
			Class<? extends NodeDatum> datumClass) {
		ModelAndView mv = resolveViewFromUrlExtension(request);

		TimeZone tz = this.defaultTimeZone;
		if ( cmd.getNodeId() != null ) {
			// get the SolarNode for the specified node, for the appropriate time zone
			SolarNode node = getSolarNodeDao().get(cmd.getNodeId());
			if ( node != null ) {
				tz = node.getTimeZone();
			}
		}

		// make sure required criteria values are set
		if ( cmd.getStartDate() == null ) {
			// default to 1 day ago
			cmd.setStartDate(new DateTime(DateTimeZone.forTimeZone(tz)).minusDays(1));
		}
		if ( cmd.getEndDate() == null ) {
			cmd.setEndDate(new DateTime(DateTimeZone.forTimeZone(tz)));
		}
		if ( cmd.getPrecision() == null ) {
			cmd.setPrecision(60);
		}

		// execute query, and return results
		List<? extends NodeDatum> results = queryBiz.getAggregatedDatum(cmd, datumClass);
		mv.addObject(MODEL_KEY_DATA_LIST, results);
		mv.addObject(MODEL_KEY_TZ, tz);

		return mv;
	}

	/**
	 * Web binder initialization.
	 * 
	 * @param binder
	 *        the binder to initialize
	 */
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		initBinderDateFormatEditor(binder);
	}

	public TimeZone getDefaultTimeZone() {
		return defaultTimeZone;
	}

	public void setDefaultTimeZone(TimeZone defaultTimeZone) {
		this.defaultTimeZone = defaultTimeZone;
	}

	public QueryBiz getQueryBiz() {
		return queryBiz;
	}

}
