/* ==================================================================
 * ObjectCriteria.java - 27/11/2020 2:24:24 pm
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

package net.solarnetwork.central.datum.v2.dao;

import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;

/**
 * Search criteria for object related data.
 * 
 * @author matt
 * @version 1.0
 * @since 2.8
 */
public interface ObjectCriteria {

	/**
	 * Get the first object ID.
	 * 
	 * <p>
	 * This returns the first available object ID from the
	 * {@link #getObjectIds()} array, or {@literal null} if not available.
	 * </p>
	 * 
	 * @return the object ID, or {@literal null} if not available
	 */
	Long getObjectId();

	/**
	 * Get an array of object IDs.
	 * 
	 * @return array of object IDs (may be {@literal null})
	 */
	Long[] getObjectIds();

	/**
	 * Get the kind of object.
	 * 
	 * @return the object kind
	 */
	ObjectDatumKind getObjectKind();

}
