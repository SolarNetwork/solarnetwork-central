/* ==================================================================
 * SnfConsolidatedInvoiceFilter.java - 15/10/2026 8:52:40 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf.domain;

import java.time.LocalDate;
import org.jspecify.annotations.Nullable;
import net.solarnetwork.domain.SimplePagination;

/**
 * Query filter for {@link SnfConsolidatedInvoice} entities.
 *
 * @author matt
 * @version 1.0
 */
public class SnfConsolidatedInvoiceFilter extends SimplePagination {

	private @Nullable Long userId;
	private @Nullable Long accountId;
	private @Nullable Long invoiceId;
	private @Nullable LocalDate startDate;
	private @Nullable LocalDate endDate;
	private boolean unpaidOnly;

	/**
	 * Create a new filter for a primary account user.
	 *
	 * @param userId
	 *        the user ID of the primary account
	 * @return the filter
	 */
	public static SnfConsolidatedInvoiceFilter forUser(Long userId) {
		SnfConsolidatedInvoiceFilter f = new SnfConsolidatedInvoiceFilter();
		f.setUserId(userId);
		return f;
	}

	/**
	 * Create a new filter for a primary account.
	 *
	 * @param accountId
	 *        the ID of the primary account
	 * @return the filter
	 */
	public static SnfConsolidatedInvoiceFilter forAccount(Long accountId) {
		SnfConsolidatedInvoiceFilter f = new SnfConsolidatedInvoiceFilter();
		f.setAccountId(accountId);
		return f;
	}

	/**
	 * Create a new filter for the consolidated invoice of an account invoice.
	 *
	 * @param invoiceId
	 *        the ID of an account invoice
	 * @return the filter
	 */
	public static SnfConsolidatedInvoiceFilter forInvoice(Long invoiceId) {
		SnfConsolidatedInvoiceFilter f = new SnfConsolidatedInvoiceFilter();
		f.setInvoiceId(invoiceId);
		return f;
	}

	@Override
	public SnfConsolidatedInvoiceFilter clone() {
		return (SnfConsolidatedInvoiceFilter) super.clone();
	}

	/**
	 * Get the user ID of the primary account.
	 *
	 * @return the user ID
	 */
	public final @Nullable Long getUserId() {
		return userId;
	}

	/**
	 * Set the user ID of the primary account.
	 *
	 * @param userId
	 *        the user ID to set
	 */
	public final void setUserId(@Nullable Long userId) {
		this.userId = userId;
	}

	/**
	 * Get the ID of the primary account.
	 *
	 * @return the account ID
	 */
	public final @Nullable Long getAccountId() {
		return accountId;
	}

	/**
	 * Set the ID of the primary account.
	 *
	 * @param accountId
	 *        the account ID to set
	 */
	public final void setAccountId(@Nullable Long accountId) {
		this.accountId = accountId;
	}

	/**
	 * Get the ID of an account invoice the consolidated invoice must include.
	 *
	 * @return the account invoice ID
	 */
	public final @Nullable Long getInvoiceId() {
		return invoiceId;
	}

	/**
	 * Set the ID of an account invoice the consolidated invoice must include.
	 *
	 * @param invoiceId
	 *        the account invoice ID to set
	 */
	public final void setInvoiceId(@Nullable Long invoiceId) {
		this.invoiceId = invoiceId;
	}

	/**
	 * Get the minimum consolidated invoice start date.
	 *
	 * @return the start date (inclusive)
	 */
	public final @Nullable LocalDate getStartDate() {
		return startDate;
	}

	/**
	 * Set the minimum consolidated invoice start date.
	 *
	 * @param startDate
	 *        the start date to set (inclusive)
	 */
	public final void setStartDate(@Nullable LocalDate startDate) {
		this.startDate = startDate;
	}

	/**
	 * Get the maximum consolidated invoice start date.
	 *
	 * @return the end date (exclusive)
	 */
	public final @Nullable LocalDate getEndDate() {
		return endDate;
	}

	/**
	 * Set the maximum consolidated invoice start date.
	 *
	 * @param endDate
	 *        the end date to set (exclusive)
	 */
	public final void setEndDate(@Nullable LocalDate endDate) {
		this.endDate = endDate;
	}

	/**
	 * Get the "unpaid only" flag.
	 *
	 * @return {@literal true} to only include consolidated invoices whose
	 *         account invoices are not all fully paid
	 */
	public final boolean isUnpaidOnly() {
		return unpaidOnly;
	}

	/**
	 * Set the "unpaid only" flag.
	 *
	 * @param unpaidOnly
	 *        {@literal true} to only include consolidated invoices whose
	 *        account invoices are not all fully paid
	 */
	public final void setUnpaidOnly(boolean unpaidOnly) {
		this.unpaidOnly = unpaidOnly;
	}

}
