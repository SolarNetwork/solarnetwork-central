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
import static net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents.eventForConfiguration;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Completed;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Executing;
import static net.solarnetwork.central.domain.BasicClaimableJobState.Queued;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.mutable.MutableInt;
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
import net.solarnetwork.central.c2c.domain.CloudDatumStreamRakeTaskEntity;
import net.solarnetwork.central.c2c.domain.CloudDatumStreamSettings;
import net.solarnetwork.central.c2c.domain.CloudIntegrationsUserEvents;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.domain.GeneralObjectDatum;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntity;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.domain.ObjectDatum;
import net.solarnetwork.central.domain.BasicClaimableJobState;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.domain.UserLongCompositePK;
import net.solarnetwork.domain.datum.Datum;
import net.solarnetwork.domain.datum.DatumId;
import net.solarnetwork.domain.datum.DatumSamplesOperations;
import net.solarnetwork.domain.datum.DatumSamplesType;
import net.solarnetwork.domain.datum.ObjectDatumKind;
import net.solarnetwork.domain.datum.ObjectDatumStreamMetadataId;
import net.solarnetwork.service.RemoteServiceException;
import net.solarnetwork.service.ServiceLifecycleObserver;
import net.solarnetwork.util.StringNaturalSortComparator;

/**
 * DAO based implementation of {@link CloudDatumStreamRakeService}.
 *
 * @author matt
 * @version 1.0
 */
