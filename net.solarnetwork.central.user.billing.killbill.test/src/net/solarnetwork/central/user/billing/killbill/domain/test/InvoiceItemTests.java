/* ==================================================================
 * InvoiceItemTests.java - 30/08/2017 7:11:19 AM
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

package net.solarnetwork.central.user.billing.killbill.domain.test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.user.billing.killbill.KillbillUtils;
import net.solarnetwork.central.user.billing.killbill.domain.InvoiceItem;
import net.solarnetwork.util.JsonUtils;

/**
 * Test cases for the {@link InvoiceItem} class.
 * 
 * @author matt
 * @version 1.0
 */
public class InvoiceItemTests {

	private ObjectMapper objectMapper;

	@Before
	public void setup() {
		objectMapper = KillbillUtils.defaultObjectMapper();
	}

	@Test
	public void serializeToJson() throws Exception {
		InvoiceItem item = new InvoiceItem(UUID.randomUUID().toString());
		item.setAmount(new BigDecimal("1.20"));
		item.setCurrencyCode("NZD");
		item.setStartDate(new LocalDate(2017, 1, 1));
		item.setEndDate(new LocalDate(2017, 2, 1));
		item.setTimeZoneId("Pacific/Auckland");

		String json = objectMapper.writeValueAsString(item);
		Map<String, Object> data = JsonUtils.getStringMap(json);

		Map<String, Object> expected = JsonUtils.getStringMap("{\"id\":\"" + item.getId()
				+ "\",\"currency\":\"NZD\",\"amount\":\"1.20\""
				+ ",\"startDate\":\"2017-01-01\",\"endDate\":\"2017-02-01\""
				+ ",\"timeZoneId\":\"Pacific/Auckland\",\"created\":"
				+ item.getStartDate().toDateTimeAtStartOfDay(DateTimeZone.forID(item.getTimeZoneId()))
						.getMillis()
				+ ",\"ended\":" + item.getEndDate()
						.toDateTimeAtStartOfDay(DateTimeZone.forID(item.getTimeZoneId())).getMillis()
				+ "}");

		assertThat(data, equalTo(expected));

	}

}
