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

import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.and;
import static org.assertj.core.api.BDDAssertions.from;
import static org.assertj.core.api.BDDAssertions.then;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
 * @version 1.1
 */
@SuppressWarnings("static-access")
public class MyBatisDatumExportTaskInfoDaoTests extends AbstractMyBatisDaoTestSupport {

	private static final Instant TEST_EXPORT_DATE = LocalDateTime.of(2017, 4, 18, 9, 0, 0)
			.atZone(ZoneOffset.UTC).toInstant();
	private static final String TEST_NAME = "test.name";
	private static final int TEST_HOUR_OFFSET = 1;

	private MyBatisDatumExportTaskInfoDao dao;

	private Long userId;
	private DatumExportTaskInfo info;

	@BeforeEach
	public void setUp() throws Exception {
		dao = new MyBatisDatumExportTaskInfoDao();
		dao.setSqlSessionFactory(getSqlSessionFactory());
		info = null;
		userId = randomLong();
		setupTestUser(userId);
	}

	private DatumExportTaskInfo newTaskInfo() {
		DatumExportTaskInfo info = new DatumExportTaskInfo(randomUUID());
		info.setConfig(new BasicConfiguration(TEST_NAME, ScheduleType.Daily, TEST_HOUR_OFFSET));
		info.setExportDate(TEST_EXPORT_DATE);
		info.setStatus(DatumExportState.Queued);
		info.setUserId(userId);
		return info;
	}

	/**
	 * Store a task with no token, as created from a cookie authenticated
	 * SolarUser session.
	 */
	@Test
	public void storeNew() {
		// GIVEN
		DatumExportTaskInfo info = newTaskInfo();

		// WHEN
		UUID id = dao.save(info);

		// THEN
		// @formatter:off
		then(id)
			.as("Primary key assigned")
			.isNotNull()
			.as("Primary key matches")
			.isEqualTo(info.getId())
			;
		// @formatter:on

		// stash results for other tests to use
		this.info = info;
	}

	/**
	 * Store a task with a token, as created from a token authenticated SolarUser
	 * API request.
	 */
	@Test
	public void storeNew_withToken() {
		// GIVEN
		DatumExportTaskInfo info = newTaskInfo();
		info.setTokenId(randomString());

		// WHEN
		UUID id = dao.save(info);

		// THEN
		// @formatter:off
		then(id)
			.as("Primary key assigned")
			.isNotNull()
			.as("Primary key matches")
			.isEqualTo(info.getId())
			;
		// @formatter:on

		// stash results for other tests to use
		this.info = info;
	}

	@Test
	public void getByPrimaryKey() {
		// GIVEN
		storeNew();

		// WHEN
		DatumExportTaskInfo info = dao.get(this.info.getId());

		// THEN
		// @formatter:off
		then(info)
			.as("Found by PK")
			.isNotNull()
			.as("PK")
			.returns(this.info.getId(), from(DatumExportTaskInfo::getId))
			.as("Created assigned")
			.returns(true, i -> i.getCreated() != null)
			.as("Modified assigned")
			.returns(true, i -> i.getModified() != null)
			.as("Export date")
			.returns(TEST_EXPORT_DATE, from(DatumExportTaskInfo::getExportDate))
			.as("Status queued")
			.returns(DatumExportState.Queued, from(DatumExportTaskInfo::getStatus))
			.as("User ID found")
			.returns(userId, from(DatumExportTaskInfo::getUserId))
			.as("No token available")
			.returns(null, from(DatumExportTaskInfo::getTokenId))
			;

		and.then(info.getConfig())
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
			.as("User ID found")
			.returns(userId, from(DatumExportTaskInfo::getUserId))
			.as("Token found")
			.returns(this.info.getTokenId(), from(DatumExportTaskInfo::getTokenId))
			;

		and.then(info.getConfig())
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
		// GIVEN
		storeNew_withToken();
		DatumExportTaskInfo info = dao.get(this.info.getId());

		// WHEN
		info.setTaskSuccess(Boolean.TRUE);
		info.setMessage("Yee haw!");
		info.setCompleted(Instant.now().truncatedTo(ChronoUnit.MICROS));
		UUID uuid = dao.save(info);

		// THEN
		then(uuid).as("UUID unchanged").isEqualTo(info.getId());

		DatumExportTaskInfo updated = dao.get(info.getId());
		// @formatter:off
		then(updated)
			.as("Updated instance")
			.isNotSameAs(info)
			.as("Success")
			.returns(Boolean.TRUE, from(DatumExportTaskInfo::getTaskSuccess))
			.as("Message")
			.returns(info.getMessage(), from(DatumExportTaskInfo::getMessage))
			.as("Completed")
			.returns(info.getCompleted(), from(DatumExportTaskInfo::getCompleted))
			.as("User ID preserved by status update")
			.returns(userId, from(DatumExportTaskInfo::getUserId))
			.as("Token preserved by status update")
			.returns(this.info.getTokenId(), from(DatumExportTaskInfo::getTokenId))
			;
		// @formatter:on
	}

