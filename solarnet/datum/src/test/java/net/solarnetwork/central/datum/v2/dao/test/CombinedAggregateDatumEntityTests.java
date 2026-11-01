/* ==================================================================
 * CombinedAggregateDatumEntityTests.java - 2/11/2026 8:12:04 am
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

package net.solarnetwork.central.datum.v2.dao.test;

import static net.solarnetwork.domain.datum.DatumProperties.propertiesOf;
import static net.solarnetwork.domain.datum.DatumPropertiesStatistics.statisticsOf;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenIllegalArgumentException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.datum.domain.NodeSourcePK;
import net.solarnetwork.central.datum.v2.dao.CombinedAggregateDatumEntity;
import net.solarnetwork.domain.datum.Aggregation;

/**
 * Test cases for the {@link CombinedAggregateDatumEntity} class.
 *
 * @author matt
 * @version 1.0
 */
public class CombinedAggregateDatumEntityTests {

	private static CombinedAggregateDatumEntity entity(Set<NodeSourcePK> contributors) {
		return new CombinedAggregateDatumEntity(UUID.randomUUID(), Instant.now(), Aggregation.Hour,
				propertiesOf(null, null, null, null), statisticsOf(null, null), contributors);
	}

	@Test
	public void clone_keepsContributors() {
		// GIVEN
		final Set<NodeSourcePK> contributors = Set.of(new NodeSourcePK(1L, "a"));
		final CombinedAggregateDatumEntity datum = entity(contributors);

		// WHEN
		final CombinedAggregateDatumEntity result = datum.clone();

		// THEN
		// @formatter:off
		then(result.contributors())
			.as("Contributors kept")
			.isEqualTo(contributors)
			;
		// @formatter:on
	}

	@Test
	public void construct_nullContributors() {
		// THEN
		thenIllegalArgumentException().isThrownBy(() -> entity(null));
	}

}
