/* ==================================================================
 * DaoDatumExportBiz.java - 18/04/2018 5:57:03 AM
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

package net.solarnetwork.central.datum.export.biz.dao;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static net.solarnetwork.util.ObjectUtils.requireNonNullArgument;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.datum.biz.QueryAuditor;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumPK;
import net.solarnetwork.central.datum.export.biz.DatumExportBiz;
import net.solarnetwork.central.datum.export.biz.DatumExportDestinationService;
import net.solarnetwork.central.datum.export.biz.DatumExportOutputFormatService;
import net.solarnetwork.central.datum.export.biz.DatumExportService;
import net.solarnetwork.central.datum.export.dao.DatumExportTaskInfoDao;
import net.solarnetwork.central.datum.export.domain.BasicConfiguration;
import net.solarnetwork.central.datum.export.domain.BasicDatumExportResult;
import net.solarnetwork.central.datum.export.domain.Configuration;
import net.solarnetwork.central.datum.export.domain.DatumExportRequest;
import net.solarnetwork.central.datum.export.domain.DatumExportResource;
import net.solarnetwork.central.datum.export.domain.DatumExportResult;
import net.solarnetwork.central.datum.export.domain.DatumExportState;
import net.solarnetwork.central.datum.export.domain.DatumExportStatus;
import net.solarnetwork.central.datum.export.domain.DatumExportTaskInfo;
import net.solarnetwork.central.datum.export.domain.ScheduleType;
import net.solarnetwork.central.datum.export.support.DatumExportException;
import net.solarnetwork.central.datum.v2.dao.BasicDatumCriteria;
import net.solarnetwork.central.datum.v2.dao.DatumEntityDao;
import net.solarnetwork.central.datum.v2.support.DatumUtils;
import net.solarnetwork.dao.BasicBulkExportOptions;
import net.solarnetwork.dao.BulkExportingDao.ExportCallback;
import net.solarnetwork.dao.BulkExportingDao.ExportCallbackAction;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.event.AppEventPublisher;
import net.solarnetwork.service.IdentifiableConfiguration;
import net.solarnetwork.service.ProgressListener;

/**
 * DAO-based implementation of {@link DatumExportBiz}.
 * 
 * @author matt
 * @version 2.0
 */
public class DaoDatumExportBiz implements DatumExportBiz {

	/** The datum export task name. */
	public static final String DATUM_EXPORT_NAME = "datum-export";

	/** The default query page size. */
	public static final int DEFAULT_QUERY_PAGE_SIZE = 1000;

	private final ConcurrentMap<String, DatumExportTask> taskMap = new ConcurrentHashMap<>(16);
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final DatumExportTaskInfoDao taskDao;
	private final DatumEntityDao datumDao;
	private final TaskScheduler scheduler;
	private final AsyncTaskExecutor executor;
	private final TransactionTemplate transactionTemplate;
	private List<DatumExportOutputFormatService> outputFormatServices;
	private List<DatumExportDestinationService> destinationServices;
	private AppEventPublisher eventPublisher;
	private long completedTaskMinimumCacheTime = TimeUnit.HOURS.toMillis(4);

	private ScheduledFuture<?> taskPurgerTask;
	private QueryAuditor queryAuditor;

	/**
	 * Constructor.
	 * 
	 * @param taskDao
	 *        the task DAO
	 * @param datumDao
	 *        the datum DAO
	 * @param scheduler
	 *        the scheduler
	 * @param executor
	 *        the executor
	 * @param transactionTemplate
	 *        the transaction template
	 * @throws IllegalArgumentException
	 *         if any argument is {@literal null}
	 */
	public DaoDatumExportBiz(DatumExportTaskInfoDao taskDao, DatumEntityDao datumDao,
			TaskScheduler scheduler, AsyncTaskExecutor executor,
			TransactionTemplate transactionTemplate) {
		super();
		this.taskDao = requireNonNullArgument(taskDao, "taskDao");
		this.datumDao = requireNonNullArgument(datumDao, "datumDao");
		this.scheduler = requireNonNullArgument(scheduler, "scheduler");
		this.executor = requireNonNullArgument(executor, "executor");
		this.transactionTemplate = requireNonNullArgument(transactionTemplate, "transactionTemplate");
	}

