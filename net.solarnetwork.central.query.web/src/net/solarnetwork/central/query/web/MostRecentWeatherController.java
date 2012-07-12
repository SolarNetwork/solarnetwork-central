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

import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.datum.dao.DayDatumDao;
import net.solarnetwork.central.datum.dao.WeatherDatumDao;
import net.solarnetwork.central.datum.domain.DayDatum;
import net.solarnetwork.central.datum.domain.ReportingDatum;
import net.solarnetwork.central.datum.domain.WeatherDatum;
import net.solarnetwork.central.domain.SolarNode;
import net.solarnetwork.central.web.AbstractNodeController;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalTime;
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

	private WeatherDatumDao weatherDatumDao;
	private DayDatumDao dayDatumDao;
	
	/**
	 * Constructor.
	 * 
	 * @param solarNodeDao the SolarNodeDao to use
	 * @param weatherDatumDao the WeatherDatumDao to use
	 * @param dayDatumDao the DayDatumDao to use
	 */
	@Autowired
	public MostRecentWeatherController(SolarNodeDao solarNodeDao, 
			WeatherDatumDao weatherDatumDao, DayDatumDao dayDatumDao) {
		super();
		setSolarNodeDao(solarNodeDao);
		this.weatherDatumDao = weatherDatumDao;
		this.dayDatumDao = dayDatumDao;
	}
	
	/**
	 * Get the most recent WeatherDatum for a given node.
	 * 
	 * @param nodeId the ID of the node to get the weather info for
	 * @param request the servlet request
	 * @return model and view
	 */
	@RequestMapping(value = "/currentWeather.*", method = RequestMethod.GET)
	public ModelAndView getMostRecentWeather(@RequestParam(value="nodeId") Long nodeId, 
			HttpServletRequest request) {
		ModelAndView mv = resolveViewFromUrlExtension(request);

		// get the SolarNode for the specified node, for the appropriate time zone
		SolarNode node = getSolarNodeDao().get(nodeId);
		
		WeatherDatum weather = weatherDatumDao.getMostRecentWeatherDatum(nodeId, new DateTime());
		DayDatum day = null;
		LocalTime infoTime = null;
		if ( weather instanceof ReportingDatum ) {
			ReportingDatum repWeather = (ReportingDatum)weather;
			day = dayDatumDao.getDatumForDate(nodeId, repWeather.getLocalDate());
			infoTime = repWeather.getLocalTime();
		} else if ( weather != null && weather.getInfoDate() != null ) {
			day = dayDatumDao.getDatumForDate(nodeId, weather.getInfoDate());
			infoTime = weather.getInfoDate().toDateTime(DateTimeZone.forTimeZone(
					node.getTimeZone())).toLocalTime();
		}
		if ( weather != null && day != null && infoTime != null
				&& (weather.getCondition() != null || day.getCondition() != null) ) {
			// check for night-time, this assumes all conditions set to day values from DAO
			if ( infoTime.isBefore(day.getSunrise()) || infoTime.isAfter(day.getSunset()) ) {
				// change to night-time
				if ( weather.getCondition() != null ) {
					weather.setCondition(weather.getCondition().getNightEquivalent());
				}
				if ( day.getCondition() != null ) {
					day.setCondition(weather.getCondition().getNightEquivalent());
				}
			}
		}
		mv.addObject("weather", weather);
		mv.addObject("day", day);
		mv.addObject("tz", node.getTimeZone());
		return mv;
	}
	
}
