/* ==================================================================
 * Response.java - Nov 20, 2012 6:55:06 AM
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
 * $Id$
 * ==================================================================
 */

package net.solarnetwork.central.reg.web.api.domain;

import java.util.Map;

/**
 * A simple service response envelope object.
 * 
 * @author matt
 * @version 1.0
 */
public class Response {

	private final Boolean success;
	private final String code;
	private final String message;
	private final Map<String, Object> data;
	
	/**
	 * Construct a successful response with just data.
	 * 
	 * @param data the data
	 */
	public Response(Map<String, Object>data) {
		this(Boolean.TRUE, null, null, data);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param success flag of success
	 * @param code optional code, e.g. error code
	 * @param message optional descriptive message
	 * @param data optional data in the response
	 */
	public Response(Boolean success, String code, String message, Map<String, Object> data) {
		super();
		this.success = success;
		this.code = code;
		this.message = message;
		this.data = data;
	}

	public Boolean getSuccess() {
		return success;
	}

	public String getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	public Map<String, Object> getData() {
		return data;
	}
	
}
