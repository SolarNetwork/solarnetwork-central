/* ==================================================================
 * InvoiceTests.java - 28/08/2017 7:08:02 AM
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
import net.solarnetwork.central.user.billing.killbill.domain.Invoice;
import net.solarnetwork.util.JsonUtils;

/**
 * Test cases for the {@link Invoice} class.
 * 
 * @author matt
 * @version 1.0
 */
public class InvoiceTests {

	private ObjectMapper objectMapper;

	@Before
	public void setup() {
		objectMapper = KillbillUtils.defaultObjectMapper();
	}

	@Test
	public void serializeToJson() throws Exception {
		Invoice invoice = new Invoice(UUID.randomUUID().toString());
		invoice.setAmount(new BigDecimal("1.20"));
		invoice.setBalance(new BigDecimal("0.12"));
		invoice.setCurrencyCode("NZD");
		invoice.setInvoiceDate(new LocalDate(2017, 1, 1));
		invoice.setTimeZoneId("Pacific/Auckland");

		String json = objectMapper.writeValueAsString(invoice);
		Map<String, Object> data = JsonUtils.getStringMap(json);

		Map<String, Object> expected = JsonUtils.getStringMap("{\"id\":\"" + invoice.getId()
				+ "\",\"currency\":\"NZD\",\"amount\":\"1.20\",\"taxAmount\":\"0\",\"balance\":\"0.12\""
				+ ",\"invoiceDate\":\"2017-01-01\",\"timeZoneId\":\"Pacific/Auckland\",\"created\":"
				+ invoice.getInvoiceDate()
						.toDateTimeAtStartOfDay(DateTimeZone.forID(invoice.getTimeZoneId())).getMillis()
				+ "}");

		assertThat(data, equalTo(expected));

	}

}
