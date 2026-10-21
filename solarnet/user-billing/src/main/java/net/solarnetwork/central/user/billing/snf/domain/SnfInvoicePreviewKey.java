/* ==================================================================
 * SnfInvoicePreviewKey.java - 22/10/2026 2:31:52 pm
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

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.YearMonth;

/**
 * The key of an account preview invoice.
 *
 * @param userId
 *        the user ID of the account
 * @param month
 *        the month of the invoice
 * @param useAccountCredit
 *        {@code true} if the invoice uses available account credit
 * @author matt
 * @version 1.0
 */
public record SnfInvoicePreviewKey(Long userId, YearMonth month, boolean useAccountCredit) {

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID of the account
	 * @param month
	 *        the month of the invoice
	 * @param useAccountCredit
	 *        {@code true} if the invoice uses available account credit
	 * @throws IllegalArgumentException
	 *         if {@code userId} or {@code month} is {@code null}
	 */
	public SnfInvoicePreviewKey {
		requireNonNullArgument(userId, "userId");
		requireNonNullArgument(month, "month");
	}

}
