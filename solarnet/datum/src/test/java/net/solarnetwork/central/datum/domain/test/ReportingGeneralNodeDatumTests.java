/* ==================================================================
 * ReportingGeneralNodeDatumTests.java - 2/11/2026 8:12:04 am
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

package net.solarnetwork.central.datum.domain.test;

import static org.assertj.core.api.BDDAssertions.then;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatum;

/**
 * Test cases for the {@link ReportingGeneralNodeDatum} class.
 *
 * @author matt
 * @version 1.0
 */
public class ReportingGeneralNodeDatumTests {

	private final ReportingGeneralNodeDatum datum = new ReportingGeneralNodeDatum(1L, Instant.now(),
			"a");

	@Test
	public void contributors_default() {
		// THEN
		// @formatter:off
		then(datum.contributors())
			.as("No contributors by default")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void setContributors() {
		// GIVEN
		final Set<NodeSourcePK> contributors = Set.of(new NodeSourcePK(1L, "a"));

		// WHEN
		datum.setContributors(contributors);

		// THEN
		// @formatter:off
		then(datum.contributors())
			.as("Contributors saved")
			.isSameAs(contributors)
			;
		// @formatter:on
	}

}