public class DaoCloudDatumStreamRakeService
		implements CloudDatumStreamRakeService, ServiceLifecycleObserver, CloudIntegrationsUserEvents {

	/** The {@code shutdownMaxWait} property default value: 1 minute. */
	public static final Duration DEFAULT_SHUTDOWN_MAX_WAIT = Duration.ofMinutes(1);

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
	private final DatumEntityDao datumDao;
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
	 * @param nodeOwnershipDao
	 *        the node ownership DAO
	 * @param taskDao
	 *        the task DAO
	 * @param pollTaskDao
	 *        the poll task DAO
	 * @param datumStreamDao
	 *        the datum stream DAO
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
	public DaoCloudDatumStreamRakeService(InstantSource clock, UserEventAppenderBiz userEventAppenderBiz,
			SolarNodeOwnershipDao nodeOwnershipDao, CloudDatumStreamRakeTaskDao taskDao,
			CloudDatumStreamPollTaskDao pollTaskDao, CloudDatumStreamConfigurationDao datumStreamDao,
			DatumEntityDao datumDao, ExecutorService executor,
			Function<String, CloudDatumStreamService> datumStreamServiceProvider) {
		super();
		this.clock = requireNonNullArgument(clock, "clock");
		this.userEventAppenderBiz = requireNonNullArgument(userEventAppenderBiz, "userEventAppenderBiz");
		this.nodeOwnershipDao = requireNonNullArgument(nodeOwnershipDao, "nodeOwnershipDao");
		this.taskDao = requireNonNullArgument(taskDao, "taskDao");
		this.pollTaskDao = requireNonNullArgument(pollTaskDao, "pollTaskDao");
		this.datumStreamDao = requireNonNullArgument(datumStreamDao, "datumStreamDao");
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
	public CloudDatumStreamRakeTaskEntity claimQueuedTask() {
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
			// go back to queued
			if ( !taskDao.updateTaskState(task.getId(), Queued, task.getState()) ) {
				log.warn("Failed to update rejected datum stream rake task {} state from {} to Queued",
						task.getId().ident(), task.getState());
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
						log.warn("Error executing datum stream {} rake task", taskInfo.getId().ident(),
								e);
					} else {
						// otherwise just print exception message, to cut down on log clutter
						log.warn("Error executing datum stream {} rake task: {}",
								taskInfo.getId().ident(), e.toString());
					}
					var errMsg = "Error executing rake task.";
					var errData = Map.of(MESSAGE_DATA_KEY, (Object) t.getMessage());
					var oldState = taskInfo.getState();
					taskInfo.setMessage(errMsg);
					taskInfo.putServiceProps(errData);
					if ( t instanceof RestClientResponseException || t instanceof IOException ) {
						// reset back to queued to try again if HTTP client or IO error
						log.info(
								"Resetting datum stream {} rake task by changing state from {} to {} after error: {}",
								taskInfo.getId().ident(), oldState, Queued, e.toString());
						taskInfo.setState(Queued);
						if ( taskInfo.getExecuteAt().isBefore(clock.instant()) ) {
							// bump date into future by 1 minute so we do not immediately try to process again
							taskInfo.setExecuteAt(clock.instant().plus(1, ChronoUnit.MINUTES));
						}
					} else {
						// stop processing job if not what appears to be an API IO exception
						log.info(
								"Stopping datum stream {} rake task by changing state from {} to {} after error: {}",
								taskInfo.getId().ident(), oldState, Completed, e.toString());
						taskInfo.setState(Completed);
					}
					userEventAppenderBiz.addEvent(taskInfo.getUserId(), eventForConfiguration(
							taskInfo.getId(), INTEGRATION_RAKE_ERROR_TAGS, errMsg, errData));
					if ( !taskDao.updateTask(taskInfo, oldState) ) {
						log.warn(
								"Unable to update datum stream {} rake task info with expected state {} with details: {}",
								taskInfo.getId().ident(), oldState, taskInfo);
					}
				} catch ( Exception e2 ) {
					log.warn("Error updating datum stream {} rake task state after error",
							taskInfo.getId().ident(), e2);
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

			final String datumStreamIdent = datumStream.getId().ident();

			if ( !datumStream.isFullyConfigured() ) {
				var errMsg = "Datum stream not fully configured.";
				userEventAppenderBiz.addEvent(datumStream.getUserId(),
						eventForConfiguration(datumStream.getId(), INTEGRATION_RAKE_ERROR_TAGS, errMsg));
				taskInfo.setMessage(errMsg);
				taskInfo.setState(Completed); // stop processing job
				userEventAppenderBiz.addEvent(taskInfo.getUserId(),
						eventForConfiguration(taskInfo.getId(), INTEGRATION_RAKE_ERROR_TAGS, errMsg));
				taskDao.updateTask(taskInfo, startState);
				return taskInfo;
			}

			// The time zone of the datum stream.
			ZoneId rakeZone = ZoneOffset.UTC;

			if ( datumStream.getKind() == ObjectDatumKind.Node ) {
				SolarNodeOwnership ownership = nodeOwnershipDao
						.ownershipForNodeId(datumStream.getObjectId());
				if ( ownership == null || !taskInfo.getUserId().equals(ownership.getUserId()) ) {
					log.warn(
							"Refusing to execute datum stream {} rake task because task owner {} does not own node {}",
							datumStreamIdent, taskInfo.getUserId(), datumStream.getObjectId());
					var errMsg = "Access denied to configured node.";
					var errData = Map.of(SOURCE_DATA_KEY, (Object) datumStream.getObjectId());
					taskInfo.setMessage(errMsg);
					taskInfo.putServiceProps(errData);
					taskInfo.setState(Completed); // stop processing job
					userEventAppenderBiz.addEvent(datumStream.getUserId(), eventForConfiguration(
							datumStream.getId(), INTEGRATION_RAKE_ERROR_TAGS, errMsg, errData));
					taskDao.updateTask(taskInfo, startState);
					return taskInfo;
				}
				if ( ownership.getZone() != null ) {
					rakeZone = ownership.getZone();
				}
			}

			// start with a single day range, offset from execute date
			final ZonedDateTime startDate = taskInfo.getExecuteAt().atZone(rakeZone).truncatedTo(DAYS)
					.minus(taskInfo.getOffset());
			final ZonedDateTime endDate = startDate.plusDays(1);

			// verify poll task is after the rake end date, so the two tasks do not overlap
			CloudDatumStreamPollTaskEntity pollTask = pollTaskDao.get(datumStream.getId());
			if ( pollTask != null && pollTask.getStartAt() != null
					&& endDate.isAfter(pollTask.getStartAt().atZone(rakeZone)) ) {
				log.debug(
						"Refusing to execute datum stream {} rake task because end date {} is after stream's poll task start date {}",
						datumStreamIdent, endDate.toInstant(), pollTask.getStartAt());
				var errMsg = "Rake task date is after poll task start.";
				var errData = Map.of("endDate", (Object) endDate.toInstant(), "startDate",
						pollTask.getStartAt());
				taskInfo.setMessage(errMsg);
				taskInfo.putServiceProps(errData);
				taskInfo.setState(Queued);
				userEventAppenderBiz.addEvent(datumStream.getUserId(), eventForConfiguration(
						datumStream.getId(), INTEGRATION_RAKE_ERROR_TAGS, errMsg, errData));
				taskDao.updateTask(taskInfo, startState);
				return taskInfo;
			}

			// save task state to Executing (TODO maybe we don't need this step?)
			if ( !taskDao.updateTaskState(taskInfo.getId(), Executing, startState) ) {
				log.warn("Failed to update rake task {} state to Executing @ {} offset @ {}",
						datumStreamIdent, taskInfo.getExecuteAt(), taskInfo.getOffset());
				var errMsg = "Failed to update task state from Claimed to Executing.";
				var errData = Map.of(SOURCE_DATA_KEY, (Object) datumStreamIdent);
				userEventAppenderBiz.addEvent(datumStream.getUserId(), eventForConfiguration(
						datumStream.getId(), INTEGRATION_RAKE_ERROR_TAGS, errMsg, errData));
				return taskInfo;
			}
			taskInfo.setState(Executing);

			final CloudDatumStreamService datumStreamService = datumStreamServiceProvider
					.apply(datumStream.getServiceIdentifier());
			if ( datumStreamService == null ) {
				// service no longer supported?...
				var errMsg = "Configured Datum Stream service not available.";
				var errData = Map.of(SOURCE_DATA_KEY, (Object) datumStream.getServiceIdentifier());
				taskInfo.setMessage(errMsg);
				taskInfo.putServiceProps(errData);
				taskInfo.setState(Completed); // stop processing job
				userEventAppenderBiz.addEvent(datumStream.getUserId(), eventForConfiguration(
						datumStream.getId(), INTEGRATION_RAKE_ERROR_TAGS, errMsg, errData));
				taskDao.updateTask(taskInfo, Executing);
				return taskInfo;
			}

			final ZonedDateTime maxDate = maxDate(rakeZone, pollTask);

			final Map<ObjectDatumStreamMetadataId, MutableInt> updateCounts = new LinkedHashMap<>();

			ZonedDateTime queryStartDate = startDate;
			ZonedDateTime queryEndDate = startDate.plusDays(1);

			while ( !queryEndDate.isAfter(maxDate) ) {
				final var filter = new BasicQueryFilter();
				filter.setStartDate(queryStartDate.toInstant());
				filter.setEndDate(queryEndDate.toInstant());

				log.debug("Raking for {} datum with filter {}", datumStreamIdent, filter);

				userEventAppenderBiz.addEvent(datumStream.getUserId(),
						eventForConfiguration(datumStream.getId(), INTEGRATION_RAKE_TAGS,
								"Rake for datum",
								Map.of("executeAt", taskInfo.getExecuteAt(), "startAt",
										filter.getStartDate(), "endAt", filter.getEndDate(), "startedAt",
										execTime)));

				int iterationUpdateCount = 0;

				final var rakedDatum = datumStreamService.datum(datumStream, filter);
				if ( rakedDatum != null && !rakedDatum.isEmpty() ) {
					log.debug("Raking for {} found {} datum to verify", datumStreamIdent,
							rakedDatum.size());

					// sort by stream
					SortedMap<DatumId, Datum> datumMapping = rakedDatum.getResults().stream()
							.collect(Collectors.toMap(
									d -> new DatumId(d.getKind(), d.getObjectId(), d.getSourceId(),
											d.getTimestamp()),
									Function.identity(), (l, r) -> l, TreeMap::new));

					ObjectDatumStreamMetadataId currStreamId = null;
					SortedMap<DatumId, Datum> existingDatum = new TreeMap<>();

					for ( var entry : datumMapping.entrySet() ) {
						final DatumId datumId = entry.getKey();
						final Datum datum = entry.getValue();
						// validate that provided datum ID matches that on the configuration
						if ( !datumStream.getObjectId().equals(datum.getObjectId()) ) {
							log.warn(
									"Datum stream {} configured with object ID {} but produced datum with object ID {}: cancelling rake task.",
									datumStreamIdent, taskInfo.getUserId(), datumStream.getObjectId());
							var errMsg = "Access denied to datum with object ID different from datum stream configuration.";
							var errData = Map.of(SOURCE_DATA_KEY, (Object) datum.getObjectId(),
									"expected", datumStream.getObjectId());
							taskInfo.setMessage(errMsg);
							taskInfo.putServiceProps(errData);
							taskInfo.setState(Completed); // stop processing job
							userEventAppenderBiz.addEvent(datumStream.getUserId(), eventForConfiguration(
									datumStream.getId(), INTEGRATION_RAKE_ERROR_TAGS, errMsg, errData));
							taskDao.updateTask(taskInfo, Executing);
							return taskInfo;
						}
						if ( currStreamId == null || !(datum.getKind().equals(currStreamId.getKind())
								&& datum.getObjectId().equals(currStreamId.getObjectId())
								&& datum.getSourceId().equals(currStreamId.getSourceId())) ) {
							// starting new stream
							currStreamId = new ObjectDatumStreamMetadataId(datum.getKind(),
									datum.getObjectId(), datum.getSourceId());

							// query for existing datum
							existingDatum.clear();
							existingDatum.putAll(existingDatum(datumStream.getUserId(), datumId,
									filter.getStartDate(), filter.getEndDate()));
						}

						Datum existing = existingDatum.get(datumId);
						if ( existing == null || differ(datum, existing) ) {
							if ( datum instanceof DatumEntity d ) {
								datumDao.store(d);
							} else if ( datum instanceof GeneralObjectDatum<?> d ) {
								datumDao.persist(d);
							} else {
								datumDao.store(datum);
							}
							iterationUpdateCount++;
							updateCounts.computeIfAbsent(currStreamId, k -> new MutableInt(0))
									.increment();
						}
					}
				}

				// iterate to next day
				queryStartDate = queryEndDate;
				queryEndDate = queryStartDate.plusDays(1);

				if ( iterationUpdateCount < 1 ) {
					// no difference found, so stop
					break;
				}
			}

			// success: update task info to start again tomorrow
			var now = clock.instant();
			taskInfo.setExecuteAt(now.atZone(rakeZone).truncatedTo(DAYS).plusDays(1).toInstant());

			// reset task back to Queued so it can be executed again
			taskInfo.setState(Queued);

			// reset message back to null
			taskInfo.setMessage(null);

			// reset props
			taskInfo.setServiceProps(null);

			// save task state
			if ( !taskDao.updateTask(taskInfo, Executing) ) {
				log.warn("Failed to reset rake task {} @ {} starting @ {}", datumStreamIdent,
						taskInfo.getExecuteAt(), startDate);
				var errMsg = "Failed to reset task state.";
				var errData = Map.of("executeAt", taskInfo.getExecuteAt(), "startAt",
						startDate.toInstant());
				userEventAppenderBiz.addEvent(datumStream.getUserId(), eventForConfiguration(
						datumStream.getId(), INTEGRATION_RAKE_ERROR_TAGS, errMsg, errData));
			} else {
				var msg = "Reset task state";
				var data = new LinkedHashMap<String, Object>(4);
				data.put("executeAt", taskInfo.getExecuteAt());
				data.put("startAt", startDate.toInstant());
				data.put("endAt", queryStartDate); // this has been moved to start of "next" day
				data.put("datumUpdateCount",
						updateCounts.values().stream().mapToInt(n -> n.intValue()).sum());
				if ( !updateCounts.isEmpty() ) {
					Map<String, Integer> sourceCounts = new TreeMap<>(
							StringNaturalSortComparator.CASE_INSENSITIVE_NATURAL_SORT);
					for ( var e : updateCounts.entrySet() ) {
						sourceCounts.put(e.getKey().getSourceId(), e.getValue().toInteger());
					}
					data.put("datumUpdateCountBySource", sourceCounts);
				}
				userEventAppenderBiz.addEvent(datumStream.getUserId(),
						eventForConfiguration(datumStream.getId(), INTEGRATION_RAKE_TAGS, msg, data));
			}
			return taskInfo;
		}

		private ZonedDateTime maxDate(ZoneId rakeZone, CloudDatumStreamPollTaskEntity pollTask) {
			var max = clock.instant().atZone(rakeZone).truncatedTo(DAYS);
			if ( pollTask != null && pollTask.getStartAt() != null
					&& pollTask.getStartAt().isAfter(max.toInstant()) ) {
				max = pollTask.getStartAt().atZone(rakeZone).truncatedTo(DAYS);
			}
			return max;
		}

		private SortedMap<DatumId, ObjectDatum> existingDatum(Long userId, DatumId datumId,
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
				var meta = results.metadataForStreamId(d.getStreamId());
				return ObjectDatum.forStreamDatum(d, userId, new DatumId(meta.getKind(),
						meta.getObjectId(), meta.getSourceId(), d.getTimestamp()), meta);
			}).collect(Collectors.toMap(d -> d.getId(), Function.identity(), (l, r) -> l, TreeMap::new));
		}

	}

	private static final Set<DatumSamplesType> MAP_SAMPLE_TYPES = EnumSet
			.of(DatumSamplesType.Instantaneous, DatumSamplesType.Accumulating, DatumSamplesType.Status);

	private static boolean differ(Datum datum, Datum datum2) {
		DatumSamplesOperations s1 = datum.asSampleOperations();
		DatumSamplesOperations s2 = datum2.asSampleOperations();
		for ( DatumSamplesType propType : MAP_SAMPLE_TYPES ) {
			Map<String, ?> m1 = s1.getSampleData(propType);
			Map<String, ?> m2 = s2.getSampleData(propType);
			if ( m1 != null && m2 != null && m1.size() == m2.size()
					&& m1.keySet().equals(m2.keySet()) ) {
				// compare all props as BigDecimal
				for ( String propName : m1.keySet() ) {
					BigDecimal p1 = s1.getSampleBigDecimal(propType, propName);
					BigDecimal p2 = s2.getSampleBigDecimal(propType, propName);
					if ( p1.compareTo(p2) != 0 ) {
						return true;
					}
				}
			} else if ( m1 != m2 && !((m1 == null && m2 != null && m2.isEmpty())
					|| (m1 != null && m2 == null && m1.isEmpty())) ) {
				return true;
			}
		}
		return false;
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
