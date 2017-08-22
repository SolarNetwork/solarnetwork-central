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

package net.solarnetwork.central.user.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.support.FilterSupport;
import net.solarnetwork.central.support.MutableSortDescriptor;
import net.solarnetwork.util.JsonUtils;

/**
 * Filter support for user actions.
 * 
 * @author matt
 * @version 1.1
 * @since 1.23
 */
public class UserFilterCommand extends FilterSupport implements UserMetadataFilter, UserFilter {

	private static final long serialVersionUID = -7815908932742535345L;

	private List<MutableSortDescriptor> sorts;
	private Integer offset = 0;
	private Integer max;
	private String email;
	private Map<String, Object> billingData;

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
		return new ArrayList<SortDescriptor>(sorts);
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
	 * Get the billing data criteria.
	 * 
	 * @return the billing data criteria
	 * @since 1.1
	 */
	@Override
	public Map<String, Object> getBillingData() {
		return billingData;
	}

	/**
	 * Set the billing data criteria.
	 * 
	 * @param billingData
	 *        the billing data criteria to set
	 * @since 1.1
	 */
	public void setBillingData(Map<String, Object> billingData) {
		this.billingData = billingData;
	}

	/**
	 * Get the billing data criteria as a JSON string.
	 * 
	 * @return the billing data criteria, as JSON
	 * @since 1.1
	 */
	public String getBillingDataJson() {
		return JsonUtils.getJSONString(this.billingData, null);
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
		result = prime * result + ((billingData == null) ? 0 : billingData.hashCode());
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
		if ( billingData == null ) {
			if ( other.billingData != null ) {
				return false;
			}
		} else if ( !billingData.equals(other.billingData) ) {
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
