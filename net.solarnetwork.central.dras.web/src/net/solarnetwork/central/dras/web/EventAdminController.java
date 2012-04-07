/* ==================================================================
 * EventAdminController.java - Jun 15, 2011 9:13:45 PM
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
import javax.validation.Valid;

import net.solarnetwork.central.dras.biz.EventAdminBiz;
import net.solarnetwork.central.dras.domain.EffectiveCollection;
import net.solarnetwork.central.dras.domain.Event;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.web.support.EventCommand;
import net.solarnetwork.web.support.WebUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Controller for EventAdmin API.
 * 
 * @author matt
 * @version $Revision$
 */
@Controller
@RequestMapping("/event/admin")
public class EventAdminController extends ControllerSupport {

	private EventAdminBiz eventAdminBiz;

	/**
	 * Constructor.
	 * 
	 * @param eventAdminBiz the EventAdminBiz to use
	 */
	@Autowired
	public EventAdminController(EventAdminBiz eventAdminBiz) {
		this.eventAdminBiz = eventAdminBiz;
	}
	
	/**
	 * Store a new Event.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param input the input data
	 * @param errors the binding errors
	 * @return view name
	 */
	// FIXME: remove GET support, only for testing
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, 
			value = {"/addEvent.*", "/saveEvent.*"})
	public String createEvent(HttpServletRequest request, 
			Model model, @Valid EventCommand input, Errors errors) {
		try {
			Event newEvent = eventAdminBiz.storeEvent(input.getEvent());
			model.addAttribute(MODEL_KEY_RESULT, newEvent);
		} catch ( DuplicateKeyException e ) {
			log.debug("Duplicate key violation adding new event [{}]", 
					input.getEvent().getName());
			errors.rejectValue("event.name", "name.taken");
		}
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}

	/**
	 * Assign participants to an Event.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param p the input data
	 * @param g the input data
	 * @return view name
	 */
	// FIXME: remove GET support, only for testing
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, 
			value = "/assignMembers.*")
	public String assignUserGroupMembers(HttpServletRequest request, Model model, 
			EventCommand input) {
		EffectiveCollection<Event, ? extends Member> result
			= eventAdminBiz.assignMembers(input.getEvent().getId(), input.getP(), input.getG());
		model.addAttribute(MODEL_KEY_RESULT, result);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
}
