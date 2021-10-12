/* ==================================================================
 * DatumController_MostRecentTests.java - 10/10/2021 6:48:33 PM
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

package net.solarnetwork.central.query.web.api.test;

import static net.solarnetwork.central.datum.v2.support.ObjectDatumStreamMetadataProvider.staticProvider;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertLocation;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUser;
import static net.solarnetwork.central.test.CommonDbTestUtils.insertUserNode;
import static net.solarnetwork.util.DateUtils.ISO_DATE_TIME_ALT_UTC;
import static net.solarnetwork.util.DateUtils.LOCAL_DATE;
import static net.solarnetwork.util.DateUtils.LOCAL_TIME;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import net.solarnetwork.central.datum.v2.dao.jdbc.DatumDbUtils;
import net.solarnetwork.central.datum.v2.domain.BasicObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.domain.Datum;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumStreamMetadata;
import net.solarnetwork.central.datum.v2.support.DatumCsvUtils;
import net.solarnetwork.central.query.SolarQueryApp;
import net.solarnetwork.central.query.web.api.DatumController;
import net.solarnetwork.central.test.AbstractJUnit5CentralTransactionalTest;
import net.solarnetwork.central.test.CommonDbTestUtils;

/**
 * Test cases for the {@link DatumController} {@literal /mostRecent} endpoint.
 * 
 * @author matt
 * @version 1.0
 */
@SpringBootTest
@AutoConfigureMockMvc
public class DatumController_MostRecentTests extends AbstractJUnit5CentralTransactionalTest {

	private static final Long TEST_NODE_ID = 1L;

	private Long userId;
	private Long locId;

	@Autowired
	private MockMvc mvc;

	@BeforeEach
	public void setup() {
		userId = insertUser(jdbcTemplate);
		locId = insertLocation(jdbcTemplate, TEST_LOC_COUNTRY, TEST_TZ);
		CommonDbTestUtils.insertNode(jdbcTemplate, TEST_NODE_ID, locId);
		insertUserNode(jdbcTemplate, userId, TEST_NODE_ID);
	}

	@Test
	public void mostRecent_noData() throws Exception {
		// GIVEN
		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
		queryParams.add("nodeId", TEST_NODE_ID.toString());

		// @formatter:off
		mvc.perform(get("/api/v1/pub/datum/mostRecent")
				.queryParams(queryParams)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success", is(true)))
			.andExpect(jsonPath("$.data.totalResults", is(0)))
			.andExpect(jsonPath("$.data.startingOffset", is(0)))
			.andExpect(jsonPath("$.data.returnedResultCount", is(0)))
			.andExpect(jsonPath("$.data.results").isEmpty())
			;
		// @formatter:on
	}

	@Test
	public void mostRecent_data() throws Exception {
		// GIVEN
		final ObjectDatumStreamMetadata meta = new BasicObjectDatumStreamMetadata(UUID.randomUUID(),
				"UTC", ObjectDatumKind.Node, 1L, "A", new String[] { "watts" },
				new String[] { "wattHours" }, null);
		final List<ObjectDatumStreamMetadata> metas = Collections.singletonList(meta);
		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ssX");
		final List<Datum> datum = DatumCsvUtils.datumResourceList(SolarQueryApp.class,
				"testdata/datum-raw-data-01.csv", staticProvider(metas), formatter);
		DatumDbUtils.insertObjectDatumStreamMetadata(log, jdbcTemplate, metas);
		DatumDbUtils.insertDatum(log, jdbcTemplate, datum);

		MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
		queryParams.add("nodeId", meta.getObjectId().toString());
		queryParams.add("sourceId", meta.getSourceId());

		// WHEN
		// 2019-08-02 12:10:00+12,1,A,0,23029000
		ZonedDateTime expectedDate = formatter.parse("2019-08-02 12:10:00+12", ZonedDateTime::from)
				.withZoneSameInstant(ZoneId.of(TEST_TZ));
		// @formatter:off
		mvc.perform(get("/api/v1/pub/datum/mostRecent")
				.queryParams(queryParams)
				.accept(MediaType.APPLICATION_JSON))
			.andDo(log())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success", is(true)))
			.andExpect(jsonPath("$.data.totalResults", is(1)))
			.andExpect(jsonPath("$.data.startingOffset", is(0)))
			.andExpect(jsonPath("$.data.returnedResultCount", is(1)))
			.andExpect(jsonPath("$.data.results.length()", is(1)))
			.andExpect(jsonPath("$.data.results[0].created", is(ISO_DATE_TIME_ALT_UTC.format(expectedDate))))
			.andExpect(jsonPath("$.data.results[0].localDate", is(LOCAL_DATE.format(expectedDate))))
			.andExpect(jsonPath("$.data.results[0].localTime", is(LOCAL_TIME.format(expectedDate))))
			.andExpect(jsonPath("$.data.results[0].nodeId", is(meta.getObjectId()), Long.class))
			.andExpect(jsonPath("$.data.results[0].sourceId", is(meta.getSourceId())))
			.andExpect(jsonPath("$.data.results[0].watts", is(0), Integer.class))
			.andExpect(jsonPath("$.data.results[0].wattHours", is(23029000L), Long.class))
			;
		// @formatter:on
	}

}
