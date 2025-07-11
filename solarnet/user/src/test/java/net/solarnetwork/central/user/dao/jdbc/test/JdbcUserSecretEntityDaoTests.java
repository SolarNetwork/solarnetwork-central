/* ==================================================================
 * JdbcUserSecretEntityDaoTests.java - 22/03/2025 7:51:58 am
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

package net.solarnetwork.central.user.dao.jdbc.test;

import static net.solarnetwork.central.test.CommonTestUtils.RNG;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.user.dao.jdbc.test.UserJdbcTestUtils.allUserSecretEntityData;
import static net.solarnetwork.central.user.dao.jdbc.test.UserJdbcTestUtils.newUserSecretEntity;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.domain.UserStringStringCompositePK;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.central.user.dao.BasicUserSecretFilter;
import net.solarnetwork.central.user.dao.jdbc.JdbcUserSecretEntityDao;
import net.solarnetwork.central.user.domain.UserSecretEntity;
import net.solarnetwork.dao.Entity;

/**
 * Test cases for the {@link JdbcUserSecretEntityDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcUserSecretEntityDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcUserSecretEntityDao dao;
	private Long userId;

	private UserSecretEntity last;

	@BeforeEach
	public void setup() {
		dao = new JdbcUserSecretEntityDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
	}

	@Test
	public void entityKey() {
		UserStringStringCompositePK id = new UserStringStringCompositePK(randomLong(), randomString(),
				randomString());
		UserSecretEntity result = dao.entityKey(id);

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
		// @formatter:off
		final UserSecretEntity conf = newUserSecretEntity(
				userId,
				randomString(),
				randomString(),
				randomString()
				);
		// @formatter:on

		// WHEN
		UserStringStringCompositePK result = dao.create(userId, conf.getTopic(), conf);

		// THEN

		// @formatter:off
		then(result).as("Primary key")
			.isNotNull()
			.as("User ID as provided")
			.returns(userId, UserStringStringCompositePK::getUserId)
			.as("Topic ID as provided")
			.returns(conf.getTopic(), UserStringStringCompositePK::getGroupId)
			.as("Key as provided")
			.returns(conf.getKey(), UserStringStringCompositePK::getEntityId)
			;

		List<Map<String, Object>> data = allUserSecretEntityData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1)
			.element(0, map(String.class, Object.class))
			.as("Row user ID")
			.containsEntry("user_id", userId)
			.as("Row topic ID")
			.containsEntry("topic", conf.getTopic())
			.as("Row key")
			.containsEntry("skey", conf.getKey())
			.as("Row creation date")
			.containsEntry("created", Timestamp.from(conf.getCreated()))
			.as("Row modification date")
			.containsEntry("modified", Timestamp.from(conf.getModified()))
			.as("Row secret")
			.containsEntry("sdata", conf.secret())
			;
		// @formatter:on
		last = conf.copyWithId(result);
	}

	@Test
	public void get() {
		// GIVEN
		insert();

		// WHEN
		UserSecretEntity result = dao.get(last.getId());

		// THEN
		then(result).as("Retrieved entity matches source").isEqualTo(last);
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// WHEN
		Instant now = Instant.now();
		String newSecret = randomString();
		UserSecretEntity conf = new UserSecretEntity(last.getId(), last.getCreated(), now, newSecret);

		UserStringStringCompositePK result = dao.save(conf);
		UserSecretEntity updated = dao.get(result);

		// THEN
		List<Map<String, Object>> data = allUserSecretEntityData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1);
		// @formatter:off
		then(updated).as("Retrieved entity matches updated source")
			.isEqualTo(conf)
			.as("Entity saved updated values")
			.matches(c -> c.isSameAs(updated));
		// @formatter:on
	}

	@Test
	public void delete() {
		// GIVEN
		insert();

		// WHEN
		dao.delete(last);

		// THEN
		List<Map<String, Object>> data = allUserSecretEntityData(jdbcTemplate);
		then(data).as("Row deleted from db").isEmpty();
	}

	@Test
	public void findForUserTopic() throws Exception {
		// GIVEN
		final int userCount = 2;
		final int topicCount = 2;
		final int secretCount = 3;
		final List<UserSecretEntity> confs = new ArrayList<>(userCount * topicCount * secretCount);

		for ( int u = 0; u < userCount; u++ ) {
			final Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			for ( int s = 0; s < topicCount; s++ ) {
				final String topic = randomString();
				for ( int i = 0; i < secretCount; i++ ) {
					// @formatter:off
						UserSecretEntity conf = newUserSecretEntity(
								userId,
								topic,
								"Key %d".formatted(i),
								randomString()
								);
						// @formatter:on
					UserStringStringCompositePK id = dao.create(userId, topic, conf);
					conf = conf.copyWithId(id);
					confs.add(conf);
				}
			}
		}

		// WHEN
		final UserSecretEntity randomConf = confs.get(RNG.nextInt(confs.size()));
		Collection<UserSecretEntity> results = dao.findAll(randomConf.getUserId(), randomConf.getTopic(),
				null);

		// THEN
		UserSecretEntity[] expected = confs.stream()
				.filter(e -> randomConf.getUserId().equals(e.getUserId())
						&& randomConf.getTopic().equals(e.getTopic()))
				.toArray(UserSecretEntity[]::new);
		then(results).as("All results for user topic returned").containsExactly(expected);
	}

	@Test
	public void deleteForUserTopic() throws Exception {
		// GIVEN
		final int userCount = 2;
		final int topicCount = 2;
		final int secretCount = 3;
		final List<UserSecretEntity> confs = new ArrayList<>(userCount * topicCount * secretCount);

		for ( int u = 0; u < userCount; u++ ) {
			final Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			for ( int s = 0; s < topicCount; s++ ) {
				final String topic = randomString();
				for ( int i = 0; i < secretCount; i++ ) {
					// @formatter:off
						UserSecretEntity conf = newUserSecretEntity(
								userId,
								topic,
								"Key %d".formatted(i),
								randomString()
								);
						// @formatter:on
					UserStringStringCompositePK id = dao.create(userId, topic, conf);
					conf = conf.copyWithId(id);
					confs.add(conf);
				}
			}
		}

		// WHEN
		final UserSecretEntity randomConf = confs.get(RNG.nextInt(confs.size()));
		dao.delete(dao.entityKey(UserStringStringCompositePK
				.unassignedEntityIdKey(randomConf.getUserId(), randomConf.getTopic())));

		// THEN
		List<Map<String, Object>> allRows = allUserSecretEntityData(jdbcTemplate);
		// @formatter:off
		then(allRows)
			.as("Should have deleted 3 rows")
			.hasSize(confs.size() - 3)
			.as("Should have deleted all rows for given (user,topic) group")
			.noneMatch(row -> {
				return row.get("user_id").equals(randomConf.getUserId())
						&& row.get("topic").equals(randomConf.getTopic());
			})
			;
		// @formatter:on
	}

	@Test
	public void findFiltered_forUser() throws Exception {
		// GIVEN
		final int userCount = 2;
		final int topicCount = 2;
		final int secretCount = 3;
		final List<UserSecretEntity> confs = new ArrayList<>(userCount * topicCount * secretCount);

		for ( int u = 0; u < userCount; u++ ) {
			final Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			for ( int s = 0; s < topicCount; s++ ) {
				final String topic = randomString();
				for ( int i = 0; i < secretCount; i++ ) {
					// @formatter:off
						UserSecretEntity conf = newUserSecretEntity(
								userId,
								topic,
								"Key %d".formatted(i),
								randomString()
								);
						// @formatter:on
					UserStringStringCompositePK id = dao.create(userId, topic, conf);
					conf = conf.copyWithId(id);
					confs.add(conf);
				}
			}
		}

		// WHEN
		final UserSecretEntity randomConf = confs.get(RNG.nextInt(confs.size()));
		final var filter = new BasicUserSecretFilter();
		filter.setUserId(randomConf.getUserId());
		var results = dao.findFiltered(filter);

		// THEN
		UserSecretEntity[] expected = confs.stream()
				.filter(e -> randomConf.getUserId().equals(e.getUserId())).sorted()
				.toArray(UserSecretEntity[]::new);
		then(results).as("Results for single user returned").containsExactly(expected);
	}

}
