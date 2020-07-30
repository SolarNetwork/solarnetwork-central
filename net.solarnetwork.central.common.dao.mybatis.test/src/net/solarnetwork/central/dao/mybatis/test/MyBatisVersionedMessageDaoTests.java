/* ==================================================================
 * MyBatisVersionedMessageDaoTests.java - 25/07/2020 11:18:08 AM
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

package net.solarnetwork.central.dao.mybatis.test;

import static java.time.Instant.now;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import net.solarnetwork.central.dao.mybatis.MyBatisVersionedMessageDao;

/**
 * Test cases for the {@link MyBatisVersionedMessageDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisVersionedMessageDaoTests extends AbstractMyBatisDaoTestSupport {

	;

	private final ZoneId UTC = ZoneId.of("UTC");
	private MyBatisVersionedMessageDao dao;

	@Before
	public void setup() {
		dao = new MyBatisVersionedMessageDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
	}

	private void insertMessages(Instant version, String bundle, String locale,
			Map<String, String> data) {
		final Iterator<Map.Entry<String, String>> itr = data.entrySet().iterator();
		jdbcTemplate.batchUpdate("insert into solarcommon.messages (vers,bundle,locale,msg_key,msg_val)"
				+ " VALUES (?,?,?,?,?)", new BatchPreparedStatementSetter() {

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						if ( i == 0 ) {
							ps.setTimestamp(1, new Timestamp(version.toEpochMilli()));
							ps.setString(2, bundle);
							ps.setString(3, locale);
						}
						Map.Entry<String, String> e = itr.next();
						ps.setString(4, e.getKey());
						ps.setString(5, e.getValue());
					}

					@Override
					public int getBatchSize() {
						return data.size();
					}
				});
	}

	@Test
	public void find_nothing() {
		// GIVEN

		// WHEN
		Properties result = dao.findMessages(now(), new String[] { "foo" }, "en");

		// THEN
		assertThat("Result not found", result, nullValue());

	}

	@Test
	public void find_noneAvailable() {
		// GIVEN
		Map<String, String> data = new LinkedHashMap<>(4);
		data.put("hello", "world");
		data.put("foo", "bar");
		Instant now = Instant.now();
		insertMessages(now, "foo", "en", data);

		// WHEN
		Properties result = dao.findMessages(now().minusSeconds(10), new String[] { "foo" }, "en");

		// THEN
		assertThat("Result not found", result, nullValue());
	}

	@Test
	public void find_singleVersion() {
		// GIVEN
		Map<String, String> data = new LinkedHashMap<>(4);
		data.put("hello", "world");
		data.put("foo", "bar");
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		insertMessages(now, "foo", "en", data);

		// WHEN
		Properties result = dao.findMessages(now(), new String[] { "foo" }, "en");

		// THEN
		Map<String, String> expected = new LinkedHashMap<>(data.size() + 1);
		expected.put("version", ISO_LOCAL_DATE_TIME.format(now.atZone(UTC)) + "Z");
		expected.putAll(data);
		assertThat("Result matches", result, equalTo(expected));
	}

	@Test
	public void find_updatedVersion_allKeysUpdated() {
		// GIVEN
		Map<String, String> v1Data = new LinkedHashMap<>(4);
		v1Data.put("hello", "world");
		v1Data.put("foo", "bar");
		Instant v1 = Instant.now().truncatedTo(ChronoUnit.SECONDS).minusSeconds(10);
		insertMessages(v1, "foo", "en", v1Data);

		Map<String, String> v2Data = new LinkedHashMap<>(4);
		v2Data.put("hello", "world 2");
		v2Data.put("foo", "bar 2");
		Instant v2 = Instant.now().truncatedTo(ChronoUnit.SECONDS).minusSeconds(9);
		insertMessages(v2, "foo", "en", v2Data);

		// WHEN
		Properties result = dao.findMessages(now(), new String[] { "foo" }, "en");

		// THEN
		Map<String, String> expected = new LinkedHashMap<>(v2Data.size() + 1);
		expected.put("version", ISO_LOCAL_DATE_TIME.format(v2.atZone(UTC)) + "Z");
		expected.putAll(v2Data);
		assertThat("Result matches", result, equalTo(expected));
	}

	@Test
	public void find_updatedVersion_keyAdded() {
		// GIVEN
		Map<String, String> v1Data = new LinkedHashMap<>(4);
		v1Data.put("hello", "world");
		v1Data.put("foo", "bar");
		Instant v1 = Instant.now().truncatedTo(ChronoUnit.SECONDS).minusSeconds(10);
		insertMessages(v1, "foo", "en", v1Data);

		Map<String, String> v2Data = new LinkedHashMap<>(4);
		v2Data.put("hello", "world 2");
		v2Data.put("foo", "bar 2");
		v2Data.put("new", "one");
		Instant v2 = Instant.now().truncatedTo(ChronoUnit.SECONDS).minusSeconds(9);
		insertMessages(v2, "foo", "en", v2Data);

		// WHEN
		Properties result = dao.findMessages(now(), new String[] { "foo" }, "en");

		// THEN
		Map<String, String> expected = new LinkedHashMap<>(v2Data.size() + 1);
		expected.put("version", ISO_LOCAL_DATE_TIME.format(v2.atZone(UTC)) + "Z");
		expected.putAll(v2Data);
		assertThat("Result matches", result, equalTo(expected));
	}

	@Test
	public void find_updatedVersion_keyDropped() {
		// GIVEN
		Map<String, String> v1Data = new LinkedHashMap<>(4);
		v1Data.put("hello", "world");
		v1Data.put("foo", "bar");
		Instant v1 = Instant.now().truncatedTo(ChronoUnit.SECONDS).minusSeconds(10);
		insertMessages(v1, "foo", "en", v1Data);

		Map<String, String> v2Data = new LinkedHashMap<>(4);
		v2Data.put("hello", "world 2");
		Instant v2 = Instant.now().truncatedTo(ChronoUnit.SECONDS).minusSeconds(9);
		insertMessages(v2, "foo", "en", v2Data);

		// WHEN
		Properties result = dao.findMessages(now(), new String[] { "foo" }, "en");

		// THEN
		Map<String, String> expected = new LinkedHashMap<>(v2Data.size() + 1);
		expected.put("version", ISO_LOCAL_DATE_TIME.format(v2.atZone(UTC)) + "Z");
		expected.putAll(v1Data);
		expected.putAll(v2Data);
		assertThat("Result matches", result, equalTo(expected));
	}

}
