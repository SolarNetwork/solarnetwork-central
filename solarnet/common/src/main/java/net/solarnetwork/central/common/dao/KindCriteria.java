/* ==================================================================
 * KindCriteria.java - 1/08/2022 2:36:47 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao;

/**
 * Criteria API for a "kind" qualifier.
 * 
 * @author matt
 * @version 1.0
 */
public interface KindCriteria {

	/**
	 * Get an array of node IDs.
	 * 
	 * @return array of node IDs (may be {@literal null})
	 */
	String[] getKinds();

	/**
	 * Get the first kind.
	 * 
	 * <p>
	 * This returns the first available kind from the {@link #getKinds()} array,
	 * or {@literal null} if not available.
	 * </p>
	 * 
	 * @return the kind, or {@literal null} if not available
	 */
	default String getKind() {
		String[] kinds = getKinds();
		return (kinds != null && kinds.length > 0 ? kinds[0] : null);
	}

}
