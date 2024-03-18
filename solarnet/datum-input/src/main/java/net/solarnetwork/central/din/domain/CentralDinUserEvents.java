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

/**
 * Constants for central datum input (DIN) user events.
 *
 * @author matt
 * @version 1.1
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

	/** User event data key for an transform ID. */
	String TRANSFORM_ID_DATA_KEY = "transformId";

	/** User event data key for an transform service ID. */
	String TRANSFORM_SERVICE_ID_DATA_KEY = "transformServiceId";

	/** User event tags for datum events. */
	String[] DATUM_TAGS = new String[] { DIN_TAG, DATUM_TAG };

	/**
	 * User event data key for a {@code Map} of additional parameters, for
	 * example input parameters to a transformation.
	 *
	 * @since 1.1
	 */
	String PARAMETERS_DATA_KEY = "parameters";

}
