/* ==================================================================
 * SnfInvoiceNodeUsageTests.java - 28/05/2021 7:11:07 AM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import java.math.BigInteger;
import java.time.Instant;
import org.junit.Test;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceNodeUsage;

/**
 * Test cases for the {@link SnfInvoiceNodeUsage} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SnfInvoiceNodeUsageTests {

	@Test
	public void differsFrom_same() {
		// GIVEN
		SnfInvoiceNodeUsage u1 = new SnfInvoiceNodeUsage(1L, 2L, Instant.now().minusSeconds(1),
				BigInteger.valueOf(3L), BigInteger.valueOf(4L), BigInteger.valueOf(5L));
		SnfInvoiceNodeUsage u2 = new SnfInvoiceNodeUsage(1L, 2L, Instant.now(), BigInteger.valueOf(3L),
				BigInteger.valueOf(4L), BigInteger.valueOf(5L));

		// THEN
		assertThat("Objects are same", u1.isSameAs(u2), is(equalTo(true)));
		assertThat("Objects are not different", u1.differsFrom(u2), is(equalTo(false)));
	}

	@Test
	public void differsFrom_differentInvoice() {
		// GIVEN
		SnfInvoiceNodeUsage u1 = new SnfInvoiceNodeUsage(1L, 2L, Instant.now().minusSeconds(1),
				BigInteger.valueOf(3L), BigInteger.valueOf(4L), BigInteger.valueOf(5L));
		SnfInvoiceNodeUsage u2 = new SnfInvoiceNodeUsage(2L, 2L, Instant.now(), BigInteger.valueOf(3L),
				BigInteger.valueOf(4L), BigInteger.valueOf(5L));

		// THEN
		assertThat("Objects are not same", u1.isSameAs(u2), is(equalTo(false)));
		assertThat("Objects are different", u1.differsFrom(u2), is(equalTo(true)));
	}

	@Test
	public void differsFrom_differentNode() {
		// GIVEN
		SnfInvoiceNodeUsage u1 = new SnfInvoiceNodeUsage(1L, 2L, Instant.now().minusSeconds(1),
				BigInteger.valueOf(3L), BigInteger.valueOf(4L), BigInteger.valueOf(5L));
		SnfInvoiceNodeUsage u2 = new SnfInvoiceNodeUsage(1L, 1L, Instant.now(), BigInteger.valueOf(3L),
				BigInteger.valueOf(4L), BigInteger.valueOf(5L));

		// THEN
		assertThat("Objects are not same", u1.isSameAs(u2), is(equalTo(false)));
		assertThat("Objects are different", u1.differsFrom(u2), is(equalTo(true)));
	}

	@Test
	public void differsFrom_differentPropsIn() {
		// GIVEN
		SnfInvoiceNodeUsage u1 = new SnfInvoiceNodeUsage(1L, 2L, Instant.now().minusSeconds(1),
				BigInteger.valueOf(3L), BigInteger.valueOf(4L), BigInteger.valueOf(5L));
		SnfInvoiceNodeUsage u2 = new SnfInvoiceNodeUsage(1L, 2L, Instant.now(), BigInteger.valueOf(30L),
				BigInteger.valueOf(4L), BigInteger.valueOf(5L));

		// THEN
		assertThat("Objects are not same", u1.isSameAs(u2), is(equalTo(false)));
		assertThat("Objects are different", u1.differsFrom(u2), is(equalTo(true)));
	}

	@Test
	public void differsFrom_differentDatumOut() {
		// GIVEN
		SnfInvoiceNodeUsage u1 = new SnfInvoiceNodeUsage(1L, 2L, Instant.now().minusSeconds(1),
				BigInteger.valueOf(3L), BigInteger.valueOf(4L), BigInteger.valueOf(5L));
		SnfInvoiceNodeUsage u2 = new SnfInvoiceNodeUsage(1L, 2L, Instant.now(), BigInteger.valueOf(3L),
				BigInteger.valueOf(40L), BigInteger.valueOf(5L));

		// THEN
		assertThat("Objects are not same", u1.isSameAs(u2), is(equalTo(false)));
		assertThat("Objects are different", u1.differsFrom(u2), is(equalTo(true)));
	}

	@Test
	public void differsFrom_differentDatumDaysStored() {
		// GIVEN
		SnfInvoiceNodeUsage u1 = new SnfInvoiceNodeUsage(1L, 2L, Instant.now().minusSeconds(1),
				BigInteger.valueOf(3L), BigInteger.valueOf(4L), BigInteger.valueOf(5L));
		SnfInvoiceNodeUsage u2 = new SnfInvoiceNodeUsage(1L, 2L, Instant.now(), BigInteger.valueOf(3L),
				BigInteger.valueOf(4L), BigInteger.valueOf(50L));

		// THEN
		assertThat("Objects are not same", u1.isSameAs(u2), is(equalTo(false)));
		assertThat("Objects are different", u1.differsFrom(u2), is(equalTo(true)));
	}

}
