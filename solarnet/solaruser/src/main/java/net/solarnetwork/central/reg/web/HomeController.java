/* ==================================================================
 * HomeController.java - Nov 22, 2012 9:58:42 AM
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

package net.solarnetwork.central.reg.web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Controller for the secure home page.
 * 
 * @author matt
 * @version 2.0
 */
@GlobalServiceController
@RequestMapping(method = RequestMethod.GET)
public class HomeController {

	/**
	 * Default constructor.
	 */
	public HomeController() {
		super();
	}

	@RequestMapping({ "/", "/index.do" })
	public String index() {
		return "index";
	}

	@RequestMapping("/u/sec/home")
	public String home() {
		return "sec/home";
	}

	@RequestMapping({ "/login", "/login.do" })
	public String login() {
		return "login";
	}

	@RequestMapping("/logoutSuccess.do")
	public String loggedOut() {
		return "loggedout";
	}

}
