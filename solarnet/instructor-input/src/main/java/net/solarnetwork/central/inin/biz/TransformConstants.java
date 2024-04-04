/* ==================================================================
 * TransformConstants.java - 29/03/2024 9:21:23 am
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

package net.solarnetwork.central.inin.biz;

import org.springframework.util.MimeType;

/**
 * Constants for instruction input.
 *
 * @author matt
 * @version 1.0
 */
public final class TransformConstants {

	private TransformConstants() {
		// not available
	}

	/** The JSON MIME type. */
	public static final MimeType JSON_TYPE = MimeType.valueOf("application/json");

	/** The XML MIME type. */
	public static final MimeType XML_TYPE = MimeType.valueOf("text/xml");

	/**
	 * A parameter key for a transform instance cache key.
	 *
	 * <p>
	 * This key is meant to provide the transform with a context-specific cache
	 * key that can be applied to a given transform invocation. The key must
	 * uniquely identify the transform configuration.
	 * </p>
	 */
	public static final String PARAM_CONFIGURATION_CACHE_KEY = "cache-key";

	/**
	 * A parameter key for a transform debug output {@link Appendable}.
	 *
	 * <p>
	 * The provided {@link Appendable} will have transform debugging info
	 * appended.
	 * </p>
	 */
	public static final String PARAM_DEBUG_OUTPUT = "debug-output";

	/** A parameter key for a SolarNetwork user ID. */
	public static final String PARAM_USER_ID = "user-id";

	/** A parameter key for a endpoint ID. */
	public static final String PARAM_ENDPOINT_ID = "endpoint-id";

	/** A parameter key for a transform ID. */
	public static final String PARAM_TRANSFORM_ID = "transform-id";

	/** A parameter key for a preview boolean flag. */
	public static final String PARAM_PREVIEW = "preview";

}
