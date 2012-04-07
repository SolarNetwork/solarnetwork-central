/* ==================================================================
 * LocationController.java - Jun 11, 2011 10:21:14 AM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dras.web;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import net.solarnetwork.central.dras.biz.LocationBiz;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.support.LocationCriteria;
import net.solarnetwork.web.support.WebUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for LocationBiz API.
 * 
 * @author matt
 * @version $Revision$
 */
@Controller
@RequestMapping("/loc")
public class LocationController extends ControllerSupport {

	private LocationBiz locationBiz;
	
	/**
	 * Constructor.
	 * 
	 * @param locationBiz the LocationBiz to use
	 */
	@Autowired
	public LocationController(LocationBiz locationBiz) {
		this.locationBiz = locationBiz;
	}
	
	/**
	 * Get a single location.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param id the ID
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/location.*")
	public String getLocation(HttpServletRequest request, Model model,
			@RequestParam(value = "locationId") Long id) {
		model.addAttribute(MODEL_KEY_RESULT, locationBiz.getLocation(id));
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Search for locations.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param input the search command
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/findLocations.*")
	public String findLocations(HttpServletRequest request, Model model,
			LocationCriteria input) {
		List<Match> results = locationBiz.findLocations(input, input.getSortDescriptors()); 
		model.addAttribute(MODEL_KEY_RESULT, results);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
}
