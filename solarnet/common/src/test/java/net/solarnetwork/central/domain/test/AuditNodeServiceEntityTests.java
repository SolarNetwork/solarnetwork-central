/* ==================================================================
 * AuditNodeServiceEntityTests.java - 22/01/2023 12:19:03 pm
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

package net.solarnetwork.central.domain.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.solarnetwork.central.dao.AuditNodeServiceEntity;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.util.DateUtils;

/**
 * Test cases for the {@link AuditNodeServiceEntity} class.
 * 
 * @author matt
 * @version 1.0
 */
public class AuditNodeServiceEntityTests {

	public ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		objectMapper.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
		return objectMapper;
	}

	@Test
	public void jsonPropertyOrder() throws Exception {
		// GIVEN
		Instant ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		AuditNodeServiceEntity c = AuditNodeServiceEntity.hourlyAuditNodeService(1L, "two", ts, 4L);

		// WHEN
		String json = JsonUtils.getJSONString(c, null);

		// THEN
		assertThat("JSON", json, is(equalTo(
				"{\"ts\":\"%s\",\"nodeId\":%d,\"service\":\"%s\",\"aggregation\":\"%s\",\"count\":%d}"
						.formatted(DateUtils.ISO_DATE_TIME_ALT_UTC.format(ts), c.getNodeId(),
								c.getService(), c.getAggregation().name(), c.getCount()))));
	}

}
