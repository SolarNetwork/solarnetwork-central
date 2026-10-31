/* ==================================================================
 * LocalizedConsolidatedInvoice.java - 16/10/2026 4:14:51 pm
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
import java.util.List;
import java.util.Locale;
import net.solarnetwork.central.user.billing.support.LocalizedInvoice;

/**
 * A localized consolidated invoice, with a summary of each account invoice it
 * is made of.
 *
 * @author matt
 * @version 1.0
 */
public class LocalizedConsolidatedInvoice extends LocalizedInvoice {

	private final List<LocalizedConsolidatedInvoiceAccount> accountInvoices;

	/**
	 * Constructor.
	 *
	 * @param invoice
	 *        the consolidated invoice
	 * @param accountInvoices
	 *        the summary of each account invoice of the consolidated invoice
	 * @param locale
	 *        the locale
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public LocalizedConsolidatedInvoice(ConsolidatedInvoiceImpl invoice,
			List<LocalizedConsolidatedInvoiceAccount> accountInvoices, Locale locale) {
		super(invoice, locale);
		this.accountInvoices = List.copyOf(requireNonNullArgument(accountInvoices, "accountInvoices"));
	}

	/**
	 * Get the summary of each account invoice of the consolidated invoice.
	 *
	 * @return the account invoices, in the order they are listed in the
	 *         consolidated invoice
	 */
	public List<LocalizedConsolidatedInvoiceAccount> getLocalizedAccountInvoices() {
		return accountInvoices;
	}

}
