/* ==================================================================
 * DaoDatumImportBiz.java - 11/11/2018 7:13:56 AM
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

package net.solarnetwork.central.datum.imp.biz.dao;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.core.io.FileSystemResource;
import net.solarnetwork.central.dao.BulkLoadingDao.LoadingContext;
import net.solarnetwork.central.dao.BulkLoadingDao.LoadingExceptionHandler;
import net.solarnetwork.central.dao.BulkLoadingDao.LoadingTransactionMode;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumComponents;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.domain.ReportingGeneralNodeDatumComponents;
import net.solarnetwork.central.datum.imp.biz.DatumImportBiz;
import net.solarnetwork.central.datum.imp.biz.DatumImportException;
import net.solarnetwork.central.datum.imp.biz.DatumImportInputFormatService;
import net.solarnetwork.central.datum.imp.biz.DatumImportInputFormatService.ImportContext;
import net.solarnetwork.central.datum.imp.biz.DatumImportJobBiz;
import net.solarnetwork.central.datum.imp.biz.DatumImportService;
import net.solarnetwork.central.datum.imp.biz.DatumImportValidationException;
import net.solarnetwork.central.datum.imp.dao.DatumImportJobInfoDao;
import net.solarnetwork.central.datum.imp.domain.BasicConfiguration;
import net.solarnetwork.central.datum.imp.domain.BasicDatumImportReceipt;
import net.solarnetwork.central.datum.imp.domain.Configuration;
import net.solarnetwork.central.datum.imp.domain.DatumImportJobInfo;
import net.solarnetwork.central.datum.imp.domain.DatumImportPreviewRequest;
import net.solarnetwork.central.datum.imp.domain.DatumImportReceipt;
import net.solarnetwork.central.datum.imp.domain.DatumImportRequest;
import net.solarnetwork.central.datum.imp.domain.DatumImportResource;
import net.solarnetwork.central.datum.imp.domain.DatumImportResult;
import net.solarnetwork.central.datum.imp.domain.DatumImportState;
import net.solarnetwork.central.datum.imp.domain.DatumImportStatus;
import net.solarnetwork.central.datum.imp.domain.InputConfiguration;
import net.solarnetwork.central.datum.imp.support.BaseDatumImportBiz;
import net.solarnetwork.central.datum.imp.support.BasicDatumImportResource;
import net.solarnetwork.central.datum.imp.support.BasicDatumImportResult;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.support.BasicFilterResults;
import net.solarnetwork.central.support.SimpleBulkLoadingOptions;
import net.solarnetwork.central.user.dao.UserNodeDao;
import net.solarnetwork.central.user.domain.UserNode;
import net.solarnetwork.central.user.domain.UserUuidPK;
import net.solarnetwork.util.ProgressListener;

/**
 * DAO based {@link DatumImportBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoDatumImportBiz extends BaseDatumImportBiz implements DatumImportJobBiz {

	/** The default value for the {@code maxPreviewCount} property. */
	public static final int DEFAULT_MAX_PREVIEW_COUNT = 200;

	/** The default value for the {@code progressLogCount} property. */
	public static final int DEFAULT_PROGRESS_LOG_COUNT = 25000;

	private final ScheduledExecutorService scheduler;
	private final ExecutorService executor;
	private final UserNodeDao userNodeDao;
	private final DatumImportJobInfoDao jobInfoDao;
	private final GeneralNodeDatumDao datumDao;
	private long completedTaskMinimumCacheTime = TimeUnit.HOURS.toMillis(4);
	private ExecutorService previewExecutor;
	private int maxPreviewCount = DEFAULT_MAX_PREVIEW_COUNT;
	private int progressLogCount = DEFAULT_PROGRESS_LOG_COUNT;

	private ScheduledFuture<?> taskPurgerTask = null;
	private final ConcurrentMap<UserUuidPK, DatumImportTask> taskMap = new ConcurrentHashMap<>(16, 0.9f,
			1);

	/**
	 * Constructor.
	 * 
	 * @param scheduler
	 *        the scheduler, to perform periodic cleanup tasks with
	 * @param executor
	 *        the executor, to perform import operations with
	 * @param userNodeDao
	 *        the user node DAO
	 * @param jobInfoDao
	 *        the job info DAO
	 * @param datumDao
	 *        the datum DAO
	 */
	public DaoDatumImportBiz(ScheduledExecutorService scheduler, ExecutorService executor,
			UserNodeDao userNodeDao, DatumImportJobInfoDao jobInfoDao, GeneralNodeDatumDao datumDao) {
		super();
		this.scheduler = scheduler;
		this.executor = executor;
		this.userNodeDao = userNodeDao;
		this.jobInfoDao = jobInfoDao;
		this.datumDao = datumDao;

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
			ConcurrentMap<UserUuidPK, DatumImportStatus> map = (ConcurrentMap) taskMap;
			taskPurgerTask = scheduler.scheduleWithFixedDelay(
					new DatumImportTaskPurger(completedTaskMinimumCacheTime, map), 1L, 1L,
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

	@Override
	public DatumImportReceipt submitDatumImportRequest(DatumImportRequest request,
			DatumImportResource resource) throws IOException {
		UUID jobId = UUID.randomUUID();
		UserUuidPK id = new UserUuidPK(request.getUserId(), jobId);
		DatumImportJobInfo info = new DatumImportJobInfo();
		info.setId(id);
		info.setConfig(new BasicConfiguration(request.getConfiguration()));
		info.setImportDate(request.getImportDate());
		info.setImportState(
				info.getConfig().isStage() ? DatumImportState.Staged : DatumImportState.Queued);

		saveToWorkDirectory(resource, id);

		jobInfoDao.store(info);

		DatumImportTask task = taskForId(id);

		CompletableFuture<DatumImportResult> future = new CompletableFuture<>();
		task.setDelegate(future);

		return new BasicDatumImportReceipt(jobId.toString(), info.getImportState());
	}

	@Override
	public Future<FilterResults<GeneralNodeDatumComponents>> previewStagedImportRequest(
			DatumImportPreviewRequest request) {
		UserUuidPK id = new UserUuidPK(request.getUserId(), UUID.fromString(request.getJobId()));
		DatumImportTask task = taskForId(id);

		if ( task.getJobState() != DatumImportState.Staged ) {
			throw new DatumImportValidationException(
					"Cannot preview import request that is not currently staged; current state is "
							+ task.getJobState());
		}

		DatumImportPreview preview = new DatumImportPreview(task.info,
				Math.min(maxPreviewCount, request.getPreviewCount()));
		if ( previewExecutor != null ) {
			return previewExecutor.submit(preview);
		}

		CompletableFuture<FilterResults<GeneralNodeDatumComponents>> result = new CompletableFuture<>();
		try {
			result.complete(preview.call());
		} catch ( Exception e ) {
			result.completeExceptionally(e);
		}
		return result;
	}

	@Override
	public DatumImportStatus performImport(UserUuidPK id) {
		DatumImportTask task = taskForId(id);

		Future<DatumImportResult> future = executor.submit(task);
		task.setDelegate(future);

		return task;
	}

	@Override
	public long purgeOldJobs(DateTime olderThanDate) {
		return jobInfoDao.purgeOldJobs(olderThanDate);
	}

	@Override
	public DatumImportStatus datumImportJobStatusForUser(Long userId, String jobId) {
		UserUuidPK id = new UserUuidPK(userId, UUID.fromString(jobId));
		return taskForId(id);
	}

	private DatumImportTask taskForId(UserUuidPK id) {
		DatumImportTask task = taskMap.get(id);
		if ( task != null && task.isExecuting() ) {
			// don't get state from database if we are the one executing the task
			return task;
		}
		DatumImportJobInfo info = jobInfoDao.get(id);
		if ( info == null ) {
			throw new AuthorizationException(Reason.UNKNOWN_OBJECT, id);
		}
		return taskForJobInfo(info);
	}

	private DatumImportTask taskForJobInfo(DatumImportJobInfo info) {
		DatumImportTask task = new DatumImportTask(info);
		CompletableFuture<DatumImportResult> future = new CompletableFuture<>();
		task.setDelegate(future);
		DatumImportTask already = taskMap.putIfAbsent(info.getId(), task);
		if ( already != null && !already.isExecuting() ) {
			// refresh info in task
			already.setInfo(info);
		}
		return (already != null ? already : task);
	}

	@Override
	public Collection<DatumImportStatus> datumImportJobStatusesForUser(Long userId,
			Set<DatumImportState> states) {
		return jobInfoDao.findForUser(userId, states).stream().map(d -> taskForJobInfo(d))
				.collect(toList());
	}

	@Override
	public DatumImportStatus updateDatumImportJobConfigurationForUser(Long userId, String jobId,
			Configuration configuration) {
		UserUuidPK id = new UserUuidPK(userId, UUID.fromString(jobId));
		jobInfoDao.updateJobConfiguration(id, configuration);
		DatumImportJobInfo info = jobInfoDao.get(id);
		DatumImportTask task = taskForId(id);
		task.info.setConfig(info.getConfig());
		return task;
	}

	@Override
	public DatumImportStatus updateDatumImportJobStateForUser(Long userId, String jobId,
			DatumImportState desiredState, Set<DatumImportState> expectedStates) {
		UserUuidPK id = new UserUuidPK(userId, UUID.fromString(jobId));
		jobInfoDao.updateJobState(id, desiredState, expectedStates);
		DatumImportJobInfo info = jobInfoDao.get(id);
		DatumImportTask task = taskForId(id);
		task.info.setImportState(info.getImportState());
		return task;
	}

	@Override
	public Collection<DatumImportStatus> deleteDatumImportJobsForUser(Long userId, Set<String> jobIds) {
		Set<DatumImportState> allowStates = EnumSet
				.complementOf(EnumSet.of(DatumImportState.Claimed, DatumImportState.Executing));
		Set<UUID> ids = (jobIds != null ? jobIds.stream().map(s -> UUID.fromString(s)).collect(toSet())
				: null);
		int deleted = jobInfoDao.deleteForUser(userId, ids, allowStates);
		log.debug("Deleted {} import jobs for user {} matching ids {} in states {}", deleted, userId,
				jobIds, allowStates);
		taskMap.entrySet().removeIf(e -> {
			DatumImportTask task = e.getValue();
			return userId.equals(task.getUserId()) && jobIds.contains(task.getJobId())
					&& allowStates.contains(task.getJobState());
		});
		return jobInfoDao.findForUser(userId, null).stream().filter(job -> {
			return userId.equals(job.getUserId()) && jobIds.contains(job.getId().getId().toString());
		}).map(job -> taskForJobInfo(job)).collect(toList());
	}

	private ImportContext createImportContext(DatumImportJobInfo info,
			ProgressListener<DatumImportService> progressListener) throws IOException {
		File dataFile = getImportDataFile(info.getId());
		if ( !dataFile.canRead() ) {
			throw new FileNotFoundException("Data file for job " + info.getId().getId() + " not found");
		}
		Configuration config = info.getConfiguration();
		if ( config == null || config.getInputConfiguration() == null ) {
			throw new IllegalArgumentException("Configuration missing for job " + info.getId());
		}
		InputConfiguration inputConfig = config.getInputConfiguration();
		DatumImportInputFormatService inputService = optionalService(getInputServices(), inputConfig);
		if ( inputService == null ) {
			throw new RuntimeException(
					"No InputService found for ID " + inputConfig.getServiceIdentifier());
		}

		BasicDatumImportResource resource = new BasicDatumImportResource(
				new FileSystemResource(dataFile), inputService.getInputContentType());
		return inputService.createImportContext(inputConfig, resource, progressListener);
	}

	private class DatumImportPreview implements Callable<FilterResults<GeneralNodeDatumComponents>>,
			ProgressListener<DatumImportService> {

		private final DatumImportJobInfo info;
		private final int previewCount;
		private final List<GeneralNodeDatumComponents> results;
		private double percentComplete;

		/**
		 * Construct from a task info.
		 * 
		 * @param info
		 *        the info
		 * @param previewCount
		 *        the maximum number of datum to preview
		 */
		private DatumImportPreview(DatumImportJobInfo info, int previewCount) {
			super();
			this.info = info;
			this.previewCount = previewCount;
			results = new ArrayList<>(previewCount);
			this.percentComplete = 0;
		}

		@Override
		public FilterResults<GeneralNodeDatumComponents> call() throws Exception {
			Configuration config = info.getConfiguration();
			if ( config == null || config.getInputConfiguration() == null ) {
				throw new IllegalArgumentException("Configuration missing for job " + info.getId());
			}

			Set<Long> allowedNodeIds = userNodeDao.findNodeIdsForUser(info.getUserId());
			Map<Long, DateTimeZone> tzMap = new HashMap<>();

			try (ImportContext input = createImportContext(info, this)) {
				for ( GeneralNodeDatum d : input ) {
					if ( !allowedNodeIds.contains(d.getNodeId()) ) {
						throw new AuthorizationException(Reason.ACCESS_DENIED, d.getNodeId());
					}
					ReportingGeneralNodeDatumComponents dc = new ReportingGeneralNodeDatumComponents(d);
					DateTimeZone tz = tzMap.computeIfAbsent(dc.getNodeId(), sourceId -> {
						UserNode userNode = userNodeDao.get(dc.getNodeId());
						DateTimeZone zone = DateTimeZone.UTC;
						if ( userNode != null && userNode.getNodeLocation() != null
								&& userNode.getNodeLocation().getTimeZoneId() != null ) {
							zone = DateTimeZone.forID(userNode.getNodeLocation().getTimeZoneId());
						}
						return zone;
					});
					dc.setLocalDateTime(d.getCreated().withZone(tz).toLocalDateTime());
					results.add(dc);
					if ( results.size() >= previewCount ) {
						break;
					}
				}
			} catch ( RuntimeException e ) {
				throw e;
			} catch ( Exception e ) {
				throw new RuntimeException(e);
			}
			// provide an estimate of the overall results by extrapolating percentComplete
			Long totalCountEstimate = null;
			if ( percentComplete > 0 ) {
				totalCountEstimate = Math.round(results.size() / percentComplete);
			}
			return new BasicFilterResults<>(results, totalCountEstimate, 0, results.size());
		}

		@Override
		public void progressChanged(DatumImportService context, double amountComplete) {
			this.percentComplete = amountComplete;
		}

	}

	private class DatumImportTask implements Callable<DatumImportResult>, DatumImportStatus,
			ProgressListener<DatumImportService>,
			LoadingExceptionHandler<GeneralNodeDatum, GeneralNodeDatumPK> {

		private DatumImportJobInfo info;
		private Future<DatumImportResult> delegate;
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
		private DatumImportTask(DatumImportJobInfo info) {
			super();
			setInfo(info);
		}

		private void setDelegate(Future<DatumImportResult> delegate) {
			this.delegate = delegate;
		}

		private void setInfo(DatumImportJobInfo info) {
			this.info = info;
		}

		private boolean isExecuting() {
			return progressExecutor != null;
		}

		@Override
		public DatumImportResult call() throws Exception {
			// update status to indicate we've started
			info.setPercentComplete(0);
			info.setStarted(new DateTime());
			updateTaskStatus(DatumImportState.Executing);

			try {
				doImport();
				String msg = "Loaded " + getLoadedCount() + " datum.";
				updateTaskStatus(DatumImportState.Completed, Boolean.TRUE, msg, new DateTime());
			} catch ( Exception e ) {
				log.warn("Error importing datum for task {}", this, e);
				Throwable root = e;
				while ( root.getCause() != null ) {
					root = root.getCause();
				}
				StringBuilder msg = new StringBuilder();
				if ( e instanceof AuthorizationException ) {
					AuthorizationException ae = (AuthorizationException) e;
					if ( ae.getReason() == Reason.ACCESS_DENIED ) {
						msg.append("Not authorized to load data for node ").append(ae.getId())
								.append(".");
					} else {
						msg.append(ae.getMessage());
					}
				} else {
					if ( root.getMessage() == null && e.getMessage() != null ) {
						msg.append(e.getMessage());
					} else if ( root.getMessage() == null ) {
						msg.append(root.getClass().getSimpleName());
					} else {
						msg.append(root.getMessage());
					}
					if ( e instanceof DatumImportValidationException ) {
						DatumImportValidationException dive = (DatumImportValidationException) e;
						if ( root.getMessage() != null ) {
							msg.insert(0, " ");
							msg.insert(0, dive.getMessage());
						}
						if ( dive.getLineNumber() != null ) {
							msg.append("; line ").append(dive.getLineNumber());
						}
						if ( dive.getLine() != null ) {
							msg.append("; <span class=\"input-line\">").append(dive.getLine())
									.append("</span>");
						}
					}
				}
				updateTaskStatus(DatumImportState.Completed, Boolean.FALSE, msg.toString(),
						new DateTime());
			} finally {
				if ( info.getImportState() != DatumImportState.Completed ) {
					updateTaskStatus(DatumImportState.Completed);
				}
				if ( progressExecutor != null && !progressExecutor.isShutdown() ) {
					progressExecutor.shutdown();
				}
			}
			return new BasicDatumImportResult(info);
		}

		private void updateTaskStatus(DatumImportState state) {
			updateTaskStatus(state, null, null, null);
		}

		private void updateTaskStatus(DatumImportState state, Boolean success, String message,
				DateTime completionDate) {
			log.info(
					"Datum import job {} for user {} transitioned to state {} with success {}; loaded {} datum",
					info.getId().getId(), info.getUserId(), state, success, getLoadedCount());
			info.setImportState(state);
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

		@Override
		public void handleLoadingException(Throwable t,
				LoadingContext<GeneralNodeDatum, GeneralNodeDatumPK> context) {
			if ( t instanceof DatumImportValidationException ) {
				DatumImportValidationException ve = (DatumImportValidationException) t;
				throw new DatumImportException(ve.getMessage(), ve.getCause(), ve.getLineNumber(),
						ve.getLine(), context.getCommittedCount());
			}
			throw new DatumImportException("Error importing datum " + context.getLastLoadedEntity(), t,
					context.getLoadedCount() + 1, null, context.getCommittedCount());
		}

		private void doImport() throws IOException {
			Configuration config = info.getConfiguration();
			if ( config == null || config.getInputConfiguration() == null ) {
				throw new IllegalArgumentException("Configuration missing for job " + info.getId());
			}

			Set<Long> allowedNodeIds = userNodeDao.findNodeIdsForUser(info.getUserId());

			LoadingTransactionMode txMode = LoadingTransactionMode.SingleTransaction;
			Integer batchSize = null;
			if ( config.getBatchSize() != null ) {
				int bs = config.getBatchSize();
				if ( bs == 1 ) {
					txMode = LoadingTransactionMode.NoTransaction;
				} else if ( bs > 1 ) {
					txMode = LoadingTransactionMode.BatchTransactions;
					batchSize = config.getBatchSize();
				}
			}
			SimpleBulkLoadingOptions loadingOptions = new SimpleBulkLoadingOptions(config.getName(),
					batchSize, txMode, null);

			log.info(
					"Starting datum import job {} for user {} from resource {} and tx mode {}; configuration: {}",
					info.getId().getId(), info.getUserId(), getImportDataFile(info.getId()), txMode,
					config);

			try (ImportContext input = createImportContext(info, this);
					LoadingContext<GeneralNodeDatum, GeneralNodeDatumPK> loader = datumDao
							.createBulkLoadingContext(loadingOptions, this)) {
				try {
					for ( GeneralNodeDatum d : input ) {
						if ( !allowedNodeIds.contains(d.getNodeId()) ) {
							throw new AuthorizationException(Reason.ACCESS_DENIED, d.getNodeId());
						}
						d.setPosted(info.getImportDate());
						loader.load(d);
						long count = loader.getLoadedCount();
						info.setLoadedCount(count);
						if ( progressLogCount > 0 && count % progressLogCount == 0 ) {
							log.info("Datum import job {} for user {} loaded {} datum with progress {}",
									info.getId().getId(), info.getUserId(), count,
									info.getPercentComplete());
						}
					}
					loader.commit();
				} finally {
					info.setLoadedCount(loader.getCommittedCount());
				}
			} catch ( RuntimeException e ) {
				throw e;
			} catch ( Exception e ) {
				throw new RuntimeException(e);
			} finally {
				getImportDataFile(info.getId()).delete();
			}
		}

		@Override
		public synchronized void progressChanged(DatumImportService context, double amountComplete) {
			log.trace("Datum import job {} for user {} progress changed: {}", info.getId().getId(),
					info.getUserId(), amountComplete);
			// update progress in different thread, so state updated outside import transaction
			DatumImportJobInfo info = this.info;
			if ( progressExecutor == null ) {
				progressExecutor = Executors.newSingleThreadExecutor();
			}
			progressExecutor.submit(new ProgressUpdater(info.getId(), amountComplete, getLoadedCount()));
			info.setPercentComplete(amountComplete);
			postJobStatusChangedEvent(this, info);
		}

		@Override
		public Configuration getConfiguration() {
			return info.getConfiguration();
		}

		@Override
		public Long getUserId() {
			return info.getUserId();
		}

		@Override
		public String getJobId() {
			return info.getId().getId().toString();
		}

		@Override
		public DatumImportState getJobState() {
			return info.getImportState();
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
		public long getImportDate() {
			DateTime d = info.getImportDate();
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
		public DatumImportResult get() throws InterruptedException, ExecutionException {
			return delegate.get();
		}

		@Override
		public DatumImportResult get(long timeout, TimeUnit unit)
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
		public long getLoadedCount() {
			return info.getLoadedCount();
		}

		@Override
		public String toString() {
			return "DatumImportTask{userId=" + getUserId() + ",jobId=" + getJobId() + ",config="
					+ (info != null ? info.getConfig() : null) + ",jobState=" + getJobState()
					+ ",percentComplete=" + getPercentComplete() + ",completionDate="
					+ getCompletionDate() + "}";
		}

	}

	private class ProgressUpdater implements Runnable {

		private final UserUuidPK id;
		private final double percentComplete;
		private final long loadedCount;

		private ProgressUpdater(UserUuidPK id, double percentComplete, long loadedCount) {
			super();
			this.id = id;
			this.percentComplete = percentComplete;
			this.loadedCount = loadedCount;
		}

		@Override
		public void run() {
			jobInfoDao.updateJobProgress(id, percentComplete, loadedCount);
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
	 * Configure an {@link ExecutorService} to run import preview tasks with.
	 * 
	 * <p>
	 * If configured, then all import preview requests via
	 * {@link #previewStagedImportForUser(Long, String)} will be executed via
	 * this service. If not configured, import preview requests will be
	 * performed on the calling thread.
	 * </p>
	 * 
	 * @param previewExecutor
	 *        the executor to set
	 */
	public void setPreviewExecutor(ExecutorService previewExecutor) {
		this.previewExecutor = previewExecutor;
	}

	/**
	 * Set the maximum number of datum to preview in staged import jobs.
	 * 
	 * @param maxPreviewCount
	 *        the maximum number of datum to preview; defaults to
	 *        {@link #DEFAULT_MAX_PREVIEW_COUNT}
	 */
	public void setMaxPreviewCount(int maxPreviewCount) {
		this.maxPreviewCount = maxPreviewCount;
	}

	/**
	 * Set the import progress log frequency.
	 * 
	 * <p>
	 * This controls how often a status log will be emitted, based on the number
	 * of datum imported.
	 * </p>
	 * 
	 * @param progressLogCount
	 *        the count of datum imported to emit a status log
	 */
	public void setProgressLogCount(int progressLogCount) {
		this.progressLogCount = progressLogCount;
	}

}