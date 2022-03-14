/* ==================================================================
 * SnfInvoiceItemTests.java - 22/07/2020 9:11:21 AM
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

import static java.math.BigDecimal.ONE;
import static java.time.Instant.now;
import static java.util.Collections.singletonMap;
import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.user.billing.snf.domain.InvoiceItemType.Recurring;
import static net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem.META_USAGE;
import static net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem.newItem;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem;
import net.solarnetwork.central.user.billing.snf.domain.UsageInfo;

/**
 * Test cases for the {@link SnfInvoiceItem} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SnfInvoiceItemTests {

	private static final String TEST_PROD_KEY = UUID.randomUUID().toString();

	@Test
	public void usageMetadata_basic() {
		// GIVEN
		Map<String, Object> usage = new LinkedHashMap<>(3);
		usage.put("unitType", "foo");
		usage.put("amount", "12345");
		usage.put("cost", "123.45");
		SnfInvoiceItem item = newItem(randomUUID().getMostSignificantBits(), Recurring, TEST_PROD_KEY,
				ONE, ONE, now(), singletonMap(META_USAGE, usage));

		// WHEN
		UsageInfo info = item.getUsageInfo();

		// THEN
		assertThat("Usage created", info, notNullValue());
		assertThat("Unit type preserved", info.getUnitType(), equalTo("foo"));
		assertThat("Amount preserved", info.getAmount(), equalTo(new BigDecimal("12345")));
		assertThat("Cost preserved", info.getCost(), equalTo(new BigDecimal("123.45")));
	}

	@Test
	public void usageMetadata_missing() {
		// GIVEN
		SnfInvoiceItem item = newItem(randomUUID().getMostSignificantBits(), Recurring, TEST_PROD_KEY,
				ONE, ONE, now(), singletonMap("foo", "bar"));

		// WHEN
		UsageInfo info = item.getUsageInfo();

		// THEN
		assertThat("Usage missing", info, nullValue());
	}

}
