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
 * @version 1.0
 */
public class BasicUserSecretFilter extends BasicCoreCriteria
		implements UserSecretFilter, UserKeyPairFilter {

	private String[] topicIds;
	private String[] keys;

	/**
	 * Constructor.
	 */
	public BasicUserSecretFilter() {
		super();
	}

	/**
	 * @param criteria
	 */
	public BasicUserSecretFilter(PaginationCriteria criteria) {
		super(criteria);
	}

	@Override
	public void copyFrom(PaginationCriteria criteria) {
		super.copyFrom(criteria);
		if ( criteria instanceof BasicUserSecretFilter c ) {
			setTopicIds(c.getTopicIds());
			setKeys(c.getKeys());
		} else {
			if ( criteria instanceof TopicCriteria c ) {
				setTopicIds(c.getTopicIds());
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
		result = prime * result + Arrays.hashCode(topicIds);
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
		if ( !(obj instanceof BasicUserSecretFilter) ) {
			return false;
		}
		BasicUserSecretFilter other = (BasicUserSecretFilter) obj;
		return Arrays.equals(topicIds, topicIds) && Arrays.equals(keys, other.keys);
	}

	@Override
	public BasicUserSecretFilter clone() {
		return (BasicUserSecretFilter) super.clone();
	}

	/**
	 * Set a single topic ID.
	 * 
	 * <p>
	 * This will completely replace the configured topic IDs.
	 * </p>
	 * 
	 * @param topicId
	 *        the topic ID to set
	 */
	public void setTopicId(String topicId) {
		setTopicIds(topicId != null ? new String[] { topicId } : null);
	}

	/**
	 * Get the topic IDs.
	 * 
	 * @return the topic IDs
	 */
	@Override
	public String[] getTopicIds() {
		return topicIds;
	}

	/**
	 * Set the topic IDS.
	 * 
	 * @param topicIds
	 *        the topic IDs to set
	 */
	public void setTopicIds(String[] topicIds) {
		this.topicIds = topicIds;
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
