/* ==================================================================
 * JdbcCloudDatumStreamPollTaskDao_DbStartStopTests.java - 13/10/2024 10:19:16 am
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
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.allCloudDatumStreamConfigurationData;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.allCloudDatumStreamPollTaskEntityData;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.allCloudIntegrationConfigurationData;
import static net.solarnetwork.central.c2c.dao.jdbc.test.CinJdbcTestUtils.newCloudDatumStreamPollTaskEntity;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Completed;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Queued;
import static net.solarnetwork.central.test.CommonTestUtils.RNG;
import static net.solarnetwork.central.test.CommonTestUtils.randomLong;
import static net.solarnetwork.central.test.CommonTestUtils.randomString;
import static org.assertj.core.api.BDDAssertions.then;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import net.solarnetwork.central.c2c.dao.BasicFilter;
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
import net.solarnetwork.domain.datum.ObjectDatumKind;

/**
 * Test cases for the datum stream task related status triggers on the datum
 * stream and integration enabled columns.
 *
 * @author matt
 * @version 1.0
 */
public class JdbcCloudDatumStreamPollTaskDao_DbStartStopTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcCloudIntegrationConfigurationDao integrationDao;
	private JdbcCloudDatumStreamConfigurationDao datumStreamDao;
	private JdbcCloudDatumStreamPollTaskDao datumStreamPollTaskDao;
	private Long userId;

	private Map<UserLongCompositePK, CloudIntegrationConfiguration> integrations;
	private Map<UserLongCompositePK, CloudDatumStreamConfiguration> datumStreams;
	private Map<UserLongCompositePK, CloudDatumStreamPollTaskEntity> datumStreamPollTasks;

	@BeforeEach
	public void setup() {
		userId = CommonDbTestUtils.insertUser(jdbcTemplate);
		integrationDao = new JdbcCloudIntegrationConfigurationDao(jdbcTemplate);
		datumStreamDao = new JdbcCloudDatumStreamConfigurationDao(jdbcTemplate);
		datumStreamPollTaskDao = new JdbcCloudDatumStreamPollTaskDao(jdbcTemplate);

		integrations = new LinkedHashMap<>();
		datumStreams = new LinkedHashMap<>();
		datumStreamPollTasks = new LinkedHashMap<>();
	}

	private CloudIntegrationConfiguration createIntegration(Long userId, boolean enabled) {
		CloudIntegrationConfiguration conf = CinJdbcTestUtils.newCloudIntegrationConfiguration(userId,
				randomString(), randomString(), null);
		conf.setEnabled(enabled);
		CloudIntegrationConfiguration entity = integrationDao.get(integrationDao.save(conf));
		integrations.put(entity.getId(), entity);
		return entity;
	}

	private CloudDatumStreamConfiguration createDatumStream(Long userId, Long integrationId,
			boolean enabled) {
		CloudDatumStreamConfiguration conf = CinJdbcTestUtils.newCloudDatumStreamConfiguration(userId,
				integrationId, randomString(), ObjectDatumKind.Node, randomLong(), randomString(),
				randomString(), randomString(), null);
		conf.setEnabled(enabled);
		CloudDatumStreamConfiguration entity = datumStreamDao.get(datumStreamDao.save(conf));
		datumStreams.put(entity.getId(), entity);
		return entity;
	}

	private CloudDatumStreamPollTaskEntity createDatumStreamPollTask(Long userId, Long datumStreamId,
			BasicClaimableJobState state) {
		// @formatter:off
		CloudDatumStreamPollTaskEntity conf = newCloudDatumStreamPollTaskEntity(userId,
				datumStreamId,
				state,
				now().truncatedTo(ChronoUnit.SECONDS).minus(1L, ChronoUnit.DAYS),
				now().truncatedTo(ChronoUnit.DAYS),
				randomString(),
				null)
				;
		// @formatter:on
		CloudDatumStreamPollTaskEntity entity = datumStreamPollTaskDao
				.get(datumStreamPollTaskDao.save(conf));
		datumStreamPollTasks.put(entity.getId(), entity);
		return entity;
	}

	@Test
	public void disableIntegrationStopsAllAssociatedTasks() {
		// GIVEN
		final int integrationCount = 3;
		final int datumStreamCount = 5;
		for ( int i = 0; i < integrationCount; i++ ) {
			CloudIntegrationConfiguration integration = createIntegration(userId, true);
			for ( int d = 0; d < datumStreamCount; d++ ) {
				CloudDatumStreamConfiguration datumStream = createDatumStream(userId,
						integration.getConfigId(), RNG.nextBoolean());
				createDatumStreamPollTask(userId, datumStream.getConfigId(), Queued);
			}
		}

		final CloudIntegrationConfiguration randomIntegration = integrations.values().stream().toList()
				.get(RNG.nextInt(integrationCount));

		// WHEN
		BasicFilter filter = new BasicFilter();
		filter.setIntegrationId(randomIntegration.getConfigId());
		integrationDao.updateEnabledStatus(userId, filter, false);

		// THEN
		allCloudIntegrationConfigurationData(jdbcTemplate);
		allCloudDatumStreamConfigurationData(jdbcTemplate);
		final List<Map<String, Object>> taskRows = allCloudDatumStreamPollTaskEntityData(jdbcTemplate);

		// @formatter:off
		then(taskRows)
			.as("Rows for tasks found")
			.hasSize(integrationCount * datumStreamCount)
			.satisfies(rows -> {
				for (int idx = 0, len = rows.size(); idx < len; idx++ ) {
					then(rows)
						.element(idx)
						.satisfies(row -> {
							CloudDatumStreamConfiguration datumStream = datumStreams.get(new UserLongCompositePK(
									userId, (Long)row.get("ds_id")));
							BasicClaimableJobState expectedState = datumStream.getIntegrationId()
										.equals(randomIntegration.getConfigId()) && datumStream.isEnabled()
									? Completed : Queued;
							then(row)
								.as("""
									Task state only Completed for datum stream %d that was enabled under
									integration %d that was then disabled: %s
									""", datumStream.getConfigId(), datumStream.getIntegrationId(), datumStream)
								.containsEntry("status", expectedState.keyValue())
								;
						})
						;
				}
			})
			;
		// @formatter:on
	}

	@Test
	public void enableIntegrationStartsAllAssociatedTasks() {
		// GIVEN
		final int integrationCount = 3;
		final int datumStreamCount = 5;
		for ( int i = 0; i < integrationCount; i++ ) {
			CloudIntegrationConfiguration integration = createIntegration(userId, false);
			for ( int d = 0; d < datumStreamCount; d++ ) {
				CloudDatumStreamConfiguration datumStream = createDatumStream(userId,
						integration.getConfigId(), RNG.nextBoolean());
				createDatumStreamPollTask(userId, datumStream.getConfigId(), Completed);
			}
		}

		final CloudIntegrationConfiguration randomIntegration = integrations.values().stream().toList()
				.get(RNG.nextInt(integrationCount));

		// WHEN
		BasicFilter filter = new BasicFilter();
		filter.setIntegrationId(randomIntegration.getConfigId());
		integrationDao.updateEnabledStatus(userId, filter, true);

		// THEN
		allCloudIntegrationConfigurationData(jdbcTemplate);
		allCloudDatumStreamConfigurationData(jdbcTemplate);
		final List<Map<String, Object>> taskRows = allCloudDatumStreamPollTaskEntityData(jdbcTemplate);

		// @formatter:off
		then(taskRows)
			.as("Rows for tasks found")
			.hasSize(integrationCount * datumStreamCount)
			.satisfies(rows -> {
				for (int idx = 0, len = rows.size(); idx < len; idx++ ) {
					then(rows)
						.element(idx)
						.satisfies(row -> {
							CloudDatumStreamConfiguration datumStream = datumStreams.get(new UserLongCompositePK(
									userId, (Long)row.get("ds_id")));
							BasicClaimableJobState expectedState = datumStream.getIntegrationId()
										.equals(randomIntegration.getConfigId()) && datumStream.isEnabled()
									? Queued : Completed;
							then(row)
								.as("""
									Task state only Queued for datum stream %d that was enabled under
									integration %d that was then enabled: %s
									""", datumStream.getConfigId(), datumStream.getIntegrationId(), datumStream)
								.containsEntry("status", expectedState.keyValue())
								;
						})
						;
				}
			})
			;
		// @formatter:on
	}

	@Test
	public void disableDatumStreamStopsAssociatedTask() {
		// GIVEN
		final int integrationCount = 5;
		final int datumStreamCount = 2;
		for ( int i = 0; i < integrationCount; i++ ) {
			CloudIntegrationConfiguration integration = createIntegration(userId,
					i == 0 ? true : RNG.nextBoolean());
			for ( int d = 0; d < datumStreamCount; d++ ) {
				CloudDatumStreamConfiguration datumStream = createDatumStream(userId,
						integration.getConfigId(), true);
				createDatumStreamPollTask(userId, datumStream.getConfigId(),
						integration.isEnabled() ? Queued : Completed);
			}
		}

		final List<CloudIntegrationConfiguration> enabledIntegrations = integrations.values().stream()
				.filter(CloudIntegrationConfiguration::isEnabled).toList();
		final CloudIntegrationConfiguration randomIntegration = enabledIntegrations
				.get(RNG.nextInt(enabledIntegrations.size()));
		final CloudDatumStreamConfiguration randomDatumStream = datumStreams.values().stream()
				.filter(ds -> ds.getIntegrationId().equals(randomIntegration.getConfigId())).toList()
				.get(RNG.nextInt(datumStreamCount));

		// WHEN
		BasicFilter filter = new BasicFilter();
		filter.setDatumStreamId(randomDatumStream.getConfigId());
		datumStreamDao.updateEnabledStatus(userId, filter, false);

		// THEN
		allCloudIntegrationConfigurationData(jdbcTemplate);
		allCloudDatumStreamConfigurationData(jdbcTemplate);
		final List<Map<String, Object>> taskRows = allCloudDatumStreamPollTaskEntityData(jdbcTemplate);

		// @formatter:off
		then(taskRows)
			.as("Rows for tasks found")
			.hasSize(integrationCount * datumStreamCount)
			.satisfies(rows -> {
				for (int idx = 0, len = rows.size(); idx < len; idx++ ) {
					then(rows)
						.element(idx)
						.satisfies(row -> {
							CloudDatumStreamConfiguration datumStream = datumStreams.get(new UserLongCompositePK(
									userId, (Long)row.get("ds_id")));
							CloudIntegrationConfiguration integration = integrations.get(new UserLongCompositePK(
									userId, datumStream.getIntegrationId()));
							BasicClaimableJobState expectedState = datumStream.getConfigId()
										.equals(randomDatumStream.getConfigId()) || !integration.isEnabled()
									? Completed : Queued;
							then(row)
								.as("""
									Task state only Completed for datum stream %d that was enabled under
									integration %d that was then disabled: %s
									""", datumStream.getConfigId(), datumStream.getIntegrationId(), datumStream)
								.containsEntry("status", expectedState.keyValue())
								;
						})
						;
				}
			})
			;
		// @formatter:on
	}

	@Test
	public void enableDatumStreamStartsAssociatedTask() {
		// GIVEN
		final int integrationCount = 5;
		final int datumStreamCount = 2;
		for ( int i = 0; i < integrationCount; i++ ) {
			CloudIntegrationConfiguration integration = createIntegration(userId,
					i == 0 ? true : RNG.nextBoolean());
			for ( int d = 0; d < datumStreamCount; d++ ) {
				CloudDatumStreamConfiguration datumStream = createDatumStream(userId,
						integration.getConfigId(), i == 0 && d == 0 ? false : RNG.nextBoolean());
				createDatumStreamPollTask(userId, datumStream.getConfigId(),
						integration.isEnabled() && datumStream.isEnabled() ? Queued : Completed);
			}
		}

		final List<CloudDatumStreamConfiguration> disabledDatumStreams = datumStreams.values().stream()
				.filter(e -> !e.isEnabled() && integrations
						.get(new UserLongCompositePK(userId, e.getIntegrationId())).isEnabled())
				.toList();
		final CloudDatumStreamConfiguration randomDatumStream = disabledDatumStreams
				.get(RNG.nextInt(disabledDatumStreams.size()));

		allCloudIntegrationConfigurationData(jdbcTemplate);
		allCloudDatumStreamConfigurationData(jdbcTemplate);
		allCloudDatumStreamPollTaskEntityData(jdbcTemplate);

		// WHEN
		BasicFilter filter = new BasicFilter();
		filter.setDatumStreamId(randomDatumStream.getConfigId());
		datumStreamDao.updateEnabledStatus(userId, filter, true);

		// THEN
		allCloudIntegrationConfigurationData(jdbcTemplate);
		allCloudDatumStreamConfigurationData(jdbcTemplate);
		final List<Map<String, Object>> taskRows = allCloudDatumStreamPollTaskEntityData(jdbcTemplate);

		// @formatter:off
		then(taskRows)
			.as("Rows for tasks found")
			.hasSize(integrationCount * datumStreamCount)
			.satisfies(rows -> {
				for (int idx = 0, len = rows.size(); idx < len; idx++ ) {
					then(rows)
						.element(idx)
						.satisfies(row -> {
							CloudDatumStreamConfiguration datumStream = datumStreams.get(new UserLongCompositePK(
									userId, (Long)row.get("ds_id")));
							CloudIntegrationConfiguration integration = integrations.get(new UserLongCompositePK(
									userId, datumStream.getIntegrationId()));
							BasicClaimableJobState expectedState = datumStream.getConfigId()
										.equals(randomDatumStream.getConfigId())
										|| (integration.isEnabled() && datumStream.isEnabled())
									? Queued : Completed;
							then(row)
								.as("""
									Task state only Queued for datum stream %d that was disabled under
									integration %d that was then enabled: %s
									""", datumStream.getConfigId(), datumStream.getIntegrationId(), datumStream)
								.containsEntry("status", expectedState.keyValue())
								;
						})
						;
				}
			})
			;
		// @formatter:on
	}

}
