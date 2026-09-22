/* ==================================================================
 * SolarQuerySecurityContractTests.java - 22/09/2026 7:41:17 pm
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

package net.solarnetwork.central.query.test;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import net.solarnetwork.central.aop.test.SolarNodeMetadataBizSecurityContract;
import net.solarnetwork.central.aop.test.UserMetadataBizSecurityContract;
import net.solarnetwork.central.biz.SolarNodeMetadataBiz;
import net.solarnetwork.central.biz.UserMetadataBiz;
import net.solarnetwork.central.datum.aop.test.DatumMetadataBizSecurityContract;
import net.solarnetwork.central.datum.aop.test.DatumStreamMetadataBizSecurityContract;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.biz.DatumStreamMetadataBiz;
import net.solarnetwork.central.query.aop.test.QueryBizSecurityContract;
import net.solarnetwork.central.query.biz.QueryBiz;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.test.aop.AppSecurityContracts;
import net.solarnetwork.central.test.tenant.SecurityContextExtension;
import net.solarnetwork.central.test.tenant.TestTenants;

/**
 * Verify the SolarQuery application applies its security aspects to the
 * services its controllers use.
 *
 * <p>
 * The denied actors of each security contract are verified against the service
 * beans of the application, so this fails if a service is not advised, for
 * example because an aspect's package is not scanned, or because a service is
 * wrapped by one that is not advised.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@ExtendWith(SecurityContextExtension.class)
public class SolarQuerySecurityContractTests extends AbstractJUnit5CentralTransactionalTest {

	/** The security contracts of the services the controllers use. */
	public static final AppSecurityContracts CONTRACTS = new AppSecurityContracts()
	// @formatter:off
			.with(QueryBiz.class, QueryBizSecurityContract::contract)
			.with(DatumMetadataBiz.class, DatumMetadataBizSecurityContract::contract)
			.with(DatumStreamMetadataBiz.class, DatumStreamMetadataBizSecurityContract::contract)
			.with(SolarNodeMetadataBiz.class, SolarNodeMetadataBizSecurityContract::contract)
			.with(UserMetadataBiz.class, UserMetadataBizSecurityContract::contract)
			;
	// @formatter:on

	private TestTenants tenants;

	@BeforeEach
	public void setup() {
		tenants = new TestTenants();
		tenants.insert(jdbcTemplate);
		tenants.insertStreams(jdbcTemplate);
	}

	@TestFactory
	public Stream<DynamicNode> securityContracts() {
		return CONTRACTS.denyTests(applicationContext, tenants);
	}

}
