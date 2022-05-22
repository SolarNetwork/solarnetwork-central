/* ==================================================================
 * JdbcLocationRequestDaoTests.java - 19/05/2022 2:33:30 pm
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

package net.solarnetwork.central.common.dao.jdbc.test;

import static java.lang.String.format;
import static net.solarnetwork.codec.JsonUtils.getStringMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.common.dao.BasicLocationRequestCriteria;
import net.solarnetwork.central.common.dao.jdbc.JdbcLocationRequestDao;
import net.solarnetwork.central.domain.LocationRequest;
import net.solarnetwork.central.domain.LocationRequestStatus;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SimpleLocation;

/**
 * Test cases for the {@link JdbcLocationRequestDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcLocationRequestDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcLocationRequestDao dao;

	@BeforeEach
	public void setup() {
		dao = new JdbcLocationRequestDao(jdbcTemplate);
	}

	private List<Map<String, Object>> allReqData() {
		List<Map<String, Object>> data = jdbcTemplate.queryForList("select * from solarnet.sn_loc_req");
		log.debug("solarnet.sn_loc_req table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(Collectors.joining("\n\t", "\n\t", "\n")));
		return data;
	}

	@Test
	public void insert() {
		// GIVEN
		LocationRequest req = new LocationRequest();
		req.setUserId(1L);
		req.setStatus(LocationRequestStatus.Submitted);
		req.setJsonData("{\"foo\":\"bar\"}");

		// WHEN
		Long id = dao.save(req);

		// THEN
		assertThat("ID returned from save", id, notNullValue());

		List<Map<String, Object>> data = allReqData();
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row ID matches", row.get("id"), is(equalTo(id)));
		assertThat("Row has creation date assigned", row,
				hasEntry(equalTo("created"), instanceOf(Timestamp.class)));
		assertThat("Row has modified date assigned", row,
				hasEntry(equalTo("modified"), instanceOf(Timestamp.class)));
		assertThat("Row has status coded value", row,
				hasEntry("status", String.valueOf((char) req.getStatus().getCode())));
		assertThat("Row has same JSON data", getStringMap(row.get("jdata").toString()),
				is(equalTo(getStringMap(req.getJsonData()))));
	}

	@Test
	public void update() {
		// GIVEN
		LocationRequest req = new LocationRequest();
		req.setUserId(1L);
		req.setStatus(LocationRequestStatus.Submitted);
		req.setJsonData("{\"foo\":\"bar\"}");
		Long id = dao.save(req);
		req = dao.get(id);

		// WHEN
		req.setStatus(LocationRequestStatus.Rejected);
		req.setJsonData("{\"no\":\"way\"}");
		req.setLocationId(UUID.randomUUID().getLeastSignificantBits());
		req.setMessage(UUID.randomUUID().toString());
		id = dao.save(req);

		// THEN
		assertThat("ID returned from save", id, notNullValue());

		List<Map<String, Object>> data = allReqData();
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row ID matches", row.get("id"), is(equalTo(id)));
		assertThat("Row has creation date assigned", row,
				hasEntry(equalTo("created"), instanceOf(Timestamp.class)));
		assertThat("Row has modified date assigned", row,
				hasEntry(equalTo("modified"), instanceOf(Timestamp.class)));
		assertThat("Row has updated status coded value", row,
				hasEntry("status", String.valueOf((char) req.getStatus().getCode())));
		assertThat("Row has update JSON data", getStringMap(row.get("jdata").toString()),
				is(equalTo(getStringMap(req.getJsonData()))));
		assertThat("Row has location ID updated", row.get("loc_id"), is(equalTo(req.getLocationId())));
		assertThat("Row has message updated", row.get("message"), is(equalTo(req.getMessage())));
	}

	@Test
	public void delete() {
		// GIVEN
		LocationRequest req = new LocationRequest();
		req.setUserId(1L);
		req.setStatus(LocationRequestStatus.Submitted);
		req.setJsonData("{\"foo\":\"bar\"}");
		Long id = dao.save(req);
		LocationRequest entity = dao.get(id);

		// WHEN
		dao.delete(entity);
		LocationRequest notFound = dao.get(entity.getId());

		// THEN
		assertThat("Deleted entity not found by ID", notFound, is(nullValue()));
		List<Map<String, Object>> rows = allReqData();
		assertThat("Row was deleted from table", rows, hasSize(0));
	}

	private static final String INSERT_SQL = "insert into solarnet.sn_loc_req (id, created, modified, user_id, status, jdata, loc_id, message)\n"
			+ "VALUES (?,?,?,?,?,?::jsonb,?,?)";

	@Test
	public void get() {
		// GIVEN
		final Long id = UUID.randomUUID().getLeastSignificantBits();
		final Instant now = Instant.now();
		final Long userId = UUID.randomUUID().getLeastSignificantBits();
		final LocationRequestStatus status = LocationRequestStatus.Duplicate;
		final String json = "{\"yeah\":\"nah\"}";
		final Long locId = UUID.randomUUID().getLeastSignificantBits();
		final String message = "Hi there.";
		jdbcTemplate.update(INSERT_SQL, id, Timestamp.from(now), Timestamp.from(now), userId,
				String.valueOf((char) status.getCode()), json, locId, message);

		// WHEN
		LocationRequest result = dao.get(id);

		// THEN
		assertThat("Entity returned for ID", result, is(notNullValue()));
		assertThat("ID matches", result.getId(), is(equalTo(id)));
		assertThat("Created populated", result.getCreated(), is(equalTo(now)));
		assertThat("Modified populated", result.getModified(), is(equalTo(now)));
		assertThat("User ID matches", result.getUserId(), is(equalTo(userId)));
		assertThat("Status matches", result.getStatus(), is(equalTo(status)));
		assertThat("JSON data matches", getStringMap(result.getJsonData()),
				is(equalTo(getStringMap(json))));
		assertThat("Location ID matches", result.getLocationId(), is(equalTo(locId)));
		assertThat("Message matches", result.getMessage(), is(equalTo(message)));
	}

	@Test
	public void find_user_status() {
		// GIVEN
		final Instant now = Instant.now();
		List<LocationRequest> data = new ArrayList<>();
		for ( int i = 0; i < 12; i++ ) {
			LocationRequest req = new LocationRequest((long) i, now);
			req.setModified(now);
			req.setUserId(i / 4L);
			switch (i % 4) {
				case 0:
					req.setStatus(LocationRequestStatus.Submitted);
					break;
				case 1:
					req.setStatus(LocationRequestStatus.Created);
					break;
				case 2:
					req.setStatus(LocationRequestStatus.Duplicate);
					break;
				default:
					req.setStatus(LocationRequestStatus.Rejected);
			}
			req.setJsonData(String.format("{\"yeah\":%d}", i));
			req.setLocationId(UUID.randomUUID().getLeastSignificantBits());
			req.setMessage(UUID.randomUUID().toString());
			jdbcTemplate.update(INSERT_SQL, req.getId(), Timestamp.from(now), Timestamp.from(now),
					req.getUserId(), String.valueOf((char) req.getStatus().getCode()), req.getJsonData(),
					req.getLocationId(), req.getMessage());
			data.add(req);
		}

		allReqData();

		// WHEN
		BasicLocationRequestCriteria filter = new BasicLocationRequestCriteria();
		filter.setUserIds(new Long[] { 1L, 2L });
		filter.setRequestStatuses(EnumSet.of(LocationRequestStatus.Created,
				LocationRequestStatus.Duplicate, LocationRequestStatus.Rejected));
		FilterResults<LocationRequest, Long> result = dao.findFiltered(filter);

		// THEN
		assertThat("Results returned", result, is(notNullValue()));
		assertThat("Matching count returned results for 2 users x 3 statuses",
				result.getReturnedResultCount(), is(equalTo(6)));

		List<LocationRequest> resultList = StreamSupport.stream(result.spliterator(), false)
				.collect(Collectors.toList());
		int i = 0;
		for ( Long userId : filter.getUserIds() ) {
			for ( int s = 1; s < 4; s++ ) {
				LocationRequest expected = data.get(userId.intValue() * 4 + s);
				LocationRequest returned = resultList.get(i++);
				assertThat(String.format("Returned expected row %d", i), returned,
						is(equalTo(expected)));
			}
		}
	}

	@Test
	public void delete_user_status() {
		// GIVEN
		final Instant now = Instant.now();
		List<LocationRequest> data = new ArrayList<>();
		for ( int i = 0; i < 12; i++ ) {
			LocationRequest req = new LocationRequest((long) i, now);
			req.setModified(now);
			req.setUserId(i / 4L);
			switch (i % 4) {
				case 0:
					req.setStatus(LocationRequestStatus.Submitted);
					break;
				case 1:
					req.setStatus(LocationRequestStatus.Created);
					break;
				case 2:
					req.setStatus(LocationRequestStatus.Duplicate);
					break;
				default:
					req.setStatus(LocationRequestStatus.Rejected);
			}
			req.setJsonData(String.format("{\"yeah\":%d}", i));
			req.setLocationId(UUID.randomUUID().getLeastSignificantBits());
			req.setMessage(UUID.randomUUID().toString());
			jdbcTemplate.update(INSERT_SQL, req.getId(), Timestamp.from(now), Timestamp.from(now),
					req.getUserId(), String.valueOf((char) req.getStatus().getCode()), req.getJsonData(),
					req.getLocationId(), req.getMessage());
			data.add(req);
		}

		allReqData();

		// WHEN
		BasicLocationRequestCriteria filter = new BasicLocationRequestCriteria();
		filter.setUserIds(new Long[] { 0L, 1L });
		filter.setRequestStatuses(EnumSet.of(LocationRequestStatus.Created,
				LocationRequestStatus.Duplicate, LocationRequestStatus.Rejected));
		int result = dao.delete(null, filter);

		// THEN
		assertThat("Deleted rows for 2 users x 3 statuses", result, is(equalTo(6)));

		List<Map<String, Object>> rows = allReqData();
		assertThat("After delete rows reduced by 2 users x 3 statuses", rows, hasSize(data.size() - 6));
		for ( int i = 0; i < 2; i++ ) {
			Map<String, Object> row = rows.get(i);
			assertThat(format("Remaining row for user %d", i), row, hasEntry("id", (long) (i * 4)));
		}
		for ( int i = 2; i < 6; i++ ) {
			Map<String, Object> row = rows.get(i);
			assertThat(format("Remaining row user 2 %d", i), row, hasEntry("id", (long) (i + 6)));
		}
	}

	@Test
	public void find_fts() {
		// GIVEN
		final Instant now = Instant.now();
		LocationRequest req = new LocationRequest(1L, now);
		req.setModified(now);
		req.setUserId(1L);
		req.setStatus(LocationRequestStatus.Submitted);
		req.setJsonData(
				"{\"sourceId\":\"OpenWeatherMap\",\"location\":{\"name\":\"Windy\",\"country\":\"NZ\",\"region\":\"Reginold\",\"stateOrProvince\":\"Stately\",\"locality\":\"Springfield\",\"postalCode\":\"123456789\"}}");
		jdbcTemplate.update(INSERT_SQL, req.getId(), Timestamp.from(now), Timestamp.from(now),
				req.getUserId(), String.valueOf((char) req.getStatus().getCode()), req.getJsonData(),
				req.getLocationId(), req.getMessage());

		LocationRequest req2 = new LocationRequest(2L, now);
		req2.setModified(now);
		req2.setUserId(1L);
		req2.setStatus(LocationRequestStatus.Submitted);
		req2.setJsonData(
				"{\"sourceId\":\"ClosedWeatherMap\",\"location\":{\"name\":\"Still\",\"country\":\"US\",\"region\":\"Benard\",\"stateOrProvince\":\"New York\",\"locality\":\"City McCityface\",\"postalCode\":\"98765\"}}");
		jdbcTemplate.update(INSERT_SQL, req2.getId(), Timestamp.from(now), Timestamp.from(now),
				req2.getUserId(), String.valueOf((char) req2.getStatus().getCode()), req2.getJsonData(),
				req2.getLocationId(), req2.getMessage());

		allReqData();

		for ( String q : new String[] { "openweathermap", "wind", "nz", "reginold", "state",
				"springfield", "123456789" } ) {
			// WHEN
			BasicLocationRequestCriteria filter = new BasicLocationRequestCriteria();
			filter.setUserId(1L);
			SimpleLocation loc = new SimpleLocation();
			loc.setName(q);
			filter.setLocation(loc);
			FilterResults<LocationRequest, Long> result = dao.findFiltered(filter);

			// THEN
			assertThat(format("Results returned for query [%s]", q), result, is(notNullValue()));
			assertThat(format("Matching count returned for query [%s]", q),
					result.getReturnedResultCount(), is(equalTo(1)));

			LocationRequest returned = StreamSupport.stream(result.spliterator(), false)
					.collect(Collectors.toList()).get(0);

			assertThat(format("Returned expected row for query [%s]", q), returned, is(equalTo(req)));
		}
	}
}
