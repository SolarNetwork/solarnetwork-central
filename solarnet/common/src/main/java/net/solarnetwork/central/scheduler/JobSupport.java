/* ==================================================================
 * JobSupport.java - Jun 30, 2011 5:09:59 PM
 *
 * Copyright 2007-2011 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;

/**
 * Base helper class for a scheduled job.
 *
 * <p>
 * This job supports running a set of tasks in parallel by configuring the
 * {@link #setParallelism(int)} property to something greater than {@literal 1}.
 * The parallel tasks can compete for a pool of <i>task iterations</i> so that
 * each task competes for a bounded amount of work. This is useful for scenarios
 * like batch processing a queue, where the job is designed to process up to a
 * maximum number of queued items each time it runs using parallel workers.
 * </p>
 *
 * @author matt
 * @version 2.0
 */
public abstract class JobSupport implements ManagedJob {

	/** The {@code maximumWaitMs} property default value. */
	public static final long DEFAULT_MAX_WAIT = 15L * 60L * 1000L;

	/** The {@code schedule} property default value. */
	public static final String DEFAULT_CRON = "0 0/1 * * * ?";

	/** The {@code maximumIterations} property default value. */
	public static final int DEFAULT_MAX_ITERATIONS = 1000;

	/** The {@code parallelism} property default value. */
	public static final int DEFAULT_PARALLELISM = 1;

	/** The {@code jitter} property default value. */
	public static final long DEFAULT_JITTER = 500L;

	/** A class-level logger. */
	protected final Logger log = LoggerFactory.getLogger(getClass());

	private long maximumWaitMs = DEFAULT_MAX_WAIT;
	private String id;
	private String groupId;
	private String schedule = DEFAULT_CRON;
	private AsyncTaskExecutor parallelTaskExecutor = null;
	private int maximumIterations = DEFAULT_MAX_ITERATIONS;
	private int parallelism = 1;
	private long jitter = DEFAULT_JITTER;

	private AsyncTaskExecutor taskExecutorForParallelTasks() {
		AsyncTaskExecutor s = getParallelTaskExecutor();
		if ( s == null ) {
			throw new RuntimeException("No AsyncTaskExecutor is configured for parallel tasks.");
		}
		return s;
	}

