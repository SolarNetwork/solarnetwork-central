/* ==================================================================
 * SnfInvoiceFilter.java - 23/07/2020 6:45:43 AM
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

package net.solarnetwork.central.user.billing.snf.domain;

import java.time.LocalDate;
import net.solarnetwork.domain.SimplePagination;

/**
 * Query filter for {@link SnfInvoice} entities.
 * 
 * @author matt
 * @version 1.1
 */
public class SnfInvoiceFilter extends SimplePagination {

	private Long userId;
	private Long accountId;
	private LocalDate startDate;
	private LocalDate endDate;
	private boolean unpaidOnly;
	private boolean ignoreCreditOnly;

	/**
	 * Create a new filter with a user ID.
	 * 
	 * @param userId
	 *        the user ID to set
	 * @return the filter, never {@literal null}
	 */
	public static SnfInvoiceFilter forUser(Long userId) {
		SnfInvoiceFilter f = new SnfInvoiceFilter();
		f.setUserId(userId);
		return f;
	}

	/**
	 * Create a new filter with an account ID.
	 * 
	 * @param accountId
	 *        the account ID to set
	 * @return the filter, never {@literal null}
	 */
	public static SnfInvoiceFilter forAccount(Long accountId) {
		SnfInvoiceFilter f = new SnfInvoiceFilter();
		f.setAccountId(accountId);
		return f;
	}

	/**
	 * Create a new filter with an account.
	 * 
	 * @param account
	 *        the account to extract the ID and userId from
	 * @return the filter, never {@literal null}
	 */
	public static SnfInvoiceFilter forAccount(Account account) {
		SnfInvoiceFilter f = new SnfInvoiceFilter();
		f.setAccountId(account.getId().getId());
		f.setUserId(account.getUserId());
		return f;
	}

	@Override
	public SnfInvoiceFilter clone() {
		return (SnfInvoiceFilter) super.clone();
	}

	/**
	 * Get the user ID.
	 * 
	 * @return the user ID
	 */
	public Long getUserId() {
		return userId;
	}

	/**
	 * Set the user ID.
	 * 
	 * @param userId
	 *        the user ID to set
	 */
	public void setUserId(Long userId) {
		this.userId = userId;
	}

	/**
	 * Get the account ID.
	 * 
	 * @return the account ID
	 */
	public Long getAccountId() {
		return accountId;
	}

	/**
	 * Set the account ID.
	 * 
	 * @param accountId
	 *        the account ID to set
	 */
	public void setAccountId(Long accountId) {
		this.accountId = accountId;
	}

	/**
	 * Get the minimum date.
	 * 
	 * @return the starting date (inclusive)
	 */
	public LocalDate getStartDate() {
		return startDate;
	}

	/**
	 * Set the minimum date.
	 * 
	 * @param startDate
	 *        the date to set (inclusive)
	 */
	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	/**
	 * Get the maximum date.
	 * 
	 * @return the date (exclusive)
	 */
	public LocalDate getEndDate() {
		return endDate;
	}

	/**
	 * Set the maximum date.
	 * 
	 * @param endDate
	 *        the date to set (exclusive)
	 */
	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	/**
	 * Get the "unpaid only" flag.
	 * 
	 * @return {@literal true} to only include invoices that are not fully paid
	 */
	public boolean getUnpaidOnly() {
		return unpaidOnly;
	}

	/**
	 * Set the "unpaid only" flag.
	 * 
	 * @param unpaidOnly
	 *        {@literal true} to only include invoices that are not fully paid
	 */
	public void setUnpaidOnly(boolean unpaidOnly) {
		this.unpaidOnly = unpaidOnly;
	}

	/**
	 * Get the "ignore credit only" flag.
	 * 
	 * @return {@literal true} to ignore invoices that only have credit items
	 *         (i.e. issued credit)
	 */
	public boolean isIgnoreCreditOnly() {
		return ignoreCreditOnly;
	}

	/**
	 * Set the "ignore credit only" flag.
	 * 
	 * @param ignoreCreditOnly
	 *        {@literal true} to ignore invoices that only have credit items
	 *        (i.e. issued credit)
	 */
	public void setIgnoreCreditOnly(boolean ignoreCreditOnly) {
		this.ignoreCreditOnly = ignoreCreditOnly;
	}

}
