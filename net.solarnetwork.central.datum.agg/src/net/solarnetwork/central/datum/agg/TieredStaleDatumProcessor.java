/* ==================================================================
 * TieredStaleDatumProcessor.java - 3/07/2018 7:46:22 AM
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

package net.solarnetwork.central.datum.agg;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.jdbc.core.JdbcOperations;

/**
 * Abstract job to process "stale" tiers of data.
 * 
 * <p>
 * This job executes a JDBC procedure, which is expected to accept one or two
 * arguments. The first argument is the configured {@code tierProcessType}. If
 * {@code tierProcessMax} is not {@literal null} then that will be passed as the
 * second argument. The JDBC procedure must return an {@code Integer} result
 * representing the number of rows processed by the call. If the procedure
 * returns zero, the job stops immediately.
 * </p>
 * 
 * <p>
 * If {@code taskCount} is higher than {@code 1} then {@code taskCount} tasks
 * will be submitted to the configured {@link #getExecutorService()}. Depending
 * on the configuration of that service, those tasks might execute in parallel.
 * </p>
 * 
 * <p>
 * The {@link #getMaximumRowCount()} value will limit the overall number or
 * stale tier rows processed in one invocation of {@link #handleJob(Event)}.
 * </p>
 * 
 * @author matt
 * @version 1.1
 * @since 1.6
 */
public abstract class TieredStaleDatumProcessor extends StaleDatumProcessor {

	private final String taskDescription;
	private String tierProcessType = "h";
	private Integer tierProcessMax = 1;
	private int taskCount = 1;

	/**
	 * Constructor.
	 * 
	 * @param eventAdmin
	 *        the EventAdmin
	 * @param jdbcOps
	 *        the JdbcOperations to use
	 * @param taskDescription
	 *        a description of the task to use in log statements
	 */
	public TieredStaleDatumProcessor(EventAdmin eventAdmin, JdbcOperations jdbcOps,
			String taskDescription) {
		super(eventAdmin, jdbcOps);
		this.taskDescription = taskDescription;
	}

	/**
	 * Execute the stale processing task.
	 * 
	 * @param remainingCount
	 *        the maximum remaining number of rows to process
	 * @param number
	 *        of rows processed
	 */
	protected abstract int execute(final AtomicInteger remainingCount);

	@Override
	protected boolean handleJob(Event job) throws Exception {
		final int tCount = taskCount;
		log.debug("Processing at most {} {} for tier '{}' using {} tasks with call {}",
				getMaximumRowCount(), taskDescription, tierProcessType, tCount, getJdbcCall());
		final AtomicInteger remainingCount = new AtomicInteger(getMaximumRowCount());
		boolean allDone = false;
		if ( tCount > 1 ) {
			final ExecutorService executorService = getExecutorService();
			final CountDownLatch latch = new CountDownLatch(tCount);
			for ( int i = 0; i < tCount; i++ ) {
				executorService.submit(new Runnable() {

					@Override
					public void run() {
						log.debug("Task {} processing at most {} {} for tier '{}'",
								Thread.currentThread().getName(), getMaximumRowCount(), taskDescription,
								tierProcessType);
						try {
							int processedCount = execute(remainingCount);
							log.debug("Task {} processed {} {} for tier '{}'",
									Thread.currentThread().getName(), processedCount, taskDescription,
									tierProcessType);
						} catch ( DeadlockLoserDataAccessException e ) {
							log.warn("Deadlock processing {} for tier '{}' with call {}",
									taskDescription, tierProcessType, getJdbcCall(), e);
						} catch ( Exception e ) {
							log.error("Error processing {} for tier '{}' with call {}", taskDescription,
									tierProcessType, getJdbcCall(), e);
						} finally {
							latch.countDown();
						}
					}
				});
			}
			allDone = latch.await(getMaximumWaitMs(), TimeUnit.MILLISECONDS);
			if ( !allDone ) {
				log.warn("Timeout processing {} for tier '{}'; {}/{} tasks completed", taskDescription,
						tierProcessType, (tCount - latch.getCount()), tCount);
			}
		} else {
			execute(remainingCount);
			allDone = true;
		}
		return allDone;
	}

	/**
	 * Get the tier process type.
	 * 
	 * @return the type
	 */
	public String getTierProcessType() {
		return tierProcessType;
	}

	/**
	 * Set the type of tier data to process.
	 * 
	 * <p>
	 * This is the first parameter passed to the JDBC procedure.
	 * </p>
	 * 
	 * @param tierProcessType
	 *        the type to set
	 */
	public void setTierProcessType(String tierProcessType) {
		this.tierProcessType = tierProcessType;
	}

	/**
	 * Get the maximum tier rows to process per procedure call.
	 * 
	 * @return the maximum row count, or {@literal null} for no explicit limit
	 */
	public Integer getTierProcessMax() {
		return tierProcessMax;
	}

	/**
	 * Set the maximum number of tier rows to process per procedure call.
	 * 
	 * <p>
	 * If this value is not {@literal null}, then it will be passed as the
	 * second parameter passed to the JDBC procedure. When {@literal null} then
	 * the JDBC procedure is expected to take only one argument.
	 * </p>
	 * 
	 * @param tierProcessMax
	 *        the maximum number of rows, or {@literal null} for no explicit
	 *        limit; default is {@literal 1}
	 */
	public void setTierProcessMax(Integer tierProcessMax) {
		this.tierProcessMax = tierProcessMax;
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
