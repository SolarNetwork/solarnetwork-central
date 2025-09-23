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

package net.solarnetwork.central.user.expire.biz.dao;

import static java.util.Arrays.stream;
import static net.solarnetwork.central.datum.v2.support.DatumUtils.criteriaFromFilter;
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
import net.solarnetwork.central.user.expire.biz.UserDatumDeleteBiz;
import net.solarnetwork.central.user.expire.biz.UserDatumDeleteJobBiz;
import net.solarnetwork.central.user.expire.dao.UserDatumDeleteJobInfoDao;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobInfo;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobState;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobStatus;
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

	private TaskScheduler scheduler;
	private long completedTaskMinimumCacheTime = TimeUnit.HOURS.toMillis(4);
	private AppEventPublisher eventPublisher;
	private Duration deleteBatchDuration = DEFAULT_DELETE_BATCH_DURATION;
	private int deleteDatumByIdMaxCount = DEFAULT_DELETE_DATUM_BY_ID_MAX_COUNT;

	private ScheduledFuture<?> taskPurgerTask = null;

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
	 */
	public DaoUserDatumDeleteBiz(AsyncTaskExecutor executor, UserNodeDao userNodeDao,
			DatumMaintenanceDao datumDao, UserDatumDeleteJobInfoDao jobInfoDao) {
		super();
		this.executor = executor;
		this.userNodeDao = userNodeDao;
		this.datumDao = datumDao;
		this.jobInfoDao = jobInfoDao;
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
			DatumRecordCounts counts = new DatumRecordCounts();
			counts.setDate(Instant.now());
			return counts;
		}
		BasicDatumCriteria c = criteriaFromFilter(filter);
		return DatumUtils.toRecordCounts(datumDao.countDatumRecords(c));
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public long purgeOldJobs(Instant olderThanDate) {
		return jobInfoDao.purgeOldJobs(olderThanDate);
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public DatumDeleteJobInfo datumDeleteJobForUser(Long userId, String jobId) {
		return jobInfoDao.get(new UserUuidPK(userId, UUID.fromString(jobId)));
	}

	@Override
	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
	public Collection<DatumDeleteJobInfo> datumDeleteJobsForUser(Long userId,
			Set<DatumDeleteJobState> states) {
		return jobInfoDao.findForUser(userId, states);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED)
	public DatumDeleteJobInfo submitDatumDeleteRequest(GeneralNodeDatumFilter request) {
		GeneralNodeDatumFilter f = prepareFilter(request);

		UUID jobId = UUID.randomUUID();
		UserUuidPK id = new UserUuidPK(request.getUserId(), jobId);

		DatumDeleteJobInfo info = new DatumDeleteJobInfo();
		info.setId(id);
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
		DatumDeleteTask already = taskMap.putIfAbsent(info.getId(), task);
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
		private Future<DatumDeleteJobInfo> delegate;
		private ExecutorService progressExecutor;

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
		 */
		private DatumDeleteTask(DatumDeleteJobInfo info) {
			super();
			setInfo(info);
		}

		private void setDelegate(Future<DatumDeleteJobInfo> delegate) {
			this.delegate = delegate;
		}

		private void setInfo(DatumDeleteJobInfo info) {
			this.info = info;
		}

		private boolean isExecuting() {
			return info.getJobState() == DatumDeleteJobState.Executing;
		}

		@Override
		public DatumDeleteJobInfo call() throws Exception {
			// update status to indicate we've started
			info.setPercentComplete(0);
			info.setStarted(Instant.now());
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
				updateTaskStatus(DatumDeleteJobState.Completed, Boolean.FALSE, msg.toString(),
						Instant.now());
			} finally {
				if ( info.getJobState() != DatumDeleteJobState.Completed ) {
					updateTaskStatus(DatumDeleteJobState.Completed);
				}
			}
			return jobInfoDao.get(info.getId());
		}

		private void updateTaskStatus(DatumDeleteJobState state) {
			updateTaskStatus(state, null, null, null);
		}

		private void updateTaskStatus(DatumDeleteJobState state, Boolean success, String message,
				Instant completionDate) {
			log.info(
					"Datum delete job {} for user {} transitioned to state {} with success {}; deleted {} datum",
					info.getId().getId(), info.getUserId(), state, success, getResultCount());
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
			@SuppressWarnings("unused")
			var unused = progressExecutor
					.submit(new ProgressUpdater(info.getId(), amountComplete, resultCount));
			postJobStatusChangedEvent(this, info);
		}

		private void doDelete() throws IOException {
			final String id = info.getJobId();
			final GeneralNodeDatumFilter f = info.getConfiguration();
			final String nodeIds = (f.getNodeIds() != null
					? stream(f.getNodeIds()).map(Object::toString).collect(Collectors.joining(","))
					: "*");
			final String sourceIds = (f.getSourceIds() != null ? String.join(",", f.getSourceIds())
					: "*");
			log.info("Executing user {} datum delete request {}: nodes = {}; sources = {}; {} - {}",
					f.getUserId(), id, nodeIds, sourceIds, f.getLocalStartDate(), f.getLocalEndDate());
			final long start = System.currentTimeMillis();
			long result = 0;
			final LocalDateTime startDate = f.getLocalStartDate();
			final LocalDateTime endDate = f.getLocalEndDate();
			final Duration timeBatchDuration = getDeleteBatchDuration();
			if ( startDate != null && endDate != null && endDate.isAfter(startDate)
					&& timeBatchDuration != null
					&& timeBatchDuration.compareTo(MIN_BATCH_DURATION) >= 0 ) {
				// break up delete into time-based batches
				final long intervalTime = ChronoUnit.MILLIS.between(startDate.toInstant(ZoneOffset.UTC),
						endDate.toInstant(ZoneOffset.UTC));
				LocalDateTime currStartDate = startDate;
				DatumFilterCommand batchFilter = new DatumFilterCommand(f);
				long offsetTime = 0;
				while ( currStartDate.isBefore(endDate) ) {
					LocalDateTime currEndDate = currStartDate.plus(timeBatchDuration);
					if ( currEndDate.isAfter(endDate) ) {
						currEndDate = endDate;
					}
					batchFilter.setLocalStartDate(currStartDate);
					batchFilter.setLocalEndDate(currEndDate);
					final long batchStart = System.currentTimeMillis();
					final long batchResult = datumDao.deleteFiltered(criteriaFromFilter(batchFilter));
					log.info(
							"Deleted {} datum in {}s for user {} request {}: nodes = {}; sources = {}; {} - {}",
							batchResult, (int) ((System.currentTimeMillis() - batchStart) / 1000.0),
							f.getUserId(), id, nodeIds, sourceIds, currStartDate, currEndDate);
					result += batchResult;
					offsetTime += timeBatchDuration.toMillis();
					currStartDate = currStartDate.plus(timeBatchDuration);
					updateTaskProgress(Math.min((double) offsetTime / intervalTime, 1.0), result);
				}
			} else {
				result = datumDao.deleteFiltered(criteriaFromFilter(f));
				info.setPercentComplete(1.0);
			}
			info.setResultCount(result);
			log.info("Deleted {} datum in {}s for user {} request {}: nodes = {}; sources = {}; {} - {}",
					result, (int) ((System.currentTimeMillis() - start) / 1000.0), f.getUserId(), id,
					nodeIds, sourceIds, f.getLocalStartDate(), f.getLocalEndDate());
		}

		@Override
		public GeneralNodeDatumFilter getConfiguration() {
			return info.getConfiguration();
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
			return info.getJobState();
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
			return delegate.cancel(mayInterruptIfRunning);
		}

		@Override
		public boolean isCancelled() {
			return delegate.isCancelled();
		}

		@Override
		public boolean isDone() {
			return delegate.isDone();
		}

		@Override
		public DatumDeleteJobInfo get() throws InterruptedException, ExecutionException {
			return delegate.get();
		}

		@Override
		public DatumDeleteJobInfo get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException, TimeoutException {
			return delegate.get(timeout, unit);
		}

		@Override
		public boolean isSuccess() {
			return info.isSuccess();
		}

		@Override
		public String getMessage() {
			return info.getMessage();
		}

		@Override
		public long getResultCount() {
			return info.getResultCount();
		}

		@Override
		public String toString() {
			return "DatumDeleteTask{userId=" + getUserId() + ",jobId=" + getJobId() + ",config="
					+ (info != null ? info.getConfiguration() : null) + ",jobState=" + getJobState()
					+ ",percentComplete=" + getPercentComplete() + ",completionDate="
					+ getCompletionDate() + "}";
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
	public AppEventPublisher getEventPublisher() {
		return eventPublisher;
	}

	/**
	 * Configure an {@link AppEventPublisher} service for posting status events.
	 *
	 * @param eventPublisher
	 *        the event publisher service
	 */
	public void setEventPublisher(AppEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	/**
	 * Configure a scheduler for task maintenance.
	 *
	 * @param scheduler
	 *        the scheduler to use
	 */
	public void setScheduler(TaskScheduler scheduler) {
		this.scheduler = scheduler;
	}

	/**
	 * Get the delete batch duration.
	 *
	 * @return the duration, or {@literal null} to not perform time-based delete
	 *         batching; defaults to {@link #DEFAULT_DELETE_BATCH_DURATION}
	 */
	public Duration getDeleteBatchDuration() {
		return deleteBatchDuration;
	}

	/**
	 * Set the delete batch duration.
	 *
	 * @param deleteBatchDuration
	 *        the duration to set, or {@literal null} to not perform time-based
	 *        delete batching
	 */
	public void setDeleteBatchDuration(Duration deleteBatchDuration) {
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
