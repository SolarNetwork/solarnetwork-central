/* ==================================================================
 * RawDatumController.java - Feb 11, 2012 2:56:07 PM
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.query.web;

import java.util.List;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import net.solarnetwork.central.ValidationException;
import net.solarnetwork.central.datum.domain.DatumQueryCommand;
import net.solarnetwork.central.datum.domain.NodeDatum;
import net.solarnetwork.central.datum.support.DatumUtils;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.web.AbstractNodeController;

/**
 * FIXME
 * 
 * <p>TODO</p>
 * 
 * <p>The configurable properties of this class are:</p>
 * 
 * <dl class="class-properties">
 *   <dt>defaultTimeZone</dt>
 *   <dd>The default TimeZone to return as the model object 
 *   {@link #MODEL_KEY_TZ}.</dd>
 *   
 *   <dt>queryBiz</dt>
 *   <dd>The {@link QueryBiz} to use for querying the data from.</dd>
 *   
 *   <dt>defaultDatumClass</dt>
 *   <dd>If no datum class is specified in the request, this class
 *   will be used.</dd>
 * </dl>
 * 
 * @author matt
 * @version $Revision$
 */
@Controller
public class RawDatumController extends AbstractNodeController {

	/** The model key for the {@code List&lt;NodeDatum&gt;} results. */
	public static final String MODEL_KEY_DATA_LIST = "data";
	
	/** The model key for the client {@link TimeZone} results. */
	public static final String MODEL_KEY_TZ = "tz";
	
	private TimeZone defaultTimeZone = TimeZone.getTimeZone("GMT+12");
	private Class<? extends NodeDatum> defaultDatumClass;
	@Autowired private QueryBiz queryBiz;
	
	/**
	 * Query for raw data.
	 * 
	 * @param cmd the query criteria
	 * @param request the servlet request
	 * @return model and view
	 */
	@RequestMapping(value = "/rawData.*", method = RequestMethod.GET)
	public ModelAndView getData(DatumQueryCommand cmd, HttpServletRequest request) {
		ModelAndView mv = resolveViewFromUrlExtension(request);

		TimeZone tz = this.defaultTimeZone;
		if ( cmd.getNodeId() != null ) {
			// get the SolarNode for the specified node, for the appropriate time zone
			SolarNode node = getSolarNodeDao().get(cmd.getNodeId());
			if ( node != null ) {
				tz = node.getTimeZone();
			}
		}
		
		Class<? extends NodeDatum> datumClass = null;
		if ( cmd.getDatumType() != null ) {
			datumClass = DatumUtils.nodeDatumClassForName(cmd.getDatumType());
		}
		if ( datumClass == null ) {
			datumClass = this.defaultDatumClass;
		}
		if ( datumClass == null ) {
			Errors errors = new BindException(cmd, "command");
			errors.rejectValue("datumClass", "DatumQueryCommand.datumClass.invalid", 
					"The specified datum class is invalid.");
			throw new ValidationException(errors);
		}
		
		// make sure required criteria values are set
		if ( cmd.getStartDate() == null ) {
			// default to 1 day ago
			cmd.setStartDate(new DateTime(DateTimeZone.forTimeZone(tz)).minusDays(1));
		}
		if ( cmd.getEndDate() == null ) {
			cmd.setEndDate(new DateTime(DateTimeZone.forTimeZone(tz)));
		}
		
		// execute query, and return results
		List<? extends NodeDatum> results = queryBiz.getAggregatedDatum(datumClass, cmd);
		mv.addObject(MODEL_KEY_DATA_LIST, results);
        mv.addObject(MODEL_KEY_TZ, tz);
        
		return mv;
	}

	/**
	 * Web binder initialization.
	 * 
	 * @param binder the binder to initialize
	 */
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		initBinderDateFormatEditor(binder);
	}
	
	public TimeZone getDefaultTimeZone() {
		return defaultTimeZone;
	}
	public Class<? extends NodeDatum> getDefaultDatumClass() {
		return defaultDatumClass;
	}
	public QueryBiz getQueryBiz() {
		return queryBiz;
	}
	
}
