/* ==================================================================
 * AggregateLocalizedInvoiceItemTests.java - 22/09/2017 1:47:51 PM
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
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.util.Locale;
import org.easymock.EasyMock;
import org.junit.Test;
import net.solarnetwork.central.user.billing.domain.InvoiceItem;
import net.solarnetwork.central.user.billing.support.AggregateLocalizedInvoiceItem;

/**
 * Test cases for the {@link AggregateLocalizedInvoiceItem} class.
 * 
 * @author matt
 * @version 1.0
 */
public class AggregateLocalizedInvoiceItemTests {

	private static final BigDecimal AMOUNT_1 = new BigDecimal("1.99");
	private static final BigDecimal AMOUNT_2 = new BigDecimal("2.99");

	@Test
	public void emptyAmount() {
		AggregateLocalizedInvoiceItem agg = new AggregateLocalizedInvoiceItem(Locale.US);
		assertThat("Empty amount", agg.getAmount(), equalTo(BigDecimal.ZERO));
	}

	@Test
	public void addReturnsSame() {
		// given
		InvoiceItem item = createMock(InvoiceItem.class);
		replay(item);

		// when
		AggregateLocalizedInvoiceItem agg = new AggregateLocalizedInvoiceItem(Locale.US);

		// then
		assertThat("Instance", agg.addItem(item), sameInstance(agg));
		verify(item);
	}

	@Test
	public void singleAmount() {
		// given
		InvoiceItem item = EasyMock.createMock(InvoiceItem.class);
		expect(item.getCurrencyCode()).andReturn("USD").anyTimes();
		expect(item.getAmount()).andReturn(AMOUNT_1).anyTimes();
		replay(item);

		// when
		AggregateLocalizedInvoiceItem agg = new AggregateLocalizedInvoiceItem(Locale.US);
		agg.addItem(item);

		// then
		assertThat("Single amount", item.getAmount(), equalTo(AMOUNT_1));
		assertThat("Formatted amount", agg.getLocalizedAmount(), equalTo("$1.99"));
		verify(item);
	}

	@Test
	public void aggregateAmount() {
		// given
		InvoiceItem item1 = EasyMock.createMock(InvoiceItem.class);
		expect(item1.getCurrencyCode()).andReturn("USD").anyTimes();
		expect(item1.getAmount()).andReturn(AMOUNT_1).anyTimes();

		InvoiceItem item2 = EasyMock.createMock(InvoiceItem.class);
		expect(item2.getAmount()).andReturn(AMOUNT_2).anyTimes();

		replay(item1, item2);

		// when
		AggregateLocalizedInvoiceItem agg = new AggregateLocalizedInvoiceItem(Locale.US);
		agg.addItem(item1).addItem(item2);

		// then
		assertThat("Aggregate amount", agg.getAmount(), equalTo(AMOUNT_1.add(AMOUNT_2)));
		assertThat("Formatted amount", agg.getLocalizedAmount(), equalTo("$4.98"));
		verify(item1, item2);
	}

}
