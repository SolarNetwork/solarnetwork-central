/* ==================================================================
 * JdbcUserKeyPairEntityDaoTests.java - 22/03/2025 7:51:58 am
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
import static net.solarnetwork.central.test.CommonTestUtils.randomBytes;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static net.solarnetwork.central.user.dao.jdbc.test.UserJdbcTestUtils.allUserKeyPairEntityData;
import static net.solarnetwork.central.user.dao.jdbc.test.UserJdbcTestUtils.newUserKeyPairEntity;
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
import net.solarnetwork.central.domain.UserStringCompositePK;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;
import net.solarnetwork.central.user.dao.BasicUserSecretFilter;
import net.solarnetwork.central.user.dao.jdbc.JdbcUserKeyPairEntityDao;
import net.solarnetwork.central.user.domain.UserKeyPairEntity;
import net.solarnetwork.dao.Entity;

/**
 * Test cases for the {@link JdbcUserKeyPairEntityDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcUserKeyPairEntityDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcUserKeyPairEntityDao dao;
	private Long userId;

	private UserKeyPairEntity last;

	@BeforeEach
	public void setup() {
		dao = new JdbcUserKeyPairEntityDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
	}

	@Test
	public void entityKey() {
		UserStringCompositePK id = new UserStringCompositePK(randomLong(), randomString());
		UserKeyPairEntity result = dao.entityKey(id);

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
		final UserKeyPairEntity conf = newUserKeyPairEntity(
				userId,
				randomString(),
				randomBytes()
				);
		// @formatter:on

		// WHEN
		UserStringCompositePK result = dao.create(userId, conf);

		// THEN

		// @formatter:off
		then(result).as("Primary key")
			.isNotNull()
			.as("User ID as provided")
			.returns(userId, UserStringCompositePK::getUserId)
			.as("Key as provided")
			.returns(conf.getKey(), UserStringCompositePK::getEntityId)
			;

		List<Map<String, Object>> data = allUserKeyPairEntityData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1)
			.element(0, map(String.class, Object.class))
			.as("Row user ID")
			.containsEntry("user_id", userId)
			.as("Row key")
			.containsEntry("skey", conf.getKey())
			.as("Row creation date")
			.containsEntry("created", Timestamp.from(conf.getCreated()))
			.as("Row modification date")
			.containsEntry("modified", Timestamp.from(conf.getModified()))
			.as("Row keystore")
			.containsEntry("keystore", conf.keyStoreData())
			;
		// @formatter:on
		last = conf.copyWithId(result);
	}

	@Test
	public void get() {
		// GIVEN
		insert();

		// WHEN
		UserKeyPairEntity result = dao.get(last.getId());

		// THEN
		then(result).as("Retrieved entity matches source").isEqualTo(last);
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// WHEN
		Instant now = Instant.now();
		byte[] newKeystore = randomBytes();
		UserKeyPairEntity conf = new UserKeyPairEntity(last.getId(), last.getCreated(), now,
				newKeystore);

		UserStringCompositePK result = dao.save(conf);
		UserKeyPairEntity updated = dao.get(result);

		// THEN
		List<Map<String, Object>> data = allUserKeyPairEntityData(jdbcTemplate);
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
		List<Map<String, Object>> data = allUserKeyPairEntityData(jdbcTemplate);
		then(data).as("Row deleted from db").isEmpty();
	}

	@Test
	public void findForUser() throws Exception {
		// GIVEN
		final int userCount = 2;
		final int secretCount = 3;
		final List<UserKeyPairEntity> confs = new ArrayList<>(userCount * secretCount);

		for ( int u = 0; u < userCount; u++ ) {
			final Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			for ( int i = 0; i < secretCount; i++ ) {
				// @formatter:off
						UserKeyPairEntity conf = newUserKeyPairEntity(
								userId,
								"Key %d".formatted(i),
								randomBytes()
								);
						// @formatter:on
				UserStringCompositePK id = dao.create(userId, conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final UserKeyPairEntity randomConf = confs.get(RNG.nextInt(confs.size()));
		Collection<UserKeyPairEntity> results = dao.findAll(randomConf.getUserId(), null);

		// THEN
		UserKeyPairEntity[] expected = confs.stream()
				.filter(e -> randomConf.getUserId().equals(e.getUserId()))
				.toArray(UserKeyPairEntity[]::new);
		then(results).as("All results for user returned").containsExactly(expected);
	}

	@Test
	public void deleteForUser() throws Exception {
		// GIVEN
		final int userCount = 2;
		final int secretCount = 3;
		final List<UserKeyPairEntity> confs = new ArrayList<>(userCount * secretCount);

		for ( int u = 0; u < userCount; u++ ) {
			final Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			for ( int i = 0; i < secretCount; i++ ) {
				// @formatter:off
						UserKeyPairEntity conf = newUserKeyPairEntity(
								userId,
								"Key %d".formatted(i),
								randomBytes()
								);
						// @formatter:on
				UserStringCompositePK id = dao.create(userId, conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final UserKeyPairEntity randomConf = confs.get(RNG.nextInt(confs.size()));
		dao.delete(dao.entityKey(UserStringCompositePK.unassignedEntityIdKey(randomConf.getUserId())));

		// THEN
		List<Map<String, Object>> allRows = allUserKeyPairEntityData(jdbcTemplate);
		// @formatter:off
		then(allRows)
			.as("Should have deleted %d rows", secretCount)
			.hasSize(confs.size() - secretCount)
			.as("Should have deleted all rows for given (user) group")
			.noneMatch(row -> {
				return row.get("user_id").equals(randomConf.getUserId());
			})
			;
		// @formatter:on
	}

	@Test
	public void findFiltered_forUser() throws Exception {
		// GIVEN
		final int userCount = 2;
		final int secretCount = 3;
		final List<UserKeyPairEntity> confs = new ArrayList<>(userCount * secretCount);

		for ( int u = 0; u < userCount; u++ ) {
			final Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			for ( int i = 0; i < secretCount; i++ ) {
				// @formatter:off
						UserKeyPairEntity conf = newUserKeyPairEntity(
								userId,
								"Key %d".formatted(i),
								randomBytes()
								);
						// @formatter:on
				UserStringCompositePK id = dao.create(userId, conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final UserKeyPairEntity randomConf = confs.get(RNG.nextInt(confs.size()));
		final var filter = new BasicUserSecretFilter();
		filter.setUserId(randomConf.getUserId());
		var results = dao.findFiltered(filter);

		// THEN
		UserKeyPairEntity[] expected = confs.stream()
				.filter(e -> randomConf.getUserId().equals(e.getUserId()))
				.toArray(UserKeyPairEntity[]::new);
		then(results).as("Results for single user returned").containsExactly(expected);
	}

}
