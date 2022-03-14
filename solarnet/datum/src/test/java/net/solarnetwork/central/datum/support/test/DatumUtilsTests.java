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

import static org.hamcrest.MatcherAssert.assertThat;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.util.AntPathMatcher;
import net.solarnetwork.central.datum.support.DatumUtils;

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
		assertThat("Sources filtered", result,
				Matchers.containsInAnyOrder("/CT/TB/S1/GEN/1", "/CT/TB/S1/INV/1", "/power/switch/1",
						"/power/switch/2", "/power/switch/3", "/power/switch/grid"));
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
		assertThat("Sources filtered", result,
				Matchers.containsInAnyOrder("/CT/TB/S1/GEN/1", "/CT/TB/S1/INV/1"));
	}

}