	/**
	 * Execute the job in parallel via multiple threads.
	 *
	 * <p>
	 * This method can be invoked by extending classes from their {@link #run()}
	 * method. When invoked, this method will create {@code parallelism} tasks
	 * and submit them to the configured {@code parallelTaskExecutor}. Each task
	 * will call the {@link #executeJobTask(AtomicInteger)} method, passing a
	 * shared {@link AtomicInteger} initially set to {@code maximumIterations},
	 * which serves as a hint as to the number of <b>overall</b> iterations
	 * <b>all</b> tasks are trying to perform. The tasks thus compete for
	 * iterations and should decrement the {@code AtomicInteger} by the number
	 * of iterations they process, and stop processing iterations when the count
	 * reaches {@literal 0} or less.
	 * </p>
	 *
	 * <p>
	 * Note that this method does not track the iteration count. It merely waits
	 * for each task to complete, by returning or throwing an exception, for up
	 * to {@code maximumWaitMs} milliseconds.
	 * </p>
	 *
	 * <p>
	 * If {@code parallelism} is {@literal 1} then the
	 * {@link #executeJobTask(AtomicInteger)} method is called directly from
	 * this method, without submitting the task to the configured
	 * {@code executorService}.
	 * </p>
	 *
	 * @param taskName
	 *        a descriptive name for the job task, to use in logging
	 * @return the {@literal true} if all tasks were completed
	 * @since 1.7
	 */
	protected final boolean executeParallelJob(final String taskName) {
		final int tCount = getParallelism();
		final int tIterations = getMaximumIterations();
		log.debug("Processing at most {} {} iterations using {} threads", tIterations, taskName, tCount);
		final AtomicInteger remainingCount = new AtomicInteger(tIterations);
		boolean allDone = false;
		if ( tCount > 1 ) {
			final AsyncTaskExecutor executorService = taskExecutorForParallelTasks();
			final CountDownLatch latch = new CountDownLatch(tCount);
			final long tJitter = getJitter();
			final List<Future<?>> futures = new ArrayList<>();
			for ( int i = 0; i < tCount; i++ ) {
				try {
					futures.add(executorService.submit(new Runnable() {

						@Override
						public void run() {
							if ( tJitter > 0 ) {
								long delay = (long) Math.ceil(Math.random() * tJitter);
								if ( delay > 0 ) {
									log.debug(
											"Delaying thread {} start of processing {} by jitter of {}ms",
											Thread.currentThread().getName(), taskName, delay);
									try {
										Thread.sleep(delay);
									} catch ( InterruptedException e ) {
										// ignore
									}
								}
							}
							log.debug("Thread {} processing at most {} {} iterations",
									Thread.currentThread().getName(), tIterations, taskName);
							try {
								int processedCount = executeJobTask(remainingCount);
								log.debug("Thread {} processed {} {} iterations",
										Thread.currentThread().getName(), processedCount, taskName);
							} catch ( Exception e ) {
								Throwable root = e;
								while ( root.getCause() != null ) {
									root = root.getCause();
								}
								log.error("Error processing {} iteration: {}", taskName, e.toString(),
										root);
							} finally {
								latch.countDown();
							}
						}
					}));
				} catch ( RejectedExecutionException e ) {
					latch.countDown();
					log.warn("Unable to process {}: queue full", taskName);
				}
			}
			try {
				allDone = latch.await(getMaximumWaitMs(), TimeUnit.MILLISECONDS);
			} catch ( InterruptedException e1 ) {
				// ignore this one
			}
			if ( !allDone ) {
				log.warn("Timeout processing {} iterations; {}/{} tasks completed", taskName,
						(tCount - latch.getCount()), tCount);
				for ( Future<?> f : futures ) {
					try {
						if ( f.cancel(false) ) {
							log.info("Cancelled task {}", taskName);
						}
					} catch ( Exception e ) {
						log.warn("Error cancelling task {}: {}", taskName, e.toString());
					}
				}
			}
		} else {
			try {
				executeJobTask(remainingCount);
			} catch ( Exception e ) {
				Throwable root = e;
				while ( root.getCause() != null ) {
					root = root.getCause();
				}
				log.error("Error processing {} iteration: {}", taskName, e.toString(), root);
			}
			allDone = true;
		}
		return allDone;
	}

	/**
	 * Execute a parallel job task.
	 *
	 * <p>
	 * This method is called from the {@link #executeParallelJob(String)} method
	 * by each thread. This method is supposed to execute up to
	 * {@code remainingIterations} of the job's task, updating
	 * {@code remainingIterations} as each iteration is processed. Keep in mind
	 * that each job task thread will be mutating (competing for)
	 * {@code remainingIterations}.
	 * </p>
	 *
	 * <p>
	 * This method throws a {@link UnsupportedOperationException} and must be
	 * overridden by extending classes.
	 * </p>
	 *
	 * @param remainingIterations
	 *        the number of iterations left to perform
	 * @return the number of iterations performed
	 * @throws Exception
	 *         if any error occurs
	 * @since 1.7
	 */
	protected int executeJobTask(AtomicInteger remainingIterations) throws Exception {
		throw new UnsupportedOperationException("Extending class must implement.");
	}

	/**
	 * Get the unique ID of the job to schedule.
	 *
	 * @return the job ID
	 */
	@Override
	public String getId() {
		return id;
	}

	/**
	 * Set the unique ID of the job to schedule.
	 *
	 * @param jobId
	 *        the job ID
	 */
	public void setId(String jobId) {
		this.id = jobId;
	}

