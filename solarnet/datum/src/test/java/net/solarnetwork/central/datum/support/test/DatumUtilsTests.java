/* ==================================================================
 * DatumUtilsTests.java - 7/10/2019 3:25:02 pm
 *
 * Copyright 2019 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.support.test;

import static java.time.Instant.now;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.util.AntPathMatcher;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.support.DatumUtils;
import net.solarnetwork.domain.datum.DatumSamples;
import net.solarnetwork.domain.datum.GeneralDatum;

/**
 * Test cases for the {@link DatumUtils} class.
 *
 * @author matt
 * @version 1.0
 */
public class DatumUtilsTests {

	@Test
	public void filterSources_startWithSlash() {
		// GIVEN
		Set<String> sources = new LinkedHashSet<>(
				Arrays.asList("/CT/TB/S1/GEN/1", "/CT/TB/S1/INV/1", "/power/switch/1", "/power/switch/2",
						"/power/switch/3", "/power/switch/grid", "ADAM-4118", "thermistor"));

		// WHEN
		Set<String> result = DatumUtils.filterSources(sources, new AntPathMatcher(), "/**");

		// THEN
		// @formatter:off
		then(result)
			.as("Sources filtered")
			.contains("/CT/TB/S1/GEN/1", "/CT/TB/S1/INV/1", "/power/switch/1",
					"/power/switch/2", "/power/switch/3", "/power/switch/grid")
			;
		// @formatter:on
	}

	@Test
	public void filterSources_startWith2Char() {
		// GIVEN
		Set<String> sources = new LinkedHashSet<>(
				Arrays.asList("/CT/TB/S1/GEN/1", "/CT/TB/S1/INV/1", "/power/switch/1", "/power/switch/2",
						"/power/switch/3", "/power/switch/grid", "ADAM-4118", "thermistor"));

		// WHEN
		Set<String> result = DatumUtils.filterSources(sources, new AntPathMatcher(), "/??/**");

		// THEN
		// @formatter:off
		then(result)
			.as("Sources filtered")
			.contains("/CT/TB/S1/GEN/1", "/CT/TB/S1/INV/1")
			;
		// @formatter:on
	}

	@Test
	public void convertDatum_node() {
		// GIVEN
		DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue("i", 1);
		s.putAccumulatingSampleValue("a", 2);
		s.putStatusSampleValue("s", "3");
		s.addTag("t");
		GeneralDatum d = GeneralDatum.nodeDatum(randomLong(), randomString(), now(), s);

		// WHEN
		Object result = DatumUtils.convertGeneralDatum(d);

		// THEN
		// @formatter:off
		then(result)
			.asInstanceOf(type(GeneralNodeDatum.class))
			.as("Node ID populated")
			.returns(d.getObjectId(), GeneralNodeDatum::getNodeId)
			.as("Source ID populated")
			.returns(d.getSourceId(), GeneralNodeDatum::getSourceId)
			.as("Timestamp populated")
			.returns(d.getTimestamp(), GeneralNodeDatum::getCreated)
			.as("Samples populated")
			.returns(s, GeneralNodeDatum::getSamples)
			;
		// @formatter:on
	}

	@Test
	public void convertDatum_location() {
		// GIVEN
		DatumSamples s = new DatumSamples();
		s.putInstantaneousSampleValue("i", 1);
		s.putAccumulatingSampleValue("a", 2);
		s.putStatusSampleValue("s", "3");
		s.addTag("t");
		GeneralDatum d = GeneralDatum.locationDatum(randomLong(), randomString(), now(), s);

		// WHEN
		Object result = DatumUtils.convertGeneralDatum(d);

		// THEN
		// @formatter:off
		then(result)
			.asInstanceOf(type(GeneralLocationDatum.class))
			.as("Location ID populated")
			.returns(d.getObjectId(), GeneralLocationDatum::getLocationId)
			.as("Source ID populated")
			.returns(d.getSourceId(), GeneralLocationDatum::getSourceId)
			.as("Timestamp populated")
			.returns(d.getTimestamp(), GeneralLocationDatum::getCreated)
			.as("Samples populated")
			.returns(s, GeneralLocationDatum::getSamples)
			;
		// @formatter:on
	}

}
