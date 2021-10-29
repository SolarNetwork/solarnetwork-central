/* ==================================================================
 * SnfInvoiceTests.java - 20/07/2020 4:00:46 PM
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

package net.solarnetwork.central.user.billing.snf.domain.test;

import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem.newItem;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.billing.snf.domain.InvoiceItemType;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem;
import net.solarnetwork.central.user.domain.UserLongPK;

/**
 * Test cases for the {@link SnfInvoice} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SnfInvoiceTests {

	private static final String TEST_PROD_KEY = UUID.randomUUID().toString();

	@Test
	public void zone_notPresent() {
		// GIVEN
		SnfInvoice invoice = new SnfInvoice(randomUUID().getMostSignificantBits());

		// WHEN
		ZoneId zone = invoice.getTimeZone();

		// THEN
		assertThat("Zone is null", zone, nullValue());
	}

	@Test
	public void zone_invalidValue() {
		// GIVEN
		SnfInvoice invoice = new SnfInvoice(randomUUID().getMostSignificantBits());
		Address addr = new Address();
		addr.setTimeZoneId("foo/bar");
		invoice.setAddress(addr);

		// WHEN
		ZoneId zone = invoice.getTimeZone();

		// THEN
		assertThat("Zone is null", zone, nullValue());
	}

	@Test
	public void zone_valid() {
		// GIVEN
		SnfInvoice invoice = new SnfInvoice(randomUUID().getMostSignificantBits());
		Address addr = new Address();
		addr.setTimeZoneId("Pacific/Auckland");
		invoice.setAddress(addr);

		// WHEN
		ZoneId zone = invoice.getTimeZone();

		// THEN
		assertThat("Zone", zone, equalTo(ZoneId.of(addr.getTimeZoneId())));
	}

	@Test
	public void sameness_basic() {
		// GIVEN
		SnfInvoice inv1 = new SnfInvoice(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits(), randomUUID().getMostSignificantBits(),
				Instant.now());
		inv1.setStartDate(LocalDate.of(2020, 1, 1));
		inv1.setEndDate(LocalDate.of(2020, 2, 1));
		inv1.setCurrencyCode("NZD");

		Address addr = new Address(randomUUID().getMostSignificantBits(), Instant.now());
		inv1.setAddress(addr);

		SnfInvoice inv2 = new SnfInvoice(inv1.getId(), inv1.getAccountId(), inv1.getCreated());
		inv2.setStartDate(inv1.getStartDate());
		inv2.setEndDate(inv1.getEndDate());
		inv2.setCurrencyCode(inv1.getCurrencyCode());
		inv2.setAddress(new Address(addr.getId(), addr.getCreated()));

		// THEN
		assertThat("Entities have sameness", inv1.isSameAs(inv2), equalTo(true));
		assertThat("Entities do not differ", inv1.differsFrom(inv2), equalTo(false));
	}

	@Test
	public void sameness_basic_diffStartDate() {
		// GIVEN
		SnfInvoice inv1 = new SnfInvoice(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits(), randomUUID().getMostSignificantBits(),
				Instant.now());
		inv1.setStartDate(LocalDate.of(2020, 1, 1));
		inv1.setEndDate(LocalDate.of(2020, 2, 1));
		inv1.setCurrencyCode("NZD");

		Address addr = new Address(randomUUID().getMostSignificantBits(), Instant.now());
		inv1.setAddress(addr);

		SnfInvoice inv2 = new SnfInvoice(inv1.getId(), inv1.getAccountId(), inv1.getCreated());
		inv2.setStartDate(inv1.getStartDate().plusDays(1));
		inv2.setEndDate(inv1.getEndDate());
		inv2.setCurrencyCode(inv1.getCurrencyCode());
		inv2.setAddress(new Address(addr.getId(), addr.getCreated()));

		// THEN
		assertThat("Entities do not have sameness", inv1.isSameAs(inv2), equalTo(false));
		assertThat("Entities differ", inv1.differsFrom(inv2), equalTo(true));
	}

	@Test
	public void sameness_basic_diffAddress() {
		// GIVEN
		SnfInvoice inv1 = new SnfInvoice(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits(), randomUUID().getMostSignificantBits(),
				Instant.now());
		inv1.setStartDate(LocalDate.of(2020, 1, 1));
		inv1.setEndDate(LocalDate.of(2020, 2, 1));
		inv1.setCurrencyCode("NZD");

		Address addr = new Address(randomUUID().getMostSignificantBits(), Instant.now());
		inv1.setAddress(addr);

		SnfInvoice inv2 = new SnfInvoice(inv1.getId(), inv1.getAccountId(), inv1.getCreated());
		inv2.setStartDate(inv1.getStartDate());
		inv2.setEndDate(inv1.getEndDate());
		inv2.setCurrencyCode(inv1.getCurrencyCode());
		inv2.setAddress(new Address(randomUUID().getMostSignificantBits(), addr.getCreated()));

		// THEN
		assertThat("Entities do not have sameness", inv1.isSameAs(inv2), equalTo(false));
		assertThat("Entities differ", inv1.differsFrom(inv2), equalTo(true));
	}

	@Test
	public void sameness_withItems() {
		// GIVEN
		SnfInvoice inv1 = new SnfInvoice(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits(), randomUUID().getMostSignificantBits(),
				Instant.now());
		inv1.setStartDate(LocalDate.of(2020, 1, 1));
		inv1.setEndDate(LocalDate.of(2020, 2, 1));
		inv1.setCurrencyCode("NZD");

		Address addr = new Address(randomUUID().getMostSignificantBits(), Instant.now());
		inv1.setAddress(addr);

		SnfInvoiceItem item1 = newItem(inv1.getId().getId(), InvoiceItemType.Fixed, TEST_PROD_KEY,
				new BigDecimal("1.23"), new BigDecimal("12345"), inv1.getCreated());
		SnfInvoiceItem item2 = newItem(inv1.getId().getId(), InvoiceItemType.Fixed, TEST_PROD_KEY,
				new BigDecimal("2.34"), new BigDecimal("23456"), inv1.getCreated());
		inv1.setItems(new HashSet<>(Arrays.asList(item1, item2)));

		SnfInvoice inv2 = new SnfInvoice(inv1.getId(), inv1.getAccountId(), inv1.getCreated());
		inv2.setStartDate(inv1.getStartDate());
		inv2.setEndDate(inv1.getEndDate());
		inv2.setCurrencyCode(inv1.getCurrencyCode());

		inv2.setAddress(new Address(addr.getId(), addr.getCreated()));

		inv2.setItems(new HashSet<>(Arrays.asList(item1, item2)));

		// THEN
		assertThat("Entities have sameness", inv1.isSameAs(inv2), equalTo(true));
		assertThat("Entities do not differ", inv1.differsFrom(inv2), equalTo(false));
	}

	@Test
	public void totalCost_noItems() {
		// GIVEN
		SnfInvoice inv1 = new SnfInvoice(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits(), randomUUID().getMostSignificantBits(),
				Instant.now());

		// WHEN
		BigDecimal totalCost = inv1.getTotalAmount();

		// THEN
		assertThat("Total amount of no items is 0", totalCost, equalTo(BigDecimal.ZERO));
	}

	@Test
	public void totalCost_withItems() {
		// GIVEN
		SnfInvoice inv1 = new SnfInvoice(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits(), randomUUID().getMostSignificantBits(),
				Instant.now());
		SnfInvoiceItem item1 = newItem(inv1.getId().getId(), InvoiceItemType.Fixed, TEST_PROD_KEY,
				new BigDecimal("12345"), new BigDecimal("1.23"), inv1.getCreated());
		SnfInvoiceItem item2 = newItem(inv1.getId().getId(), InvoiceItemType.Fixed, TEST_PROD_KEY,
				new BigDecimal("23456"), new BigDecimal("2.34"), inv1.getCreated());
		inv1.setItems(new HashSet<>(Arrays.asList(item1, item2)));

		// WHEN
		BigDecimal totalCost = inv1.getTotalAmount();

		// THEN
		assertThat("Total amount is sum of item amounts", totalCost, equalTo(new BigDecimal("3.57")));
	}

	@Test
	public void sortByDate() {
		// GIVEN
		List<SnfInvoice> invoices = new ArrayList<>(5);
		for ( int i = 0; i < 10; i++ ) {
			SnfInvoice inv = new SnfInvoice(randomUUID().getMostSignificantBits(),
					randomUUID().getMostSignificantBits(), randomUUID().getMostSignificantBits(),
					Instant.now());
			inv.setStartDate(LocalDate.of(2020 + ((int) (Math.random() * 10) - 5),
					((int) (Math.random() * 11 + 1)), 1));
			invoices.add(inv);
		}

		// WHEN
		Collections.sort(invoices, SnfInvoice.SORT_BY_DATE);

		// THEN
		LocalDate prev = null;
		for ( SnfInvoice inv : invoices ) {
			if ( prev != null ) {
				assertThat("Invoices sorted in asending order by date",
						inv.getStartDate().isBefore(prev), equalTo(false));
			}
			prev = inv.getStartDate();
		}
	}

	@Test
	public void sortByDate_thenId() {
		// GIVEN
		List<SnfInvoice> invoices = new ArrayList<>(5);
		for ( int i = 0; i < 10; i++ ) {
			SnfInvoice inv = new SnfInvoice(randomUUID().getMostSignificantBits(), 1L, 2L,
					Instant.now());
			inv.setStartDate(LocalDate.of(2020, 1, 1)); // all on same user, account, and date
			invoices.add(inv);
		}

		// WHEN
		Collections.sort(invoices, SnfInvoice.SORT_BY_DATE);

		// THEN
		UserLongPK prev = null;
		for ( SnfInvoice inv : invoices ) {
			if ( prev != null ) {
				assertThat("Invoices sorted in asending order by ID for equal dates",
						inv.getId().compareTo(prev), not(lessThan(0)));
			}
			prev = inv.getId();
		}
	}

}
