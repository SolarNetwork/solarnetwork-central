/* ==================================================================
 * ConfigurationController.java - 2/10/2017 10:10:39 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

import static net.solarnetwork.web.domain.Response.response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import net.solarnetwork.central.biz.AppConfigurationBiz;
import net.solarnetwork.central.domain.AppConfiguration;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import net.solarnetwork.web.domain.Response;

/**
 * REST controller for configuration API.
 * 
 * @author matt
 * @version 1.0
 */
@RestController("v1ConfigurationController")
@RequestMapping(value = { "/api/v1/pub/config", "/api/v1/sec/config" })
public class ConfigurationController extends WebServiceControllerSupport {

	private final AppConfigurationBiz appConfigurationBiz;

	/**
	 * Constructor.
	 * 
	 * @param appConfigurationBiz
	 *        the service to use
	 */
	@Autowired
	public ConfigurationController(AppConfigurationBiz appConfigurationBiz) {
		super();
		this.appConfigurationBiz = appConfigurationBiz;
	}

	/**
	 * Get the application configuration.
	 * 
	 * @return the app configuration response
	 */
	@ResponseBody
	@RequestMapping(value = "", method = RequestMethod.GET)
	public Response<AppConfiguration> getAppConfiguration() {
		return response(appConfigurationBiz.getAppConfiguration());
	}

}
