/* ==================================================================
 * BaseFilterSupport.java - 29/04/2022 4:35:38 pm
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

package net.solarnetwork.central.support;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.solarnetwork.central.domain.Filter;
import net.solarnetwork.central.domain.MetadataFilter;
import net.solarnetwork.central.domain.TagFilter;

/**
 * Base common filter support.
 * 
 * @author matt
 * @version 1.0
 * @since 1.2
 */
public class BaseFilterSupport implements Filter, Serializable, MetadataFilter, TagFilter {

	private static final long serialVersionUID = 4146553587756173455L;

	private Long[] userIds;
	private String[] tags;
	private String metadataFilter;

	@Override
	public Map<String, ?> getFilter() {
		Map<String, Object> filter = new LinkedHashMap<>(8);
		if ( userIds != null ) {
			filter.put("userIds", userIds);
		}
		if ( tags != null ) {
			filter.put("tags", tags);
		}
		if ( metadataFilter != null && !metadataFilter.trim().isEmpty() ) {
			filter.put("metadataFilter", metadataFilter);
		}
		return filter;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("FilterSupport{");
		if ( userIds != null ) {
			builder.append("userIds=");
			builder.append(Arrays.toString(userIds));
			builder.append(", ");
		}
		if ( tags != null ) {
			builder.append("tags=");
			builder.append(Arrays.toString(tags));
			builder.append(", ");
		}
		if ( metadataFilter != null ) {
			builder.append("metadataFilter=");
			builder.append(metadataFilter);
		}
		builder.append("}");
		return builder.toString();
	}

	@JsonIgnore
	@Override
	public String getTag() {
		return (this.tags == null || this.tags.length < 1 ? null : this.tags[0]);
	}

	/**
	 * Set a single tag.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single tag at a
	 * time. The tag is still stored on the {@code tags} array, just as the
	 * first value. Calling this method replaces any existing {@code tags} value
	 * with a new array containing just the tag passed into this method.
	 * </p>
	 * 
	 * @param tag
	 *        the tag
	 */
	@JsonSetter
	public void setTag(String tag) {
		this.tags = (tag == null ? null : new String[] { tag });
	}

	@Override
	public String[] getTags() {
		return tags;
	}

	/**
	 * Set a list of tags to filter on.
	 * 
	 * @param tags
	 *        the tags to filter on
	 */
	public void setTags(String[] tags) {
		this.tags = tags;
	}

	/**
	 * Set a single user ID.
	 * 
	 * <p>
	 * This is a convenience method for requests that use a single user ID at a
	 * time. The user ID is still stored on the {@code userIds} array, just as
	 * the first value. Calling this method replaces any existing
	 * {@code userIds} value with a new array containing just the ID passed into
	 * this method.
	 * </p>
	 * 
	 * @param userId
	 *        the ID of the user
	 */
	@JsonSetter
	public void setUserId(Long userId) {
		this.userIds = (userId == null ? null : new Long[] { userId });
	}

	/**
	 * Get the first user ID.
	 * 
	 * <p>
	 * This returns the first available user ID from the {@code userIds} array,
	 * or {@literal null} if not available.
	 * </p>
	 * 
	 * @return the first user ID, or {@literal null}
	 */
	@JsonIgnore
	public Long getUserId() {
		return (this.userIds == null || this.userIds.length < 1 ? null : this.userIds[0]);
	}

	/**
	 * Get all user IDs to filter on.
	 * 
	 * @return The user IDs, or {@literal null}.
	 */
	public Long[] getUserIds() {
		return userIds;
	}

	/**
	 * Set a list of user IDs to filter on.
	 * 
	 * @param userIds
	 *        The user IDs to filter on.
	 */
	public void setUserIds(Long[] userIds) {
		this.userIds = userIds;
	}

	@Override
	public String getMetadataFilter() {
		return metadataFilter;
	}

	/**
	 * Set a metadata search filter, in LDAP search filter syntax.
	 * 
	 * @param metadataFilter
	 *        the metadata filter to use, or {@literal null}
	 */
	public void setMetadataFilter(String metadataFilter) {
		this.metadataFilter = metadataFilter;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(tags);
		result = prime * result + Arrays.hashCode(userIds);
		result = prime * result + Objects.hash(metadataFilter);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !(obj instanceof BaseFilterSupport) ) {
			return false;
		}
		BaseFilterSupport other = (BaseFilterSupport) obj;
		return Arrays.equals(tags, other.tags) && Arrays.equals(userIds, other.userIds)
				&& Objects.equals(metadataFilter, other.metadataFilter);
	}

}
