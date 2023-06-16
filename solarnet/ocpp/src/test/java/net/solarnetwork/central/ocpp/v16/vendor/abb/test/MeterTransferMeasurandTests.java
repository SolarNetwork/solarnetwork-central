/* ==================================================================
 * MeterTransferMeasurandTests.java - 17/06/2023 8:38:17 am
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

package net.solarnetwork.central.ocpp.v16.vendor.abb.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.ocpp.v16.vendor.abb.MeterTransferMeasurand;

/**
 * Test cases for the {@link MeterTransferMeasurandTests} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MeterTransferMeasurandTests {

	@Test
	public void forKey_v_l1() {
		// GIVEN
		String key = "Voltage.L1";

		// WHEN
		MeterTransferMeasurand m = MeterTransferMeasurand.forKey(key);

		// THEN
		assertThat("Parsed key", m, is(notNullValue()));
		assertThat("Extracted name", m.name(), is(equalTo("Voltage")));
		assertThat("Extracted phase", m.phase(), is(equalTo("L1")));
	}

	@Test
	public void forKey_a_l1() {
		// GIVEN
		String key = "Current.L1";

		// WHEN
		MeterTransferMeasurand m = MeterTransferMeasurand.forKey(key);

		// THEN
		assertThat("Parsed key", m, is(notNullValue()));
		assertThat("Extracted name", m.name(), is(equalTo("Current")));
		assertThat("Extracted phase", m.phase(), is(equalTo("L1")));
	}

	@Test
	public void forKey_w_all() {
		// GIVEN
		String key = "Active.Power.ALL";

		// WHEN
		MeterTransferMeasurand m = MeterTransferMeasurand.forKey(key);

		// THEN
		assertThat("Parsed key", m, is(notNullValue()));
		assertThat("Extracted name", m.name(), is(equalTo("Active.Power")));
		assertThat("Extracted phase", m.phase(), is(equalTo("ALL")));
	}

}
