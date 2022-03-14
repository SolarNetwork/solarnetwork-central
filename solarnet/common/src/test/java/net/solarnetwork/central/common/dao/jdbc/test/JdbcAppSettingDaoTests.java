/* ==================================================================
 * JdbcAppSettingDaoTests.java - 10/11/2021 1:51:18 PM
 * 
 * Copyright 2021 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.common.dao.jdbc.test;

import static net.solarnetwork.central.domain.KeyTypePK.keyType;
import static org.assertj.core.api.BDDAssertions.then;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.test.context.transaction.TestTransaction;
import net.solarnetwork.central.common.dao.jdbc.AppSettingRowMapper;
import net.solarnetwork.central.common.dao.jdbc.JdbcAppSettingDao;
import net.solarnetwork.central.common.dao.jdbc.sql.SelectAppSetting;
import net.solarnetwork.central.domain.AppSetting;
import net.solarnetwork.central.domain.KeyTypePK;
import net.solarnetwork.central.test.AbstractJUnit5JdbcDaoTestSupport;

/**
 * Test case for the {@link JdbcAppSettingDao} class.
 * 
 * @author matt
 * @version 1.0
 */
public class JdbcAppSettingDaoTests extends AbstractJUnit5JdbcDaoTestSupport {

	private JdbcAppSettingDao dao;

	@BeforeEach
	public void setup() {
		dao = new JdbcAppSettingDao(jdbcTemplate);
	}

	@Test
	public void get_noMatch() {
		// GIVEN

		// WHEN
		AppSetting result = dao.get(keyType("foo", "bar"));

		// THEN
		then(result).describedAs("Null returned when no match").isNull();
	}

	@Test
	public void get() {
		// GIVEN
		final Instant now = Instant.now();
		AppSetting setting = AppSetting.appSetting("foo", "bar", "bam");
		dao.save(setting);

		// WHEN
		AppSetting result = dao.get(keyType("foo", "bar"));

		// THEN
		then(result).describedAs("Setting found").isNotNull();
		then(result.getId()).isEqualTo(setting.getId());
		then(result.getValue()).isEqualTo(setting.getValue());
		then(result.getCreated()).describedAs("Creation date defaulted").isAfterOrEqualTo(now);
		then(result.getModified()).describedAs("Modification date defaulted").isAfterOrEqualTo(now);
	}

	@Test
	public void save_insert() {
		// GIVEN
		AppSetting setting = AppSetting.appSetting("foo", "bar", "bam");

		// WHEN
		KeyTypePK id = dao.save(setting);

		// THEN
		then(id).describedAs("Saving setting returns ID").isNotNull();
		then(id.getKey()).isEqualTo(setting.getKey());
		then(id.getType()).isEqualTo(setting.getType());
	}

	@Test
	public void save_update() {
		// GIVEN
		AppSetting setting = AppSetting.appSetting("foo", "bar", "bam");
		dao.save(setting);

		// WHEN
		AppSetting setting2 = setting.withValue("blamo");
		AppSetting result = dao.get(dao.save(setting2));

		// THEN
		then(result).describedAs("Updated setting returned").isNotNull();
		then(result.getId()).describedAs("ID value").isEqualTo(setting.getId());
		then(result.getValue()).describedAs("value").isEqualTo(setting2.getValue());
	}

	@Test
	public void delete_noMatch() {
		// GIVEN
		AppSetting setting = AppSetting.appSetting("foo", "bar", "bam");

		// WHEN
		dao.delete(setting);
	}

	@Test
	public void delete() {
		// GIVEN
		AppSetting setting = AppSetting.appSetting("foo", "bar", "bam");
		dao.save(setting);

		// WHEN
		dao.delete(setting);

		// THEN
		AppSetting result = dao.get(setting.getId());
		then(result).describedAs("Setting was deleted").isNull();
	}

