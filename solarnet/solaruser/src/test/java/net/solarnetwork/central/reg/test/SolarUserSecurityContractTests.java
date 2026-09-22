/* ==================================================================
 * SolarUserSecurityContractTests.java - 22/09/2026 8:02:44 pm
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

package net.solarnetwork.central.reg.test;

import static net.solarnetwork.central.c2c.config.SolarNetCloudIntegrationsConfiguration.CLOUD_INTEGRATIONS;
import static net.solarnetwork.central.din.config.SolarNetDatumInputConfiguration.DATUM_INPUT;
import static net.solarnetwork.central.dnp3.config.SolarNetDnp3Configuration.DNP3;
import static net.solarnetwork.central.inin.config.SolarNetInstructionInputConfiguration.INSTRUCTION_INPUT;
import static net.solarnetwork.central.ocpp.config.SolarNetOcppConfiguration.OCPP_V16;
import static net.solarnetwork.central.oscp.config.SolarNetOscpConfiguration.OSCP_V20;
import static net.solarnetwork.central.user.config.SolarNetUserConfiguration.USER_INSTRUCTIONS;
import static net.solarnetwork.central.user.config.SolarNetUserConfiguration.USER_SECRETS;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import net.solarnetwork.central.aop.test.SolarNodeMetadataBizSecurityContract;
import net.solarnetwork.central.aop.test.UserMetadataBizSecurityContract;
import net.solarnetwork.central.biz.SolarNodeMetadataBiz;
import net.solarnetwork.central.biz.UserMetadataBiz;
import net.solarnetwork.central.datum.aop.test.AuditDatumBizSecurityContract;
import net.solarnetwork.central.datum.aop.test.DatumAuxiliaryBizSecurityContract;
import net.solarnetwork.central.datum.aop.test.DatumMaintenanceBizSecurityContract;
import net.solarnetwork.central.datum.aop.test.DatumMetadataBizSecurityContract;
import net.solarnetwork.central.datum.aop.test.DatumStreamMetadataBizSecurityContract;
import net.solarnetwork.central.datum.biz.AuditDatumBiz;
import net.solarnetwork.central.datum.biz.DatumAuxiliaryBiz;
import net.solarnetwork.central.datum.biz.DatumMaintenanceBiz;
import net.solarnetwork.central.datum.biz.DatumMetadataBiz;
import net.solarnetwork.central.datum.biz.DatumStreamMetadataBiz;
import net.solarnetwork.central.datum.imp.aop.test.DatumImportBizSecurityContract;
import net.solarnetwork.central.datum.imp.biz.DatumImportBiz;
import net.solarnetwork.central.instructor.aop.test.InstructorBizSecurityContract;
import net.solarnetwork.central.instructor.biz.InstructorBiz;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.test.aop.AppSecurityContracts;
import net.solarnetwork.central.test.tenant.SecurityContextExtension;
import net.solarnetwork.central.test.tenant.TestTenants;
import net.solarnetwork.central.user.aop.test.NodeOwnershipBizSecurityContract;
import net.solarnetwork.central.user.aop.test.RegistrationBizSecurityContract;
import net.solarnetwork.central.user.aop.test.UserAlertBizSecurityContract;
import net.solarnetwork.central.user.aop.test.UserBizSecurityContract;
import net.solarnetwork.central.user.aop.test.UserNodeInstructionBizSecurityContract;
import net.solarnetwork.central.user.aop.test.UserSecretBizSecurityContract;
import net.solarnetwork.central.user.billing.aop.test.BillingBizSecurityContract;
import net.solarnetwork.central.user.billing.biz.BillingBiz;
import net.solarnetwork.central.user.biz.NodeOwnershipBiz;
import net.solarnetwork.central.user.biz.RegistrationBiz;
import net.solarnetwork.central.user.biz.UserAlertBiz;
import net.solarnetwork.central.user.biz.UserBiz;
import net.solarnetwork.central.user.biz.UserNodeInstructionBiz;
import net.solarnetwork.central.user.biz.UserSecretBiz;
import net.solarnetwork.central.user.c2c.aop.test.UserCloudIntegrationsBizSecurityContract;
import net.solarnetwork.central.user.c2c.biz.UserCloudIntegrationsBiz;
import net.solarnetwork.central.user.datum.event.aop.test.UserEventHookBizSecurityContract;
import net.solarnetwork.central.user.datum.event.biz.UserEventHookBiz;
import net.solarnetwork.central.user.datum.expire.aop.test.UserDatumDeleteBizSecurityContract;
import net.solarnetwork.central.user.datum.expire.aop.test.UserExpireBizSecurityContract;
import net.solarnetwork.central.user.datum.expire.biz.UserDatumDeleteBiz;
import net.solarnetwork.central.user.datum.expire.biz.UserExpireBiz;
import net.solarnetwork.central.user.datum.export.aop.test.UserExportBizSecurityContract;
import net.solarnetwork.central.user.datum.export.biz.UserExportBiz;
import net.solarnetwork.central.user.datum.flux.aop.test.UserFluxBizSecurityContract;
import net.solarnetwork.central.user.datum.flux.biz.UserFluxBiz;
import net.solarnetwork.central.user.datum.stream.aop.test.UserDatumStreamAliasBizSecurityContract;
import net.solarnetwork.central.user.datum.stream.biz.UserDatumStreamAliasBiz;
import net.solarnetwork.central.user.din.aop.test.UserDatumInputBizSecurityContract;
import net.solarnetwork.central.user.din.biz.UserDatumInputBiz;
import net.solarnetwork.central.user.dnp3.aop.test.UserDnp3BizSecurityContract;
import net.solarnetwork.central.user.dnp3.biz.UserDnp3Biz;
import net.solarnetwork.central.user.inin.aop.test.UserInstructionInputBizSecurityContract;
import net.solarnetwork.central.user.inin.biz.UserInstructionInputBiz;
import net.solarnetwork.central.user.ocpp.aop.test.UserOcppBizSecurityContract;
import net.solarnetwork.central.user.ocpp.biz.UserOcppBiz;
import net.solarnetwork.central.user.oscp.aop.test.UserOscpBizSecurityContract;
import net.solarnetwork.central.user.oscp.biz.UserOscpBiz;

/**
 * Verify the SolarUser application applies its security aspects to the
 * services its controllers use.
 *
 * <p>
 * The denied actors of each security contract are verified against the service
 * beans of the application, with the profiles of all optional features active,
 * so this fails if a service is not advised, for example because an aspect's
 * package is not scanned, or because a service is wrapped by one that is not
 * advised.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@ActiveProfiles({ CLOUD_INTEGRATIONS, DATUM_INPUT, DNP3, INSTRUCTION_INPUT, OCPP_V16, OSCP_V20,
		USER_INSTRUCTIONS, USER_SECRETS })
@ExtendWith(SecurityContextExtension.class)
public class SolarUserSecurityContractTests extends AbstractJUnit5CentralTransactionalTest {

	/** The security contracts of the services the controllers use. */
	public static final AppSecurityContracts CONTRACTS = new AppSecurityContracts()
	// @formatter:off
			.with(UserBiz.class, UserBizSecurityContract::contract)
			.with(RegistrationBiz.class, RegistrationBizSecurityContract::contract)
			.with(NodeOwnershipBiz.class, NodeOwnershipBizSecurityContract::contract)
			.with(UserAlertBiz.class, UserAlertBizSecurityContract::contract)
			.with(UserNodeInstructionBiz.class, UserNodeInstructionBizSecurityContract::contract)
			.with(UserSecretBiz.class, UserSecretBizSecurityContract::contract)
			.with(BillingBiz.class, BillingBizSecurityContract::contract)
			.with(SolarNodeMetadataBiz.class, SolarNodeMetadataBizSecurityContract::contract)
			.with(UserMetadataBiz.class, UserMetadataBizSecurityContract::contract)
			.with(AuditDatumBiz.class, AuditDatumBizSecurityContract::contract)
			.with(DatumAuxiliaryBiz.class, DatumAuxiliaryBizSecurityContract::contract)
			.with(DatumMaintenanceBiz.class, DatumMaintenanceBizSecurityContract::contract)
			.with(DatumMetadataBiz.class, DatumMetadataBizSecurityContract::contract)
			.with(DatumStreamMetadataBiz.class, DatumStreamMetadataBizSecurityContract::contract)
			.with(InstructorBiz.class, InstructorBizSecurityContract::contract)
			.with(DatumImportBiz.class, DatumImportBizSecurityContract::contract)
			.with(UserEventHookBiz.class, UserEventHookBizSecurityContract::contract)
			.with(UserExpireBiz.class, UserExpireBizSecurityContract::contract)
			.with(UserDatumDeleteBiz.class, UserDatumDeleteBizSecurityContract::contract)
			.with(UserFluxBiz.class, UserFluxBizSecurityContract::contract)
			.with(UserDatumStreamAliasBiz.class, UserDatumStreamAliasBizSecurityContract::contract)
			.with(UserExportBiz.class, UserExportBizSecurityContract::contract)
			.with(UserDatumInputBiz.class, UserDatumInputBizSecurityContract::contract)
			.with(UserDnp3Biz.class, UserDnp3BizSecurityContract::contract)
			.with(UserInstructionInputBiz.class, UserInstructionInputBizSecurityContract::contract)
			.with(UserOcppBiz.class, UserOcppBizSecurityContract::contract)
			.with(UserOscpBiz.class, UserOscpBizSecurityContract::contract)
			.with(UserCloudIntegrationsBiz.class, UserCloudIntegrationsBizSecurityContract::contract)
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
