/* ==================================================================
 * ProgramAdminController.java - Jun 11, 2011 9:54:55 PM
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

import net.solarnetwork.central.dras.biz.ProgramAdminBiz;
import net.solarnetwork.central.dras.domain.EffectiveCollection;
import net.solarnetwork.central.dras.domain.Member;
import net.solarnetwork.central.dras.domain.Program;
import net.solarnetwork.central.dras.support.MembershipCommand;
import net.solarnetwork.central.dras.web.support.ConstraintCommand;
import net.solarnetwork.central.dras.web.support.ProgramCommand;
import net.solarnetwork.web.support.WebUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Controller for ProgramAdmin API.
 * 
 * @author matt
 * @version $Revision$
 */
@Controller
@RequestMapping("/pro/admin")
public class ProgramAdminController extends ControllerSupport {

	private ProgramAdminBiz programAdminBiz;

	/**
	 * Constructor.
	 * 
	 * @param programAdminBiz the ProgramAdminBiz to use
	 */
	@Autowired
	public ProgramAdminController(ProgramAdminBiz programAdminBiz) {
		this.programAdminBiz = programAdminBiz;
	}

	/**
	 * Store a new Program.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param input the input data
	 * @param errors the binding errors
	 * @return view name
	 */
	// FIXME: remove GET support, only for testing
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, 
			value = {"/addProgram.*", "/saveProgram.*"})
	public String createProgram(HttpServletRequest request, 
			Model model, @Valid ProgramCommand input, Errors errors) {
		try {
			Program newProgram = programAdminBiz.storeProgram(input.getProgram());
			model.addAttribute(MODEL_KEY_RESULT, newProgram);
		} catch ( DuplicateKeyException e ) {
			log.debug("Duplicate key violation adding new program [{}]", 
					input.getProgram().getName());
			errors.rejectValue("program.name", "name.taken");
		}
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}

	/**
	 * Assign participants to a Program.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param input the input data
	 * @return view name
	 */
	// FIXME: remove GET support, only for testing
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, 
			value = "/assignParticipantMembers.*")
	public String assignParticipantMembers(HttpServletRequest request, 
			Model model, MembershipCommand input) {
		EffectiveCollection<Program, ? extends Member> result
			= programAdminBiz.assignParticipantMembers(input);
		model.addAttribute(MODEL_KEY_RESULT, result);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Assign constraints to a Program.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param input the input data
	 * @return view name
	 */
	// FIXME: remove GET support, only for testing
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, 
			value = "/assignProgramConstraints.*")
	public String saveProgramConstraints(HttpServletRequest request, Model model,
			ConstraintCommand input) {
		programAdminBiz.storeProgramConstraints(input.getProgramId(), input.getC());
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
}
