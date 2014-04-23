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
 */

package net.solarnetwork.central.web.domain;

/**
 * A simple service response envelope object.
 * 
 * @author matt
 * @version 1.1
 * @param <T>
 *        the object type
 * @deprecated use the net.solarnetwork.web.domain.Response class directly
 */
@Deprecated
public class Response<T> extends net.solarnetwork.web.domain.Response<T> {

	/**
	 * Construct a successful response with no data.
	 */
	public Response() {
		super();
	}

	/**
	 * Construct a successful response with just data.
	 * 
	 * @param data
	 *        the data
	 */
	public Response(T data) {
		super(data);
	}

	/**
	 * Constructor.
	 * 
	 * @param success
	 *        flag of success
	 * @param code
	 *        optional code, e.g. error code
	 * @param message
	 *        optional descriptive message
	 * @param data
	 *        optional data in the response
	 */
	public Response(Boolean success, String code, String message, T data) {
		super(success, code, message, data);
	}

	/**
	 * Helper method to construct instance using generic return type inference.
	 * 
	 * <p>
	 * If you import this static method, then in your code you can write
	 * {@code return response(myData)} instead of
	 * {@code new Response&lt;Object&gt;(myData)}.
	 * </p>
	 * 
	 * @param data
	 *        the data
	 * @return the response
	 */
	public static <V> Response<V> response(V data) {
		return new Response<V>(data);
	}

}