	/**
	 * Initialize after properties configured.
	 * 
	 * <p>
	 * Call this method once all properties have been configured on the
	 * instance.
	 * </p>
	 * 
	 * @since 1.1
	 */
	public void init() {
		if ( taskPurgerTask != null ) {
			return;
		}
		if ( scheduler != null ) {
			// purge completed tasks every hour
			this.taskPurgerTask = scheduler.scheduleWithFixedDelay(new TaskPurger(),
					Instant.now().plus(1, ChronoUnit.HOURS), Duration.ofHours(1));
		}
	}

	/**
	 * Shutdown after the service is no longer needed.
	 * 
	 * @since 1.1
	 */
	public void shutdown() {
		if ( taskPurgerTask != null ) {
			taskPurgerTask.cancel(true);
		}
	}

	private final class TaskPurger implements Runnable {

		@Override
		public void run() {
			for ( Iterator<DatumExportTask> itr = taskMap.values().iterator(); itr.hasNext(); ) {
				DatumExportTask status = itr.next();
				long completeDate = status.getCompletionDate();
				if ( status.isDone()
						&& completeDate + completedTaskMinimumCacheTime < System.currentTimeMillis() ) {
					log.info("Purging status for completed export task {}: {}", status.getJobId(),
							status.getJobState());
					itr.remove();
				}
			}
		}

	}

