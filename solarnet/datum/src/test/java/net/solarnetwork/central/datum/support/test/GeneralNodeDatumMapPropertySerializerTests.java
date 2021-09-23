/* ==================================================================
 * GeneralNodeDatumMapPropertySerializerTests.java - Sep 5, 2014 7:22:06 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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
import static org.hamcrest.Matchers.is;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatum;
import net.solarnetwork.central.datum.support.GeneralNodeDatumMapPropertySerializer;
import net.solarnetwork.domain.datum.DatumSamples;

/**
 * Test cases for the {@link GeneralNodeDatumMapPropertySerializer} class.
 * 
 * @author matt
 * @version 2.0
 */
public class GeneralNodeDatumMapPropertySerializerTests {

	private static final Long TEST_NODE_ID = -1L;
	private static final String TEST_SOURCE_ID = "test.source";
	private static final LocalDateTime TEST_DATE = LocalDateTime.of(2014, 8, 22, 12, 1, 2,
			(int) TimeUnit.MILLISECONDS.toNanos(345));
	private static final Instant TEST_TIMESTAMP = TEST_DATE.toInstant(ZoneOffset.UTC);

	private GeneralNodeDatumMapPropertySerializer serializer;

	private GeneralNodeDatum getTestInstance() {
		GeneralNodeDatum datum = new GeneralNodeDatum();
		datum.setCreated(TEST_TIMESTAMP);
		datum.setNodeId(TEST_NODE_ID);
		datum.setPosted(datum.getCreated());
		datum.setSourceId(TEST_SOURCE_ID);

		DatumSamples samples = new DatumSamples();
		datum.setSamples(samples);

		// some sample data
		Map<String, Number> instants = new HashMap<String, Number>(2);
		instants.put("watts", 231);
		samples.setInstantaneous(instants);

		Map<String, Number> accum = new HashMap<String, Number>(2);
		accum.put("wattHours", 4123);
		samples.setAccumulating(accum);

		return datum;
	}

	private void verifyResults(GeneralNodeDatum d, Map<String, ?> m) {
		assertThat("created", m.get("created"), is(d.getCreated()));
		assertThat("nodeId", m.get("nodeId"), is(d.getNodeId()));
		assertThat("sourceId", m.get("sourceId"), is(d.getSourceId()));
		assertThat("watts", m.get("watts"), is(d.getSamples().getInstantaneousSampleInteger("watts")));
		assertThat("wattHours", m.get("wattHours"),
				is(d.getSamples().getAccumulatingSampleInteger("wattHours")));
	}

	@Before
	public void setup() {
		serializer = new GeneralNodeDatumMapPropertySerializer();
	}

	@Test
	public void serializeTypical() {
		GeneralNodeDatum d = getTestInstance();
		Object result = serializer.serialize(null, null, d);
		assertThat("GND should serialize to Map", result instanceof Map, is(true));
		@SuppressWarnings("unchecked")
		Map<String, ?> m = (Map<String, ?>) result;
		verifyResults(d, m);
	}

	@Test
	public void serializeReporting() {
		GeneralNodeDatum d = getTestInstance();
		ReportingGeneralNodeDatum rd = new ReportingGeneralNodeDatum();
		rd.setCreated(d.getCreated());
		rd.setNodeId(d.getNodeId());
		rd.setSourceId(d.getSourceId());
		rd.setSamples(d.getSamples());
		rd.setLocalDateTime(TEST_DATE);
		Object result = serializer.serialize(null, null, rd);
		assertThat("GND should serialize to Map", result instanceof Map, is(true));
		@SuppressWarnings("unchecked")
		Map<String, ?> m = (Map<String, ?>) result;
		verifyResults(rd, m);
		assertThat("localDate", m.get("localDate"), is(TEST_DATE.toLocalDate()));
		assertThat("localTime", m.get("localTime"), is(TEST_DATE.toLocalTime()));
	}

}
