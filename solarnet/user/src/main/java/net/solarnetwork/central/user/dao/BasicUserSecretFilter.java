/* ==================================================================
 * BasicUserSecretFilter.java - 21/03/2025 5:47:36 pm
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

package net.solarnetwork.central.user.dao;

import java.util.Arrays;
import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.central.common.dao.KeyCriteria;
import net.solarnetwork.central.common.dao.TopicCriteria;
import net.solarnetwork.dao.PaginationCriteria;

/**
 * Basic implementation of {@link UserSecretFilter}.
 * 
 * @author matt
 * @version 1.1
 */
public class BasicUserSecretFilter extends BasicCoreCriteria
		implements UserSecretFilter, UserKeyPairFilter {

	private String[] topics;
	private String[] keys;

	/**
	 * Constructor.
	 */
	public BasicUserSecretFilter() {
		super();
	}

	/**
	 * Copy constructor.
	 * 
	 * @param criteria
	 *        the criteria to copy from
	 */
	public BasicUserSecretFilter(PaginationCriteria criteria) {
		super(criteria);
	}

	@Override
	public void copyFrom(PaginationCriteria criteria) {
		super.copyFrom(criteria);
		if ( criteria instanceof BasicUserSecretFilter c ) {
			setTopics(c.getTopics());
			setKeys(c.getKeys());
		} else {
			if ( criteria instanceof TopicCriteria c ) {
				setTopics(c.getTopics());
			}
			if ( criteria instanceof KeyCriteria c ) {
				setKeys(c.getKeys());
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(topics);
		result = prime * result + Arrays.hashCode(keys);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) ) {
			return false;
		}
		if ( !(obj instanceof BasicUserSecretFilter other) ) {
			return false;
		}
		return Arrays.equals(topics, other.topics) && Arrays.equals(keys, other.keys);
	}

	@Override
	public BasicUserSecretFilter clone() {
		return (BasicUserSecretFilter) super.clone();
	}

	@Override
	public boolean hasAnyCriteria() {
		// @formatter:off
		return     super.hasAnyCriteria()
				|| (keys != null && keys.length > 0)
				|| (topics != null && topics.length > 0)
				;
		// @formatter:on
	}

	/**
	 * Set a single topic.
	 * 
	 * <p>
	 * This will completely replace the configured topics.
	 * </p>
	 * 
	 * @param topic
	 *        the topic to set
	 */
	public void setTopic(String topic) {
		setTopics(topic != null ? new String[] { topic } : null);
	}

	/**
	 * Get the topics.
	 * 
	 * @return the topics
	 */
	@Override
	public String[] getTopics() {
		return topics;
	}

	/**
	 * Set the topics.
	 * 
	 * @param topics
	 *        the topics to set
	 */
	public void setTopics(String[] topics) {
		this.topics = topics;
	}

	/**
	 * Set a single key.
	 * 
	 * <p>
	 * This will completely replace the configured keys.
	 * </p>
	 * 
	 * @param key
	 *        the key to set
	 */
	public void setKey(String key) {
		setKeys(key != null ? new String[] { key } : null);
	}

	@Override
	public String[] getKeys() {
		return keys;
	}

	/**
	 * Set the key criteria.
	 * 
	 * @param keys
	 *        the keys to set
	 */
	public void setKeys(String[] keys) {
		this.keys = keys;
	}

}
