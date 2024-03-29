/* ==================================================================
 * CentralInstructionInputUserEvents.java - 28/03/2024 11:05:42 am
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

package net.solarnetwork.central.inin.domain;

/**
 * Constants for central instruction input (ININ) user events.
 *
 * @author matt
 * @version 1.0
 */
public interface CentralInstructionInputUserEvents {

	/** A user event tag for ININ. */
	String ININ_TAG = "inin";

	/** A user event tag for ININ instruction handling. */
	String INSTRUCTION_TAG = "instruction";

	/** A user event tag for ININ "error" . */
	String ERROR_TAG = "error";

	/** User event data key for an endpoint ID. */
	String ENDPOINT_ID_DATA_KEY = "endpointId";

	/** User event data key for a content MIME type. */
	String CONTENT_TYPE_DATA_KEY = "contentType";

	/** User event data key for a request transform ID. */
	String REQ_TRANSFORM_ID_DATA_KEY = "reqTransformId";

	/** User event data key for a request transform service ID. */
	String REQ_TRANSFORM_SERVICE_ID_DATA_KEY = "reqTransformServiceId";

	/** User event data key for an output MIME type. */
	String OUTPUT_TYPE_DATA_KEY = "outputType";

	/** User event data key for a response transform ID. */
	String RES_TRANSFORM_ID_DATA_KEY = "resTransformId";

	/** User event data key for a response transform service ID. */
	String RES_TRANSFORM_SERVICE_ID_DATA_KEY = "resTransformServiceId";

	/** User event tags for instruction events. */
	String[] INSTRUCTION_TAGS = new String[] { INSTRUCTION_TAG, ININ_TAG };

	/**
	 * User event data key for a {@code Map} of additional parameters, for
	 * example input parameters to a transformation.
	 */
	String PARAMETERS_DATA_KEY = "parameters";

}
