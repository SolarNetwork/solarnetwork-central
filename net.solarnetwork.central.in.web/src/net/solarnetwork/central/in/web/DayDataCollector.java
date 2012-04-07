/* ===================================================================
 * DayDataCollector.java
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

import java.beans.PropertyEditor;

import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.datum.domain.DayDatum;
import net.solarnetwork.central.in.biz.DataCollectorBiz;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Controller for accepting day data from a node.
 * 
 * @author matt.magoffin
 * @version $Revision$ $Date$
 */
@Controller
@RequestMapping("/dayCollector.do")
public class DayDataCollector extends AbstractDataCollector {

	private static final String[] REQUIRED_POST_FIELDS = new String[] {
		"nodeId", "day", "sunrise", "sunset"
	};
	
	/**
	 * Default constructor.
	 */
	public DayDataCollector() {
		super();
	}
	
	/**
	 * Constructor.
	 * 
	 * @param dataCollectorBiz the {@link DataCollectorBiz} to use
	 * @param solarNodeDao the {@link SolarNodeDao} to use
	 */
	@Autowired
	public DayDataCollector(DataCollectorBiz dataCollectorBiz, 
			SolarNodeDao solarNodeDao) {
		setDataCollectorBiz(dataCollectorBiz);
		setSolarNodeDao(solarNodeDao);
	}

	/**
	 * Post new day data.
	 * 
	 * @param dayDatum the DayDatum to post
	 * @param model the model
	 * @return the result model
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String postDayData(DayDatum datum, Model model) {
		return defaultHandlePostDatum(datum, model, "dayDatum");
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
		binder.registerCustomEditor(LocalDate.class,
				(PropertyEditor)LOCAL_DATE_EDITOR.clone());
		binder.registerCustomEditor(LocalTime.class,
				(PropertyEditor)LOCAL_TIME_EDITOR.clone());
		
		binder.setRequiredFields(REQUIRED_POST_FIELDS);
	}
	
}
