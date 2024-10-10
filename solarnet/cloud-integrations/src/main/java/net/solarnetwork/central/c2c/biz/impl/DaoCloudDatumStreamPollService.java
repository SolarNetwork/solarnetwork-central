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
import static net.solarnetwork.central.domain.BasicClaimableJobState.Executing;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamPollService;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPollTaskDao;
import net.solarnetwork.central.c2c.domain.BasicQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPollTaskEntity;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents;
import net.solarnetwork.central.datum.domain.GeneralObjectDatum;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumWriteOnlyDao;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.scheduler.SchedulerUtils;
import net.solarnetwork.service.ServiceLifecycleObserver;

/**
 * DAO based implementation of {@link CloudDatumStreamPollService}.
 *
 * @author matt
 * @version 1.0
 */
public class DaoCloudDatumStreamPollService
		implements CloudDatumStreamPollService, ServiceLifecycleObserver, CloudIntegrationsUserEvents {

	/** The {@code shutdownMaxWait} property default value: 1 minute. */
	public static final Duration DEFAULT_SHUTDOWN_MAX_WAIT = Duration.ofMinutes(1);

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Clock clock;
	private final UserEventAppenderBiz userEventAppenderBiz;
	private final CloudDatumStreamPollTaskDao taskDao;
	private final CloudDatumStreamConfigurationDao datumStreamDao;
	private final DatumWriteOnlyDao datumDao;
	private final ExecutorService executorService;
	private final Function<String, CloudDatumStreamService> datumStreamServiceProvider;
	private Duration shutdownMaxWait = DEFAULT_SHUTDOWN_MAX_WAIT;

	/**
	 * Constructor.
	 *
	 * @param clock
	 *        the clock to use
	 * @param userEventAppenderBiz
	 *        the user event appender service
	 * @param taskDao
	 *        the task DAO
	 * @param datumStreamDao
	 *        the datum stream DAO
	 * @param datumDao
	 *        the datum DAO
	 * @param executorSupplier
	 *        the executor supplier
	 * @param datumStreamServiceProvider
	 *        function that provides a {@link CloudDatumStreamService} for a
	 *        given service identifier
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoCloudDatumStreamPollService(Clock clock, UserEventAppenderBiz userEventAppenderBiz,
			CloudDatumStreamPollTaskDao taskDao, CloudDatumStreamConfigurationDao datumStreamDao,
			DatumWriteOnlyDao datumDao, Supplier<ExecutorService> executorSupplier,
			Function<String, CloudDatumStreamService> datumStreamServiceProvider) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		this.userEventAppenderBiz = requireNonNullArgument(userEventAppenderBiz, "userEventAppenderBiz");
		this.taskDao = requireNonNullArgument(taskDao, "taskDao");
		this.datumStreamDao = requireNonNullArgument(datumStreamDao, "datumStreamDao");
		this.datumDao = requireNonNullArgument(datumDao, "datumDao");
		this.executorService = requireNonNullArgument(
				requireNonNullArgument(executorSupplier, "executorSupplier").get(), "executor");
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
		return taskDao.claimQueuedTask();
	}

	@Override
	public Future<CloudDatumStreamPollTaskEntity> executeTask(CloudDatumStreamPollTaskEntity task) {
		return executorService.submit(new CloudDatumStreamPollTask(task));
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
			final Instant execTime = clock.instant();

			final CloudDatumStreamConfiguration datumStream = datumStreamDao.get(taskInfo.getId());
			if ( datumStream == null ) {
				// configuration has been deleted... abort
				return null;
			}
			final String datumStreamIdent = datumStream.getId().ident();

			// save task state to Executing (TODO maybe we don't need this step?)
			if ( !taskDao.updateTaskState(taskInfo.getId(), Executing,
					new LinkedHashSet<>(List.of(startState))) ) {
				log.warn("Failed to reset poll task {} to execution @ {} starting @ {}",
						datumStreamIdent, taskInfo.getExecuteAt(), taskInfo.getStartAt());
				var errMsg = "Failed to udpate task state from Claimed to Executing.";
				var errData = Map.of(SOURCE_DATA_KEY, (Object) datumStreamIdent);
				userEventAppenderBiz.addEvent(datumStream.getUserId(),
						eventForConfiguration(datumStream.getId(), POLL_ERROR_TAGS, errMsg, errData));
				return null;
			}

			final Trigger schedule = triggerForSchedule(datumStream);
			if ( schedule == null ) {
				var errMsg = "Datum Stream service schedule not provided or usable.";
				var errData = Map.of(SOURCE_DATA_KEY, (Object) datumStream.getSchedule());
				taskInfo.setMessage(errMsg);
				if ( taskInfo.getServiceProps() == null ) {
					taskInfo.setServiceProps(errData);
				} else {
					taskInfo.getServiceProps().putAll(errData);
				}
				taskInfo.setState(BasicClaimableJobState.Completed); // stop processing job
				userEventAppenderBiz.addEvent(datumStream.getUserId(),
						eventForConfiguration(datumStream.getId(), POLL_ERROR_TAGS, errMsg, errData));
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
				if ( taskInfo.getServiceProps() == null ) {
					taskInfo.setServiceProps(errData);
				} else {
					taskInfo.getServiceProps().putAll(errData);
				}
				taskInfo.setState(BasicClaimableJobState.Completed); // stop processing job
				userEventAppenderBiz.addEvent(datumStream.getUserId(),
						eventForConfiguration(datumStream.getId(), POLL_ERROR_TAGS, errMsg, errData));
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
						datumDao.store(d);
					} else if ( datum instanceof GeneralObjectDatum<?> d ) {
						datumDao.persist(d);
					} else {
						datumDao.store(datum);
					}
					if ( lastDatumDate == null || lastDatumDate.isBefore(datum.getTimestamp()) ) {
						lastDatumDate = datum.getTimestamp();
					}
				}
			}

			// success: update task info
			if ( lastDatumDate != null ) {
				// set new start date to date of last datum; this might update the same datum more than once
				// across different poll executions, but that supports cloud services that return constantly
				// updating aggregate values at the same timestamp
				taskInfo.setStartAt(lastDatumDate);
			}

			// calculate the next execution time based on the datum stream schedule
			var ctx = new SimpleTriggerContext(clock);
			ctx.update(execDate, execTime, clock.instant());
			Instant nextExecTime = schedule.nextExecution(ctx);
			taskInfo.setExecuteAt(nextExecTime);

			// reset task back to Queued so it can be executed again
			taskInfo.setState(BasicClaimableJobState.Queued);

			// reset message back to null
			taskInfo.setMessage(null);

			// reset props
			taskInfo.setServiceProps(null);

			// save task state
			if ( !taskDao.updateTask(taskInfo, new LinkedHashSet<>(List.of(Executing))) ) {
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
		return SchedulerUtils.triggerForExpression(schedule, TimeUnit.SECONDS, false);
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

}
