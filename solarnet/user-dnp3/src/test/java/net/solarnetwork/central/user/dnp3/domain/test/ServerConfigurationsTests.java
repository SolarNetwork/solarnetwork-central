/* ==================================================================
 * ServerConfigurationsTests.java - 7/02/2025 3:33:55 pm
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.dnp3.domain.test;

import static java.time.Instant.now;
import static net.solarnetwork.central.test.CommonTestUtils.randomInt;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static org.assertj.core.api.BDDAssertions.then;
import java.util.List;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.dnp3.domain.ServerControlConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerMeasurementConfiguration;
import net.solarnetwork.central.user.dnp3.domain.ServerConfigurations;

/**
 * Test cases for the {@link ServerConfigurations} class.
 *
 * @author matt
 * @version 1.0
 */
public class ServerConfigurationsTests {

	@Test
	public void empty_nulls() {
		ServerConfigurations c = new ServerConfigurations(null, null);
		then(c.isEmpty()).as("Null measurements and controls is empty").isTrue();
	}

	@Test
	public void empty_empty() {
		ServerConfigurations c = new ServerConfigurations(List.of(), List.of());
		then(c.isEmpty()).as("Empty measurements and controls is empty").isTrue();
	}

	@Test
	public void empty_withMeasurement_nullControl() {
		ServerConfigurations c = new ServerConfigurations(List
				.of(new ServerMeasurementConfiguration(randomLong(), randomLong(), randomInt(), now())),
				null);
		then(c.isEmpty()).as("Non-empty measurements is not empty").isFalse();
	}

	@Test
	public void empty_nullMeasurement_withControl() {
		ServerConfigurations c = new ServerConfigurations(null,
				List.of(new ServerControlConfiguration(randomLong(), randomLong(), randomInt(), now())));
		then(c.isEmpty()).as("Non-empty controls is not empty").isFalse();
	}

	@Test
	public void empty_withMeasurement_withControl() {
		ServerConfigurations c = new ServerConfigurations(
				List.of(new ServerMeasurementConfiguration(randomLong(), randomLong(), randomInt(),
						now())),
				List.of(new ServerControlConfiguration(randomLong(), randomLong(), randomInt(), now())));
		then(c.isEmpty()).as("Non-empty measurements and controls is not empty").isFalse();
	}

}