	@Test
	public void getByClaimNoRows() {
		// WHEN
		DatumExportTaskInfo info = dao.claimQueuedTask();

		// THEN
		then(info).as("Nothing claimed").isNull();
	}

	@Test
	public void getByClaim() {
		// GIVEN
		storeNew();

		// WHEN
		DatumExportTaskInfo info = dao.claimQueuedTask();

		// THEN
		// @formatter:off
		then(info)
			.as("Found by claim")
			.isNotNull()
			.as("PK")
			.returns(this.info.getId(), from(DatumExportTaskInfo::getId))
			.as("Export date provided")
			.returns(TEST_EXPORT_DATE, from(DatumExportTaskInfo::getExportDate))
			.as("User ID provided")
			.returns(userId, from(DatumExportTaskInfo::getUserId))
			.as("No token available")
			.returns(null, from(DatumExportTaskInfo::getTokenId))
			;

		and.then(info.getConfig())
			.as("Config provided")
			.isNotNull()
			.as("Name provided")
			.returns(TEST_NAME, from(Configuration::getName))
			;
		// @formatter:on
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
			.as("Found by claim")
			.isNotNull()
			.as("PK")
			.returns(this.info.getId(), from(DatumExportTaskInfo::getId))
			.as("Export date provided")
			.returns(TEST_EXPORT_DATE, from(DatumExportTaskInfo::getExportDate))
			.as("User ID provided")
			.returns(userId, from(DatumExportTaskInfo::getUserId))
			.as("Token provided")
			.returns(this.info.getTokenId(), from(DatumExportTaskInfo::getTokenId))
			;

		and.then(info.getConfig())
			.as("Config provided")
			.isNotNull()
			.as("Name provided")
			.returns(TEST_NAME, from(Configuration::getName))
			;
		// @formatter:on
	}

	@Test
	public void getByClaimNothingLeftToClaim() {
		// GIVEN
		getByClaim();

		// flush session cache
		dao.getSqlSession().clearCache();

		// WHEN
		DatumExportTaskInfo info = dao.claimQueuedTask();

		// THEN
		then(info).as("Nothing claimed").isNull();
	}

	@Test
	public void deleteUser_deletesTask() {
		// GIVEN
		storeNew_withToken();

		// WHEN
		jdbcTemplate.update("delete from solaruser.user_user where id = ?", userId);

		// THEN
		// @formatter:off
		then(jdbcTemplate.queryForObject(
				"select count(*) from solarnet.sn_datum_export_task where id = ?::uuid",
				Integer.class, this.info.getId().toString()))
			.as("Task row deleted via CASCADE on solaruser.user_user")
			.isZero()
			;
		// @formatter:on
	}

	@Test
	public void purgeCompletedNoneCompleted() {
		// GIVEN
		storeNew();

		// WHEN
		long result = dao.purgeCompletedTasks(Instant.now());

		// THEN
		then(result).as("Delete count").isZero();
	}

	@Test
	public void purgeCompletedNoneExpired() {
		// GIVEN
		storeNew();
		this.info.setCompleted(Instant.now().truncatedTo(ChronoUnit.MINUTES));
		this.info.setStatus(DatumExportState.Completed);
		dao.save(this.info);

		// WHEN
		long result = dao.purgeCompletedTasks(this.info.getCompleted());

		// THEN
		then(result).as("Delete count").isZero();
	}

	@Test
	public void purgeCompleted() {
		// GIVEN
		storeNew();
		this.info.setCompleted(Instant.now().truncatedTo(ChronoUnit.MINUTES));
		this.info.setStatus(DatumExportState.Completed);
		dao.save(this.info);

		storeNew();
		this.info.setCompleted(Instant.now().truncatedTo(ChronoUnit.HOURS));
		this.info.setStatus(DatumExportState.Completed);
		dao.save(this.info);

		// WHEN
		long result = dao.purgeCompletedTasks(
				Instant.now().truncatedTo(ChronoUnit.HOURS).plus(1, ChronoUnit.HOURS));

		// THEN
		then(result).as("Delete count").isEqualTo(2L);
	}

}
