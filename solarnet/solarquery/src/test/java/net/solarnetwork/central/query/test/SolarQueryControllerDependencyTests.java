/* ==================================================================
 * SolarQueryControllerDependencyTests.java - 22/09/2026 7:44:36 pm
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

import static net.solarnetwork.central.test.aop.ControllerDependencies.reviewed;
import static net.solarnetwork.central.test.aop.ControllerDependencies.unreviewed;
import static net.solarnetwork.central.test.aop.ControllerDependencies.unusedReviews;
import static org.assertj.core.api.BDDAssertions.then;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import net.solarnetwork.central.biz.AppConfigurationBiz;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.query.web.api.AuthTokenController;
import net.solarnetwork.central.query.web.api.NodeMetadataController;
import net.solarnetwork.central.security.web.AuthenticationTokenService;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.test.aop.ControllerDependencies.Review;

/**
 * Verify every service dependency of the SolarQuery controllers has a security
 * contract, or has been reviewed as not needing one.
 *
 * @author matt
 * @version 1.0
 */
@SpringBootTest
public class SolarQueryControllerDependencyTests extends AbstractJUnit5CentralTransactionalTest {

	private static final List<Review> REVIEWS = List.of(
	// @formatter:off
			reviewed(AppConfigurationBiz.class,
					"global application configuration, with no user data"),
			reviewed(AuthenticationTokenService.class, AuthTokenController.class,
					"computes a signing key for the actor's own token, if its policy allows"),
			reviewed(SolarNodeOwnershipDao.class, NodeMetadataController.class,
					"finds the actor's own node IDs for queries without any; SolarNodeMetadataBiz"
					+ " reads the metadata")
			);
	// @formatter:on

	@Test
	public void everyServiceDependencyReviewed() {
		// @formatter:off
		then(unreviewed(applicationContext, SolarQuerySecurityContractTests.CONTRACTS.apis(), REVIEWS))
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
