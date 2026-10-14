/* ==================================================================
 * SnfAccountingSystem.java - 11/10/2026 2:51:35 pm
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

package net.solarnetwork.central.user.billing.snf;

import java.util.Collection;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.InvoicePayment;
import net.solarnetwork.central.user.billing.snf.domain.Payment;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;

/**
 * API for recording SNF invoices and payments in an accounting system.
 *
 * @author matt
 * @version 1.0
 */
public interface SnfAccountingSystem {

	/**
	 * Record an invoice in the accounting system.
	 *
	 * <p>
	 * Recording an invoice the accounting system already has does nothing, so
	 * a failed attempt can safely be repeated.
	 * </p>
	 *
	 * @param invoice
	 *        the invoice, with its items
	 * @param account
	 *        the invoice's account
	 * @return {@code true} if the invoice was added, or {@code false} if the
	 *         accounting system already had it
	 * @throws RepeatableTaskException
	 *         if the invoice cannot be recorded now, but might be later
	 */
	boolean syncInvoice(SnfInvoice invoice, Account account);

	/**
	 * Record a payment in the accounting system.
	 *
	 * <p>
	 * The payment is applied to the invoices it pays, which the accounting
	 * system must already have. Any amount not applied to an invoice is kept
	 * as credit for the account. Recording a payment the accounting system
	 * already has does nothing, so a failed attempt can safely be repeated.
	 * </p>
	 *
	 * @param payment
	 *        the payment
	 * @param invoicePayments
	 *        the payment's invoice payments, which say how much of the payment
	 *        was applied to each invoice
	 * @param account
	 *        the payment's account
	 * @return {@code true} if the payment was added, or {@code false} if the
	 *         accounting system already had it
	 * @throws RepeatableTaskException
	 *         if the payment cannot be recorded now, but might be later
	 */
	boolean syncPayment(Payment payment, Collection<InvoicePayment> invoicePayments,
			Account account);

}
