/* ==================================================================
 * DatumCsvUtilsTests.java - 18/05/2021 5:04:24 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.v2.support.test;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.Test;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.support.DatumCsvUtils;
import net.solarnetwork.domain.GeneralDatumSamplesType;

/**
 * Test cases for the {@link DatumCsvUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumCsvUtilsTests {

	@Test
	public void parseMetadata() throws IOException {
		// GIVEN
		InputStreamReader r = new InputStreamReader(
				getClass().getResourceAsStream("mock-energy-meta-01.csv"), "UTF-8");

		// WHEN
		List<ObjectDatumStreamMetadata> result = DatumCsvUtils.parseCsvMetadata(r, ObjectDatumKind.Node,
				ZoneOffset.UTC);

		// THEN
		assertThat("Result never null", result, is(allOf(notNullValue(), hasSize(1))));
		ObjectDatumStreamMetadata meta = result.get(0);
		assertThat("Meta i prop names", meta.propertyNamesForType(GeneralDatumSamplesType.Instantaneous),
				arrayContaining("watts", "current", "voltage", "frequency", "realPower", "powerFactor",
						"phaseVoltage", "apparentPower", "reactivePower", "tou"));
		assertThat("Meta a prop names", meta.propertyNamesForType(GeneralDatumSamplesType.Accumulating),
				arrayContaining("wattHours", "cost"));
		assertThat("Meta s prop names", meta.propertyNamesForType(GeneralDatumSamplesType.Status),
				nullValue());
		assertThat("Meta time zone", meta.getTimeZoneId(), equalTo("Pacific/Auckland"));
	}

}
