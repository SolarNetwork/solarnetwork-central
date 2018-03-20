/* ==================================================================
 * GeneralNodeAuditDatumTests.java - 22/08/2017 9:27:14 AM
 * 
 * Copyright 2017 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.domain.test;

import static org.junit.Assert.assertEquals;
import java.util.HashMap;
import java.util.Map;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import net.solarnetwork.central.datum.domain.GeneralNodeAuditDatum;
import net.solarnetwork.util.JsonUtils;

/**
 * Test cases for the {@link GeneralNodeAuditDatum} class.
 * 
 * @author matt
 * @version 1.0
 */
public class GeneralNodeAuditDatumTests {

	private static final Long TEST_NODE_ID = -1L;
	private static final String TEST_SOURCE_ID = "test.source";

	private ObjectMapper objectMapper;

	@Before
	public void setup() {
		objectMapper = new ObjectMapper().registerModule(new JodaModule())
				.setSerializationInclusion(Include.NON_NULL)
				.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
	}

	private GeneralNodeAuditDatum getTestInstance() {
		GeneralNodeAuditDatum datum = new GeneralNodeAuditDatum();
		datum.setCreated(new DateTime(2014, 8, 22, 12, 0, 0, 0));
		datum.setNodeId(TEST_NODE_ID);
		datum.setSourceId(TEST_SOURCE_ID);

		Map<String, Object> data = new HashMap<String, Object>(4);
		data.put("count", 231);
		datum.setAuditData(data);

		return datum;
	}

	@Test
	public void serializeJson() throws Exception {
		JsonNode tree = objectMapper.valueToTree(getTestInstance());
		Map<String, Object> data = JsonUtils.getStringMapFromTree(tree);

		Map<String, Object> expected = new HashMap<String, Object>(8);
		expected.put("created", 1408665600000L);
		expected.put("nodeId", -1L);
		expected.put("sourceId", "test.source");
		expected.put("count", 231);

		assertEquals(expected, data);
	}

	@Test
	public void setCountProperty() throws Exception {
		GeneralNodeAuditDatum datum = new GeneralNodeAuditDatum();
		datum.setCount(1L);
		assertEquals(1L, datum.getCount());
		assertEquals(1L, datum.getAuditData().get(GeneralNodeAuditDatum.COUNT_KEY));
	}

}
