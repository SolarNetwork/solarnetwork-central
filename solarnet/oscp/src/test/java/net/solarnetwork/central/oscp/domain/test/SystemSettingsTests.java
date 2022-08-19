/* ==================================================================
 * SystemSettingsTests.java - 19/08/2022 2:39:23 pm
 * 
 * Copyright 2022 SolarNetwork.net Dev Team
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.oscp.domain.MeasurementStyle;
import net.solarnetwork.central.oscp.domain.SystemSettings;
import oscp.v20.MeasurementConfiguration;
import oscp.v20.RequiredBehaviour;

/**
 * Test cases for the {@link SystemSettings} class.
 * 
 * @author matt
 * @version 1.0
 */
public class SystemSettingsTests {

	@Test
	public void toOscp20() {
		// GIVEN
		SystemSettings s = new SystemSettings(123, EnumSet.of(MeasurementStyle.Continuous));

		// WHEN
		RequiredBehaviour result = s.toOscp20Value();

		// THEN
		assertThat("Result provided", result, is(notNullValue()));
		assertThat("Heartbeat copied", result.getHeartbeatInterval(),
				is(equalTo(s.heartbeatSeconds().doubleValue())));
		assertThat("Measurement styles copied", result.getMeasurementConfiguration(),
				is(equalTo(EnumSet.of(MeasurementConfiguration.CONTINUOUS))));
	}

	@Test
	public void fromOscp20() {
		// GIVEN
		RequiredBehaviour b = new RequiredBehaviour(EnumSet.of(MeasurementConfiguration.INTERMITTENT));
		b.setHeartbeatInterval(345.0);

		// WHEN
		SystemSettings result = SystemSettings.forOscp20Value(b);

		// THEN
		assertThat("Result provided", result, is(notNullValue()));
		assertThat("Heartbeat copied", result.heartbeatSeconds(),
				is(equalTo(b.getHeartbeatInterval().intValue())));
		assertThat("Measurement configurations copied", result.measurementStyles(),
				is(equalTo(EnumSet.of(MeasurementStyle.Intermittent))));
	}

}
