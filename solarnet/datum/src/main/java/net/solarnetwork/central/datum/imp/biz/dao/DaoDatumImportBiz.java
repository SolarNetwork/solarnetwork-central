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
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
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
import java.util.stream.Collectors;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import net.solarnetwork.central.dao.SolarNodeOwnershipDao;
import net.solarnetwork.central.datum.domain.GeneralNodeDatum;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumComponents;
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
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.domain.FilterResults;
import net.solarnetwork.central.domain.SolarNodeOwnership;
import net.solarnetwork.central.security.AuthorizationException;
import net.solarnetwork.central.security.AuthorizationException.Reason;
import net.solarnetwork.central.support.BasicFilterResults;
import net.solarnetwork.central.user.domain.UserUuidPK;
import net.solarnetwork.dao.BasicBulkLoadingOptions;
import net.solarnetwork.dao.BulkLoadingDao.LoadingContext;
import net.solarnetwork.dao.BulkLoadingDao.LoadingExceptionHandler;
import net.solarnetwork.dao.BulkLoadingDao.LoadingTransactionMode;
import net.solarnetwork.service.OptionalService;
import net.solarnetwork.service.ProgressListener;
import net.solarnetwork.service.ResourceStorageService;
import net.solarnetwork.util.StringUtils;

/**
 * DAO based {@link DatumImportBiz}.
 * 
 * @author matt
 * @version 2.0
 */
public class DaoDatumImportBiz extends BaseDatumImportBiz implements DatumImportJobBiz {

	/** The default value for the {@code maxPreviewCount} property. */
	public static final int DEFAULT_MAX_PREVIEW_COUNT = 200;

	/** The default value for the {@code progressLogCount} property. */
	public static final int DEFAULT_PROGRESS_LOG_COUNT = 25000;

	/** The default value for the {@code resourceStorageWaitMs} property. */
	public static final long DEFAULT_RESOURCE_STORAGE_WAIT_MS = TimeUnit.MINUTES.toMillis(1);

