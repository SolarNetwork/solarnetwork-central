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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.joda.time.DateTime;
import org.springframework.core.io.FileSystemResource;
import net.solarnetwork.central.dao.BulkLoadingDao.LoadingContext;
import net.solarnetwork.central.dao.BulkLoadingDao.LoadingExceptionHandler;
import net.solarnetwork.central.dao.BulkLoadingDao.LoadingTransactionMode;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumComponents;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.imp.biz.DatumImportBiz;
import net.solarnetwork.central.datum.imp.biz.DatumImportInputFormatService;
import net.solarnetwork.central.datum.imp.biz.DatumImportInputFormatService.ImportContext;
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
import net.solarnetwork.central.user.domain.UserUuidPK;
import net.solarnetwork.util.ProgressListener;

/**
 * DAO based {@link DatumImportBiz}.
 * 
 * @author matt
 * @version 1.0
 */
public class DaoDatumImportBiz extends BaseDatumImportBiz {

	/** The default value for the {@code maxPreviewCount} property. */
	public static final int DEFAULT_MAX_PREVIEW_COUNT = 200;

	private final ScheduledExecutorService scheduler;
	private final ExecutorService executor;
	private final UserNodeDao userNodeDao;
	private final DatumImportJobInfoDao jobInfoDao;
	private final GeneralNodeDatumDao datumDao;
	private long completedTaskMinimumCacheTime = TimeUnit.HOURS.toMillis(4);
	private ExecutorService previewExecutor;
	private int maxPreviewCount = DEFAULT_MAX_PREVIEW_COUNT;

	private boolean initialized = false;
	private final ConcurrentMap<String, DatumImportStatus> taskMap = new ConcurrentHashMap<>(16, 0.9f,
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
		if ( initialized ) {
			return;
		}
		// purge completed tasks every hour
		if ( scheduler != null ) {
			this.scheduler.scheduleWithFixedDelay(
					new DatumImportTaskPurger(completedTaskMinimumCacheTime, taskMap), 1L, 1L,
					TimeUnit.HOURS);
		}
		initialized = true;
	}

	@Override
	public DatumImportReceipt submitDatumImportRequest(DatumImportRequest request,
			DatumImportResource resource) throws IOException {
		UUID jobId = UUID.randomUUID();
		UserUuidPK pk = new UserUuidPK(request.getUserId(), jobId);
		DatumImportJobInfo info = new DatumImportJobInfo();
		info.setId(pk);
		info.setConfig(new BasicConfiguration(request.getConfiguration()));
		info.setImportDate(request.getImportDate());
		info.setImportState(
				info.getConfig().isStage() ? DatumImportState.Staged : DatumImportState.Queued);

		saveToWorkDirectory(resource, pk);

		info = jobInfoDao.get(jobInfoDao.store(info));

		DatumImportTask task = new DatumImportTask(info);
		CompletableFuture<DatumImportResult> future = new CompletableFuture<>();
		task.setDelegate(future);
		taskMap.put(task.getJobId(), task);

		return new BasicDatumImportReceipt(jobId.toString(), info.getImportState());
	}

