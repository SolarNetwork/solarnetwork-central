/* ==================================================================
 * JdbcCloudDatumStreamRakeTaskDaoTests.java - 21/09/2025 6:38:58 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.c2c.dao.jdbc.test;

import static java.time.Instant.now;
import static java.util.Collections.singletonMap;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.allCloudDatumStreamRakeTaskEntityData;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.newCloudDatumStreamRakeTaskEntity;
import static net.solarnetwork.central.test.CommonTestUtils.RNG;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.c2c.dao.BasicFilter;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamMappingConfigurationDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamRakeTaskDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamMappingConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamRakeTaskEntity;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.common.dao.jdbc.sql.CommonJdbcUtils;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link JdbcCloudDatumStreamRakeTaskDao} class.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcCloudDatumStreamRakeTaskDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcCloudIntegrationConfigurationDao integrationDao;
	private JdbcCloudDatumStreamConfigurationDao datumStreamDao;
	private JdbcCloudDatumStreamMappingConfigurationDao datumStreamMappingDao;
	private JdbcCloudDatumStreamRakeTaskDao dao;
	private Long userId;

	private CloudDatumStreamMappingConfiguration lastMapping;
	private CloudDatumStreamRakeTaskEntity last;

	@BeforeEach
	public void setup() {
		dao = new JdbcCloudDatumStreamRakeTaskDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
		integrationDao = new JdbcCloudIntegrationConfigurationDao(jdbcTemplate);
		datumStreamDao = new JdbcCloudDatumStreamConfigurationDao(jdbcTemplate);
		datumStreamMappingDao = new JdbcCloudDatumStreamMappingConfigurationDao(jdbcTemplate);
	}

	private CloudIntegrationConfiguration createIntegration(Long userId, Map<String, Object> props) {
		CloudIntegrationConfiguration conf = CinJdbcTestUtils.newCloudIntegrationConfiguration(userId,
				randomString(), randomString(), props);
		CloudIntegrationConfiguration entity = integrationDao.get(integrationDao.save(conf));
		return entity;
	}

	private CloudDatumStreamConfiguration createDatumStream(Long userId, Long datumStreamMappingId,
			Map<String, Object> props) {
		CloudDatumStreamConfiguration conf = CinJdbcTestUtils.newCloudDatumStreamConfiguration(userId,
				datumStreamMappingId, randomString(), ObjectDatumKind.Node, randomLong(), randomString(),
				randomString(), randomString(), props);
		CloudDatumStreamConfiguration entity = datumStreamDao.get(datumStreamDao.save(conf));
		return entity;
	}

	private CloudDatumStreamMappingConfiguration createDatumStreamMapping(Long userId,
			Long integrationId, Map<String, Object> props) {
		CloudDatumStreamMappingConfiguration conf = CinJdbcTestUtils
				.newCloudDatumStreamMappingConfiguration(userId, integrationId, randomString(), props);
		CloudDatumStreamMappingConfiguration entity = datumStreamMappingDao
				.get(datumStreamMappingDao.save(conf));
		lastMapping = entity;
		return entity;
	}

	@Test
	public void entityKey() {
		UserLongCompositePK id = new UserLongCompositePK(randomLong(), randomLong());
		CloudDatumStreamRakeTaskEntity result = dao.entityKey(id);

		// @formatter:off
		then(result)
			.as("Entity for key returned")
			.isNotNull()
			.as("ID of entity from provided value")
			.returns(id, Entity::getId)
			;
		// @formatter:on
	}

	@Test
	public void insert() {
		// GIVEN
		final CloudIntegrationConfiguration integration = createIntegration(userId, null);
		CloudDatumStreamMappingConfiguration mapping = createDatumStreamMapping(userId,
				integration.getConfigId(), null);
		final CloudDatumStreamConfiguration datumStream = createDatumStream(userId,
				mapping.getConfigId(), null);

		Map<String, Object> props = singletonMap("foo", "bar");
		// @formatter:off
		CloudDatumStreamRakeTaskEntity conf = newCloudDatumStreamRakeTaskEntity(userId,
				datumStream.getConfigId(),
				BasicClaimableJobState.Queued,
				now().truncatedTo(ChronoUnit.SECONDS).minus(1L, ChronoUnit.DAYS),
				Period.ofDays(1),
				randomString(),
				props)
				;
		// @formatter:on

		// WHEN
		UserLongCompositePK result = dao.create(userId, conf);

		// THEN

		// @formatter:off
		then(result).as("Primary key")
			.isNotNull()
			.as("User ID as provided")
			.returns(userId, UserLongCompositePK::getUserId)
			.extracting(UserLongCompositePK::getEntityId)
			.as("Config ID generated")
			.isNotNull()
			;

		List<Map<String, Object>> data = allCloudDatumStreamRakeTaskEntityData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1).asInstanceOf(list(Map.class))
			.element(0, map(String.class, Object.class))
			.as("Row user ID")
			.containsEntry("user_id", userId)
			.as("Row datum stream ID generated")
			.containsKey("ds_id")
			.as("No creation date")
			.doesNotContainKey("created")
			.as("No modification date")
			.doesNotContainKey("modified")
			.as("Row status")
			.containsEntry("status", conf.getState().keyValue())
			.as("Row execution date")
			.containsEntry("exec_at", Timestamp.from(conf.getExecuteAt()))
			.as("Row offset")
			.hasEntrySatisfying("start_offset", o -> {
				then(CommonJdbcUtils.getIntervalPeriod(o))
					.as("Row start_offset")
					.isEqualTo(conf.getOffset())
					;
			})
			.as("Row message")
			.containsEntry("message", conf.getMessage())
			.as("Row service properties")
			.hasEntrySatisfying("sprops", o -> {
				then(JsonUtils.getStringMap(o.toString()))
					.as("Row service props")
					.isEqualTo(props)
					;
			})
			;
		// @formatter:on
		last = conf.copyWithId(result);
	}

	@Test
	public void get() {
		// GIVEN
		insert();

		// WHEN
		CloudDatumStreamRakeTaskEntity result = dao.get(last.getId());

		// THEN
		then(result).as("Retrieved entity matches source").isEqualTo(last);
	}

	@Test
	public void findFiltered_forUser() throws Exception {
		final int userCount = 2;
		final int integrationCount = 2;
		final int streamCount = 2;
		final List<CloudDatumStreamRakeTaskEntity> confs = new ArrayList<>(
				userCount * integrationCount * streamCount);

		for ( int u = 0; u < userCount; u++ ) {
			final Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			for ( int g = 0; g < integrationCount; g++ ) {
				final Long integrationId = createIntegration(userId, Map.of("foo", "bar")).getConfigId();
				for ( int s = 0; s < streamCount; s++ ) {
					final Long mappingId = createDatumStreamMapping(userId, integrationId, null)
							.getConfigId();
					final Long streamId = createDatumStream(userId, mappingId, Map.of("bim", "bam"))
							.getConfigId();
					// @formatter:off
					CloudDatumStreamRakeTaskEntity entity = newCloudDatumStreamRakeTaskEntity(
							userId,
							streamId,
							BasicClaimableJobState.Queued,
							Instant.now(),
							Period.ofDays(1),
							randomString(),
							null
							);
					// @formatter:on
					var pk = dao.save(entity);
					confs.add(dao.get(pk));
				}
			}
		}

		// WHEN
		final CloudDatumStreamRakeTaskEntity randomConf = confs.get(RNG.nextInt(confs.size()));
		final BasicFilter filter = new BasicFilter();
		filter.setUserId(randomConf.getUserId());
		var results = dao.findFiltered(filter);

		// THEN
		CloudDatumStreamRakeTaskEntity[] expected = confs.stream()
				.filter(e -> randomConf.getUserId().equals(e.getUserId()))
				.toArray(CloudDatumStreamRakeTaskEntity[]::new);
		then(results).as("Results for single user returned").containsExactly(expected);
	}

	@Test
	public void findFiltered_forUserAndState() throws Exception {
		final int userCount = 2;
		final int integrationCount = 2;
		final int streamCount = 10;
		final List<CloudDatumStreamRakeTaskEntity> confs = new ArrayList<>(
				userCount * integrationCount * streamCount);

		for ( int u = 0; u < userCount; u++ ) {
			final Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			for ( int g = 0; g < integrationCount; g++ ) {
				final Long integrationId = createIntegration(userId, Map.of("foo", "bar")).getConfigId();
				for ( int s = 0; s < streamCount; s++ ) {
					final Long mappingId = createDatumStreamMapping(userId, integrationId, null)
							.getConfigId();
					final Long streamId = createDatumStream(userId, mappingId, Map.of("bim", "bam"))
							.getConfigId();
					// @formatter:off
					CloudDatumStreamRakeTaskEntity entity = newCloudDatumStreamRakeTaskEntity(
							userId,
							streamId,
							BasicClaimableJobState.values()[s % BasicClaimableJobState.values().length],
							Instant.now(),
							Period.ofDays(1),
							randomString(),
							null
							);
					// @formatter:on
					var pk = dao.save(entity);
					confs.add(dao.get(pk));
				}
			}
		}

		// WHEN
		final CloudDatumStreamRakeTaskEntity randomConf = confs.get(RNG.nextInt(confs.size()));
		final BasicFilter filter = new BasicFilter();
		filter.setUserId(randomConf.getUserId());
		filter.setClaimableJobStates(new BasicClaimableJobState[] { BasicClaimableJobState.Claimed,
				BasicClaimableJobState.Executing });
		var results = dao.findFiltered(filter);

		// THEN
		CloudDatumStreamRakeTaskEntity[] expected = confs.stream()
				.filter(e -> randomConf.getUserId().equals(e.getUserId())
						&& EnumSet.of(BasicClaimableJobState.Claimed, BasicClaimableJobState.Executing)
								.contains(e.getState()))
				.toArray(CloudDatumStreamRakeTaskEntity[]::new);
		then(results).as("Results for single user and specified states returned")
				.containsExactly(expected);
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// WHEN
		CloudDatumStreamRakeTaskEntity conf = last.copyWithId(last.getId());
		conf.setState(BasicClaimableJobState.Completed);
		conf.setExecuteAt(now().plusMillis(474));
		conf.setOffset(Period.ofDays(4747474));
		conf.setMessage(randomString());

		Map<String, Object> props = Collections.singletonMap("bar", "foo");
		conf.setServiceProps(props);

		UserLongCompositePK result = dao.save(conf);
		CloudDatumStreamRakeTaskEntity updated = dao.get(result);

		// THEN
		List<Map<String, Object>> data = allCloudDatumStreamRakeTaskEntityData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1);
		// @formatter:off
		then(updated).as("Retrieved entity matches updated source")
			.isEqualTo(conf)
			.as("Entity saved updated values")
			.matches(c -> c.isSameAs(updated));
		// @formatter:on
	}

	@Test
	public void update_forState() {
		// GIVEN
		insert();

		// WHEN
		CloudDatumStreamRakeTaskEntity conf = last.copyWithId(last.getId());
		conf.setState(BasicClaimableJobState.Claimed);
		conf.setExecuteAt(now().plusMillis(474));
		conf.setOffset(Period.ofDays(4747474));
		conf.setMessage(randomString());

		Map<String, Object> props = Collections.singletonMap("bar", "foo");
		conf.setServiceProps(props);

		boolean result = dao.updateTask(conf, BasicClaimableJobState.Queued);
		CloudDatumStreamRakeTaskEntity updated = dao.get(conf.getId());

		// THEN
		List<Map<String, Object>> data = allCloudDatumStreamRakeTaskEntityData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1);

		// @formatter:off
		then(result)
			.as("True returned because row updated")
			.isTrue()
			;

		then(updated)
			.as("Retrieved entity matches updated source")
			.isEqualTo(conf)
			.as("Entity saved updated values")
			.matches(c -> c.isSameAs(updated))
			;
		// @formatter:on
	}

	@Test
	public void update_forState_noMatch() {
		// GIVEN
		insert();

		// WHEN
		CloudDatumStreamRakeTaskEntity conf = last.copyWithId(last.getId());
		conf.setState(BasicClaimableJobState.Completed);
		conf.setExecuteAt(now().plusMillis(474));
		conf.setOffset(Period.ofDays(4747474));
		conf.setMessage(randomString());

		Map<String, Object> props = Collections.singletonMap("bar", "foo");
		conf.setServiceProps(props);

		boolean result = dao.updateTask(conf, BasicClaimableJobState.Claimed);
		CloudDatumStreamRakeTaskEntity updated = dao.get(conf.getId());

		// THEN
		List<Map<String, Object>> data = allCloudDatumStreamRakeTaskEntityData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1);

		// @formatter:off
		then(result)
			.as("False returned because row not updated because state did not match")
			.isFalse()
			;

		then(updated).as("Retrieved entity matches updated source")
			.isEqualTo(conf)
			.matches(c -> c.isSameAs(last), "Entity keeps original values")
			;
		// @formatter:on
	}

	@Test
	public void updateState() {
		// GIVEN
		insert();

		// WHEN
		final BasicClaimableJobState newState = BasicClaimableJobState.Claimed;
		boolean result = dao.updateTaskState(last.getId(), newState, BasicClaimableJobState.Queued);
		CloudDatumStreamRakeTaskEntity updated = dao.get(last.getId());

		// THEN
		List<Map<String, Object>> data = allCloudDatumStreamRakeTaskEntityData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1);

		// @formatter:off
		then(result)
			.as("True returned because row updated")
			.isTrue()
			;

		CloudDatumStreamRakeTaskEntity expected = last.clone();
		expected.setState(newState);

		then(updated)
			.as("Retrieved entity matches updated source")
			.isEqualTo(last)
			.as("Entity saved updated state")
			.matches(c -> c.isSameAs(expected))
			;
		// @formatter:on
	}

	@Test
	public void updateState_noMatch() {
		// GIVEN
		insert();

		// WHEN
		final BasicClaimableJobState newState = BasicClaimableJobState.Completed;
		boolean result = dao.updateTaskState(last.getId(), newState, BasicClaimableJobState.Claimed);
		CloudDatumStreamRakeTaskEntity updated = dao.get(last.getId());

		// THEN
		List<Map<String, Object>> data = allCloudDatumStreamRakeTaskEntityData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1);

		// @formatter:off
		then(result)
			.as("False returned because row not updated because state did not match")
			.isFalse()
			;

		then(updated).as("Retrieved entity matches updated source")
			.isEqualTo(last)
			.matches(c -> c.isSameAs(last), "Entity keeps original values")
			;
		// @formatter:on
	}

	@Test
	public void delete() {
		// GIVEN
		insert();

		// WHEN
		dao.delete(last);

		// THEN
		List<Map<String, Object>> data = allCloudDatumStreamRakeTaskEntityData(jdbcTemplate);
		then(data).as("Row deleted from db").isEmpty();
	}

	@Test
	public void delete_filter_datumStream() {
		final int userCount = 2;
		final int integrationCount = 2;
		final int streamCount = 2;
		final int taskCount = 2;
		final List<CloudDatumStreamRakeTaskEntity> confs = new ArrayList<>(
				userCount * integrationCount * streamCount);

		for ( int u = 0; u < userCount; u++ ) {
			final Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			for ( int g = 0; g < integrationCount; g++ ) {
				final Long integrationId = createIntegration(userId, Map.of("foo", "bar")).getConfigId();
				for ( int s = 0; s < streamCount; s++ ) {
					final Long mappingId = createDatumStreamMapping(userId, integrationId, null)
							.getConfigId();
					final Long streamId = createDatumStream(userId, mappingId, Map.of("bim", "bam"))
							.getConfigId();
					for ( int t = 0; t < taskCount; t++ ) {
						// @formatter:off
						CloudDatumStreamRakeTaskEntity entity = newCloudDatumStreamRakeTaskEntity(
								userId,
								streamId,
								BasicClaimableJobState.Queued,
								Instant.now(),
								Period.ofDays(t + 1),
								randomString(),
								null
								);
						// @formatter:on
						var pk = dao.save(entity);
						confs.add(dao.get(pk));
					}
				}
			}
		}

		// WHEN
		final CloudDatumStreamRakeTaskEntity randomConf = confs.get(RNG.nextInt(confs.size()));
		final BasicFilter filter = new BasicFilter();
		filter.setUserId(randomConf.getUserId());
		filter.setDatumStreamId(randomConf.getDatumStreamId());
		int result = dao.delete(filter);

		// THEN
		// @formatter:off
		then(result)
			.as("Count of tasks for given datum stream deleted")
			.isEqualTo(taskCount)
			;
		// @formatter:on
		CloudDatumStreamRakeTaskEntity[] expected = confs.stream()
				.filter(e -> !(randomConf.getUserId().equals(e.getUserId())
						&& randomConf.getDatumStreamId().equals(e.getDatumStreamId())))
				.toArray(CloudDatumStreamRakeTaskEntity[]::new);
		then(dao.getAll(null)).as("Expected tasks deleted").containsOnlyOnce(expected);
	}

	@Test
	public void delete_filter_user() {
		final int userCount = 2;
		final int integrationCount = 2;
		final int streamCount = 2;
		final int taskCount = 2;
		final List<CloudDatumStreamRakeTaskEntity> confs = new ArrayList<>(
				userCount * integrationCount * streamCount);

		for ( int u = 0; u < userCount; u++ ) {
			final Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			for ( int g = 0; g < integrationCount; g++ ) {
				final Long integrationId = createIntegration(userId, Map.of("foo", "bar")).getConfigId();
				for ( int s = 0; s < streamCount; s++ ) {
					final Long mappingId = createDatumStreamMapping(userId, integrationId, null)
							.getConfigId();
					final Long streamId = createDatumStream(userId, mappingId, Map.of("bim", "bam"))
							.getConfigId();
					for ( int t = 0; t < taskCount; t++ ) {
						// @formatter:off
						CloudDatumStreamRakeTaskEntity entity = newCloudDatumStreamRakeTaskEntity(
								userId,
								streamId,
								BasicClaimableJobState.Queued,
								Instant.now(),
								Period.ofDays(t + 1),
								randomString(),
								null
								);
						// @formatter:on
						var pk = dao.save(entity);
						confs.add(dao.get(pk));
					}
				}
			}
		}

		// WHEN
		final CloudDatumStreamRakeTaskEntity randomConf = confs.get(RNG.nextInt(confs.size()));
		final BasicFilter filter = new BasicFilter();
		filter.setUserId(randomConf.getUserId());
		int result = dao.delete(filter);

		// THEN
		// @formatter:off
		then(result)
			.as("Count of tasks for given user deleted")
			.isEqualTo(integrationCount * streamCount * taskCount)
			;
		// @formatter:on
		CloudDatumStreamRakeTaskEntity[] expected = confs.stream()
				.filter(e -> !randomConf.getUserId().equals(e.getUserId()))
				.toArray(CloudDatumStreamRakeTaskEntity[]::new);
		then(dao.getAll(null)).as("Expected tasks deleted").containsOnlyOnce(expected);
	}

	@Test
	public void claimTask_noRows() {
		// GIVEN

		// WHEN
		CloudDatumStreamRakeTaskEntity result = dao.claimQueuedTask();

		// THEN
		// @formatter:off
		then(result)
			.as("Null returned when no rows exist")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void claimTask() {
		// GIVEN
		insert();

		// add another, but not in Queued state so not claimable
		final CloudDatumStreamConfiguration datumStream2 = createDatumStream(userId,
				lastMapping.getConfigId(), null);
		datumStream2.setDatumStreamMappingId(lastMapping.getConfigId());
		// @formatter:off
		CloudDatumStreamRakeTaskEntity conf2 = newCloudDatumStreamRakeTaskEntity(userId,
				datumStream2.getConfigId(),
				BasicClaimableJobState.Completed,
				now().truncatedTo(ChronoUnit.SECONDS),
				Period.ofDays(1),
				randomString(),
				null)
				;
		// @formatter:on
		dao.save(conf2);

		allCloudDatumStreamRakeTaskEntityData(jdbcTemplate);

		// WHEN
		CloudDatumStreamRakeTaskEntity result = dao.claimQueuedTask();

		//TestTransaction.flagForCommit();

		// THEN
		CloudDatumStreamRakeTaskEntity expected = last.clone();
		expected.setState(BasicClaimableJobState.Claimed);

		// @formatter:off
		then(result)
			.as("Retrieved entity matches with with Queued state")
			.isEqualTo(expected)
			.matches(c -> c.isSameAs(expected), "Claimed entity has Claimed state")
			;
		// @formatter:on
	}

	@Test
	public void claimTask_execAtInPast() {
		// GIVEN
		final CloudIntegrationConfiguration integration = createIntegration(userId, null);
		final CloudDatumStreamMappingConfiguration mapping = createDatumStreamMapping(userId,
				integration.getConfigId(), null);
		final CloudDatumStreamConfiguration datumStream1 = createDatumStream(userId,
				mapping.getConfigId(), null);
		final CloudDatumStreamConfiguration datumStream2 = createDatumStream(userId,
				mapping.getConfigId(), null);

		// @formatter:off
		// 1st task executeAt in future: cannot be claimed
		CloudDatumStreamRakeTaskEntity conf1 = newCloudDatumStreamRakeTaskEntity(userId,
				datumStream1.getConfigId(),
				BasicClaimableJobState.Queued,
				now().truncatedTo(ChronoUnit.SECONDS).plus(1L, ChronoUnit.DAYS),
				Period.ofDays(1),
				randomString(),
				null)
				;

		// 2nd task executeAt in past: can be claimed
		CloudDatumStreamRakeTaskEntity conf2 = newCloudDatumStreamRakeTaskEntity(userId,
				datumStream2.getConfigId(),
				BasicClaimableJobState.Queued,
				now().truncatedTo(ChronoUnit.SECONDS).minus(1L, ChronoUnit.DAYS),
				Period.ofDays(1),
				randomString(),
				null)
				;
		// @formatter:on

		conf1 = dao.get(dao.save(conf1));
		conf2 = dao.get(dao.save(conf2));

		allCloudDatumStreamRakeTaskEntityData(jdbcTemplate);

		// WHEN
		CloudDatumStreamRakeTaskEntity result = dao.claimQueuedTask();

		// THEN
		CloudDatumStreamRakeTaskEntity expected = conf2.clone();
		expected.setState(BasicClaimableJobState.Claimed);

		// @formatter:off
		then(result)
			.as("Retrieved entity matches row with executeAt in the past")
			.isEqualTo(expected)
			.matches(c -> c.isSameAs(expected), "Claimed entity has Claimed state")
			;
		// @formatter:on
	}

	@Test
	public void claimTask_withinDatumStreamGroup_executing() {
		// GIVEN
		insert();

		// add another for same datum stream, but not in Executing state so not claimable
		// @formatter:off
		CloudDatumStreamRakeTaskEntity conf2 = newCloudDatumStreamRakeTaskEntity(userId,
				last.getDatumStreamId(),
				BasicClaimableJobState.Executing,
				now().truncatedTo(ChronoUnit.SECONDS),
				Period.ofDays(2),
				randomString(),
				null)
				;
		// @formatter:on
		conf2 = dao.get(dao.save(conf2));

		allCloudDatumStreamRakeTaskEntityData(jdbcTemplate);

		// WHEN
		CloudDatumStreamRakeTaskEntity result = dao.claimQueuedTask();

		// THEN
		// @formatter:off
		then(result)
			.as("No task claimed because another for same datum stream is Executing")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void claimTask_withinDatumStreamGroup_claimed() {
		// GIVEN
		insert();

		// add another for same datum stream, but not in Claimed state so not claimable
		// @formatter:off
		CloudDatumStreamRakeTaskEntity conf2 = newCloudDatumStreamRakeTaskEntity(userId,
				last.getDatumStreamId(),
				BasicClaimableJobState.Claimed,
				now().truncatedTo(ChronoUnit.SECONDS),
				Period.ofDays(2),
				randomString(),
				null)
				;
		// @formatter:on
		conf2 = dao.get(dao.save(conf2));

		allCloudDatumStreamRakeTaskEntityData(jdbcTemplate);

		// WHEN
		CloudDatumStreamRakeTaskEntity result = dao.claimQueuedTask();

		// THEN
		// @formatter:off
		then(result)
			.as("No task claimed because another for same datum stream is Claimed")
			.isNull()
			;
		// @formatter:on
	}

	@Test
	public void claimTask_withinDatumStreamGroup_differentGroup() {
		// GIVEN
		insert();

		// add another for same datum stream, but not in Claimed state so not claimable
		// @formatter:off
		CloudDatumStreamRakeTaskEntity conf2 = newCloudDatumStreamRakeTaskEntity(userId,
				last.getDatumStreamId(),
				BasicClaimableJobState.Claimed,
				now().truncatedTo(ChronoUnit.SECONDS),
				Period.ofDays(2),
				randomString(),
				null)
				;
		// @formatter:on
		conf2 = dao.get(dao.save(conf2));

		// add another for DIFFERENT datum stream
		final CloudDatumStreamConfiguration datumStream2 = createDatumStream(userId,
				lastMapping.getConfigId(), null);
		datumStream2.setDatumStreamMappingId(lastMapping.getConfigId());
		// @formatter:off
		CloudDatumStreamRakeTaskEntity conf3 = newCloudDatumStreamRakeTaskEntity(userId,
				datumStream2.getConfigId(),
				BasicClaimableJobState.Queued,
				now().truncatedTo(ChronoUnit.SECONDS),
				Period.ofDays(1),
				randomString(),
				null)
				;
		// @formatter:on
		conf3 = dao.get(dao.save(conf3));

		allCloudDatumStreamRakeTaskEntityData(jdbcTemplate);

		// WHEN
		CloudDatumStreamRakeTaskEntity result = dao.claimQueuedTask();

		//TestTransaction.flagForCommit();

		// THEN
		CloudDatumStreamRakeTaskEntity expected = conf3.clone();
		expected.setState(BasicClaimableJobState.Claimed);

		// @formatter:off
		then(result)
			.as("Retrieved entity matches with with Queued state from other datum stream")
			.isEqualTo(expected)
			.matches(c -> c.isSameAs(expected), "Claimed entity has Claimed state")
			;
		// @formatter:on
	}

	@Test
	public void resetAbandoned() {
		// GIVEN
		final int datumStreamCount = 10;
		final Instant start = Instant.now().truncatedTo(ChronoUnit.HOURS);
		final CloudIntegrationConfiguration integration = createIntegration(userId, null);
		final CloudDatumStreamMappingConfiguration mapping = createDatumStreamMapping(userId,
				integration.getConfigId(), null);

		final List<CloudDatumStreamRakeTaskEntity> rakeTasks = new ArrayList<>(datumStreamCount);

		for ( int i = 0; i < datumStreamCount; i++ ) {
			final CloudDatumStreamConfiguration datumStream = createDatumStream(userId,
					mapping.getConfigId(), null);
			CloudDatumStreamRakeTaskEntity conf = newCloudDatumStreamRakeTaskEntity(userId,
					datumStream.getConfigId(),
					i == 0 || RNG.nextBoolean() ? BasicClaimableJobState.Executing
							: RNG.nextBoolean() ? BasicClaimableJobState.Claimed
									: BasicClaimableJobState.Queued,
					start.plusSeconds(60 * i), Period.ofDays(1), randomString(), null);
			rakeTasks.add(dao.get(dao.save(conf)));
		}

		final List<CloudDatumStreamRakeTaskEntity> tasksToReset = rakeTasks.stream()
				.filter(e -> e.getState() == BasicClaimableJobState.Executing
						|| e.getState() == BasicClaimableJobState.Claimed)
				.toList();
		final CloudDatumStreamRakeTaskEntity randomExecutingTask = tasksToReset
				.get(RNG.nextInt(tasksToReset.size()));

		allCloudDatumStreamRakeTaskEntityData(jdbcTemplate);

		// WHEN
		final Instant minimumDate = randomExecutingTask.getExecuteAt().plusSeconds(1);
		int count = dao.resetAbandondedExecutingTasks(minimumDate);

		// THEN
		final int expectedCount = (int) tasksToReset.stream()
				.filter(e -> e.getExecuteAt().isBefore(minimumDate)).count();
		// @formatter:off
		then(count)
			.as("Should reset all tasks in executing state older than given minimum date")
			.isEqualTo(expectedCount)
			;
		// @formatter:on
	}

}
