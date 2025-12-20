/* ==================================================================
 * InstructionExpressionEvaluationResult.java - 1/12/2025 10:19:16 am
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

import java.util.LinkedHashMap;
import java.util.Map;
import net.solarnetwork.central.instructor.domain.Instruction;

/**
 * Instruction expression evaluation result.
 * 
 * @author matt
 * @version 1.0
 */
public record InstructionExpressionEvaluationResult(String expression, Map<String, ?> parameters,
		Instruction instruction) {

	/**
	 * Create a new expression simulation result instance.
	 * 
	 * @param expression
	 *        the expression
	 * @param parameters
	 *        the expression parameters (will be copied)
	 * @param instruction
	 *        the instruction generated (will be copied)
	 * @return the new instance
	 */
	public static InstructionExpressionEvaluationResult expressionResult(String expression,
			Map<String, ?> parameters, Instruction instruction) {
		return new InstructionExpressionEvaluationResult(expression,
				parameters != null ? new LinkedHashMap<>(parameters) : null,
				instruction != null ? new Instruction(instruction) : null);
	}

}
