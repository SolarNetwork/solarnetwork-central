/* ==================================================================
 * ConsolidatedInvoicePreviewStatus.java - 22/10/2026 2:22:37 pm
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
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The status of a preview consolidated invoice whose account invoices are
 * generated in the background.
 *
 * @param state
 *        the combined preview state of all the accounts, as given by
 *        {@link InvoicePreviewState#combined(Iterable)}
 * @param accounts
 *        the status of each account, in the order the accounts are listed in a
 *        consolidated invoice
 * @param invoice
 *        the preview consolidated invoice, when {@code state} is {@code Done}
 *        and there are charges, otherwise {@code null}
 * @author matt
 * @version 1.0
 */
public record ConsolidatedInvoicePreviewStatus(InvoicePreviewState state,
		List<ConsolidatedInvoicePreviewAccount> accounts, @Nullable Invoice invoice) {

	/**
	 * Constructor.
	 *
	 * @param state
	 *        the combined preview state of all the accounts
	 * @param accounts
	 *        the status of each account, in the order the accounts are listed
	 *        in a consolidated invoice
	 * @param invoice
	 *        the preview consolidated invoice, when {@code state} is
	 *        {@code Done} and there are charges, otherwise {@code null}
	 * @throws IllegalArgumentException
	 *         if {@code state} or {@code accounts} is {@code null}
	 */
	public ConsolidatedInvoicePreviewStatus {
		requireNonNullArgument(state, "state");
		accounts = List.copyOf(requireNonNullArgument(accounts, "accounts"));
	}

}
