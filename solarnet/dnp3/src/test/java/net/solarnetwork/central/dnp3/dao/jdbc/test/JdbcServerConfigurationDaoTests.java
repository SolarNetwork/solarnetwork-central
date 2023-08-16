/* ==================================================================
 * JdbcServerConfigurationTests.java - 6/08/2023 7:30:21 pm
 * 
 * Copyright 2023 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.dnp3.dao.jdbc.test;

import static net.solarnetwork.central.dnp3.dao.jdbc.test.Dnp3JdbcTestUtils.allServerConfigurationData;
import static net.solarnetwork.central.dnp3.dao.jdbc.test.Dnp3JdbcTestUtils.newServerConfiguration;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.dnp3.dao.jdbc.JdbcServerConfigurationDao;
import net.solarnetwork.central.dnp3.domain.ServerConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;

/**
 * Test cases for the {@link JdbcServerConfigurationDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcServerConfigurationDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcServerConfigurationDao dao;
	private Long userId;

	private ServerConfiguration last;

	@BeforeEach
	public void setup() {
		dao = new JdbcServerConfigurationDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
	}

	@Test
	public void insert() {
		// GIVEN
		ServerConfiguration conf = newServerConfiguration(userId, UUID.randomUUID().toString());

		// WHEN
		UserLongCompositePK result = dao.create(userId, conf);

		// THEN

		// @formatter:off
		then(result).as("Primary key")
			.isNotNull()
			.as("User ID as provided")
			.returns(userId, UserLongCompositePK::getUserId)
			.as("ID generated")
			.doesNotReturn(null, UserLongCompositePK::getEntityId)
			;

		List<Map<String, Object>> data = allServerConfigurationData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1).asList().element(0, map(String.class, Object.class))
			.as("Row user ID")
			.containsEntry("user_id", userId)
			.as("Row ID generated")
			.containsKey("id")
			.as("Row creation date")
			.containsEntry("created", Timestamp.from(conf.getCreated()))
			.as("Row modification date")
			.containsEntry("modified", Timestamp.from(conf.getModified()))
			.as("Row enabled")
			.containsEntry("enabled", conf.isEnabled())
			.as("Row name")
			.containsEntry("cname", conf.getName())
			;
		// @formatter:on
		last = conf.copyWithId(result);
	}

	@Test
	public void get() {
		// GIVEN
		insert();

		// WHEN
		ServerConfiguration result = dao.get(last.getId());

		// THEN
		then(result).as("Retrieved entity matches source").isEqualTo(last);
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// WHEN
		ServerConfiguration conf = last.copyWithId(last.getId());
		conf.setEnabled(false);
		conf.setModified(Instant.now().plusMillis(474));
		conf.setName(UUID.randomUUID().toString());

		UserLongCompositePK result = dao.save(conf);
		ServerConfiguration updated = dao.get(result);

		// THEN
		List<Map<String, Object>> data = allServerConfigurationData(jdbcTemplate);
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
		List<Map<String, Object>> data = allServerConfigurationData(jdbcTemplate);
		then(data).as("Row deleted from db").isEmpty();
	}

	@Test
	public void findForUser() throws Exception {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<ServerConfiguration> confs = new ArrayList<>(count);

		for ( int i = 0; i < count; i++ ) {
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);
				} else {
					userId = userIds.get(u);
				}

				ServerConfiguration conf = Dnp3JdbcTestUtils.newServerConfiguration(userId,
						UUID.randomUUID().toString());
				UserLongCompositePK id = dao.create(userId, conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final Long userId = userIds.get(1);
		Collection<ServerConfiguration> results = dao.findAll(userId, null);

		// THEN
		ServerConfiguration[] expected = confs.stream().filter(e -> userId.equals(e.getUserId()))
				.toArray(ServerConfiguration[]::new);
		then(results).as("Results for single user returned").contains(expected);
	}

}
