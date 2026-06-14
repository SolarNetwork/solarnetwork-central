/* ==================================================================
 * DaoCloudDatumStreamRakeService.java - 21/09/2025 10:57:53 am
 *
 * Copyright 2025 SolarNetwork.net Dev Team
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

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;
import static net.solarnetwork.central.datum.v2.domain.ObjectDatum.forStreamDatum;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Completed;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Executing;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Queued;
import static net.solarnetwork.central.domain.CommonUserEvents.eventForUserRelatedKey;
import static net.solarnetwork.util.CollectionUtils.getMapLong;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientResponseException;
import net.solarnetwork.central.biz.UserEventAppenderBiz;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamRakeService;
import net.solarnetwork.central.c2c.biz.CloudDatumStreamService;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamConfigurationDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamPollTaskDao;
import net.solarnetwork.central.c2c.dao.CloudDatumStreamRakeTaskDao;
import net.solarnetwork.central.c2c.domain.BasicCloudDatumStreamSettings;
import net.solarnetwork.central.c2c.domain.BasicQueryFilter;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamConfiguration;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamPollTaskEntity;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamQueryResult;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamRakeTaskEntity;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamSettings;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.imp.domain.DatumImportUserEvents;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntity;
import net.solarnetwork.central.datum.v2.dao.DatumAuxiliaryEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.dao.DatumStreamMetadataDao;
import net.solarnetwork.central.datum.v2.domain.ObjectDatum;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumAuxiliaryRecord;
import net.solarnetwork.domain.datum.DatumAuxiliaryType;
import net.solarnetwork.domain.datum.DatumIdentity;
import net.solarnetwork.domain.datum.DatumProperties;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadata;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;
import net.solarnetwork.domain.datum.StreamDatum;
import net.solarnetwork.service.RemoteServiceException;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.util.NumberUtils;
import net.solarnetwork.util.StringNaturalSortComparator;
import net.solarnetwork.util.StringUtils;

/**
 * DAO based implementation of {@link CloudDatumStreamRakeService}.
 *
 * @author matt
 * @version 2.1
 */
