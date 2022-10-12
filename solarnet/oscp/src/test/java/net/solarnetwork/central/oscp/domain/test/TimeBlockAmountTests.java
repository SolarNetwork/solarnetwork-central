/* ==================================================================
 * TimeBlockAmountTests.java - 24/08/2022 2:34:41 pm
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

import static java.time.ZoneOffset.ofHours;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.solarnetwork.central.oscp.domain.MeasurementUnit;
import net.solarnetwork.central.oscp.domain.Phase;
import net.solarnetwork.central.oscp.domain.TimeBlockAmount;
import net.solarnetwork.codec.JsonUtils;

/**
 * Test cases for the {@link TimeBlockAmount} class.
 * 
 * @author matt
 * @version 1.0
 */
public class TimeBlockAmountTests {

	private ObjectMapper mapper;

	@BeforeEach
	public void setup() {
		mapper = JsonUtils.newObjectMapper();
	}

	@Test
	public void fromJson() throws IOException {
		// GIVEN
		String json = """
				{
					"start":"2022-08-24T18:00:00+12",
					"end":"2022-08-25T00:00:00+12",
					"phase":"All",
					"amount":"5.0",
					"unit":"kW"
				}
				""";

		// WHEN
		TimeBlockAmount result = mapper.readValue(json, TimeBlockAmount.class);

		// THEN
		assertThat("JSON parsed", result, is(notNullValue()));
		assertThat("Start", result.start(),
				is(equalTo(LocalDateTime.of(2022, 8, 24, 18, 0).toInstant(ofHours(12)))));
		assertThat("End", result.end(),
				is(equalTo(LocalDateTime.of(2022, 8, 25, 0, 0).toInstant(ofHours(12)))));
		assertThat("Phase", result.phase(), is(equalTo(Phase.All)));
		assertThat("Amount", result.amount(), is(equalTo(new BigDecimal("5.0"))));
		assertThat("Unit", result.unit(), is(equalTo(MeasurementUnit.kW)));
	}

}
