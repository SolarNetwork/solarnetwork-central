/* ==================================================================
 * UserNodeInstructionTaskSimulationOutput.java - 1/12/2025 9:18:32 am
 * 
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.util.List;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.central.domain.UserEvent;
import net.solarnetwork.central.instructor.domain.Instruction;

/**
 * Output of an instruction task simulation.
 * 
 * @author matt
 * @version 1.0
 */
public class UserNodeInstructionTaskSimulationOutput {

	private final UserNodeInstructionTaskEntity task;
	private final @Nullable Instruction instruction;
	private final @Nullable List<UserEvent> events;
	private final @Nullable List<InstructionExpressionEvaluationResult> expressionResults;
	private final @Nullable String message;

	/**
	 * Constructor.
	 * 
	 * @param task
	 *        the simulation input
	 * @param instruction
	 *        the generated instruction
	 * @param events
	 *        the events
	 * @param expressionResults
	 *        expression evaluations results
	 * @param message
	 *        an error message
	 * @throws IllegalArgumentException
	 *         if {@code task} is {@code null}
	 */
	public UserNodeInstructionTaskSimulationOutput(@Nullable UserNodeInstructionTaskEntity task,
			@Nullable Instruction instruction, @Nullable List<UserEvent> events,
			@Nullable List<InstructionExpressionEvaluationResult> expressionResults,
			@Nullable String message) {
		super();
		this.task = requireNonNullArgument(task, "task");
		this.instruction = instruction;
		this.events = events;
		this.expressionResults = expressionResults;
		this.message = message;
	}

	/**
	 * Get the task.
	 * 
	 * @return the task
	 */
	public final UserNodeInstructionTaskEntity getTask() {
		return task;
	}

	/**
	 * Get the generated instruction.
	 * 
	 * @return the instruction
	 */
	public final @Nullable Instruction getInstruction() {
		return instruction;
	}

	/**
	 * Get the events.
	 * 
	 * @return the events
	 */
	public final @Nullable List<UserEvent> getEvents() {
		return events;
	}

	/**
	 * Get the expression evaluation results.
	 * 
	 * @return the expressionResults
	 */
	public final @Nullable List<InstructionExpressionEvaluationResult> getExpressionResults() {
		return expressionResults;
	}

	/**
	 * Get the message.
	 * 
	 * @return the message
	 */
	public final @Nullable String getMessage() {
		return message;
	}

}
