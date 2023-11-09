/* ==================================================================
 * MyBatisDatumExportTaskInfoDaoTests.java - 19/04/2018 11:01:19 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.export.dao.mybatis.test;

import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import net.solarnetwork.central.datum.export.dao.mybatis.MyBatisDatumExportTaskInfoDao;
import net.solarnetwork.central.datum.export.domain.BasicConfiguration;
import net.solarnetwork.central.datum.export.domain.Configuration;
import net.solarnetwork.central.datum.export.domain.DatumExportState;
import net.solarnetwork.central.datum.export.domain.DatumExportTaskInfo;
import net.solarnetwork.central.datum.export.domain.ScheduleType;

/**
 * Test cases for the {@link MyBatisDatumExportTaskInfoDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class MyBatisDatumExportTaskInfoDaoTests extends AbstractMyBatisDaoTestSupport {

	private static final Instant TEST_EXPORT_DATE = LocalDateTime.of(2017, 4, 18, 9, 0, 0)
			.atZone(ZoneOffset.UTC).toInstant();
	private static final String TEST_NAME = "test.name";
	private static final int TEST_HOUR_OFFSET = 1;

	private MyBatisDatumExportTaskInfoDao dao;

	private DatumExportTaskInfo info;
	private String lastTokenId;
	private Long lastUserId;

	@Before
	public void setUp() throws Exception {
		dao = new MyBatisDatumExportTaskInfoDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
		info = null;
	}

	@Test
	public void storeNew() {
		DatumExportTaskInfo info = new DatumExportTaskInfo();
		info.setConfig(new BasicConfiguration(TEST_NAME, ScheduleType.Daily, TEST_HOUR_OFFSET));
		info.setExportDate(TEST_EXPORT_DATE);
		info.setId(UUID.randomUUID());
		info.setStatus(DatumExportState.Queued);
		UUID id = dao.store(info);
		assertThat("Primary key assigned", id, notNullValue());
		assertThat("Primary key matches", id, equalTo(info.getId()));

		// stash results for other tests to use
		this.info = info;
	}

	@Test
	public void storeNew_withToken() {
		// GIVEN
		final Long userId = randomLong();
		setupTestUser(userId);
		this.lastUserId = userId;

		DatumExportTaskInfo info = new DatumExportTaskInfo();
		info.setConfig(new BasicConfiguration(TEST_NAME, ScheduleType.Daily, TEST_HOUR_OFFSET));
		info.setExportDate(TEST_EXPORT_DATE);
		info.setId(UUID.randomUUID());
		info.setStatus(DatumExportState.Queued);

		final String tokenId = randomString();
		this.lastTokenId = tokenId;

		// WHEN
		UUID id = dao.store(info);

		// add user task association (adhoc)
		jdbcTemplate.update("""
				insert into solaruser.user_adhoc_export_task (user_id,schedule,task_id,auth_token)
				values (?,?,?::uuid,?)
				""", userId, ScheduleType.Adhoc.getKey(), id, tokenId);

		// THEN
		then(id).as("Primary key assigned").isNotNull();

		// stash results for other tests to use
		this.info = info;
	}

	@Test
	public void storeNew_withUserExportConfig() {
		// GIVEN
		final Long userId = randomLong();
		setupTestUser(userId);
		this.lastUserId = userId;

		DatumExportTaskInfo info = new DatumExportTaskInfo();
		info.setConfig(new BasicConfiguration(TEST_NAME, ScheduleType.Daily, TEST_HOUR_OFFSET));
		info.setExportDate(TEST_EXPORT_DATE);
		info.setId(UUID.randomUUID());
		info.setStatus(DatumExportState.Queued);

		// WHEN
		UUID id = dao.store(info);

		// add user task association
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement stmt = con.prepareStatement("""
						insert into solaruser.user_export_datum_conf (user_id,cname,delay_mins,schedule)
						values (?,?,?,?)
						returning id
						""", Statement.RETURN_GENERATED_KEYS);
				stmt.setObject(1, userId);
				stmt.setObject(2, randomString());
				stmt.setObject(3, 0);
				stmt.setObject(4, ScheduleType.Monthly.getKey());
				return stmt;
			}
		}, keyHolder);
		jdbcTemplate.update("""
				insert into solaruser.user_export_task (user_id,schedule,export_date,task_id,conf_id)
				values (?,?,?,?::uuid,?)
				""", userId, ScheduleType.Monthly.getKey(), Timestamp.from(Instant.now()), id,
				keyHolder.getKey());

		// THEN
		then(id).as("Primary key assigned").isNotNull();

		// stash results for other tests to use
		this.info = info;
	}

	@Test
	public void storeNew_withUserExportConfig_withToken() {
		// GIVEN
		final Long userId = randomLong();
		setupTestUser(userId);
		this.lastUserId = userId;

		DatumExportTaskInfo info = new DatumExportTaskInfo();
		info.setConfig(new BasicConfiguration(TEST_NAME, ScheduleType.Daily, TEST_HOUR_OFFSET));
		info.setExportDate(TEST_EXPORT_DATE);
		info.setId(UUID.randomUUID());
		info.setStatus(DatumExportState.Queued);

		final String tokenId = randomString();
		this.lastTokenId = tokenId;

		// WHEN
		UUID id = dao.store(info);

		// add user task association
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement stmt = con.prepareStatement("""
						insert into solaruser.user_export_datum_conf
						(user_id,cname,delay_mins,schedule,auth_token)
						values (?,?,?,?,?)
						returning id""", Statement.RETURN_GENERATED_KEYS);
				stmt.setObject(1, userId);
				stmt.setObject(2, randomString());
				stmt.setObject(3, 0);
				stmt.setObject(4, ScheduleType.Monthly.getKey());
				stmt.setObject(5, tokenId);
				return stmt;
			}
		}, keyHolder);
		jdbcTemplate.update("""
				insert into solaruser.user_export_task (user_id,schedule,export_date,task_id,conf_id)
				values (?,?,?,?::uuid,?)
				""", userId, ScheduleType.Monthly.getKey(), Timestamp.from(Instant.now()), id,
				keyHolder.getKey());

		// THEN
		then(id).as("Primary key assigned").isNotNull();

		// stash results for other tests to use
		this.info = info;
	}

	@Test
	public void getByPrimaryKey() {
		storeNew();
		DatumExportTaskInfo info = dao.get(this.info.getId());
		assertThat("Found by PK", info, notNullValue());
		assertThat("PK", info.getId(), equalTo(this.info.getId()));
		assertThat("Created assigned", info.getCreated(), notNullValue());
		assertThat("Modified assigned", info.getModified(), notNullValue());
		assertThat("Export date", TEST_EXPORT_DATE.equals(info.getExportDate()), equalTo(true));
		assertThat("Status", info.getStatus(), equalTo(DatumExportState.Queued));

		assertThat("Config", info.getConfig(), notNullValue());
		assertThat("Config name", info.getConfig().getName(), equalTo(TEST_NAME));
		assertThat("Config schedule", info.getConfig().getSchedule(), equalTo(ScheduleType.Daily));
		assertThat("Config offset", info.getConfig().getHourDelayOffset(), equalTo(TEST_HOUR_OFFSET));
	}

	@Test
	public void getByPrimaryKey_withToken() {
		// GIVEN
		storeNew_withToken();

		// WHEN
		DatumExportTaskInfo info = dao.get(this.info.getId());

		// THEN
		// @formatter:off
		then(info)
				.as("Found by PK")
				.isNotNull()
				.as("PK")
				.returns(this.info.getId(), from(DatumExportTaskInfo::getId))
				.as("Export date")
				.returns(TEST_EXPORT_DATE, from(DatumExportTaskInfo::getExportDate))
				.as("Status queued")
				.returns(DatumExportState.Queued, from(DatumExportTaskInfo::getStatus))
				.as("Token found")
				.returns(this.lastTokenId, from(DatumExportTaskInfo::getTokenId))
				.as("User ID found")
				.returns(this.lastUserId, from(DatumExportTaskInfo::getUserId))
				;
		
		then(info.getConfig())
				.as("Config populated")
				.isNotNull()
				.as("Name saved")
				.returns(TEST_NAME, from(Configuration::getName))
				.as("Schedule saved")
				.returns(ScheduleType.Daily, from(Configuration::getSchedule))
				.as("Offset saved")
				.returns(TEST_HOUR_OFFSET, from(Configuration::getHourDelayOffset))
				;
		// @formatter:on
	}

	@Test
	public void updateResults() {
		storeNew();
		DatumExportTaskInfo info = dao.get(this.info.getId());
		info.setTaskSuccess(Boolean.TRUE);
		info.setMessage("Yee haw!");
		info.setCompleted(Instant.now().truncatedTo(ChronoUnit.MICROS));
		UUID uuid = dao.store(info);
		assertThat("UUID unchanged", uuid, equalTo(info.getId()));

		DatumExportTaskInfo updated = dao.get(info.getId());
		assertThat("Updated instance", updated, not(sameInstance(info)));
		assertThat("Success", updated.getTaskSuccess(), equalTo(true));
		assertThat("Message", updated.getMessage(), equalTo(info.getMessage()));
		assertThat("Completed", updated.getCompleted(), equalTo(info.getCompleted()));
	}

	@Test
	public void getByClaimNoRows() {
		DatumExportTaskInfo info = dao.claimQueuedTask();
		assertThat("Nothing claimed", info, nullValue());
	}

	@Test
	public void getByClaim() {
		storeNew();
		DatumExportTaskInfo info = dao.claimQueuedTask();
		assertThat("Found by claim", info, notNullValue());
		assertThat("PK", info.getId(), equalTo(this.info.getId()));
	}

	@Test
	public void getByClaim_withToken() {
		// GIVEN
		storeNew_withToken();

		// WHEN
		DatumExportTaskInfo info = dao.claimQueuedTask();

		// THEN
		// @formatter:off
		then(info)
			.as("Found claim")
			.isNotNull()
			.as("PK")
			.returns(this.info.getId(), from(DatumExportTaskInfo::getId))
			.as("User ID provided")
			.returns(this.lastUserId, from(DatumExportTaskInfo::getUserId))
			.as("Token provided")
			.returns(this.lastTokenId, from(DatumExportTaskInfo::getTokenId))
			;
		// @formatter:on
	}

	@Test
	public void getByClaim_withUserExportConfig() {
		// GIVEN
		storeNew_withUserExportConfig();

		// WHEN
		DatumExportTaskInfo info = dao.claimQueuedTask();

		// THEN
		// @formatter:off
		then(info)
			.as("Found claim")
			.isNotNull()
			.as("PK")
			.returns(this.info.getId(), from(DatumExportTaskInfo::getId))
			.as("User ID provided")
			.returns(this.lastUserId, from(DatumExportTaskInfo::getUserId))
			.as("Token not available")
			.returns(null, from(DatumExportTaskInfo::getTokenId))
			;
		// @formatter:on
	}

	@Test
	public void getByClaim_withUserExportConfig_withToken() {
		// GIVEN
		storeNew_withUserExportConfig_withToken();

		// WHEN
		DatumExportTaskInfo info = dao.claimQueuedTask();

		// THEN
		// @formatter:off
		then(info)
			.as("Found claim")
			.isNotNull()
			.as("PK")
			.returns(this.info.getId(), from(DatumExportTaskInfo::getId))
			.as("User ID provided")
			.returns(this.lastUserId, from(DatumExportTaskInfo::getUserId))
			.as("Token not available")
			.returns(this.lastTokenId, from(DatumExportTaskInfo::getTokenId))
			;
		// @formatter:on
	}

	@Test
	public void getByClaimNothingLeftToClaim() {
		getByClaim();

		// flush session cache
		dao.getSqlSession().clearCache();

		DatumExportTaskInfo info = dao.claimQueuedTask();
		assertThat("Nothing claimed", info, nullValue());
	}

	@Test
	public void purgeCompletedNoneCompleted() {
		storeNew();
		long result = dao.purgeCompletedTasks(Instant.now());
		assertThat("Delete count", result, equalTo(0L));
	}

	@Test
	public void purgeCompletedNoneExpired() {
		storeNew();
		this.info.setCompleted(Instant.now().truncatedTo(ChronoUnit.MINUTES));
		this.info.setStatus(DatumExportState.Completed);
		dao.store(this.info);
		long result = dao.purgeCompletedTasks(this.info.getCompleted());
		assertThat("Delete count", result, equalTo(0L));
	}

	@Test
	public void purgeCompleted() {
		storeNew();
		this.info.setCompleted(Instant.now().truncatedTo(ChronoUnit.MINUTES));
		this.info.setStatus(DatumExportState.Completed);
		dao.store(this.info);

		storeNew();
		this.info.setCompleted(Instant.now().truncatedTo(ChronoUnit.HOURS));
		this.info.setStatus(DatumExportState.Completed);
		dao.store(this.info);

		long result = dao.purgeCompletedTasks(
				Instant.now().truncatedTo(ChronoUnit.HOURS).plus(1, ChronoUnit.HOURS));
		assertThat("Delete count", result, equalTo(2L));
	}

}
