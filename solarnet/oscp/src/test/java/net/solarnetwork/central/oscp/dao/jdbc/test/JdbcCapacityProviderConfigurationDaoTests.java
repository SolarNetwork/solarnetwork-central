/* ==================================================================
 * JdbcCapacityProviderConfigurationDaoTests.java - 12/08/2022 6:33:46 pm
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

import static java.util.UUID.randomUUID;
import static net.solarnetwork.central.domain.UserLongCompositePK.unassignedEntityIdKey;
import static net.solarnetwork.codec.JsonUtils.getStringMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
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
import net.solarnetwork.central.oscp.dao.jdbc.JdbcCapacityProviderConfigurationDao;
import net.solarnetwork.central.oscp.dao.jdbc.JdbcFlexibilityProviderDao;
import net.solarnetwork.central.oscp.domain.CapacityProviderConfiguration;
import net.solarnetwork.central.oscp.domain.MeasurementStyle;
import net.solarnetwork.central.oscp.domain.RegistrationStatus;
import net.solarnetwork.central.oscp.domain.SystemSettings;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;
import net.solarnetwork.central.test.CommonDbTestUtils;

/**
 * Test cases for the {@link JdbcCapacityProviderConfigurationDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcCapacityProviderConfigurationDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	@Autowired
	private PlatformTransactionManager txManager;

	private JdbcFlexibilityProviderDao flexibilityProviderDao;
	private JdbcCapacityProviderConfigurationDao dao;
	private Long userId;
	private Long flexibilityProviderId;

	private CapacityProviderConfiguration last;
	private SystemSettings lastSettings;

	@BeforeEach
	public void setup() {
		flexibilityProviderDao = new JdbcFlexibilityProviderDao(jdbcTemplate);
		dao = new JdbcCapacityProviderConfigurationDao(jdbcTemplate);
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

	private List<Map<String, Object>> allCapacityProviderTokenData() {
		List<Map<String, Object>> data = jdbcTemplate
				.queryForList("select * from solaroscp.oscp_cp_token ORDER BY user_id, id");
		log.debug("solaroscp.oscp_cp_token table has {} items: [{}]", data.size(),
				data.stream().map(Object::toString).collect(Collectors.joining("\n\t", "\n\t", "\n")));
		return data;
	}

	/**
	 * Create a new configuration instance.
	 * 
	 * @param userId
	 *        the user ID
	 * @param flexibilityProviderId
	 *        the flexibility provider ID
	 * @param created
	 *        the creation date
	 * @return the new instance
	 */
	public static CapacityProviderConfiguration newConf(Long userId, Long flexibilityProviderId,
			Instant created) {
		CapacityProviderConfiguration conf = new CapacityProviderConfiguration(
				UserLongCompositePK.unassignedEntityIdKey(userId), created);
		conf.setModified(created);
		conf.setBaseUrl("http://example.com/" + randomUUID().toString());
		conf.setEnabled(true);
		conf.setFlexibilityProviderId(flexibilityProviderId);
		conf.setName(randomUUID().toString());
		conf.setRegistrationStatus(RegistrationStatus.Registered);
		conf.setServiceProps(Collections.singletonMap("foo", randomUUID().toString()));
		conf.setToken(randomUUID().toString());
		return conf;
	}

	@Test
	public void insert() {
		// GIVEN
		CapacityProviderConfiguration conf = newConf(userId, flexibilityProviderId, Instant.now());

		// WHEN
		UserLongCompositePK result = dao.create(userId, conf);

		// THEN
		List<Map<String, Object>> data = allCapacityProviderConfigurationData();
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row ID has been generated by DB", row,
				hasEntry(equalTo("id"), allOf(notNullValue(), not(equalTo(conf.getEntityId())))));
		assertThat("Primary key returned", result, is(notNullValue()));
		assertThat("Row creation date", row, hasEntry("created", Timestamp.from(conf.getCreated())));
		assertThat("Row modification date is creation date", row,
				hasEntry("modified", Timestamp.from(conf.getCreated())));
		assertThat("Row user ID matches", row, hasEntry("user_id", conf.getUserId()));
		assertThat("Row enabled matches", row, hasEntry("enabled", conf.isEnabled()));
		assertThat("Row reg status matches", row,
				hasEntry("reg_status", conf.getRegistrationStatus().getCode()));
		assertThat("Row name matches", row, hasEntry("cname", conf.getName()));
		assertThat("Row baseUrl matches", row, hasEntry("url", conf.getBaseUrl()));
		assertThat("Row serviceProps matches", getStringMap(row.get("sprops").toString()),
				is(equalTo(conf.getServiceProps())));
		assertThat("Row matches returned primary key result", result,
				is(equalTo(new UserLongCompositePK(userId, (Long) row.get("id")))));

		last = conf.copyWithId(result);
	}

	@Test
	public void insert_authToken() {
		// GIVEN
		insert();

		// WHEN
		String token = randomUUID().toString();
		dao.saveExternalSystemAuthToken(last.getId(), token);

		// THEN
		List<Map<String, Object>> data = allCapacityProviderTokenData();
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row user ID has been assigned", row, hasEntry("user_id", last.getUserId()));
		assertThat("Row ID has been assigned", row, hasEntry("id", last.getEntityId()));
		assertThat("Row creation date assigned", row, hasEntry(equalTo("created"), notNullValue()));
		assertThat("Row modification date is creation date", row,
				hasEntry("modified", row.get("created")));
		assertThat("Row token matches return value", row, hasEntry("token", token));
	}

	@Test
	public void get_authToken() {
		// GIVEN
		insert();

		final String token = randomUUID().toString();
		dao.saveExternalSystemAuthToken(last.getId(), token);

		// WHEN
		String result = dao.getExternalSystemAuthToken(last.getId());

		assertThat("Result token matches saved value", result, is(equalTo(token)));
	}

	@Test
	public void get() {
		// GIVEN
		insert();

		// WHEN
		CapacityProviderConfiguration result = dao.get(last.getId());

		// THEN
		assertThat("Retrieved entity ID matches source", result, is(equalTo(last)));
		assertThat("Retrieved enabled matches", result.isEnabled(), is(equalTo(last.isEnabled())));
		assertThat("Retrieved name matches", result.getName(), is(equalTo(last.getName())));
		assertThat("Retrieved oscpVersion matches", result.getOscpVersion(),
				is(equalTo(last.getOscpVersion())));
		assertThat("Retrieved baseUrl matches", result.getBaseUrl(), is(equalTo(last.getBaseUrl())));
		assertThat("Retrieved reg status matches", result.getRegistrationStatus(),
				is(equalTo(last.getRegistrationStatus())));
		assertThat("Retrieved flexibilityProviderId matches", result.getFlexibilityProviderId(),
				is(equalTo(last.getFlexibilityProviderId())));
		assertThat("Retrieved serviceProps matches", result.getServiceProps(),
				is(equalTo(last.getServiceProps())));
	}

	@Test
	public void getForUpdate() throws InterruptedException {
		// GIVEN
		insert();

		// WHEN
		AtomicBoolean updateFailed = new AtomicBoolean();

		try {
			TestTransaction.flagForCommit();
			TestTransaction.end();

			TransactionTemplate tt = new TransactionTemplate(txManager);

			tt.executeWithoutResult((ts) -> {
				CapacityProviderConfiguration result = dao.getForUpdate(last.getId());
				Thread t = new Thread(() -> {
					tt.executeWithoutResult((ts2) -> {
						try {
							jdbcTemplate.queryForList(
									"SELECT * FROM solaroscp.oscp_cp_conf WHERE user_id = ? AND id = ? FOR UPDATE NOWAIT",
									result.getUserId(), result.getEntityId());
						} catch ( ConcurrencyFailureException e ) {
							updateFailed.set(true);
						}
						log.info("Update 2 signaling");
					});
				}, "Update 2");
				t.start();
				try {
					t.join(5000L);
				} catch ( InterruptedException e ) {
					// ignore
				}
			});
		} finally {
			JdbcTestUtils.deleteFromTables((JdbcTemplate) jdbcTemplate, "solaruser.user_user",
					"solaroscp.oscp_cp_conf", "solaroscp.oscp_fp_token");
		}

		// THEN
		assertThat("Update 2 failed", updateFailed.get(), is(equalTo(true)));
	}

	@Test
	public void update() {
		// GIVEN
		insert();

		// WHEN
		CapacityProviderConfiguration conf = last.copyWithId(last.getId());
		conf.setBaseUrl(randomUUID().toString());
		conf.setEnabled(false);
		conf.setModified(Instant.now().plusMillis(474));
		conf.setName(randomUUID().toString());
		conf.setRegistrationStatus(RegistrationStatus.Failed);
		conf.setServiceProps(Collections.singletonMap("bim", "bam"));

		UserLongCompositePK result = dao.save(conf);

		// THEN
		List<Map<String, Object>> data = allCapacityProviderConfigurationData();
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row ID has been generated by DB", row, hasEntry("id", conf.getEntityId()));
		assertThat("Primary key returned unchanged", result, is(equalTo(conf.getId())));
		assertThat("Row creation date unchanged", row,
				hasEntry("created", Timestamp.from(conf.getCreated())));
		assertThat("Row modification date is updated", row,
				hasEntry("modified", Timestamp.from(conf.getModified())));
		assertThat("Row user ID matches", row, hasEntry("user_id", conf.getUserId()));
		assertThat("Row enabled matches", row, hasEntry("enabled", conf.isEnabled()));
		assertThat("Row reg status matches", row,
				hasEntry("reg_status", conf.getRegistrationStatus().getCode()));
		assertThat("Row name matches", row, hasEntry("cname", conf.getName()));
		assertThat("Row baseUrl matches", row, hasEntry("url", conf.getBaseUrl()));
		assertThat("Row serviceProps matches", getStringMap(row.get("sprops").toString()),
				is(equalTo(conf.getServiceProps())));
	}

	@Test
	public void saveSettings() {
		// GIVEN
		insert();

		// WHEN
		SystemSettings settings = new SystemSettings(123,
				EnumSet.of(MeasurementStyle.Continuous, MeasurementStyle.Intermittent));
		dao.saveSettings(last.getId(), settings);

		// THEN
		List<Map<String, Object>> data = allCapacityProviderConfigurationData();
		assertThat("Table has 1 row", data, hasSize(1));
		Map<String, Object> row = data.get(0);
		assertThat("Row heartbeat matches return value", row,
				hasEntry("heartbeat_secs", settings.heartbeatSeconds()));
		lastSettings = settings;
	}

	@Test
	public void get_settings() {
		// GIVEN
		saveSettings();

		// WHEN
		CapacityProviderConfiguration result = dao.get(last.getId());

		// THEN
		assertThat("Retrieved entity ID matches source", result, is(equalTo(last)));
		assertThat("Retrieved enabled matches", result.isEnabled(), is(equalTo(last.isEnabled())));
		assertThat("Retrieved name matches", result.getName(), is(equalTo(last.getName())));
		assertThat("Retrieved oscpVersion matches", result.getOscpVersion(),
				is(equalTo(last.getOscpVersion())));
		assertThat("Retrieved baseUrl matches", result.getBaseUrl(), is(equalTo(last.getBaseUrl())));
		assertThat("Retrieved reg status matches", result.getRegistrationStatus(),
				is(equalTo(last.getRegistrationStatus())));
		assertThat("Retrieved flexibilityProviderId matches", result.getFlexibilityProviderId(),
				is(equalTo(last.getFlexibilityProviderId())));
		assertThat("Retrieved settings matches", result.getSettings(), is(equalTo(lastSettings)));
		assertThat("Retrieved settings matches", result.getLastHeartbeat(),
				is(equalTo(last.getLastHeartbeat())));
		assertThat("Retrieved serviceProps matches", result.getServiceProps(),
				is(equalTo(last.getServiceProps())));
	}

	@Test
	public void delete() {
		// GIVEN
		insert();

		// WHEN
		dao.delete(last);

		// THEN
		List<Map<String, Object>> data = allCapacityProviderConfigurationData();
		assertThat("Row deleted from db", data, hasSize(0));
	}

	@Test
	public void findForUser() {
		// GIVEN
		final int count = 3;
		final int userCount = 3;
		final List<Long> userIds = new ArrayList<>(userCount);
		final List<Long> flexibilityProviderIds = new ArrayList<>(userCount);
		final List<CapacityProviderConfiguration> confs = new ArrayList<>(count);
		final Instant start = Instant.now().truncatedTo(ChronoUnit.MINUTES);
		for ( int i = 0; i < count; i++ ) {
			Instant t = start.plusSeconds(i);
			for ( int u = 0; u < userCount; u++ ) {
				Long userId;
				Long flexibilityProviderId;
				if ( i == 0 ) {
					userId = CommonDbTestUtils.insertUser(jdbcTemplate);
					userIds.add(userId);
					flexibilityProviderId = flexibilityProviderDao.idForToken(
							flexibilityProviderDao.createAuthToken(unassignedEntityIdKey(userId)))
							.getEntityId();
					flexibilityProviderIds.add(flexibilityProviderId);
				} else {
					userId = userIds.get(u);
					flexibilityProviderId = flexibilityProviderIds.get(u);
				}
				CapacityProviderConfiguration conf = newConf(userId, flexibilityProviderId, t);
				UserLongCompositePK id = dao.create(userId, conf);
				conf = conf.copyWithId(id);
				confs.add(conf);
			}
		}

		// WHEN
		final Long userId = userIds.get(1);
		Collection<CapacityProviderConfiguration> results = dao.findAll(userId, null);

		// THEN
		CapacityProviderConfiguration[] expected = confs.stream()
				.filter(e -> userId.equals(e.getUserId())).toArray(CapacityProviderConfiguration[]::new);
		assertThat("Results for single user returned", results, contains(expected));
	}

}
