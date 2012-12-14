/* ==================================================================
 * NodeInstructionController.java - Sep 30, 2011 12:24:52 PM
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

package net.solarnetwork.central.reg.web;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.web.support.WebUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for managing node instructions.
 * 
 * @author matt
 * @version $Revision$
 */
@Controller
@RequestMapping("/instr")
public class NodeInstructionController {

	/** The model key for the primary result object. */
	public static final String MODEL_KEY_RESULT = "result";

	/** The default view name. */
	public static final String DEFAULT_VIEW_NAME = "xml";

	@Autowired private InstructorBiz instructorBiz;
	
	/**
	 * Queue a new node instruction.
	 * 
	 * @param request the servlet request
	 * @param input the instruction input
	 * @param model the model
	 * @return view name
	 */
	// FIXME: remove GET support, only for testing
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, value = "/add.*")
	public String queueInstruction(HttpServletRequest request, NodeInstruction input,
			Model model) {	
		NodeInstruction instr = instructorBiz.queueInstruction(input.getNodeId(), input);
		model.asMap().clear();
		model.addAttribute(MODEL_KEY_RESULT, instr);
		return WebUtils.resolveViewFromUrlExtension(request, null);
	}
	
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, value = "/viewActive.*")
	public String activeInstructions(HttpServletRequest request, 
			@RequestParam("nodeId") Long nodeId, Model model) {
		List<Instruction> instructions = instructorBiz.getActiveInstructionsForNode(nodeId);
		model.asMap().clear();
		model.addAttribute(MODEL_KEY_RESULT, instructions);
		return WebUtils.resolveViewFromUrlExtension(request, null);
	}
	
	@RequestMapping(method = {RequestMethod.GET, RequestMethod.POST}, value = "/view.*")
	public String viewInstruction(HttpServletRequest request, 
			@RequestParam("id") Long instructionId, Model model) {
		Instruction instruction = instructorBiz.getInstruction(instructionId);
		model.asMap().clear();
		model.addAttribute(MODEL_KEY_RESULT, instruction);
		return WebUtils.resolveViewFromUrlExtension(request, null);
	}
	
}
