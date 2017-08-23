/* ==================================================================
 * SubscriptionUsageTests.java - 23/08/2017 3:27:39 PM
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.user.billing.killbill.domain.Subscription;
import net.solarnetwork.central.user.billing.killbill.domain.SubscriptionUsage;
import net.solarnetwork.central.user.billing.killbill.domain.UsageRecord;
import net.solarnetwork.central.user.billing.killbill.domain.UsageUnitRecord;
import net.solarnetwork.util.JsonUtils;

/**
 * Test cases for the {@link SubscriptionUsage} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SubscriptionUsageTests {

	// use MappingJackson2HttpMessageConverter's ObjectMapper to pick up Joda support
	private static final ObjectMapper OBJECT_MAPPER = new MappingJackson2HttpMessageConverter()
			.getObjectMapper();

	@Test
	public void serializeToJson() throws Exception {
		Subscription subscription = new Subscription("a");

		LocalDate date = new LocalDate(2017, 1, 1);
		List<UsageRecord> usageRecords = Arrays.asList(new UsageRecord(date, new BigDecimal(1)),
				new UsageRecord(date.plusDays(1), new BigDecimal(2)),
				new UsageRecord(date.plusDays(2), new BigDecimal(3)));

		List<UsageUnitRecord> usageUnitRecords = Collections
				.singletonList(new UsageUnitRecord("b", usageRecords));
		SubscriptionUsage usage = new SubscriptionUsage(subscription, usageUnitRecords);

		String json = OBJECT_MAPPER.writeValueAsString(usage);
		Map<String, Object> data = JsonUtils.getStringMap(json);

		Map<String, Object> expected = JsonUtils
				.getStringMap("{\"subscriptionId\":\"a\",\"unitUsageRecords\":["
						+ "{\"unitType\":\"b\",\"usageRecords\":["
						+ "{\"recordDate\":\"2017-01-01\",\"amount\":1},"
						+ "{\"recordDate\":\"2017-01-02\",\"amount\":2},"
						+ "{\"recordDate\":\"2017-01-03\",\"amount\":3}]}]}");

		assertThat(data, equalTo(expected));

	}

}
