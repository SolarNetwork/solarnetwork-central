/* ==================================================================
 * BillingSystem.java - 25/08/2017 3:10:36 PM
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
import net.solarnetwork.central.user.billing.domain.BillingSystemInfo;
import net.solarnetwork.central.user.billing.domain.ConsolidatedInvoicePreviewStatus;
import net.solarnetwork.central.user.billing.domain.Invoice;
import net.solarnetwork.central.user.billing.domain.InvoiceFilter;
import net.solarnetwork.central.user.billing.domain.InvoiceGenerationOptions;
import net.solarnetwork.central.user.billing.domain.InvoiceMatch;
import net.solarnetwork.central.user.billing.domain.InvoicePreviewState;
import net.solarnetwork.central.user.billing.domain.InvoicePreviewStatus;
import net.solarnetwork.central.user.billing.domain.NamedCostTiers;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * API for interacting with a billing system.
 * 
 * @author matt
 * @version 1.5
 */
public interface BillingSystem {

	/**
	 * Get a unique system key for the accounting functions of this system.
	 * 
	 * @return a unique key
	 */
	String getAccountingSystemKey();

	/**
	 * Test if an accounting key is supported by this system.
	 * 
	 * @param key
	 *        the key to test
	 * @return {@literal true} if the key is supported
	 */
	boolean supportsAccountingSystemKey(String key);

	/**
	 * Get information about this system.
	 * 
	 * @param locale
	 *        the desired locale of the information, or {@code null} for the
	 *        default locale
	 * @return the info
	 */
	BillingSystemInfo getInfo(@Nullable Locale locale);

	/**
	 * Get all available named cost tiers.
	 * 
	 * @param locale
	 *        the desired locale, or {@code null} for the default locale
	 * @return the named cost tiers, or {@code null} if none supported
	 * @since 1.2
	 */
	@Nullable
	List<? extends NamedCostTiers> namedCostTiers(@Nullable Locale locale);

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
	 * Get an invoice by ID.
	 * 
	 * @param userId
	 *        the ID of the user to get the invoice for
	 * @param invoiceId
	 *        the ID of the invoice to get
	 * @param locale
	 *        a locale to show the invoice details in
	 * @return the invoice, or {@code null} if not available
	 */
	@Nullable
	Invoice getInvoice(Long userId, String invoiceId, Locale locale);

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
	 * <p>
	 * The filter's user is the primary account user. This implementation
	 * returns no results, for billing systems that do not consolidate
	 * invoices.
	 * </p>
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
	 * @since 1.4
	 */
	default FilterResults<InvoiceMatch, String> findFilteredConsolidatedInvoices(
			InvoiceFilter filter, @Nullable List<SortDescriptor> sortDescriptors,
			@Nullable Long offset, @Nullable Integer max) {
		return new BasicFilterResults<>(null, 0L, 0L, 0);
	}

	/**
	 * Get a consolidated invoice billed to a user, with the invoices it is
	 * made of.
	 *
	 * <p>
	 * This implementation returns {@code null}.
	 * </p>
	 *
	 * @param userId
	 *        the user ID of the primary account
	 * @param invoiceId
	 *        the consolidated invoice ID
	 * @param locale
	 *        the desired locale
	 * @return the consolidated invoice, or {@code null} if not available
	 * @since 1.4
	 */
	default @Nullable Invoice getConsolidatedInvoice(Long userId, String invoiceId,
			Locale locale) {
		return null;
	}

	/**
	 * Render a consolidated invoice billed to a user.
	 *
	 * <p>
	 * This implementation returns {@code null}.
	 * </p>
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
	 * @since 1.4
	 */
	default @Nullable Resource renderConsolidatedInvoice(Long userId, String invoiceId,
			MimeType outputType, Locale locale) {
		return null;
	}

	/**
	 * Get a preview of the consolidated invoice for the invoice consolidation
	 * group a user is the primary account of.
	 *
	 * <p>
	 * This implementation returns {@code null}.
	 * </p>
	 *
	 * @param userId
	 *        the user ID of the primary account
	 * @param options
	 *        the invoice generation options
	 * @param locale
	 *        the desired locale
	 * @return the preview consolidated invoice, or {@code null} if the user is
	 *         not the primary account of a valid group
	 * @since 1.4
	 */
	default @Nullable Invoice getPreviewConsolidatedInvoice(Long userId,
			InvoiceGenerationOptions options, Locale locale) {
		return null;
	}

	/**
	 * Render a preview of the consolidated invoice for the invoice
	 * consolidation group a user is the primary account of.
	 *
	 * <p>
	 * This implementation returns {@code null}.
	 * </p>
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
	 * @since 1.4
	 */
	default @Nullable Resource previewConsolidatedInvoice(Long userId,
			InvoiceGenerationOptions options, MimeType outputType, Locale locale) {
		return null;
	}

	/**
	 * Get the status of a preview invoice for the current billing cycle,
	 * without waiting for the preview to be generated.
	 *
	 * <p>
	 * A system that generates previews in the background returns the preview
	 * state, starting to generate the preview if needed. A caller asks again
	 * later until the state is {@code Done} or {@code Failed}.
	 * </p>
	 *
	 * <p>
	 * This implementation generates the preview with
	 * {@link #getPreviewInvoice(Long, InvoiceGenerationOptions, Locale)} and
	 * returns it as {@code Done}.
	 * </p>
	 *
	 * @param userId
	 *        the user ID to get the invoice for
	 * @param options
	 *        the invoice generation options
	 * @param locale
	 *        the desired locale
	 * @return the preview status, never {@code null}
	 * @since 1.5
	 */
	default InvoicePreviewStatus getPreviewInvoiceStatus(Long userId,
			InvoiceGenerationOptions options, Locale locale) {
		return new InvoicePreviewStatus(InvoicePreviewState.Done,
				getPreviewInvoice(userId, options, locale));
	}

	/**
	 * Get the status of a preview consolidated invoice for the current billing
	 * cycle, for the invoice consolidation group a user is the primary account
	 * of, without waiting for the preview to be generated.
	 *
	 * <p>
	 * A system that generates previews in the background returns the preview
	 * state of each account of the group, starting to generate the next one if
	 * needed. A caller asks again later until the state is {@code Done} or
	 * {@code Failed}.
	 * </p>
	 *
	 * <p>
	 * This implementation returns {@code null}.
	 * </p>
	 *
	 * @param userId
	 *        the user ID of the primary account
	 * @param options
	 *        the invoice generation options
	 * @param locale
	 *        the desired locale
	 * @return the preview status, or {@code null} if the user is not the
	 *         primary account of a valid group
	 * @since 1.5
	 */
	default @Nullable ConsolidatedInvoicePreviewStatus getPreviewConsolidatedInvoiceStatus(
			Long userId, InvoiceGenerationOptions options, Locale locale) {
		return null;
	}

}
