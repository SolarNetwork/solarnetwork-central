/* ==================================================================
 * HttpUserEvents.java - 19/11/2025 11:48:39 am
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

package net.solarnetwork.central.common.http;

/**
 * Constants and helpers for common HTTP user event handling.
 * 
 * @author matt
 * @version 1.0
 */
public interface HttpUserEvents {

	/** A user event tag for an HTTP event. */
	String HTTP_TAG = "http";

	/** User event data key for a URL. */
	String HTTP_URI_DATA_KEY = "uri";

	/** User event data key for an HTTP method. */
	String HTTP_METHOD_DATA_KEY = "method";

	/** User event data key for HTTP body content. */
	String HTTP_BODY_DATA_KEY = "body";

	/** User event data key for HTTP status code. */
	String HTTP_STATUS_CODE_DATA_KEY = "status";

	/** User event data key for HTTP response data. */
	String HTTP_RESPONSE_BODY_DATA_KEY = "responseBody";

	/** User event data keyf or HTTP response data length. */
	String HTTP_RESPONSE_BODY_LENGTH_DATA_KEY = "responseLength";

}
