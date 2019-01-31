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
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import net.solarnetwork.central.dao.BulkExportingDao.ExportCallback;
import net.solarnetwork.central.dao.BulkExportingDao.ExportCallbackAction;
import net.solarnetwork.central.datum.dao.GeneralNodeDatumDao;
import net.solarnetwork.central.datum.domain.AggregateGeneralNodeDatumFilter;
import net.solarnetwork.central.datum.domain.DatumFilterCommand;
import net.solarnetwork.central.datum.domain.GeneralNodeDatumFilterMatch;
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
import net.solarnetwork.central.query.biz.QueryAuditor;
import net.solarnetwork.central.support.FilterableBulkExportOptions;
import net.solarnetwork.domain.IdentifiableConfiguration;
import net.solarnetwork.domain.Identity;
import net.solarnetwork.util.OptionalService;
import net.solarnetwork.util.OptionalServiceCollection;
import net.solarnetwork.util.ProgressListener;

/**
 * DAO-based implementation of {@link DatumExportBiz}.
 * 
 * @author matt
 * @version 1.2
 */
public class DaoDatumExportBiz implements DatumExportBiz {

	public static final int DEFAULT_QUERY_PAGE_SIZE = 1000;

	private OptionalServiceCollection<DatumExportOutputFormatService> outputFormatServices;
	private OptionalServiceCollection<DatumExportDestinationService> destinationServices;
	private OptionalService<EventAdmin> eventAdmin;
	private long completedTaskMinimumCacheTime = TimeUnit.HOURS.toMillis(4);

	private final ConcurrentMap<String, DatumExportTask> taskMap = new ConcurrentHashMap<>(16);
	private final Logger log = LoggerFactory.getLogger(getClass());

	private final DatumExportTaskInfoDao taskDao;
	private final GeneralNodeDatumDao datumDao;
	private final ScheduledExecutorService scheduler;
	private final ExecutorService executor;
	private final TransactionTemplate transactionTemplate;
	private ScheduledFuture<?> taskPurgerTask;
	private OptionalService<QueryAuditor> queryAuditor;

	public DaoDatumExportBiz(DatumExportTaskInfoDao taskDao, GeneralNodeDatumDao datumDao,
			ScheduledExecutorService scheduler, ExecutorService executor,
			TransactionTemplate transactionTemplate) {
		super();
		this.taskDao = taskDao;
		this.datumDao = datumDao;
		this.scheduler = scheduler;
		this.executor = executor;
		this.transactionTemplate = transactionTemplate;
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
			this.taskPurgerTask = scheduler.scheduleWithFixedDelay(new TaskPurger(), 1L, 1L,
					TimeUnit.HOURS);
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

				updateTaskStatus(DatumExportState.Completed, Boolean.TRUE, null, new DateTime());
			} catch ( Exception e ) {
				log.warn("Error exporting datum for task {}", this, e);
				Throwable root = e;
				while ( root.getCause() != null ) {
					root = root.getCause();
				}
				StringBuilder msg = new StringBuilder();
				msg.append(root.getClass().getSimpleName());
				if ( root.getMessage() != null ) {
					msg.append(": ").append(root.getMessage());
				}
				msg.append("\n");
				try (StringWriter sout = new StringWriter(); PrintWriter out = new PrintWriter(sout)) {
					root.printStackTrace(out);
					msg.append(sout.toString());
				} catch ( IOException e2 ) {
					// ignore
				}
				updateTaskStatus(DatumExportState.Completed, Boolean.FALSE, msg.toString(),
						new DateTime());
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
				DateTime completionDate) {
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

			DateTimeZone zone = (config.getTimeZoneId() != null
					? DateTimeZone.forID(config.getTimeZoneId())
					: DateTimeZone.UTC);
			DatumFilterCommand filter = new DatumFilterCommand(datumFilter);
			if ( schedule == ScheduleType.Adhoc ) {
				DateTime s = datumFilter.getStartDate();
				DateTime e = datumFilter.getEndDate();
				if ( s == null || e == null ) {
					throw new DatumExportException(info.getId(),
							"Adhoc export missing start or end date in data configuration", null);
				}
				filter.setStartDate(s);
				filter.setEndDate(e);
			} else {
				filter.setStartDate(info.getExportDate().withZone(zone));
				filter.setEndDate(schedule.nextExportDate(filter.getStartDate()));
			}

			try (DatumExportOutputFormatService.ExportContext exportContext = outputService
					.createExportContext(config.getOutputConfiguration())) {

				FilterableBulkExportOptions options = new FilterableBulkExportOptions("test", filter,
						null);

				QueryAuditor auditor = (queryAuditor != null ? queryAuditor.service() : null);
				if ( auditor != null ) {
					auditor.resetCurrentAuditResults();
				}

				datumDao.batchExport(new ExportCallback<GeneralNodeDatumFilterMatch>() {

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
						if ( d != null && auditor != null ) {
							auditor.addNodeDatumAuditResults(singletonMap(d.getId(), 1));
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
		taskInfo.setCreated(new DateTime());
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
		EventAdmin ea = (this.eventAdmin != null ? this.eventAdmin.service() : null);
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

	private <T extends Identity<String>> T optionalService(OptionalServiceCollection<T> collection,
			IdentifiableConfiguration config) {
		if ( collection == null || config == null ) {
			return null;
		}
		String id = config.getServiceIdentifier();
		if ( id == null ) {
			return null;
		}
		Iterable<T> services = collection.services();
		for ( T service : services ) {
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
	public void setOutputFormatServices(
			OptionalServiceCollection<DatumExportOutputFormatService> outputFormatServices) {
		this.outputFormatServices = outputFormatServices;
	}

	/**
	 * Set the optional destination services.
	 * 
	 * @param destinationServices
	 *        the optional services
	 */
	public void setDestinationServices(
			OptionalServiceCollection<DatumExportDestinationService> destinationServices) {
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
	 * Configure an {@link EventAdmin} service for posting status events.
	 * 
	 * @param eventAdmin
	 *        the optional event admin service
	 */
	public void setEventAdmin(OptionalService<EventAdmin> eventAdmin) {
		this.eventAdmin = eventAdmin;
	}

	/**
	 * Configure an auditor for export results.
	 * 
	 * @param queryAuditor
	 *        the auditor
	 * @since 1.2
	 */
	public void setQueryAuditor(OptionalService<QueryAuditor> queryAuditor) {
		this.queryAuditor = queryAuditor;
	}

}
