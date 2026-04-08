/* ==================================================================
 * DaoUserDatumDeleteBiz.java - 24/11/2018 9:55:53 AM
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

package net.solarnetwork.central.user.datum.expire.biz.dao;

import static java.time.Instant.now;
import static java.util.Arrays.stream;
import static net.solarnetwork.central.datum.v2.support.DatumUtils.criteriaFromFilter;
import static net.solarnetwork.util.ObjectUtils.nonnull;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.dao.UserUuidPK;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumRecordCounts;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumMaintenanceDao;
import net.solarnetwork.central.datum.v2.domain.ObjectDatumId;
import net.solarnetwork.central.datum.v2.support.DatumUtils;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.datum.expire.biz.UserDatumDeleteBiz;
import net.solarnetwork.central.user.datum.expire.biz.UserDatumDeleteJobBiz;
import net.solarnetwork.central.user.datum.expire.dao.UserDatumDeleteJobInfoDao;
import net.solarnetwork.central.user.datum.expire.domain.DatumDeleteJobInfo;
import net.solarnetwork.central.user.datum.expire.domain.DatumDeleteJobState;
import net.solarnetwork.central.user.datum.expire.domain.DatumDeleteJobStatus;
import net.solarnetwork.event.AppEventPublisher;

/**
 * DAO implementation of {@link UserDatumDeleteBiz}.
 *
 * @author matt
 * @version 1.3
 */
public class DaoUserDatumDeleteBiz implements UserDatumDeleteBiz, UserDatumDeleteJobBiz {

	/** The default batch delete duration: 7 days. */
	public static final Duration DEFAULT_DELETE_BATCH_DURATION = Duration.ofDays(7);

	/** The minimum batch delete duration: 1 hour. */
	public static final Duration MIN_BATCH_DURATION = Duration.ofHours(1);

	/**
	 * The {@code deleteDatumByIdMaxCount} property default value.
	 * 
	 * @since 1.2
	 */
	public static final int DEFAULT_DELETE_DATUM_BY_ID_MAX_COUNT = 100;

	private final UserNodeDao userNodeDao;
	private final DatumMaintenanceDao datumDao;
	private final UserDatumDeleteJobInfoDao jobInfoDao;
	private final AsyncTaskExecutor executor;

	private final ConcurrentMap<UserUuidPK, DatumDeleteTask> taskMap = new ConcurrentHashMap<>(16, 0.9f,
			1);

	private @Nullable TaskScheduler scheduler;
	private long completedTaskMinimumCacheTime = TimeUnit.HOURS.toMillis(4);
	private @Nullable AppEventPublisher eventPublisher;
	private @Nullable Duration deleteBatchDuration = DEFAULT_DELETE_BATCH_DURATION;
	private int deleteDatumByIdMaxCount = DEFAULT_DELETE_DATUM_BY_ID_MAX_COUNT;

	private @Nullable ScheduledFuture<?> taskPurgerTask = null;

	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * Constructor.
	 *
	 * @param executor
	 *        the executor service to use
	 * @param userNodeDao
	 *        the user node DAO to use
	 * @param datumDao
	 *        the datum DAO to use
	 * @param jobInfoDao
	 *        the job DAO to use
	 * @throws IllegalArgumentException
	 *         if any argument is {@code null}
	 */
	public DaoUserDatumDeleteBiz(AsyncTaskExecutor executor, UserNodeDao userNodeDao,
			DatumMaintenanceDao datumDao, UserDatumDeleteJobInfoDao jobInfoDao) {
		super();
		this.executor = requireNonNullArgument(executor, "executor");
		this.userNodeDao = requireNonNullArgument(userNodeDao, "userNodeDao");
		this.datumDao = requireNonNullArgument(datumDao, "datumDao");
		this.jobInfoDao = requireNonNullArgument(jobInfoDao, "jobInfoDao");
	}

