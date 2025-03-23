/* ==================================================================
 * TopicCriteria.java - 12/08/2022 4:44:36 pm
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
 * Criteria API for topical entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface TopicCriteria {

	/**
	 * Test if any topic criteria exists.
	 * 
	 * @return {@literal true} if a topic criteria exists
	 */
	default boolean hasTopicCriteria() {
		String id = getTopic();
		return (id != null);
	}

	/**
	 * Get an array of topics.
	 * 
	 * @return array of topics (may be {@literal null})
	 */
	String[] getTopics();

	/**
	 * Get the first topic.
	 * 
	 * <p>
	 * This returns the first available topic from the {@link #getTopics()}
	 * array, or {@literal null} if not available.
	 * </p>
	 * 
	 * @return the topic, or {@literal null} if not available
	 */
	default String getTopic() {
		String[] ids = getTopics();
		return (ids != null && ids.length > 0 ? ids[0] : null);
	}

}
