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

import org.jspecify.annotations.Nullable;

/**
 * Criteria API for topical entities.
 * 
 * @author matt
 * @version 1.1
 */
public interface TopicCriteria {

	/**
	 * Test if any topic criteria exists.
	 * 
	 * @return {@code true} if a topic criteria exists
	 */
	default boolean hasTopicCriteria() {
		String id = getTopic();
		return (id != null);
	}

	/**
	 * Get an array of topics.
	 * 
	 * @return array of topics (may be {@code null})
	 */
	String @Nullable [] getTopics();

	/**
	 * Get the first topic.
	 * 
	 * <p>
	 * This returns the first available topic from the {@link #getTopics()}
	 * array, or {@code null} if not available.
	 * </p>
	 * 
	 * @return the topic, or {@code null} if not available
	 */
	default @Nullable String getTopic() {
		String[] ids = getTopics();
		return (ids != null && ids.length > 0 ? ids[0] : null);
	}

	/**
	 * Get the first topic.
	 * 
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasTopicCriteria()} returns {@code true}, to avoid nullness
	 * warnings.
	 * </p>
	 * 
	 * @return the first topic (presumed non-null)
	 * @since 1.1
	 */
	@SuppressWarnings("NullAway")
	default String topic() {
		return getTopic();
	}

	/**
	 * Get an array of topics.
	 *
	 * <p>
	 * This method is designed to be used after a call to
	 * {@link #hasTopicCriteria()} returns {@code true}, to avoid nullness
	 * warnings.
	 * </p>
	 *
	 * @return array of topics (presumed non-null)
	 * @since 1.1
	 */
	@SuppressWarnings("NullAway")
	default String[] topics() {
		return getTopics();
	}

}
