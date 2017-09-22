/* ==================================================================
 * DelegatingBillingBiz.java - 25/08/2017 3:15:14 PM
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

package net.solarnetwork.central.user.billing.support;

import java.util.List;
import java.util.Locale;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SortDescriptor;
import net.solarnetwork.central.user.billing.biz.BillingBiz;
import net.solarnetwork.central.user.billing.biz.BillingSystem;
import net.solarnetwork.central.user.billing.domain.Invoice;
import net.solarnetwork.central.user.billing.domain.InvoiceFilter;
import net.solarnetwork.central.user.billing.domain.InvoiceMatch;

/**
 * Delegating implementation of {@link BillingBiz}, mostly to help with AOP.
 * 
 * @author matt
 * @version 1.1
 */
public class DelegatingBillingBiz implements BillingBiz {

	private final BillingBiz delegate;

	/**
	 * Constructor.
	 * 
	 * @param delegate
	 *        the delgate instance
	 */
	public DelegatingBillingBiz(BillingBiz delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public BillingSystem billingSystemForUser(Long userId) {
		return delegate.billingSystemForUser(userId);
	}

	@Override
	public FilterResults<InvoiceMatch> findFilteredInvoices(InvoiceFilter filter,
			List<SortDescriptor> sortDescriptors, Integer offset, Integer max) {
		return delegate.findFilteredInvoices(filter, sortDescriptors, offset, max);
	}

	@Override
	public Invoice getInvoice(Long userId, String invoiceId, Locale locale) {
		return delegate.getInvoice(userId, invoiceId, locale);
	}

	@Override
	public Resource renderInvoice(Long userId, String invoiceId, MimeType outputType, Locale locale) {
		return delegate.renderInvoice(userId, invoiceId, outputType, locale);
	}

}
