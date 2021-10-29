/* ==================================================================
 * SnfTaxCodeResolverTests.java - 24/07/2020 10:02:19 AM
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

package net.solarnetwork.central.user.billing.snf.test;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.user.billing.snf.domain.TaxCodeFilter.filterFor;
import static net.solarnetwork.central.user.billing.snf.test.SnfMatchers.matchesFilter;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import java.time.LocalDate;
import org.junit.Test;
import net.solarnetwork.central.user.billing.snf.SnfBillingSystem;
import net.solarnetwork.central.user.billing.snf.SnfTaxCodeResolver;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.TaxCodeFilter;
import net.solarnetwork.service.StaticOptionalService;

/**
 * Test cases for the {@link SnfBillingSystem} implementation of
 * {@link SnfTaxCodeResolver}.
 * 
 * @author matt
 * @version 2.0
 */
public class SnfTaxCodeResolverTests extends AbstractSnfBililngSystemTest {

	@Test
	public void resolveTaxCodeFilter_countryOnly() {
		// GIVEN
		Address addr = new Address();
		addr.setCountry("NZ");
		addr.setTimeZoneId("Pacific/Auckland");
		SnfInvoice invoice = new SnfInvoice(randomUUID().getMostSignificantBits(), userId, now());
		invoice.setAddress(addr);
		LocalDate invoiceStartDate = LocalDate.of(2020, 2, 1);
		invoice.setStartDate(invoiceStartDate);

		// WHEN
		replayAll();
		TaxCodeFilter filter = system.taxCodeFilterForInvoice(invoice);

		// THEN
		assertThat("Filter created", filter, notNullValue());
		assertThat("Filter created with country zone and invoice start date", filter,
				matchesFilter(invoiceStartDate.atStartOfDay(addr.getTimeZone()).toInstant(), "NZ"));
	}

	@Test
	public void resolveTaxCodeFilter_countryAndState() {
		// GIVEN
		Address addr = new Address();
		addr.setCountry("US");
		addr.setStateOrProvince("CA");
		addr.setTimeZoneId("Pacific/Auckland");
		SnfInvoice invoice = new SnfInvoice(randomUUID().getMostSignificantBits(), userId, now());
		invoice.setAddress(addr);
		LocalDate invoiceStartDate = LocalDate.of(2020, 2, 1);
		invoice.setStartDate(invoiceStartDate);

		// WHEN
		replayAll();
		TaxCodeFilter filter = system.taxCodeFilterForInvoice(invoice);

		// THEN
		assertThat("Filter created", filter, notNullValue());
		assertThat("Filter created with country and state zones and invoice start date", filter,
				matchesFilter(invoiceStartDate.atStartOfDay(addr.getTimeZone()).toInstant(), "US",
						"US.CA"));
	}

	@Test
	public void resolveTaxCodeFilter_viaOptionalService() {
		// GIVEN
		final String testZone = "FOOBAR";
		system.setTaxCodeResolver(
				new StaticOptionalService<SnfTaxCodeResolver>(new SnfTaxCodeResolver() {

					@Override
					public TaxCodeFilter taxCodeFilterForInvoice(SnfInvoice invoice) {
						// TODO Auto-generated method stub
						return filterFor(
								invoice.getStartDate().atStartOfDay(invoice.getTimeZone()).toInstant(),
								testZone);
					}
				}));
		Address addr = new Address();
		addr.setTimeZoneId("UTC");
		SnfInvoice invoice = new SnfInvoice(randomUUID().getMostSignificantBits(), userId, now());
		invoice.setAddress(addr);
		LocalDate invoiceStartDate = LocalDate.of(2020, 2, 1);
		invoice.setStartDate(invoiceStartDate);

		// WHEN
		replayAll();
		TaxCodeFilter filter = system.taxCodeFilterForInvoice(invoice);

		// THEN
		assertThat("Filter created", filter, notNullValue());
		assertThat("Filter created from configured service", filter,
				matchesFilter(invoiceStartDate.atStartOfDay(addr.getTimeZone()).toInstant(), testZone));
	}
}
