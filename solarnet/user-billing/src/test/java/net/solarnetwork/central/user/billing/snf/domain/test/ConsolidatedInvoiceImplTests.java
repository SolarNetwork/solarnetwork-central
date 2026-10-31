/* ==================================================================
 * ConsolidatedInvoiceImplTests.java - 15/10/2026 2:31:14 pm
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

package net.solarnetwork.central.user.billing.snf.domain.test;

import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.user.billing.domain.Invoice;
import net.solarnetwork.central.user.billing.domain.InvoiceItem;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.billing.snf.domain.ConsolidatedInvoiceImpl;
import net.solarnetwork.central.user.billing.snf.domain.InvoiceImpl;
import net.solarnetwork.central.user.billing.snf.domain.InvoiceItemType;
import net.solarnetwork.central.user.billing.snf.domain.SnfConsolidatedInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem;

/**
 * Test cases for the {@link ConsolidatedInvoiceImpl} class.
 *
 * @author matt
 * @version 1.0
 */
public class ConsolidatedInvoiceImplTests {

	private static final LocalDate START_DATE = LocalDate.of(2026, 10, 1);

	private static InvoiceImpl invoice(Long invoiceId, String usage, String tax, String credit) {
		SnfInvoice invoice = new SnfInvoice(invoiceId, 1L, 2L, Instant.now(), START_DATE,
				START_DATE.plusMonths(1), "NZD");
		List<SnfInvoiceItem> items = new ArrayList<>(3);
		items.add(SnfInvoiceItem.newItem(invoice, InvoiceItemType.Usage, "datum-props-in",
				BigDecimal.ONE, new BigDecimal(usage)));
		items.add(SnfInvoiceItem.newItem(invoice, InvoiceItemType.Tax, "GST", BigDecimal.ONE,
				new BigDecimal(tax)));
		if ( credit != null ) {
			items.add(SnfInvoiceItem.newItem(invoice, InvoiceItemType.Credit, "account-credit",
					BigDecimal.ONE, new BigDecimal(credit)));
		}
		invoice.setItems(new LinkedHashSet<>(items));
		return new InvoiceImpl(invoice);
	}

	private static SnfConsolidatedInvoice consolidatedInvoice() {
		SnfConsolidatedInvoice invoice = new SnfConsolidatedInvoice(2010L, 1L, 2L, Instant.now(),
				START_DATE, START_DATE.plusMonths(1), "NZD");
		invoice.setAddress(new Address(1L, "Primary", "primary@localhost", "NZ", "Pacific/Auckland"));
		return invoice;
	}

	@Test
	public void totals() {
		// GIVEN
		List<Invoice> invoices = List.of(invoice(2001L, "10.00", "1.50", null),
				invoice(2002L, "20.00", "3.00", "-5.00"));

		// WHEN
		ConsolidatedInvoiceImpl result = new ConsolidatedInvoiceImpl(consolidatedInvoice(), invoices);

		// THEN
		// @formatter:off
		then(result)
			.as("ID is the consolidated invoice ID")
			.returns("2010", from(ConsolidatedInvoiceImpl::getId))
			.as("Invoice number from consolidated invoice ID")
			.returns("INV-1JU", from(ConsolidatedInvoiceImpl::getInvoiceNumber))
			.as("Month of consolidated invoice")
			.returns(YearMonth.of(2026, 10), from(ConsolidatedInvoiceImpl::getInvoiceMonth))
			.as("Time zone of consolidated invoice address")
			.returns("Pacific/Auckland", from(ConsolidatedInvoiceImpl::getTimeZoneId))
			.as("Currency of consolidated invoice")
			.returns("NZD", from(ConsolidatedInvoiceImpl::getCurrencyCode))
			.as("Amount is the sum of account invoice amounts")
			.returns(new BigDecimal("29.50"), from(ConsolidatedInvoiceImpl::getAmount))
			.as("Balance is the sum of account invoice balances")
			.returns(new BigDecimal("29.50"), from(ConsolidatedInvoiceImpl::getBalance))
			.as("Tax is the sum of account invoice tax")
			.returns(new BigDecimal("4.50"), from(ConsolidatedInvoiceImpl::getTaxAmount))
			.as("Credit is the sum of account invoice credit")
			.returns(new BigDecimal("-5.00"), from(ConsolidatedInvoiceImpl::getCreditAmount))
			.as("Remaining credit belongs to accounts")
			.returns(null, from(ConsolidatedInvoiceImpl::getRemainingCreditAmount))
			.as("No node usage")
			.returns(List.of(), from(ConsolidatedInvoiceImpl::getNodeUsageRecords))
			;
		then(result.getInvoiceItems())
			.as("Items of all account invoices")
			.extracting(InvoiceItem::getAmount)
			.containsExactly(new BigDecimal("10.00"), new BigDecimal("1.50"),
					new BigDecimal("20.00"), new BigDecimal("3.00"), new BigDecimal("-5.00"))
			;
		// @formatter:on
	}

	@Test
	public void noCredit() {
		// GIVEN
		List<Invoice> invoices = List.of(invoice(2001L, "10.00", "1.50", null));

		// WHEN
		ConsolidatedInvoiceImpl result = new ConsolidatedInvoiceImpl(consolidatedInvoice(), invoices);

		// THEN
		// @formatter:off
		then(result.getCreditAmount())
			.as("No credit")
			.isNull()
			;
		// @formatter:on
	}

}
