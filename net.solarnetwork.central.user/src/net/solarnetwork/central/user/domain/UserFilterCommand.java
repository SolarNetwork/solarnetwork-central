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

}
