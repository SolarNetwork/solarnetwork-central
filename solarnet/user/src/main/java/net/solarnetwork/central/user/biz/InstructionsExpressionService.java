/* ==================================================================
 * InstructionsExpressionService.java - 19/11/2025 9:45:21 am
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

package net.solarnetwork.central.user.biz;

import java.util.Map;
import org.springframework.expression.Expression;
import org.springframework.util.PathMatcher;
import net.solarnetwork.central.common.http.HttpOperations;
import net.solarnetwork.central.datum.biz.DatumStreamsAccessor;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.instructor.domain.NodeInstruction;
import net.solarnetwork.central.user.domain.NodeInstructionExpressionRoot;

/**
 * API for a service that can evaluate expressions.
 * 
 * @author matt
 * @version 1.0
 */
public interface InstructionsExpressionService {

	/**
	 * The user secrets topic.
	 */
	String USER_SECRET_TOPIC_ID = "instr";

	/**
	 * Get a {@link PathMatcher} that can be used for source ID matching.
	 *
	 * @return the matcher, never {@literal null}
	 */
	PathMatcher sourceIdPathMatcher();

	/**
	 * Create a standard instruction expression root instance.
	 *
	 * @param owner
	 *        the owner
	 * @param instruction
	 *        the instruction
	 * @param parameters
	 *        the parameters
	 * @param datumStreamsAccessor
	 *        the datum streams accessor
	 * @param httpOperations
	 *        the optional HTTP operations
	 * @return the root
	 */
	NodeInstructionExpressionRoot createNodeInstructionExpressionRoot(SolarNodeOwnership owner,
			NodeInstruction instruction, Map<String, ?> parameters,
			DatumStreamsAccessor datumStreamsAccessor, HttpOperations httpOperations);

	/**
	 * Parse an expression into an {@link Expression} instance.
	 *
	 * @param expression
	 *        the expression source to parse
	 * @return the expression instance
	 */
	Expression parseExpression(String expression);

	/**
	 * Evaluate an expression.
	 *
	 * @param <T>
	 *        the result type
	 * @param expression
	 *        the expression to evaluate
	 * @param root
	 *        the root object
	 * @param variables
	 *        optional expression variables
	 * @param resultClass
	 *        the result type
	 * @return the result
	 */
	<T> T evaluateExpression(Expression expression, Object root, Map<String, Object> variables,
			Class<T> resultClass);

	/**
	 * Parse and evaluate an expression.
	 * 
	 * <p>
	 * This implementation calls {@link #parseExpression(String)} and passes
	 * that to {@link #evaluateExpression(Expression, Object, Map, Class)}.
	 * </p>
	 * 
	 * @param <T>
	 *        the result type
	 * @param expression
	 *        the expression to evaluate
	 * @param root
	 *        the root object
	 * @param variables
	 *        optional expression variables
	 * @param resultClass
	 *        the result type
	 * @return the result
	 */
	default <T> T evaulateExpression(String expression, Object root, Map<String, Object> variables,
			Class<T> resultClass) {
		return evaluateExpression(parseExpression(expression), root, variables, resultClass);
	}

}
