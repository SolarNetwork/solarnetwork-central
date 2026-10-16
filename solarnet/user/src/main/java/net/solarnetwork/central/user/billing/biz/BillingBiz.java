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
import org.jspecify.annotations.Nullable;
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
 * @version 1.5
 */
public interface BillingBiz {

	/**
	 * Get a list of all available {@link BillingSystem} implementations.
	 *
	 * @return the billing systems, never {@code null}
	 * @since 1.4
	 */
	Iterable<BillingSystem> availableBillingSystems();

	/**
	 * Get the {@link BillingSystem} to use by default.
	 * 
	 * @return the default billing system, or {@code null} if no default is
	 *         available
	 * @since 1.4
	 */
	default @Nullable BillingSystem defaultBillingSystem() {
		var systems = availableBillingSystems();
		if ( systems == null ) {
			return null;
		}
		var itr = systems.iterator();
		return (itr.hasNext() ? itr.next() : null);
	}

	/**
	 * Get the billing system configured for a given user.
	 * 
	 * @param userId
	 *        the ID of the user to get the billing system for
	 * @return the billing system, or {@code null} if no system is configured or
	 *         available
	 */
	@Nullable
	BillingSystem billingSystemForUser(Long userId);

	/**
	 * Get the billing system associated with a given accounting system key.
	 * 
	 * @param key
	 *        the key of the system to get
	 * @return the system, or {@code null} if no system available for the given
	 *         {@code key}
	 * @since 1.2
	 */
	@Nullable
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
	 * @return the invoice, or {@code null} if not available
	 */
	@Nullable
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
	 * @return the results, never {@code null}
	 */
	FilterResults<InvoiceMatch, String> findFilteredInvoices(InvoiceFilter filter,
			@Nullable List<SortDescriptor> sortDescriptors, @Nullable Long offset,
			@Nullable Integer max);

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
	 * @return a resource with the result data, or {@code null} if the invoice
	 *         is not available
	 * @throws IllegalArgumentException
	 *         if {@code outputType} is not supported
	 */
	@Nullable
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
	 * @return the invoice, or {@code null} if not available
	 * @since 1.1
	 */
	@Nullable
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
	 * @return a resource with the result data, or {@code null} if the invoice
	 *         is not available
	 * @throws IllegalArgumentException
	 *         if {@code outputType} is not supported
	 * @since 1.1
	 */
	@Nullable
	Resource previewInvoice(Long userId, InvoiceGenerationOptions options, MimeType outputType,
			Locale locale);

	/**
	 * Search for consolidated invoices billed to a user.
	 *
	 * @param filter
	 *        the query filter, whose user is the primary account user
	 * @param sortDescriptors
	 *        the optional sort descriptors
	 * @param offset
	 *        an optional result offset
	 * @param max
	 *        an optional maximum number of returned results
	 * @return the results, never {@code null}
	 * @since 1.5
	 */
	FilterResults<InvoiceMatch, String> findFilteredConsolidatedInvoices(InvoiceFilter filter,
			@Nullable List<SortDescriptor> sortDescriptors, @Nullable Long offset,
			@Nullable Integer max);

	/**
	 * Get a consolidated invoice billed to a user, with the invoices it is
	 * made of.
	 *
	 * @param userId
	 *        the user ID of the primary account
	 * @param invoiceId
	 *        the consolidated invoice ID
	 * @param locale
	 *        the desired locale
	 * @return the consolidated invoice, or {@code null} if not available
	 * @since 1.5
	 */
	@Nullable
	Invoice getConsolidatedInvoice(Long userId, String invoiceId, Locale locale);

	/**
	 * Render a consolidated invoice billed to a user.
	 *
	 * @param userId
	 *        the user ID of the primary account
	 * @param invoiceId
	 *        the consolidated invoice ID
	 * @param outputType
	 *        the desired output type, e.g. {@literal text/html}
	 * @param locale
	 *        the desired output locale
	 * @return a resource with the result data, or {@code null} if the invoice
	 *         is not available
	 * @since 1.5
	 */
	@Nullable
	Resource renderConsolidatedInvoice(Long userId, String invoiceId, MimeType outputType,
			Locale locale);

	/**
	 * Get a preview of the consolidated invoice for the invoice consolidation
	 * group a user is the primary account of.
	 *
	 * @param userId
	 *        the user ID of the primary account
	 * @param options
	 *        the invoice generation options
	 * @param locale
	 *        the desired locale
	 * @return the preview consolidated invoice, or {@code null} if the user is
	 *         not the primary account of a valid group
	 * @since 1.5
	 */
	@Nullable
	Invoice getPreviewConsolidatedInvoice(Long userId, InvoiceGenerationOptions options,
			Locale locale);

	/**
	 * Render a preview of the consolidated invoice for the invoice
	 * consolidation group a user is the primary account of.
	 *
	 * @param userId
	 *        the user ID of the primary account
	 * @param options
	 *        the invoice generation options
	 * @param outputType
	 *        the desired output type, e.g. {@literal text/html}
	 * @param locale
	 *        the desired output locale
	 * @return a resource with the result data, or {@code null} if the user is
	 *         not the primary account of a valid group
	 * @since 1.5
	 */
	@Nullable
	Resource previewConsolidatedInvoice(Long userId, InvoiceGenerationOptions options,
			MimeType outputType, Locale locale);

}
