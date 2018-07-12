/* ==================================================================
 * DelegatingAuditDatumBiz.java - 12/07/2018 4:18:08 PM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.support;

import java.util.List;
import net.solarnetwork.central.datum.biz.AuditDatumBiz;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.AuditDatumRecordCounts;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;

/**
 * Implementation of {@link AuditDatumBiz} that delegates to another
 * {@link AuditDatumBiz}, designed primarily for use with AOP.
 * 
 * @author matt
 * @version 1.0
 */
public class DelegatingAuditDatumBiz implements AuditDatumBiz {

	private final AuditDatumBiz delegate;

	/**
	 * Construct with a delegate.
	 * 
	 * @param delegate
	 *        the delegate
	 */
	public DelegatingAuditDatumBiz(AuditDatumBiz delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public FilterResults<AuditDatumRecordCounts> findFilteredAuditRecordCounts(
			AggregateGeneralNodeDatumFilter filter, List<SortDescriptor> sortDescriptors, Integer offset,
			Integer max) {
		return delegate.findFilteredAuditRecordCounts(filter, sortDescriptors, offset, max);
	}

}
