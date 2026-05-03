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

import static net.solarnetwork.central.domain.BasicClaimableJobState.Completed;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Executing;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Queued;
import static net.solarnetwork.central.domain.CommonUserEvents.eventForUserRelatedKey;
import static net.solarnetwork.util.CollectionUtils.getMapLong;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.web.client.RestClientResponseException;
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
import net.solarnetwork.central.datum.biz.DatumProcessor;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralObjectDatum;
import net.solarnetwork.central.datum.support.DatumUtils;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.dao.DatumWriteOnlyDao;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.scheduler.SchedulerUtils;
import net.solarnetwork.domain.datum.BasicStreamDatum;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumIdentity;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;
import net.solarnetwork.domain.datum.StreamDatum;
import net.solarnetwork.service.RemoteServiceException;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.util.StringNaturalSortComparator;

/**
 * DAO based implementation of {@link CloudDatumStreamPollService}.
 *
 * @author matt
 * @version 2.0
 */
public class DaoCloudDatumStreamPollService
		implements CloudDatumStreamPollService, ServiceLifecycleObserver, CloudIntegrationsUserEvents {

	/** The {@code shutdownMaxWait} property default value: 1 minute. */
	public static final Duration DEFAULT_SHUTDOWN_MAX_WAIT = Duration.ofMinutes(1);

	/**
	 * The {@code fastRescheduleMinLag} property default value: 1 day.
	 *
	 * @since 1.7
	 */
	public static final Duration DEFAULT_FAST_RESCHEDULE_MIN_LAG = Duration.ofDays(1);

	/**
	 * The {@code fastRescheduleAmount} property default value: 5 minutes.
	 *
	 * @since 1.7
	 */
	public static final Duration DEFAULT_FAST_RESCHEDULE_AMOUNT = Duration.ofMinutes(5);

	/**
	 * The {@code requeueErrorCountMaximum} property default value.
	 *
	 * @since 1.9
	 */
	public static final int DEFAULT_REQUEUE_ERROR_COUNT_MAXIMUM = 100;

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
	private final DatumStreamMetadataDao datumStreamMetadataDao;
	private final DatumWriteOnlyDao datumDao;
	private final ExecutorService executorService;
	private final Function<String, CloudDatumStreamService> datumStreamServiceProvider;
	private Duration fastRescheduleMinLag = DEFAULT_FAST_RESCHEDULE_MIN_LAG;
	private Duration fastRescheduleAmount = DEFAULT_FAST_RESCHEDULE_AMOUNT;
	private Duration shutdownMaxWait = DEFAULT_SHUTDOWN_MAX_WAIT;
	private int requeueErrorCountMaximum = DEFAULT_REQUEUE_ERROR_COUNT_MAXIMUM;
	private CloudDatumStreamSettings defaultDatumStreamSettings = DEFAULT_DATUM_STREAM_SETTINGS;
	private @Nullable DatumProcessor fluxPublisher;

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
	 * @param datumStreamMetadataDao
	 *        the datum stream metadata DAO
	 * @param datumDao
	 *        the datum DAO
	 * @param executor
	 *        the executor; this must be exclusive to this service, as it will
	 *        be shut down when this service is shut down
	 * @param datumStreamServiceProvider
	 *        function that provides a {@link CloudDatumStreamService} for a
	 *        given service identifier
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DaoCloudDatumStreamPollService(Clock clock, UserEventAppenderBiz userEventAppenderBiz,
			SolarNodeOwnershipDao nodeOwnershipDao, CloudDatumStreamPollTaskDao taskDao,
			CloudDatumStreamConfigurationDao datumStreamDao,
			CloudDatumStreamSettingsEntityDao datumStreamSettingsDao,
			DatumStreamMetadataDao datumStreamMetadataDao, DatumWriteOnlyDao datumDao,
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
		this.datumStreamMetadataDao = requireNonNullArgument(datumStreamMetadataDao,
				"datumStreamMetadataDao");
		this.datumDao = requireNonNullArgument(datumDao, "datumDao");
		this.executorService = requireNonNullArgument(executor, "executor");
		this.datumStreamServiceProvider = requireNonNullArgument(datumStreamServiceProvider,
				"datumStreamServiceProvider");
	}

	@Override
	public void serviceDidStartup() {
		// nothing
	}

	@SuppressWarnings("JavaDurationGetSecondsToToSeconds")
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
	public @Nullable CloudDatumStreamPollTaskEntity claimQueuedTask() {
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
			log.debug("Datum stream poll task execution rejected, resetting state to Queued: {}",
					e.getMessage());
			// go back to queued
			if ( !taskDao.updateTaskState(task.id(), Queued, task.getState()) ) {
				log.warn("Failed to update rejected datum stream poll task {} state from {} to Queued",
						task.id().ident(), task.getState());
			}
			throw e;
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
					if ( log.isDebugEnabled() || !(e instanceof RemoteServiceException) ) {
						// log full stack trace when debug enabled or not a RemoteServiceException
						log.warn("Error executing datum stream {} poll task", taskInfo.id().ident(), e);
					} else {
						// otherwise just print exception message, to cut down on log clutter
						log.warn("Error executing datum stream {} poll task: {}", taskInfo.id().ident(),
								e.toString());
					}
					var prevErrorCount = getMapLong(ERROR_COUNT_DATA_KEY,
							taskInfo.getServiceProperties());
					long errorCount = prevErrorCount != null ? prevErrorCount + 1L : 1L;
					var errMsg = "Error executing poll task.";
					var errData = Map.of(MESSAGE_DATA_KEY, (Object) t.getMessage(), ERROR_COUNT_DATA_KEY,
							errorCount);
					var oldState = taskInfo.getState();
					taskInfo.setMessage(errMsg);
					taskInfo.putServiceProps(errData);
					if ( t instanceof RestClientResponseException || t instanceof IOException ) {
						if ( errorCount < requeueErrorCountMaximum ) {
							// reset back to queued to try again if HTTP client or IO error
							log.info(
									"Resetting datum stream {} poll task by changing state from {} to {} after error: {}",
									taskInfo.id().ident(), oldState, Queued, e.toString());
							taskInfo.setState(Queued);
							if ( taskInfo.getExecuteAt().isBefore(clock.instant()) ) {
								// bump date into future by 1 minute so we do not immediately try to process again
								taskInfo.setExecuteAt(clock.instant().plus(1, ChronoUnit.MINUTES));
							}
						} else {
							log.info(
									"Stopping datum stream {} poll task by changing state from {} to {} after {} repeated errors, most recently: {}",
									taskInfo.id().ident(), oldState, Completed, errorCount,
									e.toString());
							taskInfo.setState(Completed);
						}
					} else {
						// stop processing job if not what appears to be an API IO exception
						log.info(
								"Stopping datum stream {} poll task by changing state from {} to {} after error: {}",
								taskInfo.id().ident(), oldState, Completed, e.toString());
						taskInfo.setState(Completed);
					}
					userEventAppenderBiz.addEvent(taskInfo.getUserId(), eventForUserRelatedKey(
							taskInfo.getId(), INTEGRATION_POLL_ERROR_TAGS, errMsg, errData));
					if ( !taskDao.updateTask(taskInfo, oldState) ) {
						log.warn(
								"Unable to update datum stream {} poll task info with expected state {} with details: {}",
								taskInfo.id().ident(), oldState, taskInfo);
					}
				} catch ( Exception e2 ) {
					log.warn("Error updating datum stream {} poll task state after error",
							taskInfo.id().ident(), e2);
					// ignore, return original
				}
				throw e;
			}
		}

		private CloudDatumStreamPollTaskEntity executeTask() throws Exception {
			final Instant execTime = clock.instant();

			final CloudDatumStreamConfiguration datumStream = datumStreamDao.get(taskInfo.id());
			if ( datumStream == null ) {
				// configuration has been deleted... abort
				return taskInfo;
			}

			final CloudDatumStreamSettings datumStreamSettings = datumStreamSettingsDao.resolveSettings(
					datumStream.getUserId(), datumStream.getConfigId(), defaultDatumStreamSettings);

			final String datumStreamIdent = datumStream.id().ident();

			if ( !datumStream.isFullyConfigured() ) {
				var errMsg = "Datum stream not fully configured.";
				userEventAppenderBiz.addEvent(datumStream.getUserId(),
						eventForUserRelatedKey(datumStream.id(), INTEGRATION_POLL_ERROR_TAGS, errMsg));
				taskInfo.setMessage(errMsg);
				taskInfo.setState(Completed); // stop processing job
				taskDao.updateTask(taskInfo, startState);
				return taskInfo;
			}

			// get nonnull refs, after call to isFullyConfigured()
			final Long objectId = nonnull(datumStream.getObjectId(), "Object ID");
			final ObjectDatumKind kind = nonnull(datumStream.getKind(), "Kind");

			if ( datumStream.getKind() == ObjectDatumKind.Node ) {
				SolarNodeOwnership ownership = nodeOwnershipDao.ownershipForNodeId(objectId);
				if ( ownership == null || !taskInfo.getUserId().equals(ownership.getUserId()) ) {
					log.warn(
							"Refusing to execute datum stream {} poll task because task owner {} does not own node {}",
							datumStreamIdent, taskInfo.getUserId(), objectId);
					var errMsg = "Access denied to configured node.";
					var errData = Map.of(SOURCE_DATA_KEY, (Object) objectId);
					taskInfo.setMessage(errMsg);
					taskInfo.putServiceProps(errData);
					taskInfo.setState(Completed); // stop processing job
					userEventAppenderBiz.addEvent(datumStream.getUserId(), eventForUserRelatedKey(
							datumStream.getId(), INTEGRATION_POLL_ERROR_TAGS, errMsg, errData));
					taskDao.updateTask(taskInfo, startState);
					return taskInfo;
				}
			}

			// save task state to Executing (TODO maybe we don't need this step?)
			if ( !taskDao.updateTaskState(taskInfo.id(), Executing, startState) ) {
				log.warn("Failed to update poll task {} state to Executing @ {} starting @ {}",
						datumStreamIdent, taskInfo.getExecuteAt(), taskInfo.getStartAt());
				var errMsg = "Failed to update task state from Claimed to Executing.";
				var errData = Map.of(SOURCE_DATA_KEY, (Object) datumStreamIdent);
				userEventAppenderBiz.addEvent(datumStream.getUserId(), eventForUserRelatedKey(
						datumStream.getId(), INTEGRATION_POLL_ERROR_TAGS, errMsg, errData));
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
				userEventAppenderBiz.addEvent(datumStream.getUserId(), eventForUserRelatedKey(
						datumStream.getId(), INTEGRATION_POLL_ERROR_TAGS, errMsg, errData));
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
				userEventAppenderBiz.addEvent(datumStream.getUserId(), eventForUserRelatedKey(
						datumStream.getId(), INTEGRATION_POLL_ERROR_TAGS, errMsg, errData));
				taskDao.updateTask(taskInfo, Executing);
				return taskInfo;
			}

			final var filter = new BasicQueryFilter();
			filter.setStartDate(taskInfo.getStartAt());
			filter.setEndDate(clock.instant());

			userEventAppenderBiz.addEvent(datumStream.getUserId(),
					eventForUserRelatedKey(datumStream.getId(), INTEGRATION_POLL_TAGS, "Poll for datum",
							Map.of("executeAt", taskInfo.getExecuteAt(), "startAt",
									taskInfo.getStartAt(), "endAt", filter.getEndDate(), "startedAt",
									execTime)));

			log.debug("Polling for {} datum with filter {}", datumStreamIdent, filter);
			final var polledDatum = datumStreamService.datum(datumStream, filter);

			Instant lastDatumDate = null;
			if ( polledDatum != null && !polledDatum.isEmpty() ) {
				log.debug("Polling for {} found {} datum to import", datumStreamIdent,
						polledDatum.size());
				final DatumProcessor fluxPublisher = getFluxPublisher();

				// cache datum stream  metadata to speed up conversion to StreamDatum
				final Map<ObjectDatumStreamMetadataId, ObjectDatumStreamMetadata> streamMetaCache = new HashMap<>(
						8);

				for ( var datum : polledDatum ) {
					final var datumId = datum.datumIdent();
					// validate that provided datum ID matches that on the configuration
					if ( !objectId.equals(datumId.getObjectId()) ) {
						log.warn(
								"Datum stream {} configured with object ID {} but produced datum with object ID {}: cancelling poll task.",
								datumStreamIdent, taskInfo.getUserId(), datumStream.getObjectId());
						var errMsg = "Access denied to datum with object ID different from datum stream configuration.";
						var errData = Map.of(SOURCE_DATA_KEY, (Object) datumId.getObjectId(), "expected",
								objectId);
						taskInfo.setMessage(errMsg);
						taskInfo.putServiceProps(errData);
						taskInfo.setState(Completed); // stop processing job
						userEventAppenderBiz.addEvent(datumStream.getUserId(), eventForUserRelatedKey(
								datumStream.getId(), INTEGRATION_POLL_ERROR_TAGS, errMsg, errData));
						taskDao.updateTask(taskInfo, Executing);
						return taskInfo;
					}
					final ObjectDatumKind datumKind = (datumId.getKind() != null ? datumId.getKind()
							: kind);
					if ( !kind.equals(datumKind) ) {
						log.warn(
								"Datum stream {} configured with kind {} but produced datum with kind {}: cancelling rake task.",
								datumStreamIdent, kind, datumKind);
						var errMsg = "Access denied to datum with kind different from datum stream configuration.";
						var errData = Map.of(SOURCE_DATA_KEY, (Object) datumKind, "expected", kind);
						taskInfo.setMessage(errMsg);
						taskInfo.putServiceProps(errData);
						taskInfo.setState(Completed); // stop processing job
						userEventAppenderBiz.addEvent(datumStream.getUserId(), eventForUserRelatedKey(
								datumStream.getId(), INTEGRATION_POLL_ERROR_TAGS, errMsg, errData));
						taskDao.updateTask(taskInfo, Executing);
						return taskInfo;
					}
					if ( datumId.getSourceId() == null ) {
						continue;
					}
					if ( datumId instanceof DatumEntity d ) {
						if ( datumStreamSettings.isPublishToSolarIn() ) {
							datumDao.store(d);
						}
					} else if ( datumId instanceof GeneralObjectDatum<?> d ) {
						if ( datumStreamSettings.isPublishToSolarIn() ) {
							final StreamDatum sDatum = datumStreamDatum(datumId, datum, streamMetaCache);
							if ( sDatum != null ) {
								datumDao.store(sDatum);
							} else {
								datumDao.persist(d);
							}
						}
						if ( fluxPublisher != null && datumStreamSettings.isPublishToSolarFlux()
								&& datumId instanceof GeneralNodeDatum nodeDatum ) {
							fluxPublisher.processDatum(nodeDatum);
						}
					} else {
						if ( datumStreamSettings.isPublishToSolarIn() ) {
							final StreamDatum sDatum = datumStreamDatum(datumId, datum, streamMetaCache);
							if ( sDatum != null ) {
								datumDao.store(sDatum);
							} else {
								datumDao.store(datum);
							}
						}
						if ( fluxPublisher != null && datumStreamSettings.isPublishToSolarFlux()
								&& kind == ObjectDatumKind.Node ) {
							GeneralObjectDatum<?> gd = DatumUtils.convertGeneralDatum(datum);
							if ( gd instanceof GeneralNodeDatum nodeDatum ) {
								fluxPublisher.processDatum(nodeDatum);
							}
						}
					}
					if ( lastDatumDate == null || lastDatumDate.isBefore(datumId.getTimestamp()) ) {
						lastDatumDate = datumId.getTimestamp();
					}
				}
			}

			// success: update task info
			if ( polledDatum != null && polledDatum.getNextQueryFilter() != null
					&& polledDatum.getNextQueryFilter().getStartDate() != null ) {
				// use the start date provided by the results, so the next iteration picks up from there
				taskInfo.setStartAt(polledDatum.getNextQueryFilter().getStartDate());
			} else if ( polledDatum != null && polledDatum.getUsedQueryFilter() != null
					&& polledDatum.getUsedQueryFilter().getEndDate() != null ) {
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
			if ( fastRescheduleMinLag.isPositive() && Duration.between(taskInfo.getStartAt(), now)
					.compareTo(fastRescheduleMinLag) > 0 ) {
				log.info("Fast-rescheduling datum stream [{}] by {} to catch stream up",
						datumStreamIdent, fastRescheduleAmount);
				nextExecTime = now.plus(fastRescheduleAmount).truncatedTo(ChronoUnit.SECONDS);
			} else {
				while ( nextExecTime.isBefore(now) ) {
					// skip any missed execution times between last actual execution and now...
					ctx.update(nextExecTime,
							(ctx.lastScheduledExecution() == null ? execTime : nextExecTime), now);
					Instant net = schedule.nextExecution(ctx);
					if ( net == null ) {
						break;
					}
					nextExecTime = net.truncatedTo(ChronoUnit.SECONDS);
				}
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
				log.warn("Failed to reset poll task {} @ {} starting @ {}", datumStreamIdent,
						taskInfo.getExecuteAt(), taskInfo.getStartAt());
				var errMsg = "Failed to reset task state.";
				var errData = Map.of("executeAt", taskInfo.getExecuteAt(), "startAt",
						taskInfo.getStartAt());
				userEventAppenderBiz.addEvent(datumStream.getUserId(), eventForUserRelatedKey(
						datumStream.getId(), INTEGRATION_POLL_ERROR_TAGS, errMsg, errData));
			} else {
				var msg = "Reset task state";
				var data = new LinkedHashMap<String, Object>(4);
				data.put("executeAt", taskInfo.getExecuteAt());
				data.put("startAt", taskInfo.getStartAt());
				data.put("datumImportCount", polledDatum != null ? polledDatum.size() : 0);
				if ( lastDatumDate != null ) {
					data.put("datumLastDate", lastDatumDate);
				}
				if ( polledDatum != null && !polledDatum.isEmpty() ) {
					data.put("datumImportCount", polledDatum.size());
					Map<String, Integer> sourceCounts = new TreeMap<>(
							StringNaturalSortComparator.CASE_INSENSITIVE_NATURAL_SORT);
					Map<String, Instant> lastDates = new TreeMap<>(
							StringNaturalSortComparator.CASE_INSENSITIVE_NATURAL_SORT);
					for ( var datum : polledDatum ) {
						final var datumId = datum.datumIdent();
						sourceCounts.compute(datumId.getSourceId(), (_, old) -> {
							if ( old == null ) {
								return 1;
							}
							return old + 1;
						});
						lastDates.compute(datumId.getSourceId(), (_, old) -> {
							if ( old == null ) {
								return datumId.getTimestamp();
							}
							return datumId.getTimestamp().isAfter(old) ? datum.getTimestamp() : old;
						});
					}
					data.put("datumImportCountBySource", sourceCounts);
					data.put("datumLastDateBySource", lastDates);
				}
				userEventAppenderBiz.addEvent(datumStream.getUserId(),
						eventForUserRelatedKey(datumStream.getId(), INTEGRATION_POLL_TAGS, msg, data));
			}
			return taskInfo;
		}

	}

	private @Nullable StreamDatum datumStreamDatum(DatumIdentity datumId, Datum datum,
			Map<ObjectDatumStreamMetadataId, ObjectDatumStreamMetadata> cache) {
		final ObjectDatumStreamMetadata meta = datumStreamMetadata(datumId, cache);
		if ( meta != null ) {
			try {
				var datumProps = DatumProperties.propertiesFrom(datum, meta);
				if ( datumProps != null ) {
					return new BasicStreamDatum(meta.getStreamId(), datumId.getTimestamp(), datumProps);
				}
			} catch ( IllegalArgumentException e ) {
				// incompatible properties for stream; fall back to generic datum
			}
		}
		return null;
	}

	private @Nullable ObjectDatumStreamMetadata datumStreamMetadata(DatumIdentity datumId,
			Map<ObjectDatumStreamMetadataId, ObjectDatumStreamMetadata> cache) {
		final var metaId = new ObjectDatumStreamMetadataId(datumId.getKind(), datumId.getObjectId(),
				datumId.getSourceId());
		ObjectDatumStreamMetadata meta = cache.get(metaId);
		if ( meta == null ) {
			var f = new BasicDatumCriteria();
			if ( datumId.getKind() == ObjectDatumKind.Location ) {
				f.setLocationId(datumId.getObjectId());
			} else {
				f.setNodeId(datumId.getObjectId());
			}
			f.setSourceId(datumId.getSourceId());
			for ( ObjectDatumStreamMetadata m : datumStreamMetadataDao.findDatumStreamMetadata(f) ) {
				meta = m;
				cache.put(metaId, meta);
				break;
			}
		}
		return meta;
	}

	private @Nullable Trigger triggerForSchedule(CloudDatumStreamConfiguration datumStream) {
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
		return taskDao.resetAbandondedExecutingTasks(olderThan);
	}

	/**
	 * Get the maximum length of time to wait for executing tasks to complete
	 * when {@link #serviceDidShutdown()} is invoked.
	 *
	 * @return the maximum wait time, never {@code null}
	 */
	public final Duration getShutdownMaxWait() {
		return shutdownMaxWait;
	}

	/**
	 * Set the maximum length of time to wait for executing tasks to complete
	 * when {@link #serviceDidShutdown()} is invoked.
	 *
	 * @param shutdownMaxWait
	 *        the maximum wait time to set; if {@code null} then
	 *        {@link #DEFAULT_SHUTDOWN_MAX_WAIT} will be used
	 */
	public final void setShutdownMaxWait(Duration shutdownMaxWait) {
		this.shutdownMaxWait = (shutdownMaxWait != null ? shutdownMaxWait : DEFAULT_SHUTDOWN_MAX_WAIT);
	}

	/**
	 * Get the default datum stream settings.
	 *
	 * @return the settings, never {@code null}
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

	/**
	 * Get the SolarFlux publisher.
	 *
	 * @return the publisher, or {@code null}
	 */
	public final @Nullable DatumProcessor getFluxPublisher() {
		return fluxPublisher;
	}

	/**
	 * Set the SolarFlux publisher.
	 *
	 * @param fluxPublisher
	 *        the publisher to set
	 */
	public final void setFluxPublisher(@Nullable DatumProcessor fluxPublisher) {
		this.fluxPublisher = fluxPublisher;
	}

	/**
	 * Get the "fast reschedule" minimum lag.
	 *
	 * @return the duration; defaults to
	 *         {@link #DEFAULT_FAST_RESCHEDULE_MIN_LAG}
	 * @since 1.7
	 */
	public final Duration getFastRescheduleMinLag() {
		return fastRescheduleMinLag;
	}

	/**
	 * Set the "fast reschedule" minimum lag.
	 *
	 * <p>
	 * This is the amount of time in the past a task's {@code startAt} must be
	 * to "fast reschedule" a task.
	 * </p>
	 *
	 * @param fastRescheduleMinLag
	 *        the duration to set; if {@code null} then
	 *        {@link #DEFAULT_FAST_RESCHEDULE_MIN_LAG} will be used
	 * @since 1.7
	 */
	public final void setFastRescheduleMinLag(Duration fastRescheduleMinLag) {
		this.fastRescheduleMinLag = (fastRescheduleMinLag != null ? fastRescheduleMinLag
				: DEFAULT_FAST_RESCHEDULE_MIN_LAG);
	}

	/**
	 * Get the "fast reschedule" amount.
	 *
	 * @return the duration; defaults to {@link #DEFAULT_FAST_RESCHEDULE_AMOUNT}
	 * @since 1.7
	 */
	public final Duration getFastRescheduleAmount() {
		return fastRescheduleAmount;
	}

	/**
	 * Set the "fast reschedule" amount.
	 *
	 * <p>
	 * This is the amount of time in the future to "fast reschedule" a task at.
	 * </p>
	 *
	 * @param fastRescheduleAmount
	 *        the duration to set; if {@code null} then
	 *        {@link #DEFAULT_FAST_RESCHEDULE_AMOUNT} will be used
	 * @since 1.7
	 */
	public final void setFastRescheduleAmount(Duration fastRescheduleAmount) {
		this.fastRescheduleAmount = (fastRescheduleAmount != null ? fastRescheduleAmount
				: DEFAULT_FAST_RESCHEDULE_AMOUNT);
	}

	/**
	 * Set the "requeue" after error count maximum.
	 *
	 * @return the maximum count; defaults to
	 *         {@link #DEFAULT_REQUEUE_ERROR_COUNT_MAXIMUM}
	 * @since 1.9
	 */
	public final int getRequeueErrorCountMaximum() {
		return requeueErrorCountMaximum;
	}

	/**
	 * Set the "requeue" after error count maximum.
	 *
	 * @param requeueErrorCountMaximum
	 *        the maximum count to set
	 * @since 1.9
	 */
	public final void setRequeueErrorCountMaximum(int requeueErrorCountMaximum) {
		this.requeueErrorCountMaximum = requeueErrorCountMaximum;
	}

}