public class DaoCloudDatumStreamRakeService implements CloudDatumStreamRakeService,
		ServiceLifecycleObserver, CloudIntegrationsUserEvents, DatumImportUserEvents {

	/** The {@code shutdownMaxWait} property default value: 1 minute. */
	public static final Duration DEFAULT_SHUTDOWN_MAX_WAIT = Duration.ofMinutes(1);

	/**
	 * The {@code requeueErrorCountMaximum} property default value.
	 *
	 * @since 1.3
	 */
	public static final int DEFAULT_REQUEUE_ERROR_COUNT_MAXIMUM = 100;

	/** The default datum stream settings value. */
	public static final CloudDatumStreamSettings DEFAULT_DATUM_STREAM_SETTINGS = new BasicCloudDatumStreamSettings(
			true, false);

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final InstantSource clock;
	private final UserEventAppenderBiz userEventAppenderBiz;
	private final SolarNodeOwnershipDao nodeOwnershipDao;
	private final CloudDatumStreamRakeTaskDao taskDao;
	private final CloudDatumStreamPollTaskDao pollTaskDao;
	private final CloudDatumStreamConfigurationDao datumStreamDao;
	private final DatumStreamMetadataDao datumStreamMetadataDao;
	private final DatumEntityDao datumDao;
	private final DatumAuxiliaryEntityDao datumAuxiliaryDao;
	private final ExecutorService executorService;
	private final Function<String, CloudDatumStreamService> datumStreamServiceProvider;
	private int requeueErrorCountMaximum = DEFAULT_REQUEUE_ERROR_COUNT_MAXIMUM;
	private Duration shutdownMaxWait = DEFAULT_SHUTDOWN_MAX_WAIT;

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
	 * @param pollTaskDao
	 *        the poll task DAO
	 * @param datumStreamDao
	 *        the datum stream DAO
	 * @param datumStreamMetadataDao
	 *        the datum stream metadata DAO
	 * @param datumDao
	 *        the datum DAO
	 * @param datumAuxiliaryDao
	 *        the datum auxiliary DAO
	 * @param executor
	 *        the executor; this must be exclusive to this service, as it will
	 *        be shut down when this service is shut down
	 * @param datumStreamServiceProvider
	 *        function that provides a {@link CloudDatumStreamService} for a
	 *        given service identifier
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DaoCloudDatumStreamRakeService(InstantSource clock, UserEventAppenderBiz userEventAppenderBiz,
			SolarNodeOwnershipDao nodeOwnershipDao, CloudDatumStreamRakeTaskDao taskDao,
			CloudDatumStreamPollTaskDao pollTaskDao, CloudDatumStreamConfigurationDao datumStreamDao,
			DatumStreamMetadataDao datumStreamMetadataDao, DatumEntityDao datumDao,
			DatumAuxiliaryEntityDao datumAuxiliaryDao, ExecutorService executor,
			Function<String, CloudDatumStreamService> datumStreamServiceProvider) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		this.userEventAppenderBiz = requireNonNullArgument(userEventAppenderBiz, "userEventAppenderBiz");
		this.nodeOwnershipDao = requireNonNullArgument(nodeOwnershipDao, "nodeOwnershipDao");
		this.taskDao = requireNonNullArgument(taskDao, "taskDao");
		this.pollTaskDao = requireNonNullArgument(pollTaskDao, "pollTaskDao");
		this.datumStreamDao = requireNonNullArgument(datumStreamDao, "datumStreamDao");
		this.datumStreamMetadataDao = requireNonNullArgument(datumStreamMetadataDao,
				"datumStreamMetadataDao");
		this.datumDao = requireNonNullArgument(datumDao, "datumDao");
		this.datumAuxiliaryDao = requireNonNullArgument(datumAuxiliaryDao, "datumAuxiliaryDao");
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
				log.info("Waiting at most {}s for datum stream rake tasks to complete...",
						shutdownMaxWait.getSeconds());
				boolean success = executorService.awaitTermination(shutdownMaxWait.getSeconds(),
						TimeUnit.SECONDS);
				if ( success ) {
					log.info("All datum stream rake tasks finished.");
				} else {
					log.warn("Timeout waiting {}s for datum stream rake tasks to complete.",
							shutdownMaxWait.getSeconds());
				}
			}
		} catch ( Exception e ) {
			log.warn("Error shutting down datum stream rake task service: {}", e.getMessage(), e);
		}
	}

	@Override
	public @Nullable CloudDatumStreamRakeTaskEntity claimQueuedTask() {
		if ( executorService.isShutdown() ) {
			return null;
		}
		return taskDao.claimQueuedTask();
	}

	@Override
	public Future<CloudDatumStreamRakeTaskEntity> executeTask(CloudDatumStreamRakeTaskEntity task) {
		try {
			return executorService.submit(new CloudDatumStreamRakeTask(task));
		} catch ( RejectedExecutionException e ) {
			log.debug("Datum stream rake task execution rejected, resetting state to Queued: {}",
					e.getMessage());
			// go back to queued and reset execute_at back
			var taskCopy = task.clone();
			taskCopy.setState(Queued);
			if ( !taskDao.updateTask(taskCopy, task.getState()) ) {
				log.warn("Failed to update rejected datum stream rake task {} state from {} to Queued",
						task.id().ident(), task.getState());
			}
			throw e;
		}
	}

	@Override
	public int resetAbandondedExecutingTasks(Instant olderThan) {
		return taskDao.resetAbandondedExecutingTasks(olderThan);
	}

	private final class CloudDatumStreamRakeTask implements Callable<CloudDatumStreamRakeTaskEntity> {

		private final CloudDatumStreamRakeTaskEntity taskInfo;
		private final BasicClaimableJobState startState;
		private @Nullable Set<String> resolvedSourceIds;
		private @Nullable Map<String, UUID> sourceToStreamIds;

		private CloudDatumStreamRakeTask(CloudDatumStreamRakeTaskEntity taskInfo) {
			super();
			this.taskInfo = requireNonNullArgument(taskInfo, "taskInfo").clone();
			this.startState = requireNonNullArgument(taskInfo.getState(), "taskInfo.state");
		}

		@Override
		public CloudDatumStreamRakeTaskEntity call() throws Exception {
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
						log.warn("Error executing datum stream {} rake task {}",
								taskInfo.getDatumStreamId(), taskInfo.id().ident(), e);
					} else {
						// otherwise just print exception message, to cut down on log clutter
						log.warn("Error executing datum stream {} rake task {}: {}",
								taskInfo.getDatumStreamId(), taskInfo.id().ident(), e.toString());
					}
					var prevErrorCount = getMapLong(ERROR_COUNT_DATA_KEY,
							taskInfo.getServiceProperties());
					long errorCount = prevErrorCount != null ? prevErrorCount + 1L : 0L;
					var errMsg = "Error executing rake task.";
					var exMsg = (e instanceof RemoteServiceException ? e : t).getMessage();
					Map<String, Object> errData = Map.of(CONFIG_SUB_ID_DATA_KEY, taskInfo.getConfigId(),
							MESSAGE_DATA_KEY, Objects.requireNonNullElse(exMsg, ""),
							ERROR_COUNT_DATA_KEY, errorCount);
					var oldState = taskInfo.getState();
					taskInfo.setMessage(errMsg);
					taskInfo.putServiceProps(errData);
					if ( t instanceof RestClientResponseException || t instanceof IOException ) {
						if ( errorCount < requeueErrorCountMaximum ) {
							// reset back to queued to try again if HTTP client or IO error
							log.info(
									"Resetting datum stream {} rake task {} by changing state from {} to {} after error: {}",
									taskInfo.getDatumStreamId(), taskInfo.id().ident(), oldState, Queued,
									e.toString());
							taskInfo.setState(Queued);
							if ( taskInfo.getExecuteAt().isBefore(clock.instant()) ) {
								// bump date into future by 1 minute so we do not immediately try to process again
								taskInfo.setExecuteAt(clock.instant().truncatedTo(ChronoUnit.SECONDS)
										.plus(1, ChronoUnit.MINUTES));
							}
						} else {
							log.info(
									"Stopping datum stream {} rake task {} by changing state from {} to {} after {} repeated errors, most recently: {}",
									taskInfo.getDatumStreamId(), taskInfo.id().ident(), oldState,
									Completed, errorCount, e.toString());
							taskInfo.setState(Completed);
						}
					} else {
						// stop processing job if not what appears to be an API IO exception
						log.info(
								"Stopping datum stream {} rake task {} by changing state from {} to {} after error: {}",
								taskInfo.getDatumStreamId(), taskInfo.id().ident(), oldState, Completed,
								e.toString());
						taskInfo.setState(Completed);
					}
					userEventAppenderBiz.addEvent(taskInfo.getUserId(), eventForUserRelatedKey(
							taskInfo.id(), INTEGRATION_RAKE_ERROR_TAGS, errMsg, errData));
					if ( !taskDao.updateTask(taskInfo, oldState) ) {
						log.warn(
								"Unable to update datum stream {} rake task {} info with expected state {} with details: {}",
								taskInfo.getDatumStreamId(), taskInfo.id().ident(), oldState, taskInfo);
					}
				} catch ( Exception e2 ) {
					log.warn("Error updating datum stream {} rake task {} state after error",
							taskInfo.getDatumStreamId(), taskInfo.id().ident(), e2);
					// ignore, return original
				}
				throw e;
			}
		}

		private CloudDatumStreamRakeTaskEntity executeTask() throws Exception {
			final Instant execTime = clock.instant();

			final UserLongCompositePK datumStreamId = new UserLongCompositePK(taskInfo.getUserId(),
					taskInfo.getDatumStreamId());
			final CloudDatumStreamConfiguration datumStream = datumStreamDao.get(datumStreamId);
			if ( datumStream == null ) {
				// configuration has been deleted... abort
				return taskInfo;
			}

			final String taskIdent = taskInfo.id().ident();
			final String datumStreamIdent = datumStream.id().ident();

			if ( !datumStream.isFullyConfigured() ) {
				var errMsg = "Datum stream not fully configured.";
				userEventAppenderBiz.addEvent(datumStream.getUserId(),
						eventForUserRelatedKey(datumStream.id(), INTEGRATION_RAKE_ERROR_TAGS, errMsg));
				taskInfo.setMessage(errMsg);
				taskInfo.setState(Completed); // stop processing job
				taskDao.updateTask(taskInfo, startState);
				return taskInfo;
			}

			// get nonnull refs, after call to isFullyConfigured()
			final Long objectId = nonnull(datumStream.getObjectId(), "Object ID");
			final ObjectDatumKind kind = nonnull(datumStream.getKind(), "Kind");

			// The time zone of the datum stream.
			ZoneId rakeZone = ZoneOffset.UTC;

			if ( datumStream.getKind() == ObjectDatumKind.Node ) {
				SolarNodeOwnership ownership = nodeOwnershipDao.ownershipForNodeId(objectId);
				if ( ownership == null || !taskInfo.getUserId().equals(ownership.getUserId()) ) {
					log.warn(
							"Refusing to execute datum stream {} rake task {} because task owner {} does not own node {}",
							taskInfo.getDatumStreamId(), taskIdent, taskInfo.getUserId(), objectId);
					var errMsg = "Access denied to configured node.";
					Map<String, Object> errData = Map.of(CONFIG_SUB_ID_DATA_KEY, taskInfo.getConfigId(),
							SOURCE_DATA_KEY, objectId);
					taskInfo.setMessage(errMsg);
					taskInfo.putServiceProps(errData);
					taskInfo.setState(Completed); // stop processing job
					userEventAppenderBiz.addEvent(datumStream.getUserId(), eventForUserRelatedKey(
							datumStream.id(), INTEGRATION_RAKE_ERROR_TAGS, errMsg, errData));
					taskDao.updateTask(taskInfo, startState);
					return taskInfo;
				}
				rakeZone = ownership.getZone();
			}

			// start with a single day range, offset from execute date
			final ZonedDateTime startDate = taskInfo.getExecuteAt().atZone(rakeZone).truncatedTo(DAYS)
					.minus(taskInfo.getOffset());
			final ZonedDateTime endDate = startDate.plusDays(1);

			// verify poll task is after the rake end date, so the two tasks do not overlap
			CloudDatumStreamPollTaskEntity pollTask = pollTaskDao.get(datumStream.id());
			if ( pollTask != null && pollTask.getStartAt() != null
					&& endDate.isAfter(pollTask.getStartAt().atZone(rakeZone)) ) {
				log.debug(
						"Refusing to execute datum stream {} rake task {} because end date {} is after stream's poll task start date {}",
						taskInfo.getDatumStreamId(), taskIdent, endDate.toInstant(),
						pollTask.getStartAt());
				var errMsg = "Rake task date is after poll task start.";
				var errData = Map.of(CONFIG_SUB_ID_DATA_KEY, taskInfo.getConfigId(), "endDate",
						(Object) endDate.toInstant(), "startDate", pollTask.getStartAt());
				taskInfo.setExecuteAt(
						clock.instant().atZone(rakeZone).truncatedTo(DAYS).plusDays(1).toInstant());
				taskInfo.setMessage(errMsg);
				taskInfo.putServiceProps(errData);
				taskInfo.setState(Queued);
				taskDao.updateTask(taskInfo, startState);
				return taskInfo;
			}

			final CloudDatumStreamService datumStreamService = datumStreamServiceProvider
					.apply(datumStream.getServiceIdentifier());
			if ( datumStreamService == null ) {
				// service no longer supported?...
				var errMsg = "Configured Datum Stream service not available.";
				Map<String, Object> errData = Map.of(CONFIG_SUB_ID_DATA_KEY, taskInfo.getConfigId(),
						SOURCE_DATA_KEY, datumStream.getServiceIdentifier());
				taskInfo.setMessage(errMsg);
				taskInfo.putServiceProps(errData);
				taskInfo.setState(Completed); // stop processing job
				userEventAppenderBiz.addEvent(datumStream.getUserId(), eventForUserRelatedKey(
						datumStream.getId(), INTEGRATION_RAKE_ERROR_TAGS, errMsg, errData));
				taskDao.updateTask(taskInfo, startState);
				return taskInfo;
			}

			userEventAppenderBiz.addEvent(datumStream.getUserId(),
					eventForUserRelatedKey(datumStream.getId(), INTEGRATION_RAKE_TAGS, "Rake for datum",
							Map.of(CONFIG_SUB_ID_DATA_KEY, taskInfo.getConfigId(), EXECUTE_AT_DATA_KEY,
									taskInfo.getExecuteAt(), DATE_OFFSET_DATA_KEY,
									taskInfo.getOffset().toString(), STARTED_AT_DATA_KEY, execTime)));

			// save task state to Executing
			if ( !taskDao.updateTaskState(taskInfo.id(), Executing, startState) ) {
				log.warn(
						"Failed to update datum stream {} rake task {} state from {} to Executing @ {} offset @ {}",
						taskInfo.getDatumStreamId(), taskIdent, startState, taskInfo.getExecuteAt(),
						taskInfo.getOffset());
				var errMsg = "Failed to update task state from %s to Executing.".formatted(startState);
				var errData = Map.of(CONFIG_SUB_ID_DATA_KEY, taskInfo.getConfigId(), SOURCE_DATA_KEY,
						(Object) datumStreamIdent);
				userEventAppenderBiz.addEvent(datumStream.getUserId(), eventForUserRelatedKey(
						datumStream.getId(), INTEGRATION_RAKE_ERROR_TAGS, errMsg, errData));
				return taskInfo;
			}
			taskInfo.setState(Executing);

			final ZonedDateTime maxDate = maxDate(rakeZone, pollTask);

			final Map<ObjectDatumStreamMetadataId, MutableInt> updateCounts = new LinkedHashMap<>();

			ZonedDateTime queryStartDate = startDate;
			ZonedDateTime queryEndDate = startDate.plusDays(1);
			if ( queryEndDate.isAfter(maxDate) ) {
				queryEndDate = maxDate;
			}

			while ( queryStartDate.isBefore(maxDate) && !queryEndDate.isAfter(maxDate) ) {
				final Instant filterStartDate = queryStartDate.toInstant();
				final Instant filterEndDate = queryEndDate.toInstant();
				final var filter = new BasicQueryFilter();
				filter.setStartDate(filterStartDate);
				filter.setEndDate(filterEndDate);

				final var queryTimestamp = clock.instant();

				log.debug("Raking for {} datum with filter {}", datumStreamIdent, filter);

				int iterationUpdateCount = 0;

				final CloudDatumStreamQueryResult rakedDatum = datumStreamService.datum(datumStream,
						filter);
				if ( rakedDatum != null && !rakedDatum.isEmpty() ) {
					log.debug("Raking for {} found {} datum to verify", datumStreamIdent,
							rakedDatum.size());

					// sort by stream
					SortedMap<DatumIdentity, Datum> datumMapping = rakedDatum.getResults().stream()
							.collect(toMap(d -> d.datumIdent(), Function.identity(), (l, _) -> l,
									TreeMap::new));

					// cache datum stream  metadata to speed up conversion to StreamDatum
					final Map<ObjectDatumStreamMetadataId, ObjectDatumStreamMetadata> streamMetaCache = new HashMap<>(
							8);

					ObjectDatumStreamMetadataId currStreamId = null;
					SortedMap<DatumIdentity, Datum> existingDatum = new TreeMap<>();

					for ( var entry : datumMapping.entrySet() ) {
						final DatumIdentity datumId = entry.getKey();
						final Datum datum = entry.getValue();
						// validate that provided datum ID matches that on the configuration
						if ( datum.getObjectId() == null || !objectId.equals(datum.getObjectId()) ) {
							log.warn(
									"Datum stream {} configured with object ID {} but produced datum with object ID {}: cancelling rake task.",
									datumStreamIdent, objectId, datum.getObjectId());
							var errMsg = "Access denied to datum with object ID different from datum stream configuration.";
							Map<String, Object> errData = Map.of(CONFIG_SUB_ID_DATA_KEY,
									taskInfo.getConfigId(), SOURCE_DATA_KEY, datum.getObjectId(),
									"expected", objectId);
							taskInfo.setMessage(errMsg);
							taskInfo.putServiceProps(errData);
							taskInfo.setState(Completed); // stop processing job
							userEventAppenderBiz.addEvent(datumStream.getUserId(),
									eventForUserRelatedKey(datumStream.getId(),
											INTEGRATION_RAKE_ERROR_TAGS, errMsg, errData));
							taskDao.updateTask(taskInfo, Executing);
							return taskInfo;
						}
						final ObjectDatumKind datumKind = (datum.getKind() != null ? datum.getKind()
								: kind);
						if ( !kind.equals(datumKind) ) {
							log.warn(
									"Datum stream {} configured with kind {} but produced datum with kind {}: cancelling rake task.",
									datumStreamIdent, kind, datumKind);
							var errMsg = "Access denied to datum with kind different from datum stream configuration.";
							Map<String, Object> errData = Map.of(CONFIG_SUB_ID_DATA_KEY,
									taskInfo.getConfigId(), SOURCE_DATA_KEY, datumKind, "expected",
									kind);
							taskInfo.setMessage(errMsg);
							taskInfo.putServiceProps(errData);
							taskInfo.setState(Completed); // stop processing job
							userEventAppenderBiz.addEvent(datumStream.getUserId(),
									eventForUserRelatedKey(datumStream.getId(),
											INTEGRATION_RAKE_ERROR_TAGS, errMsg, errData));
							taskDao.updateTask(taskInfo, Executing);
							return taskInfo;
						}
						if ( datum.getSourceId() == null ) {
							continue;
						}
						if ( currStreamId == null
								|| !currStreamId.getSourceId().equals(datum.getSourceId()) ) {
							// starting new stream
							currStreamId = new ObjectDatumStreamMetadataId(kind, datum.getObjectId(),
									datum.getSourceId());

							// query for existing datum
							existingDatum.clear();
							existingDatum.putAll(existingDatum(datumStream.getUserId(), datumId,
									filterStartDate, filterEndDate));
						}

						Datum existing = existingDatum.get(datumId);
						if ( existing == null || differ(datum, existing) ) {
							if ( datum instanceof StreamDatum d ) {
								datumDao.store(d);
							} else {
								final StreamDatum sDatum = datumStreamDatum(datumId, datum,
										queryTimestamp, streamMetaCache);
								if ( sDatum != null ) {
									datumDao.store(sDatum);
								} else {
									datumDao.store(datum);
								}
							}
							iterationUpdateCount++;
							updateCounts.computeIfAbsent(currStreamId, _ -> new MutableInt(0))
									.increment();
						}
					}

					// if we made any updates, re-import auxiliary
					if ( iterationUpdateCount > 0 ) {
						maintainAuxiliaryRecords(datumStream, datumStreamService, filterStartDate,
								filterEndDate, rakedDatum);
					}
				}

				userEventAppenderBiz.addEvent(datumStream.getUserId(), eventForUserRelatedKey(
						datumStream.getId(), INTEGRATION_RAKE_PROGRESS_TAGS, null,
						progressEventData(execTime, filterStartDate, filterEndDate, updateCounts)));

				// iterate to next day
				queryStartDate = queryEndDate;
				queryEndDate = queryStartDate.plusDays(1);
				if ( queryEndDate.isAfter(maxDate) ) {
					queryEndDate = maxDate;
				}

				if ( iterationUpdateCount < 1 ) {
					// no difference found, so stop
					break;
				}
			}

			final var datumUpdateCount = datumUpdateCount(updateCounts);

			// success: update task info to start again tomorrow
			final var now = clock.instant();
			taskInfo.setExecuteAt(now.atZone(rakeZone).truncatedTo(DAYS).plusDays(1).toInstant());

			// reset task back to Queued so it can be executed again
			taskInfo.setState(Queued);

			// update message
			taskInfo.setMessage(
					datumUpdateCount > 0 ? "Updated %d datum.".formatted(datumUpdateCount) : null);

			// reset props
			taskInfo.setServiceProps(null);

			// save task state
			if ( !taskDao.updateTask(taskInfo, Executing) ) {
				log.warn("Failed to reset datum stream {} rake task {} @ {} starting @ {}",
						taskInfo.getDatumStreamId(), taskIdent, taskInfo.getExecuteAt(), startDate);
				var errMsg = "Failed to reset task state.";
				Map<String, Object> errData = Map.of(CONFIG_SUB_ID_DATA_KEY, taskInfo.getConfigId(),
						EXECUTE_AT_DATA_KEY, taskInfo.getExecuteAt(), START_AT_DATA_KEY,
						startDate.toInstant());
				userEventAppenderBiz.addEvent(datumStream.getUserId(), eventForUserRelatedKey(
						datumStream.getId(), INTEGRATION_RAKE_ERROR_TAGS, errMsg, errData));
			} else {
				var msg = "Reset task state";
				userEventAppenderBiz.addEvent(datumStream.getUserId(),
						eventForUserRelatedKey(datumStream.getId(), INTEGRATION_RAKE_TAGS, msg,
								progressEventData(execTime, startDate.toInstant(),
										queryStartDate.toInstant(), updateCounts)));
			}
			return taskInfo;
		}

		private long datumUpdateCount(Map<ObjectDatumStreamMetadataId, MutableInt> updateCounts) {
			return updateCounts.values().stream().mapToLong(n -> n.longValue()).sum();
		}

		private Map<String, Integer> datumUpdateCountBySource(
				Map<ObjectDatumStreamMetadataId, MutableInt> updateCounts) {
			Map<String, Integer> sourceCounts = new TreeMap<>(
					StringNaturalSortComparator.CASE_INSENSITIVE_NATURAL_SORT);
			for ( var e : updateCounts.entrySet() ) {
				sourceCounts.put(e.getKey().getSourceId(), e.getValue().toInteger());
			}
			return sourceCounts;
		}

		private Map<String, Object> progressEventData(Instant execTime, Instant startDate,
				Instant endDate, Map<ObjectDatumStreamMetadataId, MutableInt> updateCounts) {
			var data = new LinkedHashMap<String, Object>(4);
			data.put(CONFIG_SUB_ID_DATA_KEY, taskInfo.getConfigId());
			data.put(EXECUTE_AT_DATA_KEY, taskInfo.getExecuteAt());
			data.put(STARTED_AT_DATA_KEY, execTime);
			data.put(START_AT_DATA_KEY, startDate);
			data.put(END_AT_DATA_KEY, endDate);
			data.put(DATUM_COUNT_DATA_KEY, datumUpdateCount(updateCounts));
			if ( !updateCounts.isEmpty() ) {
				data.put(DATUM_COUNT_BY_SOURCE_DATA_KEY, datumUpdateCountBySource(updateCounts));
			}
			return data;
		}

		private ZonedDateTime maxDate(ZoneId rakeZone,
				@Nullable CloudDatumStreamPollTaskEntity pollTask) {
			var max = clock.instant().atZone(rakeZone).truncatedTo(DAYS);
			if ( pollTask != null && pollTask.getStartAt() != null
					&& pollTask.getStartAt().isAfter(max.toInstant()) ) {
				max = pollTask.getStartAt().atZone(rakeZone).truncatedTo(DAYS);
			}
			return max;
		}

		private SortedMap<DatumIdentity, ObjectDatum> existingDatum(Long userId, DatumIdentity datumId,
				Instant startDate, Instant endDate) {
			// query for existing datum
			var datumFilter = new BasicDatumCriteria();
			datumFilter.setObjectKind(datumId.getKind());
			if ( datumId.getKind() == ObjectDatumKind.Location ) {
				datumFilter.setLocationId(datumId.getObjectId());
			} else {
				datumFilter.setNodeId(datumId.getObjectId());
			}
			datumFilter.setSourceId(datumId.getSourceId());
			datumFilter.setStartDate(startDate);
			datumFilter.setEndDate(endDate);
			var results = datumDao.findFiltered(datumFilter);
			return StreamSupport.stream(results.spliterator(), false).map(d -> {
				var meta = nonnull(results.metadataForStreamId(d.getStreamId()), "Stream metadata");
				return nonnull(forStreamDatum(d, userId, meta.datumIdent(d.getTimestamp()), meta),
						"Stream datum");
			}).collect(Collectors.toMap(d -> d.datumIdent(), Function.identity(), (l, _) -> l,
					TreeMap::new));
		}

		private void maintainAuxiliaryRecords(CloudDatumStreamConfiguration datumStream,
				CloudDatumStreamService datumStreamService, Instant queryStartDate, Instant queryEndDate,
				CloudDatumStreamQueryResult rakedDatum) {
			if ( datumStream.getKind() != ObjectDatumKind.Node ) {
				return;
			}
			final Set<String> sourceIds = resolveSourceIds(datumStream, datumStreamService);
			if ( sourceIds.isEmpty() ) {
				return;
			}

			// clear out any existing generated auxiliary for query date range
			var auxFilter = new BasicDatumCriteria();
			auxFilter.setDatumAuxiliaryType(DatumAuxiliaryType.Mark);
			auxFilter.setObjectKind(datumStream.getKind());
			auxFilter.setNodeId(datumStream.getObjectId());
			auxFilter.setSourceIds(sourceIds.toArray(String[]::new));
			auxFilter.setStartDate(queryStartDate);
			auxFilter.setEndDate(queryEndDate);
			auxFilter.setSearchFilter(CloudDatumStreamService.GENERATED_AUXILIARY_SEARCH_FILTER);
			long deleteCount = datumAuxiliaryDao.deleteFiltered(auxFilter);
			if ( deleteCount > 0 ) {
				userEventAppenderBiz.addEvent(datumStream.getUserId(), eventForUserRelatedKey(
						datumStream.getId(), DATUM_IMPORT_TAGS,
						"Deleted %d generated Mark datum auxiliary records.".formatted(deleteCount),
						// @formatter:off
									Map.of(START_AT_DATA_KEY, auxFilter.getStartDate()
										, END_AT_DATA_KEY, auxFilter.getEndDate()
										, NODE_ID_DATA_KEY, auxFilter.getNodeId()
										, SOURCE_ID_DATA_KEY, auxFilter.getSourceIds()
									// @formatter:on
						)));
			}

			// save any auxiliary records returned
			final SequencedCollection<DatumAuxiliaryRecord> auxiliary = rakedDatum.getAuxiliary();
			if ( auxiliary != null ) {
				final Map<String, UUID> sourceToStreamIds = sourceToStreamIds(datumStream, sourceIds);
				for ( DatumAuxiliaryRecord aux : auxiliary ) {
					final UUID streamId = sourceToStreamIds.get(aux.getSourceId());
					if ( streamId != null ) {
						final var entity = new DatumAuxiliaryEntity(streamId, aux.getTimestamp(),
								aux.getType(), Instant.now(), aux.getSamplesFinal(),
								aux.getSamplesStart(), aux.getNotes(), aux.getMetadata());
						datumAuxiliaryDao.save(entity);
					}
				}
			}
		}

		private Set<String> resolveSourceIds(CloudDatumStreamConfiguration datumStream,
				CloudDatumStreamService service) {
			if ( resolvedSourceIds != null ) {
				return resolvedSourceIds;
			}
			Set<String> resolvedSourceIds = service.datumStreamSourceIds(datumStream);
			this.resolvedSourceIds = (resolvedSourceIds != null ? resolvedSourceIds : Set.of());
			return this.resolvedSourceIds;
		}

		private Map<String, UUID> sourceToStreamIds(CloudDatumStreamConfiguration datumStream,
				Set<String> sourceIds) {
			if ( sourceToStreamIds != null || sourceIds.isEmpty() ) {
				return (sourceToStreamIds != null ? sourceToStreamIds : Map.of());
			}

			// copy aux filter for node/source IDs but clear out other criteria
			var filter = new BasicDatumCriteria();
			filter.setObjectKind(datumStream.getKind());
			filter.setNodeId(datumStream.getObjectId());
			filter.setSourceIds(sourceIds.toArray(String[]::new));

			sourceToStreamIds = stream(datumStreamMetadataDao.findDatumStreamMetadataIds(filter)
					.spliterator(), false).collect(Collectors.toMap(
							net.solarnetwork.central.domain.ObjectDatumStreamMetadataId::getSourceId,
							net.solarnetwork.central.domain.ObjectDatumStreamMetadataId::getStreamId,
							(_, r) -> r));
			return sourceToStreamIds;
		}

	}

	private static boolean differ(Datum datum, Datum datum2) {
		DatumSamplesOperations s1 = datum.asSampleOperations();
		DatumSamplesOperations s2 = datum2.asSampleOperations();
		return s1.differsNumericallyFrom(s2, StringUtils::numberValue, NumberUtils::bigDecimalForNumber);
	}

	private @Nullable StreamDatum datumStreamDatum(DatumIdentity datumId, Datum datum, Instant received,
			Map<ObjectDatumStreamMetadataId, ObjectDatumStreamMetadata> cache) {
		final ObjectDatumStreamMetadata meta = datumStreamMetadata(datumId, cache);
		if ( meta != null ) {
			try {
				var datumProps = DatumProperties.propertiesFrom(datum, meta);
				if ( datumProps != null ) {
					return new DatumEntity(meta.getStreamId(), datumId.getTimestamp(), received,
							datumProps);
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
			f.setObjectKind(datumId.getKind());
			if ( datumId.getKind() == ObjectDatumKind.Location ) {
				f.setLocationId(datumId.getObjectId());
			} else {
				f.setNodeId(datumId.getObjectId());
			}
			f.setSourceId(datumId.getSourceId());
			meta = datumStreamMetadataDao.findStreamMetadata(f);
			if ( meta != null ) {
				cache.put(metaId, meta);
			}
		}
		return meta;
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
	 * Set the "requeue" after error count maximum.
	 *
	 * @return the maximum count; defaults to
	 *         {@link #DEFAULT_REQUEUE_ERROR_COUNT_MAXIMUM}
	 * @since 1.3
	 */
	public final int getRequeueErrorCountMaximum() {
		return requeueErrorCountMaximum;
	}

	/**
	 * Set the "requeue" after error count maximum.
	 *
	 * @param requeueErrorCountMaximum
	 *        the maximum count to set
	 * @since 1.3
	 */
	public final void setRequeueErrorCountMaximum(int requeueErrorCountMaximum) {
		this.requeueErrorCountMaximum = requeueErrorCountMaximum;
	}

}
