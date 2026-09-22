/* ==================================================================
 * SolarUserControllerDependencyTests.java - 22/09/2026 8:14:27 pm
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
import static net.solarnetwork.central.test.aop.ControllerDependencies.reviewed;
import static net.solarnetwork.central.test.aop.ControllerDependencies.unreviewed;
import static net.solarnetwork.central.test.aop.ControllerDependencies.unusedReviews;
import static net.solarnetwork.central.user.config.SolarNetUserConfiguration.USER_INSTRUCTIONS;
import static net.solarnetwork.central.user.config.SolarNetUserConfiguration.USER_SECRETS;
import static org.assertj.core.api.BDDAssertions.then;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import net.solarnetwork.central.biz.AppConfigurationBiz;
import net.solarnetwork.central.biz.UserEventBiz;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.instructor.dao.NodeInstructionDao;
import net.solarnetwork.central.mail.MailService;
import net.solarnetwork.central.reg.web.MyNodesController;
import net.solarnetwork.central.reg.web.ResetPasswordController;
import net.solarnetwork.central.reg.web.api.v1.NodeInstructionController;
import net.solarnetwork.central.reg.web.api.v1.NodeMetadataController;
import net.solarnetwork.central.reg.web.api.v1.UserEventController;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.test.aop.ControllerDependencies.Review;

/**
 * Verify every service dependency of the SolarUser controllers has a security
 * contract, or has been reviewed as not needing one.
 *
 * <p>
 * The profiles of all optional features are active, so the controllers of
 * every feature are verified.
 * </p>
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@ActiveProfiles({ CLOUD_INTEGRATIONS, DATUM_INPUT, DNP3, INSTRUCTION_INPUT, OCPP_V16, OSCP_V20,
		USER_INSTRUCTIONS, USER_SECRETS })
public class SolarUserControllerDependencyTests extends AbstractJUnit5CentralTransactionalTest {

	private static final List<Review> REVIEWS = List.of(
	// @formatter:off
			reviewed(AppConfigurationBiz.class,
					"global application configuration, with no user data"),
			reviewed(MailService.class, MyNodesController.class,
					"notifies the recipient of a node transfer requested or cancelled through"
					+ " NodeOwnershipBiz"),
			reviewed(MailService.class, ResetPasswordController.class,
					"sends a password reset code from RegistrationBiz to the account's email"),
			reviewed(NodeInstructionDao.class, NodeInstructionController.class,
					"re-reads instructions just queued through InstructorBiz, to wait for their"
					+ " results"),
			reviewed(SolarNodeOwnershipDao.class, NodeMetadataController.class,
					"finds the actor's own node IDs for queries without any; SolarNodeMetadataBiz"
					+ " reads the metadata"),
			reviewed(UserEventBiz.class, UserEventController.class,
					"the query user ID is always the actor's; policy node and source restrictions"
					+ " are not applied to events")
			);
	// @formatter:on

	@Test
	public void everyServiceDependencyReviewed() {
		// @formatter:off
		then(unreviewed(applicationContext, SolarUserSecurityContractTests.CONTRACTS.apis(), REVIEWS))
			.as("Every controller service dependency has a security contract or a review")
			.isEmpty()
			;
		// @formatter:on
	}

	@Test
	public void everyReviewUsed() {
		// @formatter:off
		then(unusedReviews(applicationContext, REVIEWS))
			.as("Every review covers a controller service dependency")
			.isEmpty()
			;
		// @formatter:on
	}

}