	private final class DatumExportTask implements Callable<DatumExportResult>, DatumExportStatus,
			ProgressListener<DatumExportService> {

		private static final int COUNT_UNKNOWN = -1;
		private final DatumExportTaskInfo info;
		private DatumExportState jobState;
		private double percentComplete;
		private long completionDate;
		private Future<DatumExportResult> delegate;

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
		private DatumExportTask(DatumExportTaskInfo info) {
			super();
			this.info = info;
			this.jobState = DatumExportState.Claimed;
		}

		/**
		 * Set the delegate {@code Future}.
		 * 
		 * @param delegate
		 *        the delegate
		 */
		private void setDelegate(Future<DatumExportResult> delegate) {
			this.delegate = delegate;
		}

		@Override
		public DatumExportResult call() throws Exception {
			percentComplete = 0;

			// update status to indicate we've started
			updateTaskStatus(DatumExportState.Executing);

			try {
				// first step: export to resources
				Iterable<DatumExportResource> resources = exportToResources(info.getConfiguration());

				// second step: upload the resources to the destination
				if ( resources != null ) {
					uploadToDestination(info.getConfiguration(), resources);
				}

				updateTaskStatus(DatumExportState.Completed, Boolean.TRUE, null, Instant.now());
			} catch ( Exception e ) {
				log.warn("Error exporting datum for task {}", this, e.getMessage());
				Throwable root = e;
				while ( root.getCause() != null ) {
					root = root.getCause();
				}
				final boolean justMessage = (root instanceof DatumExportException);
				StringBuilder msg = new StringBuilder();
				if ( !justMessage || root.getMessage() == null ) {
					msg.append(root.getClass().getSimpleName());
				}
				if ( !justMessage && root.getMessage() != null ) {
					msg.append(": ").append(root.getMessage());
				}
				if ( !justMessage ) {
					log.warn("Task {} root cause", this, root);
				}
				updateTaskStatus(DatumExportState.Completed, Boolean.FALSE, msg.toString(),
						Instant.now());
			} finally {
				if ( info.getStatus() != DatumExportState.Completed ) {
					updateTaskStatus(DatumExportState.Completed);
				}
			}
			return new BasicDatumExportResult(info);
		}

		private void updateTaskStatus(DatumExportState state) {
			updateTaskStatus(state, null, null, null);
		}

		private void updateTaskStatus(DatumExportState state, Boolean success, String message,
				Instant completionDate) {
			log.info("Datum export job {} transitioned to state {} with success {}", info.getId(), state,
					success);
			this.jobState = state;
			doWithinOptionalTransaction(() -> {
				info.setStatus(state);
				if ( success != null ) {
					info.setTaskSuccess(success);
				}
				if ( message != null ) {
					info.setMessage(message);
				}
				if ( completionDate != null ) {
					info.setCompleted(completionDate);
				}
				taskDao.store(info);
				return null;
			});
			postJobStatusChangedEvent(this);
		}

		private Iterable<DatumExportResource> exportToResources(Configuration config) {
			AggregateGeneralNodeDatumFilter datumFilter = (config.getDataConfiguration() != null
					? config.getDataConfiguration().getDatumFilter()
					: null);
			if ( datumFilter == null ) {
				throw new DatumExportException(info.getId(), "No datum filter available", null);
			}
			ScheduleType schedule = config.getSchedule();
			if ( schedule == null ) {
				schedule = ScheduleType.Daily;
			}
			if ( info.getExportDate() == null ) {
				throw new DatumExportException(info.getId(), "No export date available", null);
			}

			DatumExportOutputFormatService outputService = optionalService(outputFormatServices,
					config.getOutputConfiguration());
			if ( outputService == null ) {
				String serviceId = (config.getOutputConfiguration() != null
						? config.getOutputConfiguration().getServiceIdentifier()
						: null);
				throw new DatumExportException(info.getId(),
						"No output service available for identifier [" + serviceId + "]", null);
			}

			ZoneId zone = (config.getTimeZoneId() != null ? ZoneId.of(config.getTimeZoneId())
					: ZoneOffset.UTC);
			BasicDatumCriteria filter = DatumUtils.criteriaFromFilter(datumFilter);
			if ( schedule == ScheduleType.Adhoc ) {
				if ( !(filter.hasLocalDateRange() || filter.hasDateRange()) ) {
					throw new DatumExportException(info.getId(),
							"Adhoc export missing start or end date in data configuration", null);
				}
			} else {
				filter.setStartDate(info.getExportDate());
				filter.setEndDate(
						schedule.nextExportDate(filter.getStartDate().atZone(ZoneId.of(zone.getId())))
								.toInstant());
			}

			try (DatumExportOutputFormatService.ExportContext exportContext = outputService
					.createExportContext(config.getOutputConfiguration())) {

				BasicBulkExportOptions options = new BasicBulkExportOptions(DATUM_EXPORT_NAME,
						singletonMap(DatumEntityDao.EXPORT_PARAMETER_DATUM_CRITERIA, filter));

				final QueryAuditor auditor = queryAuditor;
				if ( auditor != null ) {
					auditor.resetCurrentAuditResults();
				}

				// all exported data will be audited on the hour we start the export at
				GeneralNodeDatumPK auditDatumKey = new GeneralNodeDatumPK();
				auditDatumKey.setCreated(Instant.now().truncatedTo(ChronoUnit.HOURS));

				datumDao.bulkExport(new ExportCallback<GeneralNodeDatumFilterMatch>() {

					@Override
					public void didBegin(Long totalResultCountEstimate) {
						try {
							exportContext
									.start(totalResultCountEstimate != null ? totalResultCountEstimate
											: COUNT_UNKNOWN);
						} catch ( IOException e ) {
							throw new DatumExportException(info.getId(), e.getMessage(), e);
						}
					}

					@Override
					public ExportCallbackAction handle(GeneralNodeDatumFilterMatch d) {
						if ( d != null && d.getId() != null && auditor != null ) {
							auditDatumKey.setNodeId(d.getId().getNodeId());
							auditDatumKey.setSourceId(d.getId().getSourceId());
							auditor.addNodeDatumAuditResults(singletonMap(auditDatumKey, 1));
						}
						try {
							exportContext.appendDatumMatch(singleton(d), DatumExportTask.this);
						} catch ( IOException e ) {
							throw new DatumExportException(info.getId(), e.getMessage(), e);
						}
						return ExportCallbackAction.CONTINUE;
					}
				}, options);

				return exportContext.finish();
			} catch ( IOException e ) {
				throw new DatumExportException(info.getId(), e.getMessage(), e);
			}
		}

		private void uploadToDestination(Configuration config, Iterable<DatumExportResource> resources)
				throws IOException {
			DatumExportDestinationService destService = optionalService(destinationServices,
					config.getDestinationConfiguration());
			if ( destService == null ) {
				String serviceId = (config.getDestinationConfiguration() != null
						? config.getDestinationConfiguration().getServiceIdentifier()
						: null);
				throw new DatumExportException(info.getId(),
						"No destination service available for identifier [" + serviceId + "]", null);
			}
			DatumExportOutputFormatService outputService = optionalService(outputFormatServices,
					config.getOutputConfiguration());
			DateTimeFormatter dateFormatter = config.createDateTimeFormatterForSchedule();
			Map<String, Object> runtimeProps = config.createRuntimeProperties(info.getExportDate(),
					dateFormatter, outputService);
			destService.export(config, resources, runtimeProps, this);
		}

		@Override
		public void progressChanged(DatumExportService context, double amountComplete) {
			// each progress here counts for 50% of overall progress
			this.percentComplete += (amountComplete / 2.0);
			postJobStatusChangedEvent(this);
		}

		@Override
		public String getJobId() {
			return info.getId().toString();
		}

		@Override
		public DatumExportState getJobState() {
			return jobState;
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
		public DatumExportResult get() throws InterruptedException, ExecutionException {
			return delegate.get();
		}

		@Override
		public DatumExportResult get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException, TimeoutException {
			return delegate.get(timeout, unit);
		}

		@Override
		public String toString() {
			return "DatumExportTask{jobId=" + getJobId() + ",config="
					+ (info != null ? info.getConfig() : null) + ",jobState=" + jobState
					+ ",percentComplete=" + percentComplete + ",completionDate=" + completionDate + "}";
		}

	}

	@Override
	public DatumExportStatus performExport(DatumExportRequest info) {
		if ( info == null ) {
			throw new IllegalArgumentException("The task info argument is required.");
		}
		if ( info.getConfiguration() == null ) {
			throw new IllegalArgumentException("The configuration argument is required.");
		}
		DatumExportTaskInfo taskInfo = new DatumExportTaskInfo();
		taskInfo.setConfig(new BasicConfiguration(info.getConfiguration()));
		taskInfo.setExportDate(info.getExportDate());
		taskInfo.setId(info.getId());
		taskInfo.setCreated(Instant.now());
		taskInfo.setStatus(DatumExportState.Claimed);
		DatumExportTask task = new DatumExportTask(taskInfo);
		Future<DatumExportResult> future = executor.submit(task);
		task.setDelegate(future);
		taskMap.putIfAbsent(task.getJobId(), task);
		return task;
	}

	@Override
	public DatumExportStatus statusForJob(String jobId) {
		return taskMap.get(jobId);
	}

	private void postJobStatusChangedEvent(DatumExportTask task) {
		if ( task == null ) {
			return;
		}
		final AppEventPublisher ea = this.eventPublisher;
		if ( ea == null ) {
			return;
		}
		ea.postEvent(task.asJobStatusChagnedEvent(task.info));
	}

	private <T> T doWithinOptionalTransaction(Supplier<T> supplier) {
		if ( transactionTemplate != null ) {
			return transactionTemplate.execute(new TransactionCallback<T>() {

				@Override
				public T doInTransaction(TransactionStatus status) {

					return supplier.get();
				}

			});
		} else {
			return supplier.get();
		}
	}

	private <T extends Identity<String>> T optionalService(List<T> collection,
			IdentifiableConfiguration config) {
		if ( collection == null || config == null ) {
			return null;
		}
		String id = config.getServiceIdentifier();
		if ( id == null ) {
			return null;
		}
		for ( T service : collection ) {
			if ( id.equals(service.getId()) ) {
				return service;
			}
		}
		return null;
	}

	/**
	 * Set the optional output format services.
	 * 
	 * @param outputFormatServices
	 *        the optional services
	 */
	public void setOutputFormatServices(List<DatumExportOutputFormatService> outputFormatServices) {
		this.outputFormatServices = outputFormatServices;
	}

	/**
	 * Set the optional destination services.
	 * 
	 * @param destinationServices
	 *        the optional services
	 */
	public void setDestinationServices(List<DatumExportDestinationService> destinationServices) {
		this.destinationServices = destinationServices;
	}

	/**
	 * The minimum amount of time to maintain completed export tasks for the
	 * purposes of returning their status in {@link #statusForJob(String)}.
	 * 
	 * @param completedTaskMinimumCacheTime
	 *        the cache time, in milliseconds; defaults to 4 hours
	 */
	public void setCompletedTaskMinimumCacheTime(long completedTaskMinimumCacheTime) {
		this.completedTaskMinimumCacheTime = completedTaskMinimumCacheTime;
	}

	/**
	 * Configure a service for posting status events.
	 * 
	 * @param eventPublisher
	 *        the optional event admin service
	 */
	public void setEventPublisher(AppEventPublisher eventAdmin) {
		this.eventPublisher = eventAdmin;
	}

	/**
	 * Configure an auditor for export results.
	 * 
	 * @param queryAuditor
	 *        the auditor
	 * @since 1.2
	 */
	public void setQueryAuditor(QueryAuditor queryAuditor) {
		this.queryAuditor = queryAuditor;
	}

}
