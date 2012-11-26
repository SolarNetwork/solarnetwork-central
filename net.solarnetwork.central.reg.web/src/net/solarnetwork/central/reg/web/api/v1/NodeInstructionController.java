/* ==================================================================
 * NodeInstructionController.java - Nov 26, 2012 4:16:58 PM
 * 
 * Copyright 2007-2012 SolarNetwork.net Dev Team
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
 */

package net.solarnetwork.central.reg.web.api.v1;

import static net.solarnetwork.central.reg.web.api.domain.Response.response;
import java.util.List;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.reg.web.api.domain.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller for node instruction web service API.
 * 
 * @author matt
 * @version 1.0
 */
@Controller("v1nodeInstructionController")
@RequestMapping(value = "/v1/sec/instr", method = RequestMethod.GET)
public class NodeInstructionController {

	@Autowired
	private InstructorBiz instructorBiz;

	/**
	 * Get a list of all active instructions for a specific node.
	 * 
	 * @param nodeId
	 *        the ID of the node to get instructions for
	 * @return the active instructions for the node
	 */
	@RequestMapping("/viewActive")
	@ResponseBody
	public Response<List<Instruction>> activeInstructions(@RequestParam("nodeId") Long nodeId) {
		List<Instruction> instructions = instructorBiz.getActiveInstructionsForNode(nodeId);
		return response(instructions);
	}

	/**
	 * View a single instruction, based on its primary key.
	 * 
	 * @param instructionId
	 *        the ID of the instruction to view
	 * @return the instruction
	 */
	@RequestMapping("/view")
	@ResponseBody
	public Response<Instruction> viewInstruction(@RequestParam("id") Long instructionId) {
		Instruction instruction = instructorBiz.getInstruction(instructionId);
		return response(instruction);
	}
}
