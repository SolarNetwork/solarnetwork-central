/* ==================================================================
 * BulkJsonDataCollectorTests.java - 23/03/2026 11:04:48 am
 * 
 * Copyright 2026 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.in.web.test;

import static java.util.Map.entry;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.JSON;
import static net.solarnetwork.central.datum.v2.support.DatumJsonUtils.DATUM_JSON_OBJECT_MAPPER;
import static net.solarnetwork.central.test.CommonTestUtils.randomInt;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.util.DateUtils.ISO_DATE_TIME_ALT_UTC;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.common.dao.jdbc.CommonDbUtils;
import net.solarnetwork.central.in.web.BulkJsonDataCollector;
import net.solarnetwork.central.test.CommonTestUtils;
import net.solarnetwork.central.test.security.WithMockAuthenticatedNode;
import net.solarnetwork.domain.datum.BasicObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.BasicStreamDatum;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link BulkJsonDataCollector}.
 * 
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("logging-user-event-appender")
public class BulkJsonDataCollectorTests {

	private static final Logger log = LoggerFactory.getLogger(BulkJsonDataCollectorTests.class);

	@Autowired
	private MockMvc mvc;

	@Autowired
	private JdbcOperations jdbcOperations;

	@Test
	@WithMockAuthenticatedNode
	public void post_Datum() throws Exception {
		// GIVEN
		final Instant ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		final String sourceId = randomString();
		final Integer aVal = randomInt();

		final String postJson = """
				[{
					"created": "%s",
					"sourceId": "%s",
					"i": {
						"a": %d
					}
				}]
				""".formatted(ISO_DATE_TIME_ALT_UTC.format(ts), sourceId, aVal);

		// WHEN
		// @formatter:off
		var response = mvc.perform(post("/solarin/bulkCollector.do")
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.content(postJson)
				)
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString()
			;

		// THEN
		then(response)
			.isNotNull()
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", true)
			.node("data")
			.as("Data is object")
			.isObject()
			.as("Data contains datum result list")
			.containsOnlyKeys("datum")
			.node("datum")
			.isArray()
			.as("One result per input datum provided")
			.hasSize(1)
			.element(0)
			.isObject()
			.contains(
					entry("kind", "n"),
					entry("nodeId", 1),
					entry("objectId", 1),
					entry("sourceId", sourceId),
					entry("created", ISO_DATE_TIME_ALT_UTC.format(ts)),
					entry("timestamp", ISO_DATE_TIME_ALT_UTC.format(ts))
				)
			;
		// @formatter:on
	}

	@Test
	@WithMockAuthenticatedNode
	public void post_StreamDatum() throws Exception {
		// GIVEN
		final var streamId = UUID.randomUUID();
		final String sourceId = randomString();
		final var streamMeta = new BasicObjectDatumStreamMetadata(streamId, "UTC", ObjectDatumKind.Node,
				1L, sourceId, new String[] { "a" }, null, null);

		CommonDbUtils.insertObjectDatumStreamMetadata(log, jdbcOperations, List.of(streamMeta));

		final Instant ts = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		final BigDecimal aVal = CommonTestUtils.randomDecimal();
		final var datum = new BasicStreamDatum(streamId, ts,
				DatumProperties.propertiesOf(new BigDecimal[] { aVal }, null, null, null));

		final String postJson = DATUM_JSON_OBJECT_MAPPER.writeValueAsString(List.of(datum));

		// WHEN
		// @formatter:off
		var response = mvc.perform(post("/solarin/bulkCollector.do")
				.accept(MediaType.APPLICATION_JSON)
				.contentType(MediaType.APPLICATION_JSON)
				.content(postJson)
				)
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString()
			;

		// THEN
		then(response)
			.isNotNull()
			.asInstanceOf(JSON)
			.isObject()
			.as("Success result")
			.containsEntry("success", true)
			.node("data")
			.as("Data is object")
			.isObject()
			.as("Data contains datum result list")
			.containsOnlyKeys("datum")
			.node("datum")
			.isArray()
			.as("One result per input datum provided")
			.hasSize(1)
			.element(0)
			.isObject()
			.contains(
					entry("streamId", streamId.toString()),
					entry("timestamp", ISO_DATE_TIME_ALT_UTC.format(ts))
				)
			;
		// @formatter:on
	}

}
