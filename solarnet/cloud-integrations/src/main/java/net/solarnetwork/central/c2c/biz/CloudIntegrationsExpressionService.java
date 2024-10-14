/* ==================================================================
 * CloudIntegrationsExpressionService.java - 8/10/2024 7:59:23 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.biz;

import java.util.Map;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPropertyConfiguration;

/**
 * API for a service that can evaluate expressions.
 *
 * @author matt
 * @version 1.0
 */
public interface CloudIntegrationsExpressionService {

	/**
	 * Evaluate a property expression.
	 *
	 * @param <T>
	 *        the result type
	 * @param property
	 *        the property to evaluate
	 * @param root
	 *        the root object, for example a
	 *        {@link net.solarnetwork.domain.datum.DatumSamplesExpressionRoot}
	 * @param variables
	 *        optional expression variables
	 * @param resultClass
	 *        the result type
	 * @return the result
	 */
	<T> T evaluateDatumPropertyExpression(CloudDatumStreamPropertyConfiguration property, Object root,
			Map<String, Object> variables, Class<T> resultClass);

}
