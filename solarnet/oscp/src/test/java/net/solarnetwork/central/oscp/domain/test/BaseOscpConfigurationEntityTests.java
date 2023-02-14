/* ==================================================================
 * BaseOscpConfigurationEntityTests.java - 10/02/2023 3:20:42 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.oscp.domain.test;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.domain.BaseOscpConfigurationEntity;

/**
 * Test cases for the {@link BaseOscpConfigurationEntity} class.
 * 
 * @author matt
 * @version 1.0
 */
public class BaseOscpConfigurationEntityTests {

	private static final class TestBaseOscpConfigurationEntity
			extends BaseOscpConfigurationEntity<TestBaseOscpConfigurationEntity> {

		private static final long serialVersionUID = 3624026527561008030L;

		private TestBaseOscpConfigurationEntity(Long userId, Long entityId, Instant created) {
			super(userId, entityId, created);
		}

		@Override
		public TestBaseOscpConfigurationEntity copyWithId(UserLongCompositePK id) {
			return null;
		}

	}

	private static TestBaseOscpConfigurationEntity entity() {
		return new TestBaseOscpConfigurationEntity(randomUUID().getMostSignificantBits(),
				randomUUID().getMostSignificantBits(), Instant.now());
	}

	@Test
	public void putServiceProperty() {
		// GIVEN
		TestBaseOscpConfigurationEntity entity = entity();

		// WHEN
		entity.putServiceProp("foo", "bar");

		// THEN
		assertThat("Service properties created with new value", entity.getServiceProps(),
				hasEntry("foo", "bar"));
	}

	@Test
	public void putServiceProperty_nullKey() {
		assertThrows(IllegalArgumentException.class, () -> {
			entity().putServiceProp(null, "bar");
		}, "Null key argument throws exception");
	}

	@Test
	public void putServiceProperty_add() {
		// GIVEN
		TestBaseOscpConfigurationEntity entity = entity();
		Map<String, Object> props = new HashMap<>(4);
		props.put("foo", "bar");
		entity.setServiceProps(props);

		// WHEN
		entity.putServiceProp("bim", "bam");

		// THEN
		assertThat("Service properties augmented with new value", entity.getServiceProps(),
				allOf(hasEntry("foo", "bar"), hasEntry("bim", "bam")));
	}

	@Test
	public void putServiceProperty_replaced() {
		// GIVEN
		TestBaseOscpConfigurationEntity entity = entity();
		Map<String, Object> props = new HashMap<>(4);
		props.put("foo", "bar");
		entity.setServiceProps(props);

		// WHEN
		entity.putServiceProp("foo", "bam");

		// THEN
		assertThat("Service properties augmented with new value", entity.getServiceProps(),
				hasEntry("foo", "bam"));
	}

	@Test
	public void putServiceProperty_remove_noProps() {
		// GIVEN
		TestBaseOscpConfigurationEntity entity = entity();

		// WHEN
		entity.putServiceProp("foo", null);

		// THEN
		assertThat("Service properties remains null", entity.getServiceProps(), is(nullValue()));
	}

	@Test
	public void putServiceProperty_remove_missingProps() {
		// GIVEN
		TestBaseOscpConfigurationEntity entity = entity();
		Map<String, Object> props = new HashMap<>(4);
		props.put("foo", "bar");
		entity.setServiceProps(props);

		// WHEN
		entity.putServiceProp("bim", null);

		// THEN
		assertThat("Service properties remains unchanged", entity.getServiceProps(),
				hasEntry("foo", "bar"));
	}

	@Test
	public void putServiceProperty_remove() {
		// GIVEN
		TestBaseOscpConfigurationEntity entity = entity();
		Map<String, Object> props = new HashMap<>(4);
		props.put("foo", "bar");
		entity.setServiceProps(props);

		// WHEN
		entity.putServiceProp("foo", null);

		// THEN
		assertThat("Service property removed", entity.getServiceProps().keySet(), hasSize(0));
	}

}
