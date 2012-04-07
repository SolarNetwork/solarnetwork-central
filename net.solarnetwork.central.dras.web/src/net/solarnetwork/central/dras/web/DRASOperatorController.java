/* ==================================================================
 * DRASOperatorController.java - May 11, 2011 4:25:42 PM
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

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.solarnetwork.central.domain.Identity;
import net.solarnetwork.central.dras.biz.DRASObserverBiz;
import net.solarnetwork.central.dras.biz.DRASOperatorBiz;
import net.solarnetwork.central.dras.domain.Event;
import net.solarnetwork.central.dras.domain.EventParticipants;
import net.solarnetwork.central.dras.domain.EventTarget;
import net.solarnetwork.central.dras.domain.EventTargets;
import net.solarnetwork.central.dras.domain.Program;
import net.solarnetwork.central.security.SecurityException;
import net.solarnetwork.util.JodaDateFormatEditor;
import net.solarnetwork.util.JodaDurationEditor;
import net.solarnetwork.util.JodaPeriodFormatEditor;
import net.solarnetwork.web.support.WebUtils;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Controller for DRASOperator API.
 * 
 * @author matt
 * @version $Revision$
 */
@Controller
@RequestMapping("/ops")
public class DRASOperatorController {

	/** The model key for the primary result object. */
	public static final String MODEL_KEY_RESULT = "result";

	/** The default value for the {@code requestDateFormat} property. */
	public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm";

	private DRASObserverBiz drasObserverBiz;
	private DRASOperatorBiz drasOperatorBiz;
	private String viewName;
	
	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());
	
	/**
	 * Constructor.
	 * 
	 * @param drasObserverBiz the DRASObserverBiz to use
	 */
	@Autowired
	public DRASOperatorController(DRASObserverBiz drasObserverBiz, DRASOperatorBiz drasOperatorBiz) {
		this.drasObserverBiz = drasObserverBiz;
		this.drasOperatorBiz = drasOperatorBiz;
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
	 * Setup support for Joda date/times.
	 * @param binder
	 */
	@InitBinder 
	public void initBinder(WebDataBinder binder) {
		// we turn OFF autoGrowNestedPaths, so we can make use of interfaces as collection types
		// e.g. List<NodeIdentity> which does not work with autoGrowNestedPaths
		binder.setAutoGrowNestedPaths(false);
		binder.registerCustomEditor(Period.class, new JodaPeriodFormatEditor());
		binder.registerCustomEditor(Duration.class, new JodaDurationEditor());
		binder.registerCustomEditor(DateTime.class, 
				new JodaDateFormatEditor(DATE_TIME_FORMAT, TimeZone.getTimeZone("UTC")));
	}
	
	/**
	 * Get list of available programs.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @return view name
	 */
	// FIXME: remove GET support, only for testing
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, value = "/createEvent.*")
	public String createEvent(HttpServletRequest request, Event input, Model model) {
		Program program = drasObserverBiz.getProgram(input.getProgramId());
		Event result = drasOperatorBiz.createEvent(program, input.getName(), 
				input.getEventDate(), input.getEndDate());
		model.asMap().clear();
		model.addAttribute(MODEL_KEY_RESULT, result);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Assign targets to an existing event.
	 * 
	 * @param request the servlet request
	 * @param input input command data
	 * @param model the model
	 * @return view name
	 */
	// FIXME: remove GET support, only for testing
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, value = "/assignEventTargets.*")
	public String assignEventTargets(HttpServletRequest request, 
			EventTargetsCommand input, Model model) {
		Event event = drasObserverBiz.getEvent(input.getEventId());
		SortedSet<EventTarget> sortedTargets = new TreeSet<EventTarget>(input.getTargets());
		EventTargets result = drasOperatorBiz.assignEventTargets(event, sortedTargets);
		model.asMap().clear();
		model.addAttribute(MODEL_KEY_RESULT, result);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Assign participants to an existing event.
	 * 
	 * @param request the servlet request
	 * @param input input command data
	 * @param model the model
	 * @return view name
	 */
	// FIXME: remove GET support, only for testing
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, value = "/assignEventParticipants.*")
	public String assignEventParticipants(HttpServletRequest request, 
			ParticipantsCommand input, Model model) {
		Event event = drasObserverBiz.getEvent(input.getId());
		Set<Identity<Long>> participants = new LinkedHashSet<Identity<Long>>(input.getP());
		EventParticipants result = drasOperatorBiz.assignEventParticipants(event, participants);
		model.asMap().clear();
		model.addAttribute(MODEL_KEY_RESULT, result);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Assign participants to an existing event.
	 * 
	 * @param request the servlet request
	 * @param input input command data
	 * @param model the model
	 * @return view name
	 */
	// FIXME: remove GET support, only for testing
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, value = "/assignEventGroups.*")
	public String assignEventGroups(HttpServletRequest request, 
			ParticipantsCommand input, Model model) {
		Event event = drasObserverBiz.getEvent(input.getId());
		Set<Identity<Long>> groups = new LinkedHashSet<Identity<Long>>(input.getG());
		EventParticipants result = drasOperatorBiz.assignEventParticipantGroups(event, groups);
		model.asMap().clear();
		model.addAttribute(MODEL_KEY_RESULT, result);
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
