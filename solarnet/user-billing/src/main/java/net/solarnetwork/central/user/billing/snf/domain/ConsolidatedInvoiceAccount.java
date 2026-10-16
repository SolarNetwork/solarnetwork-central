/* ==================================================================
 * ConsolidatedInvoiceAccount.java - 15/10/2026 1:12:48 pm
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
import net.solarnetwork.central.user.billing.domain.LocalizedInvoiceInfo;

/**
 * An account invoice within a consolidated invoice, for rendering.
 *
 * @author matt
 * @version 1.0
 */
public class ConsolidatedInvoiceAccount {

	private final Long accountId;
	private final LocalizedInvoiceInfo invoice;
	private final Address address;

	/**
	 * Constructor.
	 *
	 * @param accountId
	 *        the account ID
	 * @param invoice
	 *        the account invoice
	 * @param address
	 *        the address of the account invoice
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public ConsolidatedInvoiceAccount(Long accountId, LocalizedInvoiceInfo invoice,
			Address address) {
		super();
		this.accountId = requireNonNullArgument(accountId, "accountId");
		this.invoice = requireNonNullArgument(invoice, "invoice");
		this.address = requireNonNullArgument(address, "address");
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
	 * Get a unique identifier for the account within the consolidated invoice,
	 * for linking to the account invoice.
	 *
	 * @return the anchor, never {@code null}
	 */
	public String getAnchor() {
		return "account-" + accountId;
	}

	/**
	 * Get the account name.
	 *
	 * @return the name of the account invoice address
	 */
	public String getName() {
		return address.getName();
	}

	/**
	 * Get the account invoice.
	 *
	 * @return the account invoice, never {@code null}
	 */
	public LocalizedInvoiceInfo getInvoice() {
		return invoice;
	}

	/**
	 * Get the address of the account invoice.
	 *
	 * @return the address, never {@code null}
	 */
	public Address getAddress() {
		return address;
	}

}
