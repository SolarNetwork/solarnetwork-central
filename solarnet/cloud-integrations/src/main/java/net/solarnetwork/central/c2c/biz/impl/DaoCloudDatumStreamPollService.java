/* ==================================================================
 * DaoCloudDatumStreamPollService.java - 10/10/2024 4:27:57 pm
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

package net.solarnetwork.central.c2c.biz.impl;

import static net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents.eventForConfiguration;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Completed;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Executing;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Queued;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamPollService;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPollTaskDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamSettingsEntityDao;
import net.solarnetwork.central.c2c.domain.BasicCloudDatumStreamSettings;
import net.solarnetwork.central.c2c.domain.BasicQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPollTaskEntity;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamSettings;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.domain.GeneralObjectDatum;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumWriteOnlyDao;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.scheduler.SchedulerUtils;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.service.ServiceLifecycleObserver;

/**
 * DAO based implementation of {@link CloudDatumStreamPollService}.
 *
 * @author matt
 * @version 1.3
 */
public class DaoCloudDatumStreamPollService
		implements CloudDatumStreamPollService, ServiceLifecycleObserver, CloudIntegrationsUserEvents {

	/** The {@code shutdownMaxWait} property default value: 1 minute. */
	public static final Duration DEFAULT_SHUTDOWN_MAX_WAIT = Duration.ofMinutes(1);

	/** The {@code defaultDatumStreamSettings} default value. */
	public static final CloudDatumStreamSettings DEFAULT_DATUM_STREAM_SETTINGS = new BasicCloudDatumStreamSettings(
			true, false);

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Clock clock;
	private final UserEventAppenderBiz userEventAppenderBiz;
	private final SolarNodeOwnershipDao nodeOwnershipDao;
	private final CloudDatumStreamPollTaskDao taskDao;
	private final CloudDatumStreamConfigurationDao datumStreamDao;
	private final CloudDatumStreamSettingsEntityDao datumStreamSettingsDao;
	private final DatumWriteOnlyDao datumDao;
	private final ExecutorService executorService;
	private final Function<String, CloudDatumStreamService> datumStreamServiceProvider;
	private Duration shutdownMaxWait = DEFAULT_SHUTDOWN_MAX_WAIT;
	private CloudDatumStreamSettings defaultDatumStreamSettings = DEFAULT_DATUM_STREAM_SETTINGS;

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock to use
	 * @param userEventAppenderBiz
	 *        the user event appender service
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 * @param taskDao
	 *        the task DAO
	 * @param datumStreamDao
	 *        the datum stream DAO
	 * @param datumStreamSettingsDao
	 *        the datum stream settings DAO
	 * @param datumDao
	 *        the datum DAO
	 * @param executor
	 *        the executor; this must be exclusive to this service, as it will
	 *        be shut down when this service is shut down
	 * @param datumStreamServiceProvider
	 *        function that provides a {@link CloudDatumStreamService} for a
	 *        given service identifier
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoCloudDatumStreamPollService(Clock clock, UserEventAppenderBiz userEventAppenderBiz,
			SolarNodeOwnershipDao nodeOwnershipDao, CloudDatumStreamPollTaskDao taskDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamSettingsEntityDao datumStreamSettingsDao, DatumWriteOnlyDao datumDao,
			ExecutorService executor,
			Function<String, CloudDatumStreamService> datumStreamServiceProvider) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		this.userEventAppenderBiz = requireNonNullArgument(userEventAppenderBiz, "userEventAppenderBiz");
		this.nodeOwnershipDao = requireNonNullArgument(nodeOwnershipDao, "nodeOwnershipDao");
		this.taskDao = requireNonNullArgument(taskDao, "taskDao");
		this.datumStreamDao = requireNonNullArgument(datumStreamDao, "datumStreamDao");
		this.datumStreamSettingsDao = requireNonNullArgument(datumStreamSettingsDao,
				"datumStreamSettingsDao");
		this.datumDao = requireNonNullArgument(datumDao, "datumDao");
		this.executorService = requireNonNullArgument(executor, "executor");
		this.datumStreamServiceProvider = requireNonNullArgument(datumStreamServiceProvider,
				"datumStreamServiceProvider");
	}

	@Override
	public void serviceDidStartup() {
		// nothing
	}

	@Override
	public void serviceDidShutdown() {
		try {
			executorService.shutdown();
			if ( shutdownMaxWait.isPositive() ) {
				log.info("Waiting at most {}s for datum stream poll tasks to complete...",
						shutdownMaxWait.getSeconds());
				boolean success = executorService.awaitTermination(shutdownMaxWait.getSeconds(),
						TimeUnit.SECONDS);
				if ( success ) {
					log.info("All datum stream poll tasks finished.");
				} else {
					log.warn("Timeout waiting {}s for datum stream poll tasks to complete.",
							shutdownMaxWait.getSeconds());
				}
			}
		} catch ( Exception e ) {
			log.warn("Error shutting down datum stream poll task service: {}", e.getMessage(), e);
		}
	}

	@Override
	public CloudDatumStreamPollTaskEntity claimQueuedTask() {
		if ( executorService.isShutdown() ) {
			return null;
		}
		return taskDao.claimQueuedTask();
	}

	@Override
	public Future<CloudDatumStreamPollTaskEntity> executeTask(CloudDatumStreamPollTaskEntity task) {
		try {
			return executorService.submit(new CloudDatumStreamPollTask(task));
		} catch ( RejectedExecutionException e ) {
			log.warn("Datum stream poll task execution rejected, resetting state to Queued: {}",
					e.getMessage());
			// go back to queued
			if ( !taskDao.updateTaskState(task.getId(), Queued, task.getState()) ) {
				log.warn("Failed to update rejected datum stream poll task {} state from {} to Queued",
						task.getId().ident(), task.getState());
			}
			return CompletableFuture.failedFuture(e);
		}
	}

	private final class CloudDatumStreamPollTask implements Callable<CloudDatumStreamPollTaskEntity> {

		private final CloudDatumStreamPollTaskEntity taskInfo;
		private final BasicClaimableJobState startState;

		private CloudDatumStreamPollTask(CloudDatumStreamPollTaskEntity taskInfo) {
			super();
			this.taskInfo = requireNonNullArgument(taskInfo, "taskInfo").clone();
			this.startState = requireNonNullArgument(taskInfo.getState(), "taskInfo.state");
		}

		@Override
		public CloudDatumStreamPollTaskEntity call() throws Exception {
			try {
				return executeTask();
			} catch ( Exception e ) {
				Throwable t = e;
				while ( t.getCause() != null ) {
					t = t.getCause();
				}
				try {
					log.warn("Error executing datum stream {} poll task", taskInfo.getId().ident(), e);
					var errMsg = "Error executing poll task.";
					var errData = Map.of(MESSAGE_DATA_KEY, (Object) t.getMessage());
					var oldState = taskInfo.getState();
					taskInfo.setMessage(errMsg);
					taskInfo.putServiceProps(errData);
					if ( !((e instanceof RestClientException && !(e instanceof HttpClientErrorException))
							|| t instanceof IOException) ) {
						// stop processing job if not what appears to be a API IO exception
						log.info(
								"Stopping datum stream {} poll task by changing state from {} to {} after error: {}",
								taskInfo.getId().ident(), oldState, Completed, e.toString());
						taskInfo.setState(Completed);
					} else {
						// reset back to queued to try again
						log.info(
								"Resetting datum stream {} poll task by changing state from {} to {} after error: {}",
								taskInfo.getId().ident(), oldState, Queued, e.toString());
						taskInfo.setState(Queued);
					}
					userEventAppenderBiz.addEvent(taskInfo.getUserId(),
							eventForConfiguration(taskInfo.getId(), POLL_ERROR_TAGS, errMsg, errData));
					if ( !taskDao.updateTask(taskInfo, oldState) ) {
						log.warn(
								"Unable to update datum stream {} poll task info with expected state {} with details: {}",
								taskInfo.getId().ident(), oldState, taskInfo);
					}
				} catch ( Exception e2 ) {
					log.warn("Error updating datum stream {} poll task state after error",
							taskInfo.getId().ident(), e2);
					// ignore, return original
				}
				throw e;
			}
		}

		private CloudDatumStreamPollTaskEntity executeTask() throws Exception {
			final Instant execTime = clock.instant();

			final CloudDatumStreamConfiguration datumStream = datumStreamDao.get(taskInfo.getId());
			if ( datumStream == null ) {
				// configuration has been deleted... abort
				return taskInfo;
			}

			final CloudDatumStreamSettings datumStreamSettings = datumStreamSettingsDao.resolveSettings(
					datumStream.getUserId(), datumStream.getConfigId(), defaultDatumStreamSettings);

			final String datumStreamIdent = datumStream.getId().ident();

			if ( !datumStream.isFullyConfigured() ) {
				var errMsg = "Datum stream not fully configured.";
				userEventAppenderBiz.addEvent(datumStream.getUserId(),
						eventForConfiguration(datumStream.getId(), POLL_ERROR_TAGS, errMsg));
				taskInfo.setMessage(errMsg);
				taskInfo.setState(Completed); // stop processing job
				userEventAppenderBiz.addEvent(taskInfo.getUserId(),
						eventForConfiguration(taskInfo.getId(), POLL_ERROR_TAGS, errMsg));
				taskDao.updateTask(taskInfo, startState);
				return taskInfo;
			}

			if ( datumStream.getKind() == ObjectDatumKind.Node ) {
				SolarNodeOwnership ownership = nodeOwnershipDao
						.ownershipForNodeId(datumStream.getObjectId());
				if ( ownership == null || !taskInfo.getUserId().equals(ownership.getUserId()) ) {
					log.warn(
							"Refusing to execute datum stream {} poll task because task owner {} does not own node {}",
							datumStreamIdent, taskInfo.getUserId(), datumStream.getObjectId());
					var errMsg = "Access denied to configured node.";
					var errData = Map.of(SOURCE_DATA_KEY, (Object) datumStream.getObjectId());
					taskInfo.setMessage(errMsg);
					taskInfo.putServiceProps(errData);
					taskInfo.setState(Completed); // stop processing job
					userEventAppenderBiz.addEvent(datumStream.getUserId(), eventForConfiguration(
							datumStream.getId(), POLL_ERROR_TAGS, errMsg, errData));
					taskDao.updateTask(taskInfo, startState);
					return taskInfo;
				}
			}

			// save task state to Executing (TODO maybe we don't need this step?)
			if ( !taskDao.updateTaskState(taskInfo.getId(), Executing, startState) ) {
				log.warn("Failed to reset poll task {} to execution @ {} starting @ {}",
						datumStreamIdent, taskInfo.getExecuteAt(), taskInfo.getStartAt());
				var errMsg = "Failed to udpate task state from Claimed to Executing.";
				var errData = Map.of(SOURCE_DATA_KEY, (Object) datumStreamIdent);
				userEventAppenderBiz.addEvent(datumStream.getUserId(),
						eventForConfiguration(datumStream.getId(), POLL_ERROR_TAGS, errMsg, errData));
				return taskInfo;
			}
			taskInfo.setState(Executing);

			final Trigger schedule = triggerForSchedule(datumStream);
			if ( schedule == null ) {
				var errMsg = "Datum Stream service schedule not provided or usable.";
				var errData = Map.of(SOURCE_DATA_KEY, (Object) datumStream.getSchedule());
				taskInfo.setMessage(errMsg);
				taskInfo.putServiceProps(errData);
				taskInfo.setState(Completed); // stop processing job
				userEventAppenderBiz.addEvent(datumStream.getUserId(),
						eventForConfiguration(datumStream.getId(), POLL_ERROR_TAGS, errMsg, errData));
				taskDao.updateTask(taskInfo, Executing);
				return taskInfo;
			}

			// save this to calculate next exec date based on schedule
			final Instant execDate = taskInfo.getExecuteAt();

			final CloudDatumStreamService datumStreamService = datumStreamServiceProvider
					.apply(datumStream.getServiceIdentifier());
			if ( datumStreamService == null ) {
				// service no longer supported?...
				var errMsg = "Configured Datum Stream service not available.";
				var errData = Map.of(SOURCE_DATA_KEY, (Object) datumStream.getServiceIdentifier());
				taskInfo.setMessage(errMsg);
				taskInfo.putServiceProps(errData);
				taskInfo.setState(Completed); // stop processing job
				userEventAppenderBiz.addEvent(datumStream.getUserId(),
						eventForConfiguration(datumStream.getId(), POLL_ERROR_TAGS, errMsg, errData));
				taskDao.updateTask(taskInfo, Executing);
				return taskInfo;
			}

			final var filter = new BasicQueryFilter();
			filter.setStartDate(taskInfo.getStartAt());
			filter.setEndDate(clock.instant());

			log.debug("Polling for {} datum with filter {}", datumStreamIdent, filter);
			final var polledDatum = datumStreamService.datum(datumStream, filter);

			Instant lastDatumDate = null;
			if ( polledDatum != null && !polledDatum.isEmpty() ) {
				log.debug("Polling for {} found {} datum to import", datumStreamIdent,
						polledDatum.size());
				for ( var datum : polledDatum ) {
					if ( datum instanceof DatumEntity d ) {
						if ( datumStreamSettings.isPublishToSolarIn() ) {
							datumDao.store(d);
						}
					} else if ( datum instanceof GeneralObjectDatum<?> d ) {
						if ( datumStreamSettings.isPublishToSolarIn() ) {
							datumDao.persist(d);
						}
					} else {
						if ( datumStreamSettings.isPublishToSolarIn() ) {
							datumDao.store(datum);
						}
					}
					if ( lastDatumDate == null || lastDatumDate.isBefore(datum.getTimestamp()) ) {
						lastDatumDate = datum.getTimestamp();
					}
				}
			}

			// success: update task info
			if ( polledDatum.getUsedQueryFilter() != null
					&& polledDatum.getUsedQueryFilter().hasEndDate() ) {
				// use the end date provided by the results, so the next iteration picks up from there
				taskInfo.setStartAt(polledDatum.getUsedQueryFilter().getEndDate());
			} else if ( lastDatumDate != null ) {
				// set new start date to date of last datum; this might update the same datum more than once
				// across different poll executions, but that supports cloud services that return constantly
				// updating aggregate values at the same timestamp
				taskInfo.setStartAt(lastDatumDate);
			}

			// calculate the next execution time based on the datum stream schedule
			var now = clock.instant();
			var ctx = new SimpleTriggerContext(clock);
			Instant nextExecTime = execDate;
			while ( nextExecTime.isBefore(now) ) {
				// skip any missed execution times between last actual execution and now...
				ctx.update(nextExecTime,
						(ctx.lastScheduledExecution() == null ? execTime : nextExecTime), now);
				nextExecTime = schedule.nextExecution(ctx).truncatedTo(ChronoUnit.SECONDS);
			}
			taskInfo.setExecuteAt(nextExecTime);

			// reset task back to Queued so it can be executed again
			taskInfo.setState(Queued);

			// reset message back to null
			taskInfo.setMessage(null);

			// reset props
			taskInfo.setServiceProps(null);

			// save task state
			if ( !taskDao.updateTask(taskInfo, Executing) ) {
				log.warn("Failed to reset poll task {} to execution @ {} starting @ {}",
						datumStreamIdent, taskInfo.getExecuteAt(), taskInfo.getStartAt());
				var errMsg = "Failed to reset task state.";
				var errData = Map.of(SOURCE_DATA_KEY, (Object) datumStreamIdent, "executeAt",
						taskInfo.getExecuteAt(), "startAt", taskInfo.getStartAt());
				userEventAppenderBiz.addEvent(datumStream.getUserId(),
						eventForConfiguration(datumStream.getId(), POLL_ERROR_TAGS, errMsg, errData));
			}
			return taskInfo;
		}

	}

	private Trigger triggerForSchedule(CloudDatumStreamConfiguration datumStream) {
		assert datumStream != null;
		final String schedule = datumStream.getSchedule();
		Trigger t = SchedulerUtils.triggerForExpression(schedule, TimeUnit.SECONDS, false);
		if ( t instanceof PeriodicTrigger pt ) {
			pt.setFixedRate(false);
		}
		return t;
	}

	@Override
	public int resetAbandondedExecutingTasks(Instant olderThan) {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * Get the maximum length of time to wait for executing tasks to complete
	 * when {@link #serviceDidShutdown()} is invoked.
	 *
	 * @return the maximum wait time, never {@literal null}
	 */
	public final Duration getShutdownMaxWait() {
		return shutdownMaxWait;
	}

	/**
	 * Set the maximum length of time to wait for executing tasks to complete
	 * when {@link #serviceDidShutdown()} is invoked.
	 *
	 * @param shutdownMaxWait
	 *        the maximum wait time to set; if {@literal null} then
	 *        {@link #DEFAULT_SHUTDOWN_MAX_WAIT} will be used
	 */
	public final void setShutdownMaxWait(Duration shutdownMaxWait) {
		this.shutdownMaxWait = (shutdownMaxWait != null ? shutdownMaxWait : DEFAULT_SHUTDOWN_MAX_WAIT);
	}

	/**
	 * Get the default datum stream settings.
	 *
	 * @return the settings, never {@literal null}
	 * @since 1.3
	 */
	public final CloudDatumStreamSettings getDefaultDatumStreamSettings() {
		return defaultDatumStreamSettings;
	}

	/**
	 * Set the default datum stream settings.
	 *
	 * @param defaultDatumStreamSettings
	 *        the settings to set; if {@code null} then
	 *        {@link #DEFAULT_DATUM_STREAM_SETTINGS} will be used
	 * @since 1.3
	 */
	public final void setDefaultDatumStreamSettings(
			CloudDatumStreamSettings defaultDatumStreamSettings) {
		this.defaultDatumStreamSettings = (defaultDatumStreamSettings != null
				? defaultDatumStreamSettings
				: DEFAULT_DATUM_STREAM_SETTINGS);
	}

}
