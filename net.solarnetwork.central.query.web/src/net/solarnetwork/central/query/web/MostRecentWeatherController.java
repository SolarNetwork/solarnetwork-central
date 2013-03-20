/* ===================================================================
 * MostRecentWeatherController.java
 * 
 * Created Aug 16, 2009 2:29:12 PM
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

import javax.servlet.http.HttpServletRequest;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.query.domain.WeatherConditions;
import net.solarnetwork.central.web.AbstractNodeController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * Controller for getting the most recently available WeatherDatum for a node.
 * 
 * @author matt
 * @version $Revision$ $Date$
 */
@Controller
public class MostRecentWeatherController extends AbstractNodeController {

	private final QueryBiz queryBiz;

	/**
	 * Constructor.
	 * 
	 * @param queryBiz
	 *        the QueryBiz to use
	 */
	@Autowired
	public MostRecentWeatherController(QueryBiz queryBiz) {
		super();
		this.queryBiz = queryBiz;
	}

	/**
	 * Get the most recent WeatherDatum for a given node.
	 * 
	 * @param nodeId
	 *        the ID of the node to get the weather info for
	 * @param request
	 *        the servlet request
	 * @return model and view
	 */
	@RequestMapping(value = "/currentWeather.*", method = RequestMethod.GET)
	public ModelAndView getMostRecentWeather(@RequestParam(value = "nodeId") Long nodeId,
			HttpServletRequest request) {
		ModelAndView mv = resolveViewFromUrlExtension(request);

		WeatherConditions conditions = queryBiz.getMostRecentWeatherConditions(nodeId);
		mv.addObject("weather", conditions.getWeather());
		mv.addObject("day", conditions.getDay());
		mv.addObject("tz", conditions.getTimeZone());
		return mv;
	}

}
