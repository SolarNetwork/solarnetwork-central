/* ==================================================================
 * BillingBiz.java - 25/08/2017 2:26:46 PM
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

package net.solarnetwork.central.user.billing.biz;

import java.util.List;
import java.util.Locale;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;
import net.solarnetwork.central.user.billing.domain.Invoice;
import net.solarnetwork.central.user.billing.domain.InvoiceFilter;
import net.solarnetwork.central.user.billing.domain.InvoiceGenerationOptions;
import net.solarnetwork.central.user.billing.domain.InvoiceMatch;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * API for billing business logic.
 * 
 * @author matt
 * @version 1.3
 */
public interface BillingBiz {

	/**
	 * Get the billing system configured for a given user.
	 * 
	 * @param userId
	 *        the ID of the user to get the billing system for
	 * @return the billing system, or {@literal null} if no system is configured
	 *         or available
	 */
	BillingSystem billingSystemForUser(Long userId);

	/**
	 * Get the billing system associated with a given accounting system key.
	 * 
	 * @param key
	 *        the key of the system to get
	 * @return the system, or {@literal null} if no system available for the
	 *         given {@code key}
	 * @since 1.2
	 */
	BillingSystem billingSystemForKey(String key);

	/**
	 * Get an invoice by ID.
	 * 
	 * @param userId
	 *        the user ID to get the invoice for
	 * @param invoiceId
	 *        the invoice ID to get
	 * @param locale
	 *        the desired output locale
	 * @return the invoice, or {@literal null} if not available
	 */
	Invoice getInvoice(Long userId, String invoiceId, Locale locale);

	/**
	 * Search for invoices.
	 * 
	 * @param filter
	 *        the query filter
	 * @param sortDescriptors
	 *        the optional sort descriptors
	 * @param offset
	 *        an optional result offset
	 * @param max
	 *        an optional maximum number of returned results
	 * @return the results, never {@literal null}
	 */
	FilterResults<InvoiceMatch, String> findFilteredInvoices(InvoiceFilter filter,
			List<SortDescriptor> sortDescriptors, Long offset, Integer max);

	/**
	 * Render an invoice.
	 * 
	 * @param userId
	 *        the ID of the user to render the invoice for
	 * @param invoiceId
	 *        the ID of the invoice to render
	 * @param outputType
	 *        the desired output type, e.g. {@literal text/html}
	 * @param locale
	 *        the desired output locale
	 * @return a resource with the result data, or {@literal null} if the
	 *         invoice is not available
	 * @throws IllegalArgumentException
	 *         if {@code outputType} is not supported
	 */
	Resource renderInvoice(Long userId, String invoiceId, MimeType outputType, Locale locale);

	/**
	 * Get a preview invoice for the next billing cycle's invoice.
	 * 
	 * @param userId
	 *        the user ID to get the invoice for
	 * @param options
	 *        the options
	 * @param locale
	 *        the desired output locale
	 * @return the invoice, or {@literal null} if not available
	 * @since 1.1
	 */
	Invoice getPreviewInvoice(Long userId, InvoiceGenerationOptions options, Locale locale);

	/**
	 * Preview the next billing cycle's invoice.
	 * 
	 * @param userId
	 *        the ID of the user to render the invoice for
	 * @param options
	 *        the options
	 * @param outputType
	 *        the desired output type, e.g. {@literal text/html}
	 * @param locale
	 *        the desired output locale
	 * @return a resource with the result data, or {@literal null} if the
	 *         invoice is not available
	 * @throws IllegalArgumentException
	 *         if {@code outputType} is not supported
	 * @since 1.1
	 */
	Resource previewInvoice(Long userId, InvoiceGenerationOptions options, MimeType outputType,
			Locale locale);

}