	private final ScheduledExecutorService scheduler;
	private final ExecutorService executor;
	private final SolarNodeOwnershipDao nodeOwnershipDao;
	private final DatumImportJobInfoDao jobInfoDao;
	private final DatumEntityDao datumDao;
	private long completedTaskMinimumCacheTime = TimeUnit.HOURS.toMillis(4);
	private ExecutorService previewExecutor;
	private OptionalService<ResourceStorageService> resourceStorageService;
	private long resourceStorageWaitMs = DEFAULT_RESOURCE_STORAGE_WAIT_MS;
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
	 * @param nodeOwnershipDao
	 *        the user node DAO
	 * @param jobInfoDao
	 *        the job info DAO
	 * @param datumDao
	 *        the datum DAO
	 */
	public DaoDatumImportBiz(ScheduledExecutorService scheduler, ExecutorService executor,
			SolarNodeOwnershipDao userNodeDao, DatumImportJobInfoDao jobInfoDao,
			DatumEntityDao datumDao) {
		super();
		this.scheduler = scheduler;
		this.executor = executor;
		this.nodeOwnershipDao = userNodeDao;
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

	/**
	 * Generate a job group key based on the request user and given group key.
	 * 
	 * <p>
	 * If the request does not provide a group key, then a random UUID will be
	 * generated so the group is effectively unique for the request.
	 * </p>
	 * 
	 * @param request
	 *        the request to get a derived group key for
	 * @return the group key to use
	 */
	private String groupKeyForRequest(DatumImportRequest request) {
		if ( request.getConfiguration() != null && request.getConfiguration().getGroupKey() != null ) {
			return request.getConfiguration().getGroupKey();
		} else {
			return UUID.randomUUID().toString();
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
		info.setGroupKey(groupKeyForRequest(request));

		File f = saveToWorkDirectory(resource, id);
		ResourceStorageService rss = resourceStorageService();
		if ( rss != null && rss.isConfigured() ) {
			CompletableFuture<Boolean> saveFuture = saveToResourceStorage(f, id, rss);
			if ( this.resourceStorageWaitMs > 0 ) {
				try {
					Boolean result = saveFuture.get(this.resourceStorageWaitMs, TimeUnit.MILLISECONDS);
					if ( result != null && !result.booleanValue() ) {
						throw new RuntimeException("Failed to save resource.");
					}
				} catch ( TimeoutException e ) {
					log.warn(
							"Timeout waiting {}ms for data import file [{}] to save to resource storage [{}], moving on",
							this.resourceStorageWaitMs, f.getName(), rss.getUid());
				} catch ( InterruptedException e ) {
					log.warn(
							"Interrupted waiting {}ms for data import file [{}] to save to resource storage [{}], moving on",
							this.resourceStorageWaitMs, f.getName(), rss.getUid());
				} catch ( ExecutionException | RuntimeException e ) {
					log.error("Error saving data import file [{}] to resource storage [{}]: {}",
							this.resourceStorageWaitMs, f.getName(), rss.getUid(), e.getCause(), e);
					Throwable t = e;
					while ( t.getCause() != null ) {
						t = t.getCause();
					}
					throw new RuntimeException(
							"Failed to save import data to resource storage: " + t.getMessage());
				}
			}
		}

		jobInfoDao.store(info);

		DatumImportTask task = taskForId(id);

		CompletableFuture<DatumImportResult> future = new CompletableFuture<>();
		task.setDelegate(future);

		return new BasicDatumImportReceipt(jobId.toString(), info.getImportState(), info.getGroupKey());
	}

	private CompletableFuture<Boolean> saveToResourceStorage(File f, UserUuidPK id,
			ResourceStorageService rss) {
		return rss.saveResource(f.getName(), new FileSystemResource(f), true, (r, amount) -> {
			log.debug("Saved %.1f% of datum import [%s] to resource storage [{}]", (amount * 100.0),
					f.getName(), rss.getUid());
		});
	}

	private ResourceStorageService resourceStorageService() {
		OptionalService<ResourceStorageService> rss = this.resourceStorageService;
		return (rss != null ? rss.service() : null);
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
	public long purgeOldJobs(Instant olderThanDate) {
		return jobInfoDao.purgeOldJobs(olderThanDate);
	}

	@Override
	public DatumImportJobInfo claimQueuedJob() {
		return jobInfoDao.claimQueuedJob();
	}

	@Override
	public UserUuidPK saveJobInfo(DatumImportJobInfo jobInfo) {
		return jobInfoDao.store(jobInfo);
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
			boolean fetched = fetchImportResource(dataFile);
			if ( !fetched || !dataFile.canRead() ) {
				throw new FileNotFoundException(
						"Data file for job " + info.getId().getId() + " not found");
			}
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

	private boolean fetchImportResource(File dataFile) throws IOException {
		ResourceStorageService rss = resourceStorageService();
		if ( rss != null && rss.isConfigured() ) {
			try {
				CompletableFuture<Iterable<Resource>> resourcesFuture = rss
						.listResources(dataFile.getName());
				Iterable<Resource> resources = resourcesFuture.get(resourceStorageWaitMs,
						TimeUnit.MILLISECONDS);
				for ( Resource r : resources ) {
					try (FileOutputStream out = new FileOutputStream(dataFile)) {
						FileCopyUtils.copy(r.getInputStream(), out);
					}
					return true;
				}
			} catch ( TimeoutException e ) {
				log.warn(
						"Timeout waiting {}ms to fetch data import file [{}] from resource storage [{}]",
						this.resourceStorageWaitMs, dataFile.getName(), rss.getUid());
			} catch ( InterruptedException e ) {
				log.warn(
						"Interrupted waiting {}ms for data import file [{}] to save to resource storage [{}], moving on",
						this.resourceStorageWaitMs, dataFile.getName(), rss.getUid());
			} catch ( ExecutionException | RuntimeException e ) {
				Throwable t = e;
				while ( t.getCause() != null ) {
					t = t.getCause();
				}
				log.error("Error fetching data import file [{}] from resource storage [{}]: {}",
						this.resourceStorageWaitMs, dataFile.getName(), rss.getUid(), t.getMessage(), t);
				throw new RuntimeException(
						"Failed to save import data to resource storage: " + t.getMessage());
			}
		}
		return false;
	}

	private void cleanupAfterImportDone(File dataFile) {
		dataFile.delete();
		ResourceStorageService rss = resourceStorageService();
		if ( rss != null && rss.isConfigured() ) {
			try {
				CompletableFuture<Set<String>> f = rss
						.deleteResources(Arrays.asList(dataFile.getName()));
				f.get(resourceStorageWaitMs, TimeUnit.MILLISECONDS);
				log.info("Deleted completed data import resource from storage [{}]: {}", rss.getUid(),
						dataFile.getName());
			} catch ( TimeoutException e ) {
				log.warn(
						"Timeout waiting {}ms to delete data import file [{}] from resource storage [{}]",
						this.resourceStorageWaitMs, dataFile.getName(), rss.getUid());
			} catch ( InterruptedException e ) {
				log.warn(
						"Interrupted waiting {}ms to delete data import file [{}] from resource storage [{}]",
						this.resourceStorageWaitMs, dataFile.getName(), rss.getUid());
			} catch ( ExecutionException | RuntimeException e ) {
				Throwable t = e;
				while ( t.getCause() != null ) {
					t = t.getCause();
				}
				log.error("Error deleting data import file [{}] from resource storage [{}]: {}",
						this.resourceStorageWaitMs, dataFile.getName(), rss.getUid(), t.getMessage(), t);
			}
		}
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

			SolarNodeOwnership[] ownerships = nodeOwnershipDao.ownershipsForUserId(info.getUserId());
			final Set<Long> allowedNodeIds;
			final Map<Long, ZoneId> tzMap;
			if ( ownerships != null ) {
				allowedNodeIds = new HashSet<>(ownerships.length);
				tzMap = new HashMap<>(ownerships.length);
				for ( SolarNodeOwnership ownership : ownerships ) {
					allowedNodeIds.add(ownership.getNodeId());
					tzMap.put(ownership.getNodeId(), ownership.getZone());
				}
			} else {
				allowedNodeIds = Collections.emptySet();
				tzMap = Collections.emptyMap();
			}

			try (ImportContext input = createImportContext(info, this)) {
				for ( GeneralNodeDatum d : input ) {
					if ( !tzMap.containsKey(d.getNodeId()) ) {
						throw new AuthorizationException(Reason.ACCESS_DENIED, d.getNodeId());
					}
					ReportingGeneralNodeDatumComponents dc = new ReportingGeneralNodeDatumComponents(d);
					ZoneId tz = tzMap.get(d.getNodeId());
					dc.setLocalDateTime(d.getCreated().atZone(tz).toLocalDateTime());
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
			ProgressListener<DatumImportService>, LoadingExceptionHandler<GeneralNodeDatum> {

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

		private synchronized boolean isExecuting() {
			return progressExecutor != null;
		}

		private synchronized ExecutorService progressExecutor() {
			ExecutorService s = this.progressExecutor;
			if ( s == null ) {
				s = Executors.newSingleThreadExecutor();
				this.progressExecutor = s;
			}
			return s;
		}

		@Override
		public DatumImportResult call() throws Exception {
			// update status to indicate we've started
			info.setPercentComplete(0);
			info.setStarted(Instant.now());
			updateTaskStatus(DatumImportState.Executing);

			try {
				doImport();
				String msg = "Loaded " + getLoadedCount() + " datum.";
				updateTaskStatus(DatumImportState.Completed, Boolean.TRUE, msg, Instant.now());
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
						Instant.now());
			} finally {
				if ( info.getImportState() != DatumImportState.Completed ) {
					updateTaskStatus(DatumImportState.Completed);
				}
				ExecutorService s = progressExecutor();
				if ( !s.isShutdown() ) {
					s.shutdown();
				}
			}
			return new BasicDatumImportResult(info);
		}

		private void updateTaskStatus(DatumImportState state) {
			updateTaskStatus(state, null, null, null);
		}

		private void updateTaskStatus(DatumImportState state, Boolean success, String message,
				Instant completionDate) {
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
			postJobStatusChangedEvent(this, info);
			progressExecutor().submit(new StatusUpdater((DatumImportJobInfo) info.clone()));
		}

		@Override
		public void handleLoadingException(Throwable t, LoadingContext<GeneralNodeDatum> context) {
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

			SolarNodeOwnership[] ownerships = nodeOwnershipDao.ownershipsForUserId(info.getUserId());
			Set<Long> allowedNodeIds = (ownerships != null ? Arrays.stream(ownerships)
					.map(SolarNodeOwnership::getNodeId).collect(Collectors.toSet())
					: Collections.emptySet());

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
			BasicBulkLoadingOptions loadingOptions = new BasicBulkLoadingOptions(config.getName(),
					batchSize, txMode, null);

			log.info(
					"Starting datum import job {} for user {} from resource {} and tx mode {}; configuration: {}",
					info.getId().getId(), info.getUserId(), getImportDataFile(info.getId()), txMode,
					config);

			try (ImportContext input = createImportContext(info, this);
					LoadingContext<GeneralNodeDatum> loader = datumDao
							.createBulkLoadingContext(loadingOptions, this)) {
				try {
					for ( GeneralNodeDatum d : input ) {
						if ( !allowedNodeIds.contains(d.getNodeId()) ) {
							log.warn(
									"Datum import job {} denied access to node {}; allowed nodes are: {}",
									d.getNodeId(),
									StringUtils.commaDelimitedStringFromCollection(allowedNodeIds));
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
				cleanupAfterImportDone(getImportDataFile(info.getId()));
			}
		}

		@Override
		public synchronized void progressChanged(DatumImportService context, double amountComplete) {
			log.trace("Datum import job {} for user {} progress changed: {}", info.getId().getId(),
					info.getUserId(), amountComplete);
			// update progress in different thread, so state updated outside import transaction
			DatumImportJobInfo info = this.info;
			progressExecutor()
					.submit(new ProgressUpdater(info.getId(), amountComplete, getLoadedCount()));
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
		public String getGroupKey() {
			return info.getGroupKey();
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
		public long getImportDate() {
			Instant d = info.getImportDate();
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

	private class StatusUpdater implements Runnable {

		private final DatumImportJobInfo info;

		private StatusUpdater(DatumImportJobInfo info) {
			super();
			this.info = info;
		}

		@Override
		public void run() {
			jobInfoDao.store(info);
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
	 * {@link #previewStagedImportRequest(DatumImportPreviewRequest)} will be
	 * executed via this service. If not configured, import preview requests
	 * will be performed on the calling thread.
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

	/**
	 * Set an optional {@link ResourceStorageService} to store import data files
	 * on.
	 * 
	 * <p>
	 * This can be used as shared storage between different applications that
	 * accept the import data file versus later process it. Files will still be
	 * copied to the configured work directory. If this service is available,
	 * the data file will also be copied to this service so it can be shared by
	 * other applications.
	 * </p>
	 * 
	 * @param resourceStorageService
	 *        the storage service to use
	 * @since 1.1
	 */
	public void setResourceStorageService(
			OptionalService<ResourceStorageService> resourceStorageService) {
		this.resourceStorageService = resourceStorageService;
	}

	/**
	 * Get the amount of time to wait for saving import data to resource storage
	 * before abandoning waiting for the result.
	 * 
	 * @param resourceStorageWaitMs
	 *        the time to wait, in milliseconds; defaults to
	 *        {@link #DEFAULT_RESOURCE_STORAGE_WAIT_MS}
	 * @since 1.1
	 */
	public void setResourceStorageWaitMs(long resourceStorageWaitMs) {
		this.resourceStorageWaitMs = resourceStorageWaitMs;
	}

}
