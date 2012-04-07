/* ==================================================================
 * ParticipantController.java - Jun 12, 2011 3:42:46 PM
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

import net.solarnetwork.central.dras.biz.ParticipantBiz;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.support.ParticipantCriteria;
import net.solarnetwork.central.dras.support.ParticipantGroupCriteria;
import net.solarnetwork.web.support.WebUtils;

import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for ParticipantBiz.
 * 
 * @author matt
 * @version $Revision$
 */
@Controller
@RequestMapping("/part")
public class ParticipantController extends ControllerSupport {

	private ParticipantBiz participantBiz;
	
	/**
	 * Constructor.
	 * 
	 * @param participantBiz the ParticipantBiz
	 */
	@Autowired
	public ParticipantController(ParticipantBiz participantBiz) {
		this.participantBiz = participantBiz;
	}
	
	/**
	 * Get a single participant.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param id the ID
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/participant.*")
	public String getParticipant(HttpServletRequest request, Model model,
			@RequestParam(value = "participantId") Long id) {
		model.addAttribute(MODEL_KEY_RESULT, participantBiz.getParticipant(id));
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Get a single capable participant.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param id the ID
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/capableParticipant.*")
	public String getCapableParticipant(HttpServletRequest request, Model model,
			@RequestParam(value = "participantId") Long id, 
			@RequestParam(value = "effectiveDate", required = false)
			@DateTimeFormat(iso=ISO.DATE_TIME) DateTime effectiveDate) {
		model.addAttribute(MODEL_KEY_RESULT, participantBiz.getCapableParticipant(id));
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Search for participants.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param input the search command
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/findParticipants.*")
	public String findParticipants(HttpServletRequest request, Model model,
			ParticipantCriteria input) {
		List<Match> results = participantBiz.findParticipants(input, input.getSortDescriptors()); 
		model.addAttribute(MODEL_KEY_RESULT, results);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}

	/**
	 * Get a single participant group.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param id the ID
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/participantGroup.*")
	public String getParticipantGroup(HttpServletRequest request, Model model,
			@RequestParam(value = "participantGroupId") Long id) {
		model.addAttribute(MODEL_KEY_RESULT, participantBiz.getParticipantGroup(id));
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Get a single capable participant group.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param id the ID
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/capableParticipantGroup.*")
	public String getCapableParticipantGroup(HttpServletRequest request, Model model,
			@RequestParam(value = "participantGroupId") Long id, 
			@RequestParam(value = "effectiveDate", required = false)
			@DateTimeFormat(iso=ISO.DATE_TIME) DateTime effectiveDate) {
		model.addAttribute(MODEL_KEY_RESULT, participantBiz.getCapableParticipantGroup(id));
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Search for participant groups.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param input the search command
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/findParticipantGroups.*")
	public String findParticipantGroups(HttpServletRequest request, Model model,
			ParticipantGroupCriteria input) {
		List<Match> results = participantBiz.findParticipantGroups(input, input.getSortDescriptors()); 
		model.addAttribute(MODEL_KEY_RESULT, results);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}

}
