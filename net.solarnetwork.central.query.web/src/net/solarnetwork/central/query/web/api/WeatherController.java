/* ==================================================================
 * WeatherController.java - Mar 20, 2013 4:40:05 PM
 * 
 * Copyright 2007-2013 SolarNetwork.net Dev Team
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

import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.query.domain.WeatherConditions;
import net.solarnetwork.central.web.domain.Response;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller for querying weather related data.
 * 
 * @author matt
 * @version 1.0
 */
@Controller("v1WeatherController")
@RequestMapping({ "/api/v1/sec/weather", "/api/v1/pub/weather" })
public class WeatherController extends WebServiceControllerSupport {

	private final QueryBiz queryBiz;

	/**
	 * Constructor.
	 * 
	 * @param queryBiz
	 *        the QueryBiz to use
	 */
	@Autowired
	public WeatherController(QueryBiz queryBiz) {
		super();
		this.queryBiz = queryBiz;
	}

	/**
	 * Get the most-recently-collected weather information for a particular
	 * node.
	 * 
	 * <p>
	 * This method returns variety of information in {@code weather} and
	 * {@code day} top-level objects.
	 * </p>
	 * 
	 * <p>
	 * Example URL: <code>/api/v1/sec/weather/recent?nodeId=1</code>
	 * </p>
	 * 
	 * <p>
	 * Example JSON response:
	 * 
	 * <pre>
	 * {
	 *   "success": true,
	 *   "data": {
	 *     "weather": {
	 *       "locationId": 4298974,
	 *       "infoDate": "2013-02-24 18:10:00.000Z",
	 *       "skyConditions": "Partly cloudy",
	 *       "temperatureCelsius": 13.5,
	 *       "humidity": 79,
	 *       "barometricPressure": 1032,
	 *       "condition": "FewClouds",
	 *       "localDate": "2013-02-25",
	 *       "localTime": "07:10"
	 *     },
	 *     "day": {
	 *       "id": 4316523,
	 *       "created": "2013-02-24 19:15:00.150+0000",
	 *       "locationId": 4298974,
	 *       "day": "2013-02-25",
	 *       "sunrise": "06:57",
	 *       "sunset": "20:10"
	 *     },
	 *     "timeZone": "Pacific/Auckland"
	 *   }
	 * }
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @param nodeId
	 *        the ID of the node to query for weather data
	 * @return the weather conditions
	 */
	@ResponseBody
	@RequestMapping(value = "/recent", method = RequestMethod.GET)
	public Response<WeatherConditions> getMostRecentWeather(@RequestParam(value = "nodeId") Long nodeId) {
		WeatherConditions cond = queryBiz.getMostRecentWeatherConditions(nodeId);
		return new Response<WeatherConditions>(cond);
	}

}
