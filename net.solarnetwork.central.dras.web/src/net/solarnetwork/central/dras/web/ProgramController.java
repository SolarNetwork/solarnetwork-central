/* ==================================================================
 * ProgramController.java - Jun 11, 2011 9:36:09 PM
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

import net.solarnetwork.central.dras.biz.ProgramBiz;
import net.solarnetwork.central.dras.domain.Constraint;
import net.solarnetwork.central.dras.domain.Match;
import net.solarnetwork.central.dras.support.ProgramCriteria;
import net.solarnetwork.web.support.WebUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for ProgramBiz.
 * 
 * @author matt
 * @version $Revision$
 */
@Controller
@RequestMapping("/pro")
public class ProgramController extends ControllerSupport {

	private ProgramBiz programBiz;
	
	/**
	 * Constructor.
	 * 
	 * @param programBiz the ProgramBiz to use
	 */
	@Autowired
	public ProgramController(ProgramBiz programBiz) {
		this.programBiz = programBiz;
	}

	/**
	 * Get a single program.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param id the ID
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/program.*")
	public String getProgram(HttpServletRequest request, Model model,
			@RequestParam(value = "programId") Long id) {
		model.addAttribute(MODEL_KEY_RESULT, programBiz.getProgram(id));
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Search for programs.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param input the search command
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/findPrograms.*")
	public String findPrograms(HttpServletRequest request, Model model,
			ProgramCriteria input) {
		List<Match> results = programBiz.findPrograms(input, input.getSortDescriptors()); 
		model.addAttribute(MODEL_KEY_RESULT, results);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
	/**
	 * Get the set of Program constraints.
	 * 
	 * @param request the servlet request
	 * @param model the model
	 * @param userId the user ID
	 * @return view name
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/programConstraints.*")
	public String getProgramConstraints(HttpServletRequest request, Model model,
			@RequestParam(value = "programId") Long programId) {
		Set<Constraint> results = programBiz.getProgramConstraints(programId);
		model.addAttribute(MODEL_KEY_RESULT, results);
		return WebUtils.resolveViewFromUrlExtension(request, getViewName());
	}
	
}
