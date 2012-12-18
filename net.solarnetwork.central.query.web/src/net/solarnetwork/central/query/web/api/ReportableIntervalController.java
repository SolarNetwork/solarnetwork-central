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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.solarnetwork.central.datum.domain.NodeDatum;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.query.domain.ReportableInterval;
import net.solarnetwork.central.query.domain.ReportableIntervalType;
import net.solarnetwork.central.query.web.domain.ReportableIntervalCommand;
import net.solarnetwork.central.web.domain.Response;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller for querying for reportable interval values.
 * 
 * <p>
 * See the {@link ReportableInterval} class for information about what is
 * returned to the view.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
@Controller("v1ReportableIntervalController")
@RequestMapping({ "/api/v1/sec/range", "/api/v1/pub/range" })
public class ReportableIntervalController extends WebServiceControllerSupport {

	private final QueryBiz queryBiz;

	/**
	 * Constructor.
	 * 
	 * @param queryBiz
	 *        the QueryBiz to use
	 */
	@Autowired
	public ReportableIntervalController(QueryBiz queryBiz) {
		super();
		this.queryBiz = queryBiz;
	}

	@ResponseBody
	@RequestMapping(value = "/interval", method = RequestMethod.GET)
	public Response<ReportableInterval> getReportableInterval(ReportableIntervalCommand cmd) {
		List<Class<? extends NodeDatum>> typeList = new ArrayList<Class<? extends NodeDatum>>();
		for ( ReportableIntervalType type : cmd.getTypes() ) {
			typeList.add(type.getDatumTypeClass());
		}
		@SuppressWarnings("unchecked")
		Class<? extends NodeDatum>[] types = typeList.toArray(new Class[typeList.size()]);
		ReportableInterval data = queryBiz.getReportableInterval(cmd.getNodeId(), types);
		return new Response<ReportableInterval>(data);
	}

	@ResponseBody
	@RequestMapping(value = "/sources", method = RequestMethod.GET)
	public Response<Set<String>> getAvailableSources(ReportableIntervalCommand cmd) {
		if ( cmd.getTypes() == null || cmd.getTypes().length < 1 ) {
			Set<String> data = Collections.emptySet();
			return new Response<Set<String>>(data);
		}
		Class<? extends NodeDatum> type = cmd.getTypes()[0].getDatumTypeClass();
		Set<String> data = queryBiz.getAvailableSources(cmd.getNodeId(), type, cmd.getStart(),
				cmd.getEnd());
		return new Response<Set<String>>(data);
	}

}
