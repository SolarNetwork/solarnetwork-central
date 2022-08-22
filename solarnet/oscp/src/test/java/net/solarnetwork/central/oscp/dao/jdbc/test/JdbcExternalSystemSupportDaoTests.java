/* ==================================================================
 * JdbcExternalSystemSupportDaoTests.java - 21/08/2022 6:12:28 pm
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

package net.solarnetwork.central.oscp.dao.jdbc.test;

import static net.solarnetwork.central.domain.UserLongCompositePK.unassignedEntityIdKey;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcCapacityOptimizerConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcCapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcExternalSystemSupportDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcFlexibilityProviderDao;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.OscpRole;
import net.solarnetwork.central.oscp.domain.RegistrationStatus;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;

/**
 * Test cases for the {@link JdbcExternalSystemSupportDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcExternalSystemSupportDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	@Autowired
	private PlatformTransactionManager txManager;

	private JdbcFlexibilityProviderDao flexibilityProviderDao;
	private JdbcCapacityProviderConfigurationDao capacityProviderDao;
	private JdbcCapacityOptimizerConfigurationDao capacityOptimizerDao;
	private JdbcExternalSystemSupportDao dao;
	private Long userId;
	private Long flexibilityProviderId;

	private CapacityProviderConfiguration lastProvider;

	@BeforeEach
	public void setup() {
		flexibilityProviderDao = new JdbcFlexibilityProviderDao(jdbcTemplate);
		capacityProviderDao = new JdbcCapacityProviderConfigurationDao(jdbcTemplate);
		capacityOptimizerDao = new JdbcCapacityOptimizerConfigurationDao(jdbcTemplate);
		dao = new JdbcExternalSystemSupportDao(jdbcTemplate, capacityProviderDao, capacityOptimizerDao);
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
		flexibilityProviderId = flexibilityProviderDao
				.idForToken(flexibilityProviderDao.createAuthToken(unassignedEntityIdKey(userId)))
				.getEntityId();
	}

	private List<Map<String, Object>> allCapacityProviderConfigurationData() {
		List<Map<String, Object>> data = jdbcTemplate
				.queryForList("select * from solaroscp.oscp_cp_conf ORDER BY user_id, id");
		log.debug("solaroscp.oscp_cp_conf table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(Collectors.joining("\n\t", "\n\t", "\n")));
		return data;
	}

	@Test
	public void capacityProvider_updateHeartbeat() {
		// GIVEN
		CapacityProviderConfiguration conf = JdbcCapacityProviderConfigurationDaoTests.newConf(userId,
				flexibilityProviderId, Instant.now());
		UserLongCompositePK id = capacityProviderDao.create(userId, conf);
		jdbcTemplate.update("UPDATE solaroscp.oscp_cp_conf SET reg_status = ?, heartbeat_secs = ?",
				RegistrationStatus.Registered.getCode(), 1);
		lastProvider = capacityProviderDao.get(id);

		// WHEN
		Instant newTs = Instant.now();
		boolean result = dao.processExternalSystemWithExpiredHeartbeat((ctx) -> {
			assertThat("Role is provider", ctx.role(), is(equalTo(OscpRole.CapacityProvider)));
			assertThat("Found provider row", ctx.config().getId(), is(equalTo(lastProvider.getId())));
			return newTs;
		});

		// THEN
		assertThat("Result 'true' when Instant returned from callback", result, is(equalTo(result)));
		List<Map<String, Object>> data = allCapacityProviderConfigurationData();
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row user ID matches", row, hasEntry("user_id", lastProvider.getUserId()));
		assertThat("Row ID matches", row, hasEntry("id", lastProvider.getEntityId()));
		assertThat("Row heartbeat date updated", row, hasEntry("heartbeat_at", Timestamp.from(newTs)));
	}

	@Test
	public void capacityProvider_updateHeartbeat_skipLocked() {
		// GIVEN
		CapacityProviderConfiguration conf = JdbcCapacityProviderConfigurationDaoTests.newConf(userId,
				flexibilityProviderId, Instant.now());
		UserLongCompositePK id = capacityProviderDao.create(userId, conf);
		jdbcTemplate.update("UPDATE solaroscp.oscp_cp_conf SET reg_status = ?, heartbeat_secs = ?",
				RegistrationStatus.Registered.getCode(), 1);
		lastProvider = capacityProviderDao.get(id);

		log.info("Conf ID: {}", conf.getId());

		// WHEN
		AtomicBoolean updateFailed = new AtomicBoolean();

		AtomicBoolean result = new AtomicBoolean(false);
		Instant newTs = Instant.now();

		try {
			TestTransaction.flagForCommit();
			TestTransaction.end();

			TransactionTemplate tt = new TransactionTemplate(txManager);
			CountDownLatch latch = new CountDownLatch(1);

			tt.executeWithoutResult((ts) -> {
				boolean b = dao.processExternalSystemWithExpiredHeartbeat((ctx) -> {
					log.info("Locked ID: {}", ctx.config().getId());

					Thread t = new Thread(() -> {
						tt.executeWithoutResult((ts2) -> {
							try {
								jdbcTemplate.queryForList(
										"SELECT * FROM solaroscp.oscp_cp_conf LIMIT 1 FOR UPDATE NOWAIT");
							} catch ( ConcurrencyFailureException e ) {
								updateFailed.set(true);
							} finally {
								log.info("Tx 2 signaling");
								latch.countDown();
							}
						});
					}, "Update 2");
					t.start();

					log.info("Tx 1 waiting within lock");
					try {
						latch.await(1L, TimeUnit.MINUTES);
					} catch ( InterruptedException e ) {
						// ignore
					}
					assertThat("Role is provider", ctx.role(), is(equalTo(OscpRole.CapacityProvider)));
					assertThat("Found provider row", ctx.config().getId(),
							is(equalTo(lastProvider.getId())));
					return newTs;
				});
				result.set(b);
			});
		} finally {
			JdbcTestUtils.deleteFromTables((JdbcTemplate) jdbcTemplate, "solaruser.user_user",
					"solaroscp.oscp_cp_conf");
		}

		assertThat("Update 1 succeeded", result.get(), is(equalTo(true)));
		assertThat("Update 2 failed", updateFailed.get(), is(equalTo(true)));
	}

}
