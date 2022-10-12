/* ==================================================================
 * ExternalSystemClient.java - 22/08/2022 8:40:21 am
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.http;

import java.util.function.Supplier;
import org.springframework.http.HttpMethod;
import net.solarnetwork.central.oscp.util.TaskContext;

/**
 * Service API for integration with external systems.
 * 
 * @author matt
 * @version 1.0
 */
public interface ExternalSystemClient {

	/**
	 * Make an exchange with an external system, expecting a HTTP 204 (No
	 * Content) response.
	 * 
	 * <p>
	 * A JSON content type is assumed.
	 * </p>
	 * 
	 * @param context
	 *        the task context
	 * @param method
	 *        the HTTP method to perform
	 * @param path
	 *        the URL path supplier, relative to the external system's base URL
	 * @param body
	 *        the HTTP body content
	 */
	void systemExchange(TaskContext<?> context, HttpMethod method, Supplier<String> path, Object body);

}
