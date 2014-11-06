/* ==================================================================
 * GeneralLocationDatumMapPropertySerializerTests.java - Oct 17, 2014 2:37:14 PM
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

import java.util.HashMap;
import java.util.Map;
import net.solarnetwork.central.datum.domain.GeneralLocationDatum;
import net.solarnetwork.central.datum.domain.ReportingGeneralLocationDatum;
import net.solarnetwork.central.datum.support.GeneralLocationDatumMapPropertySerializer;
import net.solarnetwork.domain.GeneralLocationDatumSamples;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for the {@link GeneralLocationDatumMapPropertySerializer} class.
 * 
 * @author matt
 * @version 1.0
 */
public class GeneralLocationDatumMapPropertySerializerTests {

	private static final Long TEST_NODE_ID = -1L;
	private static final String TEST_SOURCE_ID = "test.source";

	private GeneralLocationDatumMapPropertySerializer serializer;

	private GeneralLocationDatum getTestInstance() {
		GeneralLocationDatum datum = new GeneralLocationDatum();
		datum.setCreated(new DateTime(2014, 8, 22, 12, 0, 0, 0));
		datum.setLocationId(TEST_NODE_ID);
		datum.setPosted(datum.getCreated());
		datum.setSourceId(TEST_SOURCE_ID);

		GeneralLocationDatumSamples samples = new GeneralLocationDatumSamples();
		datum.setSamples(samples);

		// some sample data
		Map<String, Number> instants = new HashMap<String, Number>(2);
		instants.put("temp", 231);
		samples.setInstantaneous(instants);

		Map<String, Number> accum = new HashMap<String, Number>(2);
		accum.put("rotations", 4123);
		samples.setAccumulating(accum);

		return datum;
	}

	private void verifyResults(GeneralLocationDatum d, Map<String, ?> m) {
		Assert.assertEquals("created", d.getCreated(), m.get("created"));
		Assert.assertEquals("locationId", d.getLocationId(), m.get("locationId"));
		Assert.assertEquals("sourceId", d.getSourceId(), m.get("sourceId"));
		Assert.assertEquals("temp", d.getSamples().getInstantaneousSampleInteger("temp"), m.get("temp"));
		Assert.assertEquals("temp", d.getSamples().getAccumulatingSampleInteger("rotations"),
				m.get("rotations"));
	}

	@Before
	public void setup() {
		serializer = new GeneralLocationDatumMapPropertySerializer();
	}

	@Test
	public void serializeTypical() {
		GeneralLocationDatum d = getTestInstance();
		Object result = serializer.serialize(null, null, d);
		Assert.assertTrue("GLD should serialize to Map", result instanceof Map);
		@SuppressWarnings("unchecked")
		Map<String, ?> m = (Map<String, ?>) result;
		verifyResults(d, m);
	}

	@Test
	public void serializeReporting() {
		GeneralLocationDatum d = getTestInstance();
		ReportingGeneralLocationDatum rd = new ReportingGeneralLocationDatum();
		rd.setCreated(d.getCreated());
		rd.setLocationId(d.getLocationId());
		rd.setSourceId(d.getSourceId());
		rd.setSamples(d.getSamples());
		rd.setLocalDateTime(new LocalDateTime(2014, 8, 23, 0, 0, 0, 0));
		Object result = serializer.serialize(null, null, rd);
		Assert.assertTrue("GLD should serialize to Map", result instanceof Map);
		@SuppressWarnings("unchecked")
		Map<String, ?> m = (Map<String, ?>) result;
		verifyResults(rd, m);
		Assert.assertEquals("localDate", rd.getLocalDate(), m.get("localDate"));
		Assert.assertEquals("localTime", rd.getLocalTime(), m.get("localTime"));
	}

}
