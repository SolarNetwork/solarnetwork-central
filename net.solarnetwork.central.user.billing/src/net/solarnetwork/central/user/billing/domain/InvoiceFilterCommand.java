/* ==================================================================
 * InvoiceFilterCommand.java - 25/08/2017 3:03:31 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.support.FilterSupport;
import net.solarnetwork.central.support.MutableSortDescriptor;

/**
 * Filter support for invoice actions.
 * 
 * @author matt
 * @version 1.0
 */
public class InvoiceFilterCommand extends FilterSupport implements InvoiceFilter {

	private static final long serialVersionUID = -6127206654609364990L;

	private List<MutableSortDescriptor> sorts;
	private Integer offset = 0;
	private Integer max;
	private Boolean unpaid;

	/**
	 * Get the mutable sort descriptors.
	 * 
	 * @return the sort descriptors, or {@literal null}
	 */
	public List<MutableSortDescriptor> getSorts() {
		return sorts;
	}

	/**
	 * Set the mutable sort descriptors.
	 * 
	 * @param sorts
	 *        the sort descriptors to se
	 */
	public void setSorts(List<MutableSortDescriptor> sorts) {
		this.sorts = sorts;
	}

	/**
	 * Get the sort descriptors.
	 * 
	 * <p>
	 * This returns a copy of the {@code sorts} list or an empty list if that is
	 * {@literal null}.
	 * </p>
	 * 
	 * @return the sort descriptors, never {@code null}
	 */
	public List<SortDescriptor> getSortDescriptors() {
		if ( sorts == null ) {
			return Collections.emptyList();
		}
		return new ArrayList<SortDescriptor>(sorts);
	}

	/**
	 * Get the result starting offset.
	 * 
	 * @return the starting offset, or {@literal null} for no offset
	 */
	public Integer getOffset() {
		return offset;
	}

	/**
	 * Set the result starting offset.
	 * 
	 * @param offset
	 *        the offset to set
	 */
	public void setOffset(Integer offset) {
		this.offset = offset;
	}

	/**
	 * Get the maximum desired results.
	 * 
	 * @return the maximum desired result count
	 */
	public Integer getMax() {
		return max;
	}

	/**
	 * Set the maximum desired results.
	 * 
	 * @param max
	 *        the maximum desired result count, or {@literal null} for no limit
	 */
	public void setMax(Integer max) {
		this.max = max;
	}

	@Override
	public Boolean getUnpaid() {
		return unpaid;
	}

	/**
	 * Set the unpaid criteria.
	 * 
	 * @param unpaid
	 *        the unpaid value to limit the results to
	 */
	public void setUnpaid(Boolean unpaid) {
		this.unpaid = unpaid;
	}

}