	/**
	 * Get the maximum time, in milliseconds, to allow for the job to execute
	 * before it is considered a failed job.
	 *
	 * @return the maximum wait, in milliseconds; defaults to <b>15 minutes</b>
	 */
	public long getMaximumWaitMs() {
		return maximumWaitMs;
	}

	/**
	 * Set the maximum time, in milliseconds, to allow for the job to execute
	 * before it is considered a failed job.
	 *
	 * @param maximumWaitMs
	 *        the maximum wait
	 */
	public void setMaximumWaitMs(long maximumWaitMs) {
		this.maximumWaitMs = maximumWaitMs;
	}

	/**
	 * Get the job cron expression to use for scheduling this job.
	 *
	 * @return the cron expression; defaults to {@literal 0 0/1 * * * ?} (once
	 *         per minute)
	 */
	@Override
	public String getSchedule() {
		return schedule;
	}

	/**
	 * Set the job cron expression to use for scheduling this job.
	 *
	 * @param schedule
	 *        the cron expression
	 */
	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}

	/**
	 * Get the job group ID to use.
	 *
	 * @return the job group
	 */
	@Override
	public String getGroupId() {
		return groupId;
	}

	/**
	 * Set the job group ID to use.
	 *
	 * @param groupId
	 *        the job group
	 */
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	/**
	 * Get the executor to handle parallel job tasks with.
	 *
	 * @return the service
	 */
	public AsyncTaskExecutor getParallelTaskExecutor() {
		return parallelTaskExecutor;
	}

	/**
	 * Set the executor to handle parallel job tasks with.
	 *
	 * @param parallelTaskExecutorService
	 *        the service to set
	 */
	public void setParallelTaskExecutor(AsyncTaskExecutor parallelTaskExecutorService) {
		this.parallelTaskExecutor = parallelTaskExecutorService;
	}

	/**
	 * Get the maximum number of iterations of the job task to run.
	 *
	 * @return the maximum iterations; defaults to {@literal 1}
	 * @since 1.7
	 */
	public int getMaximumIterations() {
		return maximumIterations;
	}

	/**
	 * Set the maximum number of claims to acquire per execution of this job.
	 *
	 * @param maximumIterations
	 *        the maximum iterations
	 * @since 1.7
	 */
	public void setMaximumIterations(int maximumIterations) {
		this.maximumIterations = maximumIterations;
	}

	/**
	 * Get the number of parallel threads to use while processing task
	 * iterations.
	 *
	 * @return the parallelism; defaults to {@literal 1}
	 * @since 1.7
	 */
	public int getParallelism() {
		return parallelism;
	}

	/**
	 * Set the number of parallel threads to use while processing task
	 * iterations.
	 *
	 * @param parallelism
	 *        the parallelism to set; will be forced to {@literal 1} if &lt; 1
	 * @since 1.7
	 */
	public void setParallelism(int parallelism) {
		if ( parallelism < 1 ) {
			parallelism = 1;
		}
		this.parallelism = parallelism;
	}

	/**
	 * Get a maximum amount of time, in milliseconds, to randomly add to the
	 * start of parallel tasks so they don't all try to start so closely
	 * together.
	 *
	 * @return the jitter, in milliseconds; defaults to {@link #DEFAULT_JITTER}
	 */
	public long getJitter() {
		return jitter;
	}

	/**
	 * Set et a maximum amount of time, in milliseconds, to randomly add to the
	 * start of parallel tasks so they don't all try to start so closely
	 * together.
	 *
	 * <p>
	 * This time is added to tasks started by the
	 * {@link #executeParallelJob(String)} method, and only when
	 * {@link #getParallelism()} is greater than {@literal 1}. Set to
	 * {@literal 0} to disable adding any random jitter to the start of tasks.
	 * </p>
	 *
	 * @param jitter
	 *        the jitter to set, in milliseconds
	 */
	public void setJitter(long jitter) {
		if ( jitter < 0 ) {
			jitter = 0;
		}
		this.jitter = jitter;
	}

}
