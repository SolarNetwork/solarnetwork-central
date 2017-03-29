/* ==================================================================
 * StaleGeneralNodeDatumProcessor.java - Aug 27, 2014 6:18:01 AM
 * 
 * Copyright 2007-2014 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.datum.agg;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * Job to process "stale" general node datum reporting aggregate data.
 * 
 * This job executes a JDBC procedure, which is expected to return an Integer
 * result representing the number of rows processed by the call. If the
 * procedure returns zero, the job stops immediately.
 * 
 * If {@code taskCount} is higher than {@code 1} then {@code taskCount} threads
 * will be spawned and each process {@code maximumRowCount / taskCount} rows.
 * 
 * @author matt
 * @version 1.1
 */
public class StaleGeneralNodeDatumProcessor extends StaleDatumProcessor {

	private String aggregateProcessType = "h";
	private int aggregateProcessMax = 1;
	private int taskCount = 1;

	/**
	 * Construct with properties.
	 * 
	 * @param eventAdmin
	 *        the EventAdmin
	 * @param jdbcOps
	 *        the JdbcOperations to use
	 */
	public StaleGeneralNodeDatumProcessor(EventAdmin eventAdmin, JdbcOperations jdbcOps) {
		super(eventAdmin, jdbcOps);
		setJdbcCall("{? = call solaragg.process_agg_stale_datum(?, ?)}");
	}

	private int execute(final AtomicInteger remainingCount) {
		return getJdbcOps().execute(new ConnectionCallback<Integer>() {

			@Override
			public Integer doInConnection(Connection con) throws SQLException, DataAccessException {
				CallableStatement call = con.prepareCall(getJdbcCall());
				call.registerOutParameter(1, Types.INTEGER);
				call.setString(2, aggregateProcessType);
				call.setInt(3, aggregateProcessMax);
				con.setAutoCommit(true); // we want every execution of our loop to commit immediately
				int resultCount = 0;
				int processedCount = 0;
				do {
					call.execute();
					resultCount = call.getInt(1);
					processedCount += resultCount;
					remainingCount.addAndGet(-resultCount);
				} while ( resultCount > 0 && remainingCount.get() > 0 );
				return processedCount;
			}
		});
	}

	@Override
	protected boolean handleJob(Event job) throws Exception {
		final int tCount = taskCount;
		log.debug(
				"Processing at most {} stale general data for aggregate '{}' using {} tasks with call {}",
				aggregateProcessMax * getMaximumRowCount(), aggregateProcessType, tCount, getJdbcCall());
		final AtomicInteger remainingCount = new AtomicInteger(getMaximumRowCount());
		boolean allDone = false;
		if ( tCount > 1 ) {
			final ExecutorService executorService = getExecutorService();
			final CountDownLatch latch = new CountDownLatch(tCount);
			for ( int i = 0; i < tCount; i++ ) {
				executorService.submit(new Runnable() {

					@Override
					public void run() {
						log.debug("Task {} processing at most {} stale general data for aggregate '{}'",
								Thread.currentThread().getName(),
								aggregateProcessMax * getMaximumRowCount(), aggregateProcessType);
						try {
							int processedCount = execute(remainingCount);
							log.debug("Task {} processed {} stale general data for aggregate '{}'",
									Thread.currentThread().getName(), processedCount,
									aggregateProcessType);
						} catch ( Exception e ) {
							log.error(
									"Error processing stale general data for aggregate '{}' with call {}",
									aggregateProcessType, getJdbcCall(), e);
						} finally {
							latch.countDown();
						}
					}
				});
			}
			allDone = latch.await(getMaximumWaitMs(), TimeUnit.MILLISECONDS);
			if ( !allDone ) {
				log.warn(
						"Timeout processing stale general data for aggregate '{}'; {}/{} tasks completed",
						aggregateProcessType, (tCount - latch.getCount()), tCount);
			}
		} else {
			execute(remainingCount);
			allDone = true;
		}
		return allDone;
	}

	/**
	 * Get the aggregate process type.
	 * 
	 * @return the type
	 */
	public String getAggregateProcessType() {
		return aggregateProcessType;
	}

	/**
	 * Set the type of aggregate data to process. This is the first parameter
	 * passed to the JDBC procedure.
	 * 
	 * @param aggregateProcessType
	 *        the type to set
	 */
	public void setAggregateProcessType(String aggregateProcessType) {
		this.aggregateProcessType = aggregateProcessType;
	}

	/**
	 * Get the maximum aggregate rows to process per procedure call.
	 * 
	 * @return the maximum row count
	 */
	public int getAggregateProcessMax() {
		return aggregateProcessMax;
	}

	/**
	 * Set the maximum number of aggregate rows to process per procedure call.
	 * This is the second parameter passed to the JDBC procedure. Default is
	 * {@code 1}.
	 * 
	 * @param aggregateProcessMax
	 *        the maximum number of rows
	 */
	public void setAggregateProcessMax(int aggregateProcessMax) {
		this.aggregateProcessMax = aggregateProcessMax;
	}

	/**
	 * Get the maximum number of parallel tasks to allow.
	 * 
	 * @return the numJobs the maximum task count
	 */
	public int getTaskCount() {
		return taskCount;
	}

	/**
	 * Set the maximum number of parallel tasks to allow.
	 * 
	 * Any value less than 1 will be treated as 1.
	 * 
	 * @param taskCount
	 *        the maximum number of tasks to allow
	 */
	public void setTaskCount(int taskCount) {
		if ( taskCount < 1 ) {
			taskCount = 1;
		}
		this.taskCount = taskCount;
	}

}