	@Test
	public void delete_forKey() {
		// GIVEN
		final String[] keys = new String[] { "k1", "k2" };
		final String[] types = new String[] { "t1", "t2", "t3" };
		for ( String k : keys ) {
			for ( String t : types ) {
				dao.save(AppSetting.appSetting(k, t, t + k));
			}
		}

		// WHEN
		int count = dao.deleteAll("k2");

		// THEN
		then(count).describedAs("Deleted all rows for key").isEqualTo(3);

		Collection<AppSetting> remaining = jdbcTemplate.query(new SelectAppSetting(null, null),
				AppSettingRowMapper.INSTANCE);
		// @formatter:off
		then(remaining).describedAs("Remaining settings not deleted")
			.hasSize(3)
			.allSatisfy(s -> {
				then(s.getKey()).describedAs("key 1 not deleted").isEqualTo("k1");
			}).extracting(AppSetting::getType)
				.describedAs("all key 1 types remain")
				.containsExactly(types);
		// @formatter:on
	}

	@Test
	public void lock_noMatch() {

		// WHEN
		AppSetting locked = dao.lockForUpdate("foo", "bar");

		// THEN
		then(locked).describedAs("No locked setting when row doesn't exist").isNull();
	}

	private void deleteAll() {
		jdbcTemplate.update("delete from solarcommon.app_setting");
	}

