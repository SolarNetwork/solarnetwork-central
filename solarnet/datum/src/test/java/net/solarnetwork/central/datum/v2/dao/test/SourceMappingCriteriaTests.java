/* ==================================================================
 * SourceMappingCriteriaTests.java - 4/12/2020 2:44:40 pm
 * 
 * Copyright 2020 SolarNetwork.net Dev Team
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

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import net.solarnetwork.central.datum.v2.dao.SourceMappingCriteria;

/**
 * Test cases for the {@link SourceMappingCriteria} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SourceMappingCriteriaTests {

	@Test
	public void setSourceMaps() {
		// GIVEN
		String[] sourceMaps = new String[] { "GEN:A,B,C", "CON:D" };

		// WHEN
		Map<String, Set<String>> mappings = SourceMappingCriteria.mappingsFrom(sourceMaps);

		// THEN
		Map<String, Set<String>> expected = new LinkedHashMap<String, Set<String>>(2);
		expected.put("GEN", new LinkedHashSet<String>(asList("A", "B", "C")));
		expected.put("CON", new LinkedHashSet<String>(asList("D")));
		assertThat("Source ID mapping", mappings, equalTo(expected));
	}

	@Test
	public void setSourceMapAllErrorsToNull() {
		// GIVEN
		String[] sourceMaps = new String[] { "GEN=1,2,3", "CON=D" };

		// WHEN
		Map<String, Set<String>> mappings = SourceMappingCriteria.mappingsFrom(sourceMaps);

		// THEN
		assertThat("Source ID mapping", mappings, nullValue());
	}

	@Test
	public void setSourceMapsNoColon() {
		// GIVEN
		String[] sourceMaps = new String[] { "GEN=1,2,3", "CON:D" };

		// WHEN
		Map<String, Set<String>> mappings = SourceMappingCriteria.mappingsFrom(sourceMaps);

		// THEN
		Map<String, Set<String>> expected = new LinkedHashMap<String, Set<String>>(2);
		expected.put("CON", new LinkedHashSet<String>(asList("D")));
		assertThat("Source ID mapping", mappings, equalTo(expected));
	}

	@Test
	public void setSourceMapsNoReal() {
		// GIVEN
		String[] sourceMaps = new String[] { "GEN:", "CON:D" };

		// WHEN
		Map<String, Set<String>> mappings = SourceMappingCriteria.mappingsFrom(sourceMaps);

		// THEN
		Map<String, Set<String>> expected = new LinkedHashMap<String, Set<String>>(2);
		expected.put("CON", new LinkedHashSet<String>(asList("D")));
		assertThat("Source ID mapping", mappings, equalTo(expected));
	}

	@Test
	public void setSourceMapsSpringSpecialCase() {
		// GIVEN
		String[] sourceMaps = new String[] { "GEN:A", "B", "C" };

		// WHEN
		Map<String, Set<String>> mappings = SourceMappingCriteria.mappingsFrom(sourceMaps);

		// THEN
		Map<String, Set<String>> expected = new LinkedHashMap<String, Set<String>>(2);
		expected.put("GEN", new LinkedHashSet<String>(asList("A", "B", "C")));
		assertThat("Source ID mapping", mappings, equalTo(expected));
	}

}
