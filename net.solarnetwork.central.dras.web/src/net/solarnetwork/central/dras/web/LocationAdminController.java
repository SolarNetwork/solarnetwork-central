/* ==================================================================
 * LocationAdminController.java - Jun 13, 2011 9:22:25 PM
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

import javax.servlet.http.HttpServletRequest;

import net.solarnetwork.central.dras.biz.LocationAdminBiz;
import net.solarnetwork.central.dras.domain.Location;
import net.solarnetwork.web.support.WebUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 *Controller for LocationAdminBiz.
 * 
 * @author matt
 * @version $Revision$
 */
@Controller
@RequestMapping("/loc/admin")
public class LocationAdminController extends ControllerSupport {

	private LocationAdminBiz locationAdminBiz;

	/**
	 * Constructor.
	 * 
	 * @param locationAdminBiz the LocationAdminBiz to use
	 */
	@Autowired
	public LocationAdminController(LocationAdminBiz locationAdminBiz) {
		this.locationAdminBiz = locationAdminBiz;
	}

	/**
	 * Store a Location.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param input the input data
	 * @return view name
	 */
	// FIXME: remove GET support, only for testing
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, 
			value = {"/addLocation.*", "/saveLocation.*"})
	public String createParticipant(HttpServletRequest request, 
			Model model, Location input) {
		Location newLoc = locationAdminBiz.storeLocation(input);
		model.addAttribute(MODEL_KEY_RESULT, newLoc);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}

}
