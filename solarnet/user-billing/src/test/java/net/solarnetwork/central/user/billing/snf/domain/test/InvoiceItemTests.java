/* ==================================================================
 * InvoiceItemTests.java - 24/07/2020 2:27:18 PM
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

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.billing.snf.domain.InvoiceItemImpl;
import net.solarnetwork.central.user.billing.snf.domain.InvoiceItemType;
import net.solarnetwork.central.user.billing.snf.domain.NodeUsage;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem;
import net.solarnetwork.central.user.billing.snf.domain.UsageInfo;

/**
 * Test cases for the {@link InvoiceItemImpl} class.
 *
 * @author matt
 * @version 2.0
 */
public class InvoiceItemTests {

	@Test
	public void properties() {
		// GIVEN
		Address addr = new Address(randomLong(), randomString(), randomString(), "NZ",
				"Pacific/Auckland");
		SnfInvoice inv = new SnfInvoice(randomLong(), randomLong(), randomLong(), now(),
				LocalDate.of(2020, 1, 1), LocalDate.of(2020, 2, 1), "NZD");
		inv.setAddress(addr);

		UsageInfo datumOutInfo = new UsageInfo(NodeUsage.DATUM_OUT_KEY, new BigDecimal("1234567890"),
				new BigDecimal("123456.78"));
		SnfInvoiceItem itm = SnfInvoiceItem.newItem(inv, InvoiceItemType.Usage, NodeUsage.DATUM_OUT_KEY,
				datumOutInfo.getAmount(), datumOutInfo.getCost());
		Map<String, Object> itmMeta = new LinkedHashMap<>(4);
		itmMeta.put("nodeId", randomUUID().getMostSignificantBits());
		itmMeta.put("usage", datumOutInfo.toMetadata());
		itm.setMetadata(itmMeta);

		// WHEN
		InvoiceItemImpl item = new InvoiceItemImpl(inv, itm);

		// THEN
		assertThat("ID is UUID string", item.getId(), equalTo(itm.getId().toString()));
		assertThat("Item amount same", item.getAmount(), equalTo(itm.getAmount()));
		assertThat("Item creation same", item.getCreated(), equalTo(itm.getCreated()));
		assertThat("Item currency taken from invoice", item.getCurrencyCode(),
				equalTo(inv.getCurrencyCode()));
		assertThat("Start date taken from invoice", item.getStartDate(),
				equalTo(LocalDate.of(2020, 1, 1)));
		assertThat("End date taken from invoice", item.getEndDate(), equalTo(LocalDate.of(2020, 2, 1)));
		assertThat("Time zone copied from invoice address", item.getTimeZoneId(),
				equalTo(addr.getTimeZoneId()));
		assertThat("Ended derived from end date and time zone", item.getEnded(), equalTo(
				LocalDate.of(2020, 2, 1).atStartOfDay(ZoneId.of(item.getTimeZoneId())).toInstant()));
		assertThat("Item type is enum string value", item.getItemType(),
				equalTo(itm.getItemType().toString().toUpperCase()));
		assertThat("Metadata same", item.getMetadata(), equalTo(itm.getMetadata()));
		assertThat("Item plan name is key", item.getPlanName(), equalTo(itm.getKey()));

		assertThat("Usage record created from metadata", item.getItemUsageRecords(),
				contains(datumOutInfo));
	}

}
