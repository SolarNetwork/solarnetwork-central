/* ==================================================================
 * TagCriteria.java - 1/08/2022 2:36:47 pm
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
 * Criteria API for a "tag" qualifier.
 * 
 * @author matt
 * @version 1.0
 */
public interface TagCriteria {

	/**
	 * Test if any tag criteria exists.
	 * 
	 * @return {@literal true} if a tag criteria exists
	 */
	default boolean hasTagCriteria() {
		String kind = getTag();
		return (kind != null);
	}

	/**
	 * Get an array of tags.
	 * 
	 * @return array of tags (may be {@literal null})
	 */
	String[] getTags();

	/**
	 * Get the first tag.
	 * 
	 * <p>
	 * This returns the first available tag from the {@link #getTags()} array,
	 * or {@literal null} if not available.
	 * </p>
	 * 
	 * @return the tag, or {@literal null} if not available
	 */
	default String getTag() {
		String[] kinds = getTags();
		return (kinds != null && kinds.length > 0 ? kinds[0] : null);
	}

}
