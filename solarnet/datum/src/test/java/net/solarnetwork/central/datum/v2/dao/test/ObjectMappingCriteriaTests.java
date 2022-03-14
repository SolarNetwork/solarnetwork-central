/* ==================================================================
 * ObjectMappingCriteriaTests.java - 4/12/2020 2:49:08 pm
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
import net.solarnetwork.central.datum.v2.dao.ObjectMappingCriteria;

/**
 * Test cases for the {@link ObjectMappingCriteria} class.
 * 
 * @author matt
 * @version 1.0
 */
public class ObjectMappingCriteriaTests {

	@Test
	public void setNodeMaps() {
		// GIVEN
		String[] nodeMaps = new String[] { "100:1,2,3", "200:4" };

		// WHEN
		Map<Long, Set<Long>> mappings = ObjectMappingCriteria.mappingsFrom(nodeMaps);

		// THEN
		Map<Long, Set<Long>> expected = new LinkedHashMap<Long, Set<Long>>(2);
		expected.put(100L, new LinkedHashSet<Long>(asList(1L, 2L, 3L)));
		expected.put(200L, new LinkedHashSet<Long>(asList(4L)));
		assertThat("Node ID mapping", mappings, equalTo(expected));
	}

	@Test
	public void setNodeMapAllErrorsToNull() {
		// GIVEN
		String[] nodeMaps = new String[] { "100=1,2,3", "200=4" };

		// WHEN
		Map<Long, Set<Long>> mappings = ObjectMappingCriteria.mappingsFrom(nodeMaps);

		// THEN
		assertThat("Node ID mapping", mappings, nullValue());
	}

	@Test
	public void setNodeMapsNoColon() {
		// GIVEN
		String[] nodeMaps = new String[] { "100=1,2,3", "200:4" };

		// WHEN
		Map<Long, Set<Long>> mappings = ObjectMappingCriteria.mappingsFrom(nodeMaps);

		// THEN
		Map<Long, Set<Long>> expected = new LinkedHashMap<Long, Set<Long>>(2);
		expected.put(200L, new LinkedHashSet<Long>(asList(4L)));
		assertThat("Node ID mapping", mappings, equalTo(expected));
	}

	@Test
	public void setNodeMapsNoReal() {
		// GIVEN
		String[] nodeMaps = new String[] { "100:", "200:4" };

		// WHEN
		Map<Long, Set<Long>> mappings = ObjectMappingCriteria.mappingsFrom(nodeMaps);

		// THEN
		Map<Long, Set<Long>> expected = new LinkedHashMap<Long, Set<Long>>(2);
		expected.put(200L, new LinkedHashSet<Long>(asList(4L)));
		assertThat("Node ID mapping", mappings, equalTo(expected));
	}

	@Test
	public void setNodeMapsSpringSpecialCase() {
		// GIVEN
		String[] nodeMaps = new String[] { "100:1", "2", "3" };

		// WHEN
		Map<Long, Set<Long>> mappings = ObjectMappingCriteria.mappingsFrom(nodeMaps);

		// THEN
		Map<Long, Set<Long>> expected = new LinkedHashMap<Long, Set<Long>>(2);
		expected.put(100L, new LinkedHashSet<Long>(asList(1L, 2L, 3L)));
		assertThat("Node ID mapping", mappings, equalTo(expected));
	}
}
