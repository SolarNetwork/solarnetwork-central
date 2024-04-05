/* ==================================================================
 * BasicUserMetadataFilter.java - 5/04/2024 9:31:54 am
 * 
 * Copyright 2024 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dao;

import java.util.Arrays;
import net.solarnetwork.central.common.dao.BasicCoreCriteria;
import net.solarnetwork.central.common.dao.TagCriteria;
import net.solarnetwork.central.domain.UserMetadataFilter;
import net.solarnetwork.dao.PaginationCriteria;

/**
 * Basic implementation of {@link UserMetadataFilter}.
 * 
 * @author matt
 * @version 1.0
 */
public class BasicUserMetadataFilter extends BasicCoreCriteria implements UserMetadataFilter {

	private String[] tags;

	/**
	 * Constructor.
	 */
	public BasicUserMetadataFilter() {
		super();
	}

	/**
	 * Constructor.
	 * 
	 * @param criteria
	 */
	public BasicUserMetadataFilter(PaginationCriteria criteria) {
		super(criteria);
	}

	@Override
	public BasicUserMetadataFilter clone() {
		return (BasicUserMetadataFilter) super.clone();
	}

	@Override
	public void copyFrom(PaginationCriteria criteria) {
		super.copyFrom(criteria);
		if ( criteria instanceof TagCriteria c ) {
			setTags(c.getTags());
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(tags);
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
		if ( !(obj instanceof BasicUserMetadataFilter) ) {
			return false;
		}
		BasicUserMetadataFilter other = (BasicUserMetadataFilter) obj;
		return Arrays.equals(tags, other.tags);
	}

	@Override
	public String[] getTags() {
		return tags;
	}

	/**
	 * Set a tag.
	 * 
	 * @param tag
	 *        the tag to set
	 */
	public void setTag(String tag) {
		setTags(tag != null ? new String[] { tag } : null);
	}

	/**
	 * Set the tags.
	 * 
	 * @param tags
	 *        the tags to set
	 */
	public void setTags(String[] tags) {
		this.tags = tags;
	}

}
