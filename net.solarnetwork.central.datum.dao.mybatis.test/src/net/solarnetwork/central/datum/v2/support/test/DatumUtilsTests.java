/* ==================================================================
 * DatumUtilsTests.java - 24/11/2020 3:04:02 pm
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

package net.solarnetwork.central.datum.v2.support.test;

import static net.solarnetwork.util.NumberUtils.decimalArray;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatum;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.domain.BasicNodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.DatumProperties;
import net.solarnetwork.central.datum.v2.domain.NodeDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.support.DatumUtils;
import net.solarnetwork.domain.GeneralDatumSamples;

/**
 * Test cases for the {@link DatumUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class DatumUtilsTests {

	private NodeDatumStreamMetadata newNodeMeta() {
		return new BasicNodeDatumStreamMetadata(UUID.randomUUID(), "Pacific/Auckland", 1L, "a",
				new String[] { "a", "b", "c", "d" }, new String[] { "e", "f", "g" },
				new String[] { "h", "i" });
	}

	private DatumProperties newProps() {
		return DatumProperties.propertiesOf(decimalArray("1.1", "1.2", "1.3", "1.4"),
				decimalArray("2.1", "2.2", "2.3"), new String[] { "a", "b" }, new String[] { "t" });
	}

	private void assertGeneralDatumSamples(GeneralDatumSamples s) {
		assertThat("Instantaneous keys copied", s.getInstantaneous().keySet(),
				containsInAnyOrder("a", "b", "c", "d"));

		assertThat("Accumulating keys copied", s.getAccumulating().keySet(),
				containsInAnyOrder("e", "f", "g"));

		assertThat("Status keys copied", s.getStatus().keySet(), containsInAnyOrder("h", "i"));

		assertThat("Tags copied", s.getTags(), containsInAnyOrder("t"));
	}

	@Test
	public void populateGeneralDatumSamples_typical() {
		// GIVEN
		DatumProperties props = newProps();

		// WHEN
		GeneralDatumSamples s = new GeneralDatumSamples();
		DatumUtils.populateGeneralDatumSamples(s, props, newNodeMeta());

		// THEN
		assertGeneralDatumSamples(s);
	}

	@Test
	public void toGeneralNodeDatum_typical() {
		// GIVEN
		DatumProperties props = newProps();
		DatumEntity datum = new DatumEntity(UUID.randomUUID(),
				Instant.now().truncatedTo(ChronoUnit.SECONDS), Instant.now(), props);
		NodeDatumStreamMetadata meta = newNodeMeta();

		// WHEN
		ReportingGeneralNodeDatum d = DatumUtils.toGeneralNodeDatum(datum, meta);

		// THEN
		assertThat("Node ID copied from meta", d.getNodeId(), equalTo(meta.getNodeId()));
		assertThat("Source ID copied from meta", d.getSourceId(), equalTo(meta.getSourceId()));
		assertThat("Timestamp copied from datum and meta time zone", d.getCreated(),
				equalTo(new DateTime(datum.getTimestamp().toEpochMilli(),
						DateTimeZone.forID(meta.getTimeZoneId()))));
		assertThat("Local date copied from datum and meta time zone", d.getLocalDateTime(),
				equalTo(new DateTime(datum.getTimestamp().toEpochMilli(),
						DateTimeZone.forID(meta.getTimeZoneId())).toLocalDateTime()));
		assertThat("Received date NOT copied from datum (not part of Datum API)", d.getPosted(),
				nullValue());
		assertGeneralDatumSamples(d.getSamples());
	}

}
