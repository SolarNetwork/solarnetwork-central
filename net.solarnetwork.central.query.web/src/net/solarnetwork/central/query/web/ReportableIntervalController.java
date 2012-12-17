/* ===================================================================
 * ReportableIntervalController.java
 * 
 * Created Aug 5, 2009 3:12:15 PM
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import net.solarnetwork.central.datum.domain.NodeDatum;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.query.domain.ReportableInterval;
import net.solarnetwork.central.query.domain.ReportableIntervalType;
import net.solarnetwork.central.query.web.domain.ReportableIntervalCommand;
import net.solarnetwork.central.web.AbstractNodeController;
import net.solarnetwork.util.JodaDateFormatEditor;
import net.solarnetwork.util.JodaDateFormatEditor.ParseMode;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for querying for reportable interval values.
 * 
 * <p>
 * See the {@link ReportableInterval} class for information about what is
 * returned to the view.
 * </p>
 * 
 * @author matt
 * @version $Revision$ $Date$
 */
@Controller
public class ReportableIntervalController extends AbstractNodeController {

	private final QueryBiz queryBiz;

	/**
	 * Constructor.
	 * 
	 * @param solarNodeDao
	 *        the SolarNodeDao to use
	 * @param queryBiz
	 *        the QueryBiz to use
	 */
	@Autowired
	public ReportableIntervalController(QueryBiz queryBiz) {
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
		binder.registerCustomEditor(LocalDate.class, new JodaDateFormatEditor(DEFAULT_DATE_FORMAT,
				ParseMode.LocalDate));
	}

	/**
	 * Get a reportable interval for a node and list of NodeDatum types.
	 * 
	 * @param cmd
	 *        the command
	 * @param request
	 *        the request
	 * @return the model and view
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/reportableInterval.*", method = { RequestMethod.POST, RequestMethod.GET })
	public ModelAndView getReportableInterval(ReportableIntervalCommand cmd, HttpServletRequest request) {
		ModelAndView mv = resolveViewFromUrlExtension(request);

		List<Class<? extends NodeDatum>> typeList = new ArrayList<Class<? extends NodeDatum>>();
		for ( ReportableIntervalType type : cmd.getTypes() ) {
			typeList.add(type.getDatumTypeClass());
		}
		ReportableInterval data = queryBiz.getReportableInterval(cmd.getNodeId(),
				typeList.toArray(new Class[0]));
		mv.addObject("data", data);
		if ( data.getTimeZone() != null ) {
			mv.addObject("tz", data.getTimeZone());
		}

		if ( cmd.getNodeId() != null ) {
			// set up a PropertyEditorRegistrar that can be used for serializing data into view-friendly values
			setupViewPropertyEditorRegistrar(request, DEFAULT_DATE_FORMAT, data.getTimeZone());
		}
		return mv;
	}

	/**
	 * Get the set of available source IDs for a given node and data type,
	 * optionally filtered by a date range.
	 * 
	 * @param cmd
	 *        the command
	 * @return the set of source IDs
	 */
	@RequestMapping(value = "/availableSources.*", method = RequestMethod.GET)
	@ResponseBody
	public Set<String> getAvailableSources(ReportableIntervalCommand cmd) {
		if ( cmd.getTypes() == null || cmd.getTypes().length < 1 ) {
			return Collections.emptySet();
		}
		Class<? extends NodeDatum> type = cmd.getTypes()[0].getDatumTypeClass();
		return queryBiz.getAvailableSources(cmd.getNodeId(), type, cmd.getStart(), cmd.getEnd());
	}

}
