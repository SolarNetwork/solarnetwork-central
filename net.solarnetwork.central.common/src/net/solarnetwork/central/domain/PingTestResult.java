/* ==================================================================
 * PingTestResult.java - 25/05/2015 10:23:21 am
 * 
 * Copyright 2007-2015 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.domain;

import java.util.Map;

/**
 * A results object for a single {@link PingTest} result.
 * 
 * @author matt
 * @version 1.0
 */
public class PingTestResult {

	private final boolean success;
	private final String message;
	private final Map<String, ?> properties;

	/**
	 * Construct values.
	 * 
	 * @param success
	 *        The success flag.
	 * @param message
	 *        The message.
	 * @param properties
	 *        Optional properties.
	 */
	public PingTestResult(boolean success, String message, Map<String, ?> properties) {
		super();
		this.success = success;
		this.message = message;
		this.properties = properties;
	}

	/**
	 * Construct with status flag and message.
	 * 
	 * @param success
	 *        The success flag.
	 * @param message
	 *        The message.
	 */
	public PingTestResult(boolean success, String message) {
		this(success, message, null);
	}

	public String getMessage() {
		return message;
	}

	public boolean isSuccess() {
		return success;
	}

	public Map<String, ?> getProperties() {
		return properties;
	}

}
