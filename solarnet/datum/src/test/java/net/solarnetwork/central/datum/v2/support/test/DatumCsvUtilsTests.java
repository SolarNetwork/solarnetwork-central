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

import static net.solarnetwork.central.datum.v2.support.DatumCsvUtils.ISO_DATE_OPT_TIME_ALT_HOUR_OFFSET;
import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import net.solarnetwork.central.datum.v2.domain.AggregateDatum;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.support.DatumCsvUtils;
import net.solarnetwork.central.domain.Aggregation;
import net.solarnetwork.domain.datum.DatumSamplesType;

/**
 * Test cases for the {@link DatumCsvUtils} class.
 * 
 * @author matt
 * @version 2.0
 */
public class DatumCsvUtilsTests {

	@Test
	public void parseMetadata() throws IOException {
		// GIVEN
		InputStreamReader r = new InputStreamReader(
				getClass().getResourceAsStream("mock-energy-meta-01.csv"), "UTF-8");

		// WHEN
		List<ObjectDatumStreamMetadata> result = DatumCsvUtils.parseMetadata(r, ObjectDatumKind.Node,
				ZoneOffset.UTC);

		// THEN
		assertThat("Result parsed", result, is(allOf(notNullValue(), hasSize(1))));
		ObjectDatumStreamMetadata meta = result.get(0);
		assertThat("Meta i prop names", meta.propertyNamesForType(DatumSamplesType.Instantaneous),
				arrayContaining("watts", "current", "voltage", "frequency", "realPower", "powerFactor",
						"phaseVoltage", "apparentPower", "reactivePower", "tou"));
		assertThat("Meta a prop names", meta.propertyNamesForType(DatumSamplesType.Accumulating),
				arrayContaining("wattHours", "cost"));
		assertThat("Meta s prop names", meta.propertyNamesForType(DatumSamplesType.Status), nullValue());
		assertThat("Meta time zone", meta.getTimeZoneId(), equalTo("Pacific/Auckland"));
	}

	@Test
	public void parseAggregateDatum_hour() throws IOException {
		// GIVEN
		InputStreamReader r = new InputStreamReader(
				getClass().getResourceAsStream("mock-energy-hour-01.csv"), "UTF-8");

		// WHEN
		List<AggregateDatum> result = DatumCsvUtils.parseAggregateDatum(r, Aggregation.Hour);

		// THEN
		assertThat("Result parsed", result, is(allOf(notNullValue(), hasSize(20))));
		AggregateDatum d = result.get(0);
		assertThat("Datum stream ID", d.getStreamId(),
				is(equalTo(UUID.fromString("44783e61-34c2-4ea8-83be-15d484708c83"))));
		assertThat("Datum timestamp", d.getTimestamp(), is(equalTo(
				ISO_DATE_OPT_TIME_ALT_HOUR_OFFSET.parse("2021-05-17 23:00:00+00", Instant::from))));
		assertThat("Datum agg", d.getAggregation(), is(equalTo(Aggregation.Hour)));
		assertThat("i prop values", d.getProperties().getInstantaneous(),
				is(arrayContaining(decimalArray("6528.083333333", "2.03884575", "230.905399167",
						"49.969080083", "6011.75", "0.999999953", "13.063502917", "6011.75",
						"1.333333333", "11"))));
		assertThat("a prop values", d.getProperties().getAccumulating(),
				is(arrayContaining(decimalArray("1192.249933333", "13.114749267"))));
		assertThat("s prop values", d.getProperties().getStatus(), nullValue());
		assertThat("t prop values", d.getProperties().getTags(), is(arrayContaining("_v2")));
	}

}
