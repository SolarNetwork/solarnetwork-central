/* ==================================================================
 * DatumStreamMetadata.java - 22/10/2020 3:01:10 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.domain;

import java.util.UUID;
import net.solarnetwork.domain.GeneralDatumSamplesType;

/**
 * Metadata about a datum stream.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public interface DatumStreamMetadata {

	/**
	 * Get the stream ID.
	 * 
	 * @return the stream ID
	 */
	UUID getStreamId();

	/**
	 * Get all property names included in the stream.
	 * 
	 * @return the property names
	 */
	String[] getPropertyNames();

	/**
	 * Get the subset of all property names that are of a specific type.
	 * 
	 * @param type
	 *        the type of property to get the names for
	 * @return the property names, or {@literal null} if none available or
	 *         {@code type} is {@link GeneralDatumSamplesType#Tag}
	 */
	String[] propertyNamesForType(GeneralDatumSamplesType type);

}