	@Test
	public void lock_timeout() throws Exception {
		TestTransaction.end();
		try {
			// GIVEN
			// insert a row so we can lock it
			AppSetting setting = AppSetting.appSetting("foo", "bar", "bam");
			dao.save(setting);

			// use a latch for our other thread to signal this thread
			CountDownLatch latch = new CountDownLatch(1);

			// start this thread's transaction, and lock the row
			TestTransaction.start();
			AppSetting locked = dao.lockForUpdate("foo", "bar");

			// WHEN
			// kick out another thread that tries to lock the same row
			AtomicReference<AppSetting> otherSetting = new AtomicReference<>();
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						jdbcTemplate.execute(new ConnectionCallback<Void>() {

							@Override
							public Void doInConnection(Connection con)
									throws SQLException, DataAccessException {
								con.setAutoCommit(false);
								log.info("Requesting AppSetting lock for locked row...");
								SelectAppSetting select = SelectAppSetting
										.selectForKeyType(setting.getKey(), setting.getType(), true);
								try (PreparedStatement stmt = select.createPreparedStatement(con)) {
									stmt.setQueryTimeout(2);
									try (ResultSet rs = stmt.executeQuery()) {
										rs.next();
										AppSetting s = AppSettingRowMapper.INSTANCE.mapRow(rs, 0);
										otherSetting.set(s);
									} catch ( SQLException e ) {
										log.info("Got expected SQL exception {} ({}) from query timeout",
												e.getErrorCode(), e.getSQLState());
									}
								}
								con.rollback();
								return null;
							}
						});
					} finally {
						latch.countDown();
					}
				}

			}).start();

			// wait (longer than query timeout in other thread)
			latch.await(5, TimeUnit.SECONDS);

			// THEN
			then(locked).describedAs("Locked setting available").isNotNull();
			then(locked.getKey()).describedAs("row key").isEqualTo("foo");
			then(locked.getType()).describedAs("row type").isEqualTo("bar");
			then(otherSetting.get()).describedAs("Other thread did not get lock").isNull();
		} finally {
			TestTransaction.end();

			// clean up from manual insert
			deleteAll();
		}
	}

	@Test
	public void lock_succeed() throws Exception {
		TestTransaction.end();
		try {
			// GIVEN
			// insert a row so we can lock it
			AppSetting setting = AppSetting.appSetting("foo", "bar", "bam");
			dao.save(setting);

			// use a latch for our other thread to signal this thread
			CountDownLatch latch = new CountDownLatch(1);
			CountDownLatch latch2 = new CountDownLatch(1);

			// start this thread's transaction, and lock the row
			TestTransaction.start();
			AppSetting locked = dao.lockForUpdate("foo", "bar");

			// WHEN
			// kick out another thread that tries to lock the same row
			AtomicReference<AppSetting> otherSetting = new AtomicReference<>();
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						jdbcTemplate.execute(new ConnectionCallback<Void>() {

							@Override
							public Void doInConnection(Connection con)
									throws SQLException, DataAccessException {
								con.setAutoCommit(false);
								log.info("Requesting AppSetting lock for locked row...");
								SelectAppSetting select = SelectAppSetting
										.selectForKeyType(setting.getKey(), setting.getType(), true);
								try (PreparedStatement stmt = select.createPreparedStatement(con)) {
									stmt.setQueryTimeout(5);
									latch.countDown();
									try (ResultSet rs = stmt.executeQuery()) {
										rs.next();
										AppSetting s = AppSettingRowMapper.INSTANCE.mapRow(rs, 0);
										otherSetting.set(s);
										rs.deleteRow();
									}
								}
								con.commit();
								return null;
							}
						});
					} finally {
						latch2.countDown();
					}
				}

			}).start();

			// wait for other thread to just about execute lock query
			latch.await(5, TimeUnit.SECONDS);

			// wait just a little to make other thread block
			Thread.sleep(1);

			// release locked row; other thread should then get lock
			TestTransaction.end();

			boolean threadCompleted = latch2.await(5, TimeUnit.SECONDS);

			// THEN
			then(locked).describedAs("Locked setting available").isNotNull();
			then(locked.getKey()).describedAs("row key").isEqualTo("foo");
			then(locked.getType()).describedAs("row type").isEqualTo("bar");
			then(otherSetting.get()).describedAs("Other thread got lock").isNotNull();
			then(otherSetting.get().getId()).describedAs("Other thread locked same row")
					.isEqualTo(setting.getId());
			then(threadCompleted).describedAs("Other thread completed OK").isTrue();

			AppSetting deletedSetting = dao.get(setting.getId());
			then(deletedSetting).describedAs("Other thread deleted setting after acquiring lock")
					.isNull();

		} finally {
			// clean up from manual insert
			deleteAll();
		}
	}

	@Test
	public void lockAll_timeout() throws Exception {
		TestTransaction.end();
		try {
			// GIVEN
			// insert a row so we can lock it
			AppSetting setting1 = AppSetting.appSetting("foo", "bar", "bam");
			AppSetting setting2 = AppSetting.appSetting("foo", "bim", "baz");
			dao.save(setting1);
			dao.save(setting2);

			// use a latch for our other thread to signal this thread
			CountDownLatch latch = new CountDownLatch(1);

			// start this thread's transaction, and lock the row
			TestTransaction.start();
			Collection<AppSetting> locked = dao.lockForUpdate("foo");

			// WHEN
			// kick out another thread that tries to lock the same row
			List<AppSetting> otherSettings = new ArrayList<>(2);
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						jdbcTemplate.execute(new ConnectionCallback<Void>() {

							@Override
							public Void doInConnection(Connection con)
									throws SQLException, DataAccessException {
								con.setAutoCommit(false);
								log.info("Requesting AppSetting lock for locked rows...");
								SelectAppSetting select = SelectAppSetting.selectForKey("foo", true);
								try (PreparedStatement stmt = select.createPreparedStatement(con)) {
									stmt.setQueryTimeout(2);
									try (ResultSet rs = stmt.executeQuery()) {
										rs.next();
										AppSetting s = AppSettingRowMapper.INSTANCE.mapRow(rs, 0);
										otherSettings.add(s);
									} catch ( SQLException e ) {
										log.info("Got expected SQL exception {} ({}) from query timeout",
												e.getErrorCode(), e.getSQLState());
									}
								}
								con.rollback();
								return null;
							}
						});
					} finally {
						latch.countDown();
					}
				}

			}).start();

			// wait (longer than query timeout in other thread)
			latch.await(5, TimeUnit.SECONDS);

			// THEN
			then(locked).describedAs("Locked settings available").hasSize(2);
			then(locked).describedAs("row key").allSatisfy(s -> {
				then(s.getKey()).describedAs("key value").isEqualTo("foo");
			});
			then(otherSettings).describedAs("Other thread did not get locks").isEmpty();
		} finally {
			TestTransaction.end();

			// clean up from manual insert
			deleteAll();
		}
	}

}
