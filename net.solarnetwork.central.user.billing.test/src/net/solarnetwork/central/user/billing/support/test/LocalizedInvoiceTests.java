/* ==================================================================
 * LocalizedInvoiceTests.java - 22/09/2017 2:02:13 PM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.support.test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.Test;
import net.solarnetwork.central.user.billing.domain.Invoice;
import net.solarnetwork.central.user.billing.domain.InvoiceItem;
import net.solarnetwork.central.user.billing.domain.LocalizedInvoiceItemInfo;
import net.solarnetwork.central.user.billing.support.LocalizedInvoice;

/**
 * Test cases for the {@link LocalizedInvoice} class.
 * 
 * @author matt
 * @version 1.0
 */
public class LocalizedInvoiceTests {

	private static final Locale EN_NZ = new Locale("en", "NZ");
	private static final BigDecimal AMOUNT_1 = new BigDecimal("1.99");
	private static final BigDecimal AMOUNT_2 = new BigDecimal("2.99");
	private static final BigDecimal AMOUNT_3 = new BigDecimal("3.99");
	private static final BigDecimal AMOUNT_4 = new BigDecimal("4.99");
	private static final String GST = "GST";
	private static final String VAT = "VAT";

	private static InvoiceItem createInvoiceItem(String type, String description, BigDecimal amount) {
		InvoiceItem item = createMock(InvoiceItem.class);
		expect(item.getItemType()).andReturn(type).anyTimes();
		if ( description != null ) {
			expect(item.getDescription()).andReturn(description).anyTimes();
		}
		if ( amount != null ) {
			expect(item.getAmount()).andReturn(amount).anyTimes();
		}
		expect(item.getCurrencyCode()).andReturn("NZD").anyTimes();
		expect(item.getId()).andReturn(UUID.randomUUID().toString()).anyTimes();
		return item;
	}

	private Invoice createInvoice(List<InvoiceItem> items) {
		Invoice invoice = createMock(Invoice.class);
		expect(invoice.getInvoiceItems()).andReturn(items).anyTimes();
		return invoice;
	}

	@Test
	public void aggregateTaxItem() {
		InvoiceItem item1 = createInvoiceItem("USAGE", null, null);
		InvoiceItem item2 = createInvoiceItem("RECURRING", null, null);
		InvoiceItem tax1 = createInvoiceItem("TAX", GST, AMOUNT_1);
		InvoiceItem tax2 = createInvoiceItem("TAX", GST, AMOUNT_2);
		Invoice invoice = createInvoice(Arrays.asList(item1, item2, tax1, tax2));
		replay(invoice, item1, item2, tax1, tax2);

		// when
		LocalizedInvoice fmt = new LocalizedInvoice(invoice, EN_NZ);
		List<LocalizedInvoiceItemInfo> aggItems = fmt.getLocalizedTaxInvoiceItemsGroupedByDescription();

		// then
		assertThat("Aggregate items", aggItems, hasSize(1));
		LocalizedInvoiceItemInfo agg = aggItems.get(0);
		assertThat("Aggregate description", agg.getLocalizedDescription(), equalTo(GST));
		assertThat("Aggregate amount", agg.getLocalizedAmount(), equalTo("$4.98"));
		verify(invoice, item1, item2, tax1, tax2);
	}

	@Test
	public void aggregateTaxItems() {
		InvoiceItem item1 = createInvoiceItem("USAGE", null, null);
		InvoiceItem item2 = createInvoiceItem("RECURRING", null, null);
		InvoiceItem tax1 = createInvoiceItem("TAX", GST, AMOUNT_1);
		InvoiceItem tax2 = createInvoiceItem("TAX", GST, AMOUNT_2);
		InvoiceItem tax3 = createInvoiceItem("TAX", VAT, AMOUNT_3);
		InvoiceItem tax4 = createInvoiceItem("TAX", VAT, AMOUNT_4);
		Invoice invoice = createInvoice(Arrays.asList(item1, item2, tax1, tax2, tax3, tax4));
		replay(invoice, item1, item2, tax1, tax2, tax3, tax4);

		// when
		LocalizedInvoice fmt = new LocalizedInvoice(invoice, EN_NZ);
		List<LocalizedInvoiceItemInfo> aggItems = fmt.getLocalizedTaxInvoiceItemsGroupedByDescription();

		// then
		assertThat("Aggregate items", aggItems, hasSize(2));
		LocalizedInvoiceItemInfo gst = aggItems.get(0);
		assertThat("GST description", gst.getLocalizedDescription(), equalTo(GST));
		assertThat("GST amount", gst.getLocalizedAmount(), equalTo("$4.98"));

		LocalizedInvoiceItemInfo vat = aggItems.get(1);
		assertThat("VAT description", vat.getLocalizedDescription(), equalTo(VAT));
		assertThat("VAT amount", vat.getLocalizedAmount(), equalTo("$8.98"));
		verify(invoice, item1, item2, tax1, tax2, tax3, tax4);
	}

}
