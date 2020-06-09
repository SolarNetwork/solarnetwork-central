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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.RemoteServiceException;
import net.solarnetwork.central.scheduler.JobSupport;
import net.solarnetwork.central.user.biz.UserNodeEventHookService;
import net.solarnetwork.central.user.dao.UserNodeEventTaskDao;
import net.solarnetwork.central.user.domain.UserNodeEvent;
import net.solarnetwork.central.user.domain.UserNodeEventHookConfiguration;
import net.solarnetwork.central.user.domain.UserNodeEventTask;
import net.solarnetwork.util.OptionalServiceCollection;

/**
 * FIXME
 * 
 * <p>
 * TODO
 * </p>
 * 
 * @author matt
 * @version 1.0
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
	private final OptionalServiceCollection<UserNodeEventHookService> hookServices;
	private String topic = DEFAULT_TOPIC;
	private long serviceTimeout = DEFAULT_SERVICE_TIMEOUT;

	/**
	 * Constructor.
	 * 
	 * @param eventAdmin
	 *        the event admin
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
	public UserNodeEventTaskProcessorJob(EventAdmin eventAdmin, TransactionTemplate transactionTemplate,
			UserNodeEventTaskDao taskDao,
			OptionalServiceCollection<UserNodeEventHookService> hookServices) {
		super(eventAdmin);
		this.transactionTemplate = transactionTemplate;
		if ( taskDao == null ) {
			throw new IllegalArgumentException("The taskDao argument must not be null.");
		}
		this.taskDao = taskDao;
		if ( hookServices == null ) {
			throw new IllegalArgumentException("The hookServices argument must not be null.");
		}
		this.hookServices = hookServices;
		setJobGroup("UserNodeEvent");
	}

	@Override
	protected boolean handleJob(Event job) throws Exception {
		return executeParallelJob(job, "hook tasks");
	}

	@Override
	protected int executeJobTask(Event job, AtomicInteger remainingIterataions) throws Exception {
		int processedCount = 0;
		int resultCount = 0;
		do {

		} while ( resultCount > 0 && remainingIterataions.get() > 0 );
		return processedCount;
	}

	protected boolean handleJobInternal(Event job) throws Exception {
		if ( transactionTemplate != null ) {
			return transactionTemplate.execute(new TransactionCallback<Boolean>() {

				@Override
				public Boolean doInTransaction(TransactionStatus status) {
					return execute();
				}
			});
		} else {
			return execute();
		}

	}

	private boolean execute() {
		final UserNodeEvent task = taskDao.claimQueuedTask(topic);
		if ( task == null ) {
			return true;
		}
		final UserNodeEventHookConfiguration config = task.getConfig();
		if ( config == null ) {
			return true;
		}
		final String serviceId = config.getServiceIdentifier();
		if ( serviceId == null ) {
			return true;
		}
		boolean result = false;
		for ( UserNodeEventHookService service : hookServices.services() ) {
			if ( serviceId.equals(service.getId()) ) {
				final ExecutorService executor = getExecutorService();
				if ( executor != null ) {
					Future<Boolean> future = executor.submit(new Callable<Boolean>() {

						@Override
						public Boolean call() throws Exception {
							return execute(config, task.getTask(), service);
						}
					});
					try {
						result = future.get(serviceTimeout, TimeUnit.MILLISECONDS);
					} catch ( TimeoutException | InterruptedException e ) {
						log.info("Temporary exception handling user node event task {}: {}",
								task.getId(), e.toString());
					} catch ( ExecutionException e ) {
						throw new RemoteServiceException(String.format(
								"Error handling user node event task %s with service %s: %s",
								task.getId(), serviceId, e.getMessage()), e);
					}
				} else {
					result = execute(config, task.getTask(), service);
				}
			}
		}
		if ( result ) {
			taskDao.taskCompleted(task.getTask());
		}
		return result;
	}

	private boolean execute(UserNodeEventHookConfiguration config, UserNodeEventTask task,
			UserNodeEventHookService service) {
		// TODO Auto-generated method stub
		return false;
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
		if ( topic == null ) {
			throw new IllegalArgumentException("The topic argument must not be null.");
		}
		this.topic = topic;
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