	@Override
	public Future<FilterResults<GeneralNodeDatumComponents>> previewStagedImportRequest(
			DatumImportPreviewRequest request) {
		DatumImportStatus status = datumImportJobStatusForUser(request.getUserId(), request.getJobId());
		DatumImportTask task = (DatumImportTask) status;

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
	public DatumImportStatus performImport(Long userId, String jobId) {
		DatumImportStatus status = datumImportJobStatusForUser(userId, jobId);
		DatumImportTask task = (DatumImportTask) status;

		Future<DatumImportResult> future = executor.submit(task);
		task.setDelegate(future);

		return task;
	}

	@Override
	public DatumImportStatus datumImportJobStatusForUser(Long userId, String jobId) {
		DatumImportStatus status = taskMap.get(jobId);
		if ( status == null ) {
			UserUuidPK id = new UserUuidPK(userId, UUID.fromString(jobId));
			DatumImportJobInfo info = jobInfoDao.get(id);
			if ( info == null ) {
				throw new AuthorizationException(Reason.UNKNOWN_OBJECT, id);
			}
			DatumImportTask task = new DatumImportTask(info);
			CompletableFuture<DatumImportResult> future = new CompletableFuture<>();
			task.setDelegate(future);
			status = task;
		}
		return status;
	}

	@Override
	public Collection<DatumImportStatus> datumImportJobStatusesForUser(Long userId,
			Set<DatumImportState> states) {
		return taskMap.values().stream().filter(d -> d.getUserId().equals(userId))
				.collect(Collectors.toList());
	}

	@Override
	public DatumImportStatus updateDatumImportJobStateForUser(Long userId, String jobId,
			DatumImportState desiredState, Set<DatumImportState> expectedStates) {
		UserUuidPK id = new UserUuidPK(userId, UUID.fromString(jobId));
		jobInfoDao.updateJobState(id, desiredState, expectedStates);
		DatumImportJobInfo info = jobInfoDao.get(id);
		DatumImportStatus status = datumImportJobStatusForUser(userId, jobId);
		if ( status instanceof DatumImportTask ) {
			DatumImportTask task = (DatumImportTask) status;
			task.info.setImportState(info.getImportState());
		}
		return status;
	}

	private ImportContext createImportContext(DatumImportJobInfo info,
			ProgressListener<DatumImportService> progressListener) throws IOException {
		File dataFile = getImportDataFile(info.getId());
		if ( !dataFile.canRead() ) {
			throw new FileNotFoundException("Data file for job " + info.getId() + " not found");
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

			try (ImportContext input = createImportContext(info, this)) {
				for ( GeneralNodeDatum d : input ) {
					if ( !allowedNodeIds.contains(d.getNodeId()) ) {
						throw new AuthorizationException(Reason.ACCESS_DENIED, d.getNodeId());
					}
					results.add(new GeneralNodeDatumComponents(d));
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

		private final DatumImportJobInfo info;
		private double percentComplete;
		private long completionDate;
		private Future<DatumImportResult> delegate;
		private long loadedCount;

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
			this.info = info;
			this.loadedCount = 0;
		}

		/**
		 * Set the delegate {@code Future}.
		 * 
		 * @param delegate
		 *        the delegate
		 */
		private void setDelegate(Future<DatumImportResult> delegate) {
			this.delegate = delegate;
		}

		@Override
		public DatumImportResult call() throws Exception {
			percentComplete = 0;

			// update status to indicate we've started
			updateTaskStatus(DatumImportState.Executing);

			try {
				doImport();
				String msg = "Loaded " + loadedCount + " datum.";
				updateTaskStatus(DatumImportState.Completed, Boolean.TRUE, msg, new DateTime());
			} catch ( Exception e ) {
				log.error("Error importing datum for task {}", this, e);
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
					msg.append(root.getClass().getSimpleName());
					if ( root.getMessage() != null ) {
						msg.append(": ").append(root.getMessage());
					}
					msg.append("\n");
					try (StringWriter sout = new StringWriter();
							PrintWriter out = new PrintWriter(sout)) {
						root.printStackTrace(out);
						msg.append(sout.toString());
					} catch ( IOException e2 ) {
						// ignore
					}
				}
				updateTaskStatus(DatumImportState.Completed, Boolean.FALSE, msg.toString(),
						new DateTime());
			} finally {
				if ( info.getImportState() != DatumImportState.Completed ) {
					updateTaskStatus(DatumImportState.Completed);
				}
			}
			return new BasicDatumImportResult(info);
		}

		private void updateTaskStatus(DatumImportState state) {
			updateTaskStatus(state, null, null, null);
		}

		private void updateTaskStatus(DatumImportState state, Boolean success, String message,
				DateTime completionDate) {
			log.info("Datum import job {} transitioned to state {} with success {}", info.getId(), state,
					success);
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
			throw new DatumImportValidationException(
					"Error importing datum " + context.getLastLoadedEntity(), t,
					(int) context.getLoadedCount() + 1, null);
		}

		private void doImport() throws IOException {
			Configuration config = info.getConfiguration();
			if ( config == null || config.getInputConfiguration() == null ) {
				throw new IllegalArgumentException("Configuration missing for job " + info.getId());
			}

			Set<Long> allowedNodeIds = userNodeDao.findNodeIdsForUser(info.getUserId());
			SimpleBulkLoadingOptions loadingOptions = new SimpleBulkLoadingOptions(config.getName(),
					null, LoadingTransactionMode.SingleTransaction, null);

			try (ImportContext input = createImportContext(info, this);
					LoadingContext<GeneralNodeDatum, GeneralNodeDatumPK> loader = datumDao
							.createBulkLoadingContext(loadingOptions, this)) {
				for ( GeneralNodeDatum d : input ) {
					if ( !allowedNodeIds.contains(d.getNodeId()) ) {
						throw new AuthorizationException(Reason.ACCESS_DENIED, d.getNodeId());
					}
					d.setPosted(info.getImportDate());
					loader.load(d);
					loadedCount++;
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
		public void progressChanged(DatumImportService context, double amountComplete) {
			this.percentComplete = amountComplete;
			postJobStatusChangedEvent(this, info);
		}

		@Override
		public Long getUserId() {
			return info.getUserId();
		}

		@Override
		public String getJobId() {
			return info.getId().toString();
		}

		@Override
		public DatumImportState getJobState() {
			return info.getImportState();
		}

		@Override
		public double getPercentComplete() {
			return percentComplete;
		}

		@Override
		public long getCompletionDate() {
			return completionDate;
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
		public String toString() {
			return "DatumImportTask{userId=" + getUserId() + ",jobId=" + getJobId() + ",config="
					+ (info != null ? info.getConfig() : null) + ",jobState=" + getJobState()
					+ ",percentComplete=" + percentComplete + ",completionDate=" + completionDate + "}";
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

}
