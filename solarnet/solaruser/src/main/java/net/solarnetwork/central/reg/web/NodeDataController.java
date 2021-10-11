/* ==================================================================
 * NodeDataController.java - 15/10/2016 7:14:56 AM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.reg.web;

import java.util.Set;
import java.util.TimeZone;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.web.GlobalExceptionRestController;
import net.solarnetwork.util.JodaDateFormatEditor;
import net.solarnetwork.util.JodaDateFormatEditor.ParseMode;
import net.solarnetwork.web.domain.Response;

/**
 * REST controller to support data queries.
 * 
 * @author matt
 * @version 1.1
 */
@RestController("v1NodeDataController")
@RequestMapping(value = "/sec/node-data")
@GlobalExceptionRestController
public class NodeDataController {

	private final QueryBiz queryBiz;

	@Autowired
	public NodeDataController(QueryBiz queryBiz) {
		super();
		this.queryBiz = queryBiz;
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
	 * Get the set of source IDs available for the available GeneralNodeData for
	 * a single node, optionally constrained within a date range.
	 * 
	 * @param nodeId
	 *        the node ID
	 * @return the available sources
	 */
	@ResponseBody
	@RequestMapping(value = "/{nodeId}/sources", method = RequestMethod.GET)
	public Response<Set<String>> getAvailableSources(@PathVariable("nodeId") Long nodeId) {
		DatumFilterCommand filter = new DatumFilterCommand();
		filter.setNodeId(nodeId);
		Set<String> data = queryBiz.getAvailableSources(filter);
		return new Response<Set<String>>(data);
	}

}
