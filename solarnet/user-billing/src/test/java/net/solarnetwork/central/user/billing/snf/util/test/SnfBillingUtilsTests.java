/* ==================================================================
 * SnfBillingUtilsTests.java - 6/08/2020 9:06:20 AM
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

package net.solarnetwork.central.user.billing.snf.util.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;
import net.solarnetwork.central.user.billing.snf.util.SnfBillingUtils;

/**
 * Test cases for the {@link SnfBillingUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SnfBillingUtilsTests {

	@Test
	public void formatInvoiceId_null() {
		String res = SnfBillingUtils.invoiceNumForId(null);
		assertThat("Null input results in null output.", res, nullValue());
	}

	@Test
	public void formatInvoiceId_negative() {
		String res = SnfBillingUtils.invoiceNumForId(-1L);
		assertThat("Negative results in unsigned output.", res, equalTo("INV-3W5E11264SGSF"));
	}

	@Test
	public void formatInvoiceId_small() {
		String res = SnfBillingUtils.invoiceNumForId(1L);
		assertThat("Negative results in unsigned output.", res, equalTo("INV-1"));
	}

	@Test
	public void formatInvoiceId_large() {
		String res = SnfBillingUtils.invoiceNumForId(283749283L);
		assertThat("Negative results in unsigned output.", res, equalTo("INV-4OXQCJ"));
	}

	@Test
	public void parseInvoiceNum_null() {
		Long res = SnfBillingUtils.invoiceIdForNum(null);
		assertThat("Null input results in null output.", res, nullValue());
	}

	@Test
	public void parseInvoiceNum_empty() {
		Long res = SnfBillingUtils.invoiceIdForNum("");
		assertThat("Empty input results in null output.", res, nullValue());
	}

	@Test
	public void parseInvoiceNum_garbage() {
		Long res = SnfBillingUtils.invoiceIdForNum("Hello, world.");
		assertThat("Garbage input results in null output.", res, nullValue());
	}

	@Test
	public void parseInvoiceNum_missingId() {
		Long res = SnfBillingUtils.invoiceIdForNum("INV-");
		assertThat("Missing ID input results in null output.", res, nullValue());
	}

	@Test
	public void parseInvoiceNum_garbageId() {
		Long res = SnfBillingUtils.invoiceIdForNum("INV-!D)D(J");
		assertThat("Garbage ID input results in null output.", res, nullValue());
	}

	@Test
	public void parseInvoiceNum_lowerCase() {
		Long res = SnfBillingUtils.invoiceIdForNum("INV-abc123");
		assertThat("Lower case ID input results in null output.", res, equalTo(623698779L));
	}

	@Test
	public void parseInvoiceNum_lupperCase() {
		Long res = SnfBillingUtils.invoiceIdForNum("INV-ABC123");
		assertThat("Upper case ID input results in null output.", res, equalTo(623698779L));
	}

}
