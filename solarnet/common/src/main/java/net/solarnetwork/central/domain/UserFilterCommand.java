/* ==================================================================
 * UserFilterCommand.java - 11/11/2016 5:21:29 PM
 * 
 * Copyright 2007-2016 SolarNetwork.net Dev Team
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.dao.BasicUserMetadataFilter;
import net.solarnetwork.central.support.FilterSupport;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.domain.MutableSortDescriptor;
import net.solarnetwork.domain.SortDescriptor;

/**
 * Filter support for user actions.
 * 
 * @author matt
 * @version 2.1
 * @since 1.23
 */
public class UserFilterCommand extends FilterSupport implements UserFilter {

	private static final long serialVersionUID = 915646548230356302L;

	private List<MutableSortDescriptor> sorts;
	private Integer offset = 0;
	private Integer max;
	private String email;
	private Map<String, Object> internalData;

	/**
	 * Convert to a {@link UserMetadataFilter}.
	 * 
	 * @return the filter
	 */
	public UserMetadataFilter toUserMetadataFilter() {
		BasicUserMetadataFilter f = new BasicUserMetadataFilter();
		f.setUserIds(getUserIds());
		f.setTags(getTags());
		if ( sorts != null ) {
			f.setSorts(getSortDescriptors());
		}
		f.setMax(max);
		f.setOffset(offset);
		return f;
	}

	public List<MutableSortDescriptor> getSorts() {
		return sorts;
	}

	public void setSorts(List<MutableSortDescriptor> sorts) {
		this.sorts = sorts;
	}

	public List<SortDescriptor> getSortDescriptors() {
		if ( sorts == null ) {
			return Collections.emptyList();
		}
		return new ArrayList<>(sorts);
	}

	public Integer getOffset() {
		return offset;
	}

	public void setOffset(Integer offset) {
		this.offset = offset;
	}

	public Integer getMax() {
		return max;
	}

	public void setMax(Integer max) {
		this.max = max;
	}

	/**
	 * Get the email criteria.
	 * 
	 * @return the email criteria
	 * @since 1.1
	 */
	@Override
	public String getEmail() {
		return email;
	}

	/**
	 * Set the email criteria.
	 * 
	 * @param email
	 *        the email to set
	 * @since 1.1
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Get the internal data criteria.
	 * 
	 * @return the internal data criteria
	 * @since 1.1
	 */
	@Override
	public Map<String, Object> getInternalData() {
		return internalData;
	}

	/**
	 * Set the internal data criteria.
	 * 
	 * @param internalData
	 *        the internal data criteria to set
	 * @since 1.1
	 */
	public void setInternalData(Map<String, Object> internalData) {
		this.internalData = internalData;
	}

	/**
	 * Get the internal data criteria as a JSON string.
	 * 
	 * @return the internal data criteria, as JSON
	 * @since 1.1
	 */
	public String getInternalDataJson() {
		return JsonUtils.getJSONString(this.internalData, null);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.1
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((internalData == null) ? 0 : internalData.hashCode());
		result = prime * result + ((email == null) ? 0 : email.hashCode());
		result = prime * result + ((max == null) ? 0 : max.hashCode());
		result = prime * result + ((offset == null) ? 0 : offset.hashCode());
		result = prime * result + ((sorts == null) ? 0 : sorts.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @since 1.1
	 */
	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( !super.equals(obj) ) {
			return false;
		}
		if ( !(obj instanceof UserFilterCommand) ) {
			return false;
		}
		UserFilterCommand other = (UserFilterCommand) obj;
		if ( internalData == null ) {
			if ( other.internalData != null ) {
				return false;
			}
		} else if ( !internalData.equals(other.internalData) ) {
			return false;
		}
		if ( email == null ) {
			if ( other.email != null ) {
				return false;
			}
		} else if ( !email.equals(other.email) ) {
			return false;
		}
		if ( max == null ) {
			if ( other.max != null ) {
				return false;
			}
		} else if ( !max.equals(other.max) ) {
			return false;
		}
		if ( offset == null ) {
			if ( other.offset != null ) {
				return false;
			}
		} else if ( !offset.equals(other.offset) ) {
			return false;
		}
		if ( sorts == null ) {
			if ( other.sorts != null ) {
				return false;
			}
		} else if ( !sorts.equals(other.sorts) ) {
			return false;
		}
		return true;
	}

}
