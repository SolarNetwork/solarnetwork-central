/* ==================================================================
 * EventController.java - Jun 15, 2011 8:21:35 PM
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

import net.solarnetwork.central.dras.biz.EventBiz;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.support.EventCriteria;
import net.solarnetwork.web.support.WebUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for EventBiz.
 * 
 * @author matt
 * @version $Revision$
 */
@Controller
@RequestMapping("/event")
public class EventController extends ControllerSupport {

	private EventBiz eventBiz;
	
	@Autowired
	public EventController(EventBiz eventBiz) {
		this.eventBiz = eventBiz;
	}

	/**
	 * Get a single event.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param id the ID
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/event.*")
	public String getEvent(HttpServletRequest request, Model model,
			@RequestParam(value = "eventId") Long id) {
		model.addAttribute(MODEL_KEY_RESULT, eventBiz.getEvent(id));
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Search for events.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param input the search command
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/findEvents.*")
	public String findEvents(HttpServletRequest request, Model model,
			EventCriteria input) {
		List<Match> results = eventBiz.findEvents(input, input.getSortDescriptors()); 
		model.addAttribute(MODEL_KEY_RESULT, results);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
}
