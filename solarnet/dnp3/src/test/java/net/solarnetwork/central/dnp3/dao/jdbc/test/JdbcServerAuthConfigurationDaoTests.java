/* ==================================================================
 * JdbcServerAuthConfigurationDaoTests.java - 7/08/2023 6:56:43 am
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

import static net.solarnetwork.central.dnp3.dao.jdbc.test.Dnp3JdbcTestUtils.allServerAuthConfigurationData;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.InstanceOfAssertFactories.map;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.dnp3.dao.jdbc.JdbcServerAuthConfigurationDao;
import net.solarnetwork.central.dnp3.dao.jdbc.JdbcServerConfigurationDao;
import net.solarnetwork.central.dnp3.domain.ServerAuthConfiguration;
import net.solarnetwork.central.dnp3.domain.ServerConfiguration;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.domain.UserLongStringCompositePK;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;

/**
 * Test cases for the {@link JdbcServerAuthConfigurationDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcServerAuthConfigurationDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcServerConfigurationDao serverDao;

	private JdbcServerAuthConfigurationDao dao;

	private Long userId;
	private ServerConfiguration lastServer;
	private ServerAuthConfiguration last;

	@BeforeEach
	public void setup() {
		serverDao = new JdbcServerConfigurationDao(jdbcTemplate);
		dao = new JdbcServerAuthConfigurationDao(jdbcTemplate);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
	}

	@Test
	public void insert() {
		// GIVEN
		lastServer = serverDao.get(serverDao.create(userId,
				Dnp3JdbcTestUtils.newServerConfiguration(userId, UUID.randomUUID().toString())));

		// WHEN
		ServerAuthConfiguration conf = new ServerAuthConfiguration(userId, lastServer.getServerId(),
				UUID.randomUUID().toString(), Instant.now());
		conf.setModified(Instant.now().plusMillis(234L));
		conf.setName(UUID.randomUUID().toString());
		UserLongStringCompositePK result = dao.create(userId, lastServer.getServerId(), conf);

		// THEN
		// @formatter:off
		then(result).as("Primary key")
			.isNotNull()
			.as("User ID as provided")
			.returns(conf.getUserId(), UserLongStringCompositePK::getUserId)
			.as("Server ID as provided")
			.returns(conf.getServerId(), UserLongStringCompositePK::getGroupId)
			.as("Ident as provided")
			.returns(conf.getIdentifier(), UserLongStringCompositePK::getEntityId)
			;

		List<Map<String, Object>> data = allServerAuthConfigurationData(jdbcTemplate);
		then(data).as("Table has 1 row").hasSize(1).asList().element(0, map(String.class, Object.class))
			.as("Row user ID")
			.containsEntry("user_id", conf.getUserId())
			.as("Row server ID")
			.containsEntry("server_id", conf.getServerId())
			.as("Row identifier")
			.containsEntry("ident", conf.getIdentifier())
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
		ServerAuthConfiguration result = dao.get(last.getId());

		// THEN
		then(result).as("Retrieved entity matches source").isEqualTo(last);
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// WHEN
		ServerAuthConfiguration conf = last.copyWithId(last.getId());
		conf.setEnabled(false);
		conf.setModified(Instant.now().plusMillis(474));
		conf.setName(UUID.randomUUID().toString());

		UserLongStringCompositePK result = dao.save(conf);
		ServerAuthConfiguration updated = dao.get(result);

		// THEN
		List<Map<String, Object>> data = allServerAuthConfigurationData(jdbcTemplate);
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
		List<Map<String, Object>> data = allServerAuthConfigurationData(jdbcTemplate);
		then(data).as("Row deleted from db").isEmpty();
	}

	@Test
	public void findForUser() throws Exception {
		// GIVEN
		final int userCount = 3;
		final int serverCount = 3;
		final int count = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<ServerAuthConfiguration> confs = new ArrayList<>(count);

		for ( int u = 0; u < userCount; u++ ) {
			Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			userIds.add(userId);

			for ( int s = 0; s < serverCount; s++ ) {
				ServerConfiguration server = Dnp3JdbcTestUtils.newServerConfiguration(userId,
						UUID.randomUUID().toString());
				UserLongCompositePK serverId = serverDao.create(userId, server);
				server = server.copyWithId(serverId);

				for ( int i = 0; i < count; i++ ) {
					ServerAuthConfiguration conf = new ServerAuthConfiguration(userId,
							server.getServerId(), UUID.randomUUID().toString(), Instant.now());
					conf.setModified(conf.getCreated());
					conf.setName(UUID.randomUUID().toString());
					UserLongStringCompositePK id = dao.create(userId, server.getServerId(), conf);
					confs.add(conf.copyWithId(id));
				}
			}
		}

		// WHEN
		final Long userId = userIds.get(1);
		Collection<ServerAuthConfiguration> results = dao.findAll(userId, null, null);

		// THEN
		ServerAuthConfiguration[] expected = confs.stream().filter(e -> userId.equals(e.getUserId()))
				.toArray(ServerAuthConfiguration[]::new);
		then(results).as("Results for single user returned").contains(expected);
	}

	@Test
	public void findForGroup() throws Exception {
		// GIVEN
		final int userCount = 3;
		final int serverCount = 3;
		final int count = 3;
		final Map<Long, List<Long>> userGroups = new HashMap<>(userCount);
		final List<ServerAuthConfiguration> confs = new ArrayList<>(count);

		for ( int u = 0; u < userCount; u++ ) {
			Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			userGroups.put(userId, new ArrayList<>(serverCount));

			for ( int s = 0; s < serverCount; s++ ) {
				ServerConfiguration server = Dnp3JdbcTestUtils.newServerConfiguration(userId,
						UUID.randomUUID().toString());
				UserLongCompositePK serverId = serverDao.create(userId, server);
				server = server.copyWithId(serverId);
				userGroups.get(userId).add(server.getServerId());

				for ( int i = 0; i < count; i++ ) {
					ServerAuthConfiguration conf = new ServerAuthConfiguration(userId,
							server.getServerId(), UUID.randomUUID().toString(), Instant.now());
					conf.setModified(conf.getCreated());
					conf.setName(UUID.randomUUID().toString());
					UserLongStringCompositePK id = dao.create(userId, server.getServerId(), conf);
					confs.add(conf.copyWithId(id));
				}
			}
		}

		// WHEN
		final Entry<Long, List<Long>> groups = userGroups.entrySet().iterator().next();
		final Long groupId = groups.getValue().get(1);
		Collection<ServerAuthConfiguration> results = dao.findAll(groups.getKey(), groupId, null);

		// THEN
		ServerAuthConfiguration[] expected = confs.stream()
				.filter(e -> groups.getKey().equals(e.getUserId()) && groupId.equals(e.getServerId()))
				.toArray(ServerAuthConfiguration[]::new);
		then(results).as("Results for single group returned").contains(expected);
	}

	@Test
	public void findForIdentifier() {
		// GIVEN
		final int userCount = 3;
		final int serverCount = 3;
		final int count = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<ServerAuthConfiguration> confs = new ArrayList<>(count);

		for ( int u = 0; u < userCount; u++ ) {
			Long userId = CommonDbTestUtils.insertUser(jdbcTemplate);
			userIds.add(userId);

			for ( int s = 0; s < serverCount; s++ ) {
				ServerConfiguration server = Dnp3JdbcTestUtils.newServerConfiguration(userId,
						UUID.randomUUID().toString());
				server.setEnabled(true);
				UserLongCompositePK serverId = serverDao.create(userId, server);
				server = server.copyWithId(serverId);

				for ( int i = 0; i < count; i++ ) {
					ServerAuthConfiguration conf = new ServerAuthConfiguration(userId,
							server.getServerId(), UUID.randomUUID().toString(), Instant.now());
					conf.setModified(conf.getCreated());
					conf.setName(UUID.randomUUID().toString());
					conf.setEnabled(true);
					UserLongStringCompositePK id = dao.create(userId, server.getServerId(), conf);
					confs.add(conf.copyWithId(id));
				}
			}
		}

		// WHEN
		final var expected = confs.get(new SecureRandom().nextInt(confs.size()));
		ServerAuthConfiguration result = dao.findForIdentifier(expected.getIdentifier());

		// THEN
		then(result).as("Result found for identifier").isEqualTo(expected);
	}

	@Test
	public void findForIdentifier_disabledServer() {
		// GIVEN
		ServerConfiguration server = Dnp3JdbcTestUtils.newServerConfiguration(userId,
				UUID.randomUUID().toString());
		server.setEnabled(false);
		UserLongCompositePK serverId = serverDao.create(userId, server);
		server = server.copyWithId(serverId);

		ServerAuthConfiguration conf = new ServerAuthConfiguration(userId, server.getServerId(),
				UUID.randomUUID().toString(), Instant.now());
		conf.setModified(conf.getCreated());
		conf.setName(UUID.randomUUID().toString());
		conf.setEnabled(true);
		dao.create(userId, server.getServerId(), conf);

		// WHEN
		ServerAuthConfiguration result = dao.findForIdentifier(conf.getIdentifier());

		// THEN
		then(result).as("Result not found for identifier when associated server is disabled").isNull();
	}

	@Test
	public void findForIdentifier_disabledAuth() {
		// GIVEN
		ServerConfiguration server = Dnp3JdbcTestUtils.newServerConfiguration(userId,
				UUID.randomUUID().toString());
		server.setEnabled(true);
		UserLongCompositePK serverId = serverDao.create(userId, server);
		server = server.copyWithId(serverId);

		ServerAuthConfiguration conf = new ServerAuthConfiguration(userId, server.getServerId(),
				UUID.randomUUID().toString(), Instant.now());
		conf.setModified(conf.getCreated());
		conf.setName(UUID.randomUUID().toString());
		conf.setEnabled(false);
		dao.create(userId, server.getServerId(), conf);

		// WHEN
		ServerAuthConfiguration result = dao.findForIdentifier(conf.getIdentifier());

		// THEN
		then(result).as("Result not found for identifier when auth is disabled").isNull();
	}

}
