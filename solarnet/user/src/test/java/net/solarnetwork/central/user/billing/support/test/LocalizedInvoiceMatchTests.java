/* ==================================================================
 * LocalizedInvoiceMatchTests.java - 29/08/2017 11:00:40 AM
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

import static org.easymock.EasyMock.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.user.billing.domain.InvoiceMatch;
import net.solarnetwork.central.user.billing.support.LocalizedInvoiceMatch;

/**
 * Test cases for the {@link LocalizedInvoiceMatch} class.
 * 
 * @author matt
 * @version 2.0
 */
public class LocalizedInvoiceMatchTests {

	private static final String TEST_TIME_ZONE = "Pacific/Auckland";
	private static final String TEST_CURRENCY_CODE = "NZD";
	private static final BigDecimal TEST_BALANCE = new BigDecimal("1.23");
	private static final BigDecimal TEST_AMOUNT = new BigDecimal("2.34");
	private static final Instant TEST_DATE = LocalDateTime.of(2017, 1, 1, 0, 0)
			.atZone(ZoneId.of(TEST_TIME_ZONE)).toInstant();

	private InvoiceMatch match;

	@Before
	public void setup() {
		match = EasyMock.createMock(InvoiceMatch.class);
	}

	@After
	public void teardown() {
		EasyMock.verify(match);
	}

	private void replayAll() {
		EasyMock.replay(match);
	}

	@Test
	public void testNZ_NZD() {
		// given
		expect(match.getAmount()).andReturn(TEST_AMOUNT).anyTimes();
		expect(match.getBalance()).andReturn(TEST_BALANCE).anyTimes();
		expect(match.getCreated()).andReturn(TEST_DATE).anyTimes();
		expect(match.getCurrencyCode()).andReturn(TEST_CURRENCY_CODE).anyTimes();
		expect(match.getTimeZoneId()).andReturn(TEST_TIME_ZONE).anyTimes();

		// when
		replayAll();
		LocalizedInvoiceMatch locMatch = new LocalizedInvoiceMatch(match, new Locale("en", "NZ"));

		// then
		assertThat("Amount", locMatch.getAmount(), equalTo(TEST_AMOUNT));
		assertThat("Balance", locMatch.getBalance(), equalTo(TEST_BALANCE));
		assertThat("Currency code", locMatch.getCurrencyCode(), equalTo(TEST_CURRENCY_CODE));
		assertThat("Time zone", locMatch.getTimeZoneId(), equalTo(TEST_TIME_ZONE));
		assertThat("Localized amount", locMatch.getLocalizedAmount(), equalTo("$2.34"));
		assertThat("Localized balance", locMatch.getLocalizedBalance(), equalTo("$1.23"));
		assertThat("Localized date", locMatch.getLocalizedDate(), equalTo("Sunday, 1 January 2017"));

	}

	@Test
	public void testDE_NZD() {
		// given
		expect(match.getAmount()).andReturn(TEST_AMOUNT).anyTimes();
		expect(match.getBalance()).andReturn(TEST_BALANCE).anyTimes();
		expect(match.getCreated()).andReturn(TEST_DATE).anyTimes();
		expect(match.getCurrencyCode()).andReturn(TEST_CURRENCY_CODE).anyTimes();
		expect(match.getTimeZoneId()).andReturn(TEST_TIME_ZONE).anyTimes();

		// when
		replayAll();
		LocalizedInvoiceMatch locMatch = new LocalizedInvoiceMatch(match, new Locale("de", "DE"));

		// then
		assertThat("Amount", locMatch.getAmount(), equalTo(TEST_AMOUNT));
		assertThat("Balance", locMatch.getBalance(), equalTo(TEST_BALANCE));
		assertThat("Currency code", locMatch.getCurrencyCode(), equalTo(TEST_CURRENCY_CODE));
		assertThat("Time zone", locMatch.getTimeZoneId(), equalTo(TEST_TIME_ZONE));
		assertThat("Localized amount", locMatch.getLocalizedAmount(),
				anyOf(equalTo("2,34 NZD"), equalTo("2,34\u00a0NZ$"), equalTo("2,34 NZ$")));
		assertThat("Localized balance", locMatch.getLocalizedBalance(),
				anyOf(equalTo("1,23 NZD"), equalTo("1,23\u00a0NZ$"), equalTo("1,23 NZ$")));
		assertThat("Localized date", locMatch.getLocalizedDate(), equalTo("Sonntag, 1. Januar 2017"));
	}

}