	/**
	 * Initialize after properties configured.
	 *
	 * <p>
	 * Call this method once all properties have been configured on the
	 * instance.
	 * </p>
	 */
	public synchronized void init() {
		if ( taskPurgerTask != null ) {
			return;
		}
		// purge completed tasks every hour
		if ( scheduler != null ) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			ConcurrentMap<UserUuidPK, DatumDeleteJobStatus> map = (ConcurrentMap) taskMap;
			taskPurgerTask = scheduler.scheduleWithFixedDelay(
					new DatumDeleteTaskPurger(completedTaskMinimumCacheTime, map),
					Instant.now().plus(1, ChronoUnit.HOURS), Duration.ofHours(1));
		}
	}

	/**
	 * Shutdown after the service is no longer needed.
	 */
	public synchronized void shutdown() {
		if ( taskPurgerTask != null ) {
			taskPurgerTask.cancel(true);
		}
	}

	private GeneralNodeDatumFilter prepareFilter(GeneralNodeDatumFilter filter) {
		if ( filter == null ) {
			throw new IllegalArgumentException("GeneralNodeDatumFilter is required.");
		}
		if ( filter.getUserId() == null ) {
			throw new AuthorizationException(Reason.ANONYMOUS_ACCESS_DENIED, null);
		}
		if ( filter.getLocalStartDate() == null || filter.getLocalEndDate() == null ) {
			throw new IllegalArgumentException("A local date range is required.");
		}
		DatumFilterCommand f = null;
		if ( filter.getNodeId() == null ) {
			f = new DatumFilterCommand(filter);
			Set<Long> nodes = userNodeDao.findNodeIdsForUser(filter.getUserId());
			f.setNodeIds(nodes.toArray(Long[]::new));
			filter = f;
		}
		if ( filter.getSourceIds() != null && filter.getSourceIds().length < 1 ) {
			if ( f == null ) {
				f = new DatumFilterCommand(filter);
				filter = f;
			}
			f.setSourceIds(null);
		}
		return filter;
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public DatumRecordCounts countDatumRecords(GeneralNodeDatumFilter filter) {
		filter = prepareFilter(filter);
		if ( filter.getNodeId() == null ) {
			var counts = new DatumRecordCounts();
			counts.setDate(now());
			return counts;
		}
		BasicDatumCriteria c = nonnull(criteriaFromFilter(filter), "Criteria");
		var result = DatumUtils.toRecordCounts(datumDao.countDatumRecords(c));
		if ( result != null ) {
			return result;
		}
		result = new DatumRecordCounts();
		result.setDate(now());
		return result;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public long purgeOldJobs(Instant olderThanDate) {
		return jobInfoDao.purgeOldJobs(olderThanDate);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public @Nullable DatumDeleteJobInfo datumDeleteJobForUser(Long userId, String jobId) {
		return jobInfoDao.get(new UserUuidPK(userId, UUID.fromString(jobId)));
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Collection<DatumDeleteJobInfo> datumDeleteJobsForUser(Long userId,
			@Nullable Set<DatumDeleteJobState> states) {
		return jobInfoDao.findForUser(userId, states);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public DatumDeleteJobInfo submitDatumDeleteRequest(GeneralNodeDatumFilter request) {
		GeneralNodeDatumFilter f = prepareFilter(request);

		UUID jobId = UUID.randomUUID();
		UserUuidPK id = new UserUuidPK(request.getUserId(), jobId);

		DatumDeleteJobInfo info = new DatumDeleteJobInfo(id);
		info.setConfiguration(f);
		info.setJobState(DatumDeleteJobState.Queued);

		jobInfoDao.save(info);

		DatumDeleteTask task = taskForId(id);

		CompletableFuture<DatumDeleteJobInfo> future = new CompletableFuture<>();
		task.setDelegate(future);

		return task.info;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public DatumDeleteJobStatus performDatumDelete(UserUuidPK id) {
		DatumDeleteTask task = taskForId(id);

		Future<DatumDeleteJobInfo> future = executor.submit(task);
		task.setDelegate(future);

		return task;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public Set<ObjectDatumId> deleteDatum(Long userId, Set<ObjectDatumId> ids) {
		if ( ids == null || ids.isEmpty() ) {
			return Collections.emptySet();
		}
		if ( ids.size() > deleteDatumByIdMaxCount ) {
			throw new IllegalArgumentException(
					"A maximum of %d IDs can be deleted at once.".formatted(deleteDatumByIdMaxCount));
		}
		return datumDao.deleteForIds(userId, ids);
	}

	private DatumDeleteTask taskForId(UserUuidPK id) {
		DatumDeleteTask task = taskMap.get(id);
		if ( task != null && task.isExecuting() ) {
			// don't get state from database if we are the one executing the task
			return task;
		}
		DatumDeleteJobInfo info = jobInfoDao.get(id);
		if ( info == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, id);
		}
		return taskForJobInfo(info);
	}

	private DatumDeleteTask taskForJobInfo(DatumDeleteJobInfo info) {
		DatumDeleteTask task = new DatumDeleteTask(info);
		CompletableFuture<DatumDeleteJobInfo> future = new CompletableFuture<>();
		task.setDelegate(future);
		DatumDeleteTask already = taskMap.putIfAbsent(info.id(), task);
		if ( already != null && !already.isExecuting() ) {
			// refresh info in task
			already.setInfo(info);
		}
		return (already != null ? already : task);
	}

	private void postJobStatusChangedEvent(DatumDeleteJobStatus status, DatumDeleteJobInfo result) {
		if ( status == null ) {
			return;
		}
		final AppEventPublisher ea = getEventPublisher();
		if ( ea == null ) {
			return;
		}
		ea.postEvent(status.asJobStatusChangedEvent(result));
	}

	private class DatumDeleteTask implements Callable<DatumDeleteJobInfo>, DatumDeleteJobStatus {

		private DatumDeleteJobInfo info;
		private GeneralNodeDatumFilter filter;
		private @Nullable Future<DatumDeleteJobInfo> delegate;
		private @Nullable ExecutorService progressExecutor;

		/**
		 * Construct from a task info.
		 *
		 * <p>
		 * Once this task has been submitted to an executor, call
		 * {@link #setDelegate(Future)} with the resulting {@code Future}.
		 * </p>
		 *
		 * @param info
		 *        the info
		 * @throws IllegalArgumentException
		 *         if any argument is {@code null}, or
		 *         {@code info.getConfiguration()} is {@code null}
		 */
		private DatumDeleteTask(DatumDeleteJobInfo info) {
			super();
			this.info = requireNonNullArgument(info, "info");
			this.filter = requireNonNullArgument(info.getConfiguration(), "info.configuration");
		}

		private void setDelegate(Future<DatumDeleteJobInfo> delegate) {
			this.delegate = delegate;
		}

		private void setInfo(DatumDeleteJobInfo info) {
			this.info = requireNonNullArgument(info, "info");
			this.filter = requireNonNullArgument(info.getConfiguration(), "info.configuration");
		}

		private boolean isExecuting() {
			return info.getJobState() == DatumDeleteJobState.Executing;
		}

		@Override
		public DatumDeleteJobInfo call() throws Exception {
			// update status to indicate we've started
			info.setPercentComplete(0);
			info.setStarted(now());
			updateTaskStatus(DatumDeleteJobState.Executing);

			try {
				doDelete();
				String msg = "Deleted " + getResultCount() + " datum.";
				updateTaskStatus(DatumDeleteJobState.Completed, Boolean.TRUE, msg, Instant.now());
			} catch ( Exception e ) {
				log.warn("Error deleting datum for task {}", this, e);
				Throwable root = e;
				while ( root.getCause() != null ) {
					root = root.getCause();
				}
				StringBuilder msg = new StringBuilder();
				if ( root.getMessage() == null && e.getMessage() != null ) {
					msg.append(e.getMessage());
				} else if ( root.getMessage() == null ) {
					msg.append(root.getClass().getSimpleName());
				} else {
					msg.append(root.getMessage());
				}
				updateTaskStatus(DatumDeleteJobState.Completed, Boolean.FALSE, msg.toString(), now());
			} finally {
				if ( info.getJobState() != DatumDeleteJobState.Completed ) {
					updateTaskStatus(DatumDeleteJobState.Completed);
				}
			}
			return nonnull(jobInfoDao.get(info.id()), "Entity");
		}

		private void updateTaskStatus(DatumDeleteJobState state) {
			updateTaskStatus(state, null, null, null);
		}

		private void updateTaskStatus(DatumDeleteJobState state, @Nullable Boolean success,
				@Nullable String message, @Nullable Instant completionDate) {
			log.info(
					"Datum delete job {} for user {} transitioned to state {} with success {}; deleted {} datum",
					info.id().id(), info.getUserId(), state, success, getResultCount());
			info.setJobState(state);
			if ( success != null ) {
				info.setJobSuccess(success);
			}
			if ( message != null ) {
				info.setMessage(message);
			}
			if ( completionDate != null ) {
				info.setCompleted(completionDate);
			}
			jobInfoDao.save(info);

			postJobStatusChangedEvent(this, info);
		}

		private void updateTaskProgress(double amountComplete, long resultCount) {
			info.setPercentComplete(amountComplete);
			info.setResultCount(resultCount);
			if ( progressExecutor == null ) {
				progressExecutor = Executors.newSingleThreadExecutor();
			}
			var _ = progressExecutor.submit(new ProgressUpdater(info.id(), amountComplete, resultCount));
			postJobStatusChangedEvent(this, info);
		}

		private void doDelete() throws IOException {
			final String id = info.getJobId();
			final String nodeIds = (filter.getNodeIds() != null
					? stream(filter.getNodeIds()).map(Object::toString).collect(Collectors.joining(","))
					: "*");
			final String sourceIds = (filter.getSourceIds() != null
					? String.join(",", filter.getSourceIds())
					: "*");
			log.info("Executing user {} datum delete request {}: nodes = {}; sources = {}; {} - {}",
					info.getUserId(), id, nodeIds, sourceIds, filter.getLocalStartDate(),
					filter.getLocalEndDate());
			final long start = System.currentTimeMillis();
			long result = 0;
			final LocalDateTime startDate = filter.getLocalStartDate();
			final LocalDateTime endDate = filter.getLocalEndDate();
			final Duration timeBatchDuration = getDeleteBatchDuration();
			if ( startDate != null && endDate != null && endDate.isAfter(startDate)
					&& timeBatchDuration != null
					&& timeBatchDuration.compareTo(MIN_BATCH_DURATION) >= 0 ) {
				// break up delete into time-based batches
				final long intervalTime = ChronoUnit.MILLIS.between(startDate.toInstant(ZoneOffset.UTC),
						endDate.toInstant(ZoneOffset.UTC));
				LocalDateTime currStartDate = startDate;
				DatumFilterCommand batchFilter = new DatumFilterCommand(filter);
				long offsetTime = 0;
				while ( currStartDate.isBefore(endDate) ) {
					LocalDateTime currEndDate = currStartDate.plus(timeBatchDuration);
					if ( currEndDate.isAfter(endDate) ) {
						currEndDate = endDate;
					}
					batchFilter.setLocalStartDate(currStartDate);
					batchFilter.setLocalEndDate(currEndDate);
					final long batchStart = System.currentTimeMillis();
					final long batchResult = datumDao
							.deleteFiltered(nonnull(criteriaFromFilter(batchFilter), "Criteria"));
					log.info(
							"Deleted {} datum in {}s for user {} request {}: nodes = {}; sources = {}; {} - {}",
							batchResult, (int) ((System.currentTimeMillis() - batchStart) / 1000.0),
							filter.getUserId(), id, nodeIds, sourceIds, currStartDate, currEndDate);
					result += batchResult;
					offsetTime += timeBatchDuration.toMillis();
					currStartDate = currStartDate.plus(timeBatchDuration);
					updateTaskProgress(Math.min((double) offsetTime / intervalTime, 1.0), result);
				}
			} else {
				result = datumDao.deleteFiltered(nonnull(criteriaFromFilter(filter), "Criteria"));
				info.setPercentComplete(1.0);
			}
			info.setResultCount(result);
			log.info("Deleted {} datum in {}s for user {} request {}: nodes = {}; sources = {}; {} - {}",
					result, (int) ((System.currentTimeMillis() - start) / 1000.0), filter.getUserId(),
					id, nodeIds, sourceIds, filter.getLocalStartDate(), filter.getLocalEndDate());
		}

		@Override
		public GeneralNodeDatumFilter getConfiguration() {
			return filter;
		}

		@Override
		public Long getUserId() {
			return info.getUserId();
		}

		@Override
		public String getJobId() {
			return info.getJobId();
		}

		@Override
		public DatumDeleteJobState getJobState() {
			return (info.getJobState() != null ? info.getJobState() : DatumDeleteJobState.Unknown);
		}

		@Override
		public double getPercentComplete() {
			return info.getPercentComplete();
		}

		@Override
		public long getSubmitDate() {
			Instant d = info.getCreated();
			return (d != null ? d.toEpochMilli() : 0);
		}

		@Override
		public long getStartedDate() {
			Instant d = info.getStarted();
			return (d != null ? d.toEpochMilli() : 0);
		}

		@Override
		public long getCompletionDate() {
			Instant d = info.getCompleted();
			return (d != null ? d.toEpochMilli() : 0);
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			final var delegate = this.delegate;
			return (delegate != null ? delegate.cancel(mayInterruptIfRunning) : false);
		}

		@Override
		public boolean isCancelled() {
			final var delegate = this.delegate;
			return (delegate != null ? delegate.isCancelled() : false);
		}

		@Override
		public boolean isDone() {
			final var delegate = this.delegate;
			return (delegate != null ? delegate.isDone() : false);
		}

		@Override
		public DatumDeleteJobInfo get() throws InterruptedException, ExecutionException {
			final var delegate = this.delegate;
			if ( delegate != null ) {
				return delegate.get();
			}
			return info;
		}

		@Override
		public DatumDeleteJobInfo get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException, TimeoutException {
			final var delegate = this.delegate;
			if ( delegate != null ) {
				return delegate.get(timeout, unit);
			}
			return info;
		}

		@Override
		public boolean isSuccess() {
			return info.isSuccess();
		}

		@Override
		public @Nullable String getMessage() {
			return info.getMessage();
		}

		@Override
		public long getResultCount() {
			return info.getResultCount();
		}

		@Override
		public String toString() {
			return "DatumDeleteTask{userId=" + getUserId() + ",jobId=" + getJobId() + ",config=" + filter
					+ ",jobState=" + getJobState() + ",percentComplete=" + getPercentComplete()
					+ ",completionDate=" + getCompletionDate() + "}";
		}

	}

	private class ProgressUpdater implements Runnable {

		private final UserUuidPK id;
		private final double percentComplete;
		private final long deletedCount;

		private ProgressUpdater(UserUuidPK id, double percentComplete, long deletedCount) {
			super();
			this.id = id;
			this.percentComplete = percentComplete;
			this.deletedCount = deletedCount;
		}

		@Override
		public void run() {
			jobInfoDao.updateJobProgress(id, percentComplete, deletedCount);
		}

	}

	/**
	 * Set the minimum time, in milliseconds, to maintain import job status
	 * information after the job has completed.
	 *
	 * @param completedTaskMinimumCacheTime
	 *        the time in milliseconds to set
	 */
	public void setCompletedTaskMinimumCacheTime(long completedTaskMinimumCacheTime) {
		this.completedTaskMinimumCacheTime = completedTaskMinimumCacheTime;
	}

	/**
	 * Get the event publisher service.
	 *
	 * @return the service
	 */
	public @Nullable AppEventPublisher getEventPublisher() {
		return eventPublisher;
	}

	/**
	 * Configure an {@link AppEventPublisher} service for posting status events.
	 *
	 * @param eventPublisher
	 *        the event publisher service
	 */
	public void setEventPublisher(@Nullable AppEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	/**
	 * Configure a scheduler for task maintenance.
	 *
	 * @param scheduler
	 *        the scheduler to use
	 */
	public void setScheduler(@Nullable TaskScheduler scheduler) {
		this.scheduler = scheduler;
	}

	/**
	 * Get the delete batch duration.
	 *
	 * @return the duration, or {@code null} to not perform time-based delete
	 *         batching; defaults to {@link #DEFAULT_DELETE_BATCH_DURATION}
	 */
	public @Nullable Duration getDeleteBatchDuration() {
		return deleteBatchDuration;
	}

	/**
	 * Set the delete batch duration.
	 *
	 * @param deleteBatchDuration
	 *        the duration to set, or {@code null} to not perform time-based
	 *        delete batching
	 */
	public void setDeleteBatchDuration(@Nullable Duration deleteBatchDuration) {
		this.deleteBatchDuration = deleteBatchDuration;
	}

	/**
	 * Set the delete batch duration as a number of days.
	 *
	 * @param days
	 *        the number of days to use for delete batches, or {@literal 0} to
	 *        disable batching
	 */
	public void setDeleteBatchDays(int days) {
		setDeleteBatchDuration(days > 0 ? Duration.ofDays(days) : null);
	}

	/**
	 * Get the maximum number of datum IDs to allow deleting in one call.
	 * 
	 * @return the maximum count
	 * @since 1.2
	 */
	public final int getDeleteDatumByIdMaxCount() {
		return deleteDatumByIdMaxCount;
	}

	/**
	 * Set the maximum number of datum IDs to allow deleting in one call.
	 * 
	 * @param deleteDatumByIdMaxCount
	 *        the maximum count to set
	 * @since 1.2
	 */
	public final void setDeleteDatumByIdMaxCount(int deleteDatumByIdMaxCount) {
		this.deleteDatumByIdMaxCount = deleteDatumByIdMaxCount;
	}

}
