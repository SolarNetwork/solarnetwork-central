/* ==================================================================
 * SnfBillingSystemConfigTests.java - 28 Sept 2026 11:14:38 am
 *
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.billing.snf.config.test;

import static org.assertj.core.api.BDDAssertions.then;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.user.billing.biz.BillingSystem;
import net.solarnetwork.central.user.billing.biz.BillingSystemRegistrar;
import net.solarnetwork.central.user.billing.snf.SnfBillingSystem;
import net.solarnetwork.central.user.billing.snf.SnfInvoicingSystem;
import net.solarnetwork.central.user.billing.snf.config.SnfBillingSystemConfig;

/**
 * Test cases for the {@link SnfBillingSystemConfig} class.
 *
 * @author matt
 * @version 1.0
 */
public class SnfBillingSystemConfigTests {

	/**
	 * Spring matches {@code @Bean} method candidates by the <b>declared</b>
	 * return type of the factory method, without instantiating the bean. A
	 * method declaring a single interface would therefore hide the others the
	 * bean implements, and collection injection points like
	 * {@code List<BillingSystemRegistrar>} would silently come up empty.
	 */
	@Test
	public void snfBillingSystemBeanExposesEveryApi() throws NoSuchMethodException {
		// GIVEN
		final Method beanMethod = SnfBillingSystemConfig.class.getMethod("snfBillingSystem",
				SnfInvoicingSystem.class);

		// THEN
		// @formatter:off
		then(beanMethod.getReturnType())
			.as("Bean method declares the concrete type, so Spring can see every API it implements")
			.isEqualTo(SnfBillingSystem.class)
			.as("Bean is available to BillingSystem injection points")
			.isAssignableTo(BillingSystem.class)
			.as("Bean is available to BillingSystemRegistrar injection points")
			.isAssignableTo(BillingSystemRegistrar.class)
			;
		// @formatter:on
	}

}
