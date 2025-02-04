/* ==================================================================
 * ObjectDatumStreamMetadataResult.java - 5/02/2025 10:55:22 am
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

package net.solarnetwork.central.query.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import net.solarnetwork.domain.Location;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * The result API for object datum stream metadata.
 * 
 * <p>
 * This interface is used for API documentation only.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public interface ObjectDatumStreamMetadataResult {

	/**
	 * Get the datum stream ID.
	 * 
	 * @return the stream ID
	 */
	@Schema(description = "The datum stream ID.")
	String getStreamId();

	/**
	 * Get the time zone identifier associated with the datum stream.
	 * 
	 * @return the datum stream time zone identifier
	 */
	@Schema(description = "The time zone identifier associated with the datum stream.")
	String getZone();

	/**
	 * Get the datum stream kind.
	 * 
	 * @return the kind, as a {@link ObjectDatumKind#getKey()} value
	 */
	@Schema(description = "The datum stream kind key value, one of `n` or `l` for _node_ and _location_, repsectively.")
	String getKind();

	/**
	 * Get the datum stream object (node or location) ID.
	 * 
	 * @return the object ID
	 */
	@Schema(description = "The datum stream object (node or location) ID.")
	Long getObjectId();

	/**
	 * Get the datum stream source ID.
	 * 
	 * @return the source ID
	 */
	@Schema(description = "The datum stream source ID.")
	String getSourceId();

	/**
	 * Get the location associated with the datum stream.
	 * 
	 * @return the location
	 */
	@Schema(description = "The location associated with the datum stream, if available.")
	Location getLocation();

	/**
	 * Get the datum stream instantaneous property names.
	 * 
	 * @return the instantaneous property names
	 */
	@Schema(description = "The datum stream instantaneous property names.")
	String[] getI();

	/**
	 * Get the datum stream accumulating property names.
	 * 
	 * @return the accumulating property names
	 */
	@Schema(description = "The datum stream accumulating property names.")
	String[] getA();

	/**
	 * Get the datum stream status property names.
	 * 
	 * @return the status property names
	 */
	@Schema(description = "The datum stream status property names.")
	String[] getS();

}
