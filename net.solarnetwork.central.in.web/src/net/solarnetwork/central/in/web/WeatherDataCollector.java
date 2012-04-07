/* ===================================================================
 * WeatherDataCollector.java
 * 
 * Created Aug 31, 2008 7:48:23 AM
 * 
 * Copyright (c) 2008 Solarnetwork.net Dev Team.
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

package net.solarnetwork.central.in.web;

import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.datum.domain.WeatherDatum;
import net.solarnetwork.central.in.biz.DataCollectorBiz;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Controller for accepting weather data from a node.
 * 
 * @author matt.magoffin
 * @version $Revision$ $Date$
 */
@Controller
@RequestMapping("/weatherCollector.do")
public class WeatherDataCollector extends AbstractDataCollector {

	private static final String[] REQUIRED_POST_FIELDS = new String[] {
		"nodeId", "infoDate", "temperatureCelcius",
	};

	/**
	 * Default constructor.
	 */
	public WeatherDataCollector() {
		super();
	}
	
	/**
	 * Constructor.
	 * 
	 * @param dataCollectorBiz the {@link DataCollectorBiz} to use
	 * @param solarNodeDao the {@link SolarNodeDao} to use
	 */
	@Autowired
	public WeatherDataCollector(DataCollectorBiz dataCollectorBiz, 
			SolarNodeDao solarNodeDao) {
		setDataCollectorBiz(dataCollectorBiz);
		setSolarNodeDao(solarNodeDao);
	}

	/**
	 * Post new weather data.
	 * 
	 * @param weatherDatum the WeatherDatum to post
	 * @param model the model
	 * @return the result model
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String postWeatherData(WeatherDatum datum, Model model) {
		return defaultHandlePostDatum(datum, model, "weatherDatum");
	}
	
	/**
	 * Web binder initialization.
	 * 
	 * @param binder the binder to initialize
	 */
	@InitBinder
	@Override
	public void initBinder(WebDataBinder binder) {
		super.initBinder(binder);
		binder.setRequiredFields(REQUIRED_POST_FIELDS);
	}
	
}
