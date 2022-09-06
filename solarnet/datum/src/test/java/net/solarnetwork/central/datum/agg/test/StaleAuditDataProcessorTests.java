/* ==================================================================
 * StaleAuditDataProcessorTests.java - 3/07/2018 11:13:16 AM
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

package net.solarnetwork.central.datum.agg.test;

import static net.solarnetwork.test.EasyMockUtils.assertWith;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import net.solarnetwork.central.datum.agg.StaleAuditDataProcessor;
import net.solarnetwork.domain.datum.Aggregation;
import net.solarnetwork.test.Assertion;

/**
 * Test cases for the {@link StaleAuditDataProcessor} class.
 * 
 * @author matt
 * @version 1.2
 */
public class StaleAuditDataProcessorTests {

	private static final String TEST_JOB_ID = "Test Stale Audit Datum Processor";

	private static final class TestProcessor extends StaleAuditDataProcessor {

		private final AtomicInteger taskThreadCount = new AtomicInteger(0);

		private TestProcessor(JdbcOperations jdbcOps) {
			super(jdbcOps);
			ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
			executor.setCorePoolSize(10);
			executor.setAllowCoreThreadTimeOut(true);
			executor.setThreadFactory(new ThreadFactory() {

				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r,
							"StaleAuditDataProcessorTask-" + taskThreadCount.incrementAndGet());
				}
			});
			executor.initialize();
			setParallelTaskExecutor(executor);
		}

	}

	private JdbcOperations jdbcTemplate;
	private TestProcessor job;
	private List<Object> otherMocks;

	@Before
	public void setup() {
		jdbcTemplate = EasyMock.createMock(JdbcOperations.class);

		job = new TestProcessor(jdbcTemplate);
		job.setGroupId("Test");
		job.setId(TEST_JOB_ID);
		job.setMaximumIterations(10);
		job.setTierProcessType(Aggregation.None.getKey());

		otherMocks = new ArrayList<>();
	}

	private void replayAll(Object... mocks) {
		EasyMock.replay(jdbcTemplate);
		if ( mocks != null ) {
			EasyMock.replay(mocks);
			for ( Object m : mocks ) {
				otherMocks.add(m);
			}
		}
	}

	@After
	public void teardown() {
		EasyMock.verify(jdbcTemplate);
		if ( !otherMocks.isEmpty() ) {
			EasyMock.verify(otherMocks.toArray(new Object[otherMocks.size()]));
		}
	}

	@Test
	public void runSingleTask() throws Exception {
		// GIVEN
		Connection con = EasyMock.createMock(Connection.class);
		CallableStatement stmt = EasyMock.createMock(CallableStatement.class);

		int[] cbResult = new int[] { -1 };
		expect(jdbcTemplate.execute(assertWith(new Assertion<ConnectionCallback<Integer>>() {

			@Override
			public void check(ConnectionCallback<Integer> cb) throws Throwable {
				Integer res = cb.doInConnection(con);
				if ( res != null ) {
					cbResult[0] = res.intValue();
				}
			}

		}))).andAnswer(new IAnswer<Integer>() {

			@Override
			public Integer answer() throws Throwable {
				return cbResult[0];
			}
		});

		con.setAutoCommit(true);
		expectLastCall().anyTimes();

		// execute call & indicate a ResultSet is available
		expect(con.prepareCall(StaleAuditDataProcessor.DEFAULT_SQL)).andReturn(stmt);
		stmt.registerOutParameter(1, Types.INTEGER);
		stmt.setString(2, Aggregation.None.getKey());
		expect(stmt.execute()).andReturn(false).times(2);
		expect(stmt.getInt(1)).andReturn(1).andReturn(0);

		stmt.close();

		// WHEN
		replayAll(con, stmt);
		job.run();

		// THEN
		assertThat("Thread count", job.taskThreadCount.get(), equalTo(0));
	}

	@Test
	public void runParallelTasks() throws Exception {
		// GIVEN
		final int parallelism = 3;
		List<Object> mocks = new ArrayList<>();
		job.setParallelism(parallelism);

		for ( int i = 0; i < parallelism; i++ ) {
			Connection con = EasyMock.createMock(Connection.class);
			CallableStatement stmt = EasyMock.createMock(CallableStatement.class);
			mocks.add(con);
			mocks.add(stmt);

			int[] cbResult = new int[] { -1 };
			expect(jdbcTemplate.execute(assertWith(new Assertion<ConnectionCallback<Integer>>() {

				@Override
				public void check(ConnectionCallback<Integer> cb) throws Throwable {
					Integer res = cb.doInConnection(con);
					if ( res != null ) {
						cbResult[0] = res.intValue();
					}
				}

			}))).andAnswer(new IAnswer<Integer>() {

				@Override
				public Integer answer() throws Throwable {
					return cbResult[0];
				}
			});

			con.setAutoCommit(true);
			expectLastCall().anyTimes();

			// execute call & indicate a ResultSet is available
			expect(con.prepareCall(StaleAuditDataProcessor.DEFAULT_SQL)).andReturn(stmt);
			stmt.registerOutParameter(1, Types.INTEGER);
			stmt.setString(2, Aggregation.None.getKey());
			expect(stmt.execute()).andReturn(false).times(2);
			expect(stmt.getInt(1)).andReturn(1).andReturn(0);

			stmt.close();
		}

		// WHEN
		replayAll(mocks.toArray(new Object[mocks.size()]));
		job.run();

		// THEN
		assertThat("Thread count", job.taskThreadCount.get(), equalTo(parallelism));
	}

}
