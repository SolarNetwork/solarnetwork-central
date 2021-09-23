/* ==================================================================
 * BulkLoadingDaoSupportTests.java - 12/11/2018 7:34:09 AM
 * 
 * Copyright 2018 SolarNetwork.net Dev Team
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import net.solarnetwork.central.common.dao.jdbc.BulkLoadingDaoSupport;
import net.solarnetwork.central.dao.BulkLoadingDao.LoadingContext;
import net.solarnetwork.central.dao.BulkLoadingDao.LoadingExceptionHandler;
import net.solarnetwork.central.dao.BulkLoadingDao.LoadingOptions;
import net.solarnetwork.central.dao.BulkLoadingDao.LoadingTransactionMode;
import net.solarnetwork.central.domain.BaseEntity;
import net.solarnetwork.central.support.SimpleBulkLoadingOptions;
import net.solarnetwork.central.test.AbstractJdbcDaoTestSupport;

/**
 * Test cases for the {@link BulkLoadingDaoSupport} class.
 * 
 * <p>
 * This class manually manages transactions and creates/drops a
 * {@literal solarnet.sntest_bulk_load_test} table for testing with.
 * </p>
 * 
 * @author matt
 * @version 2.0
 */
public class BulkLoadingDaoSupportTests extends AbstractJdbcDaoTestSupport {

	private JdbcTemplate jdbcTemplate;
	private PlatformTransactionManager txManager;
	private BulkLoadingDaoSupport support;

	@Override
	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Autowired
	public void setTransactionManager(PlatformTransactionManager txManager) {
		this.txManager = txManager;
	}

