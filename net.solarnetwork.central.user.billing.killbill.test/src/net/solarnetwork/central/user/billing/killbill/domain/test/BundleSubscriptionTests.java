/* ==================================================================
 * BundleSubscriptionTests.java - 23/08/2017 2:57:00 PM
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.user.billing.killbill.KillbillUtils;
import net.solarnetwork.central.user.billing.killbill.domain.Bundle;
import net.solarnetwork.central.user.billing.killbill.domain.BundleSubscription;
import net.solarnetwork.central.user.billing.killbill.domain.Subscription;
import net.solarnetwork.util.JsonUtils;

/**
 * Test cases for the {@link BundleSubscription} class.
 * 
 * @author matt
 * @version 1.0
 */
public class BundleSubscriptionTests {

	private ObjectMapper objectMapper;

	@Before
	public void setup() {
		objectMapper = KillbillUtils.defaultObjectMapper();
	}

	@Test
	public void serializeToJson() throws Exception {
		Bundle bundle = new Bundle();
		bundle.setAccountId("a");
		bundle.setBundleId("b"); // this should be ignored
		bundle.setExternalKey("c");

		Subscription sub = new Subscription();
		sub.setBillCycleDayLocal(1);
		sub.setPlanName("d");
		sub.setProductCategory(Subscription.BASE_PRODUCT_CATEGORY);

		bundle.setSubscriptions(Collections.singletonList(sub));

		String json = objectMapper.writeValueAsString(new BundleSubscription(bundle));
		Map<String, Object> data = JsonUtils.getStringMap(json);

		Map<String, Object> expected = new HashMap<>(5);
		expected.put("accountId", "a");
		expected.put("externalKey", "c");
		expected.put("billCycleDayLocal", 1);
		expected.put("planName", "d");
		expected.put("productCategory", Subscription.BASE_PRODUCT_CATEGORY);

		assertThat(data, equalTo(expected));
	}

}
