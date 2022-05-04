/* ==================================================================
 * TagFilter.java - 29/04/2022 4:37:35 pm
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

package net.solarnetwork.central.domain;

/**
 * Filter API for tags.
 * 
 * @author matt
 * @version 1.0
 * @since 1.2
 */
public interface TagFilter {

	/**
	 * Get the first tag.
	 * 
	 * <p>
	 * This returns the first available tag from the {@link #getTags()} array,
	 * or {@literal null} if not available.
	 * </p>
	 * 
	 * @return the first tag, or {@literal null} if not available
	 */
	String getTag();

	/**
	 * Get an array of tags.
	 * 
	 * @return array of tags (may be {@literal null})
	 */
	String[] getTags();

}