	@Before
	public void setup() {
		TestTransaction.end();
		jdbcTemplate.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				con.createStatement()
						.executeUpdate("drop table if exists solarnet.sntest_bulk_load_test");
				con.createStatement().executeUpdate(
						"create table solarnet.sntest_bulk_load_test (id bigint not null primary key,created timestamp,thingy character varying(64))");
				return null;
			}
		});

		support = new BulkLoadingDaoSupport(log);
		support.setDataSource(jdbcTemplate.getDataSource());
		support.setJdbcCall(
				"insert into solarnet.sntest_bulk_load_test (id,created,thingy) values (?,?,?)");
		support.setTransactionManager(this.txManager);
	}

	@After
	public void cleanup() {
		jdbcTemplate.execute(new ConnectionCallback<Void>() {

			@Override
			public Void doInConnection(Connection con) throws SQLException, DataAccessException {
				con.createStatement()
						.executeUpdate("drop table if exists solarnet.sntest_bulk_load_test");
				return null;
			}
		});
	}

	private static class BulkLoadThingy extends BaseEntity {

		private static final long serialVersionUID = 4184939958330559933L;

		private BulkLoadThingy(long id) {
			super();
			setId(id);
		}
	}

	private class TestBulkLoadingContext
			extends BulkLoadingDaoSupport.BulkLoadingContext<BulkLoadThingy, Long> {

		private final Timestamp creation = new Timestamp(System.currentTimeMillis());

		private int commitCount = 0;

		private TestBulkLoadingContext(LoadingOptions options,
				LoadingExceptionHandler<BulkLoadThingy, Long> exceptionHandler) throws SQLException {
			support.super(options, exceptionHandler);
		}

		@Override
		protected boolean doLoad(BulkLoadThingy entity, PreparedStatement stmt, long index)
				throws SQLException {
			stmt.setLong(1, index);
			stmt.setTimestamp(2, creation);
			stmt.setString(3, "Thing " + index);
			stmt.executeUpdate();
			return true;
		}

		@Override
		public void commit() {
			super.commit();
			commitCount++;
		}

	}

	private int countOfBulkLoadThingy() {
		return jdbcTemplate.queryForObject("select count(*) from solarnet.sntest_bulk_load_test",
				Integer.class);
	}

	@Test
	public void loadInSingleTransaction() throws SQLException {
		int rowCount = 50;
		LoadingOptions options = new SimpleBulkLoadingOptions("Test", null,
				LoadingTransactionMode.SingleTransaction, null);
		try (TestBulkLoadingContext ctx = new TestBulkLoadingContext(options,
				new LoadingExceptionHandler<BulkLoadingDaoSupportTests.BulkLoadThingy, Long>() {

					@Override
					public void handleLoadingException(Throwable t,
							LoadingContext<BulkLoadThingy, Long> context) {
						throw new RuntimeException(t);
					}
				})) {
			for ( long i = 0; i < rowCount; i++ ) {
				ctx.load(new BulkLoadThingy(i));
				assertThat("Running loaded count", ctx.getLoadedCount(), equalTo(i + 1));
				assertThat("Running committed count", ctx.getCommittedCount(), equalTo(0L));
			}
			ctx.commit();
			assertThat("Commit count", ctx.commitCount, equalTo(1));
			assertThat("Loaded count", ctx.getLoadedCount(), equalTo((long) rowCount));
			assertThat("Committed count", ctx.getCommittedCount(), equalTo((long) rowCount));
		}
		assertThat("Inserted row count", countOfBulkLoadThingy(), equalTo(rowCount));
	}

	@Test
	public void loadInSingleTransactionRollback() throws SQLException {
		int rowCount = 50;
		LoadingOptions options = new SimpleBulkLoadingOptions("Test", null,
				LoadingTransactionMode.SingleTransaction, null);
		try (TestBulkLoadingContext ctx = new TestBulkLoadingContext(options,
				new LoadingExceptionHandler<BulkLoadingDaoSupportTests.BulkLoadThingy, Long>() {

					@Override
					public void handleLoadingException(Throwable t,
							LoadingContext<BulkLoadThingy, Long> context) {
						throw new RuntimeException(t);
					}
				})) {
			for ( long i = 0; i < rowCount; i++ ) {
				ctx.load(new BulkLoadThingy(i));
				assertThat("Running loaded count", ctx.getLoadedCount(), equalTo(i + 1));
				assertThat("Running committed count", ctx.getCommittedCount(), equalTo(0L));
			}
			ctx.rollback();
			assertThat("Commit count", ctx.commitCount, equalTo(0));
		}
		assertThat("Inserted row count", countOfBulkLoadThingy(), equalTo(0));
	}

	@Test
	public void loadInSingleTransactionImplicitRollback() throws SQLException {
		int rowCount = 50;
		LoadingOptions options = new SimpleBulkLoadingOptions("Test", null,
				LoadingTransactionMode.SingleTransaction, null);
		try (TestBulkLoadingContext ctx = new TestBulkLoadingContext(options,
				new LoadingExceptionHandler<BulkLoadingDaoSupportTests.BulkLoadThingy, Long>() {

					@Override
					public void handleLoadingException(Throwable t,
							LoadingContext<BulkLoadThingy, Long> context) {
						throw new RuntimeException(t);
					}
				})) {
			for ( long i = 0; i < rowCount; i++ ) {
				ctx.load(new BulkLoadThingy(i));
				assertThat("Running loaded count", ctx.getLoadedCount(), equalTo(i + 1));
				assertThat("Running committed count", ctx.getCommittedCount(), equalTo(0L));
			}
			// no explicit rollback here...
			assertThat("Commit count", ctx.commitCount, equalTo(0));
			assertThat("Loaded count", ctx.getLoadedCount(), equalTo((long) rowCount));
			assertThat("Committed count", ctx.getCommittedCount(), equalTo(0L));
		}
		assertThat("Inserted row count", countOfBulkLoadThingy(), equalTo(0));
	}

	@Test
	public void loadInBatchTransactions() throws SQLException {
		int rowCount = 50;
		LoadingOptions options = new SimpleBulkLoadingOptions("Test", 10,
				LoadingTransactionMode.BatchTransactions, null);
		try (TestBulkLoadingContext ctx = new TestBulkLoadingContext(options,
				new LoadingExceptionHandler<BulkLoadingDaoSupportTests.BulkLoadThingy, Long>() {

					@Override
					public void handleLoadingException(Throwable t,
							LoadingContext<BulkLoadThingy, Long> context) {
						throw new RuntimeException(t);
					}
				})) {
			for ( long i = 0; i < rowCount; i++ ) {
				ctx.load(new BulkLoadThingy(i));
				assertThat("Running loaded count", ctx.getLoadedCount(), equalTo(i + 1));
				assertThat("Running committed count", ctx.getCommittedCount(), equalTo(i - i % 10));
			}
			ctx.commit();
			assertThat("Commit count", ctx.commitCount, equalTo(5));
			assertThat("Loaded count", ctx.getLoadedCount(), equalTo((long) rowCount));
			assertThat("Committed count", ctx.getCommittedCount(), equalTo((long) rowCount));
		}
		assertThat("Inserted row count", countOfBulkLoadThingy(), equalTo(rowCount));
	}

	@Test
	public void loadInBatchTransactionsRollbackAndStop() throws SQLException {
		int rowCount = 50;
		LoadingOptions options = new SimpleBulkLoadingOptions("Test", 10,
				LoadingTransactionMode.BatchTransactions, null);
		try (TestBulkLoadingContext ctx = new TestBulkLoadingContext(options,
				new LoadingExceptionHandler<BulkLoadingDaoSupportTests.BulkLoadThingy, Long>() {

					@Override
					public void handleLoadingException(Throwable t,
							LoadingContext<BulkLoadThingy, Long> context) {
						throw new RuntimeException(t);
					}
				})) {
			for ( long i = 0; i < rowCount; i++ ) {
				ctx.load(new BulkLoadThingy(i));
				assertThat("Running loaded count", ctx.getLoadedCount(), equalTo(i + 1));
				assertThat("Running committed count", ctx.getCommittedCount(), equalTo(i - i % 10));
			}
			ctx.rollback();
			assertThat("Commit count", ctx.commitCount, equalTo(4));
			assertThat("Loaded count", ctx.getLoadedCount(),
					equalTo((long) (rowCount - options.getBatchSize())));
			assertThat("Committed count in batch", ctx.getCommittedCount(),
					equalTo((long) (rowCount - options.getBatchSize())));
		}
		assertThat("Inserted row count", countOfBulkLoadThingy(),
				equalTo(rowCount - options.getBatchSize()));
	}
}
