/* ==================================================================
 * DRASObserverController.java - May 1, 2011 10:16:29 AM
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
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.solarnetwork.central.dras.biz.DRASObserverBiz;
import net.solarnetwork.central.dras.domain.CapabilityInformation;
import net.solarnetwork.central.dras.domain.Event;
import net.solarnetwork.central.dras.domain.EventParticipants;
import net.solarnetwork.central.dras.domain.Program;
import net.solarnetwork.central.security.SecurityException;
import net.solarnetwork.web.support.WebUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for DRASObserver API.
 * 
 * @author matt
 * @version $Revision$
 */
@Controller
@RequestMapping("/obs")
public class DRASObserverController {

	/** The model key for the primary result object. */
	public static final String MODEL_KEY_RESULT = "result";
	
	private DRASObserverBiz drasObserverBiz;
	private String viewName;
	
	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	/**
	 * Constructor.
	 * 
	 * @param drasObserverBiz the DRASObserverBiz to use
	 */
	@Autowired
	public DRASObserverController(DRASObserverBiz drasObserverBiz) {
		this.drasObserverBiz = drasObserverBiz;
	}
	
	/**
	 * SecurityException handler.
	 * 
	 * <p>Logs a WARN log and returns HTTP 403 (Forbidden).</p>
	 * 
	 * @param e the security exception
	 * @param res the servlet response
	 */
	@ExceptionHandler(SecurityException.class)
	public void handleSecurityException(SecurityException e, HttpServletResponse res) {
		if ( log.isWarnEnabled() ) {
			log.warn("Security exception: " +e.getMessage());
		}
		res.setStatus(HttpServletResponse.SC_FORBIDDEN);
	}
	
	/**
	 * Get list of available programs.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/programs.*")
	public String getAllPrograms(HttpServletRequest request, Model model) {
		List<Program> programs = drasObserverBiz.getAllPrograms(null);
		model.addAttribute(MODEL_KEY_RESULT, programs);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Get the specified program.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/program.*")
	public String getProgram(HttpServletRequest request, Model model, 
			@RequestParam(value = "programId") Long programId) {
		Program program = drasObserverBiz.getProgram(programId);
		model.addAttribute(MODEL_KEY_RESULT, program);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Get the specified event.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/event.*")
	public String getEvent(HttpServletRequest request, Model model, 
			@RequestParam(value = "eventId") Long eventId) {
		Event event = drasObserverBiz.getEvent(eventId);
		model.addAttribute(MODEL_KEY_RESULT, event);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Get all participants of a given program.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param programId the program ID
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/programParticipants.*")
	public String getProgramParticipants(HttpServletRequest request, Model model, 
			@RequestParam(value = "programId") Long programId) {
		Program program = drasObserverBiz.getProgram(programId);
		Set<CapabilityInformation> participants = drasObserverBiz.getProgramParticipants(
				program, null, null);
		model.addAttribute(MODEL_KEY_RESULT, participants);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}

	/**
	 * Get all events for a given program.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param programId the program ID
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/events.*")
	public String getProgramEvents(HttpServletRequest request, Model model, 
			@RequestParam(value = "programId") Long programId) {
		Program program = drasObserverBiz.getProgram(programId);
		List<Event> events = drasObserverBiz.getEvents(
				program, null, null);
		model.addAttribute(MODEL_KEY_RESULT, events);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}

	/**
	 * Get an EventParticipants for a given event.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param eventId the program ID
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/eventParticipants.*")
	public String getEventParticipants(HttpServletRequest request, Model model, 
			@RequestParam(value = "eventId") Long eventId) {
		Event event = drasObserverBiz.getEvent(eventId);
		EventParticipants participants = drasObserverBiz.getCurrentEventParticipants(event);
		model.addAttribute(MODEL_KEY_RESULT, participants);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Get an details for each participant for a given event.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param eventId the program ID
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/eventParticipantDetails.*")
	public String getEventParticipantDetails(HttpServletRequest request, Model model, 
			@RequestParam(value = "eventId") Long eventId) {
		Event event = drasObserverBiz.getEvent(eventId);
		Set<CapabilityInformation> details = drasObserverBiz.getEventParticipants(event, null, null);
		model.addAttribute(MODEL_KEY_RESULT, details);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Get an EventParticipants for a given event.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param eventId the program ID
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/groups.*")
	public String getParticipantGroups(HttpServletRequest request, Model model) {
		List<CapabilityInformation> groups = drasObserverBiz.getAllParticipantGroups(null);
		model.addAttribute(MODEL_KEY_RESULT, groups);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Get the set of participants for a given group.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param groupId the group ID
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/groupMembers.*")
	public String getParticipantGroupMembers(HttpServletRequest request, Model model,
			@RequestParam(value = "groupId") Long groupId) {
		Set<CapabilityInformation> members = drasObserverBiz.getParticipantGroupMembers(
				groupId, null, null);
		model.addAttribute(MODEL_KEY_RESULT, members);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
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
