/* ==================================================================
 * OscpInstructionUtilsTests.java - 6/10/2022 7:33:37 pm
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

package net.solarnetwork.central.oscp.util.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.FileCopyUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.nimbusds.jose.util.StandardCharset;
import net.solarnetwork.central.oscp.util.OscpInstructionUtils;
import net.solarnetwork.central.oscp.util.OscpUtils;
import net.solarnetwork.codec.JsonUtils;
import oscp.v20.UpdateGroupCapacityForecast;

/**
 * Test cases for the {@link OscpInstructionUtils} class.
 * 
 * @author matt
 * @version 1.0
 */
public class OscpInstructionUtilsTests {

	private ObjectMapper mapper;
	private JsonSchemaFactory jsonSchemaFactory;

	@BeforeEach
	public void setup() {
		mapper = JsonUtils.newObjectMapper();
		jsonSchemaFactory = OscpUtils.oscpSchemaFactory_v20();
	}

	@Test
	public void mapAction() {
		// GIVEN
		String action = UpdateGroupCapacityForecast.class.getSimpleName();

		// WHEN
		String schemaName = OscpInstructionUtils.OSCP_ACTION_TO_JSON_SCHEMA_NAME.apply(action);

		// THEN
		assertThat("Action mapped", schemaName, is(equalTo(
				"http://www.openchargealliance.org/schemas/oscp/2.0/update-group-capacity-forecast.json")));
	}

	@Test
	public void decodeInstruction_UpdateGroupCapacityForecast() throws IOException {
		// GIVEN
		String json = FileCopyUtils.copyToString(new InputStreamReader(
				getClass().getResourceAsStream("test-oscp-update-group-capacity-forecast-01.json"),
				StandardCharset.UTF_8));

		Map<String, String> params = new HashMap<>(4);
		params.put(OscpInstructionUtils.OSCP_ACTION_PARAM,
				UpdateGroupCapacityForecast.class.getSimpleName());
		params.put("msg", json);

		// WHEN
		Object msg = OscpInstructionUtils.decodeJsonOscp20InstructionMessage(mapper, params,
				jsonSchemaFactory);

		// THEN
		assertThat("Message parsed and validated", msg,
				Matchers.is(instanceOf(UpdateGroupCapacityForecast.class)));

	}

}
