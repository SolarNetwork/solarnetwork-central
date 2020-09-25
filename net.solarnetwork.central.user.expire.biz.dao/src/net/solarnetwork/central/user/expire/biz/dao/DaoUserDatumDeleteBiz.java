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
import java.io.IOException;
import java.util.Collection;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDateTime;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.DatumRecordCounts;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilter;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.UserUuidPK;
import net.solarnetwork.central.user.expire.biz.UserDatumDeleteBiz;
import net.solarnetwork.central.user.expire.biz.UserDatumDeleteJobBiz;
import net.solarnetwork.central.user.expire.dao.UserDatumDeleteJobInfoDao;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobInfo;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobState;
import net.solarnetwork.central.user.expire.domain.DatumDeleteJobStatus;
import net.solarnetwork.util.OptionalService;

/**
 * DAO implementation of {@link UserDatumDeleteBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoUserDatumDeleteBiz implements UserDatumDeleteBiz, UserDatumDeleteJobBiz {

	/** The default batch delete duration: 7 days. */
	public static final Duration DEFAULT_DELETE_BATCH_DURATION = Duration.standardDays(7);

	/** The minimum batch delete duration: 1 hour. */
	public static final Duration MIN_BATCH_DURATION = Duration.standardHours(1);

	private final UserNodeDao userNodeDao;
	private final GeneralNodeDatumDao datumDao;
	private final UserDatumDeleteJobInfoDao jobInfoDao;
	private final ExecutorService executor;

	private final ConcurrentMap<UserUuidPK, DatumDeleteTask> taskMap = new ConcurrentHashMap<>(16, 0.9f,
			1);

	private ScheduledExecutorService scheduler;
	private long completedTaskMinimumCacheTime = TimeUnit.HOURS.toMillis(4);
	private OptionalService<EventAdmin> eventAdmin;
	private Duration deleteBatchDuration = DEFAULT_DELETE_BATCH_DURATION;

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
	public DaoUserDatumDeleteBiz(ExecutorService executor, UserNodeDao userNodeDao,
			GeneralNodeDatumDao datumDao, UserDatumDeleteJobInfoDao jobInfoDao) {
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
					new DatumDeleteTaskPurger(completedTaskMinimumCacheTime, map), 1L, 1L,
					TimeUnit.HOURS);
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
			throw new IllegalArgumentException("GeneralNodeDatumFilter is required");
		}
		if ( filter.getUserId() == null ) {
			throw new AuthorizationException(Reason.ANONYMOUS_ACCESS_DENIED, null);
		}
		DatumFilterCommand f = null;
		if ( filter.getNodeId() == null ) {
			f = new DatumFilterCommand(filter);
			Set<Long> nodes = userNodeDao.findNodeIdsForUser(filter.getUserId());
			f.setNodeIds(nodes.toArray(new Long[nodes.size()]));
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
			counts.setDate(new DateTime());
			return counts;
		}
		return datumDao.countDatumRecords(filter);
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public long purgeOldJobs(DateTime olderThanDate) {
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
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public DatumDeleteJobInfo submitDatumDeleteRequest(GeneralNodeDatumFilter request) {
		GeneralNodeDatumFilter f = prepareFilter(request);

		UUID jobId = UUID.randomUUID();
		UserUuidPK id = new UserUuidPK(request.getUserId(), jobId);

		DatumDeleteJobInfo info = new DatumDeleteJobInfo();
		info.setId(id);
		info.setConfiguration(f);
		info.setJobState(DatumDeleteJobState.Queued);

		jobInfoDao.store(info);

		DatumDeleteTask task = taskForId(id);

		CompletableFuture<DatumDeleteJobInfo> future = new CompletableFuture<>();
		task.setDelegate(future);

		return task.info;
	}

	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public DatumDeleteJobStatus performDatumDelete(UserUuidPK id) {
		DatumDeleteTask task = taskForId(id);

		Future<DatumDeleteJobInfo> future = executor.submit(task);
		task.setDelegate(future);

		return task;
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
		EventAdmin ea = (this.eventAdmin != null ? this.eventAdmin.service() : null);
		if ( ea == null ) {
			return;
		}
		ea.postEvent(status.asJobStatusChagnedEvent(result));
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
			info.setStarted(new DateTime());
			updateTaskStatus(DatumDeleteJobState.Executing);

			try {
				doDelete();
				String msg = "Deleted " + getResultCount() + " datum.";
				updateTaskStatus(DatumDeleteJobState.Completed, Boolean.TRUE, msg, new DateTime());
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
						new DateTime());
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
				DateTime completionDate) {
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
			jobInfoDao.store(info);

			postJobStatusChangedEvent(this, info);
		}

		private void updateTaskProgress(double amountComplete, long resultCount) {
			info.setPercentComplete(amountComplete);
			info.setResultCount(resultCount);
			if ( progressExecutor == null ) {
				progressExecutor = Executors.newSingleThreadExecutor();
			}
			progressExecutor.submit(new ProgressUpdater(info.getId(), amountComplete, resultCount));
			postJobStatusChangedEvent(this, info);
		}

		private void doDelete() throws IOException {
			final String id = info.getJobId();
			final GeneralNodeDatumFilter f = info.getConfiguration();
			final String nodeIds = (f.getNodeIds() != null
					? stream(f.getNodeIds()).map(n -> n.toString()).collect(Collectors.joining(","))
					: "*");
			final String sourceIds = (f.getSourceIds() != null
					? stream(f.getSourceIds()).collect(Collectors.joining(","))
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
					&& !timeBatchDuration.isShorterThan(MIN_BATCH_DURATION) ) {
				// break up delete into time-based batches
				final long intervalTime = new Interval(startDate.toDateTime(DateTimeZone.UTC),
						endDate.toDateTime(DateTimeZone.UTC)).toDurationMillis();
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
					final long batchResult = datumDao.deleteFiltered(batchFilter);
					log.info(
							"Deleted {} datum in {}s for user {} request {}: nodes = {}; sources = {}; {} - {}",
							batchResult, (int) ((System.currentTimeMillis() - batchStart) / 1000.0),
							f.getUserId(), id, nodeIds, sourceIds, currStartDate, currEndDate);
					result += batchResult;
					offsetTime += timeBatchDuration.getMillis();
					currStartDate = currStartDate.plus(timeBatchDuration);
					updateTaskProgress(Math.min((double) offsetTime / intervalTime, 1.0), result);
				}
			} else {
				result = datumDao.deleteFiltered(f);
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
			DateTime d = info.getCreated();
			return (d != null ? d.getMillis() : 0);
		}

		@Override
		public long getStartedDate() {
			DateTime d = info.getStarted();
			return (d != null ? d.getMillis() : 0);
		}

		@Override
		public long getCompletionDate() {
			DateTime d = info.getCompleted();
			return (d != null ? d.getMillis() : 0);
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
			return "DatumImportTask{userId=" + getUserId() + ",jobId=" + getJobId() + ",config="
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
	 * Get the event admin service.
	 * 
	 * @return the service
	 */
	public OptionalService<EventAdmin> getEventAdmin() {
		return eventAdmin;
	}

	/**
	 * Configure an {@link EventAdmin} service for posting status events.
	 * 
	 * @param eventAdmin
	 *        the optional event admin service
	 */
	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	/**
	 * Configure a scheduler for task maintenance.
	 * 
	 * @param scheduler
	 *        the scheduler to use
	 */
	public void setScheduler(ScheduledExecutorService scheduler) {
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
		setDeleteBatchDuration(days > 0 ? Duration.standardDays(days) : null);
	}

}
