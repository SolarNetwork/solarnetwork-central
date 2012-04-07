/* ==================================================================
 * PriceLocationLookup.java - Feb 20, 2011 7:54:21 PM
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

package net.solarnetwork.central.in.web;

import net.solarnetwork.central.dao.SolarNodeDao;
import net.solarnetwork.central.domain.PriceLocation;
import net.solarnetwork.central.in.biz.DataCollectorBiz;
import net.solarnetwork.central.in.biz.PriceLocationCriteria;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Web access to PriceLocation data.
 * 
 * @author matt
 * @version $Revision$
 */
@Controller
@RequestMapping({"/priceLocationLookup.do", "/u/priceLocationLookup.do"})
public class PriceLocationLookup {

	/** The default value for the {@code viewName} property. */
	public static final String DEFAULT_VIEW_NAME = "xml";
	
	/** The model key for the {@code PriceLocation} result. */
	public static final String MODEL_KEY_RESULT = "result";
	
	private static final String[] REQUIRED_FIELDS = new String[] {
		"sourceName", "locationName",
	};
		
	private DataCollectorBiz dataCollectorBiz;
	private String viewName = DEFAULT_VIEW_NAME;

	///** A class-level logger. */
	//private final org.slf4j.Logger log = LoggerFactory.getLogger(getClass());
	
	/**
	 * Default constructor.
	 */
	public PriceLocationLookup() {
		super();
	}
	
	/**
	 * Constructor.
	 * 
	 * @param dataCollectorBiz the {@link DataCollectorBiz} to use
	 * @param solarNodeDao the {@link SolarNodeDao} to use
	 */
	@Autowired
	public PriceLocationLookup(DataCollectorBiz dataCollectorBiz) {
		setDataCollectorBiz(dataCollectorBiz);
	}

	/**
	 * Web binder initialization.
	 * 
	 * @param binder the binder to initialize
	 */
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.setRequiredFields(REQUIRED_FIELDS);
		binder.setIgnoreInvalidFields(true);
	}
	
	/**
	 * Query for a PriceLocation.
	 * 
	 * @param criteria the search criteria
	 * @param model the model
	 * @param criteriaModelKey the model key the criteria is stored on
	 * @return the result model name
	 */
	@RequestMapping(method = RequestMethod.GET)
	public String findPriceLocation(PriceLocationCriteria criteria, Model model, 
			String criteriaModelKey) {
		PriceLocation location = getDataCollectorBiz().findPriceLocation(criteria);
		if ( location != null ) {
			model.asMap().clear();
			model.addAttribute(MODEL_KEY_RESULT, location);
		}
		return getViewName();
	}
	
	/**
	 * @return the dataCollectorBiz
	 */
	public DataCollectorBiz getDataCollectorBiz() {
		return dataCollectorBiz;
	}

	/**
	 * @param dataCollectorBiz the dataCollectorBiz to set
	 */
	public void setDataCollectorBiz(DataCollectorBiz dataCollectorBiz) {
		this.dataCollectorBiz = dataCollectorBiz;
	}

	/**
	 * @return the viewName
	 */
	public String getViewName() {
		return viewName;
	}

	/**
	 * @param viewName the viewName to set
	 */
	public void setViewName(String viewName) {
		this.viewName = viewName;
	}

}
