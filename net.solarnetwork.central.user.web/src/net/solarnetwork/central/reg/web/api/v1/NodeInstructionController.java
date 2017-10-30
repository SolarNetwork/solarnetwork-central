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

import static net.solarnetwork.web.domain.Response.response;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.instructor.domain.Instruction;
import net.solarnetwork.central.instructor.domain.InstructionState;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.web.support.WebServiceControllerSupport;
import net.solarnetwork.web.domain.Response;

/**
 * Controller for node instruction web service API.
 * 
 * @author matt
 * @version 1.2
 */
@Controller("v1nodeInstructionController")
@RequestMapping(value = "/v1/sec/instr")
public class NodeInstructionController extends WebServiceControllerSupport {

	@Autowired
	private InstructorBiz instructorBiz;

	/**
	 * View a single instruction, based on its primary key.
	 * 
	 * @param instructionId
	 *        the ID of the instruction to view
	 * @return the instruction
	 */
	@RequestMapping(value = "/view", method = RequestMethod.GET, params = "!ids")
	@ResponseBody
	public Response<Instruction> viewInstruction(@RequestParam("id") Long instructionId) {
		Instruction instruction = instructorBiz.getInstruction(instructionId);
		return response(instruction);
	}

	/**
	 * View a set of instructions, based on their primary keys.
	 * 
	 * @param instructionIds
	 *        the IDs of the instructions to view
	 * @return the instruction
	 * @since 1.2
	 */
	@RequestMapping(value = "/view", method = RequestMethod.GET, params = "ids")
	@ResponseBody
	public Response<List<NodeInstruction>> viewInstruction(
			@RequestParam("ids") Set<Long> instructionIds) {
		List<NodeInstruction> results = instructorBiz.getInstructions(instructionIds);
		return response(results);
	}

	/**
	 * Get a list of all active instructions for a specific node.
	 * 
	 * @param nodeId
	 *        the ID of the node to get instructions for
	 * @return the active instructions for the node
	 */
	@RequestMapping(value = "/viewActive", method = RequestMethod.GET, params = "!nodeIds")
	@ResponseBody
	public Response<List<Instruction>> activeInstructions(@RequestParam("nodeId") Long nodeId) {
		List<Instruction> instructions = instructorBiz.getActiveInstructionsForNode(nodeId);
		return response(instructions);
	}

	/**
	 * Get a list of all active instructions for a set of nodes.
	 * 
	 * @param nodeIds
	 *        the IDs of the nodes to get instructions for
	 * @return the active instructions for the nodes
	 * @since 1.2
	 */
	@RequestMapping(value = "/viewActive", method = RequestMethod.GET, params = "nodeIds")
	@ResponseBody
	public Response<List<NodeInstruction>> activeInstructions(
			@RequestParam("nodeIds") Set<Long> nodeIds) {
		List<NodeInstruction> instructions = instructorBiz.getActiveInstructionsForNodes(nodeIds);
		return response(instructions);
	}

	/**
	 * Get a list of all pending instructions for a specific node.
	 * 
	 * @param nodeId
	 *        the ID of the node to get instructions for
	 * @return the pending instructions for the node
	 * @since 1.1
	 */
	@RequestMapping(value = "/viewPending", method = RequestMethod.GET, params = "!nodeIds")
	@ResponseBody
	public Response<List<Instruction>> pendingInstructions(@RequestParam("nodeId") Long nodeId) {
		List<Instruction> instructions = instructorBiz.getPendingInstructionsForNode(nodeId);
		return response(instructions);
	}

	/**
	 * Get a list of all pending instructions for a set of nodes.
	 * 
	 * @param nodeIds
	 *        the IDs of the nodes to get instructions for
	 * @return the pending instructions for the nodes
	 * @since 1.2
	 */
	@RequestMapping(value = "/viewPending", method = RequestMethod.GET, params = "nodeIds")
	@ResponseBody
	public Response<List<NodeInstruction>> pendingInstructions(
			@RequestParam("nodeIds") Set<Long> nodeIds) {
		List<NodeInstruction> instructions = instructorBiz.getPendingInstructionsForNodes(nodeIds);
		return response(instructions);
	}

	/**
	 * Enqueue a new instruction.
	 * 
	 * @param input
	 *        the instruction data to add to the queue
	 * @return the node instruction
	 */
	@RequestMapping(value = "/add", method = RequestMethod.POST, params = "!nodeIds")
	@ResponseBody
	public Response<NodeInstruction> queueInstruction(NodeInstruction input) {
		NodeInstruction instr = instructorBiz.queueInstruction(input.getNodeId(), input);
		return response(instr);
	}

	/**
	 * Enqueue one instruction for multiple nodes.
	 * 
	 * @param nodeIds
	 *        a set of node IDs to enqueue the instruction on
	 * @param input
	 *        the instruction data to add to the queue
	 * @return the node instructions
	 * @since 1.2
	 */
	@RequestMapping(value = "/add", method = RequestMethod.POST, params = "nodeIds")
	@ResponseBody
	public Response<List<NodeInstruction>> queueInstruction(@RequestParam("nodeIds") Set<Long> nodeIds,
			NodeInstruction input) {
		List<NodeInstruction> results = instructorBiz.queueInstructions(nodeIds, input);
		return response(results);
	}

	/**
	 * Update the state of an existing instruction.
	 * 
	 * @param instructionId
	 *        the ID of the instruction to update
	 * @param state
	 *        the desired state
	 */
	@RequestMapping(value = "/updateState", method = RequestMethod.POST, params = "!ids")
	@ResponseBody
	public Response<NodeInstruction> updateInstructionState(@RequestParam("id") Long instructionId,
			@RequestParam("state") InstructionState state) {
		instructorBiz.updateInstructionState(instructionId, state);
		return response(null);
	}

	/**
	 * Update the state of an existing instruction.
	 * 
	 * @param instructionIds
	 *        the IDs of the instructions to update
	 * @param state
	 *        the desired state
	 * @since 1.2
	 */
	@RequestMapping(value = "/updateState", method = RequestMethod.POST, params = "ids")
	@ResponseBody
	public Response<NodeInstruction> updateInstructionState(
			@RequestParam("ids") Set<Long> instructionIds,
			@RequestParam("state") InstructionState state) {
		instructorBiz.updateInstructionsState(instructionIds, state);
		return response(null);
	}

}
