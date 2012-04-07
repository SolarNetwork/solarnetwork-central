/* ==================================================================
 * ParticipantAdminController.java - Jun 12, 2011 3:42:59 PM
 * 
 * Copyright 2007-2011 SolarNetwork.net Dev Team
 * 
 * This participant is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This participant is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this participant; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.dras.web;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import net.solarnetwork.central.dras.biz.ParticipantAdminBiz;
import net.solarnetwork.central.dras.domain.Capability;
import net.solarnetwork.central.dras.domain.EffectiveCollection;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.domain.Participant;
import net.solarnetwork.central.dras.domain.ParticipantGroup;
import net.solarnetwork.central.dras.support.CapableParticipant;
import net.solarnetwork.central.dras.support.CapableParticipantGroup;
import net.solarnetwork.central.dras.support.MembershipCommand;
import net.solarnetwork.central.dras.web.support.ParticipantCommand;
import net.solarnetwork.central.dras.web.support.ParticipantGroupCommand;
import net.solarnetwork.web.support.WebUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Controller for ParticipantAdminBiz.
 * 
 * @author matt
 * @version $Revision$
 */
@Controller
@RequestMapping("/part/admin")
public class ParticipantAdminController extends ControllerSupport {

	private ParticipantAdminBiz participantAdminBiz;

	/**
	 * Constructor.
	 * 
	 * @param participantAdminBiz the ParticipantAdminBiz to use
	 */
	@Autowired
	public ParticipantAdminController(ParticipantAdminBiz participantAdminBiz) {
		this.participantAdminBiz = participantAdminBiz;
	}

	/**
	 * Store a new Participant.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param input the input data
	 * @param errors the binding errors
	 * @return view name
	 */
	// FIXME: remove GET support, only for testing
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, 
			value = {"/addParticipant.*", "/saveParticipant.*"})
	public String createParticipant(HttpServletRequest request, 
			Model model, @Valid ParticipantCommand input, Errors errors) {
		Participant newParticipant = participantAdminBiz.storeParticipant(input.getParticipant());
		model.addAttribute(MODEL_KEY_RESULT, newParticipant);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}

	/**
	 * Store a Participant Capability.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param input the input data
	 * @param errors the binding errors
	 * @return view name
	 */
	// FIXME: remove GET support, only for testing
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, 
			value = "/saveParticipantCapability.*")
	public String storeParticipantCapability(HttpServletRequest request, 
			Model model, CapableParticipant input) {
		Capability result = participantAdminBiz.storeParticipantCapability(
				input.getId(), input.getCapability());
		model.addAttribute(MODEL_KEY_RESULT, result);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}

	/**
	 * Store a new ParticipantGroup.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param input the input data
	 * @param errors the binding errors
	 * @return view name
	 */
	// FIXME: remove GET support, only for testing
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, 
			value = {"/addParticipantGroup.*", "/saveParticipantGroup.*"})
	public String createParticipantGroup(HttpServletRequest request, 
			Model model, @Valid ParticipantGroupCommand input, Errors errors) {
		try {
			ParticipantGroup newParticipantGroup = participantAdminBiz.storeParticipantGroup(
					input.getParticipantGroup());
			model.addAttribute(MODEL_KEY_RESULT, newParticipantGroup);
		} catch ( DuplicateKeyException e ) {
			log.debug("Duplicate key violation adding new participantGroup [{}]", 
					input.getParticipantGroup().getName());
			errors.rejectValue("participantGroup.name", "name.taken");
		}
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}

	/**
	 * Store a ParticipantGroup Capability.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param input the input data
	 * @param errors the binding errors
	 * @return view name
	 */
	// FIXME: remove GET support, only for testing
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, 
			value = "/saveParticipantGroupCapability.*")
	public String storeParticipantGroupCapability(HttpServletRequest request, 
			Model model, CapableParticipantGroup input) {
		Capability result = participantAdminBiz.storeParticipantGroupCapability(
				input.getId(), input.getCapability());
		model.addAttribute(MODEL_KEY_RESULT, result);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}

	/**
	 * Assign participants to a ParticipantGroup.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param input the input data
	 * @return view name
	 */
	// FIXME: remove GET support, only for testing
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, 
			value = "/assignParticipantGroupMembers.*")
	public String assignParticipantGroupMembers(HttpServletRequest request, 
			Model model, MembershipCommand input) {
		EffectiveCollection<ParticipantGroup, ? extends Member> result
			= participantAdminBiz.assignParticipantGroupMembers(input);
		model.addAttribute(MODEL_KEY_RESULT, result);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
}
