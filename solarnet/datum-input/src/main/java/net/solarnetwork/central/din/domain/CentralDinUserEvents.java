/* ==================================================================
 * CentralDinUserEvents.java - 7/03/2024 5:18:54 am
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

package net.solarnetwork.central.din.domain;

import java.util.List;

/**
 * Constants for central datum input (DIN) user events.
 *
 * @author matt
 * @version 1.4
 */
public interface CentralDinUserEvents {

	/** A user event tag for DIN. */
	String DIN_TAG = "din";

	/** A user event tag for DIN "error" . */
	String ERROR_TAG = "error";

	/** A user event tag for DIN datum handling . */
	String DATUM_TAG = "datum";

	/** User event data key for an endpoint ID. */
	String ENDPOINT_ID_DATA_KEY = "endpointId";

	/** User event data key for a content MIME type. */
	String CONTENT_TYPE_DATA_KEY = "contentType";

	/** User event data key for a transform ID. */
	String TRANSFORM_ID_DATA_KEY = "transformId";

	/** User event data key for a transform service ID. */
	String TRANSFORM_SERVICE_ID_DATA_KEY = "transformServiceId";

	/** User event tags for datum events. */
	List<String> DATUM_TAGS = List.of(DIN_TAG, DATUM_TAG);

	/**
	 * User event data key for a {@code Map} of additional parameters, for
	 * example input parameters to a transformation.
	 *
	 * @since 1.1
	 */
	String PARAMETERS_DATA_KEY = "parameters";

	/**
	 * User event data key for a request content value.
	 *
	 * <p>
	 * If the content is textual, the content will be included as-is. Otherwise,
	 * it will be Base64 encoded.
	 * </p>
	 *
	 * @since 1.2
	 */
	String CONTENT_DATA_KEY = "content";

	/**
	 * User event data key for the tracked previous request content value.
	 *
	 * <p>
	 * If the content is textual, the content will be included as-is. Otherwise,
	 * it will be Base64 encoded.
	 * </p>
	 *
	 * @since 1.3
	 */
	String PREVIOUS_CONTENT_DATA_KEY = "previousContent";

}
