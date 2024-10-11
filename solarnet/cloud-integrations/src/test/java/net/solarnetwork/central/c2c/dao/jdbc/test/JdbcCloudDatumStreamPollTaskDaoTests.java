/* ==================================================================
 * JdbcCloudDatumStreamPollTaskDaoTests.java - 10/10/2024 10:41:16 am
 *
 * Copyright 2024 SolarNetwork.net Dev Team
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
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.allCloudDatumStreamPollTaskEntityData;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.newCloudDatumStreamPollTaskEntity;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudDatumStreamPollTaskDao;
import net.solarnetwork.central.c2c.dao.jdbc.JdbcCloudIntegrationConfigurationDao;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPollTaskEntity;
import net.solarnetwork.central.c2c.domain.CloudIntegrationConfiguration;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.codec.JsonUtils;
import net.solarnetwork.dao.Entity;
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the {@link JdbcCloudDatumStreamPollTaskDao} class.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcCloudDatumStreamPollTaskDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcCloudIntegrationConfigurationDao integrationDao;
	private JdbcCloudDatumStreamConfigurationDao datumStreamDao;
	private JdbcCloudDatumStreamPollTaskDao dao;
	private Long userId;

	private CloudIntegrationConfiguration lastIntegration;
	private CloudDatumStreamPollTaskEntity last;

	@BeforeEach
	public void setup() {
		dao = new JdbcCloudDatumStreamPollTaskDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
		integrationDao = new JdbcCloudIntegrationConfigurationDao(jdbcTemplate);
		datumStreamDao = new JdbcCloudDatumStreamConfigurationDao(jdbcTemplate);
	}

	private CloudIntegrationConfiguration createIntegration(Long userId, Map<String, Object> props) {
		CloudIntegrationConfiguration conf = CinJdbcTestUtils.newCloudIntegrationConfiguration(userId,
				randomString(), randomString(), props);
		CloudIntegrationConfiguration entity = integrationDao.get(integrationDao.save(conf));
		lastIntegration = entity;
		return entity;
	}

	private CloudDatumStreamConfiguration createDatumStream(Long userId, Long integrationId,
			Map<String, Object> props) {
		CloudDatumStreamConfiguration conf = CinJdbcTestUtils.newCloudDatumStreamConfiguration(userId,
				integrationId, randomString(), ObjectDatumKind.Node, randomLong(), randomString(),
				randomString(), randomString(), props);
		CloudDatumStreamConfiguration entity = datumStreamDao.get(datumStreamDao.save(conf));
		return entity;
	}

	@Test
	public void entityKey() {
		UserLongCompositePK id = new UserLongCompositePK(randomLong(), randomLong());
		CloudDatumStreamPollTaskEntity result = dao.entityKey(id);

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
		final CloudDatumStreamConfiguration datumStream = createDatumStream(userId,
				integration.getConfigId(), null);

		Map<String, Object> props = singletonMap("foo", "bar");
		// @formatter:off
		CloudDatumStreamPollTaskEntity conf = newCloudDatumStreamPollTaskEntity(userId,
				datumStream.getConfigId(),
				BasicClaimableJobState.Queued,
				now().truncatedTo(ChronoUnit.SECONDS).minus(1L, ChronoUnit.DAYS),
				now().truncatedTo(ChronoUnit.DAYS),
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
			.as("Datum stream ID as provided")
			.returns(datumStream.getConfigId(), UserLongCompositePK::getEntityId)
			;

		List<Map<String, Object>> data = allCloudDatumStreamPollTaskEntityData(jdbcTemplate);
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
			.as("Row start date")
			.containsEntry("start_at", Timestamp.from(conf.getStartAt()))
			.as("Row message")
			.containsEntry("message", conf.getMessage())
			.as("Row service properties")
			.hasEntrySatisfying("sprops", o -> {
				then(JsonUtils.getStringMap(o.toString()))
					.as("Row service props")
					.isEqualTo(props);
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
		CloudDatumStreamPollTaskEntity result = dao.get(last.getId());

		// THEN
		then(result).as("Retrieved entity matches source").isEqualTo(last);
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// WHEN
		CloudDatumStreamPollTaskEntity conf = last.copyWithId(last.getId());
		conf.setState(BasicClaimableJobState.Completed);
		conf.setExecuteAt(now().plusMillis(474));
		conf.setStartAt(now().plusMillis(4747474));
		conf.setMessage(randomString());

		Map<String, Object> props = Collections.singletonMap("bar", "foo");
		conf.setServiceProps(props);

		UserLongCompositePK result = dao.save(conf);
		CloudDatumStreamPollTaskEntity updated = dao.get(result);

		// THEN
		List<Map<String, Object>> data = allCloudDatumStreamPollTaskEntityData(jdbcTemplate);
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
		CloudDatumStreamPollTaskEntity conf = last.copyWithId(last.getId());
		conf.setState(BasicClaimableJobState.Claimed);
		conf.setExecuteAt(now().plusMillis(474));
		conf.setStartAt(now().plusMillis(4747474));
		conf.setMessage(randomString());

		Map<String, Object> props = Collections.singletonMap("bar", "foo");
		conf.setServiceProps(props);

		boolean result = dao.updateTask(conf, BasicClaimableJobState.Queued);
		CloudDatumStreamPollTaskEntity updated = dao.get(conf.getId());

		// THEN
		List<Map<String, Object>> data = allCloudDatumStreamPollTaskEntityData(jdbcTemplate);
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
		CloudDatumStreamPollTaskEntity conf = last.copyWithId(last.getId());
		conf.setState(BasicClaimableJobState.Completed);
		conf.setExecuteAt(now().plusMillis(474));
		conf.setStartAt(now().plusMillis(4747474));
		conf.setMessage(randomString());

		Map<String, Object> props = Collections.singletonMap("bar", "foo");
		conf.setServiceProps(props);

		boolean result = dao.updateTask(conf, BasicClaimableJobState.Claimed);
		CloudDatumStreamPollTaskEntity updated = dao.get(conf.getId());

		// THEN
		List<Map<String, Object>> data = allCloudDatumStreamPollTaskEntityData(jdbcTemplate);
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
		CloudDatumStreamPollTaskEntity updated = dao.get(last.getId());

		// THEN
		List<Map<String, Object>> data = allCloudDatumStreamPollTaskEntityData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1);

		// @formatter:off
		then(result)
			.as("True returned because row updated")
			.isTrue()
			;

		CloudDatumStreamPollTaskEntity expected = last.clone();
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
		CloudDatumStreamPollTaskEntity updated = dao.get(last.getId());

		// THEN
		List<Map<String, Object>> data = allCloudDatumStreamPollTaskEntityData(jdbcTemplate);
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
		List<Map<String, Object>> data = allCloudDatumStreamPollTaskEntityData(jdbcTemplate);
		then(data).as("Row deleted from db").isEmpty();
	}

	@Test
	public void claimTask_noRows() {
		// GIVEN

		// WHEN
		CloudDatumStreamPollTaskEntity result = dao.claimQueuedTask();

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
				lastIntegration.getConfigId(), null);
		// @formatter:off
		CloudDatumStreamPollTaskEntity conf2 = newCloudDatumStreamPollTaskEntity(userId,
				datumStream2.getConfigId(),
				BasicClaimableJobState.Completed,
				now().truncatedTo(ChronoUnit.SECONDS),
				now().truncatedTo(ChronoUnit.DAYS),
				randomString(),
				null)
				;
		// @formatter:on
		dao.save(conf2);

		allCloudDatumStreamPollTaskEntityData(jdbcTemplate);

		// WHEN
		CloudDatumStreamPollTaskEntity result = dao.claimQueuedTask();

		//TestTransaction.flagForCommit();

		// THEN
		CloudDatumStreamPollTaskEntity expected = last.clone();
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
		final CloudDatumStreamConfiguration datumStream1 = createDatumStream(userId,
				integration.getConfigId(), null);
		final CloudDatumStreamConfiguration datumStream2 = createDatumStream(userId,
				integration.getConfigId(), null);

		// @formatter:off
		// 1st task executeAt in future: cannot be claimed
		CloudDatumStreamPollTaskEntity conf1 = newCloudDatumStreamPollTaskEntity(userId,
				datumStream1.getConfigId(),
				BasicClaimableJobState.Queued,
				now().truncatedTo(ChronoUnit.SECONDS).plus(1L, ChronoUnit.DAYS),
				now().truncatedTo(ChronoUnit.DAYS),
				randomString(),
				null)
				;

		// 2nd task executeAt in past: can be claimed
		CloudDatumStreamPollTaskEntity conf2 = newCloudDatumStreamPollTaskEntity(userId,
				datumStream2.getConfigId(),
				BasicClaimableJobState.Queued,
				now().truncatedTo(ChronoUnit.SECONDS).minus(1L, ChronoUnit.DAYS),
				now().truncatedTo(ChronoUnit.DAYS),
				randomString(),
				null)
				;
		// @formatter:on

		dao.save(conf1);
		dao.save(conf2);

		allCloudDatumStreamPollTaskEntityData(jdbcTemplate);

		// WHEN
		CloudDatumStreamPollTaskEntity result = dao.claimQueuedTask();

		// THEN
		CloudDatumStreamPollTaskEntity expected = conf2.clone();
		expected.setState(BasicClaimableJobState.Claimed);

		// @formatter:off
		then(result)
			.as("Retrieved entity matches row with executeAt in the past")
			.isEqualTo(expected)
			.matches(c -> c.isSameAs(expected), "Claimed entity has Claimed state")
			;
		// @formatter:on
	}

}
