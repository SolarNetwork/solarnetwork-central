/* ==================================================================
 * UserNodeEventTaskCleanerJob.java - 8/06/2020 2:23:54 pm
 *
 * Copyright 2020 SolarNetwork.net Dev Team
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

package net.solarnetwork.central.user.event.dao.jobs;

import static java.lang.String.format;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.RepeatableTaskException;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.central.user.event.biz.UserNodeEventHookService;
import net.solarnetwork.central.user.event.dao.UserNodeEventTaskDao;
import net.solarnetwork.central.user.event.domain.UserNodeEvent;
import net.solarnetwork.central.user.event.domain.UserNodeEventHookConfiguration;
import net.solarnetwork.central.user.event.domain.UserNodeEventTask;
import net.solarnetwork.central.user.event.domain.UserNodeEventTaskState;

/**
 * Job to process queued {@link UserNodeEventTask} entities by passing them to
 * the {@link UserNodeEventHookService} instance they are configured for.
 *
 * @author matt
 * @version 2.0
 */
public class UserNodeEventTaskProcessorJob extends JobSupport {

	/** The default value for the {@code minimumAgeMinutes} property. */
	public static final int DEFAULT_MINIMUM_AGE_MINUTES = 720;

	public static final String DEFAULT_TOPIC = "datum/agg/update";

	/** The default {@code serviceTimeout} property value. */
	public static final long DEFAULT_SERVICE_TIMEOUT = 10000;

	/** The default {@code maximumClaimCount} property value. */
	public static final int DEFAULT_MAX_CLAIM_COUNT = 1000;

	private final TransactionTemplate transactionTemplate;
	private final UserNodeEventTaskDao taskDao;
	private final List<UserNodeEventHookService> hookServices;
	private String topic = DEFAULT_TOPIC;
	private long serviceTimeout = DEFAULT_SERVICE_TIMEOUT;

	/**
	 * Constructor.
	 *
	 * @param transactionTemplate
	 *        the transaction template to use, or {@literal null}
	 * @param taskDao
	 *        the task DAO to use
	 * @param hookServices
	 *        the hook services
	 * @throws IllegalArgumentException
	 *         if {@code eventAdmin} or {@code taskDao} or {@code hookServices}
	 *         are {@literal null}
	 */
	public UserNodeEventTaskProcessorJob(TransactionTemplate transactionTemplate,
			UserNodeEventTaskDao taskDao, List<UserNodeEventHookService> hookServices) {
		super();
		this.transactionTemplate = transactionTemplate;
		this.taskDao = requireNonNullArgument(taskDao, "taskDao");
		this.hookServices = requireNonNullArgument(hookServices, "hookServices");
		setGroupId("UserNodeEvent");
	}

	private UserNodeEventHookService resolveService(String serviceId) {
		UserNodeEventHookService result = null;
		if ( serviceId != null ) {
			for ( UserNodeEventHookService service : hookServices ) {
				if ( serviceId.equals(service.getId()) ) {
					result = service;
					break;
				}
			}
		}
		return result;
	}

	@Override
	public void run() {
		executeParallelJob("hook task");
	}

	@Override
	protected int executeJobTask(AtomicInteger remainingIterations) throws Exception {
		int processedCount = 0;
		boolean processed = false;
		do {
			try {
				if ( transactionTemplate != null ) {
					processed = transactionTemplate.execute(new TransactionCallback<Boolean>() {

						@Override
						public Boolean doInTransaction(TransactionStatus status) {
							return execute();
						}
					});
				} else {
					processed = execute();
				}
			} catch ( RepeatableTaskException e ) {
				log.debug(e.getMessage(), e);
				log.info("Error processing user node event task; will re-try later: {}", e.getMessage());
				processed = true;
			}
			if ( processed ) {
				remainingIterations.decrementAndGet();
				processedCount++;
			}
		} while ( processed && remainingIterations.get() > 0 );
		return processedCount;
	}

	/**
	 * Execute the hook task as a single transaction.
	 *
	 * <p>
	 * This method is designed to operate within a single transaction, as per
	 * the contract outlined in
	 * {@link UserNodeEventTaskDao#claimQueuedTask(String)}.
	 * </p>
	 *
	 * @return {@literal true} if the task has been processed
	 * @throws RepeatableTaskException
	 *         if the task should be re-tried in the future
	 */
	private boolean execute() {
		final UserNodeEvent event = taskDao.claimQueuedTask(topic);
		if ( event == null ) {
			return false;
		}
		final UserNodeEventTask task = event.getTask();
		if ( task == null ) {
			throw new RuntimeException("User node event has no task: " + event);
		}
		boolean success = false;
		boolean retry = false;
		try {
			final UserNodeEventHookConfiguration config = event.getConfig();
			final String serviceId = (config != null ? config.getServiceIdentifier() : null);
			final UserNodeEventHookService service = resolveService(serviceId);
			if ( service == null ) {
				throw new UnsupportedOperationException(
						format("Service %s is not available", serviceId));
			}
			if ( service != null ) {
				final AsyncTaskExecutor executor = getParallelTaskExecutor();
				if ( executor != null ) {
					Future<Boolean> future = executor.submit(new Callable<Boolean>() {

						@Override
						public Boolean call() throws Exception {
							return execute(config, task, service);
						}
					});
					try {
						success = future.get(serviceTimeout, TimeUnit.MILLISECONDS);
					} catch ( ExecutionException e ) {
						throw e.getCause();
					}
				} else {
					success = execute(config, task, service);
				}
			}
		} catch ( RepeatableTaskException e ) {
			retry = true;
			throw e;
		} catch ( TimeoutException | InterruptedException | IOException e ) {
			retry = true;
			throw new RepeatableTaskException(
					format("Transient exception handling user node event task %s: %s", event.getId(),
							e.toString()),
					e);
		} catch ( Throwable t ) {
			Throwable root = t;
			while ( root.getCause() != null ) {
				root = root.getCause();
			}
			task.setMessage(root.getMessage());
		} finally {
			if ( !retry ) {
				task.setStatus(UserNodeEventTaskState.Completed);
				task.setSuccess(success);
				task.setCompleted(Instant.now());
				taskDao.taskCompleted(task);
			}
		}
		return true;
	}

	private boolean execute(UserNodeEventHookConfiguration config, UserNodeEventTask task,
			UserNodeEventHookService service) {
		log.debug("Invoking user node event task {} for hook {} with service {}", task.getId(),
				task.getHookId(), service.getId());
		return service.processUserNodeEventHook(config, task);
	}

	/**
	 * Set the event topic to process.
	 *
	 * @param topic
	 *        the topic to set
	 * @throws IllegalArgumentException
	 *         if {@code topic} is {@literal null}
	 */
	public void setTopic(String topic) {
		this.topic = requireNonNullArgument(topic, "topic");
	}

	/**
	 * Set the maximum amount of time to wait for a hook service to execute.
	 *
	 * @param serviceTimeout
	 *        the timeout to set, in milliseconds
	 */
	public void setServiceTimeout(long serviceTimeout) {
		if ( serviceTimeout < 0 ) {
			serviceTimeout = 0;
		}
		this.serviceTimeout = serviceTimeout;
	}

}
