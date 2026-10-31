/* ==================================================================
 * ConsolidatedInvoicePreviewAccount.java - 22/10/2026 2:19:12 pm
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

package net.solarnetwork.central.user.billing.domain;

import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import org.jspecify.annotations.Nullable;

/**
 * The status of the preview invoice of one account of a consolidated invoice
 * preview.
 *
 * @param userId
 *        the user ID of the account
 * @param name
 *        the account name
 * @param state
 *        the preview state of the account invoice
 * @param invoiceNumber
 *        the account invoice number, when {@code state} is {@code Done} and
 *        there are charges, otherwise {@code null}
 * @param localizedAmount
 *        the localized account invoice amount, when {@code state} is
 *        {@code Done} and there are charges, otherwise {@code null}
 * @author matt
 * @version 1.0
 */
public record ConsolidatedInvoicePreviewAccount(Long userId, String name, InvoicePreviewState state,
		@Nullable String invoiceNumber, @Nullable String localizedAmount) {

	/**
	 * Constructor.
	 *
	 * @param userId
	 *        the user ID of the account
	 * @param name
	 *        the account name
	 * @param state
	 *        the preview state of the account invoice
	 * @param invoiceNumber
	 *        the account invoice number, when {@code state} is {@code Done} and
	 *        there are charges, otherwise {@code null}
	 * @param localizedAmount
	 *        the localized account invoice amount, when {@code state} is
	 *        {@code Done} and there are charges, otherwise {@code null}
	 * @throws IllegalArgumentException
	 *         if {@code userId}, {@code name}, or {@code state} is {@code null}
	 */
	public ConsolidatedInvoicePreviewAccount {
		requireNonNullArgument(userId, "userId");
		requireNonNullArgument(name, "name");
		requireNonNullArgument(state, "state");
	}

}
