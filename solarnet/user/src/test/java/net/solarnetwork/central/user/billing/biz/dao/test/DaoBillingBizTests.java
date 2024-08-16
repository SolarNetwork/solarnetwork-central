/* ==================================================================
 * DaoBillingBizTests.java - 25/08/2017 3:28:09 PM
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

package net.solarnetwork.central.user.billing.biz.dao.test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import net.solarnetwork.central.test.CommonTestUtils;
import net.solarnetwork.central.user.billing.biz.BillingSystem;
import net.solarnetwork.central.user.billing.biz.dao.DaoBillingBiz;
import net.solarnetwork.central.user.dao.UserDao;

/**
 * Test cases for the {@link DaoBillingBiz} class.
 * 
 * @author matt
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
public class DaoBillingBizTests {

	@Mock
	private UserDao userDao;

	@Mock
	private BillingSystem system1;

	private DaoBillingBiz service;

	@BeforeEach
	public void setup() {
		List<BillingSystem> systems = new ArrayList<>(1);
		systems.add(system1);

		service = new DaoBillingBiz(userDao, systems);
	}

	@Test
	public void systemForKey_found() {
		// GIVEN
		final String key = CommonTestUtils.randomString();
		given(system1.supportsAccountingSystemKey(key)).willReturn(true);

		// WHEN
		BillingSystem result = service.billingSystemForKey(key);

		// THEN
		then(result).as("Accounting system for matching key returned").isSameAs(system1);
	}

	@Test
	public void systemForKey_notFound() {
		// GIVEN
		final String key = CommonTestUtils.randomString();
		given(system1.supportsAccountingSystemKey(key)).willReturn(false);

		// WHEN
		BillingSystem result = service.billingSystemForKey(key);

		// THEN
		then(result).as("Null returned when no matching key found").isNull();
	}

}
