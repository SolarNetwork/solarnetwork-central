/* ==================================================================
 * SnfInvoicingSystem.java - 20/07/2020 9:26:30 AM
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.domain.UserLongPK;

/**
 * API for generating invoices for the {@link SnfBillingSystem}.
 *
 * @author matt
 * @version 1.1
 */
public interface SnfInvoicingSystem {

	/**
	 * API for invoice generation options.
	 */
	interface InvoiceGenerationOptions {

		/**
		 * Get the "dry run" flag.
		 *
		 * @return {@literal true} if an invoice should be generated but not
		 *         persisted nor delivered to the account holder
		 */
		boolean isDryRun();

		/**
		 * Get the "use account credit" flag.
		 *
		 * @return {@literal true} to use available account credit by adding a
		 *         credit item to the generated invoice
		 */
		boolean isUseAccountCredit();

	}

	/**
	 * Get the billing account for a given user.
	 *
	 * @param userId
	 *        the ID of the user to get the account for
	 * @return the account, or {@code null} if not available
	 */
	@Nullable
	Account accountForUser(Long userId);

	/**
	 * Get the latest invoice for a given account.
	 *
	 * @param accountId
	 *        the ID of the account to get the latest invoice for
	 * @return the latest available invoice, or {@code null} if none
	 *         available
	 */
	@Nullable
	SnfInvoice findLatestInvoiceForAccount(UserLongPK accountId);

	/**
	 * Generate a new invoice.
	 *
	 * @param userId
	 *        the user ID to generate an invoice for
	 * @param startDate
	 *        the desired invoice period starting date; will be interpreted in
	 *        the user's account's time zone
	 * @param endDate
	 *        the desired invoice period ending date; will be interpreted in the
	 *        user's account's time zone
	 * @param options
	 *        the invoice generation options
	 * @return the generated invoice, or {@code null} if no invoice is
	 *         necessary (i.e. no charges)
	 */
	@Nullable
	SnfInvoice generateInvoice(Long userId, LocalDate startDate, LocalDate endDate,
			InvoiceGenerationOptions options);

	/**
	 * Deliver an invoice via an account-specific delivery mechanism (such as
	 * email).
	 *
	 * @param invoiceId
	 *        the ID of the invoice to deliver
	 * @return {@literal true} if the invoice was delivered successfully
	 */
	boolean deliverInvoice(UserLongPK invoiceId);

	/**
	 * Get a {@link MessageSource} appropriate for a given invoice.
	 *
	 * @param invoice
	 *        the invoice to get the message source for
	 * @return the message source, never {@code null}
	 */
	MessageSource messageSourceForInvoice(SnfInvoice invoice);

	/**
	 * Get a {@link MessageSource} appropriate for a given date.
	 *
	 * @param date
	 *        the date to get the message source for
	 * @return the message source, never {@code null}
	 * @since 1.1
	 */
	MessageSource messageSourceForDate(Instant date);

	/**
	 * Render an invoice entity.
	 *
	 * <p>
	 * This is similar to
	 * {@link net.solarnetwork.central.user.billing.biz.BillingSystem#renderInvoice(Long, String, MimeType, Locale)}
	 * except it is meant to be used internally, when an invoice entity instance
	 * already exists.
	 * </p>
	 *
	 * @param invoice
	 *        the invoice to render
	 * @param outputType
	 *        the desired output type
	 * @param locale
	 *        the output locale
	 * @return a resource with the result data
	 * @throws IllegalArgumentException
	 *         if {@code outputType} is not supported
	 */
	Resource renderInvoice(SnfInvoice invoice, MimeType outputType, Locale locale);

}
